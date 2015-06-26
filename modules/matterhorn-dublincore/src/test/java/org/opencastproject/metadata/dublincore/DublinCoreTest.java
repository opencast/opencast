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


package org.opencastproject.metadata.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.metadata.dublincore.DublinCore.ENC_SCHEME_URI;
import static org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_ANY;
import static org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_UNDEFINED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_FORMAT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.metadata.dublincore.DublinCore.TERMS_NS_URI;
import static org.opencastproject.metadata.dublincore.DublinCores.OC_PROPERTY_PROMOTED;
import static org.opencastproject.util.UrlSupport.uri;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.NamespaceBindingException;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Test class for the dublin core implementation.
 */
public class DublinCoreTest {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreTest.class);

  /**
   * The catalog name
   */
  private static final String catalogName = "/dublincore.xml";
  private static final String catalogName2 = "/dublincore2.xml";

  /** The XML catalog list name */
  private static final String xmlCatalogListName = "/dublincore-list.xml";

  /** The JSON catalog list name */
  private static final String jsonCatalogListName = "/dublincore-list.json";

  /**
   * The test catalog
   */
  private File catalogFile = null;
  private File catalogFile2 = null;

  /** Temp files for test catalogs */
  private File dcTempFile1 = null;
  private File dcTempFile2 = null;

  private DublinCoreCatalogService service = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    catalogFile = new File(this.getClass().getResource(catalogName).toURI());
    catalogFile2 = new File(this.getClass().getResource(catalogName2).toURI());
    if (!catalogFile.exists() || !catalogFile.canRead())
      throw new Exception("Unable to access test catalog");
    if (!catalogFile2.exists() || !catalogFile2.canRead())
      throw new Exception("Unable to access test catalog 2");
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(catalogFile).anyTimes();
    EasyMock.replay(workspace);
    service = new DublinCoreCatalogService();
    service.setWorkspace(workspace);
  }

  /**
   * @throws java.io.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(dcTempFile1);
    FileUtils.deleteQuietly(dcTempFile2);
  }

  /**
   * Test method for {@link org.opencastproject.metadata.dublincore.DublinCoreCatalog#fromFile(java.io.File)} .
   */
  @Test
  public void testFromFile() throws Exception {
    DublinCoreCatalog dc = null;
    FileInputStream in = new FileInputStream(catalogFile);
    dc = DublinCores.read(in);
    IOUtils.closeQuietly(in);

    // Check if the fields are available
    assertEquals("ETH Zurich, Switzerland", dc.getFirst(PROPERTY_PUBLISHER, LANGUAGE_UNDEFINED));
    assertEquals("Land and Vegetation: Key players on the Climate Scene",
            dc.getFirst(PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED));
    assertNotNull(dc.getFirst(PROPERTY_TITLE));
    assertNull(dc.getFirst(PROPERTY_TITLE, "fr"));
    // Test custom metadata element
    assertEquals("true", dc.getFirst(OC_PROPERTY_PROMOTED));
  }

  /**
   * Test method for {@link DublinCoreCatalogList#parse(String)} with an XML String
   */
  @Test
  public void testParseDublinCoreListXML() throws Exception {
    String dublinCoreListString = IOUtils.toString(getClass().getResourceAsStream(xmlCatalogListName), "UTF-8");
    DublinCoreCatalogList catalogList = DublinCoreCatalogList.parse(dublinCoreListString);
    Assert.assertEquals(2, catalogList.getTotalCount());
    Assert.assertEquals("Land 1", catalogList.getCatalogList().get(0).getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
    Assert.assertEquals("Land 2", catalogList.getCatalogList().get(1).getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
  }

  /**
   * Test method for {@link DublinCoreCatalogList#parse(String)} with a JSON String
   */
  @Test
  public void testParseDublinCoreListJSON() throws Exception {
    String dublinCoreListString = IOUtils.toString(getClass().getResourceAsStream(jsonCatalogListName), "UTF-8");
    DublinCoreCatalogList catalogList = DublinCoreCatalogList.parse(dublinCoreListString);
    Assert.assertEquals(2, catalogList.getTotalCount());
    Assert.assertEquals("Land 1", catalogList.getCatalogList().get(0).getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
    Assert.assertEquals("Land 2", catalogList.getCatalogList().get(1).getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
  }

  /**
   * Test method for {@link DublinCoreCatalog#toJson()} .
   */
  @Test
  public void testJson() throws Exception {
    DublinCoreCatalog dc = null;
    FileInputStream in = new FileInputStream(catalogFile);
    dc = DublinCores.read(in);
    IOUtils.closeQuietly(in);

    String jsonString = dc.toJson();

    JSONParser parser = new JSONParser();
    JSONObject jsonObject = (JSONObject) parser.parse(jsonString);

    JSONObject dcTerms = (JSONObject) jsonObject.get(TERMS_NS_URI);
    assertNotNull(dcTerms);

    JSONArray titleArray = (JSONArray) dcTerms.get("title");
    assertEquals("Two titles should be present", 2, titleArray.size());

    JSONArray subjectArray = (JSONArray) dcTerms.get("subject");
    assertEquals("The subject should be present", 1, subjectArray.size());

    DublinCoreCatalog fromJson = DublinCores.read(IOUtils.toInputStream(jsonString));
    assertEquals(2, fromJson.getLanguages(PROPERTY_TITLE).size());
    assertEquals("video/x-dv", fromJson.getFirst(PROPERTY_FORMAT));
    assertEquals("eng", fromJson.getFirst(PROPERTY_LANGUAGE));
    assertEquals("2007-12-05", fromJson.getFirst(PROPERTY_CREATED));

    // Serialize again, and we should get the same json
    String jsonRoundtrip = fromJson.toJson();
    assertEquals(jsonString, jsonRoundtrip);
  }

  /**
   * Test method for saving the catalog.
   */
  @Test
  public void testNewInstance() {
    try {
      // Read the sample catalog
      FileInputStream in = new FileInputStream(catalogFile);
      DublinCoreCatalog dcSample = DublinCores.read(in);
      IOUtils.closeQuietly(in);

      // Create a new catalog and fill it with a few fields
      DublinCoreCatalog dcNew = DublinCores.mkOpencast();
      dcTempFile1 = new File(FileSupport.getTempDirectory(), Long.toString(System.currentTimeMillis()));

      // Add the required fields
      dcNew.add(PROPERTY_IDENTIFIER, dcSample.getFirst(PROPERTY_IDENTIFIER));
      dcNew.add(PROPERTY_TITLE, dcSample.getFirst(PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED),
              DublinCore.LANGUAGE_UNDEFINED);

      // Add an additional field
      dcNew.add(PROPERTY_PUBLISHER, dcSample.getFirst(PROPERTY_PUBLISHER));

      // Add a null-value field
      try {
        dcNew.add(PROPERTY_CONTRIBUTOR, (String) null);
        fail();
      } catch (IllegalArgumentException ignore) {
      }

      // Add a field with an encoding scheme
      dcNew.add(PROPERTY_LICENSE, DublinCoreValue.mk("http://www.opencastproject.org/license",
              DublinCore.LANGUAGE_UNDEFINED, ENC_SCHEME_URI));
      // Don't forget to bind the namespace...
      dcNew.addBindings(XmlNamespaceContext.mk("octest", "http://www.opencastproject.org/octest"));
      dcNew.add(OC_PROPERTY_PROMOTED, DublinCoreValue.mk("true", DublinCore.LANGUAGE_UNDEFINED, new EName(
              "http://www.opencastproject.org/octest", "Boolean")));
      try {
        dcNew.add(OC_PROPERTY_PROMOTED, DublinCoreValue.mk(
                "true",
                DublinCore.LANGUAGE_UNDEFINED,
                EName.mk("http://www.opencastproject.org/enc-scheme", "Boolean")));
        fail();
      } catch (NamespaceBindingException e) {
        // Ok. This exception is expected to occur
      }

      // Store the catalog
      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "xml");
      FileWriter sw = new FileWriter(dcTempFile1);
      StreamResult result = new StreamResult(sw);
      DOMSource source = new DOMSource(dcNew.toXml());
      trans.transform(source, result);

      // Re-read the saved catalog and test for its content
      DublinCoreCatalog dcNewFromDisk = DublinCores.read(dcTempFile1.toURI().toURL().openStream());
      assertEquals(dcSample.getFirst(PROPERTY_IDENTIFIER), dcNewFromDisk.getFirst(PROPERTY_IDENTIFIER));
      assertEquals(dcSample.getFirst(PROPERTY_TITLE, "en"), dcNewFromDisk.getFirst(PROPERTY_TITLE, "en"));
      assertEquals(dcSample.getFirst(PROPERTY_PUBLISHER), dcNewFromDisk.getFirst(PROPERTY_PUBLISHER));

    } catch (IOException e) {
      fail("Error creating the catalog: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      fail("Error creating a parser for the catalog: " + e.getMessage());
    } catch (TransformerException e) {
      fail("Error saving the catalog: " + e.getMessage());
    }
  }

  /**
   * Tests overwriting of values.
   */
  @Test
  public void testOverwriting() {
    // Create a new catalog and fill it with a few fields
    DublinCoreCatalog dcNew = null;
    dcNew = DublinCores.mkOpencast();
    dcNew.set(PROPERTY_TITLE, "Title 1");
    assertEquals("Title 1", dcNew.getFirst(PROPERTY_TITLE));

    dcNew.set(PROPERTY_TITLE, "Title 2");
    assertEquals("Title 2", dcNew.getFirst(PROPERTY_TITLE));

    dcNew.set(PROPERTY_TITLE, "Title 3", "de");
    assertEquals("Title 2", dcNew.getFirst(PROPERTY_TITLE));
    assertEquals("Title 3", dcNew.getFirst(PROPERTY_TITLE, "de"));
    dcNew = null;
  }

  @Test
  public void testVarious() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    // Add a title
    dc.add(PROPERTY_TITLE, "Der alte Mann und das Meer");
    assertEquals("Der alte Mann und das Meer", dc.getFirst(PROPERTY_TITLE));
    assertEquals(1, dc.get(PROPERTY_TITLE, LANGUAGE_UNDEFINED).size());
    assertEquals(1, dc.get(PROPERTY_TITLE).size());
    // Overwrite the title
    dc.set(PROPERTY_TITLE, "La Peste");
    assertEquals("La Peste", dc.getFirst(PROPERTY_TITLE));
    assertEquals(1, dc.get(PROPERTY_TITLE).size());

    dc.set(PROPERTY_TITLE, "Die Pest", "de");
    assertEquals(2, dc.get(PROPERTY_TITLE).size());
    assertEquals(1, dc.get(PROPERTY_TITLE, LANGUAGE_UNDEFINED).size());

    // Remove the title without language code
    dc.remove(PROPERTY_TITLE, LANGUAGE_UNDEFINED);
    // The german title is now the only remaining title so we should get it here
    assertEquals("Die Pest", dc.getFirst(PROPERTY_TITLE));
    assertNotNull(dc.getFirst(PROPERTY_TITLE, "de"));
    assertNull(dc.getFirst(PROPERTY_TITLE, "fr"));
    assertEquals(1, dc.get(PROPERTY_TITLE).size());
    assertTrue(dc.hasValue(PROPERTY_TITLE));
    assertFalse(dc.hasMultipleValues(PROPERTY_TITLE));

    // Add a german title (does not make sense...)
    dc.add(PROPERTY_TITLE, "nonsense", "de");
    assertEquals(2, dc.get(PROPERTY_TITLE, "de").size());
    assertEquals(2, dc.get(PROPERTY_TITLE).size());

    // Now restore the orginal french title
    dc.set(PROPERTY_TITLE, "La Peste");
    assertEquals(3, dc.get(PROPERTY_TITLE).size());

    // And get rid of the german ones
    dc.remove(PROPERTY_TITLE, "de");
    assertEquals(0, dc.get(PROPERTY_TITLE, "de").size());
    assertNull(dc.getFirst(PROPERTY_TITLE, "de"));
    assertEquals(1, dc.get(PROPERTY_TITLE).size());

    // No contributor is set so expect null
    assertNull(dc.getFirst(PROPERTY_CONTRIBUTOR));
    // ... but expect an empty list here
    assertNotNull(dc.get(PROPERTY_CONTRIBUTOR));
  }

  @Test
  public void testVarious2() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    dc.add(PROPERTY_TITLE, "The Lord of the Rings");
    dc.add(PROPERTY_TITLE, "Der Herr der Ringe", "de");
    assertEquals(2, dc.getLanguages(PROPERTY_TITLE).size());

    assertEquals("The Lord of the Rings; Der Herr der Ringe", dc.getAsText(PROPERTY_TITLE, LANGUAGE_ANY, "; "));
    assertNull(dc.getAsText(PROPERTY_CONTRIBUTOR, LANGUAGE_ANY, "; "));

    dc.remove(PROPERTY_TITLE, "de");
    assertEquals(1, dc.getLanguages(PROPERTY_TITLE).size());

    dc.remove(PROPERTY_TITLE);

    assertNull(dc.getAsText(PROPERTY_TITLE, LANGUAGE_ANY, "; "));
  }

  @Test
  public void testVarious3() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    dc.add(PROPERTY_CONTRIBUTOR, "Heinz Strunk");
    dc.add(PROPERTY_CONTRIBUTOR, "Rocko Schamoni");
    dc.add(PROPERTY_CONTRIBUTOR, "Jacques Palminger");
    assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR));
    assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR, LANGUAGE_UNDEFINED));
    // assertTrue(dc.hasMultipleValues(PROPERTY_TITLE));
    assertEquals(3, dc.get(PROPERTY_CONTRIBUTOR).size());

    dc.add(PROPERTY_CONTRIBUTOR, "Klaus Allofs", "de");
    dc.add(PROPERTY_CONTRIBUTOR, "Karl-Heinz Rummenigge", "de");
    assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR, "de"));
    assertTrue(dc.hasMultipleValues(PROPERTY_CONTRIBUTOR, "de"));
    assertEquals(2, dc.get(PROPERTY_CONTRIBUTOR, "de").size());
    assertEquals(5, dc.get(PROPERTY_CONTRIBUTOR).size());

    dc.set(PROPERTY_CONTRIBUTOR, "Hans Manzke");
    assertEquals(1, dc.get(PROPERTY_CONTRIBUTOR, LANGUAGE_UNDEFINED).size());
    assertEquals(3, dc.get(PROPERTY_CONTRIBUTOR).size());
  }

  @Test
  public void testVarious4() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    dc.add(PROPERTY_TITLE, "deutsch", "de");
    dc.add(PROPERTY_TITLE, "english", "en");
    assertNull(dc.getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
    assertNotNull(dc.getFirst(PROPERTY_TITLE, LANGUAGE_ANY));
    assertNotNull(dc.getFirst(PROPERTY_TITLE));

    dc.add(PROPERTY_TITLE, "undefined");
    assertEquals("undefined", dc.getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
    assertEquals("undefined", dc.getFirst(PROPERTY_TITLE));
    assertEquals("deutsch", dc.getFirst(PROPERTY_TITLE, "de"));
  }

  @Test
  public void testSet() {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    dc.set(PROPERTY_CREATOR, Arrays.asList(DublinCoreValue.mk("Klaus"), DublinCoreValue.mk("Peter"),
                                           DublinCoreValue.mk("Carl", "en")));
    assertEquals(2, dc.get(PROPERTY_CREATOR, LANGUAGE_UNDEFINED).size());
    assertEquals(3, dc.get(PROPERTY_CREATOR).size());
    assertEquals("Klaus", dc.get(PROPERTY_CREATOR, LANGUAGE_UNDEFINED).get(0));
  }

  @Test
  public void testMediaPackageMetadataExtraction() throws Exception {
    // Load the DC catalog
    FileInputStream in = null;
    DublinCoreCatalog catalog = null;
    try {
      in = new FileInputStream(catalogFile);
      catalog = service.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Create a mediapackage containing the DC catalog
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.add(catalogFile.toURI(), Catalog.TYPE, MediaPackageElements.EPISODE);
    MediaPackageMetadata metadata = service.getMetadata(mp);

    assertEquals("Mediapackage metadata title not extracted from DC properly",
            catalog.getFirst(DublinCore.PROPERTY_TITLE), metadata.getTitle());
  }

  // todo fix http://opencast.jira.com/browse/MH-8759 then remove @Ignore
  @Ignore
  @Test
  public void testPreserveEncodingScheme() {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    DublinCoreValue val = DublinCoreValue.mk("http://www.opencastproject.org/license", "en", ENC_SCHEME_URI);
    dc.add(PROPERTY_LICENSE, val);
    assertEquals(1, dc.get(PROPERTY_LICENSE).size());
    assertEquals(val, dc.get(PROPERTY_LICENSE).get(0));
    assertEquals(Opt.some(ENC_SCHEME_URI), dc.get(PROPERTY_LICENSE).get(0).getEncodingScheme());
  }

  @Test
  public void testSerializeDublinCore() throws Exception {
    DublinCoreCatalog dc = null;
    FileInputStream in = new FileInputStream(catalogFile2);
    dc = DublinCores.read(in);
    IOUtils.closeQuietly(in);

    String inputXml = IOUtils.toString(new FileInputStream(catalogFile2), "UTF-8");
    Assert.assertTrue(inputXml.contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""));

    Assert.assertTrue(dc.toXmlString().contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""));

    DublinCoreValue extent = EncodingSchemeUtils.encodeDuration(3000);
    dc.set(DublinCore.PROPERTY_EXTENT, extent);

    Assert.assertTrue(dc.toXmlString().contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""));

    DublinCoreValue date = EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute);
    dc.set(DublinCore.PROPERTY_CREATED, date);

    Assert.assertTrue(dc.toXmlString().contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""));
  }

  // test that exceptions are thrown correctly
  @Test
  public void testEncodingSchemeUtilsExceptions() {
    DCMIPeriod period = new DCMIPeriod(new java.util.Date(), new java.util.Date());
    Precision precision = Precision.Year;
    try {
      EncodingSchemeUtils.encodePeriod(null, precision);
      Assert.fail("Exceptions should be thrown on null values.");
    } catch (Exception e) {
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      EncodingSchemeUtils.encodePeriod(period, null);
      Assert.fail("Exceptions should be thrown on null values.");
    } catch (Exception e) {
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      EncodingSchemeUtils.encodePeriod(null, null);
      Assert.fail("Exceptions should be thrown on null values.");
    } catch (Exception e) {
      Assert.assertFalse(e instanceof NullPointerException);
    }
  }

  @Test
  @Ignore
  // this test should verify serialization/deserialization works for a fairly minimal case
  // waiting on https://opencast.jira.com/browse/MH-9733
  public void testSerializationDeserializationOfCatalogs() throws Exception {
    DublinCoreCatalog impl = DublinCores.mkOpencast();
    impl.addTag("bob");
    impl.set(impl.PROPERTY_PUBLISHER, "test");
    DublinCoreCatalogService service = new DublinCoreCatalogService();
    DublinCoreCatalog newImpl = service.load(service.serialize(impl));
    Assert.assertEquals(impl, newImpl);
  }

  @Test
  // test for null values on various methods on the DublinCoreCatalog, they should
  // generally return an exception
  public void testForNullsInDublinCoreCatalogImpl() throws Exception {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    try {
      DublinCoreValue val = null;
      dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName.class), val);
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      dc.add(null, EasyMock.createNiceMock(DublinCoreValue.class));
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      String val2 = null;
      dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName.class), val2);
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      dc.add(null, "");
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      String val2 = null;
      dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName.class), val2, val2);
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      String val2 = null;
      dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName.class), "", val2);
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
    try {
      dc.add(null, "", "");
    } catch (Exception e) {
      // throw assertion if it happens to be a nullpointer, never a null pointer!
      Assert.assertFalse(e instanceof NullPointerException);
    }
  }

  @Test
  public void testClone() {
    final DublinCoreCatalog dc = DublinCores.mkOpencast();
    final MimeType mimeType = MimeType.mimeType("text", "xml");
    dc.setMimeType(MimeType.mimeType("text", "xml"));
    dc.setReference(new MediaPackageReferenceImpl("type", "identifier"));
    dc.setURI(uri("http://localhost"));
    assertNotNull(dc.getMimeType());
    assertEquals(mimeType, dc.getMimeType());
    // clone
    DublinCoreCatalog dcClone = (DublinCoreCatalog) dc.clone();
    assertEquals("The mime type should be cloned", dc.getMimeType(), dcClone.getMimeType());
    assertEquals("The flavor should be cloned", dc.getFlavor(), dcClone.getFlavor());
    assertEquals("The values should be cloned", dc.getValues(), dcClone.getValues());
    assertNull("The URI should not be cloned", dcClone.getURI());
    assertNull("A media package reference should not be cloned.", dcClone.getReference());
  }
}
