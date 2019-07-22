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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.property.RRule;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Scheduler service manages events (creates new, updates already existing and removes events). It enables searches over
 * existing events, retrieving event data like dublincore, acl or workflow configuration for specific event, search for
 * conflicting events and generating calendar for capture agent.
 */
public interface SchedulerService {

  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.scheduler";

  /**
   * Creates new event using specified mediapackage, workflow configuration and capture agent configuration. The
   * mediapackage id is used as the event's identifier.
   *
   * Default capture agent properties are created from agentId and DublinCore. Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from mediaPackage#getSeries())</li>
   * <li>event.location (mapped from captureAgentId)</li>
   * </ul>
   *
   * @param startDateTime
   *          the event start time (the start date must be before the end date)
   * @param endDateTime
   *          the event end time (the end date must be after the start date)
   * @param captureAgentId
   *          the capture agent id
   * @param userIds
   *          the list of user identifiers of speakers/lecturers
   * @param mediaPackage
   *          the mediapackage
   * @param wfProperties
   *          the workflow configuration
   * @param caMetadata
   *          the capture agent configuration
   * @param schedulingSource
   *          the optional scheduling source from which the event comes from
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   * @throws SchedulerConflictException
   *           if there are conflicting events
   * @throws SchedulerException
   *           if creating new events failed
   */
  void addEvent(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
          MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
          Opt<String> schedulingSource) throws UnauthorizedException,
                  SchedulerConflictException, SchedulerException;

  /**
   * Creates a group of new event using specified mediapackage, workflow configuration and capture agent configuration.
   * The mediapackage id is used as the event's identifier.
   *
   * Default capture agent properties are created from agentId and DublinCore. Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from mediaPackage#getSeries())</li>
   * <li>event.location (mapped from captureAgentId)</li>
   * </ul>
   *
   * @param rRule
   *          the {@link RRule} for the events to schedule
   * @param start
   *          the start date for the recurrence
   * @param end
   *          the end date for the recurrence
   * @param duration
   *          the duration of the events
   * @param tz
   *          the {@link TimeZone} for the events
   * @param captureAgentId
   *          the capture agent id
   * @param userIds
   *          the list of user identifiers of speakers/lecturers
   * @param templateMp
   *          the mediapackage to base the events on
   * @param wfProperties
   *          the workflow configuration
   * @param caMetadata
   *          the capture agent configuration
   * @param schedulingSource
   *          the optional scheduling source from which the event comes from
   * @return A {@link Map} of mediapackage ID and {@link Period} where the event occurs
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   * @throws SchedulerConflictException
   *           if there are conflicting events
   * @throws SchedulerException
   *           if creating new events failed
   */
  Map<String, Period> addMultipleEvents(RRule rRule, Date start, Date end, Long duration, TimeZone tz,
          String captureAgentId, Set<String> userIds, MediaPackage templateMp, Map<String,
          String> wfProperties, Map<String, String> caMetadata, Opt<String> schedulingSource)
          throws UnauthorizedException, SchedulerConflictException, SchedulerException;

