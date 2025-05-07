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

import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.fn.Properties;

import org.junit.Test;

public class AssetManagerDeletePropertyTest extends AssetManagerDeleteTestBase {

  @Test
  public void testDeleteByMediaPackage() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 2, 2);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-5"));
    assertTotals(6, 6, 3);
    assertEquals(
        "One property should be deleted",
        1,
        am.deleteProperties(mp[0])
    );
    assertTotals(6, 6, 2);
    assertEquals(
        "One property should be deleted",
        1,
        am.deleteProperties(mp[1])
    );
    assertTotals(6, 6, 1);
    assertEquals(Value.mk("agent-5"),
        am.selectProperties(mp[2], p.namespace()).get(0).getValue());
  }

  @Test
  public void testDeleteByMediaPackageAndNamespace() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 2, 2);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p2.agent.mk(mp[1], "agent-12"));
    am.setProperty(p.agent.mk(mp[2], "agent-5"));
    assertTotals(6, 6, 4);
    assertEquals(
        "One property should be deleted",
        1,
        am.deleteProperties(mp[0], p.namespace())
    );
    assertTotals(6, 6, 3);
    assertEquals(
        "One property should be deleted",
        1,
        am.deleteProperties(mp[1], p.namespace())
    );
    assertTotals(6, 6, 2);
    assertEquals(Value.mk("agent-12"),
        am.selectProperties(mp[1], p2.namespace()).get(0).getValue());
    assertEquals(Value.mk("agent-5"),
        am.selectProperties(mp[2], p.namespace()).get(0).getValue());
  }

  @Test
  public void testRemoveProperties() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(2, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent"));
    am.setProperty(p.approved.mk(mp[0], true));
    am.setProperty(p.count.mk(mp[0], 1L));
    am.setProperty(p2.agent.mk(mp[0], "agent"));
    //
    am.setProperty(p.agent.mk(mp[1], "agent"));
    am.setProperty(p.approved.mk(mp[1], true));
    am.setProperty(p.legacyId.mk(mp[1], "id"));
    assertEquals(0L, Properties.removeProperties(
        am, "unknown-mp-id", p.namespace()
    ));
    assertEquals(0L, Properties.removeProperties(
        am, mp[0], "unknown-namespace"
    ));
    assertEquals(3L, Properties.removeProperties(
        am, mp[0], p.namespace()
    ));
    assertEquals(1L, am.selectProperties(mp[0], null).size());
//    assertEquals(1L, enrich(q.select(q.properties()).where(q.mediaPackageId(mp[0])).run()).countProperties());
    assertEquals(1L, Properties.removeProperties(
        am, mp[0], p2.namespace()
    ));
    assertEquals(3L, Properties.removeProperties(
        am, mp[1], p.namespace()
    ));
  }
}
