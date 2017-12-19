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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.usersettings;

import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class used to store all of the key-value pairs that denotes the user's settings.
 */
public class UserSettings {
  /** The total number of signatures for this user in the database */
  private int total = 0;
  /** The maximum number of user settings and signatures to return. */
  private int limit = 0;
  /** The page offset into the results. */
  private int offset = 0;
  /** The user settings attached to this user */
  private Map<String, Collection<UserSetting>> userSettings = new HashMap<String, Collection<UserSetting>>();

  /**
   * Default constructor.
   */
  public UserSettings() {

  }

  public UserSettings(Map<String, Collection<UserSetting>> userSettings) {
    this.userSettings = userSettings;
  }

  public Map<String, Collection<UserSetting>> getUserSettingsMap() {
    return userSettings;
  }

  public Collection<UserSetting> getUserSettingsCollection() {
    Collection<UserSetting> userSettingCollection = new ArrayList<UserSetting>();
    for (Collection<UserSetting> collection : userSettings.values()) {
        userSettingCollection.addAll(collection);
    }
    return userSettingCollection;
  }

  public void setUserSettings(Map<String, Collection<UserSetting>> userSettings) {
    this.userSettings = userSettings;
  }

  public void addUserSetting(UserSetting userSetting) {
    Collection<UserSetting> collection = userSettings.get(userSetting.getKey());
    if (collection == null) {
        collection = new ArrayList<UserSetting>();
    }
    collection.add(userSetting);
    userSettings.put(userSetting.getKey(), collection);
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  /**
   * @return The JSON representation of these user settings.
   */
  public Obj toJson() {
    List<Val> settingsArr = new ArrayList<Val>();
    for (UserSetting userSetting : getUserSettingsCollection()) {
      settingsArr.add(userSetting.toJson());
    }
    return Jsons.obj(Jsons.p("offset", offset), Jsons.p("limit", limit), Jsons.p("total", total),Jsons.p("results", Jsons.arr(settingsArr)));
  }
}
