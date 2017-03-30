/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.liveschedule.message;

import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.SecurityService;

import com.google.common.util.concurrent.Striped;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Lock;

/**
 * Very simple approach to serialize the work of all three dependend update handlers. Todo: Merge all handlers into one
 * to avoid unnecessary distribution updates etc.
 */
public class LiveScheduleMessageReceiver {

  private static final Logger logger = LoggerFactory.getLogger(LiveScheduleMessageReceiver.class);
  // Striped lock for synchronizing on media package
  private static final Striped<Lock> lock = Striped.lazyWeakLock(1);

  private SecurityService securityService;
  private MessageReceiver messageReceiver;
  private HashMap<String, UpdateHandler> updateHandlers = new HashMap<String, UpdateHandler>();

  // One thread for each type of message watcher/queue id
  private HashMap<String, MessageWatcher> messageWatchers = new HashMap<String, MessageWatcher>();
  // Where message watchers are executed
  private ExecutorService executor = Executors.newCachedThreadPool();

  // Pool of threads for executing updates
  private ExecutorService updateExecutor = Executors.newCachedThreadPool();

  public void activate(ComponentContext cc) {
    logger.info("Activating {}", LiveScheduleMessageReceiver.class.getName());
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", LiveScheduleMessageReceiver.class.getName());
    for (String queue : messageWatchers.keySet()) {
      MessageWatcher mw = messageWatchers.get(queue);
      mw.stopListening();
    }
    executor.shutdown();
  }

  private class MessageWatcher implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(MessageWatcher.class);

    private boolean listening = true;
    private String queueId;
    private FutureTask<Serializable> future;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    MessageWatcher(String queueId) {
      this.queueId = queueId;
    }

    public void stopListening() {
      this.listening = false;
      future.cancel(true);
    }

    @Override
    public void run() {
      logger.info("Starting to listen for {} update messages", queueId);
      while (listening) {
        future = messageReceiver.receiveSerializable(queueId, MessageSender.DestinationType.Queue);
        executor.execute(future);
        try {
          BaseMessage baseMessage = (BaseMessage) future.get();
          UpdateHandler handler = updateHandlers.get(queueId);

          // Start execution in a new thread so that we don't block listening to messages.
          // We will synchronize by media package id
          updateExecutor.execute(new Runnable() {
            @Override
            public void run() {
              securityService.setOrganization(baseMessage.getOrganization());
              securityService.setUser(baseMessage.getUser());
              if (handler != null) {
                Lock l = lock.get(baseMessage.getId().get());
                try {
                  l.lock();
                  handler.execute((MessageItem) baseMessage.getObject());
                } finally {
                  l.unlock();
                }
              }
            }
          });
        } catch (InterruptedException e) {
          logger.error("Problem while getting {} message events {}", queueId, ExceptionUtils.getStackTrace(e));
        } catch (ExecutionException e) {
          logger.error("Problem while getting {} message events {}", queueId, ExceptionUtils.getStackTrace(e));
        } catch (CancellationException e) {
          logger.trace("Listening for {} messages has been cancelled.", queueId);
        } catch (Throwable t) {
          logger.error("Problem while getting {} message events {}", queueId, ExceptionUtils.getStackTrace(t));
        } finally {
          securityService.setOrganization(null);
          securityService.setUser(null);
        }
      }
      logger.info("Stopping listening for {} update messages", queueId);
    }
  }

  // === Set by OSGI begin
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void addUpdateHandler(UpdateHandler handler) {
    String queueId = handler.getDestinationId();
    if (updateHandlers.get(queueId) == null) {
      logger.info("Adding live schedule message handler for {}", queueId);
      updateHandlers.put(queueId, handler);
      MessageWatcher mw = new MessageWatcher(queueId);
      messageWatchers.put(queueId, mw);
      executor.execute(mw);
    }
  }

  public void removeUpdateHandler(UpdateHandler handler) {
    String queueId = handler.getDestinationId();
    if (updateHandlers.get(queueId) != null) {
      logger.info("Removing live schedule message handler for {}", queueId);
      MessageWatcher mw = messageWatchers.get(queueId);
      mw.stopListening();
      messageWatchers.remove(queueId);
      updateHandlers.remove(queueId);
    }
  }
  // === Set by OSGI end

}
