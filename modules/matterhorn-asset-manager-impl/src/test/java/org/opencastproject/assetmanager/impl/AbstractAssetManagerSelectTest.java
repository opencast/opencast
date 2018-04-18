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

import static com.entwinemedia.fn.Stream.$;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.assetmanager.api.fn.ARecords.getProperties;
import static org.opencastproject.assetmanager.api.fn.ARecords.getSnapshot;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.ARecords;
import org.opencastproject.assetmanager.api.fn.Properties;
import org.opencastproject.assetmanager.api.fn.Snapshots;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.security.api.DefaultOrganization;

import com.entwinemedia.fn.FnX;
import com.entwinemedia.fn.P2;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Date;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// CHECKSTYLE:OFF
public class AbstractAssetManagerSelectTest extends AbstractAssetManagerTestBase {
  @Test
  public void testSelectSnapshots() throws Exception {
    final MediaPackage mp = mkMediaPackage();
    final MediaPackageElement mpe = mkCatalog();
    mp.add(mpe);
    final Snapshot snapshot = am.takeSnapshot(OWNER, mp);
    final Version version = snapshot.getVersion();
    assertThat("Archival date should not be in the future", snapshot.getArchivalDate(), lessThanOrEqualTo(new Date()));
    assertThat("Snapshot should be available", snapshot.getAvailability(), equalTo(Availability.ONLINE));
    assertThat("Snapshot should belong to the default organization",
               snapshot.getOrganizationId(), equalTo(DefaultOrganization.DEFAULT_ORGANIZATION_ID));
    final Opt<Asset> asset = am.getAsset(version, mp.getIdentifier().toString(), mpe.getIdentifier());
    assertTrue("Asset should be found", asset.isSome());
    assertEquals("Media package element part of the asset ID should equal the element's ID",
                 mpe.getIdentifier(), asset.get().getId().getMediaPackageElementId());
    assertEquals("Mime types should equal", mpe.getMimeType(), asset.get().getMimeType().get());
    assertFalse("Asset should not be found", am.getAsset(version, "id", "id").isSome());
    // try to find the catalog of the media package by checksum
    final MediaPackage mpCopy = MediaPackageSupport.copy(mp);
    am.calcChecksumsForMediaPackageElements(AbstractAssetManager.assetsOnly(mpCopy));
    assertEquals("Media package should be set up with a single catalog", 1, mpCopy.getCatalogs().length);
    final String checksum = mpCopy.getCatalogs()[0].getChecksum().toString();
    assertTrue("Media package element should be retrievable by checksum", am.getDb().findAssetByChecksum(checksum).isSome());
    // issue some queries
    {
      logger.info("Run a failing query");
      assertEquals("The result should not contain any records", 0,
                   q.select(q.snapshot())
                           .where(q.mediaPackageId(mp.getIdentifier().toString()).and(q.availability(Availability.ONLINE)))
                           .where(q.mediaPackageId("12"))
                           .run().getSize());
    }
    {
      logger.info("Run query to find snapshot");
      final AResult r = q.select(q.snapshot())
              .where(q.mediaPackageId(mp.getIdentifier().toString()).and(q.availability(Availability.ONLINE)))
              .run();
      assertEquals("The result set should contain exactly one record", 1, r.getSize());
      assertEquals("The media package IDs should be equal", mp.getIdentifier().toString(), r.getRecords().head2().getMediaPackageId());
      assertTrue("The snapshot should be contained in the record", r.getRecords().head2().getSnapshot().isSome());
      assertEquals("The media package IDs should be equal", mp.getIdentifier(), r.getRecords().head2().getSnapshot().get().getMediaPackage().getIdentifier());
    }
    {
      final AResult r = q.select().where(q.mediaPackageId(mp.getIdentifier().toString()).and(q.availability(Availability.ONLINE))).run();
      assertEquals("The result should contain one record", 1, r.getSize());
      assertTrue("The result should not contain a snapshot", r.getRecords().head2().getSnapshot().isNone());
    }
  }

