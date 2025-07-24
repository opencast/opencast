/*
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.HostRegistrationInMemory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrustedHttpClientImplTest {

  private TrustedHttpClientImpl client;
  private ComponentContext componentContextMock;
  private BundleContext bundleContextMock;
  private CloseableHttpResponse okResponse;
  private CloseableHttpResponse digestResponse;
  private CloseableHttpResponse nonceResponse;
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private OrganizationDirectoryService organizationDirectoryService;

  @Before
  public void setUp() throws Exception {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
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

    DefaultOrganization defaultOrganization = new DefaultOrganization();

    securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(defaultOrganization).anyTimes();
    expect(securityService.getUser()).andReturn(new JaxbUser()).anyTimes();
    replay(securityService);

    organizationDirectoryService = createNiceMock(OrganizationDirectoryService.class);
    expect(organizationDirectoryService.getOrganizations()).andReturn(List.of(defaultOrganization));
    replay(organizationDirectoryService);

    HostRegistration localhost = new HostRegistrationInMemory("http://localhost:8080", "127.0.0.1",
        "admin", 4.0f, 2, 4194304);
    HostRegistration admin = new HostRegistrationInMemory("http://admin.example.org", "127.0.0.1",
        "admin", 4.0f, 2, 4194304);
    HostRegistration worker = new HostRegistrationInMemory("http://worker.example.org", "127.0.0.1",
        "worker", 4.0f, 4, 2097152);
    HostRegistration pres = new HostRegistrationInMemory("http://pres.example.org", "127.0.0.1",
        "worker", 4.0f, 4, 2097152);

    serviceRegistry = createNiceMock(ServiceRegistry.class);
    expect(serviceRegistry.getCurrentJob()).andReturn(currentJob).anyTimes();
    expect(serviceRegistry.getJob(currentJobId)).andReturn(currentJob).anyTimes();
    expect(serviceRegistry.getHostRegistrations()).andReturn(List.of(localhost, admin, worker, pres)).anyTimes();
    replay(serviceRegistry);

    client = new TrustedHttpClientImpl("u", "p");
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.activate(componentContextMock);
    // Setup responses.
    final String digestValue = "Digest realm=\"testrealm@host.com\","
        + "qop=\"auth,auth-int\","
        + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\","
        + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
    okResponse = EasyMock.createMock(CloseableHttpResponse.class);
    expect(okResponse.getStatusLine())
        .andReturn(new BasicStatusLine(new ProtocolVersion("Http", 1, 1), 200, "Good to go"))
        .anyTimes();
    expect(okResponse.getHeaders("WWW-Authenticate")).andReturn(new Header[] {}).anyTimes();
    BasicHttpResponse basicResponse = new BasicHttpResponse(new ProtocolVersion("Http", 1, 1), 401, "Unauthorized");
    basicResponse.addHeader("WWW-Authenticate", digestValue);
    digestResponse = EasyMock.createMock(CloseableHttpResponse.class);
    expect(digestResponse.getStatusLine())
        .andReturn(new BasicStatusLine(new ProtocolVersion("Http", 1, 1), 401, "Unauthorized"))
        .anyTimes();
    expect(digestResponse.getHeaders("WWW-Authenticate"))
        .andReturn(basicResponse.getHeaders("WWW-Authenticate"))
        .anyTimes();
    nonceResponse = EasyMock.createMock(CloseableHttpResponse.class);
    expect(nonceResponse.getStatusLine())
        .andReturn(new BasicStatusLine(new ProtocolVersion("Http", 1, 1), 401, "Nonce has expired/timed out"))
        .anyTimes();
    expect(nonceResponse.getHeaders("WWW-Authenticate")).andReturn(new Header[] {}).anyTimes();
    replay(okResponse, digestResponse, nonceResponse);
  }

  @Test
  public void noDefaultHttpConnectionFactoryResultsInException() {
    try {
      client.execute(new HttpPost("http://localhost:9/fakeEndpoint"));
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
  public void failsIfNonceReturnAndNoRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };

    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());

    verify(httpClient);
  }

  @Test
  public void failsIfNonceReturnAndOneRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("1");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    // First request
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    // Second Security Handshake
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // Retry request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());
  }

  @Test
  public void successIfNonceReturnOnceAndOneRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("1");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // First request
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    // Nonce retry
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // Final request with success.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");

    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void successIfNonceReturnOnceAndThreeRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("3");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    // First Digest handshake and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // First request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    // Second Digest handshake and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // First request retry.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void successIfNonceReturnThreeAndThreeRetries() throws ClientProtocolException, IOException {
    // Setup bundle context for TrustedHttpClientImpl
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY)).andReturn("3");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    // First Digest handshake and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // First request with a nonce timeout and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    // First retry getting nonce and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // First retry request and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    // Second retry getting nonce and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // Second retry request and close
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(nonceResponse);
    // Third retry getting nonce and close.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(digestResponse);
    // Third retry with successful request.
    expect(httpClient.execute(isA(HttpUriRequest.class))).andReturn(okResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setServiceRegistry(serviceRegistry);
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.activate(componentContextMock);

    HttpPost httpPost = new HttpPost("http://localhost:8080/fake");
    HttpResponse response = client.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testNotAcceptsUrlSigningService() throws IOException {
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    String notAcceptsUrl = "http://notaccepts.com";
    HttpGet get = new HttpGet(notAcceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(notAcceptsUrl)).andReturn(false);
    EasyMock.replay(urlSigningService);

    // Capture request
    Capture<HttpUriRequest> request = EasyMock.newCapture();

    // Setup Http Client
    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    expect(httpClient.execute(EasyMock.capture(request))).andReturn(okResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.setServiceRegistry(serviceRegistry);
    client.setUrlSigningService(urlSigningService);
    client.activate(componentContextMock);

    client.execute(get);
    assertTrue(request.hasCaptured());
    assertEquals(get.getURI().toString(), request.getValue().getURI().toString());
  }

  @Test
  public void testAlreadySignedUrlIgnoredByUrlSigningService() throws IOException, UrlSigningException {
    String acceptsUrl = "http://alreadysigned.com?signature=thesig&policy=thepolicy&keyId=thekey";
    HttpHead headRequest = new HttpHead(acceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(acceptsUrl)).andReturn(true);
    EasyMock.replay(urlSigningService);

    Capture<HttpUriRequest> request = EasyMock.newCapture();

    // Setup Http Client
    CloseableHttpClient httpClient = createMock(CloseableHttpClient.class);
    expect(httpClient.execute(EasyMock.capture(request))).andReturn(okResponse);
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();
    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();
    replay(httpClientBuilder, httpClient);
    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setSecurityService(securityService);
    client.setOrganizationDirectoryService(organizationDirectoryService);
    client.setServiceRegistry(serviceRegistry);
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
    assertEquals(client.getSignedUrl(notAccepted), notAccepted);
    assertEquals(client.getSignedUrl(notGetOrHead), notGetOrHead);
    assertEquals(client.getSignedUrl(ok).getURI().toString(), signedOk);
  }

  @Test
  public void testClusterDetection() throws IOException {

    Map<String, Integer> servers = new HashMap<>();
    servers.put("tenant-admin.example.org", 8080);
    servers.put("tenant-pres.example.org", 8080);
    servers = Collections.unmodifiableMap(servers);
    Organization fakeOrg = new DefaultOrganization(servers);

    SecurityService secSvc = createNiceMock(SecurityService.class);
    expect(secSvc.getOrganization()).andReturn(fakeOrg).anyTimes();
    expect(secSvc.getUser()).andReturn(new JaxbUser()).anyTimes();
    replay(secSvc);

    OrganizationDirectoryService orgDirSvc = createNiceMock(OrganizationDirectoryService.class);
    expect(orgDirSvc.getOrganizations()).andReturn(List.of(fakeOrg)).anyTimes();
    replay(orgDirSvc);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createNiceMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(EasyMock.anyString())).andReturn(false).anyTimes();
    EasyMock.replay(urlSigningService);

    Capture<HttpUriRequest> request = EasyMock.newCapture(CaptureType.ALL);

    // Setup Http Client
    CloseableHttpClient httpClient = createNiceMock(CloseableHttpClient.class);
    expect(httpClient.execute(EasyMock.capture(request))).andReturn(okResponse).anyTimes();
    httpClient.close();
    EasyMock.expectLastCall().anyTimes();

    HttpClientBuilder httpClientBuilder = createNiceMock(HttpClientBuilder.class);
    expect(httpClientBuilder.build()).andReturn(httpClient).anyTimes();

    replay(httpClientBuilder, httpClient);

    client = new TrustedHttpClientImpl("u", "p") {
      @Override
      public HttpClientBuilder makeHttpClientBuilder(int connectionTimeout, int socketTimeout) {
        return httpClientBuilder;
      }
    };
    client.setSecurityService(secSvc);
    client.setServiceRegistry(serviceRegistry);
    client.setOrganizationDirectoryService(orgDirSvc);
    client.setUrlSigningService(urlSigningService);

    //Reminder: the valid nodes for the current org are:
    // - http://localhost:80
    // - http://admin:8080
    //Note that we are *not* checking that the auth credentials are correct.  That's a final method, which won't mock.
    //Instead we check if the desire to use Digest auth is present.  That's close enough.
    client.execute(new HttpGet("http://localhost:8080/info/me.json"));
    //This isn't part of the cluster at all, don't send auth
    client.execute(new HttpGet("http://www.example.org"));
    //This is the admin node, but https rather than http as defined in the org.  Safe to send credentials, *but*
    // the host cache in TrustedHttpClientImpl uses Organization::getServers, which returns a string like http://admin
    // then compares the URI passed in to that string.  Thus, while we *could* send the auth, we don't since http!=https
    client.execute(new HttpGet("https://admin.example.org"));
    //This is the admin node, but on the wrong port (80 vs 8080).  Still safe to send credentials.
    //The behaviour here is different than the above because same cache as above discards the port defined by the server
    //This makes sense when you think of it - the *host* is the important part, not the port or protocol.
    client.execute(new HttpGet("http://admin.example.org"));
    //Similar to the admin node, but *not*.  Don't send credentials.
    client.execute(new HttpGet("http://tenant-admin.example.org"));
    client.execute(new HttpGet("https://tenant-pres.example.org"));

    assertTrue(Arrays.stream(request.getValues().get(0).getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue())));
    assertFalse(Arrays.stream(request.getValues().get(1).getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue())));
    assertTrue(Arrays.stream(request.getValues().get(2).getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue())));
    assertTrue(Arrays.stream(request.getValues().get(3).getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue())));
    assertTrue(Arrays.stream(request.getValues().get(4).getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue())));
    assertTrue(Arrays.stream(request.getValues().get(5).getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue())));
  }
}
