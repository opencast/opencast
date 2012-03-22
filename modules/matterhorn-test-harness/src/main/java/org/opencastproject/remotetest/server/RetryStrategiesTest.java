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

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of the retry strategies
 */
public class RetryStrategiesTest {

  private static final Logger logger = LoggerFactory.getLogger(RetryStrategiesTest.class);

  private TrustedHttpClient client;

  private int initCount;

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + RetryStrategiesTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    client = Main.getClient();

    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/services/count?serviceType=org.opencastproject.inspection");
    String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    logger.debug("Job count response: {}", getResponse);

    initCount = Integer.parseInt(getResponse);
  }

  @After
  public void tearDown() throws Exception {
    HttpPost postSanitize = new HttpPost(BASE_URL + "/services/sanitize");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("serviceType", "org.opencastproject.inspection"));
    formParams.add(new BasicNameValuePair("host", BASE_URL));
    postSanitize.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    HttpResponse execute = client.execute(postSanitize);
    Assert.assertEquals(204, execute.getStatusLine().getStatusCode());

    Main.returnClient(client);
  }

  @Test
  public void testNone() throws Exception {
    // Start a workflow instance via the rest endpoint
    HttpPost postStart = new HttpPost(BASE_URL + "/workflow/start");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition("workflow-none.xml")));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("properties", "this=that"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new workflow instance from the response
    String postResponse = EntityUtils.toString(client.execute(postStart).getEntity());
    String id = getWorkflowInstanceId(postResponse);

    // Ensure that the workflow finishes successfully
    int attempts = 0;
    while (true) {
      if (++attempts == 20)
        Assert.fail("workflow rest endpoint test has hung");
      HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/instance/" + id + ".xml");
      String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
      String state = getWorkflowInstanceStatus(getResponse);
      if ("FAILED".equals(state))
        break;
      if ("SUCCEEDED".equals(state))
        Assert.fail("workflow instance " + id + " succeeded");
      if ("PAUSED".equals(state))
        Assert.fail("workflow instance " + id + " paused");
      System.out.println("workflow " + id + " is " + state);
      Thread.sleep(5000);
    }

    // Get Ingest service state
    // Should be WARNING
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL
            + "/services/services.json?serviceType=org.opencastproject.inspection&host=" + BASE_URL);
    String jsonResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    String state = new JSONObject(jsonResponse).getJSONObject("services").getJSONObject("service")
            .getString("service_state");
    Assert.assertEquals("WARNING", state);

    // Get the number of failed on Ingest service,
    // Should be 1
    getWorkflowMethod = new HttpGet(BASE_URL + "/services/count?serviceType=org.opencastproject.inspection");
    String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    logger.debug("Job count response: {}", getResponse);
    Assert.assertEquals(1, Integer.parseInt(getResponse) - initCount);
  }

  @Test
  public void testHold() throws Exception {
    // Start a workflow instance via the rest endpoint
    HttpPost postStart = new HttpPost(BASE_URL + "/workflow/start");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition("workflow-hold.xml")));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("properties", "this=that"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new workflow instance from the response
    String postResponse = EntityUtils.toString(client.execute(postStart).getEntity());
    String id = getWorkflowInstanceId(postResponse);

    // Ensure that the workflow finishes successfully
    int attempts = 0;
    while (true) {
      if (++attempts == 20)
        Assert.fail("workflow rest endpoint test has hung");
      HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/instance/" + id + ".xml");
      String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
      String state = getWorkflowInstanceStatus(getResponse);
      if ("PAUSED".equals(state))
        break;
      if ("FAILED".equals(state))
        Assert.fail("workflow instance " + id + " failed");
      if ("SUCCEEDED".equals(state))
        Assert.fail("workflow instance " + id + " succeeded");
      System.out.println("workflow " + id + " is " + state);
      Thread.sleep(5000);
    }

    // Get Ingest service state
    // Should be WARNING
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL
            + "/services/services.json?serviceType=org.opencastproject.inspection&host=" + BASE_URL);
    String jsonResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    String state = new JSONObject(jsonResponse).getJSONObject("services").getJSONObject("service")
            .getString("service_state");
    Assert.assertEquals("WARNING", state);

    // Get the number of failed on Ingest service,
    // Should be 1
    getWorkflowMethod = new HttpGet(BASE_URL + "/services/count?serviceType=org.opencastproject.inspection");
    String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    logger.debug("Job count response: {}", getResponse);
    Assert.assertEquals(1, Integer.parseInt(getResponse) - initCount);
  }

  @Test
  public void testRetry() throws Exception {
    // -------------------------------------------------
    // FIRST WORKFLOW (RETRY)
    // -------------------------------------------------

    // Start a workflow instance via the rest endpoint
    HttpPost postStart = new HttpPost(BASE_URL + "/workflow/start");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition("workflow-retry.xml")));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("properties", "this=that"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new workflow instance from the response
    String postResponse = EntityUtils.toString(client.execute(postStart).getEntity());
    String id = getWorkflowInstanceId(postResponse);

    // Ensure that the workflow finishes successfully
    int attempts = 0;
    while (true) {
      if (++attempts == 20)
        Assert.fail("workflow rest endpoint test has hung");
      HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/instance/" + id + ".xml");
      String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
      String state = getWorkflowInstanceStatus(getResponse);
      if ("FAILED".equals(state))
        break;
      if ("SUCCEEDED".equals(state))
        Assert.fail("workflow instance " + id + " succeeded");
      if ("PAUSED".equals(state))
        Assert.fail("workflow instance " + id + " paused");
      System.out.println("workflow " + id + " is " + state);
      Thread.sleep(5000);
    }

    // Get Ingest service state
    // Should be WARNING
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL
            + "/services/services.json?serviceType=org.opencastproject.inspection&host=" + BASE_URL);
    String jsonResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    String state = new JSONObject(jsonResponse).getJSONObject("services").getJSONObject("service")
            .getString("service_state");
    Assert.assertEquals("WARNING", state);

    // Get the number of failed on Ingest service,
    // Should be 1
    getWorkflowMethod = new HttpGet(BASE_URL + "/services/count?serviceType=org.opencastproject.inspection");
    String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    logger.debug("Job count response: {}", getResponse);
    Assert.assertEquals(2, Integer.parseInt(getResponse) - initCount);

    // -------------------------------------------------
    // SECOND WORKFLOW (NONE)
    // -------------------------------------------------

    // Start a workflow instance via the rest endpoint
    postStart = new HttpPost(BASE_URL + "/workflow/start");
    formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition("workflow-none.xml")));
    formParams.add(new BasicNameValuePair("mediapackage", getSecondSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("properties", "this=that"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new workflow instance from the response
    postResponse = EntityUtils.toString(client.execute(postStart).getEntity());
    id = getWorkflowInstanceId(postResponse);

    // Ensure that the workflow finishes successfully
    attempts = 0;
    while (true) {
      if (++attempts == 20)
        Assert.fail("workflow rest endpoint test has hung");
      getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/instance/" + id + ".xml");
      getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
      state = getWorkflowInstanceStatus(getResponse);
      if ("FAILED".equals(state))
        break;
      if ("SUCCEEDED".equals(state))
        Assert.fail("workflow instance " + id + " succeeded");
      if ("PAUSED".equals(state))
        Assert.fail("workflow instance " + id + " paused");
      System.out.println("workflow " + id + " is " + state);
      Thread.sleep(5000);
    }

    // Get Ingest service state
    // Should be WARNING
    getWorkflowMethod = new HttpGet(BASE_URL
            + "/services/services.json?serviceType=org.opencastproject.inspection&host=" + BASE_URL);
    jsonResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    state = new JSONObject(jsonResponse).getJSONObject("services").getJSONObject("service").getString("service_state");
    Assert.assertEquals("ERROR", state);

    // Get available ingest service, because ingest service is in ERROR mode he is not returned
    getWorkflowMethod = new HttpGet(BASE_URL + "/services/available.json?serviceType=org.opencastproject.inspection");
    jsonResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    Assert.assertNull(new JSONObject(jsonResponse).optJSONObject("services"));

    // Get the number of failed on Ingest service,
    // Should be 1
    getWorkflowMethod = new HttpGet(BASE_URL + "/services/count?serviceType=org.opencastproject.inspection");
    getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    logger.debug("Job count response: {}", getResponse);
    Assert.assertEquals(3, Integer.parseInt(getResponse) - initCount);

  }

  protected String getWorkflowInstanceId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

  protected String getWorkflowInstanceStatus(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("state");
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-1.xml"), "UTF-8");
    return template.replaceAll("@SAMPLES_URL@", "http://wrong-url.ch");
  }

  protected String getSecondSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-1.xml"), "UTF-8");
    return template.replaceAll("@SAMPLES_URL@", "http://wrong-url-2.ch");
  }

  protected String getSampleWorkflowDefinition(String workflow) throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(workflow), "UTF-8");
  }

}
