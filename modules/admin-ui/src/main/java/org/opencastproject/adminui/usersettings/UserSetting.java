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

/**
 * A class used to store a key-value pair for a user setting.
 */
public class UserSetting {
  /** The unique id that identifies it in the database. */
  private final long id;
  /** The key that will identify which setting this is. */
  private final String key;
  /** The value that is what the setting is set to. */
  private final String value;

  /**
   * @param id
   *          A unique id in the database.
   * @param key
   *          A key that will identify which setting this is, may not be unique.
   * @param value
   *          A value that the setting is set to.
   */
  public UserSetting(long id, String key, String value) {
    this.id = id;
    this.key = key;
    this.value = value;
  }

  public long getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  /**
   * @return The JSON representation of this user setting.
   */
  public Obj toJson() {
    return Jsons.obj(Jsons.p("id", id), Jsons.p("key", key), Jsons.p("value", value));
  }
}
