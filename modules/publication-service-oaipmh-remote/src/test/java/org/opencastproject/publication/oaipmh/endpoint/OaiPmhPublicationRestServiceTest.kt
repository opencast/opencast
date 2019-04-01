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
package org.opencastproject.publication.oaipmh.endpoint

import io.restassured.RestAssured.given
import org.junit.Assert.assertEquals
import org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort
import org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses
import org.opencastproject.util.UrlSupport.uri

import org.opencastproject.job.api.Job
import org.opencastproject.kernel.http.impl.HttpClientFactory
import org.opencastproject.kernel.security.TrustedHttpClientImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.publication.oaipmh.remote.OaiPmhPublicationServiceRemoteImpl
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.TrustedHttpClientException
import org.opencastproject.serviceregistry.api.ServiceRegistration
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.test.rest.RestServiceTestEnv

import com.entwinemedia.fn.data.ListBuilders

import org.apache.http.client.methods.HttpRequestBase
import org.easymock.EasyMock
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.net.URI
import java.net.URL
import java.util.HashSet

/**
 * These tests are tightly coupled to [TestOaiPmhPublicationRestService].
 */
class OaiPmhPublicationRestServiceTest {


    @Test
    @Throws(Exception::class)
    fun testPublishBrokenMediaPackage() {
        // this should yield an error
        given().formParam("mediapackage", "bla").expect().statusCode(500).`when`().post(host("/"))
    }

    @Test
    @Throws(Exception::class)
    fun testPublish() {
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.addCreator(CREATOR)
        val mpXml = MediaPackageParser.getAsXml(mp)
        given().formParam("mediapackage", mpXml).expect().statusCode(200).`when`().post(host("/"))
    }

    @Test
    @Throws(Exception::class)
    fun testPublishUsingRemoteService() {
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.addCreator(CREATOR)
        //
        val registry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        val registration = EasyMock.createNiceMock<ServiceRegistration>(ServiceRegistration::class.java)
        EasyMock.expect(registration.host).andReturn(url.protocol + "://" + url.host + ":" + url.port).anyTimes()
        EasyMock.expect(registration.path).andReturn(url.path).anyTimes()
        EasyMock.expect(registry.getServiceRegistrationsByLoad(EasyMock.anyString())).andReturn(ListBuilders.SIA.mk(registration)).anyTimes()
        EasyMock.replay(registry, registration)
        val remote = OaiPmhPublicationServiceRemoteImpl()
        remote.setTrustedHttpClient(TestHttpClient())
        remote.setRemoteServiceManager(registry)
        //
        val job = remote.publish(mp, "mmp", HashSet(), HashSet(), false)
        assertEquals(job.uri, JOB_URI)
    }

    //
    // setup
    //

    private class TestHttpClient internal constructor() : TrustedHttpClientImpl("user", "pass") {
        init {
            setHttpClientFactory(HttpClientFactory())
            setSecurityService(EasyMock.createNiceMock(SecurityService::class.java))
        }

        /**
         * Override method with a no-op. In a test environment where the Opencast servlet filter chain is not in place
         * this will result in an inadvertent call to the REST endpoint which will most likely cause exceptions.
         */
        @Throws(TrustedHttpClientException::class)
        override fun getRealmAndNonce(request: HttpRequestBase): Array<String>? {
            return null
        }
    }

    companion object {
        val CREATOR = "Tshiyoyo, Dieudonné"
        val JOB_URI = uri("http://localhost/job")

        private val url = localhostRandomPort()
        private val rt = testEnvForClasses(url, TestOaiPmhPublicationRestService::class.java)

        // Great. Checkstyle: "This method should no be static". JUnit: "Method setUp() should be static." ;)
        // CHECKSTYLE:OFF
        @BeforeClass
        @Throws(Exception::class)
        fun setUp() {
            rt.setUpServer()
        }

        @AfterClass
        fun tearDownAfterClass() {
            rt.tearDownServer()
        }

        // CHECKSTYLE:ON

        // shortcut to testEnv.host
        fun host(path: String): String {
            return rt.host(path)
        }
    }
}
