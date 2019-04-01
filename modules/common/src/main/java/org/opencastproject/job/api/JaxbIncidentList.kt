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

import org.opencastproject.serviceregistry.api.IncidentServiceException
import org.opencastproject.util.NotFoundException

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentList", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentList", namespace = "http://job.opencastproject.org")
class JaxbIncidentList {
    @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
    private val incidents: List<JaxbIncident>

    /** Default constructor needed by jaxb  */
    constructor() {}

    @Throws(IncidentServiceException::class, NotFoundException::class)
    constructor(incidents: List<Incident>) {
        this.incidents = mlist(incidents).map(JaxbIncident.mkFn).value()
    }

    fun toIncidents(): List<Incident> {
        return mlist(nullToNil(incidents)).map(JaxbIncident.toIncidentFn).value()
    }
}
