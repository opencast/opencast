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
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrustedHttpClientImplTest {

  private static final String DIGEST_CHALLENGE = "Digest realm=\"testrealm@host.com\","
      + "qop=\"auth,auth-int\","
      + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\","
      + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

  private TrustedHttpClientImpl client;
  private ComponentContext componentContextMock;
  private BundleContext bundleContextMock;
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private OrganizationDirectoryService organizationDirectoryService;

  /**
   * A trusted http client which does not send anything but answers with canned responses and records the requests it
   * would have sent.
   */
  private static final class CannedResponseClient extends TrustedHttpClientImpl {
    private final Deque<ClassicHttpResponse> responses = new ArrayDeque<>();
    private final List<ClassicHttpRequest> requests = new ArrayList<>();

    CannedResponseClient(ClassicHttpResponse... cannedResponses) {
      super("u", "p");
      responses.addAll(Arrays.asList(cannedResponses));
    }

    @Override
    protected ClassicHttpResponse send(ClassicHttpRequest request, HttpClientContext context) {
      requests.add(request);
      if (responses.isEmpty()) {
        throw new IllegalStateException("No more canned responses");
      }
      return responses.pop();
    }

    List<ClassicHttpRequest> getRequests() {
      return requests;
    }

    int remainingResponses() {
      return responses.size();
    }
  }

  private static ClassicHttpResponse okResponse() {
    return new BasicClassicHttpResponse(200, "Good to go");
  }

  private static ClassicHttpResponse digestResponse() {
    final BasicClassicHttpResponse response = new BasicClassicHttpResponse(401, "Unauthorized");
    response.addHeader(HttpHeaders.WWW_AUTHENTICATE, DIGEST_CHALLENGE);
    return response;
  }

  private static ClassicHttpResponse staleNonceResponse() {
    final BasicClassicHttpResponse response = new BasicClassicHttpResponse(401, "Unauthorized");
    response.addHeader(HttpHeaders.WWW_AUTHENTICATE, DIGEST_CHALLENGE + ",stale=\"true\"");
    return response;
  }

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
  }

  /** Wires the mocked services into a canned client and activates it with the given nonce retry configuration. */
  private CannedResponseClient cannedClient(int nonceRetries, ClassicHttpResponse... responses) {
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_USER_KEY)).andReturn("u");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.DIGEST_AUTH_PASS_KEY)).andReturn("p");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_KEY))
        .andReturn(Integer.toString(nonceRetries));
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_BASE_TIME_KEY)).andReturn("0");
    expect(bundleContextMock.getProperty(TrustedHttpClientImpl.NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY))
            .andReturn("0");
    replay(bundleContextMock);

    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    final CannedResponseClient canned = new CannedResponseClient(responses);
    canned.setServiceRegistry(serviceRegistry);
    canned.setSecurityService(securityService);
    canned.setOrganizationDirectoryService(organizationDirectoryService);
    canned.activate(componentContextMock);
    return canned;
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
    Assert.assertEquals(TrustedHttpClientImpl.DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME,
        client.getRetryMaximumVariableTime());

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
  public void failsIfStaleNonceAndNoRetries() throws Exception {
    final CannedResponseClient canned = cannedClient(0, digestResponse(), staleNonceResponse());

    final ClassicHttpResponse response = canned.execute(new HttpPost("http://localhost:8080/fake"));
    Assert.assertEquals(401, response.getCode());
    Assert.assertEquals(0, canned.remainingResponses());
  }

  @Test
  public void failsIfStaleNonceAndOneRetry() throws Exception {
    final CannedResponseClient canned = cannedClient(1,
        digestResponse(), staleNonceResponse(), digestResponse(), staleNonceResponse());

    final ClassicHttpResponse response = canned.execute(new HttpPost("http://localhost:8080/fake"));
    Assert.assertEquals(401, response.getCode());
    Assert.assertEquals(0, canned.remainingResponses());
  }

  @Test
  public void successIfStaleNonceOnceAndOneRetry() throws Exception {
    final CannedResponseClient canned = cannedClient(1,
        digestResponse(), staleNonceResponse(), digestResponse(), okResponse());

    final ClassicHttpResponse response = canned.execute(new HttpPost("http://localhost:8080/fake"));
    Assert.assertEquals(200, response.getCode());
    Assert.assertEquals(0, canned.remainingResponses());
  }

  @Test
  public void successIfStaleNonceThreeTimesAndThreeRetries() throws Exception {
    final CannedResponseClient canned = cannedClient(3,
        digestResponse(), staleNonceResponse(),
        digestResponse(), staleNonceResponse(),
        digestResponse(), staleNonceResponse(),
        digestResponse(), okResponse());

    final ClassicHttpResponse response = canned.execute(new HttpPost("http://localhost:8080/fake"));
    Assert.assertEquals(200, response.getCode());
    Assert.assertEquals(0, canned.remainingResponses());
  }

  @Test
  public void plainUnauthorizedIsNotRetried() throws Exception {
    final CannedResponseClient canned = cannedClient(3, digestResponse(), digestResponse());

    final ClassicHttpResponse response = canned.execute(new HttpPost("http://localhost:8080/fake"));
    Assert.assertEquals(401, response.getCode());
    Assert.assertEquals(0, canned.remainingResponses());
  }

  @Test
  public void nonRepeatableRequestIsNotRetried() throws Exception {
    final CannedResponseClient canned = cannedClient(3,
        digestResponse(), staleNonceResponse(), digestResponse(), okResponse());

    final HttpPost post = new HttpPost("http://localhost:8080/fake");
    post.setEntity(new InputStreamEntity(new ByteArrayInputStream(new byte[] { 1, 2, 3 }),
        ContentType.APPLICATION_OCTET_STREAM));
    final ClassicHttpResponse response = canned.execute(post);
    Assert.assertEquals(401, response.getCode());
    Assert.assertEquals(2, canned.remainingResponses());
  }

  @Test
  public void digestAuthorizationIsComputedFromChallenge() throws Exception {
    final CannedResponseClient canned = cannedClient(0, digestResponse(), okResponse());

    final HttpPost post = new HttpPost("http://localhost:8080/fake?a=b");
    post.setEntity(new StringEntity("data", ContentType.TEXT_PLAIN));
    final ClassicHttpResponse response = canned.execute(post);
    Assert.assertEquals(200, response.getCode());

    final List<ClassicHttpRequest> requests = canned.getRequests();
    Assert.assertEquals(2, requests.size());
    // The probe carries no body but asks for digest authentication
    final ClassicHttpRequest probe = requests.get(0);
    Assert.assertEquals("POST", probe.getMethod());
    Assert.assertEquals(post.getUri(), probe.getUri());
    Assert.assertNull(probe.getEntity());
    Assert.assertEquals(DelegatingAuthenticationEntryPoint.DIGEST_AUTH,
        probe.getFirstHeader(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER).getValue());
    // The actual request carries the authorization computed from the challenge
    final Header authorization = requests.get(1).getFirstHeader(HttpHeaders.AUTHORIZATION);
    assertNotNull(authorization);
    final String value = authorization.getValue();
    assertTrue(value, value.startsWith("Digest "));
    assertTrue(value, value.contains("username=\"u\""));
    assertTrue(value, value.contains("realm=\"testrealm@host.com\""));
    assertTrue(value, value.contains("nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\""));
    assertTrue(value, value.contains("uri=\"/fake?a=b\""));
    assertTrue(value, value.contains("qop=auth"));
    assertTrue(value, value.contains("nc=00000001"));
    assertTrue(value, value.contains("cnonce="));
    assertTrue(value, value.contains("opaque=\"5ccc069c403ebaf9f0171e9517f40e41\""));
    assertTrue(value, value.contains("response=\""));
  }

  @Test
  public void testNotAcceptsUrlSigningService() throws Exception {
    String notAcceptsUrl = "http://notaccepts.com/";
    HttpGet get = new HttpGet(notAcceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(notAcceptsUrl)).andReturn(false);
    EasyMock.replay(urlSigningService);

    final CannedResponseClient canned = cannedClient(0, okResponse());
    canned.setUrlSigningService(urlSigningService);

    canned.execute(get);
    assertEquals(1, canned.getRequests().size());
    assertEquals(get.getUri().toString(), canned.getRequests().get(0).getUri().toString());
  }

  @Test
  public void testAlreadySignedUrlIgnoredByUrlSigningService() throws Exception {
    String acceptsUrl = "http://alreadysigned.com/?signature=thesig&policy=thepolicy&keyId=thekey";
    HttpHead headRequest = new HttpHead(acceptsUrl);

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(acceptsUrl)).andReturn(true);
    EasyMock.replay(urlSigningService);

    final CannedResponseClient canned = cannedClient(0, okResponse());
    canned.setUrlSigningService(urlSigningService);

    canned.execute(headRequest);
    assertEquals(1, canned.getRequests().size());
    assertEquals(acceptsUrl, canned.getRequests().get(0).getUri().toString());
  }

  @Test
  public void testGetSignedUrl() throws IOException, UrlSigningException, URISyntaxException {
    HttpGet notAccepted = new HttpGet("http://notAccepted.com/");
    HttpGet alreadySigned =
        new HttpGet("http://alreadySigned.com/?signature=thesignature&keyId=theKeyId&policy=thePolicy");
    HttpPost notGetOrHead = new HttpPost("http://notGetOrHead.com/");
    HttpGet ok = new HttpGet("http://ok.com/");
    ok.addHeader("X-Test", "copied");
    String signedOk = "http://ok.com/?signature=thesignature&keyId=theKeyId&policy=thePolicy";

    // Setup signing service
    UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(notAccepted.getUri().toString())).andReturn(false);
    EasyMock.expect(urlSigningService.accepts(alreadySigned.getUri().toString())).andReturn(true);
    EasyMock.expect(urlSigningService.accepts(notGetOrHead.getUri().toString())).andReturn(true);
    EasyMock.expect(urlSigningService.accepts(ok.getUri().toString())).andReturn(true);
    EasyMock.expect(
            urlSigningService.sign(ok.getUri().toString(), TrustedHttpClientImpl.DEFAULT_URL_SIGNING_EXPIRES_DURATION,
                    null, null)).andReturn(signedOk);
    EasyMock.replay(urlSigningService);

    client = new TrustedHttpClientImpl("user", "pass");
    client.setUrlSigningService(urlSigningService);
    assertEquals(client.getSignedUrl(notAccepted), notAccepted);
    assertEquals(client.getSignedUrl(notGetOrHead), notGetOrHead);
    final ClassicHttpRequest signed = client.getSignedUrl(ok);
    assertEquals(signed.getUri().toString(), signedOk);
    assertEquals("copied", signed.getFirstHeader("X-Test").getValue());
  }

  @Test
  public void testClusterDetection() throws Exception {

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

    final CannedResponseClient canned = cannedClient(0,
        okResponse(), okResponse(), okResponse(), okResponse(), okResponse(), okResponse());
    canned.setSecurityService(secSvc);
    canned.setOrganizationDirectoryService(orgDirSvc);
    canned.setUrlSigningService(urlSigningService);

    //Reminder: the valid nodes for the current org are:
    // - http://localhost:80
    // - http://admin:8080
    //Note that we are *not* checking that the auth credentials are correct.
    //Instead we check if the desire to use Digest auth is present.  That's close enough.
    canned.execute(new HttpGet("http://localhost:8080/info/me.json"));
    //This isn't part of the cluster at all, don't send auth
    canned.execute(new HttpGet("http://www.example.org"));
    //This is the admin node, but https rather than http as defined in the org.  Safe to send credentials, *but*
    // the host cache in TrustedHttpClientImpl uses Organization::getServers, which returns a string like http://admin
    // then compares the URI passed in to that string.  Thus, while we *could* send the auth, we don't since http!=https
    canned.execute(new HttpGet("https://admin.example.org"));
    //This is the admin node, but on the wrong port (80 vs 8080).  Still safe to send credentials.
    //The behaviour here is different than the above because same cache as above discards the port defined by the server
    //This makes sense when you think of it - the *host* is the important part, not the port or protocol.
    canned.execute(new HttpGet("http://admin.example.org"));
    //Similar to the admin node, but *not*.  Don't send credentials.
    canned.execute(new HttpGet("http://tenant-admin.example.org"));
    canned.execute(new HttpGet("https://tenant-pres.example.org"));

    final List<ClassicHttpRequest> requests = canned.getRequests();
    assertTrue(requestsDigest(requests.get(0)));
    assertFalse(requestsDigest(requests.get(1)));
    assertTrue(requestsDigest(requests.get(2)));
    assertTrue(requestsDigest(requests.get(3)));
    assertTrue(requestsDigest(requests.get(4)));
    assertTrue(requestsDigest(requests.get(5)));
  }

  private static boolean requestsDigest(ClassicHttpRequest request) {
    return Arrays.stream(request.getHeaders(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER))
        .anyMatch(header -> DelegatingAuthenticationEntryPoint.DIGEST_AUTH.equals(header.getValue()));
  }
}
