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
 * Represent a user in Opencast
 */
interface User {

    /**
     * Gets this user's unique account name.
     *
     * @return the account name
     */
    val username: String

    /**
     * Gets this user's password, if available.
     *
     * @return the password
     */
    val password: String

    /**
     * Gets the user's name.
     *
     * @return the user name
     */
    val name: String

    /**
     * Gets the user's email address.
     *
     * @return the user's email address
     */
    val email: String

    /**
     * Gets the provider where the user is coming from.
     *
     * @return the provider where the user is coming from.
     */
    val provider: String

    /**
     * Returns `true` if this user object can be managed by Opencast.
     *
     * @return `true` if this user is manageable
     */
    val isManageable: Boolean

    /**
     * Returns the user's organization identifier.
     *
     * @return the organization
     */
    val organization: Organization

    /**
     * Gets the user's roles. For anonymous users, this will return Anonymous.
     *
     * @return the user's roles
     */
    val roles: Set<Role>

    /**
     * Returns `true` if this user object can be used to log into Opencast.
     *
     * @return `true` if this user can login
     */
    fun canLogin(): Boolean

    /**
     * Returns whether the user is in a specific role.
     *
     * @param role
     * the role to check
     * @return whether the role is one of this user's roles
     */
    fun hasRole(role: String): Boolean

}
