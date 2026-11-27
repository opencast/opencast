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

package org.opencastproject.capture.admin.api;

public enum AgentVersion {
  VERSION_1("1"),
  VERSION_2("2");

  private final String version;

  AgentVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return this.version;
  }

  public String toString() {
    return this.version;
  }

  public static AgentVersion fromVersion(String version) {
    for (AgentVersion v : AgentVersion.values()) {
      if (v.getVersion().equalsIgnoreCase(version)) {
        return v;
      }
    }
    // If nothing else matches, it's a VERSION_1 agent
    return VERSION_1;
  }
}
