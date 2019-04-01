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

package org.opencastproject.animate.api

import org.opencastproject.job.api.Job

import java.net.URI

/**
 * Generate animated video sequences.
 */
interface AnimateService {

    /**
     * Generate animated video clip based on an Synfig animation file and a set of metadata.
     *
     * @param animation
     * Location of the animation file to use
     * @param metadata
     * Map of metadata used for replacements
     * @param arguments
     * List of Synfig command line arguments
     * @return Animate service job.
     * @throws AnimateServiceException
     * If something went wrong during the animation
     */
    @Throws(AnimateServiceException::class)
    fun animate(animation: URI, metadata: Map<String, String>, arguments: List<String>): Job

    companion object {

        /**
         * The namespace distinguishing animation jobs from other types
         */
        val JOB_TYPE = "org.opencastproject.animate"
    }
}
