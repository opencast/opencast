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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Tests authorization behavior of workflow instances.
 */
public class WorkflowAuthorizationTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(WorkflowAuthorizationTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + WorkflowAuthorizationTest.class.getName());
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
  public void testStartAndRetrieveWorkflowInstance() throws Exception {

    // Create a series with attached ACLs
    HttpPost postSeries = new HttpPost(BASE_URL + "/series/");
    List<NameValuePair> seriesParams = new ArrayList<NameValuePair>();
    seriesParams.add(new BasicNameValuePair("series", getSampleSeries()));
    seriesParams.add(new BasicNameValuePair("acl", getSampleAcl()));
    postSeries.setEntity(new UrlEncodedFormEntity(seriesParams, "UTF-8"));
    HttpResponse seriesResponse = client.execute(postSeries);
    Assert.assertEquals(200, seriesResponse.getStatusLine().getStatusCode());
    EntityUtils.toString(seriesResponse.getEntity());

    // Start a workflow instance associated with this series
    HttpPost postWorkflow = new HttpPost(BASE_URL + "/workflow/start");
    List<NameValuePair> workflowParams = new ArrayList<NameValuePair>();
    workflowParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition()));
    workflowParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    postWorkflow.setEntity(new UrlEncodedFormEntity(workflowParams, "UTF-8"));
    HttpResponse workflowResponse = client.execute(postWorkflow);
    Assert.assertEquals(200, workflowResponse.getStatusLine().getStatusCode());

    // Grab the new workflow instance from the response
    String postResponse = EntityUtils.toString(workflowResponse.getEntity());

    // Ensure that the mediapackage has a XACML attachment reflecting the series ACLs
    String xacmlAttachmentId = getXACMLAttachmentId(postResponse);
    Assert.assertNotNull(xacmlAttachmentId);
  }

  protected String getXACMLAttachmentId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((String) XPathFactory.newInstance().newXPath()
            .compile("//*[local-name() = 'attachment'][@type='security/xacml']/@id")
            .evaluate(doc, XPathConstants.STRING));
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("authorization/mediapackage-with-series.xml"), "UTF-8");
    return template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
  }

  protected String getSampleWorkflowDefinition() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("authorization/workflow-definition-1.xml"),
            "UTF-8");
  }

  protected String getSampleSeries() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("authorization/sample-series-1.xml"),
            "UTF-8");
  }

  protected String getSampleAcl() throws Exception {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><acl xmlns=\"org.opencastproject.security\"><ace><role>admin</role><action>delete</action><allow>true</allow></ace></acl>";

  }
}
