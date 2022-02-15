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
package org.opencastproject.editor.api;

import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author Tobias M Schiebeck
 */
public class LockData {
  public static final String REFRESH_LOCK = "refresh_lock";
  private final String user;
  private final String email;
  private final String name;
  @SerializedName(REFRESH_LOCK)
  private final Boolean refreshLock;

  public LockData(String user, String name, String email, boolean refreshLock) {
    this.user = user;
    this.name = name;
    this.email = email;
    this.refreshLock = refreshLock;
  }

  public static LockData parse(String json) {
    requireNonNull(json);
    Gson gson = new Gson();
    LockData lockData = gson.fromJson(json, LockData.class);

    return lockData;
  }



  public String getUser() {
    return user;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public Boolean isRefresh() {
    return refreshLock;
  }

  public String toJSONString() {
    Gson gson = new GsonBuilder().serializeNulls().create();
    return gson.toJson(this);
  }

  @Override
  public String toString() {
    String out = "is locked by ";
    if (! "".equals(name)) {
      out += name + " ";
    }
    if (! "".equals(user)) {
      out +=  "user " + user + " ";
    }
    if (! "".equals(email)) {
      out += "(" + email + ").";
    }
    return out;
  }

}
