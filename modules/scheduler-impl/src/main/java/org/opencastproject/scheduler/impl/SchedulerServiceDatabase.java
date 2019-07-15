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

import org.opencastproject.scheduler.impl.persistence.ExtendedEventDto;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Permanent storage for events. Does not support searching.
 */
public interface SchedulerServiceDatabase {

  /**
   * Touches the most recent entry by updating its last modification date.
   *
   * @param agentId
   *          the capture agent identifier
   * @throws SchedulerServiceDatabaseException
   *           if updating of the last modified value fails
   */
  void touchLastEntry(String agentId) throws SchedulerServiceDatabaseException;

  /**
   * Get the last modification date by an agent identifier
   *
   * @param agentId
   *          the capture agent identifier
   * @return the last modification date
   * @throws NotFoundException
   *           if the agent could not be found
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Date getLastModified(String agentId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Get a {@link Map} of last modification dates of all existing capture agents.
   *
   * @return the last modified map
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Map<String, Date> getLastModifiedDates() throws SchedulerServiceDatabaseException;


  /**
   * Create or update an event identified by mediapackageId and organizationId
   *
   * @param mediapackageId
   *          the mediapackage ID
   * @param organizationId
   *          the organization ID of the organization owning the event
   * @param captureAgentId
   *          the capture agent ID of the capture agent this event is scheduled on
   * @param start
   *          the recording start time
   * @param end
   *          the recording end time
   * @param source
   *          the source
   * @param recordingState
   *          the recording state
   * @param  recordingLastHeard
   *          the recording last heard
   * @param presenters
   *          the presenters
   * @param lastModifiedDate
   *          the last modified date
   * @param checksum
   *          the checksum
   * @param workflowProperties
   *          the workflow properties
   * @param captureAgentProperties
   *          the capture agent properties
   * @throws SchedulerServiceDatabaseException in case the event cannot be stored.
   */
  void storeEvent(
      String mediapackageId,
      String organizationId,
      Opt<String> captureAgentId,
      Opt<Date> start,
      Opt<Date> end,
      Opt<String> source,
      Opt<String> recordingState,
      Opt<Long> recordingLastHeard,
      Opt<String> presenters,
      Opt<Date> lastModifiedDate,
      Opt<String> checksum,
      Opt<Map<String,String>> workflowProperties,
      Opt<Map<String,String>> captureAgentProperties
  ) throws SchedulerServiceDatabaseException;

  /**
   * Get the mediapackage IDs of all events scheduled on the given capture agent between the given start/end time.
   * Events which are only partially contained within the given interval are also included in the result set. The
   * results are ordered by start date ascending.
   *
   * @param captureAgentId
   *          the capture agent ID of the capture agent to check
   * @param start
   *          the start date of the interval to check
   * @param end
   *          the end date of the interval to check
   * @param separationMillis
   *          number of milliseconds to prepend and append to given interval
   * @return The mediapackage IDs of the events between start (inclusive) and end (inclusive) scheduled on the given
   * capture agent.
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  List<String> getEvents(String captureAgentId, Date start, Date end, int separationMillis) throws SchedulerServiceDatabaseException;

  /**
   * Retrieve all events matching given filter ordered by start time ascending.
   *
   * @param captureAgentId
   *          the capture agent id filter
   * @param startsFrom
   *          the start from date filter
   * @param startsTo
   *          the start to date filter
   * @param endFrom
   *          the end from date filter
   * @param endTo
   *          the end to date filter
   * @param limit
   *          the maximum number of results to retrieve
   * @return The events matching the given filter
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  List<ExtendedEventDto> search(Opt<String> captureAgentId, Opt<Date> startsFrom, Opt<Date> startsTo, Opt<Date> endFrom, Opt<Date> endTo, Opt<Integer> limit) throws SchedulerServiceDatabaseException;

  /**
   * Retrieve all events which have a recording state and a recording last heard.
   *
   * @return all events which have a recording state and a recording last heard
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  List<ExtendedEventDto> getKnownRecordings() throws SchedulerServiceDatabaseException;

  /**
   * Removes the extended event from persistent storage.
   *
   * @param mediapackageId
   *          ID of event to be removed
   * @throws NotFoundException
   *           if there is no element with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void deleteEvent(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Get the event with the given mediapackage id for the current organization.
   *
   * @param mediapackageId
   *          The mediapackage id to look for
   *
   * @return The event or nothing, if the event couldn't be found.
   *
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  Opt<ExtendedEventDto> getEvent(String mediapackageId) throws SchedulerServiceDatabaseException;

  /**
   * Get the event with the given mediapackage id and organization.
   *
   * @param mediapackageId
   *          The mediapackage id to look for
   * @param orgId
   *          The organization id to look for
   *
   * @return The event or nothing, if the event couldn't be found.
   *
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  Opt<ExtendedEventDto> getEvent(String mediapackageId, String orgId) throws SchedulerServiceDatabaseException;

  /**
   * Get all events from the scheduler for the current organizations.
   *
   * @return The list of events.
   *
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  List<ExtendedEventDto> getEvents() throws SchedulerServiceDatabaseException;

  /**
   * Nulls recording state and recording last heard of of the given media package.
   * @param mediapackageId
   *          The mediapackage id to look for
   * @throws NotFoundException
   *           if there is no element with specified ID
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  void resetRecordingState(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Retrieve the number of events.
   *
   * @return The number of events.
   * @throws SchedulerServiceDatabaseException
   *           If the database cannot be queried.
   */
  int countEvents() throws SchedulerServiceDatabaseException;
}
