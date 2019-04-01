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
 * Provides access to the current user's username and roles, if any.
 */
interface SecurityService {

    /**
     * Gets the current user, or the local organization's anonymous user if the user has not been authenticated.
     *
     * @return the user
     * @throws IllegalStateException
     * if no organization is set in the security context
     */
    /**
     * Sets the current thread's user context to another user. This is useful when spawning new threads that must contain
     * the parent thread's user context.
     *
     * @param user
     * the user to set for the current user context
     */
    var user: User

    /**
     * Gets the organization associated with the current thread context.
     *
     * @return the organization
     */
    /**
     * Sets the organization for the calling thread.
     *
     * @param organization
     * the organization
     */
    var organization: Organization

    /**
     * Gets the current user's IP or null if unable to determine the User's IP.
     *
     * @return The current user's IP.
     */
    /**
     * Sets the current thread's user's IP address.
     *
     * @param userIP
     * The IP address of the user.
     */
    var userIP: String

}
