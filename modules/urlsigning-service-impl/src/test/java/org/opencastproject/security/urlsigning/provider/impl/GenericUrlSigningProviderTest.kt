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
package org.opencastproject.security.urlsigning.provider.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.urlsigning.common.Policy

import org.easymock.EasyMock
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.osgi.service.cm.ConfigurationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Dictionary
import java.util.Hashtable

class GenericUrlSigningProviderTest {

    @Before
    @Throws(Exception::class)
    fun setUp() {
        properties = Hashtable()
        signerA!!.updated(properties)
        signerB!!.updated(properties)
    }

    @Test
    @Throws(ConfigurationException::class)
    fun testPropertiesUpdated() {
        // Handles empty properties
        assertFalse(signer!!.accepts(RESOURCE_PATH))
        assertEquals(0, signer!!.uris.size.toLong())

        // Incomplete entries
        properties = Hashtable()
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), MATCHING_URI)
        signer!!.updated(properties)
        assertFalse(signer!!.accepts(RESOURCE_PATH))
        assertEquals(0, signer!!.uris.size.toLong())

        // Non-Matching key
        properties = Hashtable()
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), NON_MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), NON_MATCHING_KEY)
        signer!!.updated(properties)
        assertFalse(signer!!.accepts(RESOURCE_PATH))
        assertEquals(1, signer!!.uris.size.toLong())

        // Matching Key
        properties = Hashtable()
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)
        signer!!.updated(properties)
        assertTrue(signer!!.accepts(RESOURCE_PATH))
        assertEquals(1, signer!!.uris.size.toLong())

        // Matching Key and Unrelated Key
        properties = Hashtable()
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)

        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID_2, GenericUrlSigningProvider.URL).joinToString("."), NON_MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID_2, GenericUrlSigningProvider.SECRET).joinToString("."), NON_MATCHING_KEY)

        signer!!.updated(properties)
        assertTrue(signer!!.accepts(RESOURCE_PATH))
        assertEquals(2, signer!!.uris.size.toLong())

        // Organization set to "any" organization works
        properties = Hashtable()
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.ORGANIZATION).joinToString("."),
                GenericUrlSigningProvider.ANY_ORGANIZATION)

        signer!!.updated(properties)
        assertTrue(signer!!.accepts(RESOURCE_PATH))
        assertEquals(1, signer!!.uris.size.toLong())
    }

    @Test
    @Throws(UrlSigningException::class, ConfigurationException::class)
    fun testSign() {
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)

        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID_2, GenericUrlSigningProvider.URL).joinToString("."), RTMP_MATCHER)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID_2, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)

        signer!!.updated(properties)

        val before = DateTime(2020, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)
        // Handles a policy without query parameters.
        val withoutQuery = Policy.mkSimplePolicy(RESOURCE_PATH, before)
        var result = signer!!.sign(withoutQuery)
        logger.info(result)
        assertEquals(
                "http://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=5b45e678275e6bc7b06a579f7f42e9a7ea5c58f1da130701db532f121e363e98",
                result)
        // Handles a policy with additional query parameters.
        val withQuery = Policy.mkSimplePolicy("$RESOURCE_PATH?queryparam=this", before)
        result = signer!!.sign(withQuery)
        logger.info(result)
        assertEquals(
                "http://www.opencast.org/path/to/resource.mp4?queryparam=this&policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wND9xdWVyeXBhcmFtPXRoaXMifX0&keyId=theId&signature=0f9cab4393a5d0ebe2683124a92094c3ccf06ed07e87b8f60fa2bab3963bd462",
                result)
        // Handles rtmp protocol
        val withRtmp = Policy.mkSimplePolicy("rtmp://www.opencast.org/path/to/resource.mp4", before)
        result = signer!!.sign(withRtmp)
        logger.info(result)
        assertEquals(
                "rtmp://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoicnRtcDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theSecondId&signature=0464d9672fa5cb62ed82e7a6c46db5552dcd76590cf11efa5ba5222f53f5bbaa",
                result)
    }

    @Test
    @Throws(Exception::class)
    fun testSignUrlWithPort() {
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), RTMP_MATCHER)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)

        signer!!.updated(properties)

        assertTrue(signer!!.sign(Policy.mkSimplePolicy("rtmp://myhost.com:1935/vod/mp4:movie.mp4", DateTime()))
                .startsWith(
                        "rtmp://myhost.com:1935/vod/mp4:movie.mp4"))
    }

    @Test
    @Throws(UrlSigningException::class, ConfigurationException::class)
    fun testMultitenantSign() {

        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), KEY)

        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, ORGANIZATION_A_KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), ORGANIZATION_A_MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, ORGANIZATION_A_KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), ORGANIZATION_A_KEY)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, ORGANIZATION_A_KEY_ID, GenericUrlSigningProvider.ORGANIZATION).joinToString("."), ORGANIZATION_A_ID)

        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, ORGANIZATION_B_KEY_ID, GenericUrlSigningProvider.URL).joinToString("."), ORGANIZATION_B_MATCHING_URI)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, ORGANIZATION_B_KEY_ID, GenericUrlSigningProvider.SECRET).joinToString("."), ORGANIZATION_B_KEY)
        properties!!.put(arrayOf(AbstractUrlSigningProvider.KEY_ENTRY_PREFIX, ORGANIZATION_B_KEY_ID, GenericUrlSigningProvider.ORGANIZATION).joinToString("."), ORGANIZATION_B_ID)

        signerA!!.updated(properties)
        signerB!!.updated(properties)

        val before = DateTime(2020, 3, 1, 0, 46, 17, 0, DateTimeZone.UTC)
        var policy: Policy
        var result: String
        var exceptionThrown: Boolean

        // Organization A can sign its URLs using its key
        policy = Policy.mkSimplePolicy(ORGANIZATION_A_RESOURCE_PATH, before)
        result = signerA!!.sign(policy)
        logger.info(result)
        assertEquals(
                "http://organization-a.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvb3JnYW5pemF0aW9uLWEub3BlbmNhc3Qub3JnXC9wYXRoXC90b1wvcmVzb3VyY2UubXA0In19&keyId=key-id-organization-a&signature=ea715d6ff561f49bb6e2cb8cc3e925029e139f14eeda62fc92573f362c694423",
                result)

        // Organization A can sign URLs not specific to any organization
        policy = Policy.mkSimplePolicy(RESOURCE_PATH, before)
        result = signerA!!.sign(policy)
        logger.info(result)
        assertEquals(
                "http://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=5b45e678275e6bc7b06a579f7f42e9a7ea5c58f1da130701db532f121e363e98",
                result)

        // Organization A cannot sign URLs of organization B
        exceptionThrown = false
        policy = Policy.mkSimplePolicy(ORGANIZATION_B_RESOURCE_PATH, before)
        try {
            result = signerA!!.sign(policy)
        } catch (e: UrlSigningException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)

        // Organization B can sign its URLs using its key
        policy = Policy.mkSimplePolicy(ORGANIZATION_B_RESOURCE_PATH, before)
        result = signerB!!.sign(policy)
        logger.info(result)
        assertEquals(
                "http://organization-b.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvb3JnYW5pemF0aW9uLWIub3BlbmNhc3Qub3JnXC9wYXRoXC90b1wvcmVzb3VyY2UubXA0In19&keyId=key-id-organizatino-b&signature=a6d803d4766f808bf2eaabdcf2e9114f85513d3d5596b4a01cb8d2488de816e8",
                result)

        // Organization B can sign URLs not specific to any organization
        policy = Policy.mkSimplePolicy(RESOURCE_PATH, before)
        result = signerB!!.sign(policy)
        logger.info(result)
        assertEquals(
                "http://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=5b45e678275e6bc7b06a579f7f42e9a7ea5c58f1da130701db532f121e363e98",
                result)

        // Organization B cannot sign URLs of organization B
        exceptionThrown = false
        policy = Policy.mkSimplePolicy(ORGANIZATION_A_RESOURCE_PATH, before)
        try {
            result = signerB!!.sign(policy)
        } catch (e: UrlSigningException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GenericUrlSigningProviderTest::class.java)

        private val RTMP_MATCHER = "rtmp"
        private val RESOURCE_URL = "/path/to/resource.mp4"
        private val NON_MATCHING_URI = "http://hello.com"
        private val NON_MATCHING_KEY = "abcdef0123456789"

        private val KEY_ID = "theId"
        private val KEY_ID_2 = "theSecondId"
        private val MATCHING_URI = "http://www.opencast.org"
        private val KEY = "0123456789abcdef"
        private val RESOURCE_PATH = MATCHING_URI + RESOURCE_URL

        private val ORGANIZATION_A_ID = "organization-a"
        private val ORGANIZATION_A_MATCHING_URI = "http://organization-a.opencast.org"
        private val ORGANIZATION_A_KEY_ID = "key-id-organization-a"
        private val ORGANIZATION_A_KEY = "key-organization-a"
        private val ORGANIZATION_A_RESOURCE_PATH = ORGANIZATION_A_MATCHING_URI + RESOURCE_URL

        private val ORGANIZATION_B_ID = "organization-b"
        private val ORGANIZATION_B_MATCHING_URI = "http://organization-b.opencast.org"
        private val ORGANIZATION_B_KEY_ID = "key-id-organizatino-b"
        private val ORGANIZATION_B_KEY = "key-organization-b"
        private val ORGANIZATION_B_RESOURCE_PATH = ORGANIZATION_B_MATCHING_URI + RESOURCE_URL

        private var signer: GenericUrlSigningProvider? = null
        private var signerA: GenericUrlSigningProvider? = null
        private var signerB: GenericUrlSigningProvider? = null

        private var properties: Dictionary<String, String>? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setUpClass() {

            /* Create mockup for organization a */
            val organizationA = EasyMock.createNiceMock<JaxbOrganization>(JaxbOrganization::class.java)
            EasyMock.expect<String>(organizationA.id).andReturn(ORGANIZATION_A_ID).anyTimes()
            EasyMock.replay(organizationA)
            val securityServiceA = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
            EasyMock.expect<Organization>(securityServiceA.organization).andReturn(organizationA).anyTimes()
            EasyMock.replay(securityServiceA)
            signerA = GenericUrlSigningProvider()
            signerA!!.setSecurityService(securityServiceA)

            /* Create mockup for organization b */
            val organizationB = EasyMock.createNiceMock<JaxbOrganization>(JaxbOrganization::class.java)
            EasyMock.expect<String>(organizationB.id).andReturn(ORGANIZATION_B_ID).anyTimes()
            EasyMock.replay(organizationB)
            val securityServiceB = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
            EasyMock.expect<Organization>(securityServiceB.organization).andReturn(organizationB).anyTimes()
            EasyMock.replay(securityServiceB)
            signerB = GenericUrlSigningProvider()
            signerB!!.setSecurityService(securityServiceB)

            properties = Hashtable()

            /* To indicate that specific tests are not supposed to test multi-tenant capabilities, we just set an alias */
            signer = signerA
        }
    }
}
