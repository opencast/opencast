/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.util.data.Option.none;

import org.opencastproject.util.data.Option;

public class BundleInfoImpl implements BundleInfo {
  private final String host;
  private final String bundleSymbolicName;
  private final long bundleId;
  private final String bundleVersion;
  private final Option<String> buildNumber;
  private final Option<String> dbSchemaVersion;
  private final BundleVersion version;

  public BundleInfoImpl(String host, String bundleSymbolicName, long bundleId, String bundleVersion,
          Option<String> buildNumber, Option<String> dbSchemaVersion) {
    this.host = host;
    this.bundleSymbolicName = bundleSymbolicName;
    this.bundleId = bundleId;
    this.bundleVersion = bundleVersion;
    this.buildNumber = buildNumber;
    this.dbSchemaVersion = dbSchemaVersion;
    this.version = new BundleVersion(bundleVersion, buildNumber);
  }

  public static BundleInfo bundleInfo(String host, String bundleSymbolicName, long bundleId, String bundleVersion,
          Option<String> buildNumber) {
    return new BundleInfoImpl(host, bundleSymbolicName, bundleId, bundleVersion, buildNumber, none(""));
  }

  public static BundleInfo bundleInfo(String host, String bundleSymbolicName, long bundleId, String bundleVersion,
          Option<String> buildNumber, Option<String> dbSchemaVersion) {
    return new BundleInfoImpl(host, bundleSymbolicName, bundleId, bundleVersion, buildNumber, dbSchemaVersion);
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public String getBundleSymbolicName() {
    return bundleSymbolicName;
  }

  @Override
  public long getBundleId() {
    return bundleId;
  }

  @Override
  public String getBundleVersion() {
    return bundleVersion;
  }

  @Override
  public Option<String> getBuildNumber() {
    return buildNumber;
  }

  @Override
  public BundleVersion getVersion() {
    return version;
  }
}
