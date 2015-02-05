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
package org.opencastproject.workflow.handler.episode;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler;

public class ArchiveWorkflowOperationHandlerTest {

  private ArchiveWorkflowOperationHandler operationHandler;
  private MediaPackage mp;
  private URI uriMP;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    uriMP = InspectWorkflowOperationHandler.class.getResource("/archive_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    // set up the handler
    operationHandler = new ArchiveWorkflowOperationHandler();
  }

  @Test
  public void testAllTagsFlavors() throws Exception {
    List<String> tags = new ArrayList<String>();
    String[] flavors = new String[0];
    MediaPackage mediaPackageForArchival = operationHandler.getMediaPackageForArchival(mp, tags, flavors);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 5,
            mediaPackageForArchival.getElements().length);
  }

  @Test
  public void testByFlavor() throws Exception {
    List<String> tags = new ArrayList<String>();
    String[] flavors = new String[] { "presentation/*" };
    MediaPackage mediaPackageForArchival = operationHandler.getMediaPackageForArchival(mp, tags, flavors);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1,
            mediaPackageForArchival.getElements().length);
  }

  @Test
  public void testByTags() throws Exception {
    List<String> tags = new ArrayList<String>();
    tags.add("archive");
    tags.add("atom");
    String[] flavors = new String[0];
    MediaPackage mediaPackageForArchival = operationHandler.getMediaPackageForArchival(mp, tags, flavors);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 2,
            mediaPackageForArchival.getElements().length);
  }

  @Test
  public void testByFlavorAndTags() throws Exception {
    List<String> tags = new ArrayList<String>();
    tags.add("archive");
    String[] flavors = new String[] { "*/source" };
    MediaPackage mediaPackageForArchival = operationHandler.getMediaPackageForArchival(mp, tags, flavors);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 3,
            mediaPackageForArchival.getElements().length);
  }

}
