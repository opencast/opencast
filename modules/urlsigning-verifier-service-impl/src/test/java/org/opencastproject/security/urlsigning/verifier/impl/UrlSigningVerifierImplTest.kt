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
package org.opencastproject.security.urlsigning.verifier.impl

import org.junit.Assert.assertEquals

import org.opencastproject.urlsigning.common.Policy
import org.opencastproject.urlsigning.common.ResourceRequest
import org.opencastproject.urlsigning.common.ResourceRequest.Status
import org.opencastproject.urlsigning.utils.ResourceRequestUtil

import org.joda.time.DateTime
import org.junit.Test

import java.util.Dictionary
import java.util.Hashtable

class UrlSigningVerifierImplTest {

    @Test
    @Throws(Exception::class)
    fun testVerifiesWithSigningProviders() {
        val keyId = "theKeyId"
        val key = "TheKeyIsThis"
        val future = DateTime(4749125399000L)
        val policy = Policy.mkSimplePolicy(URL, future)
        val queryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyId, key)

        // Test with no configured keys
        var urlSigningVerifierImpl = UrlSigningVerifierImpl()
        var result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true)
        assertEquals(Status.Forbidden, result.status)

        // Test no matching key
        urlSigningVerifierImpl = UrlSigningVerifierImpl()
        var keys: Dictionary<String, String> = Hashtable()
        keys.put(UrlSigningVerifierImpl.KEY_PREFIX + "otherKey", "ThisIsTheOtherKey")
        urlSigningVerifierImpl.updated(keys)
        result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true)
        assertEquals(Status.Forbidden, result.status)

        // Test only matching keys
        urlSigningVerifierImpl = UrlSigningVerifierImpl()
        keys = Hashtable()
        keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, key)
        urlSigningVerifierImpl.updated(keys)
        result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true)
        assertEquals(Status.Ok, result.status)

        // Test matching and non-matching keys
        urlSigningVerifierImpl = UrlSigningVerifierImpl()
        keys = Hashtable()
        keys.put(UrlSigningVerifierImpl.KEY_PREFIX + "otherKey", "ThisIsTheOtherKey")
        keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, key)
        urlSigningVerifierImpl.updated(keys)
        result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true)
        assertEquals(Status.Ok, result.status)

        // Test correct key id and wrong key
        urlSigningVerifierImpl = UrlSigningVerifierImpl()
        keys = Hashtable()
        keys.put(UrlSigningVerifierImpl.KEY_PREFIX + "otherKey", "ThisIsTheOtherKey")
        keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, "The Wrong Key")
        urlSigningVerifierImpl.updated(keys)
        result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true)
        assertEquals(Status.Forbidden, result.status)
    }

    companion object {
        private val CLIENT_IP = "10.0.0.1"
        private val URL = "http://testurl.com"
    }
}
