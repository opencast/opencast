/**
 *  Copyright 2009 The Regents of the University of California
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

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;

/**
 * Posts a zip file to the ingest service
 */
public class MaintenanceModeTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceModeTest.class);

  /** The service type (this can be any valid service that is likely to be running) */
  public static final String SERVICE_TYPE = "org.opencastproject.composer";

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + MaintenanceModeTest.class.getName());
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
  public void testMaintenanceMode() throws Exception {
    // Ensure that there is a service available
    HttpGet availableServicesGet = new HttpGet(Main.BASE_URL + "/services/available.xml?serviceType="
            + SERVICE_TYPE);
    HttpResponse availableServicesResponse = client.execute(availableServicesGet);
    Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatusLine().getStatusCode());
    String availableServicesXml = EntityUtils.toString(availableServicesResponse.getEntity());
    NodeList availableServicesNodes = (NodeList) Utils.xpath(availableServicesXml, "//service", XPathConstants.NODESET);
    Assert.assertTrue(availableServicesNodes.getLength() == 1);

    // Start a job
    HttpPost postJob = new HttpPost(Main.BASE_URL + "/services/job");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("jobType", SERVICE_TYPE));
    formParams.add(new BasicNameValuePair("host", Main.BASE_URL));
    formParams.add(new BasicNameValuePair("operation", "test"));
    formParams.add(new BasicNameValuePair("start", "false"));
    postJob.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Ensure that the job was created successfully
    HttpResponse jobResponse = client.execute(postJob);
    EntityUtils.toString(jobResponse.getEntity()); // read the response so the connection can be closed
    Assert.assertEquals(HttpStatus.SC_CREATED, jobResponse.getStatusLine().getStatusCode());

    // Put the server into maintenance mode
    HttpPost postMaintenance = new HttpPost(Main.BASE_URL + "/services/maintenance");
    List<NameValuePair> maintenanceParams = new ArrayList<NameValuePair>();
    maintenanceParams.add(new BasicNameValuePair("host", Main.BASE_URL));
    maintenanceParams.add(new BasicNameValuePair("maintenance", "true"));
    postMaintenance.setEntity(new UrlEncodedFormEntity(maintenanceParams, "UTF-8"));

    // Ensure that the server was put into maintenance mode
    HttpResponse maintenanceResponse = client.execute(postMaintenance);
    Assert.assertEquals(HttpStatus.SC_NO_CONTENT, maintenanceResponse.getStatusLine().getStatusCode());

    // The service should no longer be "available"
    availableServicesResponse = client.execute(availableServicesGet);
    Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatusLine().getStatusCode());
    availableServicesXml = EntityUtils.toString(availableServicesResponse.getEntity());
    availableServicesNodes = (NodeList) Utils.xpath(availableServicesXml, "//service", XPathConstants.NODESET);
    Assert.assertTrue(availableServicesNodes.getLength() == 0);

    // Try to start another job on this server. This should still be possible, even in maintenance mode, because the job
    // will be dispatched elsewhere.

    HttpResponse maintenanceModeJobCreationResponse = client.execute(postJob);
    EntityUtils.toString(maintenanceModeJobCreationResponse.getEntity());
    Assert.assertEquals(HttpStatus.SC_CREATED, maintenanceModeJobCreationResponse.getStatusLine().getStatusCode());

    // Restore the server to normal mode
    HttpPost postNormal = new HttpPost(Main.BASE_URL + "/services/maintenance");
    maintenanceParams.remove(1);
    maintenanceParams.add(new BasicNameValuePair("maintenance", "false"));
    postNormal.setEntity(new UrlEncodedFormEntity(maintenanceParams, "UTF-8"));

    // Ensure that the server was put into normal mode
    HttpResponse normalResponse = client.execute(postNormal);
    Assert.assertEquals(HttpStatus.SC_NO_CONTENT, normalResponse.getStatusLine().getStatusCode());
  }
}
