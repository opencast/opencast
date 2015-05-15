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

import static org.junit.Assert.assertEquals;
import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Option;

import org.junit.Test;

public class BundleInfoJpaTest {
  @Test
  public void testTransfer() throws Exception {
    final BundleInfoJpa dto = BundleInfoJpa
            .create(bundleInfo("host", "bundle", 1L, "version", some("sha"), some("db")));
    run(BundleInfo.class, new BundleInfo() {
      @Override
      public String getHost() {
        assertEquals("host transferred", "host", dto.host);
        return null;
      }

      @Override
      public String getBundleSymbolicName() {
        assertEquals("bundle symbolic name transferred", "bundle", dto.bundleSymbolicName);
        return null;
      }

      @Override
      public long getBundleId() {
        assertEquals("bundle id transferred", 1L, dto.bundleId);
        return 0L;
      }

      @Override
      public String getBundleVersion() {
        assertEquals("bundle version transferred", "version", dto.bundleVersion);
        return null;
      }

      @Override
      public Option<String> getBuildNumber() {
        assertEquals("build number transferred", "sha", dto.buildNumber);
        return null;
      }

      @Override
      public BundleVersion getVersion() {
        assertEquals("bundle version transferred", "version", dto.bundleVersion);
        assertEquals("build number transferred", "sha", dto.buildNumber);
        return null;
      }
    });
    final BundleInfo info = dto.toBundleInfo();
    run(BundleInfo.class, new BundleInfo() {
      @Override
      public String getHost() {
        assertEquals("host transferred", "host", info.getHost());
        return null;
      }

      @Override
      public String getBundleSymbolicName() {
        assertEquals("bundle symbolic name transferred", "bundle", info.getBundleSymbolicName());
        return null;
      }

      @Override
      public long getBundleId() {
        assertEquals("bundle id transferred", 1L, info.getBundleId());
        return 0L;
      }

      @Override
      public String getBundleVersion() {
        assertEquals("bundle version transferred", "version", info.getBundleVersion());
        return null;

      }

      @Override
      public Option<String> getBuildNumber() {
        assertEquals("build number transferred", some("sha"), info.getBuildNumber());
        return null;
      }

      @Override
      public BundleVersion getVersion() {
        assertEquals("bundle version transferred", "version", info.getVersion().getBundleVersion());
        assertEquals("build number transferred", some("sha"), info.getVersion().getBuildNumber());
        return null;
      }
    });
    //
    assertEquals("no build number", Option.<String> none(),
            BundleInfoJpa.create(bundleInfo("-", "-", 0L, "-", Option.<String> none())).toBundleInfo().getBuildNumber());
  }
}
