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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.server.resource.IngestResources;
import org.opencastproject.remotetest.server.resource.SearchResources;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

/**
 * Integration test for file upload using thin client
 */
@Ignore
// Until we can make the Jersey client work with digest auth, this must remain ignored
public class UploadTest {
  String trackUrl;
  String catalogUrl;
  String attachmentUrl;
  String mediaPackage;
  String[] catalogKeys;
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(UploadTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + UploadTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    client = Main.getClient();
    trackUrl = Main.getBaseUrl() + "/workflow/samples/camera.mpg";
    catalogUrl = Main.getBaseUrl() + "/workflow/samples/dc-1.xml";
    attachmentUrl = Main.getBaseUrl() + "/workflow/samples/index.txt";
    catalogKeys = new String[] { "format", "promoted", "description", "subject", "publisher", "identifier", "title" };
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testIngestThinClient() throws Exception {

    // Create Media Package
    HttpResponse response = IngestResources.createMediaPackage(client);
    assertEquals("Response code (createMediaPacakge):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Add Track
    response = IngestResources.add(client, "Track", trackUrl, "presenter/source", mediaPackage);
    assertEquals("Response code (addTrack):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Add Catalog
    response = IngestResources.add(client, "Catalog", catalogUrl, "dublincore/episode", mediaPackage);
    assertEquals("Response code (addCatalog):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Add Attachment
    response = IngestResources.add(client, "Attachment", attachmentUrl, "attachment/txt", mediaPackage);
    assertEquals("Response code (addAttachment):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Ingest
    response = IngestResources.ingest(client, mediaPackage);
    assertEquals("Response code (ingest):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    Document xml = Utils.parseXml(IOUtils.toInputStream(mediaPackage, "UTF-8"));
    String workflowId = (String) Utils.xpath(xml, "//*[local-name() = 'workflow']/@id", XPathConstants.STRING);
    String mediaPackageId = (String) Utils.xpath(xml, "//*[local-name() = 'mediapackage']/@id", XPathConstants.STRING);

    // Confirm ingest
    response = IngestResources.getWorkflowInstance(client, workflowId);
    assertEquals("Response code (workflow instance):", 200, response.getStatusLine().getStatusCode());

    // Compare Track
    String ingestedTrackUrl = (String) Utils.xpath(xml,
            "//*[local-name() = 'media']/*[local-name() = 'track'][@type='presenter/source']/*[local-name() = 'url']",
            XPathConstants.STRING);
    String ingestedMd5 = Utils.md5(Utils.getUrlAsFile(ingestedTrackUrl));
    String trackMd5 = Utils.md5(Utils.getUrlAsFile(trackUrl));
    assertEquals("Media Track Checksum:", ingestedMd5, trackMd5);

    // Compare Catalog
    String ingestedCatalogUrl = (String) Utils
            .xpath(xml,
                    "//*[local-name() = 'metadata']/*[local-name() = 'catalog'][@type='dublincore/episode']/*[local-name() = 'url']",
                    XPathConstants.STRING);
    Document ingestedCatalog = Utils.getUrlAsDocument(ingestedCatalogUrl);
    Document catalog = Utils.getUrlAsDocument(catalogUrl);
    for (String key : catalogKeys) {
      assertEquals("Catalog " + key + ":",
              ((String) Utils.xpath(ingestedCatalog, "//dcterms:" + key, XPathConstants.STRING)).trim(),
              ((String) Utils.xpath(catalog, "//dcterms:" + key, XPathConstants.STRING)).trim());
    }

    // Compare Attachment
    String ingestedAttachmentUrl = (String) Utils
            .xpath(xml,
                    "//*[local-name() = 'attachments']/*[local-name() = 'attachment'][@type='attachment/txt']/*[local-name() = 'url']",
                    XPathConstants.STRING);
    ingestedMd5 = Utils.md5(Utils.getUrlAsFile(ingestedAttachmentUrl));
    String attachmentMd5 = Utils.md5(Utils.getUrlAsFile(attachmentUrl));
    assertEquals("Attachment Checksum:", ingestedMd5, attachmentMd5);

    // Confirm search indexing
    int retries = 0;
    int timeout = 20;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check workflow instance status
      response = SearchResources.episode(client, mediaPackageId);
      assertEquals("Response code (episode):", 200, response.getStatusLine().getStatusCode());
      xml = Utils.parseXml(response.getEntity().getContent());
      if (Utils.xpathExists(xml,
              "//*[local-name() = 'result'][@id='" + mediaPackageId + "']/*[local-name() = 'mediapackage']").equals(
              true)) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("Search Service failed to index file.");
    }

  }

}
