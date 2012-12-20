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

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Posts a zip file to the ingest service
 */
public class IngestRestEndpointTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(IngestRestEndpointTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + IngestRestEndpointTest.class.getName());
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
  public void testIngestThinClient() throws Exception {
    // create emptiy MediaPackage
    HttpGet get = new HttpGet(BASE_URL + "/ingest/createMediaPackage");
    HttpResponse response = client.execute(get);
    HttpEntity entity = response.getEntity();
    String mp = EntityUtils.toString(entity);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    // Grow media package
    mp = postCall("/ingest/addTrack", "av.mov", "presenter/source", mp);
    mp = postCall("/ingest/addCatalog", "dublincore.xml", "dublincore/episode", mp);
    mp = postCall("/ingest/addAttachment", "cover.png", "cover/source", mp);

    // Ingest the new grown media package
    mp = postCall("/ingest/ingest", null, null, mp);
  }

  @Test
  public void testUploadClient() throws Exception {
    InputStream is = getClass().getResourceAsStream("/mp-test.zip");
    InputStreamBody fileContent = new InputStreamBody(is, "mp-test.zip");

    // create emptiy MediaPackage
    HttpPost postStart = new HttpPost(BASE_URL + "/ingest/addZippedMediaPackage");

    MultipartEntity mpEntity = new MultipartEntity();
    mpEntity.addPart("workflowDefinitionId", new StringBody("full"));
    mpEntity.addPart("userfile", fileContent);
    postStart.setEntity(mpEntity);

    HttpResponse response = client.execute(postStart);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  protected String postCall(String method, String mediaFile, String flavor, String mediaPackage)
          throws ClientProtocolException, IOException {
    HttpPost post = new HttpPost(BASE_URL + method);
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    if (mediaFile != null) {
      URL url = getClass().getClassLoader().getResource(mediaFile);
      formParams.add(new BasicNameValuePair("url", url.toString()));
    }
    if (flavor != null) {
      formParams.add(new BasicNameValuePair("flavor", flavor));
    }
    formParams.add(new BasicNameValuePair("mediaPackage", mediaPackage));
    post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    HttpResponse response = client.execute(post);

    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    return EntityUtils.toString(response.getEntity());
  }
}
