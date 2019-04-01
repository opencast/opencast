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

package org.opencastproject.videosegmenter.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.metadata.mpeg7.MediaLocator
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl
import org.opencastproject.metadata.mpeg7.MediaRelTimeImpl
import org.opencastproject.metadata.mpeg7.MediaTime
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService
import org.opencastproject.metadata.mpeg7.MultimediaContentType
import org.opencastproject.metadata.mpeg7.Segment
import org.opencastproject.metadata.mpeg7.TemporalDecomposition
import org.opencastproject.metadata.mpeg7.Video
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl
import org.opencastproject.util.MimeTypes
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.easymock.IAnswer
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.Collections
import java.util.LinkedList

/**
 * Test class for video segmentation.
 */
class VideoSegmenterTest {

    /** The in-memory service registration  */
    protected var serviceRegistry: ServiceRegistry? = null
    protected var serviceRegistry1: ServiceRegistry? = null

    /** The video segmenter  */
    protected var vsegmenter: VideoSegmenterServiceImpl? = null
    protected var vsegmenter1: VideoSegmenterServiceImpl? = null

    protected var mpeg7Service: Mpeg7CatalogService? = null
    protected var mpeg7Service1: Mpeg7CatalogService? = null

    /** Temp file  */
    protected var tempFile: File? = null
    protected var tempFile1: File? = null

    @Rule
    var testFolder = TemporaryFolder()

