/**
 * Copyright 2009 The Regents of the University of California Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.remotetest.server;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

import org.opencastproject.remotetest.Main;
import static org.opencastproject.remotetest.Main.BASE_URL;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.opencastproject.remotetest.Main.BASE_URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets series feeds
 */
public class SeriesFeedTest {

  TrustedHttpClient client;
  private static final Logger logger = LoggerFactory.getLogger(SeriesFeedTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + SeriesFeedTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {

    client = Main.getClient();

  }

  @After
  public void tearDown() throws Exception {

    Main.returnClient(client);
  }

  // Assert MH-9569 is fixed
  @Test
  public void testEmptySeriesFeed() throws Exception {
    // Add a series
    HttpPost postSeries = new HttpPost(BASE_URL + "/series/");
    List<NameValuePair> seriesParams = new ArrayList<NameValuePair>();
    seriesParams.add(new BasicNameValuePair("series", getSampleSeries()));
    //seriesParams.add(new BasicNameValuePair("acl", getSampleAcl()));
    postSeries.setEntity(new UrlEncodedFormEntity(seriesParams, "UTF-8"));
    HttpResponse response = client.execute(postSeries);
    response.getEntity().consumeContent();
    Assert.assertEquals(201, response.getStatusLine().getStatusCode());

    HttpGet get = new HttpGet(BASE_URL + "/feeds/rss/2.0/series/10.245/5819");
    response = client.execute(get);
    HttpEntity entity = response.getEntity();
    String feed = EntityUtils.toString(entity);
    // Though empty should generate a valid feed for a valid series
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    // Remove series
    HttpDelete del = new HttpDelete(BASE_URL + "/series/10.245/5819");
    response = client.execute(del);
    response.getEntity().consumeContent();
  }

  @Test
  public void testNonExistentSeriesFeed() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/feeds/rss/2.0/series/SERIES_DOES_NOT_EXIST");
    HttpResponse response = client.execute(get);
    HttpEntity entity = response.getEntity();
    String feed = EntityUtils.toString(entity);
    Assert.assertEquals(404, response.getStatusLine().getStatusCode());
  }

  protected String getSampleSeries() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("authorization/sample-series-1.xml"),
            "UTF-8");
  }

  protected String getSampleAcl() throws Exception {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><acl xmlns=\"org.opencastproject.security\"><ace><role>admin</role><action>delete</action><allow>true</allow></ace></acl>";

  }
}
