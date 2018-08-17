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

package org.opencastproject.userdirectory.moodle;

import java.util.Objects;

public class MoodleUser {
  private String id;
  private String username;
  private String fullname;
  private String idnumber;
  private String email;
  private String auth;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFullname() {
    return fullname;
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public String getIdnumber() {
    return idnumber;
  }

  public void setIdnumber(String idnumber) {
    this.idnumber = idnumber;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MoodleUser that = (MoodleUser) o;
    return Objects.equals(id, that.id) && Objects.equals(username, that.username) && Objects
            .equals(fullname, that.fullname) && Objects.equals(idnumber, that.idnumber) && Objects
            .equals(email, that.email) && Objects.equals(auth, that.auth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, fullname, idnumber, email, auth);
  }

  @Override
  public String toString() {
    return "MoodleUser{" + "id=" + id + ", username='" + username + '\'' + ", fullname='" + fullname + '\''
            + ", idnumber='" + idnumber + '\'' + ", email='" + email + '\'' + ", auth='" + auth + '\'' + '}';
  }
}
