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

package org.opencastproject.message.broker.api.index;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * This service produces messages for an elastic search index
 */
public abstract class AbstractIndexProducer implements IndexProducer {

  public abstract String getClassName();

  public abstract MessageReceiver getMessageReceiver();

  public abstract IndexRecreateObject.Service getService();

  /** The message watcher */
  private MessageWatcher messageWatcher;

  /** Single thread executor */
  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

  public void activate() {
    messageWatcher = new MessageWatcher();
    singleThreadExecutor.execute(messageWatcher);
  }

  public void deactivate() {
    if (messageWatcher != null)
      messageWatcher.stopListening();

    singleThreadExecutor.shutdown();
  }

  private class MessageWatcher implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(MessageWatcher.class);
    private boolean listening = true;
    private FutureTask<Serializable> future;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void stopListening() {
      this.listening = false;
      if (future != null) {
        future.cancel(true);
      }
    }

    @Override
    public void run() {
      if (getMessageReceiver() == null) {
        logger.warn("The message receiver for " + getClassName()
                + " was null so unable to listen for repopulate index messages. Ignore this warning if this is a test.");
        listening = false;
      }
      logger.info("Starting to listen for {} Messages", getClassName());
      while (listening) {
        try {
          future = getMessageReceiver().receiveSerializable(IndexProducer.RECEIVER_QUEUE + "." + getService(),
                  MessageSender.DestinationType.Queue);
          executor.execute(future);
          BaseMessage message = (BaseMessage) future.get();
          if (!(message.getObject() instanceof IndexRecreateObject))
            continue;

          IndexRecreateObject indexObject = (IndexRecreateObject) message.getObject();
          if (!indexObject.getService().equals(getService())
                  || !indexObject.getStatus().equals(IndexRecreateObject.Status.Start))
            continue;
          logger.info("Index '{}' has received a start repopulating command for service '{}'.",
                  indexObject.getIndexName(), getService());
          repopulate(indexObject.getIndexName());
          logger.info("Index '{}' has finished repopulating service '{}'.", indexObject.getIndexName(), getService());
        } catch (InterruptedException e) {
          logger.error("Problem while getting {} message events {}", getClassName(), ExceptionUtils.getStackTrace(e));
        } catch (ExecutionException e) {
          logger.error("Problem while getting {} message events {}", getClassName(), ExceptionUtils.getStackTrace(e));
        } catch (CancellationException e) {
          logger.trace("Listening for messages {} has been cancelled.", getClassName());
        } catch (Throwable t) {
          logger.error("Problem while getting {} message events {}", getClassName(), ExceptionUtils.getStackTrace(t));
        }
      }
      logger.info("Stopping listening for {} Messages", getClassName());
    }

  }

}
