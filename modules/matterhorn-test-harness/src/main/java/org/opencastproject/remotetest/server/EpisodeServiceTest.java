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
import static org.opencastproject.remotetest.util.Tuple.tuple;
import static org.opencastproject.remotetest.util.Utils.xpath;
import static org.opencastproject.remotetest.util.WorkflowUtils.countSucceededWorkflows;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Tuple;
import org.opencastproject.remotetest.util.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;

public class EpisodeServiceTest {
  private static final Logger logger = LoggerFactory.getLogger(EpisodeServiceTest.class);

  private TrustedHttpClient client;
  private String baseUrl = BASE_URL;

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + EpisodeServiceTest.class.getName());
    while (countSucceededWorkflows() < 2) {
      logger.info("waiting 5 sec. for workflows to succeed");
      Thread.sleep(5000);
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    logger.info("Finished " + EpisodeServiceTest.class.getName());
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
  public void testLockMediaPackage() throws Exception {
    // get a media package id
    Document r1 = doGetRequest("workflow/instances.xml", HttpStatus.SC_OK, tuple("startPage", 0), tuple("count", 1));
    String id = (String) xpath(r1, "//*[local-name() = 'mediapackage']/@id", XPathConstants.STRING);
    //
    Document r2 = doGetRequest("episode/episode.xml", HttpStatus.SC_OK, tuple("id", id), tuple("limit", 0),
            tuple("offset", 0));
    boolean locked = Boolean.parseBoolean((String) xpath(r2, "//*[local-name() = 'ocLocked']", XPathConstants.STRING));
    // un/lock this package
    if (locked) {
      doPostRequest("episode/unlock", HttpStatus.SC_NO_CONTENT, tuple("id", id));
    } else {
      doPostRequest("episode/lock", HttpStatus.SC_NO_CONTENT, tuple("id", id));
      // TODO test apply should not work
    }
    // and test it
    Document r3 = doGetRequest("episode/episode.xml", HttpStatus.SC_OK, tuple("id", id), tuple("limit", 0),
            tuple("offset", 0));
    boolean newLocked = Boolean
            .parseBoolean((String) xpath(r3, "//*[local-name() = 'ocLocked']", XPathConstants.STRING));
    System.out.println(newLocked);
    assertEquals(!locked, newLocked);
  }

  @Test
  public void testRetractMediaPackage() throws Exception {
    // get a media package id
    Document r1 = doGetRequest("workflow/instances.xml", HttpStatus.SC_OK, tuple("startPage", 0), tuple("count", 1));
    String id = (String) xpath(r1, "//*[local-name() = 'mediapackage']/@id", XPathConstants.STRING);
    String retractWorkflow = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("retract.xml"), "UTF-8");
    System.out.println("Retracting media package " + id);
    doPostRequest("episode/applyworkflow", HttpStatus.SC_NO_CONTENT, tuple("definition", retractWorkflow),
            tuple("id", id));
  }

  @Test
  public void testDeleteMediaPackage() throws Exception {
    // get a media package id
    Document r1 = doGetRequest("workflow/instances.xml", HttpStatus.SC_OK, tuple("startPage", 0), tuple("count", 1));
    String id = (String) xpath(r1, "//mediapackage/@id", XPathConstants.STRING);

    Document r2 = doGetRequest("episode/episode.xml", HttpStatus.SC_OK, tuple("id", id), tuple("limit", 0),
            tuple("offset", 0));

    // assert is in episode service
    boolean locked = Boolean.parseBoolean((String) xpath(r2, "//ocLocked", XPathConstants.STRING));

    // get ArchiveMP, assert is same and not null
    // archive test getElement, assertOK

    // archive delete assert ok

    // get episode.xml, assert not found or found but marked as deleted

    // get ArchiveMP, assert is null
    // archive test getElement, assert Not found
  }

  private Document doGetRequest(String path, int expectedHttpResonseCode, Tuple<String, ?>... params) {
    List<NameValuePair> qparams = new ArrayList<NameValuePair>();
    for (Tuple<String, ?> p : params) {
      qparams.add(new BasicNameValuePair(p.getA(), p.getB().toString()));
    }
    String query = URLEncodedUtils.format(qparams, "UTF-8");
    final String url = baseUrl + (path.startsWith("/") ? path : "/" + path)
            + (StringUtils.isNotEmpty(query) ? "?" + query : "");

    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get);
    assertEquals(expectedHttpResonseCode, response.getStatusLine().getStatusCode());
    if (expectedHttpResonseCode != HttpStatus.SC_NO_CONTENT)
      return getXml(response);
    else
      return null;
  }

  private Document doPostRequest(String path, int expectedHttpResonseCode, Tuple<String, ?>... params) {
    final String url;
    if (path.startsWith("/"))
      url = baseUrl + path;
    else
      url = baseUrl + "/" + path;

    HttpPost post = new HttpPost(url);
    // create form
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    for (Tuple<String, ?> p : params) {
      formParams.add(new BasicNameValuePair(p.getA(), p.getB().toString()));
    }
    try {
      post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    HttpResponse response = client.execute(post);
    assertEquals(expectedHttpResonseCode, response.getStatusLine().getStatusCode());
    if (expectedHttpResonseCode != HttpStatus.SC_NO_CONTENT)
      return getXml(response);
    else
      return null;
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

  private String getTestWorkflow() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("test-archive.xml"), "UTF-8");
  }

  private String getTestApplyWorkflow() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("test-apply-archive.xml"), "UTF-8");
  }
}
