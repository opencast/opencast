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

package org.opencastproject.workflow.handler.workflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult

import org.apache.commons.lang3.math.Fraction
import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

class AnalyzeTracksWorkflowOperationHandlerTest {

    private val logger = LoggerFactory.getLogger(AnalyzeTracksWorkflowOperationHandlerTest::class.java)

    private var operationHandler: AnalyzeTracksWorkflowOperationHandler? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        operationHandler = AnalyzeTracksWorkflowOperationHandler()
    }

    @Test
    @Throws(Exception::class)
    fun testGetNearestResolution() {
        val frac43 = Fraction.getFraction(4, 3)
        val frac169 = Fraction.getFraction(16, 9)
        val aspects = ArrayList<Fraction>()
        aspects.add(frac43)
        aspects.add(frac169)

        val aspect43 = arrayOf(intArrayOf(640, 480), intArrayOf(768, 576), intArrayOf(720, 576), intArrayOf(703, 576), intArrayOf(720, 576), intArrayOf(720, 480), intArrayOf(240, 180), intArrayOf(638, 512), intArrayOf(704, 576), intArrayOf(756, 576), intArrayOf(800, 600), intArrayOf(1024, 768), intArrayOf(1280, 1023), intArrayOf(1016, 768), intArrayOf(1280, 1024))
        val aspect169 = arrayOf(intArrayOf(1024, 576), intArrayOf(1280, 720), intArrayOf(1068, 600), intArrayOf(1248, 702), intArrayOf(1278, 720))

        for (resArr in aspect43) {
            val res = Fraction.getFraction(resArr[0], resArr[1])
            val aspect = operationHandler!!.getNearestAspectRatio(res, aspects)
            logger.info("res: {} -> aspect: {} | expected 4/3", res, aspect)
            assertEquals(frac43, aspect)
        }

        for (resArr in aspect169) {
            val res = Fraction.getFraction(resArr[0], resArr[1])
            val aspect = operationHandler!!.getNearestAspectRatio(res, aspects)
            logger.info("res: {} -> aspect: {} | expected 16/9", res, aspect)
            assertEquals(frac169, aspect)
        }
    }

    @Test
    fun testGetAspectRatio() {
        val a = operationHandler!!.getAspectRatio("4/3,16/9")
        assertEquals(Fraction.getFraction(4, 3), a[0])
        assertEquals(Fraction.getFraction(16, 9), a[1])
        assertTrue(operationHandler!!.getAspectRatio("").isEmpty())
    }

    @Test
    @Throws(MediaPackageException::class, WorkflowOperationException::class)
    fun testStart() {
        val mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        val videoStream = VideoStreamImpl("234")
        videoStream.setFrameWidth(1280)
        videoStream.setFrameHeight(720)
        videoStream.frameRate = 30.0f
        val track = TrackImpl()
        track.flavor = MediaPackageElementFlavor.parseFlavor("presenter/source")
        track.addStream(videoStream)

        val jobContext = EasyMock.createMock<JobContext>(JobContext::class.java)
        EasyMock.replay(jobContext)

        val operationInstance = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        val config = arrayOf(arrayOf(AnalyzeTracksWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source"), arrayOf(AnalyzeTracksWorkflowOperationHandler.OPT_VIDEO_ASPECT, "4/3,16/9"))
        for (cfg in config) {
            EasyMock.expect(operationInstance.getConfiguration(cfg[0])).andReturn(cfg[1]).anyTimes()
        }
        EasyMock.expect(operationInstance.getConfiguration(AnalyzeTracksWorkflowOperationHandler.OPT_FAIL_NO_TRACK))
                .andReturn("true")
        EasyMock.expect(operationInstance.getConfiguration(AnalyzeTracksWorkflowOperationHandler.OPT_FAIL_NO_TRACK))
                .andReturn("false").anyTimes()
        EasyMock.replay(operationInstance)

        val workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflowInstance.id).andReturn(0L).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(operationInstance).anyTimes()
        EasyMock.replay(workflowInstance)

        // With no matching track (should fail)
        try {
            operationHandler!!.start(workflowInstance, jobContext)
            fail()
        } catch (e: WorkflowOperationException) {
            logger.info("Fail on no tracks works")
        }

        var workflowOperationResult = operationHandler!!.start(workflowInstance, jobContext)
        var properties = workflowOperationResult.properties
        assertTrue(properties.isEmpty())

        // With matching track
        mediaPackage.add(track)
        workflowOperationResult = operationHandler!!.start(workflowInstance, jobContext)
        properties = workflowOperationResult.properties

        val props = arrayOf(arrayOf("presenter_source_media", "true"), arrayOf("presenter_source_audio", "false"), arrayOf("presenter_source_aspect", "16/9"), arrayOf("presenter_source_resolution_y", "720"), arrayOf("presenter_source_resolution_x", "1280"), arrayOf("presenter_source_aspect_snap", "16/9"), arrayOf("presenter_source_video", "true"), arrayOf("presenter_source_framerate", "30.0"))
        for (prop in props) {
            assertEquals(prop[1], properties[prop[0]])
        }
    }

}
