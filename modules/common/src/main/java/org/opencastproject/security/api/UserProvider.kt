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

import java.util.ArrayList

/**
 * Provides access to users and roles.
 */
interface UserProvider {

    /**
     * Gets the provider name
     *
     * @return the provider name
     */
    val name: String

    /**
     * Gets all known users.
     *
     * @return the users
     */
    val users: Iterator<User>

    /**
     * Returns the identifier for the organization that is associated with this user provider. If equal to
     * [.ALL_ORGANIZATIONS], this provider will always be consulted, regardless of the organization.
     *
     * @return the defining organization
     */
    val organization: String

    /**
     * Loads a user by username, or returns null if this user is not known to this provider.
     *
     * @param userName
     * the username
     * @return the user
     */
    fun loadUser(userName: String): User?

    /**
     * Returns the number of users in the provider
     *
     * @return the count of users in the provider
     */
    fun countUsers(): Long

    /**
     * Return the found user's as an iterator.
     *
     * @param query
     * the query. Use the wildcards "_" to match any single character and "%" to match an arbitrary number of
     * characters (including zero characters).
     * @param offset
     * the offset
     * @param limit
     * the limit. 0 means no limit
     * @return an iterator of user's
     * @throws IllegalArgumentException
     * if the query is `null`
     */
    fun findUsers(query: String, offset: Int, limit: Int): Iterator<User>

    /**
     * Find a list of users by their user names
     *
     * Note that the default implementation of this might be slow, as it calls `loadUser` on every single user.
     * @param userNames A list of user names
     * @return A list of resolved user objects
     */
    open fun findUsers(userNames: Collection<String>): Iterator<User> {
        val result = ArrayList<User>(0)
        for (name in userNames) {
            val e = loadUser(name)
            if (e != null) {
                result.add(e)
            }
        }
        return result.iterator()
    }

    /**
     * Discards any cached value for given user name.
     *
     * @param userName
     * the user name
     */
    fun invalidate(userName: String)

    companion object {

        /** The constant indicating that a provider should be consulted for all organizations  */
        val ALL_ORGANIZATIONS = "*"
    }

}