    /**
     * Setup for the video segmenter service, including creation of a mock workspace.
     *
     * @throws Exception
     * if setup fails
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        mpeg7Service = Mpeg7CatalogService()
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject<Any>() as URI)).andReturn(File(track!!.getURI()))
        tempFile = testFolder.newFile(javaClass.name + ".xml")
        EasyMock.expect(
                workspace.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as InputStream)).andAnswer {
            val `in` = EasyMock.getCurrentArguments()[2] as InputStream
            IOUtils.copy(`in`, FileOutputStream(tempFile!!))
            tempFile!!.toURI()
        }
        EasyMock.replay(workspace)

        mpeg7Service1 = Mpeg7CatalogService()
        val workspace1 = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace1.get(EasyMock.anyObject<Any>() as URI)).andReturn(File(track1!!.getURI()))
        tempFile1 = testFolder.newFile(javaClass.name + "-1.xml")
        EasyMock.expect(
                workspace1.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as InputStream)).andAnswer {
            val `in` = EasyMock.getCurrentArguments()[2] as InputStream
            IOUtils.copy(`in`, FileOutputStream(tempFile1!!))
            tempFile1!!.toURI()
        }
        EasyMock.replay(workspace1)

        val anonymous = JaxbUser("anonymous", "test", DefaultOrganization(), JaxbRole(
                DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, DefaultOrganization()))
        val userDirectoryService = EasyMock.createMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject<Any>() as String)).andReturn(anonymous).anyTimes()
        EasyMock.replay(userDirectoryService)

        val organization = DefaultOrganization()
        val organizationDirectoryService = EasyMock.createMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect(organizationDirectoryService.getOrganization(EasyMock.anyObject<Any>() as String))
                .andReturn(organization).anyTimes()
        EasyMock.replay(organizationDirectoryService)

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.user).andReturn(anonymous).anyTimes()
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService)

        vsegmenter = VideoSegmenterServiceImpl()
        serviceRegistry = ServiceRegistryInMemoryImpl(vsegmenter!!, securityService, userDirectoryService,
                organizationDirectoryService, EasyMock.createNiceMock(IncidentService::class.java))
        vsegmenter!!.serviceRegistry = serviceRegistry
        vsegmenter!!.setMpeg7CatalogService(mpeg7Service)
        vsegmenter!!.setWorkspace(workspace)
        vsegmenter!!.securityService = securityService
        vsegmenter!!.userDirectoryService = userDirectoryService
        vsegmenter!!.organizationDirectoryService = organizationDirectoryService

        vsegmenter1 = VideoSegmenterServiceImpl()
        serviceRegistry1 = ServiceRegistryInMemoryImpl(vsegmenter1!!, securityService, userDirectoryService,
                organizationDirectoryService, EasyMock.createNiceMock(IncidentService::class.java))
        vsegmenter1!!.serviceRegistry = serviceRegistry1
        vsegmenter1!!.setMpeg7CatalogService(mpeg7Service1)
        vsegmenter1!!.setWorkspace(workspace1)
        vsegmenter1!!.securityService = securityService
        vsegmenter1!!.userDirectoryService = userDirectoryService
        vsegmenter1!!.organizationDirectoryService = organizationDirectoryService

        // set parameters for segmentation because the default parameters are not suitable for too short videos
        vsegmenter!!.prefNumber = 2
        vsegmenter!!.stabilityThreshold = 2
        vsegmenter!!.absoluteMin = 1

        vsegmenter1!!.stabilityThreshold = 2
        vsegmenter1!!.changesThreshold = 0.025f
        vsegmenter1!!.prefNumber = 5
        vsegmenter1!!.maxCycles = 5
        vsegmenter1!!.maxError = 0.2f
        vsegmenter1!!.absoluteMin = 1
    }

    /**
     * @throws java.io.File.IOException
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(tempFile)
        FileUtils.deleteQuietly(tempFile1)
        (serviceRegistry as ServiceRegistryInMemoryImpl).dispose()
        (serviceRegistry1 as ServiceRegistryInMemoryImpl).dispose()
    }

    @Test
    @Throws(Exception::class)
    fun testAnalyze() {
        val receipt = vsegmenter!!.segment(track!!)
        val jobBarrier = JobBarrier(null, serviceRegistry, 1000, receipt)
        jobBarrier.waitForJobs()

        val catalog = MediaPackageElementParser.getFromXml(receipt.payload) as Catalog

        val mpeg7 = Mpeg7CatalogImpl(catalog.getURI().toURL().openStream())

        // Is there multimedia content in the mpeg7?
        assertTrue("Audiovisual content was expected", mpeg7.hasVideoContent())

        val contentType = mpeg7.multimediaContent().next().elements().next()

        // Is there at least one segment?
        val segments = contentType.temporalDecomposition
        val si = segments.segments()
        assertTrue(si.hasNext())
        val firstSegment = si.next()
        val firstSegmentMediaTime = firstSegment.mediaTime
        var startTime = firstSegmentMediaTime.mediaTimePoint.timeInMilliseconds
        var duration = firstSegmentMediaTime.mediaDuration.durationInMilliseconds
        assertEquals("Unexpected start time of first segment", 0, startTime)
        assertEquals("Unexpected duration of first segment", firstSegmentDuration, duration)

        // What about the second one?
        assertTrue("Video is expected to have more than one segment", si.hasNext())

        val secondSegment = si.next()
        val secondSegmentMediaTime = secondSegment.mediaTime
        startTime = secondSegmentMediaTime.mediaTimePoint.timeInMilliseconds
        duration = secondSegmentMediaTime.mediaDuration.durationInMilliseconds
        assertEquals("Unexpected start time of second segment", firstSegmentDuration, startTime)
        assertEquals("Unexpected duration of second segment", secondSegmentDuration, duration)

        // There should be no third segment
        assertFalse("Found an unexpected third video segment", si.hasNext())
    }

    @Test
    @Throws(Exception::class)
    fun testAnalyzeOptimization() {
        val receipt = vsegmenter1!!.segment(track1!!)
        val jobBarrier = JobBarrier(null, serviceRegistry1, 1000, receipt)
        jobBarrier.waitForJobs()

        val catalog = MediaPackageElementParser.getFromXml(receipt.payload) as Catalog

        val mpeg7 = Mpeg7CatalogImpl(catalog.getURI().toURL().openStream())

        // Is there multimedia content in the mpeg7?
        assertTrue("Audiovisual content was expected", mpeg7.hasVideoContent())
        assertNotNull("Audiovisual content expected", mpeg7.multimediaContent().next().elements().hasNext())

        val contentType = mpeg7.multimediaContent().next().elements().next()

        // Is there at least one segment?
        val segments = contentType.temporalDecomposition
        val si = segments.segments()
        assertTrue(si.hasNext())

        // Is the error of optimization small enough?
        var segmentCounter = 0
        while (si.hasNext()) {
            si.next()
            ++segmentCounter
        }
        val error = Math.abs((segmentCounter - vsegmenter1!!.prefNumber) / vsegmenter1!!.prefNumber.toFloat())
        assertTrue("Error of Optimization is too big", error <= vsegmenter1!!.maxError)
    }

    @Test
    @Throws(Exception::class)
    fun testAnalyzeOptimizedList() {
        val receipt = vsegmenter!!.segment(track!!)
        val jobBarrier = JobBarrier(null, serviceRegistry, 1000, receipt)
        jobBarrier.waitForJobs()

        val catalog = MediaPackageElementParser.getFromXml(receipt.payload) as Catalog
        val mpeg7 = Mpeg7CatalogImpl(catalog.getURI().toURL().openStream())

        val optimizedList = LinkedList<OptimizationStep>()
        val firstStep = OptimizationStep(10, 0.015f, 46, 41, mpeg7, null)
        val secondStep = OptimizationStep(10, 0.167f, 34, 41, mpeg7, null)
        val thirdStep = OptimizationStep(10, 0.011f, 44, 41, mpeg7, null)
        val fourthStep = OptimizationStep(10, 0.200f, 23, 41, mpeg7, null)

        val error1 = (46 - 41) / 41.toFloat() // ~  0.122
        val error2 = (34 - 41) / 41.toFloat() // ~ -0.171
        val error3 = (44 - 41) / 41.toFloat() // ~  0.073
        val error4 = (23 - 41) / 41.toFloat() // ~ -0.439

        optimizedList.add(firstStep)
        optimizedList.add(secondStep)
        optimizedList.add(thirdStep)
        optimizedList.add(fourthStep)
        Collections.sort(optimizedList)

        // check if the errors were calculated correctly and  whether the elements are in the correct order
        assertEquals("first element of optimized list incorrect", error3, optimizedList[0].error, 0.0001f)
        assertEquals("second element of optimized list incorrect", error1, optimizedList[1].error, 0.0001f)
        assertEquals("third element of optimized list incorrect", error4, optimizedList[2].error, 0.0001f)
        assertEquals("fourth element of optimized list incorrect", error2, optimizedList[3].error, 0.0001f)
        assertTrue("first error in optimized list is not positive", optimizedList[0].error >= 0)
        assertTrue("second error in optimized list is not bigger than first",
                optimizedList[1].error > optimizedList[0].error)
        assertTrue("third error in optimized list is not negative", optimizedList[2].error < 0)
        assertTrue("fourth error in optimized list is smaller than third",
                optimizedList[3].error > optimizedList[2].error)
    }

    @Test
    fun testAnalyzeSegmentMerging() {
        val mpeg7catalogService = vsegmenter!!.mpeg7CatalogService
        val contentTime = MediaRelTimeImpl(0, track!!.duration!!)
        val contentLocator = MediaLocatorImpl(track!!.getURI())
        val mpeg7 = mpeg7catalogService!!.newInstance()
        val videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator)
        var segments: LinkedList<Segment>
        var result: LinkedList<Segment>
        var segmentcount = 1
        track!!.duration = 47000L

        // list of segment durations (starttimes can be calculated from those)
        val segmentArray1 = intArrayOf(3000, 2000, 8000, 3000, 1000, 6000, 3000, 2000, 4000, 11000, 2000, 2000)
        val segmentArray2 = intArrayOf(1000, 2000, 8000, 3000, 1000, 6000, 3000, 2000, 4000, 11000, 2000, 4000)
        val segmentArray3 = intArrayOf(1000, 2000, 4000, 3000, 1000, 2000, 3000, 2000, 4000, 1000, 2000, 4000)
        val segmentArray4 = intArrayOf(6000, 7000, 13000, 9000, 8000, 11000, 5000, 16000)

        // predicted outcome of filtering the segmentation
        val prediction1 = intArrayOf(5000, 10000, 8000, 9000, 15000)
        val prediction2 = intArrayOf(13000, 8000, 9000, 11000, 6000)
        val prediction3 = intArrayOf(29000)
        val prediction4 = intArrayOf(6000, 7000, 13000, 9000, 8000, 11000, 5000, 16000)

        // total duration of respective segment arrays
        val duration1 = 47000L
        val duration2 = 47000L
        val duration3 = 29000L
        val duration4 = 75000L

        val segmentArray = arrayOf(segmentArray1, segmentArray2, segmentArray3, segmentArray4)
        val prediction = arrayOf(prediction1, prediction2, prediction3, prediction4)
        val durations = longArrayOf(duration1, duration2, duration3, duration4)

        // check for all test segmentations if "filterSegmentation" yields the expected result
        for (k in segmentArray.indices) {

            segments = LinkedList()
            result = LinkedList()
            track!!.duration = durations[k]
            var previous = 0

            for (i in 0 until segmentArray[k].size) {
                val s = videoContent.temporalDecomposition.createSegment("segment-" + segmentcount++)
                s.mediaTime = MediaRelTimeImpl(previous.toLong(), segmentArray[k][i].toLong())
                segments.add(s)

                previous += segmentArray[k][i]
            }

            vsegmenter!!.filterSegmentation(segments, track, result, 5000)

            assertEquals("segment merging yields wrong number of segments", prediction[k].size.toLong(), result.size.toLong())

            previous = 0
            for (i in 0 until prediction[k].size) {
                val message = "segment $i in set $k has the wrong start time."
                val message1 = "segment $i in set $k has the wrong duration."
                assertEquals(message, previous.toLong(), result[i].mediaTime.mediaTimePoint.timeInMilliseconds)
                assertEquals(message1, prediction[k][i].toLong(), result[i].mediaTime.mediaDuration
                        .durationInMilliseconds)
                previous += prediction[k][i]
            }
        }

    }

    companion object {

        /** Video file to test. Contains a new scene at 00:12  */
        protected val mediaResource = "/scene-change.mov"

