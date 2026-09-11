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
package org.opencastproject.assetmanager.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssetManagerDeleteSnapshotTest extends AssetManagerDeleteTestBase {

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
    am.setProperty(p.agent(mp[0], "agent-1"));
    am.setProperty(p.agent(mp[1], "agent-2"));
    am.setProperty(p.agent(mp[2], "agent-2"));
    assertTotals(mpCount * versionCount, mpCount * versionCount, 9);
    assertStoreSize(mpCount * versionCount * 2);
    assertEquals(versionCount, am.deleteSnapshots(mp[0]));
    assertTotals((mpCount - 1) * versionCount, (mpCount - 1) * versionCount, 9);
    assertStoreSize((mpCount - 1) * versionCount * 2);
  }

  /**
   * If, after deleting versions of an episode no version remains, all properties of the episode should be deleted.
   */
  @Test
  public void testDeleteAllVersionsOfOne2() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 2, 2);
    am.setProperty(p.agent(mp[0], "agent-1"));
    am.setProperty(p.agent(mp[1], "agent-2"));
    am.setProperty(p.agent(mp[2], "agent-2"));
    assertTotals(6, 6, 9);
    assertStoreSize(6 * 2);
    assertEquals(
        "Two snapshots should be deleted",
        2,
        am.deleteSnapshots(mp[0])
    );
    assertTotals(4, 4, 9);
    assertStoreSize(4 * 2);
  }

  /**
   * Delete all but the latest snapshot
   */
  @Test
  public void testDeleteAllButLatestVersionsOfOne() throws Exception {
    final int mpCount = 3;
    final int versionCount = 5;
    final String[] mp = createAndAddMediaPackagesSimple(mpCount, versionCount, versionCount);
    // each mp has one property
    am.setProperty(p.agent(mp[0], "agent-1"));
    am.setProperty(p.agent(mp[1], "agent-2"));
    am.setProperty(p.agent(mp[2], "agent-2"));
    assertTotals(mpCount * versionCount, mpCount * versionCount, 9);
    assertStoreSize(mpCount * versionCount * 2);
    assertEquals(versionCount - 1, am.deleteAllButLatestSnapshot(mp[0]));
    assertTotals((mpCount - 1) * versionCount + 1, 11, 9);
    assertStoreSize(((mpCount - 1) * versionCount * 2) + 2);
  }
}
