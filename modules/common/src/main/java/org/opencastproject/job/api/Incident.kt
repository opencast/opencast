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

import org.opencastproject.util.data.Tuple

import java.util.Date

/** Describes an incident relating to a [Job].  */
interface Incident {

    /** Return the incident id.  */
    val id: Long

    /**
     * The job related to this incident.
     *
     * @return the job id
     */
    val jobId: Long

    /**
     * The service type on which the incident was occurring.
     *
     * @return the service type
     */
    val serviceType: String

    /**
     * The processing host running the job where the incident was occurring.
     *
     * @return the processing host
     */
    val processingHost: String

    /**
     * The date where the incident was happening.
     *
     * @return the date
     */
    val timestamp: Date

    /**
     * The severity of this incident.
     *
     * @return the severity
     */
    val severity: Severity

    /**
     * The unique code of this incident. Incident codes may be mapped to plain text, possibly localized.
     * It is recommended to create codes after the schema `service_type.number`,
     * e.g. `org.opencastproject.service.1511`
     *
     * @return the incident code
     * @see org.opencastproject.job.api.Job.getJobType
     */
    val code: String

    /**
     * List of additional technical information having a name and a text `[(name, text)]`.
     * This may be an exception, an ffmpeg commandline, memory statistics, etc.
     *
     * @return a list of technical background information describing the incident in depth
     * [(detail_name, detail)]
     */
    val details: List<Tuple<String, String>>

    /**
     * Named parameters describing the incident in more detail. These parameters may be used to
     * construct a description message.
     *
     * @return the message parameters; parameter_name -&gt; parameter_value
     */
    val descriptionParameters: Map<String, String>

    enum class Severity {
        INFO,
        WARNING,

        /**
         * An incident of type FAILURE shall only be recorded when a job fails, i.e. enters the
         * [org.opencastproject.job.api.Job.Status.FAILED] state. That implies that there
         * can be at most one FAILURE incident per job.
         */
        FAILURE
    }
}
