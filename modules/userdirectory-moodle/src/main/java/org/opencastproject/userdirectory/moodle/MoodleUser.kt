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

package org.opencastproject.userdirectory.moodle

import java.util.Objects

class MoodleUser {
    var id: String? = null
    var username: String? = null
    var fullname: String? = null
    var idnumber: String? = null
    var email: String? = null
    var auth: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o)
            return true
        if (o == null || javaClass != o.javaClass)
            return false
        val that = o as MoodleUser?
        return id == that!!.id && username == that.username && fullname == that.fullname && idnumber == that.idnumber && email == that.email && auth == that.auth
    }

    override fun hashCode(): Int {
        return Objects.hash(id, username, fullname, idnumber, email, auth)
    }

    override fun toString(): String {
        return ("MoodleUser{" + "id=" + id + ", username='" + username + '\''.toString() + ", fullname='" + fullname + '\''.toString()
                + ", idnumber='" + idnumber + '\''.toString() + ", email='" + email + '\''.toString() + ", auth='" + auth + '\''.toString() + '}'.toString())
    }
}