        /** Video file to test the optimization  */
        protected val mediaResource1 = "/test-optimization.mp4"

        /** Duration of whole movie  */
        protected val mediaDuration = 20000L
        protected val mediaDuration1 = 30000L

        /** Duration of the first segment  */
        protected val firstSegmentDuration = 12000L

        /** Duration of the seconds segment  */
        protected val secondSegmentDuration = mediaDuration - firstSegmentDuration

        /** The media url  */
        protected var track: TrackImpl? = null
        protected var track1: TrackImpl? = null

        /**
         * Copies test files to the local file system, since jmf is not able to access movies from the resource section of a
         * bundle.
         *
         * @throws Exception
         * if setup fails
         */
        @BeforeClass
        @Throws(Exception::class)
        fun setUpClass() {
            track = TrackImpl.fromURI(VideoSegmenterTest::class.java.getResource(mediaResource).toURI())
            track!!.flavor = MediaPackageElements.PRESENTATION_SOURCE
            track!!.mimeType = MimeTypes.MJPEG
            track!!.addStream(VideoStreamImpl())
            track!!.duration = 20000

            track1 = TrackImpl.fromURI(VideoSegmenterTest::class.java.getResource(mediaResource1).toURI())
            track1!!.flavor = MediaPackageElements.PRESENTATION_SOURCE
            track1!!.mimeType = MimeTypes.MJPEG
            track1!!.addStream(VideoStreamImpl())
            track1!!.duration = mediaDuration1
        }
    }

}
