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

import org.opencastproject.remotetest.Main;

import junit.framework.Assert;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.OAuthResponseMessage;
import net.oauth.client.httpclient4.HttpClient4;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of a remote workflow service rest endpoint
 */
public class LtiAuthenticationTest {
  private static final Logger logger = LoggerFactory.getLogger(LtiAuthenticationTest.class);

  public static final String CONSUMER_KEY = "consumerkey";

  public static final String CONSUMER_SECRET = "consumersecret";

  public static final String LTI_USER_PREFIX = "lti:";

  public static final String LTI_CONSUMER_USER = "admin";

  public static final String LTI_CONSUMER_CONTEXT = "a sample course";

  public static final String LTI_CONSUMER_GUID = "sample-uuid";

  private DefaultHttpClient httpClient;
  private OAuthClient oauthClient;


  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + LtiAuthenticationTest.class.getName());
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    logger.info("Finished " + LtiAuthenticationTest.class.getName());
  }
  
  @Before
  public void setUp() throws Exception {
    httpClient = new DefaultHttpClient();
    oauthClient = new OAuthClient(new HttpClient4());
  }

  @After
  public void tearDown() throws Exception {
    httpClient.getConnectionManager().shutdown();
  }

  @Test
  public void testLtiLaunch() throws Exception {
    // Construct a POST message with the oauth parameters
    String nonce = UUID.randomUUID().toString();
    String timestamp = Long.toString(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    OAuthMessage oauthMessage = new OAuthMessage(OAuthMessage.POST, Main.BASE_URL + "/lti", null);
    oauthMessage.addParameter(OAuth.OAUTH_CONSUMER_KEY, CONSUMER_KEY);
    oauthMessage.addParameter(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    oauthMessage.addParameter(OAuth.OAUTH_NONCE, nonce);
    oauthMessage.addParameter(OAuth.OAUTH_TIMESTAMP, timestamp);
    
    // Add some LTI parameters
    oauthMessage.addParameter("user_id", LTI_CONSUMER_USER);
    oauthMessage.addParameter("context_id", LTI_CONSUMER_CONTEXT);
    oauthMessage.addParameter("consumer_gui", LTI_CONSUMER_GUID);
    oauthMessage.addParameter("custom_test", "true");
    
    // Sign the request
    OAuthConsumer consumer = new OAuthConsumer(null, CONSUMER_KEY, CONSUMER_SECRET, null);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    oauthMessage.sign(accessor);

    // Get the response
    OAuthResponseMessage oauthResponse = (OAuthResponseMessage)oauthClient.invoke(oauthMessage, ParameterStyle.BODY);
    
    // Make sure we got what we wanted
    Assert.assertEquals(HttpStatus.SC_OK, oauthResponse.getHttpResponse().getStatusCode());
    String cookie = oauthResponse.getHttpResponse().getHeader("Set-Cookie");
    Assert.assertNotNull(cookie);
    
    String sessionId = cookie.substring(0, cookie.lastIndexOf(";"));
    
    // Send a GET request to "/info/me.json" using this cookie
    HttpGet get = new HttpGet(Main.BASE_URL + "/info/me.json");
    get.setHeader("Cookie", sessionId);
    HttpResponse httpResponse = httpClient.execute(get);
    String me = EntityUtils.toString(httpResponse.getEntity());
    JSONObject meJson = (JSONObject) new JSONParser().parse(me);
    
    // Ensure that the "current user" was set by the LTI consumer
    Assert.assertEquals(LTI_USER_PREFIX + LTI_CONSUMER_USER, meJson.get("username"));
    
    // Send a GET request to "/lti" using this cookie
    get = new HttpGet(Main.BASE_URL + "/lti");
    get.setHeader("Cookie", sessionId);
    httpResponse = httpClient.execute(get);
    String lti = EntityUtils.toString(httpResponse.getEntity());
    JSONObject ltiJson = (JSONObject) new JSONParser().parse(lti);

    // Ensure that the LTI information sent by the tool consumer is available
    Assert.assertEquals(LTI_CONSUMER_CONTEXT, ltiJson.get("context_id"));
    
    // Make sure we can't use the same nonce twice
    try {
      oauthResponse = (OAuthResponseMessage)oauthClient.invoke(oauthMessage, ParameterStyle.BODY);
      Assert.fail();
    } catch(OAuthProblemException e) {
      // expected
    }
  }
  
  @Test
  public void testLtiLaunchFromUnknownConsumer() throws Exception {
    // Construct a POST message with the oauth parameters
    String nonce = UUID.randomUUID().toString();
    String timestamp = Long.toString(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    OAuthMessage oauthMessage = new OAuthMessage(OAuthMessage.POST, Main.BASE_URL + "/lti", null);
    oauthMessage.addParameter(OAuth.OAUTH_CONSUMER_KEY, CONSUMER_KEY);
    oauthMessage.addParameter(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    oauthMessage.addParameter(OAuth.OAUTH_NONCE, nonce);
    oauthMessage.addParameter(OAuth.OAUTH_TIMESTAMP, timestamp);
    
    // Add some LTI parameters
    oauthMessage.addParameter("user_id", LTI_CONSUMER_USER);
    oauthMessage.addParameter("context_id", LTI_CONSUMER_CONTEXT);
    oauthMessage.addParameter("custom_test", "true");
    
    // Sign the request
    OAuthConsumer consumer = new OAuthConsumer(null, CONSUMER_KEY, "wrong secret", null);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    oauthMessage.sign(accessor);

    // Get the response
    try {
      oauthClient.invoke(oauthMessage, ParameterStyle.BODY);
      Assert.fail("OAuth with a bad signature should result in an exception");
    } catch(OAuthProblemException e) {
      // expected
    }
    
  }


  @Test
  public void testLtiLaunchFromUnknownUser() throws Exception {
    // Construct a POST message with the oauth parameters
    String nonce = UUID.randomUUID().toString();
    String timestamp = Long.toString(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    String unknownUserId = "somebody_unknown_to_matterhorn";

    OAuthMessage oauthMessage = new OAuthMessage(OAuthMessage.POST, Main.BASE_URL + "/lti", null);
    oauthMessage.addParameter(OAuth.OAUTH_CONSUMER_KEY, CONSUMER_KEY);
    oauthMessage.addParameter(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    oauthMessage.addParameter(OAuth.OAUTH_NONCE, nonce);
    oauthMessage.addParameter(OAuth.OAUTH_TIMESTAMP, timestamp);
    
    // Add some LTI parameters
    oauthMessage.addParameter("user_id", unknownUserId);
    oauthMessage.addParameter("context_id", LTI_CONSUMER_CONTEXT);
    oauthMessage.addParameter("custom_test", "true");
    
    // Sign the request
    OAuthConsumer consumer = new OAuthConsumer(null, CONSUMER_KEY, CONSUMER_SECRET, null);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    oauthMessage.sign(accessor);

    // Get the response
    OAuthResponseMessage oauthResponse = (OAuthResponseMessage)oauthClient.invoke(oauthMessage, ParameterStyle.BODY);
    
    // Make sure we got what we wanted
    Assert.assertEquals(HttpStatus.SC_OK, oauthResponse.getHttpResponse().getStatusCode());
    String cookie = oauthResponse.getHttpResponse().getHeader("Set-Cookie");
    Assert.assertNotNull(cookie);
    
    String sessionId = cookie.substring(0, cookie.lastIndexOf(";"));
    
    // Send a GET request to "/info/me.json" using this cookie
    HttpGet get = new HttpGet(Main.BASE_URL + "/info/me.json");
    get.setHeader("Cookie", sessionId);
    HttpResponse httpResponse = httpClient.execute(get);
    String me = EntityUtils.toString(httpResponse.getEntity());
    JSONObject meJson = (JSONObject) new JSONParser().parse(me);
    
    // Ensure that the "current user" was set by the LTI consumer
    Assert.assertEquals(LTI_USER_PREFIX + unknownUserId, meJson.get("username"));
  }

}
