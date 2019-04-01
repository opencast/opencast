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

package org.opencastproject.message.broker.api.scheduler

import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser

import com.google.gson.Gson

import org.apache.commons.lang3.StringUtils

import java.io.IOException
import java.io.Serializable
import java.io.StringReader
import java.util.Date
import java.util.HashMap
import java.util.Properties

/**
 * [Serializable] class that represents all of the possible messages sent through a SchedulerService queue.
 */
class SchedulerItem : Serializable {

    private val event: String?
    private val properties: String?
    private val acl: String?
    val agentId: String?
    private val end: Long
    private val presenters: String?
    val recordingState: String?
    private val start: Long

    val type: Type

    enum class Type {
        UpdateCatalog, UpdateProperties, UpdateAcl, UpdateAgentId, UpdateEnd, UpdatePresenters, UpdateRecordingStatus,
        UpdateStart, DeleteRecordingStatus, Delete
    }

    /**
     * Constructor to build an update event [SchedulerItem].
     *
     * @param event
     * The event details to update.
     */
    constructor(event: DublinCoreCatalog) {
        try {
            this.event = event.toXmlString()
        } catch (e: IOException) {
            throw IllegalStateException()
        }

        this.properties = null
        this.acl = null
        this.agentId = null
        this.end = -1
        this.presenters = null
        this.recordingState = null
        this.start = -1
        this.type = Type.UpdateCatalog
    }

    /**
     * Constructor to build an update properties for an event [SchedulerItem].
     *
     * @param properties
     * The properties to update.
     */
    constructor(properties: Map<String, String>) {
        this.event = null
        this.properties = serializeProperties(properties)
        this.acl = null
        this.agentId = null
        this.end = -1
        this.presenters = null
        this.recordingState = null
        this.start = -1
        this.type = Type.UpdateProperties
    }

    /**
     * Constructor to build a delete event [SchedulerItem].
     *
     */
    constructor(type: Type) {
        this.event = null
        this.properties = null
        this.acl = null
        this.agentId = null
        this.end = -1
        this.presenters = null
        this.recordingState = null
        this.start = -1
        this.type = type
    }

    /**
     * Constructor to build an update access control list event [SchedulerItem].
     *
     * @param accessControlList
     * The access control list
     */
    constructor(accessControlList: AccessControlList) {
        this.event = null
        this.properties = null
        try {
            this.acl = AccessControlParser.toJson(accessControlList)
        } catch (e: IOException) {
            throw IllegalStateException()
        }

        this.agentId = null
        this.end = -1
        this.presenters = null
        this.recordingState = null
        this.start = -1
        this.type = Type.UpdateAcl
    }

    /**
     * Constructor to build an update recording status event [SchedulerItem].
     *
     * @param state
     * the recording status
     * @param lastHeardFrom
     * the last heard from time
     */
    constructor(state: String, lastHeardFrom: Long?) {
        this.event = null
        this.properties = null
        this.acl = null
        this.agentId = null
        this.end = -1
        this.presenters = null
        this.recordingState = state
        this.start = -1
        this.type = Type.UpdateRecordingStatus
    }

    constructor(start: Date?, end: Date?, type: Type) {
        this.event = null
        this.acl = null
        this.agentId = null
        this.end = end?.time ?: -1
        this.presenters = null
        this.properties = null
        this.recordingState = null
        this.start = start?.time ?: -1
        this.type = type
    }

    constructor(agentId: String) {
        this.event = null
        this.acl = null
        this.agentId = agentId
        this.end = -1
        this.presenters = null
        this.properties = null
        this.recordingState = null
        this.start = -1
        this.type = Type.UpdateAgentId
    }

    constructor(presenters: Set<String>) {
        this.event = null
        this.acl = null
        this.agentId = null
        this.end = -1
        this.presenters = gson.toJson(presenters)
        this.properties = null
        this.recordingState = null
        this.start = -1
        this.type = Type.UpdatePresenters
    }

    fun getEvent(): DublinCoreCatalog? {
        return if (StringUtils.isBlank(event)) null else DublinCoreXmlFormat.readOpt(event).orNull()

    }

