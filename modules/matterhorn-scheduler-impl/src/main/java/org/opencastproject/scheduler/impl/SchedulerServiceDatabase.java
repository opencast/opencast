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
package org.opencastproject.scheduler.impl;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.util.NotFoundException;

import java.util.Properties;

/**
 * Permanent storage for events. Each event consist of {@link DublinCoreCatalog} and optionally capture agent
 * Properties. Does not support searching.
 *
 */
public interface SchedulerServiceDatabase {

  /**
   * Removes event from persistent storage.
   *
   * @param eventId
   *          ID of event to be removed
   * @throws NotFoundException
   *           if there is no element with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void deleteEvent(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns all events in persistent storage.
   *
   * @return {@link DublinCoreCatalog} array representing events
   * @throws SchedulerServiceDatabaseException
   */
  DublinCoreCatalog[] getAllEvents() throws SchedulerServiceDatabaseException;

  /**
   * Returns CA metadata associated with specified event
   *
   * @param eventId
   *          event of which metadata should be returned
   * @return metadata as properties
   * @throws NotFoundException
   *           if event with given ID does not exist
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Properties getEventMetadata(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Store event(s).
   *
   * @param event
   *          {@link DublinCoreCatalog} representing event
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void storeEvents(DublinCoreCatalog... event) throws SchedulerServiceDatabaseException;

  /**
   * Updates event.
   *
   * @param event
   *          {@link DublinCoreCatalog} representing event to be updated
   * @throws NotFoundException
   *           if there is no previous event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEvent(DublinCoreCatalog event) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Adds metadata to events. Event must already be created, otherwise not found exception will be fired.
   *
   * @param eventId
   *          ID of events for which metadata will be added
   * @param caProperties
   *          Capture Agent properties to be added to metadata
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventWithMetadata(long eventId, Properties caProperties) throws NotFoundException,
          SchedulerServiceDatabaseException;

}
