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

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Strings.asString;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import org.osgi.framework.Bundle;

/** Functions on {@link BundleInfo}. */
public final class BundleInfos {
  public static final String MANIFEST_BUILD_NUMBER = "Build-Number";
  public static final String MANIFEST_DB_VERSION = "Mh-Db-Version";

  private BundleInfos() {
  }

  /** Check if version and build numbers are equal. */
  public static boolean versionEq(BundleInfo a, BundleInfo b) {
    return eq(a.getBundleVersion(), b.getBundleVersion()) && eq(a.getBuildNumber(), b.getBuildNumber());
  }

  /** {@link BundleInfos#versionEq(BundleInfo, BundleInfo)} as a function. */
  public static final Function2<BundleInfo, BundleInfo, Boolean> versionEq = new Function2<BundleInfo, BundleInfo, Boolean>() {
    @Override
    public Boolean apply(BundleInfo a, BundleInfo b) {
      return versionEq(a, b);
    }
  };

  public static final Function<BundleInfo, String> getBundleVersion = new Function<BundleInfo, String>() {
    @Override
    public String apply(BundleInfo bundleInfo) {
      return bundleInfo.getBundleVersion();
    }
  };

  public static final Function<BundleInfo, Option<String>> getBuildNumber = new Function<BundleInfo, Option<String>>() {
    @Override
    public Option<String> apply(BundleInfo bundleInfo) {
      return bundleInfo.getBuildNumber();
    }
  };

  /** Extract the build number of a bundle. */
  public static Option<String> getBuildNumber(Bundle bundle) {
    return option(bundle.getHeaders().get(MANIFEST_BUILD_NUMBER)).bind(asString());
  }
}
