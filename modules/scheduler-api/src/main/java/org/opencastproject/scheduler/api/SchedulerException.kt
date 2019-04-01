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

/**
 * Thrown when a scheduled event can not be saved or loaded from persistence.
 */
open class SchedulerException : Exception {

    /**
     * Build a new scheduler exception with a message and an original cause.
     *
     * @param message
     * the error message
     * @param cause
     * the original exception causing this scheduler exception to be thrown
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {}

    /**
     * Build a new scheduler exception from the original cause.
     *
     * @param cause
     * the original exception causing this scheduler exception to be thrown
     */
    constructor(cause: Throwable) : super(cause) {}

    /**
     * Build a new scheduler exception with a message
     *
     * @param message
     * the error message
     */
    constructor(message: String) : super(message) {}

    companion object {

        /** The UID for java serialization  */
        private val serialVersionUID = 9115248123073779409L
    }
}
