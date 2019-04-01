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
package org.opencastproject.scheduler.api

import com.entwinemedia.fn.data.Opt

import java.util.Date
import java.util.HashMap
import java.util.HashSet

/**
 * An in-memory construct to represent the technical metadata of an scheduled event
 */
class TechnicalMetadataImpl
/**
 * Builds a representation of the technical metadata.
 *
 * @param eventId
 * the event identifier
 * @param agentId
 * the agent identifier
 * @param startDate
 * the start date
 * @param endDate
 * the end date
 * @param presenters
 * the list of presenters
 * @param workflowProperties
 * the workflow properties
 * @param agentConfig
 * the capture agent configuration
 * @param recording
 * the recording
 */
(override var eventId: String?, override var agentId: String?, override var startDate: Date?, override var endDate: Date?,
 presenters: Set<String>, workflowProperties: Map<String, String>, agentConfig: Map<String, String>,
 override var recording: Opt<Recording>?) : TechnicalMetadata {
    override var presenters: Set<String> = HashSet()
    override var workflowProperties: Map<String, String> = HashMap()
    override var captureAgentConfiguration: Map<String, String> = HashMap()

    init {
        this.presenters = presenters
        this.workflowProperties = workflowProperties
        this.captureAgentConfiguration = agentConfig
    }

}
