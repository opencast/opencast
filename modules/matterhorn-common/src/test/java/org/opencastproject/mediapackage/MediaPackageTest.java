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

import org.junit.Test;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.util.ConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static com.jayway.restassured.path.xml.XmlPath.from;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.mediapackage.MediaPackageElements.PRESENTATION_SOURCE;
import static org.opencastproject.mediapackage.MediaPackageElements.PRESENTER_SOURCE;
import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.mediapackage.PublicationImpl.publication;
import static org.opencastproject.util.MimeType.mimeType;

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
      MediaPackageElement derivedElement = mediaPackageElementBuilder.elementFromURI(dcFile.toURI(), Type.Catalog, derivedFlavor);
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

  @Test
  public void testPublicationElement() throws Exception {
    final MediaPackage mp = mediaPackageBuilder.createNew();
    mp.add(publication("1", "engage", new URI("http://localhost/1.html"), mimeType("text", "html")));
    assertEquals("Number of media package elements", 1, mp.getElements().length);
    final String xml = MediaPackageParser.getAsXml(mp);
    System.out.println(xml);
    assertEquals("Media package identifier", mp.getIdentifier().toString(), from(xml).get("mediapackage.@id"));
    assertEquals("Publication channel name", "engage", from(xml).get("mediapackage.publications.publication.@channel"));
  }

  @Test
  public void testPublicationElementFromFile() throws Exception {
    final MediaPackage mp = loadFromClassPath("/manifest.xml");
    assertEquals("Number of publication elements", 1, mp.getPublications().length);
    assertEquals("Publication channel name in deserialized mediapackage",
                 "engage", mp.getPublications()[0].getChannel());
    final String xml = MediaPackageParser.getAsXml(mp);
    assertEquals("Publication channel name in serialized mediapackage",
                 "engage", from(xml).get("mediapackage.publications.publication.@channel"));
  }

  @Test
  public void testAddElement() throws Exception {
    final MediaPackage mp1 = mediaPackageBuilder.createNew();
    final MediaPackage mp2 = mediaPackageBuilder.createNew();

    final MediaPackageElement presentation = mediaPackageElementBuilder.newElement(Type.Track, PRESENTATION_SOURCE);
    presentation.setURI(new URI("http://localhost/presentation"));

    final MediaPackageElement presenter = mediaPackageElementBuilder.newElement(Type.Track, PRESENTER_SOURCE);
    presenter.setURI(new URI("http://localhost/presenter"));

    final Set<MediaPackageElement> elements = new HashSet<MediaPackageElement>();
    elements.add(presentation);
    elements.add(presenter);
    assertEquals("Expect two elements", 2, elements.size());
    assertTrue("Expect presenter to be in set", elements.contains(presenter));
    assertTrue("Expect presentation to be in set", elements.contains(presentation));

    mp1.add(presentation);
    assertEquals("Expect parent mediapackage to be mp1", mp1, presentation.getMediaPackage());

    // breaks element <-> parent relationship
    mp2.add(presentation);
    assertEquals(mp2, presentation.getMediaPackage()); // this works!
    assertEquals(1, mp1.getElements().length);
    assertEquals(1, mp2.getElements().length);

    mp2.add(presenter);
    assertEquals(2, mp2.getElements().length);

    // check element set again
    assertEquals(2, elements.size());
    // crash! does not hold true anymore since adding mutates fields that are used for hash code calculation
    // comment in if issue has been resolved
//    assertTrue("Expect presenter to be in set", elements.contains(presenter));
//    assertTrue("Expect presentation to be in set", elements.contains(presentation));
  }
}
