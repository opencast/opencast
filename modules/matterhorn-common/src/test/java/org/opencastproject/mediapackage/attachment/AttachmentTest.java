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
package org.opencastproject.mediapackage.attachment;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageBuilderTest;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.UnsupportedElementException;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Test case for the {@link AttachmentImpl} attachment implementation.
 */
public class AttachmentTest {

  /* png test image */
  private File coverFile = null;

  /**
   * Creates everything that is needed to test a media package.
   *
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    coverFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").toURI());
    assertTrue(coverFile.exists() && coverFile.canRead());
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.attachment.AttachmentImpl#toManifest(org.w3c.dom.Document, org.opencastproject.mediapackage.MediaPackageSerializer)}
   * .
   */
  @Test
  public void testToManifest() {
    // fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.attachment.AttachmentImpl#fromURI(java.net.URI)}.
   */
  @Test
  public void testFromURL() {
    MediaPackageElementBuilderFactory factory = MediaPackageElementBuilderFactory.newInstance();
    MediaPackageElementBuilder builder = factory.newElementBuilder();
    MediaPackageElement packageElement = null;

    // Create the element
    try {
      packageElement = builder.elementFromURI(coverFile.toURI());
    } catch (UnsupportedElementException e) {
      fail("Attachment is unsupported: " + e.getMessage());
    }

    // Type test
    assertTrue("Type mismatch", packageElement instanceof Attachment);

  }

}
