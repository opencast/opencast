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
package org.opencastproject.timelinepreviews.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.identifier.IdBuilderFactory
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.MimeTypes
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.exception.ExceptionUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.BeforeClass
import org.junit.Test

import java.io.File
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.Hashtable

/**
 * Test class for TimelinePreviewsServiceImpl.
 */
class TimelinePreviewsServiceImplTest {

    /**
     * Test of updated method of class TimelinePreviewsServiceImpl.
     * @throws java.lang.Exception
     */
    @Test
    @Throws(Exception::class)
    fun testUpdated() {
        val properties = Hashtable<String, String>()
        properties[TimelinePreviewsServiceImpl.OPT_RESOLUTION_X] = "200"
        properties[TimelinePreviewsServiceImpl.OPT_RESOLUTION_Y] = "90"
        properties[TimelinePreviewsServiceImpl.OPT_OUTPUT_FORMAT] = ".jpg"
        properties[TimelinePreviewsServiceImpl.OPT_MIMETYPE] = "image/jpg"

        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect<List<HostRegistration>>(serviceRegistry.hostRegistrations).andReturn(ArrayList())
        EasyMock.replay(serviceRegistry)

        val instance = TimelinePreviewsServiceImpl()
        instance.serviceRegistry = serviceRegistry
        try {
            instance.updated(properties)
            // we cannot check private fields but it should not throw any exception
            assertEquals(200, instance.resolutionX.toLong())
            assertEquals(90, instance.resolutionY.toLong())
            assertEquals(".jpg", instance.outputFormat)
            assertEquals("image/jpg", instance.mimetype)
        } catch (e: Exception) {
            fail("updated method should not throw any exceptions but has thrown: " + ExceptionUtils.getStackTrace(e))
        }

    }

    /**
     * Test of createTimelinePreviewImages method of class TimelinePreviewsServiceImpl.
     * @throws java.lang.Exception
     */
    @Test
    @Throws(Exception::class)
    fun testCreateTimelinePreviewImages() {
        val expectedJob = JobImpl(1)
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.createJob(
                EasyMock.eq<String>(TimelinePreviewsServiceImpl.JOB_TYPE),
                EasyMock.eq(TimelinePreviewsServiceImpl.Operation.TimelinePreview.toString()),
                EasyMock.anyObject<Any>() as List<String>, EasyMock.anyFloat()))
                .andReturn(expectedJob)
        EasyMock.replay(serviceRegistry)

        val instance = TimelinePreviewsServiceImpl()
        instance.serviceRegistry = serviceRegistry
        val job = instance.createTimelinePreviewImages(track!!, 10)
        assertEquals(expectedJob, job)
    }

    /**
     * Test of process method of class TimelinePreviewsServiceImpl.
     * @throws java.lang.Exception
     */
    @Test
    @Throws(Exception::class)
    fun testProcess() {
        val file = File(track!!.getURI())
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject()))
                .andReturn(file)
        val filenameCapture = Capture.newInstance<String>()
        EasyMock.expect(workspace.putInCollection(
                EasyMock.anyString(), EasyMock.capture(filenameCapture), EasyMock.anyObject<InputStream>()))
                .andReturn(URI("timelinepreviews.png"))
        EasyMock.replay(workspace)

        val instance = TimelinePreviewsServiceImpl()
        instance.setWorkspace(workspace)

        val trackXml = MediaPackageElementParser.getAsXml(track)
        val job = JobImpl(1)
        job.jobType = TimelinePreviewsServiceImpl.JOB_TYPE
        job.operation = TimelinePreviewsServiceImpl.Operation.TimelinePreview.toString()
        job.arguments = Arrays.asList(trackXml, 10.toString())
        val result = instance.process(job)
        assertNotNull(result)

        val timelinepreviewsAttachment = MediaPackageElementParser.getFromXml(result)
        assertEquals(URI("timelinepreviews.png"), timelinepreviewsAttachment.getURI())
        assertTrue(filenameCapture.hasCaptured())
    }

    companion object {
        /** Video file to test the optimization  */
        protected val mediaResource = "/test-optimization.mp4"

        /** Duration of whole movie  */
        protected val mediaDuration = 30000L

        /** The media url  */
        protected var track: TrackImpl? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setUpClass() {
            track = TrackImpl.fromURI(TimelinePreviewsServiceImplTest::class.java.getResource(mediaResource).toURI())
            track!!.flavor = MediaPackageElements.PRESENTATION_SOURCE
            track!!.mimeType = MimeTypes.MJPEG
            track!!.addStream(VideoStreamImpl())
            track!!.duration = mediaDuration
            track!!.identifier = IdBuilderFactory.newInstance().newIdBuilder().createNew().compact()
        }
    }
}
