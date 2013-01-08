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

package org.opencastproject.remotetest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.remotetest.Main.BASE_URL;
import static org.opencastproject.remotetest.util.WorkflowUtils.countSucceededWorkflows;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

public class OaiPmhServerTest {
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhServerTest.class);

  private TrustedHttpClient client;
  private static final String baseUrl = BASE_URL + "/oaipmh";

  @BeforeClass
  public static void setupClass() throws Exception {
    Assume.assumeTrue(checkPresenceOfOaiPmhRepository());
    logger.info("Running " + OaiPmhServerTest.class.getName());
    while (countSucceededWorkflows() < 2) {
      logger.info("waiting 5 sec. for workflows to succeed");
      Thread.sleep(5000);
    }
  }

  private static boolean checkPresenceOfOaiPmhRepository() throws IOException {
    HttpGet get = new HttpGet(baseUrl);
    TrustedHttpClient client = Main.getClient();
    HttpResponse response = client.execute(get);
    response.getEntity().consumeContent();
    Main.returnClient(client);
    boolean serverPresent = response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND;
    if (serverPresent) {
      logger.info("OAI-PMH server present. Running tests.");
    } else {
      logger.info("OAI-PMH server not deployed. Skipping tests.");
    }
    return serverPresent;
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    logger.info("Finished " + OaiPmhServerTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    client = Main.getClient();
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testBadVerbError() {
    Document response = doGetRequest("verb=Scrunch");
    assertXpathExists(response, "//*[local-name() = 'error'][@code='badVerb']");
  }

  @Test
  public void testGetRecordBadArgumentError() {
    Document response = doGetRequest("verb=GetRecord&identifier=unknown");
    assertXpathExists(response, "//*[local-name() = 'error'][@code='badArgument']");
  }

  @Test
  public void testGetRecordIdDoesNotExistError() {
    Document response = doGetRequest("verb=GetRecord&identifier=_&metadataPrefix=oai_dc");
    assertXpathExists(response, "//*[local-name() = 'error'][@code='idDoesNotExist']");
  }

  @Test
  public void testIdentify() {
    Document response = doGetRequest("verb=Identify");
    assertXpathExists(response, "//*[local-name() = 'Identify']");
    assertXpathEquals(response, "//*[local-name() = 'protocolVersion']/text()", "2.0");
  }

  @Test
  public void testListMetadataFormatsAll() throws Exception {
    Document response = doGetRequest("verb=ListMetadataFormats");
    assertXpathExists(response, "//*[local-name() = 'ListMetadataFormats']");
    List<String> prefixes = Utils.nodeListToStringList((NodeList) Utils.xpath(response,
            "//*[local-name() = 'metadataPrefix']", XPathConstants.NODESET));
    assertTrue(prefixes.contains("oai_dc"));
    assertTrue(prefixes.contains("matterhorn"));
  }

  @Test
  public void testListSets() {
    Document doc = doGetRequest("verb=ListSets");
    assertXpathExists(doc, "//*[local-name() = 'ListSets']/*[local-name() = 'set']/*[local-name() = 'setSpec']");
    assertXpathExists(doc, "//*[local-name() = 'ListSets']/*[local-name() = 'set']/*[local-name() = 'setName']");
    assertXpathExists(doc,
            "//*[local-name() = 'ListSets']/*[local-name() = 'set']/*[local-name() = 'setDescription']/*/*/text()");
    assertXpathExists(doc, "//*[local-name() = 'ListSets']/*[local-name() = 'set'][setSpec=\"series\"]");
    assertXpathExists(doc, "//*[local-name() = 'ListSets']/*[local-name() = 'set'][setSpec=\"episode\"]");
    assertXpathExists(doc, "//*[local-name() = 'ListSets']/*[local-name() = 'set'][setSpec=\"episode:audio\"]");
    assertXpathExists(doc, "//*[local-name() = 'ListSets']/*[local-name() = 'set'][setSpec=\"episode:video\"]");
  }

  @Test
  public void testVerbListIdentifiersBadArgument() {
    Document doc = doGetRequest("verb=ListIdentifiers");
    assertXpathExists(doc, "//*[local-name() = 'error'][@code=\"badArgument\"]");
  }

  @Test
  public void testVerbListIdentifiers() throws TransformerException, XPathExpressionException {
    Document doc = doGetRequest("verb=ListIdentifiers&metadataPrefix=oai_dc");
    assertXpathExists(doc,
            "//*[local-name() = 'ListIdentifiers']/*[local-name() = 'header']/*[local-name() = 'datestamp']");
    assertXpathExists(doc,
            "//*[local-name() = 'ListIdentifiers']/*[local-name() = 'header']/*[local-name() = 'identifier']");
  }

  @Test
  public void testVerbListRecords() throws TransformerException, XPathExpressionException {
    Document doc = doGetRequest("verb=ListRecords&metadataPrefix=oai_dc");
    assertXpathExists(doc, "//*[local-name() = 'ListRecords']/*[local-name() = 'record']/*[local-name() = 'header']");
    assertXpathExists(doc,
            "//*[local-name() = 'ListRecords']/*[local-name() = 'record']/*[local-name() = 'header']/*[local-name() = 'datestamp']");
    assertXpathExists(
            doc,
            "//*[local-name() = 'ListRecords']/*[local-name() = 'record']/*[local-name() = 'header']/*[local-name() = 'identifier']");
    assertXpathExists(doc, "//*[local-name() = 'ListRecords']/*[local-name() = 'record']/*[local-name() = 'metadata']");
  }

  @Test
  public void testVerbListRecordsNoMatch() throws TransformerException, XPathExpressionException {
    Document doc = doGetRequest("verb=ListRecords&metadataPrefix=oai_dc&until=1000-01-01");
    assertXpathExists(doc, "//*[local-name() = 'error'][@code=\"noRecordsMatch\"]");
  }

  @Test
  public void testMatterhornMetadataProvider() {
    Document doc = doGetRequest("verb=ListRecords&metadataPrefix=matterhorn");
    assertXpathExists(doc, "//*[local-name() = 'metadata']//*[local-name() = 'title']/text()");
  }

  private Document doGetRequest(String query) {
    HttpGet get = new HttpGet(baseUrl + "?" + query);
    HttpResponse response = client.execute(get);
    assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    return getXml(response);
  }

  private static void assertXpathExists(Document doc, String path) {
    try {
      NodeList nodes = (NodeList) Utils.xpath(doc, path, XPathConstants.NODESET);
      assertTrue(nodes.getLength() > 0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void assertXpathEquals(Document doc, String path, String expected) {
    try {
      assertEquals(expected, ((String) Utils.xpath(doc, path, XPathConstants.STRING)).trim());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Document getXml(HttpResponse response) {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.getEntity().getContent());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
