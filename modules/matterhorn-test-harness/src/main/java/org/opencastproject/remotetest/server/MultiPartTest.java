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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Tests multipart requests against a digest auth protected URL.
 */
public class MultiPartTest {

  /** The http client */
  protected TrustedHttpClient httpClient;

  private static final Logger logger = LoggerFactory.getLogger(MultiPartTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + MultiPartTest.class.getName());
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
  public void testMultiPartPost() throws Exception {

    String mp = "<oc:mediapackage xmlns:oc=\"http://mediapackage.opencastproject.org\" id=\"10.0000/1\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\"></oc:mediapackage>";

    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/av.mov");
      InputStreamBody fileContent = new InputStreamBody(is, "av.mov");
      MultipartEntity mpEntity = new MultipartEntity();
      mpEntity.addPart("mediaPackage", new StringBody(mp));
      mpEntity.addPart("flavor", new StringBody("presentation/source"));
      mpEntity.addPart("userfile", fileContent);
      HttpPost httppost = new HttpPost(BASE_URL + "/ingest/addAttachment");
      httppost.setEntity(mpEntity);
      HttpResponse response = httpClient.execute(httppost);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

}
