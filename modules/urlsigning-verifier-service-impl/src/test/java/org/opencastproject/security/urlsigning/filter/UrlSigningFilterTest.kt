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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.security.urlsigning.filter

import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.verifier.UrlSigningVerifier
import org.opencastproject.security.urlsigning.verifier.impl.UrlSigningVerifierImpl
import org.opencastproject.urlsigning.common.Policy
import org.opencastproject.urlsigning.common.ResourceRequest
import org.opencastproject.urlsigning.common.ResourceRequest.Status
import org.opencastproject.urlsigning.utils.ResourceRequestUtil

import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import org.osgi.service.cm.ConfigurationException

import java.io.IOException
import java.util.Dictionary
import java.util.Hashtable
import java.util.Properties
import java.util.stream.Collectors

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UrlSigningFilterTest {
    private var matchAllProperties: Dictionary<String, String>? = null
    private val keyId = "TheKeyID"
    private val key = "TheFullKey"
    private val clientIp = "10.0.0.1"

    @Before
    fun setUp() {
        matchAllProperties = Hashtable()
        matchAllProperties!!.put(UrlSigningFilter.URL_REGEX_PREFIX + ".foo", ".*")
    }

    @Test
    @Throws(ConfigurationException::class, IOException::class, ServletException::class)
    fun testSkippedIfNotGetOrHead() {
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn("POST")
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer("$BASE_URL/post/path"))
        val response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)
        val filterChain = EasyMock.createMock<FilterChain>(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)

        val filter = UrlSigningFilter()
        filter.updated(matchAllProperties)
        filter.doFilter(request, response, filterChain)
        EasyMock.verify(filterChain)
    }

    @Throws(Exception::class)
    private fun createSignedHttpServletRequest(method: String, path: String): HttpServletRequest {
        val fullUrl = BASE_URL + path
        val policy = Policy.mkSimplePolicy(fullUrl, DateTime(2056, 5, 13, 14, 32))
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn(method)
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(fullUrl))
        EasyMock.expect(request.queryString).andStubReturn(ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyId, key))
        EasyMock.expect(request.remoteAddr).andStubReturn(clientIp)
        return request
    }

    @Throws(Exception::class)
    private fun createUnSignedHttpServletRequest(method: String, path: String): HttpServletRequest {
        val fullUrl = BASE_URL + path
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn(method)
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(fullUrl))
        EasyMock.expect(request.queryString).andStubReturn("")
        EasyMock.expect(request.remoteAddr).andStubReturn(clientIp)
        return request
    }

    @Test
    @Throws(Exception::class)
    fun testURLMatching() {
        val keys = Hashtable<String, String>()
        keys[UrlSigningVerifierImpl.KEY_PREFIX + keyId] = key
        val urlSigningVerifier = UrlSigningVerifierImpl()
        urlSigningVerifier.updated(keys)

        val properties = Properties()
        properties.load(IOUtils.toInputStream(IOUtils.toString(javaClass.getResource("/UrlSigningFilter.properties"))))
        val filter = UrlSigningFilter()
        filter.updated(Hashtable(properties.entries.stream()
                .collect<Map<String, Any>, Any>(Collectors.toMap<Entry<Any, Any>, String, Any>(
                        { entry: Entry<Any, Any> -> entry.key },
                        Function<Entry<Any, Any>, Any> { it.value }))))

        // Check /files/collection/{collectionId}/{fileName} is protected
        var path = "/files/collection/collectionId/filename.mp4"
        var request = createSignedHttpServletRequest("GET", path)
        var response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)
        var filterChain = EasyMock.createMock<FilterChain>(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)

        // Check /files/collectionuri/{collectionId}/{fileName} is not protected
        path = "/files/collectionuri/collectionID/filename.mp4"
        request = createUnSignedHttpServletRequest("GET", path)
        response = EasyMock.createMock(HttpServletResponse::class.java)
        filterChain = EasyMock.createMock(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)

        // Check /files/mediapackage/{mediaPackageID}/{mediaPackageElementID} is protected
        path = "/files/mediapackage/mediaPackageID/mediaPackageElementID"
        request = createSignedHttpServletRequest("GET", path)
        response = EasyMock.createMock(HttpServletResponse::class.java)
        filterChain = EasyMock.createMock(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)

        // Check /files/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{fileName} is protected
        path = "/files/mediapackage/mediaPackageID/mediaPackageElementID/fileName"
        request = createSignedHttpServletRequest("GET", path)
        response = EasyMock.createMock(HttpServletResponse::class.java)
        filterChain = EasyMock.createMock(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)

        // Check /staticfiles/{uuid}
        path = "/staticfiles/uuid"
        request = createSignedHttpServletRequest("GET", path)
        response = EasyMock.createMock(HttpServletResponse::class.java)
        filterChain = EasyMock.createMock(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)

        // Check /staticfiles/{uuid}/url is not protected
        path = "/staticfiles/uuid/url"
        request = createSignedHttpServletRequest("GET", path)
        response = EasyMock.createMock(HttpServletResponse::class.java)
        filterChain = EasyMock.createMock(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)

        // Check /archive/archive/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{version} is protected
        path = "/archive/archive/mediapackage/mediaPackageID/mediaPackageElementID/version"
        request = createSignedHttpServletRequest("GET", path)
        response = EasyMock.createMock(HttpServletResponse::class.java)
        filterChain = EasyMock.createMock(FilterChain::class.java)
        filterChain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(filterChain, request, response)
        filter.setUrlSigningVerifier(urlSigningVerifier)
        filter.doFilter(request, response, filterChain)
    }

    @Test
    @Throws(Exception::class)
    fun testCorrectPolicyAndSignature() {
        val encryptionKeyId = "theKey"
        val acceptedUrl = "http://accepted.com"
        val acceptedKey = "ThisIsTheKey"
        val acceptedIp = "10.0.0.1"
        val future = DateTime(4749125399000L)
        val policy = Policy.mkSimplePolicy(acceptedUrl, future)
        val acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey)

        val acceptedRequest = ResourceRequest()
        acceptedRequest.status = Status.Ok

        // Setup the Mock Url Signing Service
        val urlSigningVerifier = EasyMock.createMock<UrlSigningVerifier>(UrlSigningVerifier::class.java)
        EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest)
        EasyMock.replay(urlSigningVerifier)

        val filter = UrlSigningFilter()
        filter.setUrlSigningVerifier(urlSigningVerifier)

        // Setup the Mock Request
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn("GET")
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(acceptedUrl))
        EasyMock.expect(request.queryString)
                .andStubReturn(acceptedQueryString)
        EasyMock.expect(request.remoteAddr).andStubReturn(acceptedIp)
        EasyMock.replay(request)

        val response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)

        // Setup the mock filter chain.
        val chain = EasyMock.createMock<FilterChain>(FilterChain::class.java)
        chain.doFilter(request, response)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(chain)

        filter.doFilter(request, response, chain)
        EasyMock.verify(chain)
    }

    @Test
    @Throws(Exception::class)
    fun testDeniedOnException() {
        val encryptionKeyId = "theKey"
        val acceptedUrl = "http://accepted.com"
        val acceptedKey = "ThisIsTheKey"
        val acceptedIp = "10.0.0.1"
        val future = DateTime(4749125399000L)
        val policy = Policy.mkSimplePolicy(acceptedUrl, future)
        val acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey)

        val acceptedRequest = ResourceRequest()
        acceptedRequest.status = Status.Ok

        // Setup the Mock Url Signing Service
        val urlSigningVerifier = EasyMock.createMock<UrlSigningVerifier>(UrlSigningVerifier::class.java)
        EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true))
                .andThrow(UrlSigningException.internalProviderError())
        EasyMock.replay(urlSigningVerifier)

        val filter = UrlSigningFilter()
        filter.updated(matchAllProperties)
        filter.setUrlSigningVerifier(urlSigningVerifier)

        // Setup the Mock Request
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn("GET")
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(acceptedUrl))
        EasyMock.expect(request.queryString)
                .andStubReturn(acceptedQueryString)
        EasyMock.expect(request.remoteAddr).andStubReturn(acceptedIp)
        EasyMock.replay(request)

        val response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)

        // Setup the mock filter chain.
        val chain = EasyMock.createStrictMock<FilterChain>(FilterChain::class.java)
        EasyMock.replay(chain)

        filter.doFilter(request, response, chain)
        EasyMock.verify(chain)
    }

    @Test
    @Throws(Exception::class)
    fun testDeniedOnBadRequest() {
        val encryptionKeyId = "theKey"
        val acceptedUrl = "http://accepted.com"
        val acceptedKey = "ThisIsTheKey"
        val acceptedIp = "10.0.0.1"
        val future = DateTime(4749125399000L)
        val policy = Policy.mkSimplePolicy(acceptedUrl, future)
        val acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey)

        val acceptedRequest = ResourceRequest()
        acceptedRequest.status = Status.BadRequest

        // Setup the Mock Url Signing Service
        val urlSigningVerifier = EasyMock.createMock<UrlSigningVerifier>(UrlSigningVerifier::class.java)
        EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest)
        EasyMock.replay(urlSigningVerifier)

        val filter = UrlSigningFilter()
        filter.updated(matchAllProperties)
        filter.setUrlSigningVerifier(urlSigningVerifier)

        // Setup the Mock Request
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn("GET")
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(acceptedUrl))
        EasyMock.expect(request.queryString)
                .andStubReturn(acceptedQueryString)
        EasyMock.expect(request.remoteAddr).andStubReturn(acceptedIp)
        EasyMock.replay(request)

        val response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)

        // Setup the mock filter chain.
        val chain = EasyMock.createMock<FilterChain>(FilterChain::class.java)
        EasyMock.replay(chain)

        filter.doFilter(request, response, chain)
        EasyMock.verify(chain)
    }

    @Test
    @Throws(Exception::class)
    fun testDeniedOnForbidden() {
        val encryptionKeyId = "theKey"
        val acceptedUrl = "http://accepted.com"
        val acceptedKey = "ThisIsTheKey"
        val acceptedIp = "10.0.0.1"
        val future = DateTime(4749125399000L)
        val policy = Policy.mkSimplePolicy(acceptedUrl, future)
        val acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey)

        val acceptedRequest = ResourceRequest()
        acceptedRequest.status = Status.Forbidden

        // Setup the Mock Url Signing Service
        val urlSigningVerifier = EasyMock.createMock<UrlSigningVerifier>(UrlSigningVerifier::class.java)
        EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest)
        EasyMock.replay(urlSigningVerifier)

        val filter = UrlSigningFilter()
        filter.updated(matchAllProperties)
        filter.setUrlSigningVerifier(urlSigningVerifier)

        // Setup the Mock Request
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn("GET")
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(acceptedUrl))
        EasyMock.expect(request.queryString)
                .andStubReturn(acceptedQueryString)
        EasyMock.expect(request.remoteAddr).andStubReturn(acceptedIp)
        EasyMock.replay(request)

        val response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)

        // Setup the mock filter chain.
        val chain = EasyMock.createMock<FilterChain>(FilterChain::class.java)
        EasyMock.replay(chain)

        filter.doFilter(request, response, chain)
        EasyMock.verify(chain)
    }

    @Test
    @Throws(Exception::class)
    fun testDeniedOnGone() {
        val encryptionKeyId = "theKey"
        val acceptedUrl = "http://accepted.com"
        val acceptedKey = "ThisIsTheKey"
        val acceptedIp = "10.0.0.1"
        val future = DateTime(4749125399000L)
        val policy = Policy.mkSimplePolicy(acceptedUrl, future)
        val acceptedQueryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, encryptionKeyId, acceptedKey)

        val acceptedRequest = ResourceRequest()
        acceptedRequest.status = Status.Gone

        // Setup the Mock Url Signing Service
        val urlSigningVerifier = EasyMock.createMock<UrlSigningVerifier>(UrlSigningVerifier::class.java)
        EasyMock.expect(urlSigningVerifier.verify(acceptedQueryString, acceptedIp, acceptedUrl, true)).andReturn(acceptedRequest)
        EasyMock.replay(urlSigningVerifier)

        val filter = UrlSigningFilter()
        filter.updated(matchAllProperties)
        filter.setUrlSigningVerifier(urlSigningVerifier)

        // Setup the Mock Request
        val request = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andStubReturn("GET")
        EasyMock.expect(request.requestURL).andStubReturn(StringBuffer(acceptedUrl))
        EasyMock.expect(request.queryString)
                .andStubReturn(acceptedQueryString)
        EasyMock.expect(request.remoteAddr).andStubReturn(acceptedIp)
        EasyMock.replay(request)

        val response = EasyMock.createMock<HttpServletResponse>(HttpServletResponse::class.java)

        // Setup the mock filter chain.
        val chain = EasyMock.createMock<FilterChain>(FilterChain::class.java)
        EasyMock.replay(chain)

        filter.doFilter(request, response, chain)
        EasyMock.verify(chain)
    }

    companion object {
        private val BASE_URL = "http://test.com"
    }
}
