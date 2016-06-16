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

package org.opencastproject.remotetest.util;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Utility class that deals with jobs.
 */
public final class JobUtils {

  /**
   * This utility class is not meant to be instantiated.
   */
  private JobUtils() {
    // Nothing to do here
  }

  /**
   * Checks whether the given workflow is in the requested state.
   *
   * @param jobId
   *          identifier of the workflow
   * @param state
   *          the state that the workflow is expected to be in
   * @return <code>true</code> if the workflow is in the expected state
   * @throws IllegalStateException
   *           if the specified workflow can't be found
   */
  public static boolean isJobInState(String jobId, String state) throws IllegalStateException, Exception {
    String jobXml = getJobAsXml(jobId);
    String currentState = (String) Utils.xpath(jobXml, "/*[local-name() = 'job']/@status", XPathConstants.STRING);
    return state.equalsIgnoreCase(currentState);
  }

  /**
   * Gets the xml representation of a job.
   *
   * @param jobId
   *          the job identifier
   * @return the job as xml
   * @throws IOException
   *           if the job could not be loaded
   */
  public static String getJobAsXml(String jobId) throws IOException {
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/services/job/" + jobId + ".xml");
    TrustedHttpClient client = Main.getClient();
    try {
      HttpResponse response = client.execute(getWorkflowMethod);
      if (response.getStatusLine().getStatusCode() != 200)
        throw new IllegalStateException(EntityUtils.toString(response.getEntity()));
      String job = EntityUtils.toString(response.getEntity());
      return job;
    } finally {
      Main.returnClient(client);
    }
  }

  /**
   * Parses the job instance represented by <code>xml</code> and extracts the job identifier.
   *
   * @param xml
   *          the job instance
   * @return the job identifier
   * @throws Exception
   *           if parsing fails
   */
  public static String getJobId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

  /**
   * Parses the job instance represented by <code>xml</code> and extracts the job state.
   *
   * @param xml
   *          the job instance
   * @return the job state
   * @throws Exception
   *           if parsing fails
   */
  public static String getJobState(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("state");
  }

  /**
   * Parses the job instance represented by <code>xml</code> and extracts the job type.
   *
   * @param xml
   *          the job instance
   * @return the job type
   * @throws Exception
   *           if parsing fails
   */
  public static String getJobType(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("type");
  }

}
