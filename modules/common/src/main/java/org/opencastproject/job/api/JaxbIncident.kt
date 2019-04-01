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

package org.opencastproject.job.api

import org.opencastproject.util.data.Collections.nullToNil
import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.job.api.Incident.Severity
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Function2
import org.opencastproject.util.jaxb.UtcTimestampAdapter

import java.util.Date
import java.util.HashMap
import kotlin.collections.Map.Entry

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/** 1:1 serialization of a [Incident].  */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "incident", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incident", namespace = "http://job.opencastproject.org")
class JaxbIncident {
    @XmlElement(name = "id")
    private val id: Long

    @XmlElement(name = "jobId")
    private val jobId: Long

    @XmlElement(name = "serviceType")
    private val serviceType: String

    @XmlElement(name = "processingHost")
    private val processingHost: String

    @XmlElement(name = "timestamp")
    @XmlJavaTypeAdapter(UtcTimestampAdapter::class)
    private val timestamp: Date

    @XmlElement(name = "severity")
    private val severity: Severity

    @XmlElement(name = "code")
    private val code: String

    @XmlElementWrapper(name = "descriptionParameters")
    @XmlElement(name = "param")
    private val descriptionParameters: List<Param>

    @XmlElementWrapper(name = "details")
    @XmlElement(name = "detail")
    private val details: List<JaxbIncidentDetail>

    /** Constructor for JAXB  */
    constructor() {}

    constructor(incident: Incident) {
        this.id = incident.id
        this.jobId = incident.jobId
        this.serviceType = incident.serviceType
        this.processingHost = incident.processingHost
        this.timestamp = Date(incident.timestamp.time)
        this.severity = incident.severity
        this.code = incident.code
        this.descriptionParameters = mlist<Entry<String, String>>(incident.descriptionParameters.entries).map(Param.mkFn).value()
        this.details = mlist<Tuple<String, String>>(incident.details).map(JaxbIncidentDetail.mkFn).value()
    }

    fun toIncident(): Incident {
        return IncidentImpl(id, jobId, serviceType, processingHost, timestamp, severity, code,
                mlist(nullToNil(details)).map<Tuple<String, String>>(JaxbIncidentDetail.toDetailFn).value(), mlist(
                nullToNil(descriptionParameters)).foldl(HashMap(),
                object : Function2<Map<String, String>, Param, Map<String, String>>() {
                    override fun apply(sum: Map<String, String>, param: Param): Map<String, String> {
                        sum.put(param.name, param.value)
                        return sum
                    }
                }))
    }

    /**
     * An description parameter. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "param", namespace = "http://job.opencastproject.org")
    class Param {
        @XmlAttribute(name = "name")
        var name: String? = null
            private set

        @XmlValue
        var value: String? = null
            private set

        companion object {

            fun mk(entry: Entry<String, String>): Param {
                val dto = Param()
                dto.name = entry.key
                dto.value = entry.value
                return dto
            }

            val mkFn: Function<Entry<String, String>, Param> = object : Function<Entry<String, String>, Param>() {
                override fun apply(entry: Entry<String, String>): Param {
                    return mk(entry)
                }
            }
        }
    }

    companion object {

        val mkFn: Function<Incident, JaxbIncident> = object : Function<Incident, JaxbIncident>() {
            override fun apply(incident: Incident): JaxbIncident {
                return JaxbIncident(incident)
            }
        }

        val toIncidentFn: Function<JaxbIncident, Incident> = object : Function<JaxbIncident, Incident>() {
            override fun apply(dto: JaxbIncident): Incident {
                return dto.toIncident()
            }
        }
    }
}
