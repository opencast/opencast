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
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.security.api.SecurityService;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Very simple approach to serialize the work of all three dependend update handlers. Todo: Merge all handlers into one
 * to avoid unnecessary distribution updates etc.
 */
public class ConductingSeriesUpdatedEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(SeriesUpdatedEventHandler.class);
  private static final String QUEUE_ID = "SERIES.Conductor";

  private SecurityService securityService;
  private MessageReceiver messageReceiver;

  private ArchivePermissionsUpdatedEventHandler archivePermissionsUpdatedEventHandler;
  private SeriesUpdatedEventHandler seriesUpdatedEventHandler;
  private WorkflowPermissionsUpdatedEventHandler workflowPermissionsUpdatedEventHandler;

  // Use a single thread executor to ensure that only one update is handled at a time.
  // This is because Matterhorn lacks a distributed synchronization model on media packages and/or series.
  // Note that this measure only _reduces_ the chance of data corruption cause by concurrent modifications.
  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

  private MessageWatcher messageWatcher;

  public void activate(ComponentContext cc) {
    logger.info("Activating {}", ConductingSeriesUpdatedEventHandler.class.getName());
    messageWatcher = new MessageWatcher();
    singleThreadExecutor.execute(messageWatcher);
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", ConductingSeriesUpdatedEventHandler.class.getName());
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
      future.cancel(true);
    }

    @Override
    public void run() {
      logger.info("Starting to listen for series update Messages");
      while (listening) {
        future = messageReceiver.receiveSerializable(QUEUE_ID, MessageSender.DestinationType.Queue);
        executor.execute(future);
        try {
          BaseMessage baseMessage = (BaseMessage) future.get();
          securityService.setOrganization(baseMessage.getOrganization());
          securityService.setUser(baseMessage.getUser());
          SeriesItem seriesItem = (SeriesItem) baseMessage.getObject();

          if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())
                  || SeriesItem.Type.UpdateAcl.equals(seriesItem.getType())
                  || SeriesItem.Type.Delete.equals(seriesItem.getType())) {
            seriesUpdatedEventHandler.handleEvent(seriesItem);
            archivePermissionsUpdatedEventHandler.handleEvent(seriesItem);
            workflowPermissionsUpdatedEventHandler.handleEvent(seriesItem);
          }
        } catch (InterruptedException e) {
          logger.error("Problem while getting series update message events {}", ExceptionUtils.getStackTrace(e));
        } catch (ExecutionException e) {
          logger.error("Problem while getting series update message events {}", ExceptionUtils.getStackTrace(e));
        } catch (CancellationException e) {
          logger.trace("Listening for series update messages has been cancelled.");
        } catch (Throwable t) {
          logger.error("Problem while getting series update message events {}", ExceptionUtils.getStackTrace(t));
        } finally {
          securityService.setOrganization(null);
          securityService.setUser(null);
        }
      }
      logger.info("Stopping listening for series update Messages");
    }

  }

  /** OSGi DI callback. */
  public void setArchivePermissionsUpdatedEventHandler(ArchivePermissionsUpdatedEventHandler h) {
    this.archivePermissionsUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setSeriesUpdatedEventHandler(SeriesUpdatedEventHandler h) {
    this.seriesUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setWorkflowPermissionsUpdatedEventHandler(WorkflowPermissionsUpdatedEventHandler h) {
    this.workflowPermissionsUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
