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

package org.opencastproject.message.broker.api

import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationParser
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserParser

import com.entwinemedia.fn.data.Opt

import java.io.Serializable

class BaseMessage(organization: Organization, user: User, val `object`: Serializable) : Serializable {

    private val organization: String
    private val user: String

    val id: Opt<String>
        get() = if (`object` is MessageItem) Opt.some(`object`.id) else Opt.none()

    init {
        this.organization = OrganizationParser.toXml(JaxbOrganization.fromOrganization(organization))
        this.user = UserParser.toXml(JaxbUser.fromUser(user))
    }

    fun getOrganization(): Organization {
        return OrganizationParser.fromXml(organization)
    }

    fun getUser(): User {
        return UserParser.fromXml(user)
    }

    companion object {

        private const val serialVersionUID = 3895355230339323251L
    }

}
