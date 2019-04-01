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

import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.EqualsUtil.hash

import org.opencastproject.util.data.Tuple

import java.util.Collections
import java.util.Date

class IncidentImpl(override val id: Long,
                   override val jobId: Long,
                   override val serviceType: String,
                   override val processingHost: String,
                   timestamp: Date,
                   override val severity: Incident.Severity,
                   override val code: String,
                   details: List<Tuple<String, String>>,
                   parameters: Map<String, String>) : Incident {
    override val timestamp: Date
    override val details: List<Tuple<String, String>>
    override val descriptionParameters: Map<String, String>

    init {
        this.timestamp = Date(timestamp.time)
        this.details = Collections.unmodifiableList(details)
        this.descriptionParameters = Collections.unmodifiableMap(parameters)
    }

    override fun hashCode(): Int {
        return hash(id, jobId, serviceType, processingHost, timestamp, severity, code, details, descriptionParameters)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is Incident && eqFields((that as Incident?)!!)
    }

    private fun eqFields(that: Incident): Boolean {
        return (eq(id, that.id)
                && eq(jobId, that.jobId)
                && eq(serviceType, that.serviceType)
                && eq(processingHost, that.processingHost)
                && eq(timestamp, that.timestamp)
                && eq(severity, that.severity)
                && eq(code, that.code)
                && eq(details, that.details)
                && eq(descriptionParameters, that.descriptionParameters))
    }
}
