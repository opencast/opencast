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

import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Tuple

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue

/**
 * JAXB DTO for a technical detail of a job incident. See [Incident.getDetails].
 *
 *
 * To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "detail", namespace = "http://job.opencastproject.org")
class JaxbIncidentDetail {
    @XmlAttribute(name = "title")
    private val title: String

    @XmlValue
    private val content: String

    /** Constructor for JAXB  */
    constructor() {}

    constructor(detail: Tuple<String, String>) {
        this.title = detail.a
        this.content = detail.b
    }

    fun toDetail(): Tuple<String, String> {
        return tuple(title, content)
    }

    companion object {

        val mkFn: Function<Tuple<String, String>, JaxbIncidentDetail> = object : Function<Tuple<String, String>, JaxbIncidentDetail>() {
            override fun apply(detail: Tuple<String, String>): JaxbIncidentDetail {
                return JaxbIncidentDetail(detail)
            }
        }

        val toDetailFn: Function<JaxbIncidentDetail, Tuple<String, String>> = object : Function<JaxbIncidentDetail, Tuple<String, String>>() {
            override fun apply(dto: JaxbIncidentDetail): Tuple<String, String> {
                return dto.toDetail()
            }
        }
    }
}
