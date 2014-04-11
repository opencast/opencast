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
package org.opencastproject.event.handler;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opencastproject.event.EventAdminConstants.ID;

/**
 * Very simple approach to serialize the work of all three dependend update handlers.
 * Todo: Merge all handlers into one to avoid unnecessary distribution updates etc.
 */
public class ConductingSeriesUpdatedEventHandler implements EventHandler {
  private static final Logger logger = LoggerFactory.getLogger(SeriesUpdatedEventHandler.class);
  private EpisodesPermissionsUpdatedEventHandler episodesPermissionsUpdatedEventHandler;
  private SeriesUpdatedEventHandler seriesUpdatedEventHandler;
  private WorkflowPermissionsUpdatedEventHandler workflowPermissionsUpdatedEventHandler;
  // Use a single thread executor to ensure that only one update is handled at a time.
  // This is because Matterhorn lacks a distributed synchronization model on media packages and/or series.
  // Note that this measure only _reduces_ the chance of data corruption cause by concurrent modifications.
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Override public void handleEvent(final Event event) {
    // Handle events in a separate thread to return immediately because
    // long running event handlers get black listed by the event admin.
    // See http://felix.apache.org/site/apache-felix-event-admin.html for details.
    executor.execute(new Runnable() {
      @Override public void run() {
        final String seriesId = (String) event.getProperty(ID);
        logger.debug("Handling event for series " + seriesId);
        seriesUpdatedEventHandler.handleEvent(event);
        episodesPermissionsUpdatedEventHandler.handleEvent(event);
        workflowPermissionsUpdatedEventHandler.handleEvent(event);
      }
    });
  }

  /** OSGi DI callback. */
  public void setEpisodesPermissionsUpdatedEventHandler(EpisodesPermissionsUpdatedEventHandler h) {
    this.episodesPermissionsUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setSeriesUpdatedEventHandler(SeriesUpdatedEventHandler h) {
    this.seriesUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setWorkflowPermissionsUpdatedEventHandler(WorkflowPermissionsUpdatedEventHandler h) {
    this.workflowPermissionsUpdatedEventHandler = h;
  }
}
