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


package org.opencastproject.mediapackage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.util.ConfigurationException;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test case for media package references.
 */
public class MediaPackageReferenceTest extends AbstractMediaPackageTest {

  /**
   * Test method for {@link org.opencastproject.mediapackage.MediaPackageReferenceImpl#matches(MediaPackageReference)}.
   */
  @Test
  public void testMatches() {
    MediaPackageReference mediaPackageReference = new MediaPackageReferenceImpl(mediaPackage);
    MediaPackageReference genericMediaPackageReference = new MediaPackageReferenceImpl(
            MediaPackageReference.TYPE_MEDIAPACKAGE, "*");
    MediaPackageReference trackReference = new MediaPackageReferenceImpl(mediaPackage.getElementById("track-2"));
    MediaPackageReference genericTrackReference = new MediaPackageReferenceImpl("track", "*");

    assertFalse(mediaPackageReference.matches(trackReference));
    assertFalse(trackReference.matches(mediaPackageReference));

    assertTrue(mediaPackageReference.matches(mediaPackageReference));
    assertTrue(mediaPackageReference.matches(genericMediaPackageReference));
    assertTrue(genericMediaPackageReference.matches(mediaPackageReference));

    assertTrue(trackReference.matches(trackReference));
    assertTrue(trackReference.matches(genericTrackReference));
    assertTrue(genericTrackReference.matches(trackReference));
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.MediaPackageImpl#add(java.net.URI)}.
   */
  @Test
  public void testMediaPackageReference() {
    try {
      // Add first catalog without any reference
      URI catalogXTestFile = MediaPackageReferenceTest.class.getResource("/dublincore.xml").toURI();
      MediaPackageElement catalogX = mediaPackage.add(catalogXTestFile);
      catalogX.setIdentifier("catalog-x");

      // Add second catalog with media package reference
      URI catalogYTestFile = MediaPackageReferenceTest.class.getResource("/dublincore.xml").toURI();
      MediaPackageElement catalogY = mediaPackage.add(catalogYTestFile);
      catalogY.referTo(new MediaPackageReferenceImpl(mediaPackage));
      catalogY.setIdentifier("catalog-y");

      // Add third catalog with track reference
      URI catalogZTestFile = MediaPackageReferenceTest.class.getResource("/dublincore.xml").toURI();
      MediaPackageElement catalogZ = mediaPackage.add(catalogZTestFile);
      catalogZ.referTo(new MediaPackageReferenceImpl("track", "track-1"));
      catalogZ.setIdentifier("catalog-z");

    } catch (UnsupportedElementException e) {
      fail("Adding of catalog failed: " + e.getMessage());
    } catch (URISyntaxException e) {
      fail("Adding of catalog failed: " + e.getMessage());
    }

    // Re-read the media package and test the references
    try {
      MediaPackageElement catalogX = mediaPackage.getElementById("catalog-x");
      assertTrue(catalogX.getReference() == null);
      MediaPackageElement catalogY = mediaPackage.getElementById("catalog-y");
      assertNotNull(catalogY.getReference());
      MediaPackageElement catalogZ = mediaPackage.getElementById("catalog-z");
      assertNotNull(catalogZ.getReference());
      assertTrue(catalogZ.getReference().matches(new MediaPackageReferenceImpl("track", "track-1")));
    } catch (ConfigurationException e) {
      fail("Configuration error while loading media package from manifest: " + e.getMessage());
    }
  }

}
