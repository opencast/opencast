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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.util.ReflectionUtil.run;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBTestEnv;

import org.junit.jupiter.api.Test;

import java.util.Optional;

public class BundleInfoDbTest {
  @Test
  public void testPersistence() {
    final BundleInfoDb db = db();
    final BundleInfo info = bundleInfo("localhost", "bundle", 1L, "1.4.0", Optional.of("1345"), Optional.of("9012"));
    db.store(info);
    assertEquals(1, db.getBundles().size(), "db contains an element");
    for (final BundleInfo a : db.getBundles()) {
      run(BundleInfo.class, new BundleInfo() {
        @Override
        public String getHost() {
          assertEquals(info.getHost(), a.getHost(), "host persisted");
          return null;
        }

        @Override
        public String getBundleSymbolicName() {
          assertEquals(info.getBundleSymbolicName(), a.getBundleSymbolicName(), "bundle symbolic name persisted");
          return null;
        }

        @Override
        public long getBundleId() {
          assertEquals(info.getBundleId(), a.getBundleId(), "bundle id persisted");
          return 0L;
        }

        @Override
        public String getBundleVersion() {
          assertEquals(info.getBundleVersion(), a.getBundleVersion(), "bundle version persisted");
          return null;
        }

        @Override
        public Optional<String> getBuildNumber() {
          assertEquals(info.getBuildNumber(), a.getBuildNumber(), "build number persisted");
          return null;
        }

        @Override
        public BundleVersion getVersion() {
          assertEquals(info.getVersion().getBundleVersion(), a.getVersion()
                  .getBundleVersion(), "bundle version persisted");
          assertEquals(info.getVersion().getBuildNumber(), a.getVersion().getBuildNumber(), "build number persisted");
          return null;
        }
      });
    }
    db.clearAll();
    assertEquals(0, db.getBundles().size(), "db is empty");
    //
    db.store(bundleInfo("localhost", "bundle", 2L, "1.4.1", Optional.<String> empty()));
    assertEquals(Optional.<String> empty(), db.getBundles().get(0).getBuildNumber(), "no build number");
  }

  @Test
  public void testContstraints() {
    assertThrows(BundleInfoDbException.class, () -> {
      final BundleInfoDb db = db();
      db.store(bundleInfo("localhost", "bundle-1", 1L, "1.4.0", Optional.empty()));
      // insert violates constraints
      db.store(bundleInfo("localhost", "bundle-1", 5L, "1.4.0", Optional.of("ae41b09")));
    });
  }

  @Test
  public void testPrefixQuery() {
    final BundleInfoDb db = db();
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "eth-1", 4L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "opencast-1", 5L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "opencast-2", 6L, "1.4.0", Optional.empty()));
    assertEquals(6, db.getBundles().size());
    assertEquals(3, db.getBundles("matterhorn").size());
    assertEquals(1, db.getBundles("eth").size());
    assertEquals(2, db.getBundles("opencast").size());
    assertEquals(0, db.getBundles("UNKNOWN").size());
    assertEquals(4, db.getBundles("matterhorn", "eth").size());
  }

  @Test
  public void testDeleteOne() {
    final BundleInfoDb db = db();
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", Optional.empty()));
    assertEquals(3, db.getBundles().size());
    db.delete("localhost", db.getBundles().get(0).getBundleId());
    assertEquals(2, db.getBundles().size());
  }

  @Test
  public void testDeleteByHost() {
    final BundleInfoDb db = db();
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", Optional.empty()));
    db.store(bundleInfo("remote", "matterhorn-1", 1L, "1.4.0", Optional.empty()));
    assertEquals(2, db.getBundles().size(), "db size");
    db.clear("localhost");
    assertEquals(1, db.getBundles().size(), "db size");
    db.clear("remote");
    assertEquals(0, db.getBundles().size(), "db size");
  }

  private BundleInfoDb db() {
    final DBSession db = DBTestEnv.newDBSession(OsgiBundleInfoDb.PERSISTENCE_UNIT);
    return new AbstractBundleInfoDb() {
      @Override
      protected DBSession getDBSession() {
        return db;
      }
    };
  }
}
