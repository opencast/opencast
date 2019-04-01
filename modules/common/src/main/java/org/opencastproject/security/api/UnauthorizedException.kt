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

package org.opencastproject.security.api

/**
 * An exception that indicates that a subject attempted to take an action for which it was not authorized.
 */
class UnauthorizedException : Exception {

    /**
     * Constructs an UnauthorizedException using the specified message.
     *
     * @param message
     * the message describing the reason for this exception
     */
    constructor(message: String) : super(message) {}

    /**
     * Constructs an UnauthorizedException for the specified user's attempt to take a specified action.
     *
     * @param user
     * the current user
     * @param action
     * the action attempted
     */
    constructor(user: User, action: String) : super(formatMessage(user, action, null)) {}

    /**
     * Constructs an UnauthorizedException for the specified user's attempt to take a specified action.
     *
     * @param user
     * the current user
     * @param action
     * the action attempted
     * @param acl
     * the access control list that prevented the action
     */
    constructor(user: User, action: String, acl: AccessControlList) : super(formatMessage(user, action, acl)) {}

    companion object {

        /** The java.io.serialization uid  */
        private val serialVersionUID = 7717178990322180202L

        private fun formatMessage(user: User, action: String, acl: AccessControlList?): String {
            val sb = StringBuilder()
            sb.append(user.toString())
            sb.append(" can not take action ")
            sb.append("'")
            sb.append(action)
            sb.append("'")
            if (acl != null) {
                sb.append(" according to ")
                sb.append(acl)
            }
            return sb.toString()
        }
    }
}
