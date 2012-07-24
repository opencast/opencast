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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of the youtube distribution rest endpoint
 */
public class YoutubeDistributionRestEndpointTest {

  private static final Logger logger = LoggerFactory.getLogger(RetryStrategiesTest.class);

  private TrustedHttpClient client;

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + RetryStrategiesTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    client = Main.getClient();
    retract();
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testDistributeAndRetract() throws Exception {

    // -----------------------------------------------------
    // Upload the video to youtube
    // -----------------------------------------------------

    HttpPost postStart = new HttpPost(BASE_URL + "/youtube/");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("elementId", "track-1"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Ensure we get a 200 OK
    HttpResponse response = client.execute(postStart);
    Assert.assertEquals("Upload to youtube failed!", 200, response.getStatusLine().getStatusCode());

    String jobXML = EntityUtils.toString(response.getEntity());
    long jobId = getJobId(jobXML);

    // Check if job is finished
    // Ensure that the job finishes successfully
    int attempts = 0;
    while (true) {
      if (++attempts == 20)
        Assert.fail("ServiceRegistry rest endpoint test has hung");
      HttpGet getJobMethod = new HttpGet(BASE_URL + "/services/job/" + jobId + ".xml");
      jobXML = EntityUtils.toString(client.execute(getJobMethod).getEntity());
      String state = getJobStatus(jobXML);
      if ("FINISHED".equals(state)) {
        // Get Mediapackage Track from flavor youtube/watchpage
        String payload = getPayloadFromJob(jobXML);
        String youtubeXmlUrl = getYoutubeURL(payload);
        HttpURLConnection connection = (HttpURLConnection) new URL(youtubeXmlUrl).openConnection();
        Assert.assertEquals("Url status code from uploaded video is not 200", 200, connection.getResponseCode());
        break;
      }
      if ("FAILED".equals(state))
        Assert.fail("Job " + jobId + " failed");
      System.out.println("Job " + jobId + " is " + state);
      Thread.sleep(5000);
    }

    // -----------------------------------------------------
    // Retract the video from youtube
    // -----------------------------------------------------

    retract();
  }

  private void retract() throws Exception, UnsupportedEncodingException, IOException, InterruptedException {

    HttpPost postStart = new HttpPost(BASE_URL + "/youtube/retract");
    ArrayList<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("elementId", "track-1"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Ensure we get a 200 OK
    HttpResponse response = client.execute(postStart);
    Assert.assertEquals("Error during retract", 200, response.getStatusLine().getStatusCode());

    String jobXML = EntityUtils.toString(response.getEntity());
    long jobId = getJobId(jobXML);

    // Check if job is finished
    // Ensure that the job finishes successfully
    int attempts = 0;
    while (true) {
      if (++attempts == 20)
        Assert.fail("ServiceRegistry rest endpoint test has hung");
      HttpGet getJobMethod = new HttpGet(BASE_URL + "/services/job/" + jobId + ".xml");
      String getResponse = EntityUtils.toString(client.execute(getJobMethod).getEntity());
      String state = getJobStatus(getResponse);
      if ("FINISHED".equals(state))
        break;
      if ("FAILED".equals(state))
        Assert.fail("Retract from youtube failed! (Job " + jobId + "). Do it manually for video!");
      System.out.println("Job " + jobId + " is " + state);
      Thread.sleep(5000);
    }
  }

  private String getPayloadFromJob(String jobXML) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(jobXML, "UTF-8"));
    XPath xPath = XPathFactory.newInstance().newXPath();
    return (String) xPath.evaluate("/*[local-name() = 'job']/*[local-name() = 'payload']", doc, XPathConstants.STRING);
  }

  private String getYoutubeURL(String mediaPackageElementXML) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(mediaPackageElementXML, "UTF-8"));
    XPath xPath = XPathFactory.newInstance().newXPath();
    return (String) xPath.evaluate("/*[local-name() = 'track']/*[local-name() = 'url']", doc, XPathConstants.STRING);
  }

  private String getJobStatus(String jobXML) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(jobXML, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("status");
  }

  private long getJobId(String jobXML) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(jobXML, "UTF-8"));
    return Long.parseLong(((Element) XPathFactory.newInstance().newXPath().compile("/*")
            .evaluate(doc, XPathConstants.NODE)).getAttribute("id"));
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-youtube.xml"),
            "UTF-8");
    return template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
  }

}
