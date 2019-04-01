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

package org.opencastproject.adminui.usersettings

import org.opencastproject.util.Jsons
import org.opencastproject.util.Jsons.Obj
import org.opencastproject.util.Jsons.Val

import java.util.ArrayList
import java.util.HashMap

/**
 * A class used to store all of the key-value pairs that denotes the user's settings.
 */
class UserSettings {
    /** The total number of signatures for this user in the database  */
    var total = 0
    /** The maximum number of user settings and signatures to return.  */
    var limit = 0
    /** The page offset into the results.  */
    var offset = 0
    /** The user settings attached to this user  */
    private var userSettings: MutableMap<String, Collection<UserSetting>> = HashMap()

    val userSettingsMap: Map<String, Collection<UserSetting>>
        get() = userSettings

    val userSettingsCollection: Collection<UserSetting>
        get() {
            val userSettingCollection = ArrayList<UserSetting>()
            for (collection in userSettings.values) {
                userSettingCollection.addAll(collection)
            }
            return userSettingCollection
        }

    /**
     * Default constructor.
     */
    constructor() {

    }

    constructor(userSettings: MutableMap<String, Collection<UserSetting>>) {
        this.userSettings = userSettings
    }

    fun setUserSettings(userSettings: MutableMap<String, Collection<UserSetting>>) {
        this.userSettings = userSettings
    }

    fun addUserSetting(userSetting: UserSetting) {
        var collection: MutableCollection<UserSetting>? = userSettings[userSetting.key]
        if (collection == null) {
            collection = ArrayList()
        }
        collection.add(userSetting)
        userSettings[userSetting.key] = collection
    }

    /**
     * @return The JSON representation of these user settings.
     */
    fun toJson(): Obj {
        val settingsArr = ArrayList<Val>()
        for (userSetting in userSettingsCollection) {
            settingsArr.add(userSetting.toJson())
        }
        return Jsons.obj(Jsons.p("offset", offset), Jsons.p("limit", limit), Jsons.p("total", total), Jsons.p("results", Jsons.arr(settingsArr)))
    }
}
