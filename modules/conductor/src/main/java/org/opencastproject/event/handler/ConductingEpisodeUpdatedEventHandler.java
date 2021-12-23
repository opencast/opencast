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

package org.opencastproject.event.handler;

import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.security.api.SecurityService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * This handler listens for changes to episodes. Whenever a change is done, this is propagated to OAI-PMH.
 */
public class ConductingEpisodeUpdatedEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ConductingEpisodeUpdatedEventHandler.class);
  private static final String QUEUE_ID = "ASSETMANAGER.Conductor";

  private SecurityService securityService;
  private MessageReceiver messageReceiver;

  private OaiPmhUpdatedEventHandler oaiPmhUpdatedEventHandler;

  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

  private MessageWatcher messageWatcher;

  public void activate(ComponentContext cc) {
    logger.info("Activating {}", ConductingEpisodeUpdatedEventHandler.class.getName());
    messageWatcher = new MessageWatcher();
    singleThreadExecutor.execute(messageWatcher);
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", ConductingEpisodeUpdatedEventHandler.class.getName());
    if (messageWatcher != null) {
      messageWatcher.stopListening();
    }

    singleThreadExecutor.shutdown();
  }

  private class MessageWatcher implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(MessageWatcher.class);

    private volatile boolean listening = true;
    private FutureTask<Serializable> future;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void stopListening() {
      this.listening = false;
      future.cancel(true);
    }

    @Override
    public void run() {
      logger.info("Starting to listen for episode update messages");
      while (listening) {
        future = messageReceiver.receiveSerializable(QUEUE_ID, MessageSender.DestinationType.Queue);
        executor.execute(future);
        try {
          BaseMessage baseMessage = (BaseMessage) future.get();
          if (!(baseMessage.getObject() instanceof AssetManagerItem.TakeSnapshot)) {
            // We don't want to handle anything but TakeSnapshot messages.
            continue;
          }
          securityService.setOrganization(baseMessage.getOrganization());
          securityService.setUser(baseMessage.getUser());
          AssetManagerItem.TakeSnapshot snapshotItem = (AssetManagerItem.TakeSnapshot) baseMessage.getObject();
          if (AssetManagerItem.Type.Update.equals(snapshotItem.getType())) {
            // the OAI-PMH handler is a dynamic dependency
            if (oaiPmhUpdatedEventHandler != null) {
              oaiPmhUpdatedEventHandler.handleEvent(snapshotItem);
            }
          }
        } catch (CancellationException e) {
          logger.trace("Listening for episode update messages has been cancelled.");
        } catch (Throwable t) {
          logger.error("Problem while getting episode update message events", t);
        } finally {
          securityService.setOrganization(null);
          securityService.setUser(null);
        }
      }
      logger.info("Stopping listening for episode update messages");
    }

  }

  /**
   * OSGi DI callback.
   */
  public void setOaiPmhUpdatedEventHandler(OaiPmhUpdatedEventHandler h) {
    this.oaiPmhUpdatedEventHandler = h;
  }

  /**
   * OSGi DI callback.
   */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * OSGi DI callback.
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
