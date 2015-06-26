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

package org.opencastproject.scheduler.api;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Scheduler service manages events (creates new, updates already existing and removes events). It enables searches over
 * existing events, retrieving DublinCore or capture agent properties for specific event, search for conflicting events
 * and generating calendar for capture agent.
 */
public interface SchedulerService {

  public enum ReviewStatus {
    UNSENT, UNCONFIRMED, CONFIRMED
  }

  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.scheduler";

  /**
   * Creates new event using specified DublinCore. Default capture agent properties are created from DublinCore.
   * Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   *
   * @param eventCatalog
   *          {@link DublinCoreCatalog} used for creating event
   * @param wfProperties
   *          any properties to apply to the workflow definition
   * @return ID of created event
   * @throws SchedulerException
   *           if creating new events failed
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   */
  Long addEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties) throws SchedulerException,
          UnauthorizedException;

  /**
   * Creates series of events using DublinCore as template and recurrence pattern. For each event default capture agent
   * properties are created from DublinCore. Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   *
   * @param eventCatalog
   *          template {@link DublinCoreCatalog} used to create events
   * @param wfProperties
   *          any properties to apply to the workflow definition
   * @return array of events IDs that were created
   * @throws SchedulerException
   *           if events cannot be created
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   */
  Long[] addReccuringEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties) throws SchedulerException,
          UnauthorizedException;

  /**
   * Updates existing events with capture agent metadata. Configuration will be updated from event's DublinCore:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   *
   * @param events
   *          array of events that should be updated
   * @param configuration
   *          Properties for capture agent
   * @throws NotFoundException
   *           there is event that does not exist
   * @throws SchedulerException
   *           if update fails
   */
  void updateCaptureAgentMetadata(Properties configuration, Tuple<Long, DublinCoreCatalog>... events)
          throws NotFoundException, SchedulerException;

  /**
   * Update event's DublinCore. Capture agent metadata will also be updated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   * Please note that the dublin core's identifier property is <em>not</em> used.
   *
   * @param eventCatalog
   *          updated {@link DublinCoreCatalog}
   * @param wfProperties
   *          any properties to apply to the workflow definition
   * @throws NotFoundException
   *           if events with specified DublinCore ID cannot be found
   * @throws SchedulerException
   *           if update fails
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   */
  void updateEvent(long eventId, DublinCoreCatalog eventCatalog, Map<String, String> wfProperties)
          throws NotFoundException, SchedulerException, UnauthorizedException;

  /**
   * Removes event with specified ID.
   *
   * @param eventId
   *          event to be removed
   * @throws SchedulerException
   *           if exception occurred
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   */
  void removeEvent(long eventId) throws SchedulerException, NotFoundException, UnauthorizedException;

  /**
   * Retrieves DublinCore associated with specified event ID.
   *
   * @param eventId
   *          ID of event for which DublinCore will be retrieved
   * @return {@link DublinCoreCatalog} for specified event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalog getEventDublinCore(long eventId) throws NotFoundException, SchedulerException;

  /**
   * Retrieves capture agent configuration for specified event.
   *
   * @param eventId
   *          ID of event for which capture agent configuration should be retrieved
   * @return Properties for capture agent
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  Properties getEventCaptureAgentConfiguration(long eventId) throws NotFoundException, SchedulerException;

  /**
   * Retrieves all events matching given query object.
   *
   * @param query
   *          {@link SchedulerQuery} representing query
   * @return {@link DublinCoreCatalogList} with results matching given query
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalogList search(SchedulerQuery query) throws SchedulerException;

  /**
   * Returns list of all conflicting events, i.e. all events that ends after start date and begins before end date.
   *
   * @param captureDeviceID
   *          capture device ID for which conflicting events are searched for
   * @param startDate
   *          start date of of conflicting period
   * @param endDate
   *          end date of conflicting period
   * @return list of events that are in conflict with specified period
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalogList findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
          throws SchedulerException;

  /**
   * Returns list of all conflicting events. Conflicting periods are calculated based on recurrence rule, start date,
   * end date and duration of each conflicting period.
   *
   * @param captureDeviceID
   *          capture device ID for which conflicting events are searched for
   * @param rrule
   *          recurrence rule
   * @param startDate
   *          beginning of period
   * @param endDate
   *          ending of period
   * @param duration
   *          duration of each period
   * @return list of all conflicting events
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalogList findConflictingEvents(String captureDeviceID, String rrule, Date startDate, Date endDate,
          long duration, String timezone) throws SchedulerException;

  /**
   * Generates calendar for specified capture agent.
   *
   * @param filter
   *          filter on which calendar will be generated
   * @return generated calendar
   * @throws SchedulerException
   *           if exception occurred
   */
  String getCalendar(SchedulerQuery filter) throws SchedulerException;

  /**
   * Returns hash of last modification of event belonging to specified capture agent.
   *
   * @param agentId
   *          the agent id
   * @return the last modification hash
   * @throws SchedulerException
   *           if exception occurred
   */
  String getScheduleLastModified(String agentId) throws SchedulerException;

  /** Update all events with metadata from eventCatalog. */
  void updateEvents(List<Long> eventIds, final DublinCoreCatalog eventCatalog) throws NotFoundException,
          SchedulerException, UnauthorizedException;

  /**
   * Remove all of the scheduled events before a buffer.
   *
   * @param buffer
   *          The number of seconds before now that defines a cutoff for events, if they have their end time before this
   *          cutoff they will be removed
   * @throws SchedulerException
   */
  void removeScheduledRecordingsBeforeBuffer(long buffer) throws SchedulerException;

  /**
   * Returns the access control list of the event with the id
   *
   * @param eventId
   *          the event ID
   * @return the access control list or <code>null</code> if no acces control list has been set
   * @throws NotFoundException
   *           if there is no event with the same ID
   * @throws SchedulerException
   *           if exception occurred
   */
  AccessControlList getAccessControlList(long eventId) throws NotFoundException, SchedulerException;

  /**
   * Update the access control list of the event with the id
   *
   * @param eventId
   *          the event ID
   * @param accessControlList
   *          the access control list
   * @throws NotFoundException
   *           if there is no event with the same ID
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateAccessControlList(long eventId, AccessControlList accessControlList) throws NotFoundException,
          SchedulerException;

  /**
   * Returns the mediapackage of the event with the id
   *
   * @param eventId
   *          the event ID
   * @return the mediapackage identifier
   * @throws NotFoundException
   *           if there is no event with the same ID
   * @throws SchedulerException
   *           if exception occurred
   */
  String getMediaPackageId(long eventId) throws NotFoundException, SchedulerException;

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
  Long getEventId(String mediaPackageId) throws NotFoundException, SchedulerException;

  /**
   * Returns the opt out status of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the opt out status
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerException
   *           if exception occurred
   */
  boolean isOptOut(String mediapackageId) throws NotFoundException, SchedulerException;

  /**
   * Updates the opted out status of the event with the given ID
   *
   * @param mediapackageId
   *          ID of event's mediapackage for which opted out status will be changed
   * @param optedOut
   *          the opted out status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateOptOutStatus(String mediapackageId, boolean optedOut) throws NotFoundException, SchedulerException;

  /**
   * Returns the review status of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the review status
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerException
   *           if exception occurred
   */
  ReviewStatus getReviewStatus(String mediapackageId) throws NotFoundException, SchedulerException;

  /**
   * Updates the review status of the event with the given mediapackage ID
   *
   * @param mediapackageId
   *          ID of event's mediapackage for which review status will be changed
   * @param reviewStatus
   *          the review status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateReviewStatus(String mediapackageId, ReviewStatus reviewStatus) throws NotFoundException,
          SchedulerException;

  /**
   * Updates the workflow properties of the event with the given mediapackage ID
   *
   * @param mediapackageId
   *          ID of event's mediapackage for which the workflow properties will be changed
   * @param properties
   *          the workflow properties
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateWorkflowConfig(String mediapackageId, Map<String, String> properties) throws NotFoundException,
          SchedulerException;

  /**
   * Returns the blacklist status of an event with the given mediapackage id
   *
   * @param mediapackageId
   *          the mediapackage id
   * @return the blacklist status
   * @throws NotFoundException
   *           if there is no event with specified mediapackage ID
   * @throws SchedulerException
   *           if exception occurred
   */
  boolean isBlacklisted(String mediapackageId) throws NotFoundException, SchedulerException;

  /**
   * Updates the blacklist status of the event with the given mediapackage ID
   *
   * @param mediapackageId
   *          ID of event's mediapackage for which blacklist status will be changed
   * @param blacklisted
   *          the blacklist status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateBlacklistStatus(String mediapackageId, boolean blacklisted) throws NotFoundException, SchedulerException;

}
