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

/** The existing versions */
public enum ApiVersion {

  VERSION_UNDEFINED("v*.*.*"),

  VERSION_1_0_0("v1.0.0");

  public static final ApiVersion CURRENT_VERSION = VERSION_1_0_0;

  private String versionString;

  ApiVersion(String versionString) {
    this.versionString = versionString;
  }

  public static ApiVersion of(String version) {
    switch (version) {
      case "v1.0.0":
        return VERSION_1_0_0;
      default:
        throw new IllegalArgumentException("'" + version + "' is not a valid version");
    }
  }

  public String toExternalForm() {
    return versionString;
  }

  @Override
  public String toString() {
    return toExternalForm();
  }

}
