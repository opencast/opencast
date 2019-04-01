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

package org.opencastproject.scheduler.remote

import java.nio.charset.StandardCharsets.UTF_8
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.apache.http.HttpStatus.SC_CONFLICT
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_OK
import org.apache.http.HttpStatus.SC_UNAUTHORIZED

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.scheduler.api.Recording
import org.opencastproject.scheduler.api.RecordingImpl
import org.opencastproject.scheduler.api.SchedulerConflictException
import org.opencastproject.scheduler.api.SchedulerException
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.scheduler.api.TechnicalMetadata
import org.opencastproject.scheduler.api.TechnicalMetadataImpl
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport

import com.entwinemedia.fn.data.Opt

import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.property.RRule

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.Map.Entry
import java.util.Properties
import java.util.TimeZone

/**
 * A proxy to a remote series service.
 */
class SchedulerServiceRemoteImpl : RemoteBase(JOB_TYPE), SchedulerService {

    /** A parser for handling JSON documents inside the body of a request.  */
    private val parser = JSONParser()

    override val eventCount: Int
        @Throws(SchedulerException::class, UnauthorizedException::class)
        get() {
            val get = HttpGet(UrlSupport.concat("eventCount"))
            val response = getResponse(get, SC_OK, SC_UNAUTHORIZED)
            try {
                if (SC_UNAUTHORIZED == response!!.statusLine.statusCode) {
                    logger.info("Unauthorized to get event count")
                    throw UnauthorizedException("Unauthorized to get event count")
                }
                val countString = EntityUtils.toString(response.entity, UTF_8)
                return Integer.parseInt(countString)
            } catch (e: UnauthorizedException) {
                throw e
            } catch (e: Exception) {
                throw SchedulerException("Unable to get event count from remote scheduler service", e)
            } finally {
                closeConnection(response)
            }
        }

    override val knownRecordings: Map<String, Recording>
        @Throws(SchedulerException::class)
        get() {
            val get = HttpGet("recordingStatus")
            val response = getResponse(get, SC_OK)
            try {
                if (response != null) {
                    if (SC_OK == response.statusLine.statusCode) {
                        val recordingStates = EntityUtils.toString(response.entity, UTF_8)
                        val recordings = parser.parse(recordingStates) as JSONArray
                        val recordingsMap = HashMap<String, Recording>()
                        for (i in recordings.indices) {
                            val recording = recordings[i] as JSONObject
                            val recordingId = recording["id"] as String
                            val status = recording["state"] as String
                            val lastHeard = recording["lastHeardFrom"] as Long
                            recordingsMap[recordingId] = RecordingImpl(recordingId, status, lastHeard)
                        }
                        logger.info("Successfully get recording states from the remote scheduler service")
                        return recordingsMap
                    }
                }
            } catch (e: Exception) {
                throw SchedulerException("Unable to get recording states from remote scheduler service", e)
            } finally {
                closeConnection(response)
            }
            throw SchedulerException("Unable to get recording states from remote scheduler service")
        }

