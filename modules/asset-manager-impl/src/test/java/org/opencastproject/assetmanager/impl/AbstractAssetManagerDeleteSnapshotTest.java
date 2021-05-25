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
package org.opencastproject.assetmanager.impl;

import static org.junit.Assert.assertEquals;

import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.DefaultOrganization;

import com.mysema.query.jpa.JPASubQuery;

import org.junit.Test;

public class AbstractAssetManagerDeleteSnapshotTest extends AbstractAssetManagerDeleteTestBase implements EntityPaths {
  // run asset manager or raw JPA queries
  private static final boolean RUN_RAW_QUERIES = false;

  /**
   * Deleting a whole episode should also delete all of its properties.
   * This test case deletes all snapshots.
   */
  @Test
  public void testDeleteAll() throws Exception {
    final int mpCount = 3;
    final String[] mp = createAndAddMediaPackagesSimple(mpCount, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-2"));
    assertTotals(mpCount, mpCount, 3);
    assertStoreSize(6);
    assertEquals(3, q.delete(OWNER, q.snapshot()).run());
    assertTotals(0, 0, 3);
    assertStoreSize(0);
  }

  /**
   * Deleting a complete episode should also delete all of its properties.
   * This test case deletes all versions (snapshots) of one episode.
   */
  @Test
  public void testDeleteAllVersionsOfOne() throws Exception {
    final int mpCount = 3;
    final int versionCount = 5;
    final String[] mp = createAndAddMediaPackagesSimple(mpCount, versionCount, versionCount);
    // each mp has one property
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-2"));
    assertTotals(mpCount * versionCount, mpCount * versionCount, 3);
    assertStoreSize(mpCount * versionCount * 2);
    assertEquals(versionCount, q.delete(OWNER, q.snapshot()).where(q.mediaPackageId(mp[0])).run());
    assertTotals((mpCount - 1) * versionCount, (mpCount - 1) * versionCount, 3);
    assertStoreSize((mpCount - 1) * versionCount * 2);
  }

  /**
   * If, after deleting versions of an episode at least one version remains, no
   * properties of the episode should be deleted.
   */
  @Test
  public void testDeleteOneVersionOfOne() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 2, 2);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-2"));
    assertTotals(6, 6, 3);
    assertStoreSize(6 * 2);
    if (RUN_RAW_QUERIES) {
      assertEquals(
          3,
          delete(
              Q_ASSET,
              Q_ASSET.snapshot.id.in(
                  new JPASubQuery().from(Q_SNAPSHOT)
                      .where(
                          Q_SNAPSHOT.version
                              .eq(new JPASubQuery().from(Q_SNAPSHOT).unique(Q_SNAPSHOT.version.min()))
                      )
                      .list(Q_SNAPSHOT.id)
              )
          )
      );
      assertEquals(
          3,
          delete(
              Q_SNAPSHOT,
              Q_SNAPSHOT.version.eq(new JPASubQuery().from(Q_SNAPSHOT).unique(Q_SNAPSHOT.version.min()))
          )
      );
    } else {
      assertEquals(
          "Three snapshots should be deleted",
          3,
          q.delete(OWNER, q.snapshot()).where(q.version().isFirst()).run()
      );
    }
    assertTotals(3, 3, 3);
    assertStoreSize(3 * 2);
  }

  /**
   * If, after deleting versions of an episode no version remains, all properties of the episode should be deleted.
   */
  @Test
  public void testDeleteAllVersionsOfOne2() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 2, 2);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-2"));
    assertTotals(6, 6, 3);
    assertStoreSize(6 * 2);
    assertEquals(
        "Three snapshots should be deleted",
        3,
        q.delete(OWNER, q.snapshot()).where(q.version().isFirst()).run()
    );
    assertTotals(3, 3, 3);
    assertStoreSize(3 * 2);
  }

  @Test
  public void testDeleteByProperty() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[2], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    assertTotals(3, 3, 3);
    assertStoreSize(3 * 2);
    assertEquals("Two snapshots should be deleted", 2,
            q.delete(OWNER, q.snapshot()).where(p.agent.eq("agent-1")).run());
    assertTotals(1, 1, 3);
    assertStoreSize(2);
  }

  @Test
  public void testDeleteByOrganizationAndPropertyExistence() {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    assertTotals(3, 3, 1);
    assertEquals(
        2,
        q.delete(OWNER, q.snapshot())
            .where(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID).and(q.hasProperties().not()))
            .run()
    );
    assertTotals(1, 1, 1);
  }

  @Test
  public void testOwnership() throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    am.takeSnapshot("owner-1", mp);
    am.takeSnapshot("owner-2", mp);
    am.takeSnapshot("owner-3", mp);
    am.takeSnapshot("owner-3", mp);
    am.setProperty(p.agent.mk(mp.getIdentifier().toString(), "agent"));
    //
    assertEquals("There should be 1 property",
                 1, q.select(q.propertiesOf()).where(q.version().isLatest()).run().getSize());
    assertEquals("There should be 4 snapshots",
                 4, q.select(q.snapshot()).run().getSize());
    //
    assertEquals("Two snapshots should be deleted", 2, q.delete("owner-3", q.snapshot()).run());
    assertEquals("There should be 2 snapshots left", 2, q.select(q.snapshot()).run().getSize());
    //
    assertEquals("Property should be deleted since ownership is being ignored when deleting properties",
                 1, q.delete("owner-3", p.agent.target()).run());
    assertEquals("Property has already been deleted",
                 0, q.delete("owner-1", p.agent.target()).run());
    assertEquals("There should be no more properties",
                 0, q.select().where(p.agent.exists()).run().getSize());
    //
    // Wildcard deletion. Disabled as of ticket CERV-1158. Kept for potentially later reference.
    // assertEquals("All remaining snapshots should be deleted", 2, q.delete("", q.snapshot()).run());
    // assertEquals("There should be no more snapshots", 0, q.select(q.snapshot()).run().getSize());
  }
}
