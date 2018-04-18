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
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.fn.Properties;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.security.api.DefaultOrganization;

import com.entwinemedia.fn.data.Opt;
import com.mysema.query.jpa.JPASubQuery;

import org.junit.Test;

public class AbstractAssetManagerDeletePropertyTest extends AbstractAssetManagerDeleteTestBase implements EntityPaths {
  // run asset manager or raw JPA queries
  private static final boolean RUN_RAW_QUERIES = false;

  @Test
  public void testDeleteOfNamespace() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.legacyId.mk(mp[2], "id"));
    am.setProperty(Property.mk(PropertyId.mk(mp[0], "namespace", "prop-name"), Value.mk(true)));
    assertTotals(3, 3, 3);
    if (RUN_RAW_QUERIES) {
      assertEquals(2, delete(Q_PROPERTY,
                             Q_PROPERTY.namespace.eq(p.namespace())));
    } else {
      assertEquals(2, q.delete(OWNER, p.allProperties()).run());
    }
    assertTotals(3, 3, 1);
  }

  @Test
  public void testDeleteAll() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.legacyId.mk(mp[0], "id"));
    am.setProperty(Property.mk(PropertyId.mk(mp[2], "namespace", "prop-name"), Value.mk(true)));
    assertTotals(3, 3, 3);
    if (RUN_RAW_QUERIES) {
      assertEquals(3, delete(Q_PROPERTY));
    } else {
      assertEquals(3, q.delete(OWNER, q.properties()).run());
    }
    assertTotals(3, 3, 0);
  }

  @Test
  public void testDeleteOne() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.legacyId.mk(mp[0], "id"));
    am.setProperty(p.agent.mk(mp[1], "agent-1"));
    am.setProperty(Property.mk(PropertyId.mk(mp[2], "namespace", "prop-name"), Value.mk(true)));
    assertTotals(3, 3, 4);
    if (RUN_RAW_QUERIES) {
      assertEquals(2, delete(Q_PROPERTY,
                             Q_PROPERTY.namespace.eq(p.agent.name().getNamespace())
                                     .and(Q_PROPERTY.propertyName.eq(p.agent.name().getName()))));
    } else {
      assertEquals(2, q.delete(OWNER, p.agent.target()).run());
    }
    assertTotals(3, 3, 2);
  }

  @Test
  public void testDeleteByValue() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.legacyId.mk(mp[0], "id"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-2"));
    assertTotals(3, 3, 4);
    assertEquals(0, q.delete(OWNER, p.agent.target()).where(p.agent.eq("unknown-agent")).run());
    assertTotals(3, 3, 4);
    assertEquals(2, q.delete(OWNER, p.agent.target()).where(p.agent.eq("agent-2")).run());
    assertEquals(1, q.delete(OWNER, p.legacyId.target()).where(p.agent.eq("agent-1")).run());
    assertTotals(3, 3, 1);
    final RichAResult r = enrich(q.select(q.properties()).where(q.mediaPackageId(mp[0])).run());
    assertEquals("Media package " + mp[0] + " should still have the agent property",
                 p.agent.name(), r.getProperties().head2().getId().getFqn());
    assertEquals("The media package itself should be found", 1, r.getSize());
  }

  @Test
  public void testDeleteBySeries1() throws Exception {
    final String[] mp1 = createAndAddMediaPackagesSimple(3, 1, 1, Opt.some("123"));
    final String[] mp2 = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.agent.mk(mp1[0], "agent-1"));
    am.setProperty(p.agent.mk(mp1[1], "agent-2"));
    am.setProperty(p.agent.mk(mp2[0], "agent-5"));
    assertTotals(6, 6, 3);
    if (RUN_RAW_QUERIES) {
      assertEquals(2, delete(
              Q_PROPERTY,
              Q_PROPERTY.mediaPackageId.in(
                      new JPASubQuery().from(Q_SNAPSHOT).where(Q_SNAPSHOT.seriesId.eq("123")).list(Q_SNAPSHOT.mediaPackageId))));
    } else {
      assertEquals("Two properties should be deleted", 2, q.delete(OWNER, q.properties()).where(q.seriesId().eq("123")).run());
    }
    assertTotals(6, 6, 1);
    assertEquals(Value.mk("agent-5"), enrich(q.select(p.agent.target()).run()).getProperties().head2().getValue());
  }

  @Test
  public void testDeleteBySeries2() throws Exception {
    final Snapshot[] mp1 = createAndAddMediaPackages(3, 2, 2, Opt.some("series-1"));
    final Snapshot[] mp2 = createAndAddMediaPackages(3, 2, 2, Opt.some("series-2"));
    am.setProperty(Properties.mkProperty(p.agent, mp1[0], "agent-1"));
    am.setProperty(Properties.mkProperty(p.agent, mp2[1], "agent-2"));
    am.setProperty(Properties.mkProperty(p.agent, mp2[2], "agent-1"));
    assertTotals(12, 12, 3);
    assertEquals("One property should be deleted",
                 1, q.delete(OWNER, q.properties()).where(q.seriesId().eq("series-1")).run());
    assertTotals(12, 12, 2);
    assertEquals("One property should be deleted",
                 1, q.delete(OWNER, q.properties()).where(q.seriesId().eq("series-2").and(q.mediaPackageId(mp2[1].getMediaPackage().getIdentifier().toString()))).run());
    assertTotals(12, 12, 1);
    assertEquals(Value.mk("agent-1"), enrich(q.select(p.agent.target()).run()).getProperties().head2().getValue());
  }

  @Test
  public void testDeleteByMediaPackage() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 2, 2);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.agent.mk(mp[2], "agent-5"));
    assertTotals(6, 6, 3);
    assertEquals("One property should be deleted", 1, q.delete(OWNER, q.properties()).where(q.mediaPackageId(mp[0])).run());
    assertTotals(6, 6, 2);
    assertEquals("One property should be deleted", 1, q.delete(OWNER, q.properties()).where(q.mediaPackageId(mp[1])).run());
    assertTotals(6, 6, 1);
    assertEquals(Value.mk("agent-5"), enrich(q.select(p.agent.target()).run()).getProperties().head2().getValue());
  }

  @Test
  public void testDeleteByOrganization() throws Exception {
    final Snapshot[] mp1 = createAndAddMediaPackages(3, 2, 2, Opt.some("series-1"));
    final Snapshot[] mp2 = createAndAddMediaPackages(3, 2, 2, Opt.some("series-2"));
    am.setProperty(Properties.mkProperty(p.agent, mp1[0], "agent-1"));
    am.setProperty(Properties.mkProperty(p.agent, mp2[1], "agent-2"));
    am.setProperty(Properties.mkProperty(p.agent, mp2[2], "agent-1"));
    assertTotals(12, 12, 3);
    assertEquals("Three property should be deleted",
                 3, q.delete(OWNER, q.properties()).where(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID)).run());
    assertTotals(12, 12, 0);
  }

  @Test
  public void testDeleteAllByMixedPredicates() throws Exception {
    final Snapshot[] mpSeries1 = createAndAddMediaPackages(3, 2, 2, Opt.some("series-1"));
    final Snapshot[] mpSeries2 = createAndAddMediaPackages(3, 1, 1, Opt.some("series-2"));
    final Snapshot[] mpNoSeries = createAndAddMediaPackages(3, 2, 2, Opt.<String>none());
    // set some properties
    am.setProperty(Properties.mkProperty(p.agent, mpSeries1[0], "agent-1"));
    am.setProperty(Properties.mkProperty(p.count, mpSeries1[0], 2L));
    //
    am.setProperty(Properties.mkProperty(p.agent, mpNoSeries[1], "agent-2"));
    //
    am.setProperty(Properties.mkProperty(p.agent, mpNoSeries[2], "agent-1"));
    am.setProperty(Properties.mkProperty(p.legacyId, mpNoSeries[2], "id"));
    //
    am.setProperty(Properties.mkProperty(p.agent, mpSeries2[0], "agent-4"));
    //
    assertTotals(15, 15, 6);
    assertEquals("All properties of mpSeries1[0] (2) should be deleted",
                 2, q.delete(OWNER, q.properties())
                         .where(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID)
                                        .and(p.agent.eq("agent-1")).and(q.seriesId().exists()))
                         .name("AND")
                         .run());
    assertTotals(15, 15, 4);
    assertEquals("All properties of mpNoSeries[2] (2) and mpNoSeries[1] (1) should be deleted",
                 3, q.delete(OWNER, q.properties())
                         .where(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID)
                                        .and(p.agent.eq("agent-1").or(p.agent.eq("agent-2"))))
                         .name("OR")
                         .run());
    assertTotals(15, 15, 1);
  }

  @Test
  public void testDeleteOfNamespaceByNonExistingProperty() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(2, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent"));
    am.setProperty(p.approved.mk(mp[0], true));
    am.setProperty(p.count.mk(mp[0], 1L));
    am.setProperty(p2.agent.mk(mp[0], "agent"));
    //
    am.setProperty(p.agent.mk(mp[1], "agent"));
    am.setProperty(p.approved.mk(mp[1], true));
    am.setProperty(p.legacyId.mk(mp[1], "id"));
    //
//    if (true) return;
    assertEquals("All properties of media package " + mp[1] + "should be deleted",
                 3, q.delete(OWNER, q.propertiesOf(p.namespace()))
                         .where(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID).and(p.agent.eq("agent")).and(p.count.notExists()))
                         .name("by non existing property")
                         .run());
    {
      final RichAResult r = enrich(q.select(q.properties()).where(q.mediaPackageId(mp[1])).run());
      assertEquals(0, r.countProperties());
    }
    {
      final RichAResult r = enrich(q.select(q.properties()).where(q.mediaPackageId(mp[0])).run());
      assertEquals("No properties should have been deleted from the other media package since it has the 'count' property set",
                   4, r.countProperties());
    }
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
    assertEquals(0L, Properties.removeProperties(am, OWNER, "unknown_org", mp[0], p.namespace()));
    assertEquals(0L, Properties.removeProperties(am, OWNER, DefaultOrganization.DEFAULT_ORGANIZATION_ID, "unknown-mp-id", p.namespace()));
    assertEquals(0L, Properties.removeProperties(am, OWNER, DefaultOrganization.DEFAULT_ORGANIZATION_ID, mp[0], "unknown-namespace"));
    assertEquals(3L, Properties.removeProperties(am, OWNER, DefaultOrganization.DEFAULT_ORGANIZATION_ID, mp[0], p.namespace()));
    assertEquals(1L, enrich(q.select(q.properties()).where(q.mediaPackageId(mp[0])).run()).countProperties());
    assertEquals(1L, Properties.removeProperties(am, OWNER, DefaultOrganization.DEFAULT_ORGANIZATION_ID, mp[0], p2.namespace()));
    assertEquals(3L, Properties.removeProperties(am, OWNER, DefaultOrganization.DEFAULT_ORGANIZATION_ID, mp[1], p.namespace()));
  }
}
