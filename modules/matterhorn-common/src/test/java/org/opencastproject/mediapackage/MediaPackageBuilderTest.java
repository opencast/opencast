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

package org.opencastproject.mediapackage;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test case used to make sure the media package builder works as expected.
 */
public class MediaPackageBuilderTest extends AbstractMediaPackageTest {

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.MediaPackageBuilderImpl#createNew(org.opencastproject.mediapackage.identifier.Id)}
   * .
   */
  @Test
  public void testCreateNew() {
    MediaPackage mediaPackage = null;
    try {
      mediaPackage = mediaPackageBuilder.createNew(identifier);
      assertEquals(identifier, mediaPackage.getIdentifier());
    } catch (MediaPackageException e) {
      fail("Error creating new media package: " + e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.MediaPackageBuilderImpl#loadFromXml(java.io.InputStream)}.
   */
  @Test
  public void testLoadFromManifest() {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.loadFromXml(new FileInputStream(manifestFile));

      // Test presence of tracks
      assertEquals(2, mediaPackage.getTracks().length);

      // Test presence of catalogs
      assertEquals(3, mediaPackage.getCatalogs().length);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.EPISODE));

      // Test presence of attachments
      assertEquals(2, mediaPackage.getAttachments().length);

    } catch (MediaPackageException e) {
      fail("Media package exception while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (FileNotFoundException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    }
  }

  @Test
  public void testLoadPublicationElement() {

  String fileName = "/publicationElement.xml";
  String id = "p-1";
  String channel = "engage";
  String uri = "http://localhost/engage.html";
  MimeType mimeType = MimeTypes.parseMimeType("text/html");

  try {
    File baseDir = new File(MediaPackageBuilderTest.class.getResource("/").toURI());
    File xmlFile = new File(baseDir, fileName);
    String xml = IOUtils.toString(new FileInputStream(xmlFile));
    Publication pubElement = (Publication) MediaPackageElementParser.getFromXml(xml);
      assertNotNull(pubElement);

      assertEquals(id, pubElement.getIdentifier());
      assertEquals(channel, pubElement.getChannel());
      assertEquals(new URI(uri), pubElement.getURI());
      assertEquals(mimeType, pubElement.getMimeType());


  } catch (FileNotFoundException e) {
    fail("Media package exception while reading media package from manifest: " + e.getMessage());
  } catch (MediaPackageException e) {
    fail("Media package exception while reading media package from manifest: " + e.getMessage());
  } catch (IOException e) {
    fail("Media package exception while reading media package from manifest: " + e.getMessage());
  } catch (URISyntaxException e) {
    fail("Not able to load xml file containing the publication element: " + e.getMessage());
  }

  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.MediaPackageBuilderImpl#loadFromXml(org.w3c.dom.Node)}.
   */
  @Test
  public void testLoadFromNode() throws Exception {
    try {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document xml = docBuilder.parse(manifestFile);
      MediaPackage mediaPackage = mediaPackageBuilder.loadFromXml(xml);

      assertNotNull(mediaPackage.getTitle());
      assertEquals(1, mediaPackage.getCreators().length);

      // Test presence of tracks
      assertEquals(2, mediaPackage.getTracks().length);

      // Test presence of catalogs
      assertEquals(3, mediaPackage.getCatalogs().length);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.EPISODE));

      // Test presence of attachments
      assertEquals(2, mediaPackage.getAttachments().length);
    } catch (MediaPackageException e) {
      fail("Media package exception while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (FileNotFoundException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    }
  }
}
