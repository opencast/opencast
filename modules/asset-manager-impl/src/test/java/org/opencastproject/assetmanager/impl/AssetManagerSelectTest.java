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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.security.api.DefaultOrganization;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// CHECKSTYLE:OFF
public class AssetManagerSelectTest extends AssetManagerTestBase {
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
    final Optional<Asset> asset = am.getAsset(version, mp.getIdentifier().toString(), mpe.getIdentifier());
    assertTrue("Asset should be found", asset.isPresent());
    assertEquals("Media package element part of the asset ID should equal the element's ID",
                 mpe.getIdentifier(), asset.get().getId().getMediaPackageElementId());
    assertEquals("Mime types should equal", mpe.getMimeType(), asset.get().getMimeType().get());
    assertFalse("Asset should not be found", am.getAsset(version, "id", "id").isPresent());
    // try to find the catalog of the media package by checksum
    final MediaPackage mpCopy = MediaPackageSupport.copy(mp);
    am.calcChecksumsForMediaPackageElements(AssetManagerImpl.assetsOnly(mpCopy));
    assertEquals("Media package should be set up with a single catalog", 1, mpCopy.getCatalogs().length);
    final String checksum = mpCopy.getCatalogs()[0].getChecksum().toString();
    assertTrue("Media package element should be retrievable by checksum", am.getDatabase().findAssetByChecksum(checksum).isPresent());
    // issue some queries
//    {
//      logger.info("Run a failing query");
//      assertEquals("The result should not contain any records", 0,
//                   q.select(q.snapshot())
//                           .where(q.mediaPackageId(mp.getIdentifier().toString()).and(q.availability(Availability.ONLINE)))
//                           .where(q.mediaPackageId("12"))
//                           .run().getSize());
//    }
//    {
//      logger.info("Run query to find snapshot");
//      final AResult r = q.select(q.snapshot())
//              .where(q.mediaPackageId(mp.getIdentifier().toString()).and(q.availability(Availability.ONLINE)))
//              .run();
//      assertEquals("The result set should contain exactly one record", 1, r.getSize());
//      assertEquals("The media package IDs should be equal", mp.getIdentifier().toString(), r.getRecords().stream().findFirst().get().getMediaPackageId());
//      assertTrue("The snapshot should be contained in the record", r.getRecords().stream().findFirst().get().getSnapshot().isPresent());
//      assertEquals("The media package IDs should be equal", mp.getIdentifier(), r.getRecords().stream().findFirst().get().getSnapshot().get().getMediaPackage().getIdentifier());
//    }
//    {
//      final AResult r = q.select().where(q.mediaPackageId(mp.getIdentifier().toString()).and(q.availability(Availability.ONLINE))).run();
//      assertEquals("The result should contain one record", 1, r.getSize());
//      assertTrue("The result should not contain a snapshot", r.getRecords().stream().findFirst().get().getSnapshot().isEmpty());
//    }
  }

  @Test
  public void testSelectProperties() throws Exception {
    final MediaPackage mp1 = mkMediaPackage();
    final MediaPackageElement mpe = mkCatalog();
    mp1.add(mpe);
    am.takeSnapshot(OWNER, mp1);
    assertEquals("No properties should be found", 0, am.selectProperties(mp1.getIdentifier().toString(), null).size());

    logger.info("Set property on first episode");
    am.setProperty(Property.mk(PropertyId.mk(mp1.getIdentifier().toString(), "org.opencastproject.service", "count"), Value.mk(10L)));
    assertEquals("One property should be found", 1, am.selectProperties(mp1.getIdentifier().toString(), null).size());

    logger.info("Add another media package with some properties of the same namespace");
    final MediaPackage mp2 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp2);
    am.setProperty(p.count(mp2.getIdentifier().toString(), 20L));
    am.setProperty(p.approved(mp2.getIdentifier().toString(), true));
    am.setProperty(p.start(mp2.getIdentifier().toString(), new Date()));
    logger.info("Add a 3rd media package without any properties");
    am.takeSnapshot(OWNER, mkMediaPackage(mkCatalog()));


    assertEquals("One property should be found", 1, am.selectProperties(mp1.getIdentifier().toString(), null).size());
    assertEquals("Three properties should be found", 3, am.selectProperties(mp2.getIdentifier().toString(), null).size());

    assertEquals("One property should be found", 1, am.selectProperties(mp1.getIdentifier().toString(), "org.opencastproject.service").size());
    assertEquals("Three properties should be found", 3, am.selectProperties(mp2.getIdentifier().toString(), p.getNamespace()).size());
  }

  @Test
  public void testSelectAllPropertiesOfNamespace() throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp);
    am.setProperty(Property.mk(PropertyId.mk(mp.getIdentifier().toString(), "namespace-1", "prop-1"), Value.mk(true)));
    am.setProperty(Property.mk(PropertyId.mk(mp.getIdentifier().toString(), "namespace-2", "prop-2"), Value.mk("value-2")));
    {
      List<Property> properties = am.selectProperties(mp.getIdentifier().toString(), "namespace-1");
      assertEquals("One property should be returned", 1, properties.size());
      assertEquals("Property of namespace-1 should be returned",
                   PropertyId.mk(mp.getIdentifier().toString(), "namespace-1", "prop-1"),
          properties.stream().findFirst().get().getId());
    }
  }

  @Test
  public void testSelectSnapshotAndProperties() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp1);

    List<Snapshot> snapshots = am.getSnapshotsById(mp1.getIdentifier().toString());
    List<Property> properties = am.selectProperties(mp1.getIdentifier().toString(), null);
    assertEquals("One snapshot should be found", 1, snapshots.size());
    assertEquals("No properties should be found", 0, properties.size());

  }

  @Test
  public void testSelectByVersion() throws Exception {
    final MediaPackage mp1 = mkMediaPackage(mkCatalog());
    logger.info("Create 4 versions");
    am.takeSnapshot(OWNER, mp1);
    am.takeSnapshot(OWNER, mp1);
    am.takeSnapshot(OWNER, mp1);
    am.takeSnapshot(OWNER, mp1);
    assertEquals("4 records should be found", 4, am.getSnapshotsById(mp1.getIdentifier().toString()).size());

    final Version latest;
    List<Snapshot> descSnapshots = am.getSnapshotsByIdOrderedByVersion(mp1.getIdentifier().toString(), false);
    latest = descSnapshots.get(0).getVersion();
    final Version first;
    List<Snapshot> ascSnapshots = am.getSnapshotsByIdOrderedByVersion(mp1.getIdentifier().toString(), true);
    first = ascSnapshots.get(0).getVersion();

    assertTrue("The first version should be older", first.isOlder(latest));
    assertTrue("The last version should be younger", latest.isYounger(first));
    assertFalse("The versions should not be equal", latest.equals(first));

    assertEquals("4 records should be found", 4, descSnapshots.size());
    assertEquals("4 records should be found", 4, ascSnapshots.size());
  }

  @Test
  public void testSelectBySeries() throws Exception {
    final MediaPackage mp = mkMediaPackage();
    logger.info("The series ID field of the media package links it to a series. Attached DublinCore catalogs are not relevant.");
    mp.setSeries("series-1");
    am.takeSnapshot(OWNER, mp);

    assertEquals(1, am.getLatestSnapshotsBySeriesId("series-1").size());
    assertEquals(0, am.getLatestSnapshotsBySeriesId("series-2").size());
  }

  // TODO: Write more tests related to getting things from the asset manager
}
