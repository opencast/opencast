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
package org.opencastproject.urlsigning.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.opencastproject.urlsigning.common.Policy
import org.opencastproject.urlsigning.common.ResourceRequest
import org.opencastproject.urlsigning.common.ResourceRequest.Status

import org.apache.http.NameValuePair
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.net.URISyntaxException
import java.util.Properties

class ResourceRequestUtilTest {
    private var properties: Properties? = null

    @Before
    fun setUp() {
        properties = Properties()
        properties!![keyId] = key
    }

    @Test
    fun testQueryStringParsing() {
        val policyValue = "{policy:'value'}"
        val signatureValue = "randomString"

        val queryString = ("?" + ResourceRequest.POLICY_KEY + "=" + policyValue + "&" + ResourceRequest.SIGNATURE_KEY
                + "=" + signatureValue + "&" + ResourceRequest.ENCRYPTION_ID_KEY + "=" + keyId)

        val parameters = ResourceRequestUtil.parseQueryString(queryString)

        var foundOrg = false
        var foundPolicy = false
        var foundSignature = false

        for (nameValuePair in parameters) {
            if (ResourceRequest.ENCRYPTION_ID_KEY == nameValuePair.name) {
                assertEquals(keyId, nameValuePair.value)
                foundOrg = true
            }
            if (ResourceRequest.POLICY_KEY == nameValuePair.name) {
                assertEquals(policyValue, nameValuePair.value)
                foundPolicy = true
            }
            if (ResourceRequest.SIGNATURE_KEY == nameValuePair.name) {
                assertEquals(signatureValue, nameValuePair.value)
                foundSignature = true
            }
        }

        assertTrue("Didn't find the organization value.", foundOrg)
        assertTrue("Didn't find the policy value.", foundPolicy)
        assertTrue("Didn't find the signature value.", foundSignature)
    }

    @Test
    fun testAuthenticateDuplicateProperties() {
        // Test duplicate query properties.
        val twoOrgs = ResourceRequest.ENCRYPTION_ID_KEY + "=org1&" + ResourceRequest.ENCRYPTION_ID_KEY + "=org2"

        assertEquals(Status.BadRequest,
                ResourceRequestUtil.resourceRequestFromQueryString(twoOrgs, clientIp, null, properties, true).status)

        val twoPolicies = ResourceRequest.POLICY_KEY + "=policy1&" + ResourceRequest.POLICY_KEY + "=policy2"
        assertEquals(Status.BadRequest, ResourceRequestUtil
                .resourceRequestFromQueryString(twoPolicies, clientIp, null, properties, true).status)

        val twoSignatures = (ResourceRequest.SIGNATURE_KEY + "=signature1&" + ResourceRequest.SIGNATURE_KEY
                + "=signature1")
        assertEquals(Status.BadRequest, ResourceRequestUtil
                .resourceRequestFromQueryString(twoSignatures, clientIp, null, properties, true).status)
    }

    @Test
    fun testAuthenticateMissingProperties() {
        // Test Missing query properties
        val missingOrg = ResourceRequest.POLICY_KEY + "=policy&" + ResourceRequest.SIGNATURE_KEY + "=signature"
        assertEquals(Status.BadRequest, ResourceRequestUtil
                .resourceRequestFromQueryString(missingOrg, clientIp, null, properties, true).status)

        val missingPolicy = (ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.SIGNATURE_KEY
                + "=signature")
        assertEquals(Status.BadRequest, ResourceRequestUtil
                .resourceRequestFromQueryString(missingPolicy, clientIp, null, properties, true).status)

        val missingSignature = (ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.POLICY_KEY
                + "=policy")
        assertEquals(Status.BadRequest, ResourceRequestUtil
                .resourceRequestFromQueryString(missingSignature, clientIp, null, properties, true).status)
    }

