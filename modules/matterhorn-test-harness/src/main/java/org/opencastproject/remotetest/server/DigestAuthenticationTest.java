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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DigestAuthenticationTest {
  private static final Logger logger = LoggerFactory.getLogger(DigestAuthenticationTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + DigestAuthenticationTest.class.getName());
  }

  @Test
  public void testDigestAuthenticatedGet() throws Exception {
    UsernamePasswordCredentials creds = new UsernamePasswordCredentials("matterhorn_system_account", "CHANGE_ME");
    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpGet get = new HttpGet(BASE_URL + "/welcome.html");
    get.addHeader("X-Requested-Auth", "Digest");
    try {
      httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
      HttpResponse response = httpclient.execute(get);
      String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      Assert.assertTrue(content.contains("Opencast Matterhorn"));
    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  @Test
  public void testBadDigestAuthenticatedGet() throws Exception {
    UsernamePasswordCredentials creds = new UsernamePasswordCredentials("matterhorn_system_account", "wrong_password");
    DefaultHttpClient httpclient = new DefaultHttpClient();
    httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
    HttpGet get = new HttpGet(BASE_URL + "/welcome.html");
    get.addHeader("X-Requested-Auth", "Digest");
    try {
      HttpResponse response = httpclient.execute(get);
      Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  @Test
  public void testUnauthenticatedGet() throws Exception {
    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpGet get = new HttpGet(BASE_URL + "/welcome.html");
    try {
      HttpResponse response = httpclient.execute(get);
      String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      Assert.assertTrue(content.contains("Login Page"));
      Assert.assertTrue( ! content.contains("Start Climbing"));
    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  @Test
  public void testDigestAuthenticatedPost() throws Exception {
    DefaultHttpClient httpclient = new DefaultHttpClient();

    // Perform a HEAD, and extract the realm and nonce
    HttpHead head = new HttpHead(BASE_URL);
    head.addHeader("X-Requested-Auth", "Digest");
    HttpResponse headResponse = httpclient.execute(head);
    Header authHeader = headResponse.getHeaders("WWW-Authenticate")[0];
    String nonce = null;
    String realm = null;
    for(HeaderElement element : authHeader.getElements()) {
      if("nonce".equals(element.getName())) {
        nonce = element.getValue();
      } else if("Digest realm".equals(element.getName())) {
        realm = element.getValue();
      }
    }
    // Build the post
    UsernamePasswordCredentials creds = new UsernamePasswordCredentials("matterhorn_system_account", "CHANGE_ME");
    HttpPost post = new HttpPost(BASE_URL + "/capture-admin/agents/testagent");
    post.addHeader("X-Requested-Auth", "Digest");
    httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("state", "idle"));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
    post.setEntity(entity);

    // Add the previously obtained nonce
    HttpContext localContext = new BasicHttpContext();
    DigestScheme digestAuth = new DigestScheme();
    digestAuth.overrideParamter("realm", realm);
    digestAuth.overrideParamter("nonce", nonce);
    localContext.setAttribute("preemptive-auth", digestAuth);

    // Send the POST
    try {
      HttpResponse response = httpclient.execute(post, localContext);
      String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      Assert.assertEquals("testagent set to idle", content);
    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

}
