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

package org.opencastproject.series.impl.persistence;

import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesCreator;

import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class SeriesCreatorEntity {

  private String username;

  private String name;

  SeriesCreatorEntity(User user) {
    this.username = user.getUsername();
    this.name = user.getName();
  }

  SeriesCreatorEntity(String username, String name) {
    this.username = username;
    this.name = name;
  }

  protected SeriesCreatorEntity() {
  }

  public String username() {
    return username;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SeriesCreatorEntity that)) {
      return false;
    }
    return Objects.equals(username, that.username) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, name);
  }

  public SeriesCreator toModel() {
    return new SeriesCreator(username, name);
  }

}
