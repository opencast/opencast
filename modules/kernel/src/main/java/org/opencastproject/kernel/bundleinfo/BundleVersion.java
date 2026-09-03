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

package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.util.EqualsUtil.hash;

import java.util.Objects;
import java.util.Optional;

public final class BundleVersion {
  private final String bundleVersion;
  private final Optional<String> buildNumber;

  public BundleVersion(String bundleVersion, Optional<String> buildNumber) {
    this.bundleVersion = bundleVersion;
    this.buildNumber = buildNumber;
  }

  public String getBundleVersion() {
    return bundleVersion;
  }

  public Optional<String> getBuildNumber() {
    return buildNumber;
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof BundleVersion && eqFields((BundleVersion) that));
  }

  private boolean eqFields(BundleVersion that) {
    return Objects.equals(this.bundleVersion, that.bundleVersion) && Objects.equals(this.buildNumber, that.buildNumber);
  }

  @Override public int hashCode() {
    return hash(bundleVersion, buildNumber);
  }
}
