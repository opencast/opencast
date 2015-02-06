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
package org.opencastproject.workflow.handler.search;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RepublishWorkflowOperationHandlerTest {

  /** The operation handler to test */
  private RepublishWorkflowOperationHandler operationHandler;

  /** The updated media package */
  private MediaPackage updatedMediaPackage;

  /** The currently published media package */
  private MediaPackage publishedMediaPackage;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // Load the test resources
    URI updatedMediaPackageURI = RepublishWorkflowOperationHandlerTest.class.getResource("/updated-mediapackage.xml")
            .toURI();
    updatedMediaPackage = builder.loadFromXml(updatedMediaPackageURI.toURL().openStream());
    updatedMediaPackage.setTitle("Land and Vegetation: Key players on the Climate Scene");

    URI publishedMediaPackageURI = RepublishWorkflowOperationHandlerTest.class.getResource(
            "/published-mediapackage.xml").toURI();
    publishedMediaPackage = builder.loadFromXml(publishedMediaPackageURI.toURL().openStream());
    publishedMediaPackage.setTitle("Land and Vegetation: Key players on the Climate Scene");

    // Set up the operation handler
    operationHandler = new RepublishWorkflowOperationHandler();
  }

  @Test
  public void testFilterByNothing() throws Exception {
    Set<MediaPackageElementFlavor> flavors = new HashSet<MediaPackageElementFlavor>();
    List<String> tags = new ArrayList<String>();

    tags.clear();
    MediaPackage filteredMediaPackage = operationHandler.filterMediaPackage(updatedMediaPackage, flavors, tags);
    Assert.assertEquals(3, filteredMediaPackage.getElements().length);
    Assert.assertEquals(1, filteredMediaPackage.getTracks().length);
    Assert.assertEquals(2, filteredMediaPackage.getCatalogs().length);
    Assert.assertEquals(0, filteredMediaPackage.getAttachments().length);
  }

  @Test
  public void testFilterByFlavor() throws Exception {
    Set<MediaPackageElementFlavor> flavors = new HashSet<MediaPackageElementFlavor>();
    flavors.add(MediaPackageElements.EPISODE);

    List<String> tags = new ArrayList<String>();

    MediaPackage filteredMediaPackage = operationHandler.filterMediaPackage(updatedMediaPackage, flavors, tags);
    Assert.assertEquals(0, filteredMediaPackage.getTracks().length);
    Assert.assertEquals(1, filteredMediaPackage.getCatalogs().length);
    Assert.assertEquals(0, filteredMediaPackage.getAttachments().length);
  }

  @Test
  public void testFilterByTag() throws Exception {
    Set<MediaPackageElementFlavor> flavors = new HashSet<MediaPackageElementFlavor>();

    List<String> tags = new ArrayList<String>();
    tags.add("archive");

    MediaPackage filteredMediaPackage = operationHandler.filterMediaPackage(updatedMediaPackage, flavors, tags);
    Assert.assertEquals(1, filteredMediaPackage.getElements().length);
    Assert.assertEquals(0, filteredMediaPackage.getTracks().length);
    Assert.assertEquals(1, filteredMediaPackage.getCatalogs().length);
    Assert.assertEquals(0, filteredMediaPackage.getAttachments().length);
  }

  @Test
  public void testFilterByFlavorAndTag() throws Exception {
    Set<MediaPackageElementFlavor> flavors = new HashSet<MediaPackageElementFlavor>();
    flavors.add(MediaPackageElements.EPISODE);

    List<String> tags = new ArrayList<String>();
    tags.add("archive");

    MediaPackage filteredMediaPackage = operationHandler.filterMediaPackage(updatedMediaPackage, flavors, tags);
    Assert.assertEquals(1, filteredMediaPackage.getElements().length);
    Assert.assertEquals(0, filteredMediaPackage.getTracks().length);
    Assert.assertEquals(1, filteredMediaPackage.getCatalogs().length);
    Assert.assertEquals(0, filteredMediaPackage.getAttachments().length);

    // Try a combination that should yield an empty set of elements
    flavors.clear();
    flavors.add(MediaPackageElements.SEGMENTS);

    filteredMediaPackage = operationHandler.filterMediaPackage(updatedMediaPackage, flavors, tags);
    Assert.assertEquals(0, filteredMediaPackage.getElements().length);
  }

  @Test
  public void testMergeFull() throws Exception {
    MediaPackage mp = RepublishWorkflowOperationHandler.merge(updatedMediaPackage, publishedMediaPackage);

    // Check that there is the expected number of elements in the mediapackage
    Assert.assertEquals("Found more elements than expected", 7, mp.getElements().length);
    Assert.assertEquals("Found more tracks than expected", 3, mp.getTracks().length);
    Assert.assertEquals("Found more catalogs than expected", 2, mp.getCatalogs().length);
    Assert.assertEquals("Found more attachments than expected", 1, mp.getAttachments().length);
    Assert.assertEquals("Found more publications than expected", 1, mp.getPublications().length);

    // Make sure that the merged mediapackage contains the updated catalog
    Assert.assertEquals("Merge picked the wrong catalog", "catalog-1",
            mp.getElementsByFlavor(MediaPackageElements.EPISODE)[0].getIdentifier());
  }

  @Test
  public void testMergeFiltered() throws Exception {
    Set<MediaPackageElementFlavor> flavors = new HashSet<MediaPackageElementFlavor>();
    flavors.add(MediaPackageElements.EPISODE);
    List<String> tags = new ArrayList<String>();
    MediaPackage filteredMediaPackage = operationHandler.filterMediaPackage(updatedMediaPackage, flavors, tags);
    MediaPackage mp = RepublishWorkflowOperationHandler.merge(filteredMediaPackage, publishedMediaPackage);

    // Check that there is the expected number of elements in the mediapackage
    Assert.assertEquals("Found more elements than expected", 6, mp.getElements().length);
    Assert.assertEquals("Found more tracks than expected", 2, mp.getTracks().length);
    Assert.assertEquals("Found more catalogs than expected", 2, mp.getCatalogs().length);
    Assert.assertEquals("Found more attachments than expected", 1, mp.getAttachments().length);
    Assert.assertEquals("Found more publications than expected", 1, mp.getPublications().length);

    // Make sure that the merged mediapackage contains the published track
    Assert.assertEquals("Merge picked the wrong track", "track-1-published",
            mp.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presentation/delivery"))[0].getIdentifier());

    // Make sure that the merged mediapackage contains the updated catalog
    Assert.assertEquals("Merge picked the wrong catalog", "catalog-1",
            mp.getElementsByFlavor(MediaPackageElements.EPISODE)[0].getIdentifier());
  }

}
