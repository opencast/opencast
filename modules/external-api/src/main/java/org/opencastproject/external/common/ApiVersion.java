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
package org.opencastproject.external.common;

/** The existing External API versions */
public enum ApiVersion {

  VERSION_1_0_0(1, 0, 0),
  VERSION_1_1_0(1, 1, 0);

  /** The most recent version of the External API */
  public static final ApiVersion CURRENT_VERSION = VERSION_1_1_0;

  private int major;
  private int minor;
  private int patch;

  private String versionString;

  ApiVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    versionString = "v" + major + "." + minor + "." + patch;
  }

  public boolean isSmallerThan(ApiVersion other) {
    if (this.major < other.major) return true;
    if (this.major > other.major) return false;
    if (this.minor < other.minor) return true;
    if (this.minor > other.minor) return false;
    return this.patch < other.patch;
  }

  public String toExternalForm() {
    return versionString;
  }

  @Override
  public String toString() {
    return toExternalForm();
  }

}
