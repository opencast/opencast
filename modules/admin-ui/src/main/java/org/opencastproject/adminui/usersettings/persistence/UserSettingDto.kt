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

package org.opencastproject.adminui.usersettings.persistence

import org.opencastproject.util.RequireUtil.notEmpty

import org.opencastproject.adminui.usersettings.UserSetting

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/** Entity object for user settings.  */
@Entity(name = "UserSettings")
@Table(name = "oc_user_settings", uniqueConstraints = [UniqueConstraint(columnNames = ["id"])])
@NamedQueries(NamedQuery(name = "UserSettings.countByUserName", query = "SELECT COUNT(us) FROM UserSettings us WHERE us.username = :username AND us.organization = :org"), NamedQuery(name = "UserSettings.findByIdAndUsernameAndOrg", query = "SELECT us FROM UserSettings us WHERE us.id = :id AND us.username = :username AND us.organization = :org"), NamedQuery(name = "UserSettings.findByUserName", query = "SELECT us FROM UserSettings us WHERE us.username = :username AND us.organization = :org"), NamedQuery(name = "UserSettings.findByKey", query = "SELECT us FROM UserSettings us WHERE us.key = :key AND us.username = :username AND us.organization = :org"), NamedQuery(name = "UserSettings.clear", query = "DELETE FROM UserSettings us WHERE us.organization = :org"))
class UserSettingDto {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    var id: Long = 0

    @Column(name = "setting_key", nullable = false)
    var key: String? = null

    @Column(name = "setting_value", nullable = false)
    var value: String? = null

    @Column(name = "username", nullable = false)
    var username: String? = null

    @Column(name = "organization", nullable = false)
    var organization: String? = null

    /** Default constructor  */
    constructor() {}

    /**
     * Creates a user setting
     * @param id
     * A unique id that identifies a user setting.
     * @param key
     * The key to identify which user setting this is.
     * @param value
     * THe value of the of the user setting.
     * @param username
     * The user name to identify which user this user setting is for.
     * @param organization
     * The org that the user belongs to.
     */
    constructor(id: Long, key: String, value: String, username: String, organization: String) {
        this.id = id
        this.key = notEmpty(key, "key")
        this.value = notEmpty(value, "value")
        this.username = username
        this.organization = organization
    }


    /**
     * @return the business object model of this message signature
     */
    fun toUserSetting(): UserSetting {
        return UserSetting(id, key, value)
    }
}
