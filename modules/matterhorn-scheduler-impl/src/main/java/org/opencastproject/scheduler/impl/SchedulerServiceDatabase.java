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

package org.opencastproject.scheduler.impl;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.NotFoundException;

import java.util.Date;
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
   * Count the number of events currently in the database.
   *
   * @return The number of events in the database.
   * @throws SchedulerServiceDatabaseException
   */
  int countEvents() throws SchedulerServiceDatabaseException;

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

  /**
   * Update the event access control list
   *
   * @param eventId
   *          ID of events for which access control list will be updated
   * @param accessControlList
   *          the access control list
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventAccessControlList(long eventId, AccessControlList accessControlList) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Update the event mediapackage identifier
   *
   * @param eventId
   *          ID of event for which the mediapackage will be updated
   * @param mediaPackageId
   *          the mediapackage ID to update
   * @throws NotFoundException
   *           if there is no event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventMediaPackageId(long eventId, String mediaPackageId) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Returns the access control list of the event with the id
   *
   * @param eventId
   *          the event ID
   * @return the access control list
   * @throws NotFoundException
   *           if there is no event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  AccessControlList getAccessControlList(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the mediapackage of the event with the id
   *
   * @param eventId
   *          the event ID
   * @return the mediapackage identifier
   * @throws NotFoundException
   *           if there is no event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  String getMediaPackageId(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the event identifier of the event with the given mediapackage id
   *
   * @param mediaPackageId
   *          the event's mediapackage id
   * @return the event identifier
   * @throws NotFoundException
   *           if there is no event with the given mediapackage id
   * @throws SchedulerException
   *           if exception occurred
   */
  Long getEventId(String mediaPackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the opt out status of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the opt out status
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isOptOut(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the opt out status of an event with the given event id
   *
   * @param eventId
   *          the event's id
   * @return the opt out status
   * @throws NotFoundException
   *           if there is no event with specified event ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isOptOut(Long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Update the event opted out status
   *
   * @param mediapackageId
   *          ID of events mediapackage for which the opted out status will be updated
   * @param optOut
   *          the opted out status
   * @throws NotFoundException
   *           if there is no previous event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventOptOutStatus(String mediapackageId, boolean optOut) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Update the event blacklist status
   *
   * @param eventId
   *          ID of event for which the blacklist status will be updated
   * @param blacklisted
   *          the blacklist status
   * @throws NotFoundException
   *           if there is no previous event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventOptOutStatus(Long eventId, boolean blacklisted) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Returns the review status of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the review status
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  ReviewStatus getReviewStatus(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the review date of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the review date
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Date getReviewDate(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Update the event review status
   *
   * @param mediapackageId
   *          ID of events mediapackage for which the review status will be updated
   * @param reviewStatus
   *          the review status
   * @param modificationDate
   *          the modification date
   * @throws NotFoundException
   *           if there is no previous event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventReviewStatus(String mediapackageId, ReviewStatus reviewStatus, Date modificationDate)
          throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the blacklist status of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the blacklist status
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isBlacklisted(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the blacklist status of an event with the given id
   *
   * @param eventId
   *          the event's id
   * @return the blacklist status
   * @throws NotFoundException
   *           if there is no event with specified event ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isBlacklisted(Long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Update the event blacklist status
   *
   * @param mediapackageId
   *          ID of events mediapackage for which the blacklist status will be updated
   * @param blacklisted
   *          the blacklist status
   * @throws NotFoundException
   *           if there is no previous event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventBlacklistStatus(String mediapackageId, boolean blacklisted) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Update the event blacklist status
   *
   * @param eventId
   *          ID of event for which the blacklist status will be updated
   * @param blacklisted
   *          the blacklist status
   * @throws NotFoundException
   *           if there is no previous event with the same ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void updateEventBlacklistStatus(Long eventId, boolean blacklisted) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Returns the count of reviewed events
   *
   * @return the number of reviewd events
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  long countTotalResponses() throws SchedulerServiceDatabaseException;

  /**
   * Returns the count of confirmed events
   *
   * @return the number of confirmed events
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  long countConfirmedResponses() throws SchedulerServiceDatabaseException;

  /**
   * Returns the quarter count of confirmed events
   *
   * @return the querter number of confirmed events
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  long countQuarterConfirmedResponses() throws SchedulerServiceDatabaseException;

  /**
   * Returns the daily count of confirmed events
   *
   * @return the daily number of confirmed events
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  long countDailyConfirmedResponses() throws SchedulerServiceDatabaseException;

  /**
   * Returns the count of unconfirmed events
   *
   * @return the number of unconfirmed events
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  long countUnconfirmedResponses() throws SchedulerServiceDatabaseException;

  /**
   * Returns the count of opted out events
   *
   * @return the number of opted out events
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  long countOptedOutResponses() throws SchedulerServiceDatabaseException;

}
