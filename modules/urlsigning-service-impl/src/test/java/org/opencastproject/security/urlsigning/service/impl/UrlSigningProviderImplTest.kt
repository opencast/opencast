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
package org.opencastproject.security.urlsigning.service.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.fail

import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider
import org.opencastproject.urlsigning.common.Policy

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Test


class UrlSigningProviderImplTest {
    private val before = DateTime(2020, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)

    @Test
    @Throws(UrlSigningException::class)
    fun testFindsSigningProviders() {
        // Test no signing providers
        var urlSigningServiceImpl = UrlSigningServiceImpl()
        try {
            urlSigningServiceImpl.sign(URL, before, null!!, null!!)
            fail("There are no signing services, this should fail.")
        } catch (e: UrlSigningException) {
            // This test should have a UrlSigningException as there are no supporting providers.
        }

        // Test no accepting signing providers
        urlSigningServiceImpl = UrlSigningServiceImpl()
        urlSigningServiceImpl.registerSigningProvider(TestRejectingSigningProvider())
        try {
            urlSigningServiceImpl.sign(URL, before, null!!, null!!)
            fail("There are no signing services, this should fail.")
        } catch (e: UrlSigningException) {
            // This test should have a UrlSigningException as there are no supporting providers.
        }

        // Test only accepting signing providers
        urlSigningServiceImpl = UrlSigningServiceImpl()
        urlSigningServiceImpl.registerSigningProvider(TestAcceptingSigningProvider())
        var result = urlSigningServiceImpl.sign(URL, before, null!!, null!!)
        assertEquals(SIGNED_URL, result)

        // Test accepting signing provider with non-accepting
        urlSigningServiceImpl = UrlSigningServiceImpl()
        urlSigningServiceImpl.registerSigningProvider(TestRejectingSigningProvider())
        urlSigningServiceImpl.registerSigningProvider(TestAcceptingSigningProvider())
        result = urlSigningServiceImpl.sign(URL, before, null!!, null!!)
        assertEquals(SIGNED_URL, result)
    }

    private inner class TestRejectingSigningProvider : UrlSigningProvider {
        override fun accepts(baseUrl: String): Boolean {
            return false
        }

        @Throws(UrlSigningException::class)
        override fun sign(policy: Policy): String {
            return SIGNED_URL
        }
    }

    private inner class TestAcceptingSigningProvider : UrlSigningProvider {
        override fun accepts(baseUrl: String): Boolean {
            return true
        }

        @Throws(UrlSigningException::class)
        override fun sign(policy: Policy): String {
            return SIGNED_URL
        }
    }

    companion object {
        private val SIGNED_URL = "signedUrl"
        private val URL = "http://testurl.com"
    }
}
