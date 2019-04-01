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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.scheduler.api

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.util.NotFoundException

import com.entwinemedia.fn.data.Opt

import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.property.RRule

import java.util.Date
import java.util.TimeZone

/**
 * Scheduler service manages events (creates new, updates already existing and removes events). It enables searches over
 * existing events, retrieving event data like dublincore, acl or workflow configuration for specific event, search for
 * conflicting events and generating calendar for capture agent.
 */
interface SchedulerService {

    /**
     * Query
     */

    /**
     * Returns the number of scheduled events.
     *
     * @return the number of scheduled events
     * @throws SchedulerException
     * if exception occurred
     */
    val eventCount: Int

    /**
     * Gets the state of all recordings in the system.
     *
     * @return A map of recording-state pairs.
     */
    val knownRecordings: Map<String, Recording>

    /**
     * Creates new event using specified mediapackage, workflow configuration and capture agent configuration. The
     * mediapackage id is used as the event's identifier.
     *
     * Default capture agent properties are created from agentId and DublinCore. Following values are generated:
     *
     *  * event.title (mapped from dc:title)
     *  * event.series (mapped from mediaPackage#getSeries())
     *  * event.location (mapped from captureAgentId)
     *
     *
     * @param startDateTime
     * the event start time (the start date must be before the end date)
     * @param endDateTime
     * the event end time (the end date must be after the start date)
     * @param captureAgentId
     * the capture agent id
     * @param userIds
     * the list of user identifiers of speakers/lecturers
     * @param mediaPackage
     * the mediapackage
     * @param wfProperties
     * the workflow configuration
     * @param caMetadata
     * the capture agent configuration
     * @param schedulingSource
     * the optional scheduling source from which the event comes from
     * @throws UnauthorizedException
     * if the caller is not authorized to take this action
     * @throws SchedulerConflictException
     * if there are conflicting events
     * @throws SchedulerException
     * if creating new events failed
     */
    @Throws(UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    fun addEvent(startDateTime: Date, endDateTime: Date, captureAgentId: String, userIds: Set<String>,
                 mediaPackage: MediaPackage, wfProperties: Map<String, String>, caMetadata: Map<String, String>,
                 schedulingSource: Opt<String>)

    /**
     * Creates a group of new event using specified mediapackage, workflow configuration and capture agent configuration.
     * The mediapackage id is used as the event's identifier.
     *
     * Default capture agent properties are created from agentId and DublinCore. Following values are generated:
     *
     *  * event.title (mapped from dc:title)
     *  * event.series (mapped from mediaPackage#getSeries())
     *  * event.location (mapped from captureAgentId)
     *
     *
     * @param rRule
     * the [RRule] for the events to schedule
     * @param start
     * the start date for the recurrence
     * @param end
     * the end date for the recurrence
     * @param duration
     * the duration of the events
     * @param tz
     * the [TimeZone] for the events
     * @param captureAgentId
     * the capture agent id
     * @param userIds
     * the list of user identifiers of speakers/lecturers
     * @param templateMp
     * the mediapackage to base the events on
     * @param wfProperties
     * the workflow configuration
     * @param caMetadata
     * the capture agent configuration
     * @param schedulingSource
     * the optional scheduling source from which the event comes from
     * @return A [Map] of mediapackage ID and [Period] where the event occurs
     * @throws UnauthorizedException
     * if the caller is not authorized to take this action
     * @throws SchedulerConflictException
     * if there are conflicting events
     * @throws SchedulerException
     * if creating new events failed
     */
    @Throws(UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    fun addMultipleEvents(rRule: RRule, start: Date, end: Date, duration: Long?, tz: TimeZone,
                          captureAgentId: String, userIds: Set<String>, templateMp: MediaPackage, wfProperties: Map<String, String>, caMetadata: Map<String, String>, schedulingSource: Opt<String>): Map<String, Period>

    /**
     * Updates event with specified ID and check for conflicts.
     *
     * Default capture agent properties are created from DublinCore. Following values are generated:
     *
     *  * event.title (mapped from dc:title)
     *  * event.series (mapped from mediaPackage#getSeries())
     *  * event.location (mapped from captureAgentId)
     *
     *
     * @param mediaPackageId
     * the optional event identifier
     * @param startDateTime
     * the optional event start time
     * @param endDateTime
     * the optional event end time
     * @param captureAgentId
     * the optional capture agent id
     * @param userIds
     * the optional list of user identifiers of speakers/lecturers
     * @param mediaPackage
     * the optional mediapackage to update
     * @param wfProperties
     * the optional workflow configuration to update
     * @param caMetadata
     * the optional capture configuration to update
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SchedulerConflictException
     * if there are conflicting events
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    fun updateEvent(mediaPackageId: String, startDateTime: Opt<Date>, endDateTime: Opt<Date>, captureAgentId: Opt<String>,
                    userIds: Opt<Set<String>>, mediaPackage: Opt<MediaPackage>, wfProperties: Opt<Map<String, String>>,
                    caMetadata: Opt<Map<String, String>>)

    /**
     * Updates event with specified ID and possibly checking for conflicts.
     *
     * Default capture agent properties are created from DublinCore. Following values are generated:
     *
     *  * event.title (mapped from dc:title)
     *  * event.series (mapped from mediaPackage#getSeries())
     *  * event.location (mapped from captureAgentId)
     *
     *
     * @param mediaPackageId
     * the event identifier
     * @param startDateTime
     * the optional event start time
     * @param endDateTime
     * the optional event end time
     * @param captureAgentId
     * the optional capture agent id
     * @param userIds
     * the optional list of user identifiers of speakers/lecturers
     * @param mediaPackage
     * the optional mediapackage to update
     * @param wfProperties
     * the optional workflow configuration to update
     * @param caMetadata
     * the optional capture configuration to update
     * @param allowConflict
     * the flag to ignore conflict checks
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SchedulerConflictException
     * if there are conflicting events
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    fun updateEvent(mediaPackageId: String, startDateTime: Opt<Date>, endDateTime: Opt<Date>, captureAgentId: Opt<String>,
                    userIds: Opt<Set<String>>, mediaPackage: Opt<MediaPackage>, wfProperties: Opt<Map<String, String>>,
                    caMetadata: Opt<Map<String, String>>, allowConflict: Boolean)

    /**
     * Removes event with specified ID.
     *
     * @param mediaPackageId
     * the event identifier
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun removeEvent(mediaPackageId: String)

    /**
     * Retrieves mediapackage associated with specified event ID.
     *
     * @param mediaPackageId
     * ID of event for which mediapackage will be retrieved
     * @return [MediaPackage] for specified event
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun getMediaPackage(mediaPackageId: String): MediaPackage

    /**
     * Retrieves dublin core catalog associated with specified event ID.
     *
     * @param mediaPackageId
     * ID of event for which DublinCore will be retrieved
     * @return [DublinCoreCatalog] for specified event
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun getDublinCore(mediaPackageId: String): DublinCoreCatalog

    /**
     * Retrieves the technical metadata associated with specified event ID.
     *
     * @param mediaPackageId
     * ID of event for which technical metadata will be retrieved
     * @return [TechnicalMetadata] for specified event
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun getTechnicalMetadata(mediaPackageId: String): TechnicalMetadata

    /**
     * Retrieves access control list associated with specified event ID.
     *
     * @param mediaPackageId
     * ID of event for which access control list will be retrieved
     * @return [AccessControlList] for specified event
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun getAccessControlList(mediaPackageId: String): AccessControlList

    /**
     * Retrieves workflow configuration associated with specified event ID.
     *
     * @param mediaPackageId
     * ID of event for which workflow configuration will be retrieved
     * @return configuration of the workflow
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun getWorkflowConfig(mediaPackageId: String): Map<String, String>

    /**
     * Retrieves capture agent configuration for specified event.
     *
     * @param mediaPackageId
     * ID of event for which capture agent configuration will be retrieved
     * @return configurations of capture agent
     * @throws NotFoundException
     * if event with specified ID cannot be found
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun getCaptureAgentConfiguration(mediaPackageId: String): Map<String, String>

    /**
     * Retrieves all events matching given filter.
     *
     * @param captureAgentId
     * the capture agent id filter
     * @param startsFrom
     * the start from date filter
     * @param startsTo
     * the start to date filter
     * @param endFrom
     * the end from date filter
     * @param endTo
     * the end to date filter
     * @return a [MediaPackage] list of matching events
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(SchedulerException::class, UnauthorizedException::class)
    fun search(captureAgentId: Opt<String>, startsFrom: Opt<Date>, startsTo: Opt<Date>, endFrom: Opt<Date>,
               endTo: Opt<Date>): List<MediaPackage>

    /**
     * Retrieves the currently active recording for the given capture agent (if any).
     *
     * @param captureAgentId
     * The id of the agent to get the current recording of.
     * @return The currently active recording or none, if agent is currently idle
     * @throws SchedulerException
     * In case the current recording cannot be retrieved.
     */
    @Throws(SchedulerException::class, UnauthorizedException::class)
    fun getCurrentRecording(captureAgentId: String): Opt<MediaPackage>

    /**
     * Retrieves the upcoming recording for the given capture agent (if any).
     *
     * @param captureAgentId
     * The id of the agent to get the upcoming recording of.
     * @return The cupcoming recording or none, if there is none.
     * @throws SchedulerException
     * In case the upcoming recording cannot be retrieved.
     */
    @Throws(SchedulerException::class, UnauthorizedException::class)
    fun getUpcomingRecording(captureAgentId: String): Opt<MediaPackage>

    /**
     * Returns list of all conflicting events, i.e. all events that ends after start date and begins before end date.
     *
     * @param captureDeviceID
     * capture device ID for which conflicting events are searched for
     * @param startDate
     * start date of of conflicting period
     * @param endDate
     * end date of conflicting period
     * @return a [MediaPackage] list of all conflicting events
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(UnauthorizedException::class, SchedulerException::class)
    fun findConflictingEvents(captureDeviceID: String, startDate: Date, endDate: Date): List<MediaPackage>

    /**
     * Returns list of all conflicting events. Conflicting periods are calculated based on recurrence rule, start date,
     * end date and duration of each conflicting period.
     *
     * @param captureAgentId
     * capture agent ID for which conflicting events are searched for
     * @param rrule
     * recurrence rule
     * @param startDate
     * beginning of period
     * @param endDate
     * ending of period
     * @param duration
     * duration of each period
     * @param timezone
     * the time zone of the capture agent
     * @return a [MediaPackage] list of all conflicting events
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(UnauthorizedException::class, SchedulerException::class)
    fun findConflictingEvents(captureAgentId: String, rrule: RRule, startDate: Date, endDate: Date,
                              duration: Long, timezone: TimeZone): List<MediaPackage>


    /**
     * CA
     */

    /**
     * Generates calendar for specified capture agent.
     *
     * @param captureAgentId
     * capture agent id filter
     * @param seriesId
     * series id filter
     * @param cutoff
     * cutoff date filter
     * @return generated calendar
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(SchedulerException::class)
    fun getCalendar(captureAgentId: Opt<String>, seriesId: Opt<String>, cutoff: Opt<Date>): String

    /**
     * Returns hash of last modification of event belonging to specified capture agent.
     *
     * @param captureAgentId
     * the capture agent identifier
     * @return the last modification hash
     * @throws SchedulerException
     * if exception occurred
     */
    @Throws(SchedulerException::class)
    fun getScheduleLastModified(captureAgentId: String): String

    /**
     * Updates the state of a recording with the given state, if it exists.
     *
     * @param mediaPackageId
     * The id of the recording in the system.
     * @param state
     * The state to set for that recording. This should be defined from [Recording].
     * @throws NotFoundException
     * if the recording with the given id has not been found
     */
    @Throws(NotFoundException::class, SchedulerException::class)
    fun updateRecordingState(mediaPackageId: String, state: String): Boolean

    /**
     * Gets the state of a recording, if it exists.
     *
     * @param mediaPackageId
     * The id of the recording.
     * @return The state of the recording, or null if it does not exist. This should be defined from [Recording].
     * @throws NotFoundException
     * if the recording with the given id has not been found
     */
    @Throws(NotFoundException::class, SchedulerException::class)
    fun getRecordingState(mediaPackageId: String): Recording

    /**
     * Removes a recording from the system, if the recording exists.
     *
     * @param mediaPackageId
     * The id of the recording to remove.
     * @throws NotFoundException
     * if the recording with the given id has not been found
     */
    @Throws(NotFoundException::class, SchedulerException::class)
    fun removeRecording(mediaPackageId: String)

    /**
     * Cleanup
     */

    /**
     * Remove all of the scheduled events before a buffer.
     *
     * @param buffer
     * The number of seconds before now that defines a cutoff for events, if they have their end time before this
     * cutoff they will be removed
     * @throws SchedulerException
     */
    @Throws(UnauthorizedException::class, SchedulerException::class)
    fun removeScheduledRecordingsBeforeBuffer(buffer: Long)

    companion object {

        /**
         * Identifier for service registration and location
         */
        val JOB_TYPE = "org.opencastproject.scheduler"
    }

}
