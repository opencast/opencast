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
package org.opencastproject.security.urlsigning.filter;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.verifier.UrlSigningVerifier;
import org.opencastproject.security.urlsigning.verifier.impl.UrlSigningVerifierImpl;
import org.opencastproject.urlsigning.common.Policy;
import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.urlsigning.common.ResourceRequest.Status;
import org.opencastproject.urlsigning.utils.ResourceRequestUtil;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UrlSigningFilterTest {
  private static final String BASE_URL = "http://test.com";
  private Dictionary<String, String> matchAllProperties;
  private String keyId = "TheKeyID";
  private String key = "TheFullKey";
  private String clientIp = "10.0.0.1";

  @Before
  public void setUp() {
    matchAllProperties = new Hashtable<>();
    matchAllProperties.put(UrlSigningFilter.URL_REGEX_PREFIX + ".foo", ".*");
  }

  @Test
  public void testSkippedIfNotGetOrHead() throws ConfigurationException, IOException, ServletException {
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("POST");
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(BASE_URL + "/post/path"));
    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
    FilterChain filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);

    UrlSigningFilter filter = new UrlSigningFilter();
    filter.updated(matchAllProperties);
    filter.doFilter(request, response, filterChain);
    EasyMock.verify(filterChain);
  }

  private HttpServletRequest createSignedHttpServletRequest(String method, String path) throws Exception {
    String fullUrl = BASE_URL + path;
    Policy policy = Policy.mkSimplePolicy(fullUrl, new DateTime(2056, 05, 13, 14, 32));
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn(method);
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(fullUrl));
    EasyMock.expect(request.getQueryString()).andStubReturn(ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyId, key));
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(clientIp);
    return request;
  }

  private HttpServletRequest createUnSignedHttpServletRequest(String method, String path) throws Exception {
    String fullUrl = BASE_URL + path;
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn(method);
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(fullUrl));
    EasyMock.expect(request.getQueryString()).andStubReturn("");
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(clientIp);
    return request;
  }

  @Test
  public void testURLMatching() throws Exception {
    Dictionary<String, String> keys = new Hashtable<>();
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, key);
    UrlSigningVerifierImpl urlSigningVerifier = new UrlSigningVerifierImpl();
    urlSigningVerifier.updated(keys);

    Properties properties = new Properties();
    properties.load(IOUtils.toInputStream(IOUtils.toString(getClass().getResource("/UrlSigningFilter.properties"))));
    UrlSigningFilter filter = new UrlSigningFilter();
    filter.updated(new Hashtable<>(properties.entrySet().stream()
            .collect(Collectors.toMap(
                    (Map.Entry<Object, Object> entry) -> (String) entry.getKey(),
                    Map.Entry::getValue))));

    // Check /files/collection/{collectionId}/{fileName} is protected
    String path = "/files/collection/collectionId/filename.mp4";
    HttpServletRequest request = createSignedHttpServletRequest("GET", path);
    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
    FilterChain filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);

    // Check /files/collectionuri/{collectionId}/{fileName} is not protected
    path = "/files/collectionuri/collectionID/filename.mp4";
    request = createUnSignedHttpServletRequest("GET", path);
    response = EasyMock.createMock(HttpServletResponse.class);
    filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);

    // Check /files/mediapackage/{mediaPackageID}/{mediaPackageElementID} is protected
    path = "/files/mediapackage/mediaPackageID/mediaPackageElementID";
    request = createSignedHttpServletRequest("GET", path);
    response = EasyMock.createMock(HttpServletResponse.class);
    filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);

    // Check /files/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{fileName} is protected
    path = "/files/mediapackage/mediaPackageID/mediaPackageElementID/fileName";
    request = createSignedHttpServletRequest("GET", path);
    response = EasyMock.createMock(HttpServletResponse.class);
    filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);

    // Check /staticfiles/{uuid}
    path = "/staticfiles/uuid";
    request = createSignedHttpServletRequest("GET", path);
    response = EasyMock.createMock(HttpServletResponse.class);
    filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);

    // Check /staticfiles/{uuid}/url is not protected
    path = "/staticfiles/uuid/url";
    request = createSignedHttpServletRequest("GET", path);
    response = EasyMock.createMock(HttpServletResponse.class);
    filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);

    // Check /archive/archive/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{version} is protected
    path = "/archive/archive/mediapackage/mediaPackageID/mediaPackageElementID/version";
    request = createSignedHttpServletRequest("GET", path);
    response = EasyMock.createMock(HttpServletResponse.class);
    filterChain = EasyMock.createMock(FilterChain.class);
    filterChain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(filterChain, request, response);
    filter.setUrlSigningVerifier(urlSigningVerifier);
    filter.doFilter(request, response, filterChain);
  }

  @Test
  public void testCorrectPolicyAndSignature() throws Exception {
    String encryptionKeyId = "theKey";
    String acceptedUrl = "http://accepted.com";
    String acceptedKey = "ThisIsTheKey";
    String acceptedIp = "10.0.0.1";
    DateTime future = new DateTime(4749125399000L);
    Policy policy = Policy.mkSimplePolicy(acceptedUrl, future);
    String acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey);

    ResourceRequest acceptedRequest = new ResourceRequest();
    acceptedRequest.setStatus(Status.Ok);

    // Setup the Mock Url Signing Service
    UrlSigningVerifier urlSigningVerifier = EasyMock.createMock(UrlSigningVerifier.class);
    EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest);
    EasyMock.replay(urlSigningVerifier);

    UrlSigningFilter filter = new UrlSigningFilter();
    filter.setUrlSigningVerifier(urlSigningVerifier);

    // Setup the Mock Request
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("GET");
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(acceptedUrl));
    EasyMock.expect(request.getQueryString())
            .andStubReturn(acceptedQueryString);
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(acceptedIp);
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);

    // Setup the mock filter chain.
    FilterChain chain = EasyMock.createMock(FilterChain.class);
    chain.doFilter(request, response);
    EasyMock.expectLastCall();
    EasyMock.replay(chain);

    filter.doFilter(request, response, chain);
    EasyMock.verify(chain);
  }

  @Test
  public void testDeniedOnException() throws Exception {
    String encryptionKeyId = "theKey";
    String acceptedUrl = "http://accepted.com";
    String acceptedKey = "ThisIsTheKey";
    String acceptedIp = "10.0.0.1";
    DateTime future = new DateTime(4749125399000L);
    Policy policy = Policy.mkSimplePolicy(acceptedUrl, future);
    String acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey);

    ResourceRequest acceptedRequest = new ResourceRequest();
    acceptedRequest.setStatus(Status.Ok);

    // Setup the Mock Url Signing Service
    UrlSigningVerifier urlSigningVerifier = EasyMock.createMock(UrlSigningVerifier.class);
    EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true))
            .andThrow(UrlSigningException.internalProviderError());
    EasyMock.replay(urlSigningVerifier);

    UrlSigningFilter filter = new UrlSigningFilter();
    filter.updated(matchAllProperties);
    filter.setUrlSigningVerifier(urlSigningVerifier);

    // Setup the Mock Request
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("GET");
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(acceptedUrl));
    EasyMock.expect(request.getQueryString())
            .andStubReturn(acceptedQueryString);
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(acceptedIp);
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);

    // Setup the mock filter chain.
    FilterChain chain = EasyMock.createStrictMock(FilterChain.class);
    EasyMock.replay(chain);

    filter.doFilter(request, response, chain);
    EasyMock.verify(chain);
  }

  @Test
  public void testDeniedOnBadRequest() throws Exception {
    String encryptionKeyId = "theKey";
    String acceptedUrl = "http://accepted.com";
    String acceptedKey = "ThisIsTheKey";
    String acceptedIp = "10.0.0.1";
    DateTime future = new DateTime(4749125399000L);
    Policy policy = Policy.mkSimplePolicy(acceptedUrl, future);
    String acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey);

    ResourceRequest acceptedRequest = new ResourceRequest();
    acceptedRequest.setStatus(Status.BadRequest);

    // Setup the Mock Url Signing Service
    UrlSigningVerifier urlSigningVerifier = EasyMock.createMock(UrlSigningVerifier.class);
    EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest);
    EasyMock.replay(urlSigningVerifier);

    UrlSigningFilter filter = new UrlSigningFilter();
    filter.updated(matchAllProperties);
    filter.setUrlSigningVerifier(urlSigningVerifier);

    // Setup the Mock Request
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("GET");
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(acceptedUrl));
    EasyMock.expect(request.getQueryString())
            .andStubReturn(acceptedQueryString);
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(acceptedIp);
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);

    // Setup the mock filter chain.
    FilterChain chain = EasyMock.createMock(FilterChain.class);
    EasyMock.replay(chain);

    filter.doFilter(request, response, chain);
    EasyMock.verify(chain);
  }

  @Test
  public void testDeniedOnForbidden() throws Exception {
    String encryptionKeyId = "theKey";
    String acceptedUrl = "http://accepted.com";
    String acceptedKey = "ThisIsTheKey";
    String acceptedIp = "10.0.0.1";
    DateTime future = new DateTime(4749125399000L);
    Policy policy = Policy.mkSimplePolicy(acceptedUrl, future);
    String acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey);

    ResourceRequest acceptedRequest = new ResourceRequest();
    acceptedRequest.setStatus(Status.Forbidden);

    // Setup the Mock Url Signing Service
    UrlSigningVerifier urlSigningVerifier = EasyMock.createMock(UrlSigningVerifier.class);
    EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest);
    EasyMock.replay(urlSigningVerifier);

    UrlSigningFilter filter = new UrlSigningFilter();
    filter.updated(matchAllProperties);
    filter.setUrlSigningVerifier(urlSigningVerifier);

    // Setup the Mock Request
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("GET");
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(acceptedUrl));
    EasyMock.expect(request.getQueryString())
            .andStubReturn(acceptedQueryString);
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(acceptedIp);
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);

    // Setup the mock filter chain.
    FilterChain chain = EasyMock.createMock(FilterChain.class);
    EasyMock.replay(chain);

    filter.doFilter(request, response, chain);
    EasyMock.verify(chain);
  }

  @Test
  public void testDeniedOnGone() throws Exception {
    String encryptionKeyId = "theKey";
    String acceptedUrl = "http://accepted.com";
    String acceptedKey = "ThisIsTheKey";
    String acceptedIp = "10.0.0.1";
    DateTime future = new DateTime(4749125399000L);
    Policy policy = Policy.mkSimplePolicy(acceptedUrl, future);
    String acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey);

    ResourceRequest acceptedRequest = new ResourceRequest();
    acceptedRequest.setStatus(Status.Gone);

    // Setup the Mock Url Signing Service
    UrlSigningVerifier urlSigningVerifier = EasyMock.createMock(UrlSigningVerifier.class);
    EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest);
    EasyMock.replay(urlSigningVerifier);

    UrlSigningFilter filter = new UrlSigningFilter();
    filter.updated(matchAllProperties);
    filter.setUrlSigningVerifier(urlSigningVerifier);

    // Setup the Mock Request
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("GET");
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer(acceptedUrl));
    EasyMock.expect(request.getQueryString())
            .andStubReturn(acceptedQueryString);
    EasyMock.expect(request.getRemoteAddr()).andStubReturn(acceptedIp);
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);

    // Setup the mock filter chain.
    FilterChain chain = EasyMock.createMock(FilterChain.class);
    EasyMock.replay(chain);

    filter.doFilter(request, response, chain);
    EasyMock.verify(chain);
  }
}
