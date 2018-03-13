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

import static org.opencastproject.external.common.ApiMediaType.VERSION_REG_EX_PATTERN;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The existing versions */
public enum ApiVersion {

  VERSION_UNDEFINED("v*.*.*"),

  VERSION_1_0_0("v1.0.0"),

  VERSION_1_1_0("v1.1.0");

  public static final ApiVersion CURRENT_VERSION = VERSION_1_1_0;

  private String versionString;
  private int major;
  private int minor;
  private int micro;

  ApiVersion(String versionString) {
    this.versionString = versionString;
    if ("v*.*.*".equals(versionString)) {
      major = Integer.MAX_VALUE;
      minor = Integer.MAX_VALUE;
      micro = Integer.MAX_VALUE;
    } else {
      Matcher matcher = Pattern.compile(VERSION_REG_EX_PATTERN).matcher(versionString);
      if (!matcher.matches() || matcher.groupCount() != 3) {
        throw new IllegalArgumentException("'" + versionString + "' is not a valid version");
      }
      major = Integer.parseInt(matcher.group(1));
      minor = Integer.parseInt(matcher.group(2));
      micro = Integer.parseInt(matcher.group(3));
    }
  }

  public static ApiVersion of(String version) {
    switch (version) {
      case "v1.0.0":
        return VERSION_1_0_0;
      case "v1.1.0":
        return VERSION_1_1_0;
      default:
        throw new IllegalArgumentException("'" + version + "' is not a valid version");
    }
  }

  public boolean isSmallerThan(ApiVersion other) {
    if (this.major < other.major) return true;
    if (this.major > other.major) return false;
    if (this.minor < other.minor) return true;
    if (this.minor > other.minor) return false;
    return this.micro < other.micro;
  }

  public String toExternalForm() {
    return versionString;
  }

  @Override
  public String toString() {
    return toExternalForm();
  }

}