    fun getProperties(): Map<String, String> {
        try {
            return parseProperties(properties)
        } catch (e: IOException) {
            throw IllegalStateException()
        }

    }

    fun getAcl(): AccessControlList? {
        try {
            return if (acl == null) null else AccessControlParser.parseAcl(acl)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

    }

    fun getEnd(): Date? {
        return if (end < 0) null else Date(end)
    }

    fun getPresenters(): Set<String> {
        return gson.fromJson<Set<*>>(presenters, Set<*>::class.java)
    }

    fun getStart(): Date? {
        return if (start < 0) null else Date(start)
    }

    /**
     * Serializes Properties to String.
     *
     * @param caProperties
     * properties to be serialized
     * @return serialized properties
     */
    private fun serializeProperties(caProperties: Map<String, String>): String {
        val wfPropertiesString = StringBuilder()
        for ((key, value) in caProperties)
            wfPropertiesString.append("$key=$value\n")
        return wfPropertiesString.toString()
    }

    /**
     * Parses Properties represented as String.
     *
     * @param serializedProperties
     * properties to be parsed.
     * @return parsed properties
     * @throws IOException
     * if parsing fails
     */
    @Throws(IOException::class)
    private fun parseProperties(serializedProperties: String?): Map<String, String> {
        val caProperties = Properties()
        caProperties.load(StringReader(serializedProperties!!))
        return HashMap<String, String>(caProperties as Map<*, *>)
    }

    companion object {
        private const val serialVersionUID = 6061069989788904237L

        private val gson = Gson()

        val SCHEDULER_QUEUE_PREFIX = "SCHEDULER."

        val SCHEDULER_QUEUE = SCHEDULER_QUEUE_PREFIX + "QUEUE"

        /**
         * @param event
         * The event details to update to.
         * @return Builds [SchedulerItem] for updating a scheduled event.
         */
        fun updateCatalog(event: DublinCoreCatalog): SchedulerItem {
            return SchedulerItem(event)
        }

        /**
         * @param properties
         * The new properties to update to.
         * @return Builds [SchedulerItem] for updating the properties of an event.
         */
        fun updateProperties(properties: Map<String, String>): SchedulerItem {
            return SchedulerItem(properties)
        }

        /**
         * @return Builds [SchedulerItem] for deleting an event.
         */
        fun delete(): SchedulerItem {
            return SchedulerItem(Type.Delete)
        }

        /**
         * @param accessControlList
         * the access control list
         * @return Builds [SchedulerItem] for updating the access control list of an event.
         */
        fun updateAcl(accessControlList: AccessControlList): SchedulerItem {
            return SchedulerItem(accessControlList)
        }

        /**
         * @param state
         * The recording state
         * @param lastHeardFrom
         * The recording last heard from date
         * @return Builds [SchedulerItem] for updating a recording.
         */
        fun updateRecordingStatus(state: String, lastHeardFrom: Long?): SchedulerItem {
            return SchedulerItem(state, lastHeardFrom)
        }

        /**
         * @param start
         * The new start time for the event.
         * @return Builds [SchedulerItem] for updating the start of an event.
         */
        fun updateStart(start: Date): SchedulerItem {
            return SchedulerItem(start, null, Type.UpdateStart)
        }

        /**
         * @param end
         * The new end time for the event.
         * @return Builds [SchedulerItem] for updating the end of an event.
         */
        fun updateEnd(end: Date): SchedulerItem {
            return SchedulerItem(null, end, Type.UpdateEnd)
        }

        /**
         * @param presenters
         * The new set of presenters for the event.
         * @return Builds [SchedulerItem] for updating the presenters of an event.
         */
        fun updatePresenters(presenters: Set<String>): SchedulerItem {
            return SchedulerItem(presenters)
        }

        /**
         * @param agentId
         * The new agent id for the event.
         * @return Builds [SchedulerItem] for updating the agent id of an event.
         */
        fun updateAgent(agentId: String): SchedulerItem {
            return SchedulerItem(agentId)
        }

        /**
         * @return Builds [SchedulerItem] for deleting a recording.
         */
        fun deleteRecordingState(): SchedulerItem {
            return SchedulerItem(Type.DeleteRecordingStatus)
        }
    }

}
