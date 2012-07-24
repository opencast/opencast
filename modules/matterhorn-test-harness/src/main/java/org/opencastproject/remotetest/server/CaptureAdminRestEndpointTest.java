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
import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Test the Capture Admin REST endpoints
 */
public class CaptureAdminRestEndpointTest {
  private TrustedHttpClient httpClient;

  private static final Logger logger = LoggerFactory.getLogger(CaptureAdminRestEndpointTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + CaptureAdminRestEndpointTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    httpClient = Main.getClient();
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(httpClient);
  }

  @Test
  public void testGetAgents() throws Exception {
    String endpoint = "/capture-admin/agents";
    HttpGet get = new HttpGet(BASE_URL + endpoint + ".xml");
    String xmlResponse = EntityUtils.toString(httpClient.execute(get).getEntity());
    Main.returnClient(httpClient);

    // parse the xml and extract the running clients names
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // factory.setNamespaceAware(true); // don't forget this!
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xmlResponse, "UTF-8"));
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList agents = (NodeList) xPath.compile("//*[local-name() = 'agent']").evaluate(doc, XPathConstants.NODESET);

    // validate the REST endpoint for each agent state is functional
    for (int i = 0; i < agents.getLength(); i++) {
      try {
        httpClient = Main.getClient();
        String agentName = (String) xPath.evaluate("*[local-name() = 'name']/text()", agents.item(i),
                XPathConstants.STRING);
        HttpGet agentGet = new HttpGet(BASE_URL + endpoint + "/" + agentName + ".xml");
        int agentResponse = httpClient.execute(agentGet).getStatusLine().getStatusCode();
        assertEquals(HttpStatus.SC_OK, agentResponse);
      } finally {
        Main.returnClient(httpClient);
      }
    }
  }
}
