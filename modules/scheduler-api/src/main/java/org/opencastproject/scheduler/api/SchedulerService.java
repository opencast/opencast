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

  /** The origin constant for internally modifications of scheduled events */
  String ORIGIN = "org.opencastproject";

  enum ReviewStatus {
    UNSENT, UNCONFIRMED, CONFIRMED
  }

  interface SchedulerTransaction {

    /**
     * Returns the transaction identifier
     *
     * @return the transaction identifier
     */
    String getId();

    /**
     * Returns the scheduling source
     *
     * @return the scheduling source
     */
    String getSource();

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
     *          the event start time
     * @param endDateTime
     *          the event end time
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
     * @param optOut
     *          the optional opt out status
     * @throws NotFoundException
     *           if the scheduler transaction cannot be found anymore
     * @throws UnauthorizedException
     *           if the caller is not authorized to take this action
     * @throws SchedulerException
     *           if creating new events failed
     */
    void addEvent(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
            MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
            Opt<Boolean> optOut) throws NotFoundException, UnauthorizedException, SchedulerException;

    /**
     * Commit the current scheduler transaction, writing any unflushed changes to the persistence layer.
     *
     * @throws NotFoundException
     *           if the scheduler transaction cannot be found anymore
     * @throws UnauthorizedException
     *           if the caller is not authorized to take this action
     * @throws SchedulerConflictException
     *           if there are conflicting events
     * @throws SchedulerException
     *           if committing the transaction failed
     */
    void commit() throws NotFoundException, UnauthorizedException, SchedulerConflictException, SchedulerException;

    /**
     * Roll back the current scheduler transaction.
     *
     * @throws NotFoundException
     *           if the scheduler transaction cannot be found anymore
     * @throws UnauthorizedException
     *           if the caller is not authorized to take this action
     * @throws SchedulerException
     *           if rolling back the transaction failed
     */
    void rollback() throws NotFoundException, UnauthorizedException, SchedulerException;

  }

  /**
   * Returns the scheduling transaction from the given identifier.
   *
   * @param id
   *          the transaction identifier
   * @return the scheduler transaction
   * @throws NotFoundException
   *           if scheduler transaction with specified ID cannot be found
   * @throws SchedulerException
   *           if getting scheduler transaction failed
   */
  SchedulerTransaction getTransaction(String id) throws NotFoundException, SchedulerException;

  /**
   * Returns the scheduling transaction from the given source.
   *
   * @param schedulingSource
   *          The scheduling source
   * @return the scheduler transaction
   * @throws NotFoundException
   *           if scheduler transaction with specified source cannot be found
   * @throws SchedulerException
   *           if getting scheduler transaction failed
   */
  SchedulerTransaction getTransactionBySource(String schedulingSource) throws NotFoundException, SchedulerException;

  /**
   * Returns whether the given event has an active transaction or not
   *
   * @param mediaPackageId
   *          the event identifier
   * @return whether the event has an active transaction <code>true</code> or not <code>false</code>
   */
  boolean hasActiveTransaction(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Starts a scheduling transaction on the given scheduling source.
   *
   * @param schedulingSource
   *          the scheduling source
   * @return the scheduler transaction
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   * @throws SchedulerConflictException
   *           if the transaction already exists
   * @throws SchedulerException
   *           if creating new scheduler transaction failed
   */
  SchedulerTransaction createTransaction(String schedulingSource)
          throws UnauthorizedException, SchedulerConflictException, SchedulerException;

  /**
   * Cleanup outdated transactions.
   *
   * @throws SchedulerException
   *           if cleaning up transactions failed
   */
  void cleanupTransactions() throws UnauthorizedException, SchedulerException;

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
   * @param optOut
   *          the optional opt out status
   * @param schedulingSource
   *          the optional scheduling source from which the event comes from
   * @param modificationOrigin
   *          the origin of the modifier which adds the event
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   * @throws SchedulerConflictException
   *           if there are conflicting events
   * @throws SchedulerTransactionLockException
   *           if there is a conflict with an open transaction
   * @throws SchedulerException
   *           if creating new events failed
   */
  void addEvent(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
          MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
          Opt<Boolean> optOut, Opt<String> schedulingSource, String modificationOrigin) throws UnauthorizedException,
                  SchedulerConflictException, SchedulerTransactionLockException, SchedulerException;

  /**
   * Updates event with specified ID.
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
   * @param optOut
   *          the optional opt out status to update
   * @param modificationOrigin
   *          the origin of the modifier which updates the event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SchedulerConflictException
   *           if there are conflicting events
   * @throws SchedulerTransactionLockException
   *           if there is a conflict with an open transaction
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateEvent(String mediaPackageId, Opt<Date> startDateTime, Opt<Date> endDateTime, Opt<String> captureAgentId,
          Opt<Set<String>> userIds, Opt<MediaPackage> mediaPackage, Opt<Map<String, String>> wfProperties,
          Opt<Map<String, String>> caMetadata, Opt<Opt<Boolean>> optOut, String modificationOrigin)
                  throws NotFoundException, UnauthorizedException, SchedulerConflictException,
                  SchedulerTransactionLockException, SchedulerException;

  /**
   * Removes event with specified ID.
   *
   * @param mediaPackageId
   *          the event identifier
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SchedulerTransactionLockException
   *           if there is a conflict with an open transaction
   * @throws SchedulerException
   *           if exception occurred
   */
  void removeEvent(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerTransactionLockException, SchedulerException;

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
   * Returns the opt out status of an event with the given mediapackage id
   *
   * @param mediaPackageId
   *          ID of event for which opt out status will be retrieved
   * @return the opt out status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  boolean isOptOut(String mediaPackageId) throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Updates the review status of the event with the given mediapackage ID
   *
   * @param mediaPackageId
   *          ID of event's mediapackage for which review status will be changed
   * @param reviewStatus
   *          the review status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  void updateReviewStatus(String mediaPackageId, ReviewStatus reviewStatus)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Returns the review status of an event with the given mediapackage id
   *
   * @param mediaPackageId
   *          ID of event for which review status will be retrieved
   * @return the review status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  ReviewStatus getReviewStatus(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException;

  /**
   * Query
   */

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
          Opt<Date> endTo) throws UnauthorizedException, SchedulerException;

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
   * PM
   */

  /**
   * Returns the blacklist status of an event with the given mediapackage id
   *
   * @param mediaPackageId
   *          ID of event for which blacklist status will be retrieved
   * @return the blacklist status
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  boolean isBlacklisted(String mediaPackageId) throws NotFoundException, UnauthorizedException, SchedulerException;

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
