/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.index.service.message;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Status;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.OsgiUtil;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public abstract class BaseMessageReceiverImpl<T extends Serializable> {

  private static final String DESTINATION_ID_KEY = "destinationId";
  private static final Logger logger = LoggerFactory.getLogger(BaseMessageReceiverImpl.class);
  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

  private SecurityService securityService;
  private MessageSender messageSender;
  private MessageReceiver messageReceiver;
  private MessageWatcher messageWatcher;
  private AbstractSearchIndex index;
  private String destinationId;
  private MessageSender.DestinationType destinationType;

  public BaseMessageReceiverImpl(MessageSender.DestinationType destinationType) {
    this.destinationType = destinationType;
  }

  public void activate(ComponentContext cc) {
    logger.info("Activating {}", this.getClass().getName());
    destinationId = OsgiUtil.getComponentContextProperty(cc, DESTINATION_ID_KEY);
    logger.info("The {} for this message receiver is '{}'", DESTINATION_ID_KEY, destinationId);
    messageWatcher = new MessageWatcher();
    singleThreadExecutor.execute(messageWatcher);
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", this.getClass().getName());
    if (messageWatcher != null)
      messageWatcher.stopListening();

    singleThreadExecutor.shutdown();
  }

  protected abstract void execute(T messageContent);

  protected String getDestinationId() {
    return destinationId;
  }

  protected DestinationType getDestinationType() {
    return destinationType;
  }

  protected AbstractSearchIndex getSearchIndex() {
    return index;
  }

  protected SecurityService getSecurityService() {
    return securityService;
  }

  private class MessageWatcher implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(MessageWatcher.class);

    private boolean listening = true;
    private FutureTask<Serializable> future;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String clazzName = BaseMessageReceiverImpl.this.getClass().getName();

    public void stopListening() {
      this.listening = false;
      future.cancel(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      logger.info("Starting to listen for {} Messages", clazzName);
      while (listening) {
        future = messageReceiver.receiveSerializable(getDestinationId(), getDestinationType());
        executor.execute(future);
        try {
          BaseMessage baseMessage = (BaseMessage) future.get();
          securityService.setOrganization(baseMessage.getOrganization());
          securityService.setUser(baseMessage.getUser());
          if (baseMessage.getObject() instanceof IndexRecreateObject) {
            IndexRecreateObject obj = (IndexRecreateObject) baseMessage.getObject();
            if (Status.End.equals(obj.getStatus()))
              messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                      IndexRecreateObject.end(obj.getIndexName(), obj.getService()));
          } else {
            execute((T) baseMessage.getObject());
          }
        } catch (InterruptedException e) {
          logger.error("Problem while getting {} message events {}", clazzName, ExceptionUtils.getStackTrace(e));
        } catch (ExecutionException e) {
          logger.error("Problem while getting {} message events {}", clazzName, ExceptionUtils.getStackTrace(e));
        } catch (CancellationException e) {
          logger.trace("Listening for messages {} has been cancelled.", clazzName);
        } catch (Throwable t) {
          logger.error("Problem while getting {} message events {}", clazzName, ExceptionUtils.getStackTrace(t));
        } finally {
          securityService.setOrganization(null);
          securityService.setUser(null);
        }
      }
      logger.info("Stopping listening for {} Messages", clazzName);
    }
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  public void setSearchIndex(AbstractSearchIndex index) {
    this.index = index;
  }

}
