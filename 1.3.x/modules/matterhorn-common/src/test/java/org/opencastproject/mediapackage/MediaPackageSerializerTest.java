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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.util.ConfigurationException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Test case used to make sure the media package serializer works as expected.
 */
@Ignore
public class MediaPackageSerializerTest extends AbstractMediaPackageTest {

  /** a uri pointing to a web resource */
  private URI webURI = null;

  /** a uri pointing to a linux file system resource */
  private URI linuxRootURI = null;

  /** a uri pointing to a linux file system resource */
  private URI linuxURI = null;

  /** a uri pointing to a windows file system resource */
  private URI windowsRootURI = null;

  /** a uri pointing to a windows file system resource */
  private URI windowsURI = null;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    webURI = new URI("http://www.opencastproject.org/dc.xml");
    linuxRootURI = new URI("file:///Users/John+Doe/My+Mediapackage");
    linuxURI = new URI("file:///Users/John+Doe/My+Mediapackage/dc.xml");
    windowsRootURI = new URI("file://c:\\\\Users\\John+Doe\\My+Mediapackage");
    windowsURI = new URI("file://c:\\\\Users\\John+Doe\\My+Mediapackage\\dc.xml");
  }

  @Test
  public void testRelativeNativePaths() {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();

      // Create a media package and add an element
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      mediaPackage.add(dcFile.toURI());

      // Test relative path, using serializer
      MediaPackageSerializer serializer = null;
      serializer = new DefaultMediaPackageSerializerImpl(manifestFile.getParentFile());
      Document xml = MediaPackageParser.getAsXml(mediaPackage, serializer);

      // Test linux file relative to media package root
      String expected = dcFile.getAbsolutePath().substring(packageDir.getAbsolutePath().length() + 1);
      assertEquals(expected, xPath.evaluate("//url", xml));
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Exception while creating url: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error while creating media package: " + e.getMessage());
    } catch (XPathExpressionException e) {
      fail("Selecting node form xml document failed: " + e.getMessage());
    }
  }

  @Test
  public void testRelativeLinuxPaths() {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();

      // Create a media package and add an element
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      mediaPackage.add(linuxURI);

      // Test relative path, using serializer
      MediaPackageSerializer serializer = null;
      serializer = new DefaultMediaPackageSerializerImpl(new File(linuxRootURI));
      Document xml = MediaPackageParser.getAsXml(mediaPackage, serializer);

      // Test linux file relative to media package root
      String expected = linuxURI.toString().substring(linuxRootURI.toString().length() + 1);
      assertEquals(expected, xPath.evaluate("//url", xml));
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Exception while creating url: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error while creating media package: " + e.getMessage());
    } catch (XPathExpressionException e) {
      fail("Selecting node form xml document failed: " + e.getMessage());
    }
  }

  @Test
  public void testRelativeWindowsPaths() {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();

      // Create a media package and add an element
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      mediaPackage.add(windowsURI);

      // Test relative path, using serializer
      MediaPackageSerializer serializer = null;
      serializer = new DefaultMediaPackageSerializerImpl(new File(windowsRootURI));
      Document xml = MediaPackageParser.getAsXml(mediaPackage, serializer);

      // Test windows file relative to media package root
      String expected = windowsURI.toString().substring(windowsRootURI.toString().length() + 1);
      assertEquals(expected, xPath.evaluate("//url", xml));
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Exception while creating url: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error while creating media package: " + e.getMessage());
    } catch (XPathExpressionException e) {
      fail("Selecting node form xml document failed: " + e.getMessage());
    }
  }

}
