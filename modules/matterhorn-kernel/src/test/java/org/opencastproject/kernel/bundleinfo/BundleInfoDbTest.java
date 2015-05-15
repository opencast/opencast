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
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;

import org.junit.Test;

public class BundleInfoDbTest {
  @Test
  public void testPersistence() {
    final BundleInfoDb db = db();
    final BundleInfo info = bundleInfo("localhost", "bundle", 1L, "1.4.0", some("1345"), some("9012"));
    db.store(info);
    assertEquals("db contains an element", 1, db.getBundles().size());
    for (final BundleInfo a : db.getBundles()) {
      run(BundleInfo.class, new BundleInfo() {
        @Override
        public String getHost() {
          assertEquals("host persisted", info.getHost(), a.getHost());
          return null;
        }

        @Override
        public String getBundleSymbolicName() {
          assertEquals("bundle symbolic name persisted", info.getBundleSymbolicName(), a.getBundleSymbolicName());
          return null;
        }

        @Override
        public long getBundleId() {
          assertEquals("bundle id persisted", info.getBundleId(), a.getBundleId());
          return 0L;
        }

        @Override
        public String getBundleVersion() {
          assertEquals("bundle version persisted", info.getBundleVersion(), a.getBundleVersion());
          return null;
        }

        @Override
        public Option<String> getBuildNumber() {
          assertEquals("build number persisted", info.getBuildNumber(), a.getBuildNumber());
          return null;
        }

        @Override
        public BundleVersion getVersion() {
          assertEquals("bundle version persisted", info.getVersion().getBundleVersion(), a.getVersion()
                  .getBundleVersion());
          assertEquals("build number persisted", info.getVersion().getBuildNumber(), a.getVersion().getBuildNumber());
          return null;
        }
      });
    }
    db.clearAll();
    assertEquals("db is empty", 0, db.getBundles().size());
    //
    db.store(bundleInfo("localhost", "bundle", 2L, "1.4.1", Option.<String> none()));
    assertEquals("no build number", Option.<String> none(), db.getBundles().get(0).getBuildNumber());
  }

  @Test(expected = BundleInfoDbException.class)
  public void testContstraints() {
    final BundleInfoDb db = db();
    db.store(bundleInfo("localhost", "bundle-1", 1L, "1.4.0", none("")));
    // insert violates constraints
    db.store(bundleInfo("localhost", "bundle-1", 5L, "1.4.0", some("ae41b09")));
  }

  @Test
  public void testPrefixQuery() {
    final BundleInfoDb db = db();
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "eth-1", 4L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "opencast-1", 5L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "opencast-2", 6L, "1.4.0", none("")));
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
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", none("")));
    assertEquals(3, db.getBundles().size());
    db.delete("localhost", db.getBundles().get(0).getBundleId());
    assertEquals(2, db.getBundles().size());
  }

  @Test
  public void testDeleteByHost() {
    final BundleInfoDb db = db();
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", none("")));
    db.store(bundleInfo("remote", "matterhorn-1", 1L, "1.4.0", none("")));
    assertEquals("db size", 2, db.getBundles().size());
    db.clear("localhost");
    assertEquals("db size", 1, db.getBundles().size());
    db.clear("remote");
    assertEquals("db size", 0, db.getBundles().size());
  }

  private BundleInfoDb db() {
    final PersistenceEnv penv = PersistenceUtil.newTestPersistenceEnv(OsgiBundleInfoDb.PERSISTENCE_UNIT);
    return new AbstractBundleInfoDb() {
      @Override
      protected PersistenceEnv getPersistenceEnv() {
        return penv;
      }
    };
  }
}
