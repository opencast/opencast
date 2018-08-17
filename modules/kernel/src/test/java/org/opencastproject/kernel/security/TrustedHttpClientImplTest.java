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

package org.opencastproject.kernel.security;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.kernel.http.api.HttpClient;
import org.opencastproject.kernel.http.impl.HttpClientFactory;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import com.entwinemedia.fn.data.Opt;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;

import junit.framework.Assert;

public class TrustedHttpClientImplTest {

  private TrustedHttpClientImpl client;
  private ComponentContext componentContextMock;
  private BundleContext bundleContextMock;
  private BasicHttpResponse okResponse;
  private BasicHttpResponse digestResponse;
  private BasicHttpResponse nonceResponse;
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;

  @Before
  public void setUp() throws Exception {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("3");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);
    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    long currentJobId = 20L;
    Job currentJob = createNiceMock(Job.class);
    expect(currentJob.getId()).andReturn(currentJobId).anyTimes();
    replay(currentJob);

    securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    expect(securityService.getUser()).andReturn(new JaxbUser()).anyTimes();
    replay(securityService);

    serviceRegistry = createNiceMock(ServiceRegistry.class);
    expect(serviceRegistry.getCurrentJob()).andReturn(currentJob).anyTimes();
    expect(serviceRegistry.getJob(currentJobId)).andReturn(currentJob).anyTimes();
    replay(serviceRegistry);

    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.activate(componentContextMock);
    // Setup responses.
    String digestValue = "Digest realm=\"testrealm@host.com\"," + "qop=\"auth,auth-int\","
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\"," + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
    okResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("Http", 1, 1), 200, "Good to go"));
    digestResponse = new BasicHttpResponse(new ProtocolVersion("Http", 1, 1), 401, "Unauthorized");
    digestResponse.addHeader("WWW-Authenticate", digestValue);
    nonceResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("Http", 1, 1), 401,
            "Nonce has expired/timed out"));
  }

  @Test
  public void noDefaultHttpConnectionFactoryResultsInException() {
    try {
      client.execute(new HttpPost("http://localhost:8080/fakeEndpoint"));
      // It should fail without a default http connection factory
      Assert.fail();
    } catch (TrustedHttpClientException e) {

    }
  }

  @Test
  public void nonceTimeoutRetryPropertySetsOkay() {
    // Test Default value
    componentContextMock = null;
    bundleContextMock = null;
    client = null;
    bundleContextMock = createNiceMock("DefaultRetryValueMock", BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    replay(bundleContextMock);

    componentContextMock = createMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.activate(componentContextMock);
    Assert.assertEquals(TrustedHttpClientImpl.DEFAULT_NONCE_TIMEOUT_RETRIES, client.getNonceTimeoutRetries());

    setupNonceRetries(0);
    Assert.assertEquals(0, client.getNonceTimeoutRetries());

    setupNonceRetries(1);
    Assert.assertEquals(1, client.getNonceTimeoutRetries());

    setupNonceRetries(100);
    Assert.assertEquals(100, client.getNonceTimeoutRetries());

    setupNonceRetries(Integer.MAX_VALUE);
    Assert.assertEquals(Integer.MAX_VALUE, client.getNonceTimeoutRetries());

    setupNonceRetries(Integer.MIN_VALUE);
    Assert.assertEquals(Integer.MIN_VALUE, client.getNonceTimeoutRetries());
  }

  private void setupNonceRetries(int retries) {
    componentContextMock = null;
    bundleContextMock = null;
    client = null;
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn(retries + "")
            .anyTimes();
    replay(bundleContextMock);
    componentContextMock = createNiceMock(ComponentContext.class);
    EasyMock.reset(componentContextMock);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.activate(componentContextMock);
  }

  @Test
  public void nonceRetryBaseTimePropertySetsOkay() {
    // Test Default value
    componentContextMock = null;
    bundleContextMock = null;
    client = null;
    bundleContextMock = createNiceMock("DefaultRetryValueMock", BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    replay(bundleContextMock);

    componentContextMock = createMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.activate(componentContextMock);
    Assert.assertEquals(TrustedHttpClientImpl.DEFAULT_RETRY_BASE_TIME, client.getRetryBaseDelay());

    setupNonceBaseDelay(0);
    Assert.assertEquals(0, client.getRetryBaseDelay());

    setupNonceBaseDelay(1);
    Assert.assertEquals(1, client.getRetryBaseDelay());

    setupNonceBaseDelay(100);
    Assert.assertEquals(100, client.getRetryBaseDelay());

    setupNonceBaseDelay(Integer.MAX_VALUE);
    Assert.assertEquals(Integer.MAX_VALUE, client.getRetryBaseDelay());

    setupNonceBaseDelay(Integer.MIN_VALUE);
    Assert.assertEquals(Integer.MIN_VALUE, client.getRetryBaseDelay());
  }

  private void setupNonceBaseDelay(int baseDelay) {
    componentContextMock = null;
    bundleContextMock = null;
    client = null;
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn(
            baseDelay + "").anyTimes();
    replay(bundleContextMock);
    componentContextMock = createNiceMock(ComponentContext.class);
    EasyMock.reset(componentContextMock);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.activate(componentContextMock);
  }

  @Test
  public void nonceRetryMaximumVariablePropertySetsOkay() {
    // Test Default value
    componentContextMock = null;
    bundleContextMock = null;
    client = null;
    bundleContextMock = createNiceMock("DefaultRetryValueMock", BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    replay(bundleContextMock);

    componentContextMock = createMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.activate(componentContextMock);
    Assert.assertEquals(TrustedHttpClientImpl.DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME, client.getRetryMaximumVariableTime());

    setupNonceMaximumVariableDelay(0);
    Assert.assertEquals(0, client.getRetryMaximumVariableTime());

    setupNonceMaximumVariableDelay(1);
    Assert.assertEquals(1, client.getRetryMaximumVariableTime());

    setupNonceMaximumVariableDelay(100);
    Assert.assertEquals(100, client.getRetryMaximumVariableTime());

    setupNonceMaximumVariableDelay(Integer.MAX_VALUE);
    Assert.assertEquals(Integer.MAX_VALUE, client.getRetryMaximumVariableTime());

    setupNonceMaximumVariableDelay(Integer.MIN_VALUE);
    Assert.assertEquals(Integer.MIN_VALUE, client.getRetryMaximumVariableTime());
  }

  private void setupNonceMaximumVariableDelay(int maxVariableDelay) {
    componentContextMock = null;
    bundleContextMock = null;
    client = null;
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn(maxVariableDelay + "").anyTimes();
    replay(bundleContextMock);
    componentContextMock = createNiceMock(ComponentContext.class);
    EasyMock.reset(componentContextMock);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.activate(componentContextMock);
  }

  @Test
  public void successfullRequestResultsInNoRetries() throws ClientProtocolException, IOException {
    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    ClientConnectionManager clientConnectionManager = createMock(ClientConnectionManager.class);

    // Setup DefaultHttpClients
    HttpClient securityDefaultHttpClient = createMock("Digest", HttpClient.class);
    expect(securityDefaultHttpClient.getParams()).andReturn(httpParams).anyTimes();
    expect(securityDefaultHttpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(securityDefaultHttpClient.getConnectionManager()).andReturn(clientConnectionManager);
    replay(securityDefaultHttpClient);

    HttpClient requestDefaultHttpClient = createMock("Request", HttpClient.class);
    expect(requestDefaultHttpClient.getParams()).andReturn(httpParams).anyTimes();
    expect(requestDefaultHttpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    replay(requestDefaultHttpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(requestDefaultHttpClient);
    expect(httpClientFactory.makeHttpClient()).andReturn(securityDefaultHttpClient);
    replay(httpClientFactory);

    client.setHttpClientFactory(httpClientFactory);

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void failsIfNonceReturnAndNoRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    ClientConnectionManager clientConnectionManager = createMock(ClientConnectionManager.class);

    HttpClient requestDefaultHttpClient = createMock("Request", HttpClient.class);
    expect(requestDefaultHttpClient.getParams()).andReturn(httpParams).anyTimes();
    // Digest authentication and close
    expect(requestDefaultHttpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(requestDefaultHttpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Try request and close.
    expect(requestDefaultHttpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    replay(requestDefaultHttpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(requestDefaultHttpClient).atLeastOnce();
    replay(httpClientFactory);

    client.setHttpClientFactory(httpClientFactory);

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());
    verify(requestDefaultHttpClient);
  }

  @Test
  public void failsIfNonceReturnAndOneRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("1");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    ClientConnectionManager clientConnectionManager = createMock(ClientConnectionManager.class);

    HttpClient httpClient = createMock("Request", HttpClient.class);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    // Security Handshake and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // First request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Second Security Handshake and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Retry request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client.setHttpClientFactory(httpClientFactory);

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());
  }

  @Test
  public void successIfNonceReturnOnceAndOneRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("1");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    ClientConnectionManager clientConnectionManager = createMock(ClientConnectionManager.class);

    HttpClient httpClient = createMock("Request", HttpClient.class);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    // Security Handshake and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // First request and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Nonce retry and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Final request with success.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client.setHttpClientFactory(httpClientFactory);

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void successIfNonceReturnOnceAndThreeRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("3");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    ClientConnectionManager clientConnectionManager = createMock(ClientConnectionManager.class);

    IMocksControl ctrl = EasyMock.createNiceControl();
    ctrl.checkOrder(false);

    HttpClient httpClient = createMock("Request", HttpClient.class);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    // First Digest handshake and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // First request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Second Digest handshake and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // First request retry.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client.setHttpClientFactory(httpClientFactory);

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void successIfNonceReturnThreeAndThreeRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn(
            "matterhorn_system_account");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("CHANGE_ME");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("3");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);
    client = new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    ClientConnectionManager clientConnectionManager = createMock(ClientConnectionManager.class);

    HttpClient httpClient = createMock("Request", HttpClient.class);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    // First Digest handshake and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // First request with a nonce timeout and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);

    // First retry getting nonce and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // First retry request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);

    // Second retry getting nonce and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Second retry request and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);

    // Third retry getting nonce and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    expect(httpClient.getConnectionManager()).andReturn(clientConnectionManager);
    // Third retry with successful request.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client.setHttpClientFactory(httpClientFactory);

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testNotAcceptsUrlSigningService() throws IOException {
    String notAcceptsUrl = "http://notaccepts.com";
    HttpGet get = new HttpGet(notAcceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(notAcceptsUrl)).andReturn(false);
    EasyMock.replay(urlSigningService);

    CredentialsProvider cp = EasyMock.createNiceMock(CredentialsProvider.class);
    Capture<HttpUriRequest> request = EasyMock.newCapture();

    // Setup Http Client
    HttpClient httpClient = createMock("Request", HttpClient.class);
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    expect(httpClient.getCredentialsProvider()).andReturn(cp);
    expect(httpClient.execute(EasyMock.capture(request))).andReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "ok")));
    EasyMock.replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client = new TrustedHttpClientImpl("user", "pass");
    client.setHttpClientFactory(httpClientFactory);
    client.setSecurityService(securityService);
    client.setUrlSigningService(urlSigningService);
    client.execute(get);
    assertTrue(request.hasCaptured());
    assertEquals(get.getURI().toString(), request.getValue().getURI().toString());
  }

  @Test
  public void testAcceptsUrlSigningService() throws IOException, UrlSigningException {
    String acceptsUrl = "http://accepts.com";
    String signedUrl = "http://accepts.com?policy=testPolicy&signature=testSignature&keyId=testKeyId";
    HttpGet get = new HttpGet(acceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(acceptsUrl)).andReturn(true);
    EasyMock.expect(urlSigningService.sign(EasyMock.anyString(), EasyMock.anyLong(), EasyMock.anyLong(),
            EasyMock.anyString())).andReturn(signedUrl);
    EasyMock.replay(urlSigningService);

    CredentialsProvider cp = EasyMock.createNiceMock(CredentialsProvider.class);
    Capture<HttpUriRequest> request = EasyMock.newCapture();

    // Setup Http Client
    HttpClient httpClient = createMock("Request", HttpClient.class);
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    expect(httpClient.getCredentialsProvider()).andReturn(cp);
    expect(httpClient.execute(EasyMock.capture(request))).andReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "ok")));
    EasyMock.replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client = new TrustedHttpClientImpl("user", "pass");
    client.setHttpClientFactory(httpClientFactory);
    client.setSecurityService(securityService);
    client.setUrlSigningService(urlSigningService);
    client.execute(get);
    assertTrue(request.hasCaptured());
    assertEquals(signedUrl, request.getValue().getURI().toString());
  }

  @Test
  public void testAlreadySignedUrlIgnoredByUrlSigningService() throws IOException, UrlSigningException {
    String acceptsUrl = "http://alreadysigned.com?signature=thesig&policy=thepolicy&keyId=thekey";
    HttpHead headRequest = new HttpHead(acceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(acceptsUrl)).andReturn(true);
    EasyMock.replay(urlSigningService);

    CredentialsProvider cp = EasyMock.createNiceMock(CredentialsProvider.class);
    Capture<HttpUriRequest> request = EasyMock.newCapture();

    // Setup Http Client
    HttpClient httpClient = createMock("Request", HttpClient.class);
    HttpParams httpParams = createNiceMock(HttpParams.class);
    expect(httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)).andReturn(httpParams);
    expect(httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)).andReturn(httpParams);
    replay(httpParams);
    expect(httpClient.getParams()).andReturn(httpParams).anyTimes();
    expect(httpClient.getCredentialsProvider()).andReturn(cp);
    expect(httpClient.execute(EasyMock.capture(request))).andReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "ok")));
    EasyMock.replay(httpClient);

    // Setup DefaultHttpClientFactory
    HttpClientFactory httpClientFactory = createMock(HttpClientFactory.class);
    expect(httpClientFactory.makeHttpClient()).andReturn(httpClient).atLeastOnce();
    replay(httpClientFactory);

    client = new TrustedHttpClientImpl("user", "pass");
    client.setHttpClientFactory(httpClientFactory);
    client.setSecurityService(securityService);
    client.setUrlSigningService(urlSigningService);
    client.execute(headRequest);
    assertTrue(request.hasCaptured());
    assertEquals(acceptsUrl, request.getValue().getURI().toString());
  }

  @Test
  public void testGetSignedUrl() throws IOException, UrlSigningException {
    HttpGet notAccepted = new HttpGet("http://notAccepted.com");
    HttpGet alreadySigned = new HttpGet("http://alreadySigned.com?signature=thesignature&keyId=theKeyId&policy=thePolicy");
    HttpPost notGetOrHead = new HttpPost("http://notGetOrHead.com");
    HttpGet ok = new HttpGet("http://ok.com");
    String signedOk = "http://ok.com?signature=thesignature&keyId=theKeyId&policy=thePolicy";

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(notAccepted.getURI().toString())).andReturn(false);
    EasyMock.expect(urlSigningService.accepts(alreadySigned.getURI().toString())).andReturn(true);
    EasyMock.expect(urlSigningService.accepts(notGetOrHead.getURI().toString())).andReturn(true);
    EasyMock.expect(urlSigningService.accepts(ok.getURI().toString())).andReturn(true);
    EasyMock.expect(
            urlSigningService.sign(ok.getURI().toString(), TrustedHttpClientImpl.DEFAULT_URL_SIGNING_EXPIRES_DURATION,
                    null, null)).andReturn(signedOk);
    EasyMock.replay(urlSigningService);

    client = new TrustedHttpClientImpl("user", "pass");
    client.setUrlSigningService(urlSigningService);
    assertTrue(client.getSignedUrl(notAccepted).isNone());
    assertTrue(client.getSignedUrl(notGetOrHead).isNone());
    Opt<HttpUriRequest> result = client.getSignedUrl(ok);
    assertTrue(result.isSome());
    assertEquals(signedOk, result.get().getURI().toString());
  }
}
