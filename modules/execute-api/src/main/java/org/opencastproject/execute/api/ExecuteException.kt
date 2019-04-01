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


package org.opencastproject.execute.api


/**
 * The class for any exceptions related with the ExecuteService
 */
class ExecuteException : Exception {

    /**
     * Constructs a new execute exception without a message or a cause.
     */
    constructor() {}

    /**
     * Constructs a new execute exception with a message.
     *
     * @param message
     * the message describing the exception
     */
    constructor(message: String) : super(message) {}

    /**
     * Constructs a new execute exception with the throwable causing this exception to be thrown.
     *
     * @param cause
     * the cause of this exception
     */
    constructor(cause: Throwable) : super(cause) {}

    /**
     * Constructs a new Execute exception with a message and the throwable that caused this exception to be
     * thrown.
     *
     * @param message
     * the message describing the exception
     * @param cause
     * the cause of this exception
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {}

    companion object {

        private val serialVersionUID = 1L
    }
}
