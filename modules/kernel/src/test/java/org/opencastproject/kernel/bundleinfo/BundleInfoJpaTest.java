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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.util.ReflectionUtil.run;

import org.junit.jupiter.api.Test;

import java.util.Optional;

public class BundleInfoJpaTest {
  @Test
  public void testTransfer() throws Exception {
    final BundleInfoJpa dto = BundleInfoJpa
            .create(bundleInfo("host", "bundle", 1L, "version", Optional.of("sha"), Optional.of("db")));
    run(BundleInfo.class, new BundleInfo() {
      @Override
      public String getHost() {
        assertEquals("host", dto.host, "host transferred");
        return null;
      }

      @Override
      public String getBundleSymbolicName() {
        assertEquals("bundle", dto.bundleSymbolicName, "bundle symbolic name transferred");
        return null;
      }

      @Override
      public long getBundleId() {
        assertEquals(1L, dto.bundleId, "bundle id transferred");
        return 0L;
      }

      @Override
      public String getBundleVersion() {
        assertEquals("version", dto.bundleVersion, "bundle version transferred");
        return null;
      }

      @Override
      public Optional<String> getBuildNumber() {
        assertEquals("sha", dto.buildNumber, "build number transferred");
        return null;
      }

      @Override
      public BundleVersion getVersion() {
        assertEquals("version", dto.bundleVersion, "bundle version transferred");
        assertEquals("sha", dto.buildNumber, "build number transferred");
        return null;
      }
    });
    final BundleInfo info = dto.toBundleInfo();
    run(BundleInfo.class, new BundleInfo() {
      @Override
      public String getHost() {
        assertEquals("host", info.getHost(), "host transferred");
        return null;
      }

      @Override
      public String getBundleSymbolicName() {
        assertEquals("bundle", info.getBundleSymbolicName(), "bundle symbolic name transferred");
        return null;
      }

      @Override
      public long getBundleId() {
        assertEquals(1L, info.getBundleId(), "bundle id transferred");
        return 0L;
      }

      @Override
      public String getBundleVersion() {
        assertEquals("version", info.getBundleVersion(), "bundle version transferred");
        return null;

      }

      @Override
      public Optional<String> getBuildNumber() {
        assertEquals(Optional.of("sha"), info.getBuildNumber(), "build number transferred");
        return null;
      }

      @Override
      public BundleVersion getVersion() {
        assertEquals("version", info.getVersion().getBundleVersion(), "bundle version transferred");
        assertEquals(Optional.of("sha"), info.getVersion().getBuildNumber(), "build number transferred");
        return null;
      }
    });
    //
    assertEquals(Optional.<String> empty(),
        BundleInfoJpa.create(bundleInfo("-", "-", 0L, "-", Optional.<String> empty()))
                .toBundleInfo().getBuildNumber(),
            "no build number");
  }
}
