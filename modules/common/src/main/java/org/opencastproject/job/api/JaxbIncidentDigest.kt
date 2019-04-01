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

import org.opencastproject.serviceregistry.api.IncidentL10n
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.util.data.Function

import java.util.Date
import java.util.Locale

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentDigest", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentDigest", namespace = "http://job.opencastproject.org")
class JaxbIncidentDigest {
    @XmlElement(name = "id")
    private val id: Long

    @XmlElement(name = "jobid")
    private val jobId: Long

    @XmlElement(name = "title")
    private val title: String

    @XmlElement(name = "description")
    private val description: String

    @XmlElement(name = "date")
    private val date: Date

    @XmlElement(name = "severity")
    private val severity: String

    /** Constructor for JAXB  */
    constructor() {}

    constructor(incident: Incident, l10n: IncidentL10n) {
        this.id = incident.id
        this.jobId = incident.jobId
        this.title = l10n.title
        this.date = incident.timestamp
        this.severity = incident.severity.name
        this.description = l10n.description
    }

    companion object {

        fun mkFn(svc: IncidentService, locale: Locale): Function<Incident, JaxbIncidentDigest> {
            return object : Function.X<Incident, JaxbIncidentDigest>() {
                @Throws(Exception::class)
                public override fun xapply(incident: Incident): JaxbIncidentDigest {
                    return JaxbIncidentDigest(incident, svc.getLocalization(incident.id, locale))
                }
            }
        }
    }
}
