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

import org.opencastproject.util.data.Option;

public interface BundleInfo {
  /** The host where the bundle lives. */
  String getHost();

  /** The bundle's OSGi symbolic name. */
  String getBundleSymbolicName();

  /** The bundle id in the OSGi container. */
  long getBundleId();

  /** The full bundle version which is a tuple of {@link #getBundleVersion()} and {@link #getBuildNumber()}. */
  BundleVersion getVersion();

  /** The OSGi bundle version. */
  String getBundleVersion();

  /** Get the build number, e.g. the git hash. */
  Option<String> getBuildNumber();
}