    @Throws(UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    override fun addEvent(startDateTime: Date, endDateTime: Date, captureAgentId: String, userIds: Set<String>,
                          mediaPackage: MediaPackage, wfProperties: Map<String, String>, caMetadata: Map<String, String>,
                          schedulingSource: Opt<String>) {
        val post = HttpPost("/")
        val eventId = mediaPackage.identifier.compact()
        logger.debug("Start adding a new event {} through remote Schedule Service", eventId)

        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("start", java.lang.Long.toString(startDateTime.time)))
        params.add(BasicNameValuePair("end", java.lang.Long.toString(endDateTime.time)))
        params.add(BasicNameValuePair("agent", captureAgentId))
        params.add(BasicNameValuePair("users", StringUtils.join(userIds, ",")))
        params.add(BasicNameValuePair("mediaPackage", MediaPackageParser.getAsXml(mediaPackage)))
        params.add(BasicNameValuePair("wfproperties", toPropertyString(wfProperties)))
        params.add(BasicNameValuePair("agentparameters", toPropertyString(caMetadata)))
        if (schedulingSource.isSome)
            params.add(BasicNameValuePair("source", schedulingSource.get()))
        post.entity = UrlEncodedFormEntity(params, UTF_8)

        val response = getResponse(post, SC_CREATED, SC_UNAUTHORIZED, SC_CONFLICT)
        try {
            if (response != null && SC_CREATED == response.statusLine.statusCode) {
                logger.info("Successfully added event {} to the scheduler service", eventId)
                return
            } else if (response != null && SC_CONFLICT == response.statusLine.statusCode) {
                val errorJson = EntityUtils.toString(response.entity, UTF_8)
                val json = parser.parse(errorJson) as JSONObject
                val error = json["error"] as JSONObject
                val errorCode = error["code"] as String
                if (SchedulerConflictException.ERROR_CODE == errorCode) {
                    logger.info("Conflicting events found when adding event {}", eventId)
                    throw SchedulerConflictException("Conflicting events found when adding event $eventId")
                } else {
                    throw SchedulerException("Unexpected error code $errorCode")
                }
            } else if (response != null && SC_UNAUTHORIZED == response.statusLine.statusCode) {
                logger.info("Unauthorized to create the event")
                throw UnauthorizedException("Unauthorized to create the event")
            } else {
                throw SchedulerException("Unable to add event $eventId to the scheduler service")
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: SchedulerConflictException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to add event $eventId to the scheduler service", e)
        } finally {
            closeConnection(response)
        }
    }

    @Throws(UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    override fun addMultipleEvents(rRule: RRule, start: Date, end: Date, duration: Long?, tz: TimeZone,
                                   captureAgentId: String, userIds: Set<String>, templateMp: MediaPackage, wfProperties: Map<String, String>,
                                   caMetadata: Map<String, String>, schedulingSource: Opt<String>): Map<String, Period> {
        val post = HttpPost("/")
        logger.debug("Start adding a new events through remote Schedule Service")

        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("rrule", rRule.value))
        params.add(BasicNameValuePair("start", java.lang.Long.toString(start.time)))
        params.add(BasicNameValuePair("end", java.lang.Long.toString(end.time)))
        params.add(BasicNameValuePair("duration", java.lang.Long.toString(duration!!)))
        params.add(BasicNameValuePair("tz", tz.toZoneId().id))
        params.add(BasicNameValuePair("agent", captureAgentId))
        params.add(BasicNameValuePair("users", StringUtils.join(userIds, ",")))
        params.add(BasicNameValuePair("templateMp", MediaPackageParser.getAsXml(templateMp)))
        params.add(BasicNameValuePair("wfproperties", toPropertyString(wfProperties)))
        params.add(BasicNameValuePair("agentparameters", toPropertyString(caMetadata)))
        if (schedulingSource.isSome)
            params.add(BasicNameValuePair("source", schedulingSource.get()))
        post.entity = UrlEncodedFormEntity(params, UTF_8)

        val eventId = templateMp.identifier.compact()

        val response = getResponse(post, SC_CREATED, SC_UNAUTHORIZED, SC_CONFLICT)
        try {
            if (response != null && SC_CREATED == response.statusLine.statusCode) {
                logger.info("Successfully added events to the scheduler service")
                return null
            } else if (response != null && SC_CONFLICT == response.statusLine.statusCode) {
                val errorJson = EntityUtils.toString(response.entity, UTF_8)
                val json = parser.parse(errorJson) as JSONObject
                val error = json["error"] as JSONObject
                val errorCode = error["code"] as String
                if (SchedulerConflictException.ERROR_CODE == errorCode) {
                    logger.info("Conflicting events found when adding event based on {}", eventId)
                    throw SchedulerConflictException("Conflicting events found when adding event based on$eventId")
                } else {
                    throw SchedulerException("Unexpected error code $errorCode")
                }
            } else if (response != null && SC_UNAUTHORIZED == response.statusLine.statusCode) {
                logger.info("Unauthorized to create the event")
                throw UnauthorizedException("Unauthorized to create the event")
            } else {
                throw SchedulerException("Unable to add event $eventId to the scheduler service")
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: SchedulerConflictException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to add event $eventId to the scheduler service", e)
        } finally {
            closeConnection(response)
        }
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    override fun updateEvent(eventId: String, startDateTime: Opt<Date>, endDateTime: Opt<Date>, captureAgentId: Opt<String>,
                             userIds: Opt<Set<String>>, mediaPackage: Opt<MediaPackage>, wfProperties: Opt<Map<String, String>>,
                             caMetadata: Opt<Map<String, String>>) {

        updateEvent(eventId, startDateTime, endDateTime, captureAgentId, userIds,
                mediaPackage, wfProperties, caMetadata, false)
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerConflictException::class, SchedulerException::class)
    override fun updateEvent(eventId: String, startDateTime: Opt<Date>, endDateTime: Opt<Date>, captureAgentId: Opt<String>,
                             userIds: Opt<Set<String>>, mediaPackage: Opt<MediaPackage>, wfProperties: Opt<Map<String, String>>,
                             caMetadata: Opt<Map<String, String>>, allowConflict: Boolean) {

        logger.debug("Start updating event {}.", eventId)
        val put = HttpPut("/$eventId")

        val params = ArrayList<BasicNameValuePair>()
        if (startDateTime.isSome)
            params.add(BasicNameValuePair("start", java.lang.Long.toString(startDateTime.get().time)))
        if (endDateTime.isSome)
            params.add(BasicNameValuePair("end", java.lang.Long.toString(endDateTime.get().time)))
        if (captureAgentId.isSome)
            params.add(BasicNameValuePair("agent", captureAgentId.get()))
        if (userIds.isSome)
            params.add(BasicNameValuePair("users", StringUtils.join(userIds.get(), ",")))
        if (mediaPackage.isSome)
            params.add(BasicNameValuePair("mediaPackage", MediaPackageParser.getAsXml(mediaPackage.get())))
        if (wfProperties.isSome)
            params.add(BasicNameValuePair("wfproperties", toPropertyString(wfProperties.get())))
        if (caMetadata.isSome)
            params.add(BasicNameValuePair("agentparameters", toPropertyString(caMetadata.get())))
        params.add(BasicNameValuePair("allowConflict", BooleanUtils.toString(allowConflict, "true", "false", "false")))
        put.entity = UrlEncodedFormEntity(params, UTF_8)

        val response = getResponse(put, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED, SC_FORBIDDEN, SC_CONFLICT)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    logger.info("Event {} was not found by the scheduler service", eventId)
                    throw NotFoundException("Event '$eventId' not found on remote scheduler service!")
                } else if (SC_OK == response.statusLine.statusCode) {
                    logger.info("Event {} successfully updated with capture agent metadata.", eventId)
                    return
                } else if (response != null && SC_CONFLICT == response.statusLine.statusCode) {
                    val errorJson = EntityUtils.toString(response.entity, UTF_8)
                    val json = parser.parse(errorJson) as JSONObject
                    val error = json["error"] as JSONObject
                    val errorCode = error["code"] as String
                    if (SchedulerConflictException.ERROR_CODE == errorCode) {
                        logger.info("Conflicting events found when updating event {}", eventId)
                        throw SchedulerConflictException("Conflicting events found when updating event $eventId")
                    } else {
                        throw SchedulerException("Unexpected error code $errorCode")
                    }
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to update the event {}.", eventId)
                    throw UnauthorizedException("Unauthorized to update the event $eventId")
                } else if (SC_FORBIDDEN == response.statusLine.statusCode) {
                    logger.info("Forbidden to update the event {}.", eventId)
                    throw SchedulerException("Event with specified ID cannot be updated")
                } else {
                    throw SchedulerException("Unexpected status code " + response.statusLine)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: SchedulerConflictException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to update event $eventId to the scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to update  event $eventId")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun removeEvent(eventId: String) {
        logger.debug("Start removing event {} from scheduling service.", eventId)
        val delete = HttpDelete("/$eventId")

        val response = getResponse(delete, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED, SC_CONFLICT)
        try {
            if (response != null && SC_NOT_FOUND == response.statusLine.statusCode) {
                logger.info("Event {} was not found by the scheduler service", eventId)
                throw NotFoundException("Event '$eventId' not found on remote scheduler service!")
            } else if (response != null && SC_OK == response.statusLine.statusCode) {
                logger.info("Event {} removed from scheduling service.", eventId)
                return
            } else if (response != null && SC_UNAUTHORIZED == response.statusLine.statusCode) {
                logger.info("Unauthorized to remove the event {}.", eventId)
                throw UnauthorizedException("Unauthorized to remove the event $eventId")
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to remove event $eventId from the scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to remove  event $eventId")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun getMediaPackage(eventId: String): MediaPackage {
        val get = HttpGet("$eventId/mediapackage.xml")
        val response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Event mediapackage '$eventId' not found on remote scheduler service!")
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to get mediapacakge of the event {}.", eventId)
                    throw UnauthorizedException("Unauthorized to get mediapackage of the event $eventId")
                } else {
                    val mp = MediaPackageParser.getFromXml(EntityUtils.toString(response.entity, UTF_8))
                    logger.info("Successfully get event mediapackage {} from the remote scheduler service", eventId)
                    return mp
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to parse event media package from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get event media package from remote scheduler service")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun getDublinCore(eventId: String): DublinCoreCatalog {
        val get = HttpGet("$eventId/dublincore.xml")
        val response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Event catalog '$eventId' not found on remote scheduler service!")
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to get dublincore of the event {}.", eventId)
                    throw UnauthorizedException("Unauthorized to get dublincore of the event $eventId")
                } else {
                    val dublinCoreCatalog = DublinCores.read(response.entity.content)
                    logger.info("Successfully get event dublincore {} from the remote scheduler service", eventId)
                    return dublinCoreCatalog
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to parse event dublincore from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get event dublincore from remote scheduler service")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun getTechnicalMetadata(eventId: String): TechnicalMetadata {
        val get = HttpGet("$eventId/technical.json")
        val response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Event with id '$eventId' not found on remote scheduler service!")
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to get the technical metadata of the event {}.", eventId)
                    throw UnauthorizedException("Unauthorized to get the technical metadata of the event $eventId")
                } else {
                    val technicalMetadataJson = EntityUtils.toString(response.entity, UTF_8)
                    val json = parser.parse(technicalMetadataJson) as JSONObject
                    val recordingId = json["id"] as String
                    val start = Date(DateTimeSupport.fromUTC(json["start"] as String))
                    val end = Date(DateTimeSupport.fromUTC(json["end"] as String))
                    val location = json["location"] as String

                    val presenters = HashSet<String>()
                    val presentersArr = json["presenters"] as JSONArray
                    for (i in presentersArr.indices) {
                        presenters.add(presentersArr[i] as String)
                    }

                    val wfProperties = HashMap<String, String>()
                    val wfPropertiesObj = json["wfProperties"] as JSONObject
                    var entrySet: Set<Entry<String, String>> = wfPropertiesObj.entries
                    for ((key, value) in entrySet) {
                        wfProperties[key] = value
                    }

                    val agentConfig = HashMap<String, String>()
                    val agentConfigObj = json["agentConfig"] as JSONObject
                    entrySet = agentConfigObj.entries
                    for (entry in entrySet) {
                        agentConfig[entry.key] = entry.value
                    }

                    val status = json["state"] as String
                    val lastHeard = json["lastHeardFrom"] as String
                    var recording: Recording? = null
                    if (StringUtils.isNotBlank(status) && StringUtils.isNotBlank(lastHeard)) {
                        recording = RecordingImpl(recordingId, status, DateTimeSupport.fromUTC(lastHeard))
                    }
                    val recordingOpt = Opt.nul(recording)
                    logger.info("Successfully get the technical metadata of event '{}' from the remote scheduler service",
                            eventId)
                    return TechnicalMetadataImpl(recordingId, location, start, end, presenters, wfProperties,
                            agentConfig, recordingOpt)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to parse the technical metadata from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get the technical metadata from remote scheduler service")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun getAccessControlList(eventId: String): AccessControlList {
        val get = HttpGet("$eventId/acl")
        val response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_NO_CONTENT, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                when (response.statusLine.statusCode) {
                    SC_NOT_FOUND -> throw NotFoundException("Event '$eventId' not found on remote scheduler service!")
                    SC_NO_CONTENT -> return null
                    SC_UNAUTHORIZED -> {
                        logger.info("Unauthorized to get acl of the event {}.", eventId)
                        throw UnauthorizedException("Unauthorized to get acl of the event $eventId")
                    }
                    else -> {
                        val aclString = EntityUtils.toString(response.entity, "UTF-8")
                        val accessControlList = AccessControlParser.parseAcl(aclString)
                        logger.info("Successfully get event {} access control list from the remote scheduler service", eventId)
                        return accessControlList
                    }
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get event access control list from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get event access control list from remote scheduler service")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun getWorkflowConfig(eventId: String): Map<String, String> {
        val get = HttpGet("$eventId/workflow.properties")
        val response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException(
                            "Event workflow configuration '$eventId' not found on remote scheduler service!")
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to get workflow config of the event {}.", eventId)
                    throw UnauthorizedException("Unauthorized to get workflow config of the event $eventId")
                } else {
                    val properties = Properties()
                    properties.load(response.entity.content)
                    logger.info("Successfully get event workflow configuration {} from the remote scheduler service", eventId)
                    return HashMap<String, String>(properties as Map<*, *>)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to parse event workflow configuration from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get event workflow configuration from remote scheduler service")
    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun getCaptureAgentConfiguration(eventId: String): Map<String, String> {
        val get = HttpGet("$eventId/agent.properties")
        val response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException(
                            "Event capture agent configuration '$eventId' not found on remote scheduler service!")
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to get capture agent config of the event {}.", eventId)
                    throw UnauthorizedException("Unauthorized to get capture agent config of the event $eventId")
                } else {
                    val properties = Properties()
                    properties.load(response.entity.content)
                    logger.info("Successfully get event capture agent configuration {} from the remote scheduler service",
                            eventId)
                    return HashMap<String, String>(properties as Map<*, *>)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException(
                    "Unable to parse event capture agent configuration from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get event capture agent configuration from remote scheduler service")
    }

    @Throws(SchedulerException::class)
    override fun getScheduleLastModified(agentId: String): String {
        val get = HttpGet(UrlSupport.concat(agentId, "lastmodified"))
        val response = getResponse(get, SC_OK)
        try {
            if (response != null) {
                if (SC_OK == response.statusLine.statusCode) {
                    val agentHash = EntityUtils.toString(response.entity, UTF_8)
                    logger.info("Successfully get agent last modified hash of agent with id {} from the remote scheduler service",
                            agentId)
                    return agentHash
                }
            }
        } catch (e: Exception) {
            throw SchedulerException("Unable to get agent last modified hash from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get agent last modified hash from remote scheduler service")
    }

    @Throws(UnauthorizedException::class, SchedulerException::class)
    override fun search(captureAgentId: Opt<String>, startsFrom: Opt<Date>, startsTo: Opt<Date>,
                        endFrom: Opt<Date>, endTo: Opt<Date>): List<MediaPackage> {
        val queryStringParams = ArrayList<NameValuePair>()
        for (s in captureAgentId) {
            queryStringParams.add(BasicNameValuePair("agent", s))
        }
        for (d in startsFrom) {
            queryStringParams.add(BasicNameValuePair("startsfrom", java.lang.Long.toString(d.time)))
        }
        for (d in startsTo) {
            queryStringParams.add(BasicNameValuePair("startsto", java.lang.Long.toString(d.time)))
        }
        for (d in endFrom) {
            queryStringParams.add(BasicNameValuePair("endsfrom", java.lang.Long.toString(d.time)))
        }
        for (d in endTo) {
            queryStringParams.add(BasicNameValuePair("endsto", java.lang.Long.toString(d.time)))
        }
        val get = HttpGet("recordings.xml?" + URLEncodedUtils.format(queryStringParams, UTF_8))
        val response = getResponse(get, SC_OK, SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (SC_OK == response.statusLine.statusCode) {
                    val mediaPackageXml = EntityUtils.toString(response.entity, UTF_8)
                    val events = MediaPackageParser.getArrayFromXml(mediaPackageXml)
                    logger.info("Successfully get recordings from the remote scheduler service")
                    return events
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to search for events")
                    throw UnauthorizedException("Unauthorized to search for events")
                }
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get recordings from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get recordings from remote scheduler service")
    }

    @Throws(SchedulerException::class, UnauthorizedException::class)
    override fun getCurrentRecording(captureAgentId: String): Opt<MediaPackage> {
        val get = HttpGet(UrlSupport.concat("currentRecording", captureAgentId))
        val response = getResponse(get, SC_OK, SC_NO_CONTENT, SC_UNAUTHORIZED)
        try {
            if (SC_OK == response!!.statusLine.statusCode) {
                val mediaPackageXml = EntityUtils.toString(response.entity, UTF_8)
                val event = MediaPackageParser.getFromXml(mediaPackageXml)
                logger.info("Successfully get current recording of agent {} from the remote scheduler service", captureAgentId)
                return Opt.some(event)
            } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                logger.info("Unauthorized to get current recording of agent {}", captureAgentId)
                throw UnauthorizedException("Unauthorized to get current recording of agent $captureAgentId")
            } else {
                return Opt.none()
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get current recording from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
    }

    @Throws(SchedulerException::class, UnauthorizedException::class)
    override fun getUpcomingRecording(captureAgentId: String): Opt<MediaPackage> {
        val get = HttpGet(UrlSupport.concat("upcomingRecording", captureAgentId))
        val response = getResponse(get, SC_OK, SC_NO_CONTENT, SC_UNAUTHORIZED)
        try {
            if (SC_OK == response!!.statusLine.statusCode) {
                val mediaPackageXml = EntityUtils.toString(response.entity, UTF_8)
                val event = MediaPackageParser.getFromXml(mediaPackageXml)
                logger.info("Successfully get upcoming recording of agent {} from the remote scheduler service", captureAgentId)
                return Opt.some(event)
            } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                logger.info("Unauthorized to get upcoming recording of agent {}", captureAgentId)
                throw UnauthorizedException("Unauthorized to get upcoming recording of agent $captureAgentId")
            } else {
                return Opt.none()
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get upcoming recording from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
    }

    @Throws(UnauthorizedException::class, SchedulerException::class)
    override fun findConflictingEvents(captureDeviceID: String, startDate: Date, endDate: Date): List<MediaPackage> {
        val queryStringParams = ArrayList<NameValuePair>()
        queryStringParams.add(BasicNameValuePair("agent", captureDeviceID))
        queryStringParams.add(BasicNameValuePair("start", java.lang.Long.toString(startDate.time)))
        queryStringParams.add(BasicNameValuePair("end", java.lang.Long.toString(endDate.time)))
        val get = HttpGet("conflicts.xml?" + URLEncodedUtils.format(queryStringParams, UTF_8))
        val response = getResponse(get, SC_OK, SC_NO_CONTENT)
        try {
            if (response != null) {
                if (SC_OK == response.statusLine.statusCode) {
                    val mediaPackageXml = EntityUtils.toString(response.entity, UTF_8)
                    val events = MediaPackageParser.getArrayFromXml(mediaPackageXml)
                    logger.info("Successfully get conflicts from the remote scheduler service")
                    return events
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to search for conflicting events")
                    throw UnauthorizedException("Unauthorized to search for conflicting events")
                } else if (SC_NO_CONTENT == response.statusLine.statusCode) {
                    return emptyList()
                }
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get conflicts from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get conflicts from remote scheduler service")
    }

    @Throws(UnauthorizedException::class, SchedulerException::class)
    override fun findConflictingEvents(captureAgentId: String, rrule: RRule, startDate: Date, endDate: Date,
                                       duration: Long, timezone: TimeZone): List<MediaPackage> {
        val queryStringParams = ArrayList<NameValuePair>()
        queryStringParams.add(BasicNameValuePair("agent", captureAgentId))
        queryStringParams.add(BasicNameValuePair("rrule", rrule.recur.toString()))
        queryStringParams.add(BasicNameValuePair("start", java.lang.Long.toString(startDate.time)))
        queryStringParams.add(BasicNameValuePair("end", java.lang.Long.toString(endDate.time)))
        queryStringParams.add(BasicNameValuePair("duration", java.lang.Long.toString(duration)))
        queryStringParams.add(BasicNameValuePair("timezone", timezone.id))
        val get = HttpGet("conflicts.xml?" + URLEncodedUtils.format(queryStringParams, UTF_8))
        val response = getResponse(get, SC_OK, SC_NO_CONTENT)
        try {
            if (response != null) {
                if (SC_OK == response.statusLine.statusCode) {
                    val mediaPackageXml = EntityUtils.toString(response.entity, UTF_8)
                    val events = MediaPackageParser.getArrayFromXml(mediaPackageXml)
                    logger.info("Successfully get conflicts from the remote scheduler service")
                    return events
                } else if (SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    logger.info("Unauthorized to search for conflicting events")
                    throw UnauthorizedException("Unauthorized to search for conflicting events")
                } else if (SC_NO_CONTENT == response.statusLine.statusCode) {
                    return emptyList()
                }
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get conflicts from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get conflicts from remote scheduler service")
    }

    @Throws(SchedulerException::class)
    override fun getCalendar(captureAgentId: Opt<String>, seriesId: Opt<String>, cutoff: Opt<Date>): String {
        val queryStringParams = ArrayList<NameValuePair>()
        for (s in captureAgentId) {
            queryStringParams.add(BasicNameValuePair("agentid", s))
        }
        for (s in seriesId) {
            queryStringParams.add(BasicNameValuePair("seriesid", s))
        }
        for (d in cutoff) {
            queryStringParams.add(BasicNameValuePair("cutoff", java.lang.Long.toString(d.time)))
        }
        val get = HttpGet("calendars?" + URLEncodedUtils.format(queryStringParams, UTF_8))
        val response = getResponse(get, SC_OK)
        try {
            if (response != null) {
                if (SC_OK == response.statusLine.statusCode) {
                    val calendar = EntityUtils.toString(response.entity, UTF_8)
                    logger.info("Successfully get calendar of agent with id {} from the remote scheduler service",
                            captureAgentId)
                    return calendar
                }
            }
        } catch (e: Exception) {
            throw SchedulerException("Unable to get calendar from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get calendar from remote scheduler service")
    }

    @Throws(UnauthorizedException::class, SchedulerException::class)
    override fun removeScheduledRecordingsBeforeBuffer(buffer: Long) {
        val post = HttpPost("/removeOldScheduledRecordings")
        logger.debug("Start removing old schedules before buffer {} through remote Schedule Service", buffer)

        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("buffer", java.lang.Long.toString(buffer)))
        post.entity = UrlEncodedFormEntity(params, UTF_8)

        val response = getResponse(post, SC_OK, SC_UNAUTHORIZED)
        try {
            if (response != null && SC_OK == response.statusLine.statusCode) {
                logger.info("Successfully started removing old schedules before butter {} to the scheduler service", buffer)
                return
            } else if (SC_UNAUTHORIZED == response!!.statusLine.statusCode) {
                logger.info("Unauthorized to remove old schedules before buffer {}.", buffer)
                throw UnauthorizedException("Unauthorized to remove old schedules")
            }
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to remove old schedules from the scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to remove old schedules from the scheduler service")
    }

    @Throws(NotFoundException::class, SchedulerException::class)
    override fun updateRecordingState(mediapackageId: String, state: String): Boolean {
        val put = HttpPut(UrlSupport.concat(mediapackageId, "recordingStatus"))

        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("state", state))
        put.entity = UrlEncodedFormEntity(params, UTF_8)

        val response = getResponse(put, SC_OK, SC_NOT_FOUND, SC_BAD_REQUEST)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    logger.warn("Event with mediapackage id {} was not found by the scheduler service", mediapackageId)
                    throw NotFoundException(
                            "Event with mediapackage id '$mediapackageId' not found on remote scheduler service!")
                } else if (SC_BAD_REQUEST == response.statusLine.statusCode) {
                    logger.info("Unable to update event with mediapackage id {}, invalid recording state: {}.", mediapackageId,
                            state)
                    return false
                } else if (SC_OK == response.statusLine.statusCode) {
                    logger.info("Event with mediapackage id {} successfully updated with recording status.", mediapackageId)
                    return true
                } else {
                    throw SchedulerException("Unexpected status code " + response.statusLine)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to update recording state of event with mediapackage id " + mediapackageId
                    + " to the scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to update recording state of event with mediapackage id $mediapackageId")
    }

    @Throws(NotFoundException::class, SchedulerException::class)
    override fun getRecordingState(id: String): Recording {
        val get = HttpGet(UrlSupport.concat(id, "recordingStatus"))
        val response = getResponse(get, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_OK == response.statusLine.statusCode) {
                    val recordingStateJson = EntityUtils.toString(response.entity, UTF_8)
                    val json = parser.parse(recordingStateJson) as JSONObject
                    val recordingId = json["id"] as String
                    val status = json["state"] as String
                    val lastHeard = json["lastHeardFrom"] as Long
                    logger.info("Successfully get calendar of agent with id {} from the remote scheduler service", id)
                    return RecordingImpl(recordingId, status, lastHeard)
                } else if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    logger.warn("Event with mediapackage id {} was not found by the scheduler service", id)
                    throw NotFoundException("Event with mediapackage id '$id' not found on remote scheduler service!")
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException("Unable to get calendar from remote scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to get calendar from remote scheduler service")
    }

    @Throws(NotFoundException::class, SchedulerException::class)
    override fun removeRecording(eventId: String) {
        val delete = HttpDelete(UrlSupport.concat(eventId, "recordingStatus"))

        val response = getResponse(delete, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null && SC_NOT_FOUND == response.statusLine.statusCode) {
                logger.info("Event {} was not found by the scheduler service", eventId)
                throw NotFoundException("Event '$eventId' not found on remote scheduler service!")
            } else if (response != null && SC_OK == response.statusLine.statusCode) {
                logger.info("Recording status of event {} removed from scheduling service.", eventId)
                return
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw SchedulerException(
                    "Unable to remove recording status of event $eventId from the scheduler service", e)
        } finally {
            closeConnection(response)
        }
        throw SchedulerException("Unable to remove  recording status of event $eventId")
    }

    private fun toPropertyString(properties: Map<String, String>): String {
        val wfPropertiesString = StringBuilder()
        for ((key, value) in properties)
            wfPropertiesString.append("$key=$value\n")
        return wfPropertiesString.toString()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SchedulerServiceRemoteImpl::class.java)
    }

}
