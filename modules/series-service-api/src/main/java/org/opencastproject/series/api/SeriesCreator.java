/*
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

package org.opencastproject.series.api;

import org.opencastproject.security.api.User;

import java.util.Objects;

public class SeriesCreator {

  private final String username;

  private final String name;

  public SeriesCreator(User user) {
    this.username = Objects.requireNonNull(user.getUsername(), "Username must not be null");
    this.name = user.getName();
  }

  public SeriesCreator(String username, String name) {
    this.username = Objects.requireNonNull(username, "Username must not be null");
    this.name = name;
  }

  public String username() {
    return username;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SeriesCreator that)) {
      return false;
    }
    return Objects.equals(username, that.username) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, name);
  }

}
