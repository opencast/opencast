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

package org.opencastproject.util

/**
 * This exception is thrown by the [ProcessExecutor] and indicates that the process failed.
 *
 */
@Deprecated("see {@link ProcessExecutor}")
class ProcessExcecutorException
/**
 * Creates a new exception.
 *
 * @param msg
 * the error message
 * @param cause
 * the original cause
 */
(msg: String, cause: Throwable) : Exception(msg, cause) {
    companion object {

        /** Serial version uid  */
        private val serialVersionUID = -9194578448624904231L
    }

}
