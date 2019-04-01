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

package org.opencastproject.staticfiles.endpoint

import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.eq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.staticfiles.api.StaticFileService
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.NotFoundException

import org.apache.commons.fileupload.FileUploadException
import org.apache.commons.fileupload.MockHttpServletRequest
import org.easymock.EasyMock
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.Dictionary
import java.util.Properties

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

class StaticFileRestServiceTest {

    @Before
    @Throws(IOException::class)
    fun setUp() {
        // FileUtils.forceMkdir(rootDir);
    }

    @After
    fun tearDown() {
        // FileUtils.deleteQuietly(rootDir);
    }

    @Throws(Exception::class)
    private fun newMockRequest(): MockHttpServletRequest {
        val requestBody = StringBuilder()
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"other.mov\"\r\n")
        requestBody.append("Content-Type: text/whatever\r\n")
        requestBody.append("\r\n")
        requestBody.append(MOCK_FILE_CONTENT)
        requestBody.append("\r\n")
        requestBody.append("-----1234")
        return MockHttpServletRequest(requestBody.toString().toByteArray(charset("UTF-8")), "multipart/form-data; boundary=---1234")
    }

    @Throws(Exception::class)
    private fun newUnsizedMockRequest(): MockHttpServletRequest {
        val requestBody = StringBuilder()
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"other.mov\"\r\n")
        requestBody.append("Content-Type: text/whatever\r\n")
        requestBody.append("\r\n")
        requestBody.append(MOCK_FILE_CONTENT)
        requestBody.append("\r\n")
        requestBody.append("-----1234")
        return MockHttpServletRequest(ByteArrayInputStream(requestBody.toString().toByteArray(charset("UTF-8"))), -1,
                "multipart/form-data; boundary=---1234")
    }

    @Test
    @Throws(ConfigurationException::class)
    fun testUseWebserver() {
        val staticFileRestService = StaticFileRestService()
        staticFileRestService.activate(getComponentContext(null, 100000000L))
        assertFalse(staticFileRestService.useWebserver)

        staticFileRestService.activate(getComponentContext("", 100000000L))
        assertFalse(staticFileRestService.useWebserver)

        staticFileRestService.activate(getComponentContext("false", 100000000L))
        assertFalse(staticFileRestService.useWebserver)

        staticFileRestService.activate(getComponentContext("other", 100000000L))
        assertFalse(staticFileRestService.useWebserver)

        staticFileRestService.activate(getComponentContext("true", 100000000L))
        assertTrue(staticFileRestService.useWebserver)
    }

    @Test
    @Throws(FileUploadException::class, Exception::class)
    fun testStoreStaticFileInputHttpServletRequest() {
        // Setup static file service.
        val fileService = EasyMock.createMock<StaticFileService>(StaticFileService::class.java)
        val fileUuid = "12345"
        val fileName = "other.mov"
        EasyMock.expect(fileService.storeFile(eq(fileName), anyObject(InputStream::class.java))).andReturn(fileUuid)
        EasyMock.expect(fileService.getFileName(fileUuid)).andReturn(fileName)
        EasyMock.replay(fileService)

        // Run the test
        val staticFileRestService = StaticFileRestService()
        staticFileRestService.activate(getComponentContext(null, 100000000L))
        staticFileRestService.setSecurityService(securityService)
        staticFileRestService.setStaticFileService(fileService)

        // Test a good store request
        var result = staticFileRestService.postStaticFile(newMockRequest())
        assertEquals(Status.CREATED.statusCode.toLong(), result.status.toLong())
        assertTrue(result.metadata.size > 0)
        assertTrue(result.metadata["location"].size > 0)
        val location = result.metadata["location"].get(0).toString()
        val uuid = location.substring(location.lastIndexOf("/") + 1)

        // assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(MOCK_FILE_CONTENT.getBytes("UTF-8")),
        // staticFileServiceImpl.getFile(uuid)));

        // Test a request with too large of an input stream
        val tooLargeRequest = EasyMock.createMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(tooLargeRequest.contentLength).andReturn(1000000000).anyTimes()
        EasyMock.replay(tooLargeRequest)
        result = staticFileRestService.postStaticFile(tooLargeRequest)
        assertEquals(Status.BAD_REQUEST.statusCode.toLong(), result.status.toLong())

        staticFileRestService.activate(getComponentContext("true", 100000000L))
        val staticFileURL = staticFileRestService.getStaticFileURL(uuid)
        assertEquals("http://localhost/staticfiles/mh_default_org/$uuid/other.mov", staticFileURL.toString())
    }

    @Test
    @Throws(FileUploadException::class, Exception::class)
    fun testUploadMaxSizeReached() {
        // Setup static file service.
        val fileService = EasyMock.createMock<StaticFileService>(StaticFileService::class.java)
        EasyMock.expect(fileService.storeFile(eq("other.mov"), anyObject(InputStream::class.java))).andReturn("12345")
        EasyMock.replay(fileService)

        // Run the test
        val staticFileRestService = StaticFileRestService()
        staticFileRestService.activate(getComponentContext(null, 10L))
        staticFileRestService.setSecurityService(securityService)
        staticFileRestService.setStaticFileService(fileService)

        // Test a sized mock request
        val result = staticFileRestService.postStaticFile(newMockRequest())
        assertEquals(Status.BAD_REQUEST.statusCode.toLong(), result.status.toLong())
    }

    @Test
    @Throws(FileUploadException::class, Exception::class)
    fun testDeleteStaticFile() {
        // Setup static file service.
        val fileService = EasyMock.createMock<StaticFileService>(StaticFileService::class.java)
        val fileUuid = "12345"
        EasyMock.expect(fileService.storeFile(anyObject(String::class.java), anyObject(InputStream::class.java))).andReturn(fileUuid)
        fileService.deleteFile(fileUuid)
        EasyMock.expectLastCall<Any>()
        EasyMock.expect(fileService.getFile(fileUuid)).andThrow(NotFoundException())
        EasyMock.replay(fileService)

        // Run the test
        val staticFileRestService = StaticFileRestService()
        staticFileRestService.activate(getComponentContext(null, 100000000L))
        staticFileRestService.setSecurityService(securityService)
        staticFileRestService.setStaticFileService(fileService)

        // Test a good store request
        val result = staticFileRestService.postStaticFile(newMockRequest())
        assertEquals(Status.CREATED.statusCode.toLong(), result.status.toLong())

        val location = result.metadata["location"].get(0).toString()
        val uuid = location.substring(location.lastIndexOf("/") + 1)

        val response = staticFileRestService.deleteStaticFile(uuid)
        assertEquals(Status.NO_CONTENT.statusCode.toLong(), response.status.toLong())

        try {
            staticFileRestService.getStaticFile(uuid)
            fail("NotFoundException must be passed on")
        } catch (e: NotFoundException) {
            // expected
        }

    }

    companion object {

        private val MOCK_FILE_CONTENT = "This is the content of the file\n"

        private val logger = LoggerFactory.getLogger(StaticFileRestServiceTest::class.java)

        private val SERVER_URL = "http://localhost:8080"

        private val WEBSERVER_URL = "http://localhost/staticfiles"

        private val videoFilename = "av.mov"
        private val imageFilename = "image.jpg"

        /** The File object that is an example image  */
        private val imageFile: File? = null
        /** Location where the files are copied to  */
        private val rootDir: File? = null
        /** The File object that is an example video  */
        private val videoFile: File? = null
        /** The org to use for the tests  */
        private val org = DefaultOrganization()
        /** The test root directory  */
        private val baseDir: URI? = null

        @BeforeClass
        @Throws(URISyntaxException::class)
        fun beforeClass() {
            // baseDir = StaticFileServiceImplTest.class.getResource("/").toURI();
            // rootDir = new File(new File(baseDir), "ingest-temp");
            // imageFile = new File(StaticFileServiceImplTest.class.getResource("/" + imageFilename).getPath());
            // videoFile = new File(StaticFileServiceImplTest.class.getResource("/" + videoFilename).getPath());
        }

        private fun getComponentContext(useWebserver: String?, maxSize: Long): ComponentContext {
            // Create BundleContext
            val bundleContext = EasyMock.createMock<BundleContext>(BundleContext::class.java)
            EasyMock.expect(bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)).andReturn(SERVER_URL)
            EasyMock.expect(bundleContext.getProperty(StaticFileRestService.STATICFILES_UPLOAD_MAX_SIZE_KEY)).andReturn(
                    java.lang.Long.toString(maxSize))
            EasyMock.replay(bundleContext)
            // Create ComponentContext
            val properties = Properties()
            if (useWebserver != null) {
                properties[StaticFileRestService.STATICFILES_WEBSERVER_ENABLED_KEY] = useWebserver
            }
            properties[StaticFileRestService.STATICFILES_WEBSERVER_URL_KEY] = WEBSERVER_URL
            val cc = EasyMock.createMock<ComponentContext>(ComponentContext::class.java)
            EasyMock.expect(cc.properties).andReturn(properties).anyTimes()
            EasyMock.expect(cc.bundleContext).andReturn(bundleContext).anyTimes()
            EasyMock.replay(cc)
            return cc
        }

        private val securityService: SecurityService
            get() {
                val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
                EasyMock.expect(securityService.organization).andReturn(org).anyTimes()
                EasyMock.replay(securityService)
                return securityService
            }
    }
}