  @Test
  public void testSelectSnapshotByProperty() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.approved.mk(mp[0], true));
    final RichAResult r = enrich(q.select(q.snapshot()).where(q.hasPropertiesOf(p.namespace())).run());
    assertEquals("The result set should contain one record", 1, r.getSize());
    assertEquals("The result set should contain one snapshot", 1, r.countSnapshots());
    assertEquals("The result set should contain media package " + mp[0], mp[0], r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
  }

  @Test
  public void testSelectSnapshotByProperties() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(3, 1, 1);
    am.setProperty(p.approved.mk(mp[0], true));
    am.setProperty(p.count.mk(mp[0], 1L));
    final RichAResult r = enrich(q.select(q.snapshot()).where(q.hasProperties()).run());
    assertEquals("The result set should contain one record", 1, r.getSize());
    assertEquals("The result set should contain one snapshot", 1, r.countSnapshots());
    assertEquals("The result set should contain media package " + mp[0], mp[0], r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
  }

  @Test
  public void testSelectProperties() throws Exception {
    final MediaPackage mp1 = mkMediaPackage();
    final MediaPackageElement mpe = mkCatalog();
    mp1.add(mpe);
    am.takeSnapshot(OWNER, mp1);
    //
    assertEquals("No records should be found", 0, q.select(q.snapshot()).where(q.hasPropertiesOf("org.opencastproject.service")).run().getSize());
    //
    logger.info("Set property on first episode");
    am.setProperty(Property.mk(PropertyId.mk(mp1.getIdentifier().toString(), "org.opencastproject.service", "count"), Value.mk(10L)));
    assertEquals("One record should be found", 1, q.select(q.snapshot()).where(q.hasPropertiesOf("org.opencastproject.service")).run().getSize());
    //
    logger.info("Add another media package with some properties of the same namespace");
    final MediaPackage mp2 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp2);
    am.setProperty(p.count.mk(mp2.getIdentifier().toString(), 20L));
    am.setProperty(p.approved.mk(mp2.getIdentifier().toString(), true));
    am.setProperty(p.start.mk(mp2.getIdentifier().toString(), new Date()));
    //
    logger.info("Add a 3rd media package without any properties");
    am.takeSnapshot(OWNER, mkMediaPackage(mkCatalog()));
    //
    {
      final AResult r = q.select(q.snapshot(), q.nothing()).where(p.hasPropertiesOfNamespace()).run();
      assertEquals("Two records should be found", 2, r.getSize());
      logger.info(r.getSearchTime() + "ms");
    }
    {
      final AResult r = q.select(q.snapshot()).where(p.hasPropertiesOfNamespace()).run();
      assertEquals("Two snapshots should be found", 2, r.getRecords().bind(getSnapshot).toList().size());
      logger.info(r.getSearchTime() + "ms");
    }
    {
      final AResult r = q.select(q.snapshot(), p.allProperties()).where(p.hasPropertiesOfNamespace()).run();
      assertEquals("Two snapshots should be found", 2, r.getRecords().bind(getSnapshot).toList().size());
      logger.info(r.getSearchTime() + "ms");
    }
    {
      final AResult r = q.select(q.snapshot(), p.allProperties()).where(p.hasPropertiesOfNamespace()).run();
      assertEquals("Two records with properties of the defined namespace should be found", 2, r.getSize());
      logger.info(r.getSearchTime() + "ms");
    }
    {
      final AResult r = q.select(q.snapshot(), p.allProperties()).run();
      assertEquals("Three records should be found in total", 3, r.getSize());
      logger.info(r.getSearchTime() + "ms");
    }
    {
      final AResult r = q.select(q.snapshot(), p.allProperties()).where(p.count.le(5L)).run();
      assertEquals("No records should be found in total", 0, r.getSize());
    }
    {
      final AResult r = q.select(q.snapshot(), p.allProperties()).where(p.count.gt(5L)).run();
      assertEquals("No records should be found in total", 2, r.getSize());
    }
  }

  @Test
  public void testSelectParticularProperties() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp1);
    // set some properties
    am.setProperty(p.count.mk(mp1.getIdentifier().toString(), 10L));
    am.setProperty(p.approved.mk(mp1.getIdentifier().toString(), true));
    {
      final AResult r = q.select(p.count.target()).run();
      assertEquals("One record should be selected", 1, r.getSize());
      assertEquals("No snapshot should be selected", 0, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("One property should be selected", 1, r.getRecords().bind(getProperties).toList().size());
    }
    {
      final AResult r = q.select(q.properties(
              PropertyName.mk(p.namespace(), p.count.name().getName()),
              PropertyName.mk(p.namespace(), "unkown-property"))).run();
      assertEquals("One record should be selected", 1, r.getSize());
      assertEquals("No snapshot should be selected", 0, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("One property should be selected", 1, r.getRecords().bind(getProperties).toList().size());
    }
  }

  @Test
  public void testSelectMultipleProperties() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp1);
    am.setProperty(p.approved.mk(mp1.getIdentifier().toString(), true));
    am.setProperty(p.count.mk(mp1.getIdentifier().toString(), 10L));
    am.setProperty(p.start.mk(mp1.getIdentifier().toString(), new Date()));
    assertEquals("Querying all properties, three should be found",
                 3, sizeOf(q.select(q.properties()).run().getRecords().bind(getProperties)));
    assertEquals("Querying 'approved' and 'count', both should be found",
                 2, sizeOf(q.select(p.approved.target(), p.count.target())
                                   .where(q.mediaPackageId(mp1.getIdentifier().toString()))
                                   .run().getRecords().bind(getProperties)));
    assertEquals("Querying 'approved' and 'count' in a different style, both should be found again",
                 2, sizeOf(q.select(q.properties(p.approved.name(), p.count.name()))
                                   .where(q.mediaPackageId(mp1.getIdentifier().toString()))
                                   .run().getRecords().bind(getProperties)));
    logger.info("Add another media package with some properties");
    final MediaPackage mp2 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp2);
    am.setProperty(p.approved.mk(mp2.getIdentifier().toString(), true));
    am.setProperty(p.start.mk(mp2.getIdentifier().toString(), new Date()));
    assertEquals("Querying all properties, five should be found",
                 5, sizeOf(q.select(q.properties()).run().getRecords().bind(getProperties)));
    assertEquals("Querying target and count of the first media package, both should be found",
                 2, sizeOf(q.select(p.approved.target(), p.count.target())
                                   .where(q.mediaPackageId(mp1.getIdentifier().toString()))
                                   .run().getRecords().bind(getProperties)));
    assertEquals("Querying target and count of all media packages, 3 properties should be found",
                 3, sizeOf(q.select(q.properties(p.approved.name(), p.count.name()))
                                   .run().getRecords().bind(getProperties)));
  }

  @Test
  public void testSelectMultiplePropertyNamespaces() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    final String mp1Id = mp1.getIdentifier().toString();
    am.takeSnapshot(OWNER, mp1);
    am.setProperty(p.approved.mk(mp1Id, true));
    am.setProperty(p.count.mk(mp1Id, 10L));
    am.setProperty(p.start.mk(mp1Id, new Date()));
    //
    am.setProperty(Property.mk(PropertyId.mk(mp1Id, "org.opencastproject.scheduler", "start"), Value.mk(new Date())));
    am.setProperty(Property.mk(PropertyId.mk(mp1Id, "org.opencastproject.scheduler", "end"), Value.mk(new Date())));
    //
    {
      final RichAResult r = enrich(q.select(p.allProperties(), q.propertiesOf("org.opencastproject.scheduler")).run());
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("All five properties should be found", 5, r.countProperties());
    }
    {
      final RichAResult r = enrich(q.select(q.propertiesOf(p.namespace())).run());
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("No snapshot has been selected", 0, r.countSnapshots());
      assertEquals("Three properties should be found", 3, r.countProperties());
    }
    {
      final RichAResult r = enrich(q.select(q.propertiesOf("org.opencastproject.scheduler")).where(q.mediaPackageId(mp1Id)).run());
      assertEquals(2, r.countProperties());
    }
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(p.hasPropertiesOfNamespace()).run());
      assertEquals(1, r.getSize());
    }
    {
      final RichAResult r = enrich(q.select(p.allProperties(), q.propertiesOf("org.opencastproject.scheduler")).run());
      assertEquals(1, r.getSize());
      assertEquals(5, r.countProperties());
    }
  }

  @Test
  public void testSelectMultiplePropertyNamespaces_CERV_695() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(5, 1, 1);
    //
    logger.info("Add properties to only one of the media packages");
    am.setProperty(p.approved.mk(mp[0], true));
    am.setProperty(p.count.mk(mp[0], 10L));
    am.setProperty(p.start.mk(mp[0], new Date()));
    //
    am.setProperty(Property.mk(PropertyId.mk(mp[0], "org.opencastproject.scheduler", "start"), Value.mk(new Date())));
    am.setProperty(Property.mk(PropertyId.mk(mp[0], "org.opencastproject.scheduler", "end"), Value.mk(new Date())));
    am.setProperty(Property.mk(PropertyId.mk(mp[0], "org.opencastproject.scheduler", "enabled"), Value.mk(true)));
    //
    am.setProperty(Property.mk(PropertyId.mk(mp[0], "org.opencastproject.annotation", "annotation"), Value.mk("some text")));
    //
    am.setProperty(Property.mk(PropertyId.mk(mp[0], "org.opencastproject.distribution", "channel"), Value.mk("engage")));
    {
      final RichAResult r = enrich(q.select(
              q.snapshot(),
              p.count.target(), p.approved.target(),
              q.propertiesOf("org.opencastproject.annotation", "org.opencastproject.scheduler"),
              p.legacyId.target()).run());
      assertEquals("Five records should be found since there is no filtering", 5, r.getSize());
      assertEquals("There should be 6 properties total", 6, r.countProperties());
      assertEquals("There should be 2 properties in the 'service' namespace, 'count' and 'approved'",
                   2, sizeOf(r.getProperties().filter(Properties.byNamespace(p.namespace()))));
      assertEquals("There should be 3 properties in the 'scheduler' namespace",
                   3, sizeOf(r.getProperties().filter(Properties.byNamespace("org.opencastproject.scheduler"))));
      assertEquals("There should be 1 property in the 'annotation' namespace",
                   1, sizeOf(r.getProperties().filter(Properties.byNamespace("org.opencastproject.annotation"))));
      assertEquals("There should be 5 snapshots", 5, r.countSnapshots());
      assertEquals("Only one record should have properties", 1, sizeOf(r.getRecords().filter(ARecords.hasProperties)));
      assertEquals("The record with properties should have media package ID " + mp[0],
              mp[0], r.getRecords().filter(ARecords.hasProperties).map(ARecords.getMediaPackageId).head().get());
    }
    {
      final RichAResult r = enrich(
              q.select(
                      q.snapshot(),
                      p.count.target(),
                      p.approved.target(),
                      q.propertiesOf("org.opencastproject.annotation", "org.opencastproject.scheduler"), p.legacyId.target())
                      .where(
                              q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID)
                                      .and(q.mediaPackageId(mp[0]))
                                      .and(q.version().isLatest()))
                      .run());
      assertEquals("Only one record should be found since a media package predicate is given",
                   1, r.getSize());
    }
  }

  @Test
  public void testSelectWithMultiplePredicates_CERV_696() {
    final String[] mp = createAndAddMediaPackagesSimple(2, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-id"));
    final Predicate p1 = q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID).and(q.version().isLatest());
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(p1).run());
      assertEquals("Two records should be found", 2, r.getSize());
      assertEquals("Two snapshots should be contained", 2, r.countSnapshots());
      assertEquals("No properties should be found since they haven't been selected", 0, r.countProperties());
    }
    // But when adding an additional predicate on a property (e.g. agent):
    final Predicate p2 = p1.and(p.agent.eq("agent-id"));
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(p2).run());
      assertEquals("Only one record should be found now", 1, r.getSize());
      assertEquals("One snapshots should be found", 1, r.countSnapshots());
      assertEquals("No properties should be found since they haven't been selected", 0, r.countProperties());
    }
  }

  @Test
  @Parameters
  public void testSelectPropertiesWithPropertyPredicate_CERV_698(Target allProperties) {
    final String[] mp = createAndAddMediaPackagesSimple(2, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-id"));
    am.setProperty(p.approved.mk(mp[0], true));
    am.setProperty(p.count.mk(mp[0], 1L));
    am.setProperty(p.start.mk(mp[0], new Date(0)));
    {
      logger.info("No predicate");
      final RichAResult r = enrich(q.select(allProperties).run());
      assertEquals("Two records should match", 2, r.getSize());
      assertEquals("No snapshots should be contained in the records", 0, r.countSnapshots());
      assertEquals("Four properties should be contained in the records", 4, r.countProperties());
      assertEquals("All four properties should belong to one media package",
                   4, sizeOf(r.getProperties().filter(Properties.byMediaPackageId(mp[0]))));
    }
    {
      logger.info("'approved' == true");
      final RichAResult r = enrich(q.select(allProperties).where(p.approved.eq(true)).run());
      assertEquals("Only one record should match", 1, r.getSize());
      assertEquals("Four properties should be contained", 4, r.countProperties());
    }
    if (true) return;
    {
      logger.info("'approved' == true or 'agent' == 'agent-id'");
      final RichAResult r = enrich(q.select(allProperties).where(p.approved.eq(true).or(p.agent.eq("agent-id"))).run());
      assertEquals("Only one record should match", 1, r.getSize());
      assertEquals("Four properties should be contained", 4, r.countProperties());
    }
    {
      logger.info("'approved' exists");
      final RichAResult r = enrich(q.select(allProperties).where(p.approved.exists()).run());
      assertEquals("Only one record should match", 1, r.getSize());
      assertEquals("Four properties should be contained", 4, r.countProperties());
      assertEquals("The matching record should represent the first media package",
                   mp[0], r.getRecords().map(ARecords.getMediaPackageId).head().get());
    }
    {
      logger.info("'approved' notExists");
      final RichAResult r = enrich(q.select(allProperties).where(p.approved.notExists()).run());
      assertEquals("Only one record should match", 1, r.getSize());
      assertEquals("The found record should represent the second media package since it does not have an 'approved' property",
                   mp[1], r.getRecords().map(ARecords.getMediaPackageId).head().get());
    }
  }

  // parameter provider for above test
  private Object parametersForTestSelectPropertiesWithPropertyPredicate_CERV_698() throws Exception {
    setUp();
    return $a(q.properties(), q.propertiesOf(), p.allProperties());
  }

  @Test
  public void testSelectAllPropertiesOfNamespace() throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp);
    am.setProperty(Property.mk(PropertyId.mk(mp.getIdentifier().toString(), "namespace-1", "prop-1"), Value.mk(true)));
    am.setProperty(Property.mk(PropertyId.mk(mp.getIdentifier().toString(), "namespace-2", "prop-2"), Value.mk("value-2")));
    {
      final RichAResult r = enrich(q.select(q.propertiesOf("namespace-1")).run());
      assertEquals("One record should be returned", 1, r.getSize());
      assertEquals("No snapshots should be returned", 0, r.countSnapshots());
      assertEquals("One property should be returned", 1, r.countProperties());
      assertEquals("Property of namespace-1 should be returned",
                   PropertyId.mk(mp.getIdentifier().toString(), "namespace-1", "prop-1"),
                   r.getProperties().head2().getId());
    }
    {
      final RichAResult r = enrich(q.select(q.propertiesOf("namespace-1")).where(q.hasPropertiesOf("namespace-2")).run());
      assertEquals("One record should be returned", 1, r.getSize());
      assertEquals("No snapshots should be returned", 0, r.countSnapshots());
      assertEquals("One property should be returned", 1, r.countProperties());
      assertEquals("Property of namespace-1 should be returned",
                   PropertyId.mk(mp.getIdentifier().toString(), "namespace-1", "prop-1"),
                   r.getProperties().head2().getId());
    }
  }

  @Test
  public void testSelectSnapshotAndProperties() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp1);
    // only select snapshots
    {
      final AResult r = q.select(q.snapshot()).run();
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("One snapshot should be found", 1, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("No properties should be found", 0, r.getRecords().bind(getProperties).toList().size());
    }
    // select snapshots and properties
    {
      final AResult r = q.select(q.snapshot(), p.allProperties()).run();
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("One snapshot should be found", 1, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("No properties should be found", 0, r.getRecords().bind(getProperties).toList().size());
    }
  }

  @Test
  public void testSelectByArchivedDate() throws Exception {
    final Date backThen = DateTime.now().minusSeconds(5).withMillisOfSecond(0).toDate();
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp1);
    final MediaPackage mp2 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp2);
    final Date now = DateTime.now().plusSeconds(1).withMillisOfSecond(0).toDate();
    assertEquals("Two records should be found", 2, q.select(q.snapshot()).where(q.archived().le(now)).run().getSize());
    assertEquals("No record should be found", 0, q.select(q.snapshot()).where(q.archived().gt(now)).run().getSize());
    assertEquals("Two records should be found", 2, q.select(q.snapshot()).where(q.archived().ge(backThen)).run().getSize());
    assertEquals("No record should be found", 0, q.select(q.snapshot()).where(q.archived().lt(backThen)).run().getSize());
  }

  @Test
  public void testSelectByVersion() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    logger.info("Create 4 versions");
    am.takeSnapshot(OWNER, mp1);
    am.takeSnapshot(OWNER, mp1);
    am.takeSnapshot(OWNER, mp1);
    am.takeSnapshot(OWNER, mp1);
    assertEquals("4 records should be found", 4, q.select(q.snapshot()).run().getSize());
    assertEquals("4 records should be found", 4, q.select().run().getSize());
    final Version latest;
    {
      AResult r = q.select(q.snapshot()).where(q.version().isLatest()).run();
      assertEquals("1 latest record should be found", 1, r.getSize());
      latest = r.getRecords().head2().getSnapshot().get().getVersion();
    }
    final Version first;
    {
      AResult r = q.select(q.snapshot()).where(q.version().isFirst()).run();
      assertEquals("1 first record should be found", 1, r.getSize());
      first = r.getRecords().head2().getSnapshot().get().getVersion();
    }
    assertTrue("The first version should be older", first.isOlder(latest));
    assertTrue("The last version should be younger", latest.isYounger(first));
    assertFalse("The versions should not be equal", latest.equals(first));
    //
    assertEquals("Three older records should be found", 3, q.select(q.snapshot()).where(q.version().lt(latest)).run().getSize());
    assertEquals("4 records should be found", 4, q.select(q.snapshot()).where(q.version().le(latest)).run().getSize());
    assertEquals("Three younger records should be found", 3, q.select(q.snapshot()).where(q.version().gt(first)).run().getSize());
    assertEquals("4 records should be found", 4, q.select(q.snapshot()).where(q.version().ge(first)).run().getSize());
    assertEquals("2 intermediate records should be found",
                 2, q.select(q.snapshot()).where(q.version().gt(first).and(q.version().lt(latest))).run().getSize());
    //
    logger.info("Now add another media package");
    am.takeSnapshot(OWNER, mkMediaPackage(mkCatalog()));
    assertEquals("Three older records should be found",
                 3, q.select(q.snapshot()).where(q.version().lt(latest).and(q.mediaPackageId(mp1.getIdentifier().toString()))).run().getSize());
    assertEquals("Two latest versions should be found",
                 2, q.select(q.snapshot()).where(q.version().isLatest()).run().getSize());
    assertEquals("Two first versions should be found",
                 2, q.select(q.snapshot()).where(q.version().isFirst()).run().getSize());
  }

  private Pred<Snapshot> findSnapshot(final String mpId, final Version version) {
    return new Pred<Snapshot>() {
      @Override public Boolean apply(Snapshot snapshot) {
        return snapshot.getMediaPackage().getIdentifier().toString().equals(mpId) && snapshot.getVersion().equals(version);
      }
    };
  }

  /**
   * CERV-1047
   */
  @Test
  public void testSelectBySnapshotFieldEqualsProperty() throws Exception {
    final Snapshot[] mp1 = createAndAddMediaPackages(3, 2, 2, Opt.some("series-1"));
    // fill asset manager a bit
    final Snapshot[] mp2 = createAndAddMediaPackages(3, 1, 1, Opt.some("series-2"));
    final Snapshot[] mp3 = createAndAddMediaPackages(3, 1, 1, Opt.<String>none());
    am.setProperty(Properties.mkProperty(p.seriesId, mp1[0], "series-1"));
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.seriesId().eq(p.seriesId)).run());
      assertEquals("Query should return all versions which are two", 2, r.getSize());
      assertEquals(mp1[0].getMediaPackage().getIdentifier().toString(),
                   r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
    }
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.seriesId().eq(p.seriesId).and(q.version().isLatest())).run());
      assertEquals("Query should return only the latest version", 1, r.getSize());
      assertEquals(mp1[0].getMediaPackage().getIdentifier().toString(),
                   r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
    }
    // now set a seriesId property to a media package that belongs to "series-2"
    am.setProperty(Properties.mkProperty(p.seriesId, mp2[0], "series-1"));
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.seriesId().eq(p.seriesId).and(q.version().isLatest())).run());
      assertEquals("Query should not return the media package of series-2", 1, r.getSize());
      assertEquals(mp1[0].getMediaPackage().getIdentifier().toString(),
                   r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
    }
    {
      // do a greater than comparison
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.seriesId().gt(p.seriesId).and(q.version().isLatest())).run());
      assertEquals("Query should return the media package of series-2", 1, r.getSize());
      assertEquals(mp2[0].getMediaPackage().getIdentifier().toString(),
                   r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
    }
  }

  /**
   * CERV-1047
   * Test for selecting snapshots by version which is stored in a property of the media package.
   */
  @Test
  public void testSelectBySnapshotVersionEqualsVersionProperty() throws Exception {
    final Snapshot[] mp1 = createAndAddMediaPackages(3, 3, 3, Opt.<String>none());
    am.setProperty(Properties.mkProperty(p.versionId, mp1[0], mp1[0].getVersion()));
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.version().eq(p.versionId)).run());
      assertEquals("Query should return only the first version", 1, r.getSize());
      assertEquals(mp1[0].getMediaPackage().getIdentifier().toString(),
                   r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
    }
    // set version to version of second snapshot
    am.setProperty(Properties.mkProperty(p.versionId, mp1[0], mp1[1].getVersion()));
    {
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.version().eq(p.versionId)).run());
      assertEquals("Query should now return only the second version", 1, r.getSize());
      assertEquals(mp1[1].getMediaPackage().getIdentifier().toString(),
                   r.getSnapshots().head2().getMediaPackage().getIdentifier().toString());
    }
  }

  @Test
  public void testSelect1() throws Exception {
    logger.info("Testing a select that caused an issue.");
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp);
    final String mpId = mp.getIdentifier().toString();
    RichAResult r = enrich(q.select(p.count.target(), p.approved.target())
                                   .where(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID)
                                                  .and(q.mediaPackageId(mpId).and(q.version().isLatest())))
                                   .run());
    assertEquals(1, r.getSize());
  }

  @Ignore
  @Test
  public void testManyMediaPackages() throws Exception {
    final long tStart = System.nanoTime();
    final int mpCount = 1000;
    final Stream<P2<String, Integer>> inserts = Stream.cont(inc()).take(mpCount).map(new FnX<Integer, P2<String, Integer>>() {
      @Override public P2<String, Integer> applyX(Integer ignore) throws Exception {
        final MediaPackage mp = mkMediaPackage(mkCatalog());
        final String mpId = mp.getIdentifier().toString();
        final int versions = (int) (Math.random() * 10.0 + 1.0);
        for (int i = 0; i < Math.random() * 10 + 1; i++) {
          am.takeSnapshot(OWNER, mp);
        }
        // set the legacy ID property
        am.setProperty(p.legacyId.mk(mpId, "legacyId=" + mpId));
        return Products.E.p2(mp.getIdentifier().toString(), versions);
      }
    }).eval();
    final long tInserted = System.nanoTime();
    {
      RichAResult r = enrich(q.select(q.snapshot()).where(q.version().isLatest()).run());
      assertEquals(mpCount, r.getSize());
      assertEquals(mpCount, r.countSnapshots());
    }
    for (final P2<String, Integer> insert : inserts) {
      final RichAResult r = enrich(q.select(p.legacyId.target()).where(q.mediaPackageId(insert.get1()).and(q.version().isLatest())).run());
      assertEquals(1, r.getSize());
      assertEquals(0, r.countSnapshots());
      assertEquals(1, r.countProperties());
      assertEquals("legacyId=" + r.getRecords().head().get().getMediaPackageId(), r.getProperties().head().get().getValue().get(Value.STRING));
    }
    final long tQueried = System.nanoTime();
    logger.info("Insertion ms " + ((tInserted - tStart) / 1000000));
    logger.info("Queries ms " + ((tQueried - tInserted) / 1000000));
  }

  @Test
  public void testPredicateZero() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp1);
    {
      final AResult r = q.select(q.snapshot()).run();
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("One snapshot should be found", 1, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("No properties should be found", 0, r.getRecords().bind(getProperties).toList().size());
    }
    {
      final AResult r = q.select(q.snapshot()).where(q.always()).run();
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("One snapshot should be found", 1, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("No properties should be found", 0, r.getRecords().bind(getProperties).toList().size());
    }
    {
      final AResult r = q.select(q.snapshot()).where(q.always().not()).run();
      assertEquals("No record should be found", 0, r.getSize());
    }
    {
      logger.info("Show an example use case of predicate's zero element");
      final boolean selectAll = true;
      final AResult r = q.select(q.snapshot()).where(selectAll ? q.always() : q.mediaPackageId("bla")).run();
      assertEquals("One record should be found", 1, r.getSize());
      assertEquals("One snapshot should be found", 1, r.getRecords().bind(getSnapshot).toList().size());
      assertEquals("No properties should be found", 0, r.getRecords().bind(getProperties).toList().size());
    }
  }

  @Test
  public void testSelectByEndAndStartProperty() throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    final String mpId = mp.getIdentifier().toString();
    am.takeSnapshot(OWNER, mp);
    final Date d0 = new Date(0);
    final Date d1 = new Date(1);
    final Date d3 = new Date(3);
    assertTrue("The property should be set", am.setProperty(p.start.mk(mpId, d1)));
    assertTrue("The property should be set", am.setProperty(p.end.mk(mpId, d3)));

    logger.info("Select all media package's end from d0 and start to d3");
    {
      AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(p.start.le(d3)).and(p.end.ge(d0))).run();
      assertEquals("One record should be found", 1, r.getSize());
    }
  }

  @Test
  public void testSelectByMultipleProperties() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(5, 1, 1);
    final Version v3 = am.toVersion("3").get();
    am.setProperty(p.agent.mk(mp[0], "agent"));
    am.setProperty(p.versionId.mk(mp[0], v3));
    am.setProperty(p.approved.mk(mp[0], true));
    assertThat(
            q.select(p.agent.target(), p.approved.target()).where(p.approved.eq(true).and(p.versionId.eq(v3)))
                    .run().getRecords().head().get().getProperties().map(Properties.getValue),
            Matchers.containsInAnyOrder(equalTo((Value) Value.mk("agent")), equalTo((Value) Value.mk(true))));
  }

  @Test
  public void testComparisons() throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    final String mpId = mp.getIdentifier().toString();
    am.takeSnapshot(OWNER, mp);
    // set the milliseconds to 0 since MySQL (or the driver) rounds milliseconds to the next or previous
    // second which may cause subsequent tests (.le(now)) to fail.
    final Date now = DateTime.now().withMillisOfSecond(0).toDate();
    logger.info("now=" + now);
    // set up a property for each property type
    am.setProperty(p.start.mk(mpId, now));
    am.setProperty(p.agent.mk(mpId, "agent"));
    am.setProperty(p.approved.mk(mpId, true));
    am.setProperty(p.count.mk(mpId, 10L));
    {
      assertEquals(1, q.select(q.snapshot()).where(p.count.le(10L)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.count.ge(10L)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.count.eq(10L)).run().getSize());
      assertEquals(0, q.select(q.snapshot()).where(p.count.lt(10L)).run().getSize());
      assertEquals(0, q.select(q.snapshot()).where(p.count.gt(10L)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.count.lt(11L)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.count.gt(9L)).run().getSize());
    }
    {
      assertEquals(1, q.select(q.snapshot()).where(p.start.le(now)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.start.ge(now)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.start.eq(now)).run().getSize());
      assertEquals(0, q.select(q.snapshot()).where(p.start.lt(now)).run().getSize());
      assertEquals(0, q.select(q.snapshot()).where(p.start.gt(now)).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.start.lt(new Date(now.getTime() + 120000L))).run().getSize());
      assertEquals(1, q.select(q.snapshot()).where(p.start.gt(new Date(now.getTime() - 120000L))).run().getSize());
    }
  }

  @Test
  public void testSelectBySeries() throws Exception {
    final MediaPackage mp = mkMediaPackage();
    logger.info("The series ID field of the media package links it to a series. Attached DublinCore catalogs are not relevant.");
    mp.setSeries("series-1");
    am.takeSnapshot(OWNER, mp);
    assertEquals(1, q.select(q.snapshot()).where(q.seriesId().eq("series-1")).run().getSize());
    assertEquals(0, q.select(q.snapshot()).where(q.seriesId().eq("series-2")).run().getSize());
  }

  @Test
  public void testSelectWithoutTarget() throws Exception {
    final String[] mp = createAndAddMediaPackagesSimple(5, 1, 1);
    am.setProperty(p.agent.mk(mp[0], "agent-1"));
    am.setProperty(p.agent.mk(mp[1], "agent-2"));
    am.setProperty(p.count.mk(mp[2], 4L));
    assertEquals("Five records should be found", 5, q.select().run().getSize());
    for (final ARecord r : q.select().run()) {
      assertTrue("No snapshots should be included in the record", r.getSnapshot().isNone());
      assertTrue("No properties should be included in the record", r.getProperties().isEmpty());
    }
  }

  @Test
  public void testOrderBySnapshotField() throws Exception {
    final String[] mp1 = createAndAddMediaPackagesSimple(1, 1, 1, Opt.some("series-1"));
    final String[] mp2 = createAndAddMediaPackagesSimple(1, 1, 1, Opt.some("series-2"));
    final String[] mp3 = createAndAddMediaPackagesSimple(1, 1, 1, Opt.some("series-3"));
    {
      final List<String> orgAsc = enrich(q.select(q.snapshot()).orderBy(q.seriesId().asc()).run())
              .getSnapshots().bind(Snapshots.getSeriesId).toList();
      final List<String> orgDesc = enrich(q.select(q.snapshot()).orderBy(q.seriesId().desc()).run())
              .getSnapshots().bind(Snapshots.getSeriesId).toList();
      assertEquals($("series-1", "series-2", "series-3").toList(), orgAsc);
      assertEquals($("series-3", "series-2", "series-1").toList(), orgDesc);
    }
    {
      assertEquals(
              enrich(q.select(q.snapshot()).orderBy(q.archived().asc()).run())
                      .getSnapshots().map(Snapshots.getArchivalDate).toList(),
              enrich(q.select(q.snapshot()).orderBy(q.archived().desc()).run())
                      .getSnapshots().map(Snapshots.getArchivalDate).reverse().toList());
    }
    {
      assertEquals(
              enrich(q.select(q.snapshot()).orderBy(q.version().asc()).run())
                      .getSnapshots().map(Snapshots.getVersion).toList(),
              enrich(q.select(q.snapshot()).orderBy(q.version().desc()).run())
                      .getSnapshots().map(Snapshots.getVersion).reverse().toList());
    }
  }

  @Test
  public void testPaging() throws Exception {
    final String[] mp1 = createAndAddMediaPackagesSimple(5, 1, 1, Opt.some("series-1"));
    assertEquals(1, q.select().page(0, 1).where(q.seriesId().eq("series-1")).run().getSize());
    assertEquals(3, q.select().page(0, 3).where(q.seriesId().eq("series-1")).run().getSize());
    assertEquals(3, q.select().where(q.seriesId().eq("series-1")).page(0, 3).run().getSize());
    assertEquals(1, q.select().page(4, 5).where(q.seriesId().eq("series-1")).run().getSize());
    assertEquals(1, q.select().where(q.seriesId().eq("series-1")).page(4, 5).run().getSize());
  }
}
