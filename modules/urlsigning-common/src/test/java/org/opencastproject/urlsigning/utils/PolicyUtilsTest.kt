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

import org.opencastproject.urlsigning.common.Policy

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Test

import java.io.UnsupportedEncodingException

class PolicyUtilsTest {

    @Test
    fun testToJson() {
        val before = DateTime(2015, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)
        var policy = Policy.mkSimplePolicy("http://mh-allinone/", before)
        assertEquals("{\"Statement\":{\"Condition\":{\"DateLessThan\":" + before.millis
                + "},\"Resource\":\"http:\\/\\/mh-allinone\\/\"}}", PolicyUtils.toJson(policy).toJSONString())
        // With optional parameters
        policy = Policy.mkPolicyValidFromWithIP("http://mh-allinone/", before, DateTime(2015, 2, 28, 0, 46, 19, 0,
                DateTimeZone.UTC), EXAMPLE_IP)
        assertEquals(
                "{\"Statement\":{\"Condition\":{\"DateGreaterThan\":1425084379000,\"DateLessThan\":1425170777000,\"IpAddress\":\"10.0.0.1\"},\"Resource\":\"http:\\/\\/mh-allinone\\/\"}}",
                PolicyUtils.toJson(policy).toJSONString())
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun testFromJson() {
        val policyJson = "{\"Statement\": {\"Resource\":\"http://mh-allinone/engage/url/to/resource.mp4\",\"Condition\":{\"DateLessThan\":1425170777000,\"DateGreaterThan\":1425084379000,\"IpAddress\": \"10.0.0.1\"}}}"
        val policy = PolicyUtils.fromJson(policyJson)
        assertEquals("http://mh-allinone/engage/url/to/resource.mp4", policy.baseUrl)
        assertEquals(EXAMPLE_IP, policy.clientIpAddress.get().hostAddress)

        var after = DateTime(2015, 2, 28, 0, 46, 19, 0, DateTimeZone.UTC)
        after = after.withSecondOfMinute(19)
        assertEquals(after, policy.validFrom.get())

        val before = DateTime(2015, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)
        assertEquals(before, policy.validUntil)
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun testBase64Decoding() {
        val policyValue = "{policy:'The Policy'}"
        val result = PolicyUtils.base64Decode(PolicyUtils.base64Encode(policyValue))
        assertEquals(policyValue, result)
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun testFromBase64EncodedPolicy() {
        val examplePolicy = "{\"Statement\": {\"Resource\":\"http://mh-allinone/engage/url/to/resource.mp4\",\"Condition\":{\"DateLessThan\":1425170777000,\"DateGreaterThan\":1425084379000,\"IpAddress\": \"10.0.0.1\"}}}"
        val policy = PolicyUtils.fromBase64EncodedPolicy(PolicyUtils.base64Encode(examplePolicy))
        assertEquals("http://mh-allinone/engage/url/to/resource.mp4", policy.baseUrl)
        assertEquals(EXAMPLE_IP, policy.clientIpAddress.get().hostAddress)

        var after = DateTime(2015, 2, 28, 0, 46, 19, 0, DateTimeZone.UTC)
        after = after.withSecondOfMinute(19)
        assertEquals(after, policy.validFrom.get())

        val before = DateTime(2015, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)
        assertEquals(before, policy.validUntil)
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun testToBase64EncodedPolicy() {
        val resource = "http://mh-allinone/"
        val before = DateTime(2015, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)
        val policy = Policy.mkSimplePolicy("http://mh-allinone/", before)

        val result = PolicyUtils.fromBase64EncodedPolicy(PolicyUtils.toBase64EncodedPolicy(policy))
        assertEquals(resource, result.baseUrl)
        assertEquals(before, result.validUntil)
    }

    companion object {
        private val EXAMPLE_IP = "10.0.0.1"
    }
}
