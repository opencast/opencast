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

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A wrapper for user collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "users", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "users", namespace = "http://org.opencastproject.security")
class JaxbUserList {

    /** A list of users.  */
    @XmlElement(name = "user")
    protected var users: MutableList<JaxbUser> = ArrayList()

    constructor() {}

    constructor(user: JaxbUser) {
        users.add(user)
    }

    constructor(users: Collection<JaxbUser>) {
        for (user in users)
            this.users.add(user)
    }

    /**
     * @return the users
     */
    fun getUsers(): List<JaxbUser> {
        return users
    }

    /**
     * @param users
     * the users to set
     */
    fun setUsers(users: MutableList<JaxbUser>) {
        this.users = users
    }

    fun add(user: User) {
        if (user is JaxbUser) {
            users.add(user)
        } else {
            users.add(JaxbUser.fromUser(user))
        }
    }

}
