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
import org.opencastproject.remotetest.server.resource.ComposerResources;
import org.opencastproject.remotetest.util.SampleUtils;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;

/**
 * Tests the working file repository's rest endpoint
 */
public class ServiceRegistryRestEndpointTest {
  String serviceUrl = null;

  String remoteHost = null;
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryRestEndpointTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + ServiceRegistryRestEndpointTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    remoteHost = URLEncoder.encode(BASE_URL, "UTF-8");
    serviceUrl = BASE_URL + "/services";
    client = Main.getClient();
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testGetServiceRegistrations() throws Exception {
    // Get a known service registration as xml
    HttpGet get = new HttpGet(serviceUrl + "/services.xml?serviceType=org.opencastproject.composer&host=" + remoteHost);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    InputStream in = null;
    Document doc = null;

    try {
      in = response.getEntity().getContent();
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      String typeFromResponse = (String) Utils.xpath(doc, "//*[local-name() = 'type']", XPathConstants.STRING);
      Assert.assertEquals("org.opencastproject.composer", typeFromResponse);
      String hostFromResponse = (String) Utils.xpath(doc, "//*[local-name() = 'host']", XPathConstants.STRING);
      Assert.assertEquals(BASE_URL, hostFromResponse);
    } finally {
      IOUtils.closeQuietly(in);
      Main.returnClient(client);
    }

    // Get a registration that is known to not exist, and ensure we get a 404
    client = Main.getClient();
    get = new HttpGet(serviceUrl + "/services.xml?serviceType=foo&host=" + remoteHost);
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());

    // Get all services on a single host
    client = Main.getClient();
    get = new HttpGet(serviceUrl + "/services.xml?host=" + remoteHost);
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    try {
      in = response.getEntity().getContent();
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      int serviceCount = ((Number) Utils.xpath(doc, "count(//*[local-name() = 'service'])", XPathConstants.NUMBER))
              .intValue();
      Assert.assertTrue(serviceCount > 0);
    } finally {
      IOUtils.closeQuietly(in);
      Main.returnClient(client);
    }

    // Get all services of a single type
    client = Main.getClient();
    get = new HttpGet(serviceUrl + "/services.xml?serviceType=org.opencastproject.composer");
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    try {
      in = response.getEntity().getContent();
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      int serviceCount = ((Number) Utils.xpath(doc, "count(//*[local-name() = 'service'])", XPathConstants.NUMBER))
              .intValue();
      Assert.assertEquals(1, serviceCount);
    } finally {
      IOUtils.closeQuietly(in);
      Main.returnClient(client);
    }

    // Get statistics
    client = Main.getClient();
    get = new HttpGet(serviceUrl + "/statistics.xml");
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    try {
      in = response.getEntity().getContent();
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      int serviceCount = ((Number) Utils.xpath(doc, "count(//*[local-name() = 'service'])", XPathConstants.NUMBER))
              .intValue();
      Assert.assertTrue(serviceCount > 0);
    } finally {
      IOUtils.closeQuietly(in);
      Main.returnClient(client);
    }
  }

  /**
   * TODO: Finish this after coming up with a good strategy on how to propagate maintenance mode from the service
   * implementation (serviceRegistry.createJob()) to the rest endpoint.
   */
  @Test
  @Ignore
  public void testNodeMaintenance() throws Exception {

    // Start an encoding job via the rest endpoint
    HttpPost postEncode = new HttpPost(ComposerResources.getServiceUrl() + "encode");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", SampleUtils.generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("profileId", "flash.http"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the job from the response
    HttpResponse postResponse = client.execute(postEncode);
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());

    // Try to start another job, make sure we don't succeed
    postResponse = client.execute(postEncode);
    Assert.assertEquals(404, postResponse.getStatusLine().getStatusCode());

  }

}