  /**
   * Updates event with specified ID and check for conflicts.
   *
   * Default capture agent properties are created from DublinCore. Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from mediaPackage#getSeries())</li>
   * <li>event.location (mapped from captureAgentId)</li>
   * </ul>
   *
   * @param mediaPackageId
   *          the optional event identifier
   * @param startDateTime
   *          the optional event start time
   * @param endDateTime
   *          the optional event end time
   * @param captureAgentId
   *          the optional capture agent id
   * @param userIds
   *          the optional list of user identifiers of speakers/lecturers
   * @param mediaPackage
   *          the optional mediapackage to update
   * @param wfProperties
   *          the optional workflow configuration to update
   * @param caMetadata
   *          the optional capture configuration to update
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SchedulerConflictException
   *           if there are conflicting events
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateEvent(String mediaPackageId, Opt<Date> startDateTime, Opt<Date> endDateTime, Opt<String> captureAgentId,
          Opt<Set<String>> userIds, Opt<MediaPackage> mediaPackage, Opt<Map<String, String>> wfProperties,
          Opt<Map<String, String>> caMetadata)
                  throws NotFoundException, UnauthorizedException, SchedulerConflictException, SchedulerException;

  /**
   * Updates event with specified ID and possibly checking for conflicts.
   *
   * Default capture agent properties are created from DublinCore. Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from mediaPackage#getSeries())</li>
   * <li>event.location (mapped from captureAgentId)</li>
   * </ul>
   *
   * @param mediaPackageId
   *          the event identifier
   * @param startDateTime
   *          the optional event start time
   * @param endDateTime
   *          the optional event end time
   * @param captureAgentId
   *          the optional capture agent id
   * @param userIds
   *          the optional list of user identifiers of speakers/lecturers
   * @param mediaPackage
   *          the optional mediapackage to update
   * @param wfProperties
   *          the optional workflow configuration to update
   * @param caMetadata
   *          the optional capture configuration to update
   * @param allowConflict
   *          the flag to ignore conflict checks
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SchedulerConflictException
   *           if there are conflicting events
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateEvent(String mediaPackageId, Opt<Date> startDateTime, Opt<Date> endDateTime, Opt<String> captureAgentId,
          Opt<Set<String>> userIds, Opt<MediaPackage> mediaPackage, Opt<Map<String, String>> wfProperties,
          Opt<Map<String, String>> caMetadata, boolean allowConflict)
                  throws NotFoundException, UnauthorizedException, SchedulerConflictException, SchedulerException;

  /**
   * Removes event with specified ID.
   *
   * @param mediaPackageId
   *          the event identifier
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SchedulerException
   *           if exception occurred
   */
  void removeEvent(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Retrieves mediapackage associated with specified event ID.
   *
   * @param mediaPackageId
   *          ID of event for which mediapackage will be retrieved
   * @return {@link MediaPackage} for specified event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  MediaPackage getMediaPackage(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Retrieves dublin core catalog associated with specified event ID.
   *
   * @param mediaPackageId
   *          ID of event for which DublinCore will be retrieved
   * @return {@link DublinCoreCatalog} for specified event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalog getDublinCore(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Retrieves the technical metadata associated with specified event ID.
   *
   * @param mediaPackageId
   *          ID of event for which technical metadata will be retrieved
   * @return {@link TechnicalMetadata} for specified event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  TechnicalMetadata getTechnicalMetadata(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Retrieves access control list associated with specified event ID.
   *
   * @param mediaPackageId
   *          ID of event for which access control list will be retrieved
   * @return {@link AccessControlList} for specified event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  AccessControlList getAccessControlList(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Retrieves workflow configuration associated with specified event ID.
   *
   * @param mediaPackageId
   *          ID of event for which workflow configuration will be retrieved
   * @return configuration of the workflow
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  Map<String, String> getWorkflowConfig(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Retrieves capture agent configuration for specified event.
   *
   * @param mediaPackageId
   *          ID of event for which capture agent configuration will be retrieved
   * @return configurations of capture agent
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  Map<String, String> getCaptureAgentConfiguration(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Query
   */

  /**
   * Returns the number of scheduled events.
   *
   * @return the number of scheduled events
   * @throws SchedulerException
   *           if exception occurred
   */
  int getEventCount()  throws SchedulerException, UnauthorizedException;

  /**
   * Retrieves all events matching given filter.
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
   * @return a {@link MediaPackage} list of matching events
   * @throws SchedulerException
   *           if exception occurred
   */
  List<MediaPackage> search(Opt<String> captureAgentId, Opt<Date> startsFrom, Opt<Date> startsTo, Opt<Date> endFrom,
          Opt<Date> endTo) throws SchedulerException, UnauthorizedException;

  /**
   * Retrieves the currently active recording for the given capture agent (if any).
   *
   * @param captureAgentId
   *          The id of the agent to get the current recording of.
   * @return The currently active recording or none, if agent is currently idle
   * @throws SchedulerException
   *           In case the current recording cannot be retrieved.
   */
  Opt<MediaPackage> getCurrentRecording(String captureAgentId) throws SchedulerException, UnauthorizedException;

  /**
   * Retrieves the upcoming recording for the given capture agent (if any).
   *
   * @param captureAgentId
   *          The id of the agent to get the upcoming recording of.
   * @return The cupcoming recording or none, if there is none.
   * @throws SchedulerException
   *           In case the upcoming recording cannot be retrieved.
   */
  Opt<MediaPackage> getUpcomingRecording(String captureAgentId) throws SchedulerException, UnauthorizedException;

  /**
   * Returns list of all conflicting events, i.e. all events that ends after start date and begins before end date.
   *
   * @param captureDeviceID
   *          capture device ID for which conflicting events are searched for
   * @param startDate
   *          start date of of conflicting period
   * @param endDate
   *          end date of conflicting period
   * @return a {@link MediaPackage} list of all conflicting events
   * @throws SchedulerException
   *           if exception occurred
   */
  List<MediaPackage> findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
          throws UnauthorizedException, SchedulerException;

  /**
   * Returns list of all conflicting events. Conflicting periods are calculated based on recurrence rule, start date,
   * end date and duration of each conflicting period.
   *
   * @param captureAgentId
   *          capture agent ID for which conflicting events are searched for
   * @param rrule
   *          recurrence rule
   * @param startDate
   *          beginning of period
   * @param endDate
   *          ending of period
   * @param duration
   *          duration of each period
   * @param timezone
   *          the time zone of the capture agent
   * @return a {@link MediaPackage} list of all conflicting events
   * @throws SchedulerException
   *           if exception occurred
   */
  List<MediaPackage> findConflictingEvents(String captureAgentId, RRule rrule, Date startDate, Date endDate,
          long duration, TimeZone timezone) throws UnauthorizedException, SchedulerException;


  /**
   * CA
   */

  /**
   * Generates calendar for specified capture agent.
   *
   * @param captureAgentId
   *          capture agent id filter
   * @param seriesId
   *          series id filter
   * @param cutoff
   *          cutoff date filter
   * @return generated calendar
   * @throws SchedulerException
   *           if exception occurred
   */
  String getCalendar(Opt<String> captureAgentId, Opt<String> seriesId, Opt<Date> cutoff) throws SchedulerException;

  /**
   * Returns hash of last modification of event belonging to specified capture agent.
   *
   * @param captureAgentId
   *          the capture agent identifier
   * @return the last modification hash
   * @throws SchedulerException
   *           if exception occurred
   */
  String getScheduleLastModified(String captureAgentId) throws SchedulerException;

  /**
   * Updates the state of a recording with the given state, if it exists.
   *
   * @param mediaPackageId
   *          The id of the recording in the system.
   * @param state
   *          The state to set for that recording. This should be defined from {@link Recording}.
   * @throws NotFoundException
   *           if the recording with the given id has not been found
   */
  boolean updateRecordingState(String mediaPackageId, String state) throws NotFoundException, SchedulerException;

  /**
   * Gets the state of a recording, if it exists.
   *
   * @param mediaPackageId
   *          The id of the recording.
   * @return The state of the recording, or null if it does not exist. This should be defined from {@link Recording}.
   * @throws NotFoundException
   *           if the recording with the given id has not been found
   */
  Recording getRecordingState(String mediaPackageId) throws NotFoundException, SchedulerException;

  /**
   * Removes a recording from the system, if the recording exists.
   *
   * @param mediaPackageId
   *          The id of the recording to remove.
   * @throws NotFoundException
   *           if the recording with the given id has not been found
   */
  void removeRecording(String mediaPackageId) throws NotFoundException, SchedulerException;

  /**
   * Gets the state of all recordings in the system.
   *
   * @return A map of recording-state pairs.
   */
  Map<String, Recording> getKnownRecordings() throws SchedulerException;

  /**
   * Cleanup
   */

  /**
   * Remove all of the scheduled events before a buffer.
   *
   * @param buffer
   *          The number of seconds before now that defines a cutoff for events, if they have their end time before this
   *          cutoff they will be removed
   * @throws SchedulerException
   */
  void removeScheduledRecordingsBeforeBuffer(long buffer) throws UnauthorizedException, SchedulerException;

}
