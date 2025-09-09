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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.security.api.DefaultOrganization;

import org.junit.Test;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class AssetManagerBasicTest extends AssetManagerTestBase {
  @Test
  public void testAddToStore() {
    createAndAddMediaPackagesSimple(1, 1, 1);
    assertStoreSize(2);
    createAndAddMediaPackagesSimple(3, 1, 1);
    assertStoreSize(8);
  }

  @Test
  public void testVersioning() throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    final Version v1 = am.takeSnapshot(OWNER, mp).getVersion();
    final Version v2 = am.takeSnapshot(OWNER, mp).getVersion();
    assertTrue("First version must be older", v1.isOlder(v2));
    assertTrue("Second version must be younger", v2.isYounger(v1));
    assertTrue(v1.equals(v1));
    assertFalse(v1.equals(v2));
  }

  @Test
  public void testSerializeVersion() throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    final Version v1 = am.takeSnapshot(OWNER, mp).getVersion();
    assertTrue("Version should be ", am.toVersion(v1.toString()).isPresent());
    assertEquals(v1, am.toVersion(v1.toString()).get());
  }

  @Test
  public void testUnwrapException() {
    assertTrue(AssetManagerImpl.unwrapExceptionUntil(Exception.class, new AssetManagerException()).isPresent());
    assertThat(AssetManagerImpl.unwrapExceptionUntil(Exception.class, new AssetManagerException()).get(),
        instanceOf(AssetManagerException.class));
    assertEquals("error", AssetManagerImpl.unwrapExceptionUntil(
            AssetManagerException.class,
            new AssetManagerException("error")).get().getMessage());
    assertEquals("error", AssetManagerImpl.unwrapExceptionUntil(
            AssetManagerException.class,
            new Exception(new AssetManagerException("error"))).get().getMessage());
    assertEquals("wrapper", AssetManagerImpl.unwrapExceptionUntil(
            AssetManagerException.class,
            new AssetManagerException("wrapper", new AssetManagerException("error"))).get().getMessage());
  }

  @Test
  public void testSetAvailability() throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    final Version v1 = am.takeSnapshot(OWNER, mp).getVersion();
    am.setAvailability(v1, mp.getIdentifier().toString(), Availability.OFFLINE);
    Optional<Snapshot> optSnapshot = am.getSnapshotByMpIdOrgIdAndVersion(
        mp.getIdentifier().toString(),
        new DefaultOrganization().getId(),
        v1
    );
    Snapshot snapshot = optSnapshot.get();
    assertEquals("One offline snapshot should be found", Availability.OFFLINE, snapshot.getAvailability());
  }

  @Test
  public void testSetPropertyOnNonExistingMediaPackage() throws Exception {
    assertFalse("Property should not be stored since the referenced media package does not exist",
                am.setProperty(Property.mk(PropertyId.mk("id", "namespace", "name"),
                    Value.mk("value"))));
  }

  @Test
  public void testGetFileNameFromUrn() throws Exception {
    MediaPackageElement element = new MediaPackageElementBuilderImpl().newElement(Type.Track,
            MediaPackageElements.PRESENTER_SOURCE);

    Optional<String> fileNameFromUrn = AssetManagerImpl.getFileNameFromUrn(element);
    assertTrue(fileNameFromUrn.isEmpty());

    element.setURI(URI.create("file://test.txt"));
    fileNameFromUrn = AssetManagerImpl.getFileNameFromUrn(element);
    assertTrue(fileNameFromUrn.isEmpty());

    element.setURI(URI.create("urn:matterhorn:uuid:22:uuid2:caption-ger.vtt"));
    fileNameFromUrn = AssetManagerImpl.getFileNameFromUrn(element);
    assertTrue(fileNameFromUrn.isPresent());
    assertEquals("caption-ger.vtt", fileNameFromUrn.get());
  }

  @Test
  public void testSetAndUpdateProperty() throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    final String mpId = mp.getIdentifier().toString();
    am.takeSnapshot(OWNER, mp);
    final Date d1 = new Date(0);
    final Date d2 = new Date(1);
    final Date d3 = new Date(2);
    logger.info("Add a property to the media package");
    assertTrue("The property should be set", am.setProperty(p.start(mpId, d1)));
    logger.info("Select all properties of the media package");
    {
      List<Property> properties = am.selectProperties(mpId, null);
      assertEquals("One property should be found", 1, properties.size());
      assertEquals("Value check", d1, properties.stream().findFirst().get().getValue().get(Value.DATE));
    }
    logger.info("Update the property");
    assertTrue("The property should be updated", am.setProperty(p.start(mpId, d2)));
    {
      List<Property> properties = am.selectProperties(mpId, null);
      assertEquals("One property should be found", 1, properties.size());
      assertEquals("Value check", d2, properties.stream().findFirst().get().getValue().get(Value.DATE));
    }
    logger.info("The existence of multiple versions of a media package should not affect property storage");
    logger.info("Add a new version of the media package");
    am.takeSnapshot(OWNER, mp);
    logger.info("Update the property again");
    assertTrue("The property should be updated", am.setProperty(p.start(mpId, d3)));
    {
      List<Snapshot> snapshots = am.getSnapshotsById(mpId);
      List<Property> properties = am.selectProperties(mpId, null);
      assertEquals("Two records should be found since there are now two versions of the media package and no"
                  + "version restriction has been applied",
                   2, snapshots.size());
      assertEquals("One properties should be found", 1, properties.size());
      assertEquals("There should be one distinct property in all of the found records",
          1, new HashSet(properties).size());
      assertEquals("Value check", d3, properties.stream().findFirst().get().getValue().get(Value.DATE));
    }
  }
}
