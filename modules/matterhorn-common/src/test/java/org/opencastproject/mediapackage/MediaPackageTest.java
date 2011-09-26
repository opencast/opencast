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

import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.util.ConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Test cases for the media package.
 */
public class MediaPackageTest extends AbstractMediaPackageTest {

  @Test
  public void testEmptyMediaPackage() {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      MediaPackageParser.getAsXml(mediaPackage);
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    }
  }

  @Test
  public void testElementUrls() throws ParserConfigurationException, SAXException, IOException {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();

      // Create a media package and add an element
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      mediaPackage.add(dcFile.toURI());

      // Test url
      String xmlString = MediaPackageParser.getAsXml(mediaPackage);
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document xml = docBuilder.parse(new ByteArrayInputStream(xmlString.getBytes()));
      String expected = dcFile.toURI().toURL().toExternalForm();
      assertEquals(expected, xPath.evaluate("//url", xml));

      // TODO: Add more
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
  public void testDerivates() {
    try {

      // Create a media package and add an element
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      MediaPackageElementFlavor sourceFlavor = new MediaPackageElementFlavor("some", "source");
      MediaPackageElement dcCatalog = mediaPackage.add(dcFile.toURI(), Type.Catalog, sourceFlavor);

      // Add the "derived" catalog
      MediaPackageElementFlavor derivedFlavor = new MediaPackageElementFlavor("dublincore", "specialedition");
      MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      MediaPackageElement derivedElement = elementBuilder.elementFromURI(dcFile.toURI(), Type.Catalog, derivedFlavor);
      mediaPackage.addDerived(derivedElement, dcCatalog);

      // Test the whole thing
      assertEquals(1, mediaPackage.getElementsByFlavor(derivedFlavor).length);
      assertEquals(2, mediaPackage.getCatalogs().length);
      assertEquals(1, mediaPackage.getCatalogs(derivedFlavor).length);
      assertEquals(1, mediaPackage.getDerived(dcCatalog, derivedFlavor).length);
      assertEquals(derivedElement, mediaPackage.getDerived(dcCatalog, derivedFlavor)[0]);

    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error while creating media package: " + e.getMessage());
    }
  }

}
