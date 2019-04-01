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

package org.opencastproject.ingest.endpoint

import org.junit.Assert.assertEquals

import org.opencastproject.ingest.api.IngestService
import org.opencastproject.ingest.impl.IngestServiceImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl
import org.opencastproject.workflow.api.WorkflowInstanceImpl

import org.apache.commons.fileupload.MockHttpServletRequest
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.HashMap

import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

class IngestRestServiceTest {
    protected var restService: IngestRestService
    private var testDir: File? = null
    private var limitVerifier: LimitVerifier? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        testDir = File("./target", "ingest-rest-service-test")
        if (testDir!!.exists()) {
            FileUtils.deleteQuietly(testDir)
            logger.info("Removing  " + testDir!!.absolutePath)
        } else {
            logger.info("Didn't Delete " + testDir!!.absolutePath)
        }
        testDir!!.mkdir()

        restService = IngestRestService()

        // Create a mock ingest service
        val ingestService = EasyMock.createNiceMock<IngestService>(IngestService::class.java)
        EasyMock.expect(ingestService.createMediaPackage())
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.createMediaPackage("1a6f70ab-4262-4523-9f8e-babce22a1ea8"))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                        .createNew(UUIDIdBuilderImpl().fromString("1a6f70ab-4262-4523-9f8e-babce22a1ea8")))
        EasyMock.expect(ingestService.addAttachment(EasyMock.anyObject<Any>() as URI,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addCatalog(EasyMock.anyObject<Any>() as URI,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addTrack(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as MediaPackageElementFlavor,
                EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addTrack(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as MediaPackageElementFlavor,
                EasyMock.anyObject<Any>() as Array<String>, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addAttachment(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addAttachment(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as Array<String>,
                EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addCatalog(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addCatalog(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as Array<String>,
                EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addTrack(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addTrack(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as Array<String>,
                EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect(ingestService.addPartialTrack(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyLong(), EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.replay(ingestService)

        // Set the service, and activate the rest endpoint
        restService.setIngestService(ingestService)
        restService.activate(null)
    }

    @Test
    fun testNoIngestLimit() {
        setupAndTestLimit(null, -1, false)
    }

    @Test
    fun testIngestLimitOfNegativeOne() {
        setupAndTestLimit("-1", -1, false)
    }

    @Test
    fun testIngestLimitOfZero() {
        setupAndTestLimit("0", -1, false)
    }

    @Test
    fun testIngestLimitOfOne() {
        setupAndTestLimit("1", 1, true)
    }

    @Test
    fun testIngestLimitOfTen() {
        setupAndTestLimit("10", 10, true)
    }

    @Test
    fun testIngestLimitOfThousand() {
        setupAndTestLimit("1000", 1000, true)
    }

    @Test
    fun testInvalidLimitAddZippedMediaPackage() {
        setupAndTestLimit("This is not a number", -1, false)
    }

    fun setupAndTestLimit(limit: String?, expectedLimit: Int, expectedEnabled: Boolean) {
        restService = IngestRestService()

        // Create a mock ingest service
        val ingestService = EasyMock.createNiceMock<IngestService>(IngestService::class.java)
        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()
        EasyMock.replay(cc)
        EasyMock.expect(bc.getProperty(IngestRestService.DEFAULT_WORKFLOW_DEFINITION)).andReturn("full").anyTimes()
        if (StringUtils.trimToNull(limit) != null) {
            EasyMock.expect(bc.getProperty(IngestRestService.MAX_INGESTS_KEY)).andReturn(limit).anyTimes()
        }
        EasyMock.replay(bc)
        restService.setIngestService(ingestService)
        restService.activate(cc)
        Assert.assertEquals(expectedLimit.toLong(), restService.ingestLimit.toLong())
        Assert.assertEquals(expectedEnabled, restService.isIngestLimitEnabled)
    }

    @Test
    fun testLimitOfOneToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("1", 1, 1, 0)
    }

    @Test
    fun testLimitOfOneWithTwoIngestsToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("1", 2, 1, 1)
    }

    @Test
    fun testLimitOfOneWithTenIngestsToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("1", 10, 1, 9)
    }

    @Test
    fun testLimitOfTwoWithTenIngestsToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("2", 10, 2, 8)
    }

    @Test
    fun testLimitOfFiveWithTenIngestsToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("5", 10, 5, 5)
    }

    @Test
    fun testLimitOfTenWithOneHundredIngestsToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("10", 100, 10, 90)
    }

    @Test
    fun testLimitOfZeroWithTenIngestsToAddZippedMediaPackage() {
        setupAndTestIngestingLimit("0", 10, 10, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testLegacyMediaPackageIdPropertyUsingZippedIngest() {
        // Create a mock ingest service
        val workflowConfigCapture = EasyMock.newCapture<Map<String, String>>()

        val ingestService = EasyMock.createNiceMock<IngestService>(IngestService::class.java)
        EasyMock.expect<WorkflowInstance>(ingestService.addZippedMediaPackage(EasyMock.anyObject(InputStream::class.java), EasyMock.anyString(),
                EasyMock.capture(workflowConfigCapture))).andReturn(WorkflowInstanceImpl())
        EasyMock.replay(ingestService)
        restService.setIngestService(ingestService)

        val mpId = "6f7a7850-3232-4719-9064-24c9bad2832f"
        val response = restService.addZippedMediaPackage(setupAddZippedMediaPackageHttpServletRequest(), "test", mpId)
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
        val config = workflowConfigCapture.value
        Assert.assertFalse(config.isEmpty())
        Assert.assertEquals(mpId, config[IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY])
    }

    @Test
    @Throws(Exception::class)
    fun testLegacyMediaPackageIdPropertyUsingIngest() {
        // Create a mock ingest service
        val workflowConfigCapture = EasyMock.newCapture<Map<String, String>>()

        val ingestService = EasyMock.createNiceMock<IngestService>(IngestService::class.java)
        EasyMock.expect(ingestService.createMediaPackage())
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew())
        EasyMock.expect<WorkflowInstance>(ingestService.ingest(EasyMock.anyObject(MediaPackage::class.java), EasyMock.anyString(),
                EasyMock.capture(workflowConfigCapture))).andReturn(WorkflowInstanceImpl())
        EasyMock.replay(ingestService)
        restService.setIngestService(ingestService)

        val mpId = "6f7a7850-3232-4719-9064-24c9bad2832f"

        val createMediaPackage = restService.createMediaPackage()
        val mp = createMediaPackage.entity as MediaPackage
        val metadataMap = HashMap<String, Array<String>>()
        metadataMap["mediaPackage"] = arrayOf(MediaPackageParser.getAsXml(mp))
        metadataMap[IngestRestService.WORKFLOW_INSTANCE_ID_PARAM] = arrayOf(mpId)
        val request = EasyMock.createNiceMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.parameterMap).andReturn(metadataMap).anyTimes()
        EasyMock.replay(request)
        val response = restService.ingest(request)
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
        val config = workflowConfigCapture.value
        Assert.assertFalse(config.isEmpty())
        Assert.assertEquals(mpId, config[IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY])
    }

    fun setupAndTestIngestingLimit(limit: String, numberOfIngests: Int, expectedOK: Int, expectedBusy: Int) {
        restService = IngestRestService()
        restService.setIngestService(setupAddZippedMediaPackageIngestService())
        restService.activate(setupAddZippedMediaPackageComponentContext(limit))

        limitVerifier = LimitVerifier(numberOfIngests)
        limitVerifier!!.start()

        Assert.assertEquals("There should be no errors when making requests.", 0, limitVerifier!!.error.toLong())
        Assert.assertEquals("There should have been the same number of successful ingests finished as expected.",
                expectedOK.toLong(), limitVerifier!!.ok.toLong())
        Assert.assertEquals("The extra ingests beyond the limit should have received a server unavailable error. ",
                expectedBusy.toLong(), limitVerifier!!.unavailable.toLong())
    }

    private inner class LimitVerifier internal constructor(private val numberOfIngests: Int) {
        private var current: Int = 0

        @get:Synchronized
        private var unavailable = 0
        @get:Synchronized
        private var ok = 0
        @get:Synchronized
        private var error = 0

        fun start() {
            getResponse()
        }

        private fun getResponse() {
            current++
            if (current > numberOfIngests) {
                return
            } else {
                val response = restService.addZippedMediaPackage(setupAddZippedMediaPackageHttpServletRequest())
                if (response.status == Status.SERVICE_UNAVAILABLE.statusCode) {
                    unavailable++
                    // Because there is no mock that gets called if the service is unavailable we will have to do the next request
                    // here.
                    getResponse()
                } else if (response.status == Status.OK.statusCode) {
                    ok++
                } else {
                    error++
                }
            }
        }

        fun callback() {
            getResponse()
        }
    }

    private fun setupAddZippedMediaPackageServletInputStream(): ServletInputStream {
        return object : ServletInputStream() {
            @Throws(IOException::class)
            override fun read(): Int {
                return 0
            }

            override fun isFinished(): Boolean {
                return false
            }

            override fun isReady(): Boolean {
                return false
            }

            override fun setReadListener(readListener: ReadListener) {}
        }
    }

    private fun setupAddZippedMediaPackageHttpServletRequest(): HttpServletRequest {
        val request = EasyMock.createNiceMock<HttpServletRequest>(HttpServletRequest::class.java)
        EasyMock.expect(request.method).andReturn("post")
        try {
            EasyMock.expect(request.inputStream).andReturn(setupAddZippedMediaPackageServletInputStream())
        } catch (e: IOException) {
            Assert.fail("Failed due to exception " + e.message)
        }

        EasyMock.replay(request)
        return request
    }

    private fun setupAddZippedMediaPackageBundleContext(limit: String): BundleContext {
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bc.getProperty(IngestRestService.DEFAULT_WORKFLOW_DEFINITION)).andReturn("full").anyTimes()
        if (StringUtils.trimToNull(limit) != null) {
            EasyMock.expect(bc.getProperty(IngestRestService.MAX_INGESTS_KEY)).andReturn(limit).anyTimes()
        }
        EasyMock.replay(bc)
        return bc
    }

    private fun setupAddZippedMediaPackageComponentContext(limit: String): ComponentContext {
        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(setupAddZippedMediaPackageBundleContext(limit)).anyTimes()
        EasyMock.replay(cc)
        return cc
    }

    private fun setupAddZippedMediaPackageIngestService(): IngestService {
        // Create a mock ingest service
        val ingestService = EasyMock.createNiceMock<IngestService>(IngestService::class.java)
        try {
            EasyMock.expect<WorkflowInstance>(ingestService.addZippedMediaPackage(EasyMock.anyObject(InputStream::class.java), EasyMock.anyString(),
                    EasyMock.anyObject<Map<*, *>>(Map<*, *>::class.java), EasyMock.anyLong()))
                    .andAnswer {
                        limitVerifier!!.callback()
                        WorkflowInstanceImpl()
                    }.anyTimes()
            EasyMock.expect<WorkflowInstance>(ingestService.addZippedMediaPackage(EasyMock.anyObject(InputStream::class.java), EasyMock.anyString(),
                    EasyMock.anyObject<Map<*, *>>(Map<*, *>::class.java)))
                    .andAnswer {
                        limitVerifier!!.callback()
                        WorkflowInstanceImpl()
                    }.anyTimes()
        } catch (e: Exception) {
            Assert.fail("Threw exception " + e.message)
        }

        EasyMock.replay(ingestService)
        return ingestService
    }

    @Test
    @Throws(Exception::class)
    fun testCreateMediaPackage() {
        var response = restService.createMediaPackage()
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
        var mp = response.entity as MediaPackage
        Assert.assertNotNull(mp)

        response = restService.createMediaPackage("1a6f70ab-4262-4523-9f8e-babce22a1ea8")
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
        mp = response.entity as MediaPackage
        Assert.assertNotNull(mp)

    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageTrack() {
        val response = restService.addMediaPackageTrack("http://foo/av.mov", "presenter/source", "testtag",
                MediaPackageParser.getAsXml(restService.createMediaPackage().entity as MediaPackage))
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageCatalog() {
        val response = restService.addMediaPackageCatalog("http://foo/dc.xml", "dublincore/episode",
                MediaPackageParser.getAsXml(restService.createMediaPackage().entity as MediaPackage))
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageAttachment() {
        val response = restService.addMediaPackageAttachment("http://foo/cover.png", "image/cover",
                MediaPackageParser.getAsXml(restService.createMediaPackage().entity as MediaPackage))
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageAttachmentFromRequest() {
        // Upload the mediapackage with its new element
        val postResponse = restService.addMediaPackageAttachment(newMockRequest())
        Assert.assertEquals(Status.OK.statusCode.toLong(), postResponse.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageCatalogFromRequest() {
        // Upload the mediapackage with its new element
        val postResponse = restService.addMediaPackageCatalog(newMockRequest())
        Assert.assertEquals(Status.OK.statusCode.toLong(), postResponse.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageTrackFromRequest() {
        // Upload the mediapackage with its new element
        val postResponse = restService.addMediaPackageTrack(newMockRequest())
        Assert.assertEquals(Status.OK.statusCode.toLong(), postResponse.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackagePartialTrack() {
        val mediaPackage = MediaPackageParser.getAsXml(restService.createMediaPackage().entity as MediaPackage)

        var response = restService.addMediaPackagePartialTrack("http://foo/av.mov", "presenter/source+partial", 1000L,
                mediaPackage)
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())

        response = restService.addMediaPackagePartialTrack(newPartialMockRequest())
        Assert.assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddMediaPackageTrackWithStartTime() {
        val ingestService = EasyMock.createNiceMock<IngestService>(IngestService::class.java)
        EasyMock.expect(ingestService.addPartialTrack(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyLong(), EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()).once()
        EasyMock.expect(ingestService.addTrack(EasyMock.anyObject<Any>() as InputStream, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as MediaPackageElementFlavor, EasyMock.anyObject<Any>() as Array<String>,
                EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()).once()
        EasyMock.replay(ingestService)
        restService.setIngestService(ingestService)

        var request = newPartialMockRequest()
        request.pathInfo = "/addTrack"
        var response = restService.addMediaPackageTrack(request)
        assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())

        request = newPartialMockRequest()
        request.pathInfo = "/addPartialTrack"
        response = restService.addMediaPackageTrack(request)
        assertEquals(Status.OK.statusCode.toLong(), response.status.toLong())
        EasyMock.verify(ingestService)
    }

    @Throws(Exception::class)
    private fun newMockRequest(): HttpServletRequest {
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        val requestBody = StringBuilder()
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"flavor\"\r\n")
        requestBody.append("\r\ntest/flavor\r\n")
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"mediaPackage\"\r\n")
        requestBody.append("\r\n")
        requestBody.append(MediaPackageParser.getAsXml(mp))
        requestBody.append("\r\n")
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"catalog.txt\"\r\n")
        requestBody.append("Content-Type: text/whatever\r\n")
        requestBody.append("\r\n")
        requestBody.append("This is the content of the file\n")
        requestBody.append("\r\n")
        requestBody.append("-----1234")
        return MockHttpServletRequest(requestBody.toString().toByteArray(charset("UTF-8")),
                "multipart/form-data; boundary=---1234")
    }

    @Throws(Exception::class)
    private fun newPartialMockRequest(): MockHttpServletRequest {
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        val requestBody = StringBuilder()
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"flavor\"\r\n")
        requestBody.append("\r\ntest/flavor\r\n")
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"mediaPackage\"\r\n")
        requestBody.append("\r\n")
        requestBody.append(MediaPackageParser.getAsXml(mp))
        requestBody.append("\r\n")
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"startTime\"\r\n")
        requestBody.append("\r\n2000\r\n")
        requestBody.append("-----1234\r\n")
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"catalog.txt\"\r\n")
        requestBody.append("Content-Type: text/whatever\r\n")
        requestBody.append("\r\n")
        requestBody.append("This is the content of the file\n")
        requestBody.append("\r\n")
        requestBody.append("-----1234")
        return MockHttpServletRequest(requestBody.toString().toByteArray(charset("UTF-8")),
                "multipart/form-data; boundary=---1234")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IngestRestServiceTest::class.java)
    }

}
