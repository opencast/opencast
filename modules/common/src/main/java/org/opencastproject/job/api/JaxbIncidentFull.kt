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

import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.serviceregistry.api.IncidentL10n
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Tuple

import java.util.Date
import java.util.Locale

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentFull", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentFull", namespace = "http://job.opencastproject.org")
class JaxbIncidentFull {
    @XmlElement(name = "id")
    private val id: Long

    @XmlElement(name = "jobid")
    private val jobId: Long

    @XmlElement(name = "title")
    private val title: String

    @XmlElement(name = "description")
    private val description: String

    @XmlElement(name = "serviceType")
    private val serviceType: String

    @XmlElement(name = "processingHost")
    private val processingHost: String

    @XmlElement(name = "date")
    private val date: Date

    @XmlElement(name = "severity")
    private val severity: String

    @XmlElement(name = "code")
    private val code: String

    @XmlElement(name = "detail")
    @XmlElementWrapper(name = "details")
    private val details: List<JaxbIncidentDetail>

    constructor() {}

    constructor(incident: Incident, l10n: IncidentL10n) {
        this.id = incident.id
        this.jobId = incident.jobId
        this.serviceType = incident.serviceType
        this.title = l10n.title
        this.processingHost = incident.processingHost
        this.date = incident.timestamp
        this.severity = incident.severity.name
        this.code = incident.code
        this.details = mlist<Tuple<String, String>>(incident.details).map(JaxbIncidentDetail.mkFn).value()
        this.description = l10n.description
    }

    companion object {

        fun mkFn(svc: IncidentService, locale: Locale): Function<Incident, JaxbIncidentFull> {
            return object : Function.X<Incident, JaxbIncidentFull>() {
                @Throws(Exception::class)
                public override fun xapply(incident: Incident): JaxbIncidentFull {
                    return JaxbIncidentFull(incident, svc.getLocalization(incident.id, locale))
                }
            }
        }
    }
}
