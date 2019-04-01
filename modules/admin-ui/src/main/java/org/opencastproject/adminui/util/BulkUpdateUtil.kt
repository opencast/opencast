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

package org.opencastproject.adminui.util

import org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_AGENT_ID_KEY
import org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_END_KEY
import org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_START_KEY

import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.index.service.api.IndexService
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter
import org.opencastproject.index.service.impl.index.event.Event
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.mediapackage.MediaPackageElements

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Arrays
import java.util.Optional


/**
 * This class holds utility functions which are related to the bulk update feature for events.
 */
object BulkUpdateUtil {

    private val parser = JSONParser()

    /**
     * Wraps the IndexService.getEvent() method to convert SearchIndexExceptions into RuntimeExceptions. Useful when
     * using Java's functional programming features.
     *
     * @param indexSvc The IndexService instance.
     * @param index The index to get the event from.
     * @param id The id of the event to get.
     * @return An optional holding the event or nothing, if not found.
     */
    fun getEvent(
            indexSvc: IndexService,
            index: AdminUISearchIndex,
            id: String): Optional<Event> {
        try {
            val event = indexSvc.getEvent(id, index).orNull()
            return Optional.ofNullable(event)
        } catch (e: SearchIndexException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Takes the given scheduling information and completes the event start and end dates as well as the duration for the
     * given event. If the weekday shall be changed, the start and end dates are adjusted accordingly.
     *
     * @param event The event to complete the scheduling information for.
     * @param scheduling The (yet incomplete) scheduling information to complete.
     * @return The completed scheduling information, adjusted for the given event.
     */
    fun addSchedulingDates(event: Event, scheduling: JSONObject): JSONObject {
        val result = deepCopy(scheduling)
        var startDate = ZonedDateTime.parse(event.recordingStartDate)
        var endDate = ZonedDateTime.parse(event.recordingEndDate)
        val oldDuration = InternalDuration.of(startDate.toInstant(), endDate.toInstant())
        val timezone = ZoneId.of(result["timezone"] as String)

        // The client only sends start time hours and/or minutes. We have to apply this to each event to get a full date.
        if (result.containsKey(SCHEDULING_START_KEY)) {
            startDate = adjustedSchedulingDate(result, SCHEDULING_START_KEY, startDate, timezone)
        }
        // The client only sends end time hours and/or minutes. We have to apply this to each event to get a full date.
        if (result.containsKey(SCHEDULING_END_KEY)) {
            endDate = adjustedSchedulingDate(result, SCHEDULING_END_KEY, endDate, timezone)
        }
        if (endDate.isBefore(startDate)) {
            endDate = endDate.plusDays(1)
        }

        // If duration is set, we have to adjust the end or start date.
        if (result.containsKey("duration")) {
            val time = result["duration"] as JSONObject
            val newDuration = InternalDuration(oldDuration)
            if (time.containsKey("hour")) {
                newDuration.hours = time["hour"] as Long
            }
            if (time.containsKey("minute")) {
                newDuration.minutes = time["minute"] as Long
            }
            if (time.containsKey("second")) {
                newDuration.seconds = time["second"] as Long
            }
            if (result.containsKey(SCHEDULING_END_KEY)) {
                startDate = endDate.minusHours(newDuration.hours)
                        .minusMinutes(newDuration.minutes)
                        .minusSeconds(newDuration.seconds)
            } else {
                endDate = startDate.plusHours(newDuration.hours)
                        .plusMinutes(newDuration.minutes)
                        .plusSeconds(newDuration.seconds)
            }
        }

        // Setting the weekday means that the event should be moved to the new weekday within the same week
        if (result.containsKey("weekday")) {
            val weekdayAbbrev = result["weekday"] as String
            if (weekdayAbbrev != null) {
                val newWeekDay = Arrays.stream(DayOfWeek.values())
                        .filter { d -> d.name.startsWith(weekdayAbbrev.toUpperCase()) }
                        .findAny()
                        .orElseThrow { IllegalArgumentException("Cannot parse weekday: $weekdayAbbrev") }
                val daysDiff = newWeekDay.value - startDate.dayOfWeek.value
                startDate = startDate.plusDays(daysDiff.toLong())
                endDate = endDate.plusDays(daysDiff.toLong())
            }
        }

        result[SCHEDULING_START_KEY] = startDate.format(DateTimeFormatter.ISO_INSTANT)
        result[SCHEDULING_END_KEY] = endDate.format(DateTimeFormatter.ISO_INSTANT)
        return result
    }

    /**
     * Creates a json object containing meta data based on the given scheduling information.
     *
     * @param scheduling The scheduling information to extract meta data from.
     * @return The meta data, consisting of location, startDate, and duration.
     */
    fun toNonTechnicalMetadataJson(scheduling: JSONObject): JSONObject {
        val fields = ArrayList<JSONObject>()
        if (scheduling.containsKey(SCHEDULING_AGENT_ID_KEY)) {
            val locationJson = JSONObject()
            locationJson["id"] = "location"
            locationJson["value"] = scheduling[SCHEDULING_AGENT_ID_KEY]
            fields.add(locationJson)
        }
        if (scheduling.containsKey(SCHEDULING_START_KEY) && scheduling.containsKey(SCHEDULING_END_KEY)) {
            val startDateJson = JSONObject()
            startDateJson["id"] = "startDate"
            val startDate = Instant.parse(scheduling[SCHEDULING_START_KEY] as String)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".000Z"
            startDateJson["value"] = startDate
            fields.add(startDateJson)

            val durationJson = JSONObject()
            durationJson["id"] = "duration"
            val start = Instant.parse(scheduling[SCHEDULING_START_KEY] as String)
            val end = Instant.parse(scheduling[SCHEDULING_END_KEY] as String)
            val duration = InternalDuration.of(start, end)
            durationJson["value"] = duration.toString()
            fields.add(durationJson)
        }

        val result = JSONObject()
        result["flavor"] = MediaPackageElements.EPISODE.toString()
        result["title"] = CommonEventCatalogUIAdapter.EPISODE_TITLE
        result["fields"] = fields
        return result
    }

    /**
     * Merges all fields of the given meta data json objects into one object.
     *
     * @param first The first meta data json object.
     * @param second The second meta data json object.
     * @return A new json meta data object, containing the field of both input objects.
     */
    fun mergeMetadataFields(first: JSONObject?, second: JSONObject?): JSONObject? {
        if (first == null) {
            return second
        }
        if (second == null) {
            return first
        }
        val result = deepCopy(first)
        val fields = result["fields"] as Collection<*>
        fields.addAll(second["fields"] as Collection<*>)
        return result
    }

    private fun deepCopy(o: JSONObject): JSONObject {
        try {
            return parser.parse(o.toJSONString()) as JSONObject
        } catch (e: ParseException) {
            throw IllegalArgumentException(e)
        }

    }

    private class InternalDuration {
        private var hours: Long = 0
        private var minutes: Long = 0
        private var seconds: Long = 0

        internal constructor() {}

        internal constructor(other: InternalDuration) {
            this.hours = other.hours
            this.minutes = other.minutes
            this.seconds = other.seconds
        }

        override fun toString(): String {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        companion object {

            fun of(start: Instant, end: Instant): InternalDuration {
                val result = InternalDuration()
                val duration = Duration.between(start, end)
                result.hours = duration.toHours()
                result.minutes = duration.minusHours(result.hours).toMinutes()
                result.seconds = duration.minusHours(result.hours).minusMinutes(result.minutes).seconds
                return result
            }
        }
    }

    private fun adjustedSchedulingDate(
            scheduling: JSONObject,
            dateKey: String,
            date: ZonedDateTime,
            timezone: ZoneId): ZonedDateTime {
        val time = scheduling[dateKey] as JSONObject
        var result = date.withZoneSameInstant(timezone)
        if (time.containsKey("hour")) {
            val hour = Math.toIntExact(time["hour"] as Long)
            result = result.withHour(hour)
        }
        if (time.containsKey("minute")) {
            val minute = Math.toIntExact(time["minute"] as Long)
            result = result.withMinute(minute)
        }
        return result.withZoneSameInstant(ZoneOffset.UTC)
    }

    /**
     * Model class for one group of update instructions
     */
    class BulkUpdateInstructionGroup
    /**
     * Create a new group from parsed JSON data
     *
     * @param eventIds Event IDs in this group
     * @param metadata Metadata for this group
     * @param scheduling Scheduling for this group
     */
    (
            /**
             * Get the list of IDs of events to apply the bulk update to.
             *
             * @return The list of IDs of the events to apply the bulk update to.
             */
            val eventIds: List<String>,
            /**
             * Get the meta data update to apply.
             *
             * @return The meta data update to apply.
             */
            val metadata: JSONObject,
            /**
             * Get the scheduling information update to apply.
             *
             * @return The scheduling information update to apply.
             */
            val scheduling: JSONObject)

    /**
     * Model class for the bulk update instructions which are sent by the UI.
     */
    class BulkUpdateInstructions
    /**
     * Create a new instance by parsing the given json String.
     *
     * @param json The json serialized version of the bulk update instructions sent by the UI.
     *
     * @throws IllegalArgumentException If the json string cannot be parsed.
     */
    @Throws(IllegalArgumentException::class)
    constructor(json: String) {

        private val groups: MutableList<BulkUpdateInstructionGroup>

        init {
            try {
                val root = parser.parse(json) as JSONArray
                groups = ArrayList(root.size)
                for (jsonGroup in root) {
                    val jsonObject = jsonGroup as JSONObject
                    val eventIds = jsonObject[KEY_EVENTS] as JSONArray
                    val metadata = jsonObject[KEY_METADATA] as JSONObject
                    val scheduling = jsonObject[KEY_SCHEDULING] as JSONObject
                    groups.add(BulkUpdateInstructionGroup(eventIds, metadata, scheduling))
                }
            } catch (e: ParseException) {
                throw IllegalArgumentException(e)
            }

        }

        fun getGroups(): List<BulkUpdateInstructionGroup> {
            return groups
        }

        companion object {
            private val KEY_EVENTS = "events"
            private val KEY_METADATA = "metadata"
            private val KEY_SCHEDULING = "scheduling"
        }
    }

}
