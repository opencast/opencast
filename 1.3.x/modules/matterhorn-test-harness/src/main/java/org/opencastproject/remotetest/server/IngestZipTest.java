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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Posts a zip file to the ingest service
 */
public class IngestZipTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(IngestZipTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + IngestZipTest.class.getName());
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
  public void testIngestZip() throws Exception {
    byte[] bytesToPost = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("ingest.zip"));
    HttpPost post = new HttpPost(BASE_URL + "/ingest/addZippedMediaPackage");
    post.setEntity(new ByteArrayEntity(bytesToPost));
    HttpResponse response = client.execute(post);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());    
  }
}
