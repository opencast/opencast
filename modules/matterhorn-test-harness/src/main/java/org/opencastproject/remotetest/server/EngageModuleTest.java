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

package org.opencastproject.remotetest.server;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of the Engage module Tests if all media player components are available
 *
 * This needs to be improved by String[] EngageGFXuri = { "/engage-hybrid-player/icons/cc_off.png",
 * "/engage-hybrid-player/icons/cc_on.png", .... }
 *
 * String[] EngageJSuri = { ... }
 *
 * to remove many Testcases
 *
 * The DefaultHttpClient needs to be threadsafe - included in org.apache.httpcomponents version 4-1alpha
 *
 */
public class EngageModuleTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(EngageModuleTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + EngageModuleTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    client = Main.getClient();
    domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(false); // don't forget this!
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }

  private static DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
  private static XPathFactory factory = XPathFactory.newInstance();

  public static String ENGAGE_BASE_URL = BASE_URL + "/engage/ui";

  private void clearSearchIndex() throws Exception {
    HttpPost post = new HttpPost(BASE_URL + "/search/clear");

    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    client.execute(post);
  }

  @Test
  public void testJQuery() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/jquery/jquery.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryXSLT() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/jquery/plugins/jquery.xslt.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testEngageUI() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/engage-ui.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFABridge() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/bridge/lib/FABridge.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testVideodisplay() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/bridge/Videodisplay.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

}