    @Test
    @Throws(Exception::class)
    fun testAuthenticatePolicyMatchesSignature() {
        var after = DateTime(DateTimeZone.UTC)
        after = after.minus(2 * 60 * 60 * 1000L)
        var before = DateTime(DateTimeZone.UTC)
        before = before.plus(2 * 60 * 60 * 1000L)
        val nonMatchingResource = "http://other.com"
        val nonMatchingPolicy = Policy.mkSimplePolicy(nonMatchingResource, before)
        val matchingResource = "http://mh-allinone/"
        val matchingPolicy = Policy.mkSimplePolicy(matchingResource, before)
        val signature = PolicyUtils.getPolicySignature(matchingPolicy, key)

        // Test non-existant encryption key is forbidden.
        val wrongEncryptionKeyId = (ResourceRequest.ENCRYPTION_ID_KEY + "=" + "WrongId" + "&" + ResourceRequest.POLICY_KEY
                + "=" + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "="
                + signature)
        assertEquals(Status.Forbidden,
                ResourceRequestUtil
                        .resourceRequestFromQueryString(wrongEncryptionKeyId, clientIp, matchingResource, properties, true)
                        .status)

        // Test non matching resource results is forbidden.
        val nonMatching = (ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.POLICY_KEY + "="
                + PolicyUtils.toBase64EncodedPolicy(nonMatchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "="
                + signature)
        assertEquals(Status.Forbidden, ResourceRequestUtil
                .resourceRequestFromQueryString(nonMatching, clientIp, matchingResource, properties, true).status)

        // Test non-matching client ip results in forbidden.
        val wrongClientPolicy = Policy.mkPolicyValidWithIP(matchingResource, before, "10.0.0.255")
        val wrongClient = ResourceRequestUtil.policyToResourceRequestQueryString(wrongClientPolicy, keyId, key)
        assertEquals(Status.Forbidden, ResourceRequestUtil
                .resourceRequestFromQueryString(wrongClient, clientIp, matchingResource, properties, true).status)

        // Test matching client ip results in ok.
        val rightClientPolicy = Policy.mkPolicyValidWithIP(matchingResource, before, clientIp)
        val rightClient = ResourceRequestUtil.policyToResourceRequestQueryString(rightClientPolicy, keyId, key)
        assertEquals(Status.Ok, ResourceRequestUtil
                .resourceRequestFromQueryString(rightClient, clientIp, matchingResource, properties, true).status)

        // Test not yet DateGreaterThan results in gone
        val wrongDateGreaterThanPolicy = Policy.mkPolicyValidFrom(matchingResource, before, before)
        val wrongDateGreaterThan = ResourceRequestUtil.policyToResourceRequestQueryString(wrongDateGreaterThanPolicy,
                keyId, key)
        assertEquals(Status.Gone,
                ResourceRequestUtil
                        .resourceRequestFromQueryString(wrongDateGreaterThan, clientIp, matchingResource, properties, true)
                        .status)

        // Test after DateGreaterThan results in ok
        val rightDateGreaterThanPolicy = Policy.mkPolicyValidFrom(matchingResource, before, after)
        val rightDateGreaterThan = ResourceRequestUtil.policyToResourceRequestQueryString(rightDateGreaterThanPolicy,
                keyId, key)
        assertEquals(Status.Ok,
                ResourceRequestUtil
                        .resourceRequestFromQueryString(rightDateGreaterThan, clientIp, matchingResource, properties, true)
                        .status)

        // Test before DateLessThan results in gone
        val wrongDateLessThanPolicy = Policy.mkSimplePolicy(matchingResource, after)
        val wrongDateLessThan = ResourceRequestUtil.policyToResourceRequestQueryString(wrongDateLessThanPolicy, keyId,
                key)
        assertEquals(Status.Gone,
                ResourceRequestUtil
                        .resourceRequestFromQueryString(wrongDateLessThan, clientIp, matchingResource, properties, true)
                        .status)

        // Test matching results in ok.
        val matching = (ResourceRequest.ENCRYPTION_ID_KEY + "=" + keyId + "&" + ResourceRequest.POLICY_KEY + "="
                + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "=" + signature)
        assertEquals(Status.Ok, ResourceRequestUtil
                .resourceRequestFromQueryString(matching, clientIp, matchingResource, properties, true).status)
    }

    @Test
    @Throws(URISyntaxException::class)
    fun testIsSigned() {
        val noQueryString = "http://notsigned.com"
        val wrongQueryString = "http://notsigned.com?irrelevant=value"
        val signed = "http://notsigned.com?signature=theSignature&keyId=theKey&policy=thePolicy"
        assertFalse(ResourceRequestUtil.isSigned(URI(noQueryString)))
        assertFalse(ResourceRequestUtil.isSigned(URI(wrongQueryString)))
        assertTrue(ResourceRequestUtil.isSigned(URI(signed)))
    }

    @Test
    @Throws(Exception::class)
    fun testNonStrictResourceChecking() {
        var before = DateTime(DateTimeZone.UTC)
        before = before.plus(2 * 60 * 60 * 1000L)
        val hostname = "signed.host.com"
        val path = "/path/to/resource"
        val rtmpResource = "rtmp://$hostname$path"
        val httpResource = "http://$hostname$path"
        val portResource = "rtmp://$hostname:8080$path"
        val differentHostnameResource = "rtmp://different.host.com$path"

        val differentScheme = Policy.mkSimplePolicy(rtmpResource, before)
        val signature = PolicyUtils.getPolicySignature(differentScheme, key)
        val differentSchemeQueryString = (ResourceRequest.ENCRYPTION_ID_KEY + "=default&" + ResourceRequest.POLICY_KEY + "="
                + PolicyUtils.toBase64EncodedPolicy(differentScheme) + "&" + ResourceRequest.SIGNATURE_KEY + "="
                + signature)

        assertEquals(Status.Ok, ResourceRequestUtil
                .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, httpResource, properties, false).status)
        assertEquals(Status.Forbidden, ResourceRequestUtil
                .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, httpResource, properties, true).status)

        assertEquals(Status.Ok, ResourceRequestUtil
                .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, portResource, properties, false).status)
        assertEquals(Status.Forbidden, ResourceRequestUtil
                .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, portResource, properties, true).status)

        assertEquals(Status.Ok, ResourceRequestUtil
                .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, differentHostnameResource, properties, false).status)
        assertEquals(Status.Forbidden, ResourceRequestUtil
                .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, differentHostnameResource, properties, true).status)
    }

    companion object {
        private val keyId = "default"
        private val key = "0123456789abcdef"
        private val clientIp = "10.0.0.1"
    }
}
