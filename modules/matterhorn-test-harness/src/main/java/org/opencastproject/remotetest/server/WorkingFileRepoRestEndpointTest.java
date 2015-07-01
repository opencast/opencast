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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Tests the working file repository's rest endpoint
 */
public class WorkingFileRepoRestEndpointTest {

  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepoRestEndpointTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + WorkingFileRepoRestEndpointTest.class.getName());
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
  public void testPutAndGetFile() throws Exception {
    // Store a file in the repository
    String mediapackageId = "123";
    String elementId = "456";
    byte[] bytesFromPost = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("opencast_header.gif"));
    InputStream in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
    String fileName = "our_logo.gif"; // Used to simulate a file upload
    MultipartEntity postEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    postEntity.addPart("file", new InputStreamBody(in, fileName));
    HttpPost post = new HttpPost(BASE_URL + "/files/mediapackage/" + mediapackageId + "/" + elementId);
    post.setEntity(postEntity);
    HttpResponse response = client.execute(post);
    HttpEntity responseEntity  = response.getEntity();
    String stringResponse = EntityUtils.toString(responseEntity);
    String expectedResponse = BASE_URL + "/files/mediapackage/" + mediapackageId + "/" + elementId + "/" + fileName;
    Assert.assertEquals(expectedResponse, stringResponse);

    // Get the file back from the repository
    HttpGet get = new HttpGet(BASE_URL + "/files/mediapackage/" + mediapackageId + "/" + elementId);
    HttpResponse getResponse = client.execute(get);
    byte[] bytesFromGet = IOUtils.toByteArray(getResponse.getEntity().getContent());

    // Ensure that the bytes that we posted are the same we received
    Assert.assertTrue(Arrays.equals(bytesFromGet, bytesFromPost));
  }
}
