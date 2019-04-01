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

import java.util.HashSet

class ProbeResolutionWorkflowOperationHandlerTest {

    private var operationHandler: ProbeResolutionWorkflowOperationHandler? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        operationHandler = ProbeResolutionWorkflowOperationHandler()
    }

    @Test
    fun testGetResolutions() {
        assertTrue(operationHandler!!.getResolutions("").isEmpty())

        val res = operationHandler!!.getResolutions("320x240,1280x720, 1920x1080")
        assertEquals(Fraction.getFraction(320, 240), res[0])
        assertEquals(Fraction.getFraction(1280, 720), res[1])
        assertEquals(Fraction.getFraction(1920, 1080), res[2])
    }

    @Test
    @Throws(MediaPackageException::class, WorkflowOperationException::class)
    fun testStart() {
        val mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        val videoStream = VideoStreamImpl("234")
        videoStream.setFrameWidth(1280)
        videoStream.setFrameHeight(720)
        val track = TrackImpl()
        track.flavor = MediaPackageElementFlavor.parseFlavor("presenter/source")
        track.addStream(videoStream)

        val jobContext = EasyMock.createMock<JobContext>(JobContext::class.java)
        EasyMock.replay(jobContext)

        val operationInstance = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        val config = arrayOf(arrayOf(ProbeResolutionWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source"), arrayOf(ProbeResolutionWorkflowOperationHandler.OPT_VAR_PREFIX + "aspect", "1280x720,1280x700"), arrayOf(ProbeResolutionWorkflowOperationHandler.OPT_VAL_PREFIX + "aspect", "16/9"), arrayOf(ProbeResolutionWorkflowOperationHandler.OPT_VAR_PREFIX + "is_720", "1280x720,1280x700"), arrayOf(ProbeResolutionWorkflowOperationHandler.OPT_VAR_PREFIX + "is_1080", "1920x1080"))
        val keys = HashSet<String>()
        for (cfg in config) {
            keys.add(cfg[0])
            EasyMock.expect(operationInstance.getConfiguration(cfg[0])).andReturn(cfg[1]).anyTimes()
        }
        EasyMock.expect(operationInstance.configurationKeys).andReturn(keys).anyTimes()
        EasyMock.replay(operationInstance)

        val workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(operationInstance).anyTimes()
        EasyMock.replay(workflowInstance)

        // With no matching track
        assertEquals(null, operationHandler!!.start(workflowInstance, jobContext).properties)

        // With matching track
        mediaPackage.add(track)
        val workflowOperationResult = operationHandler!!.start(workflowInstance, jobContext)
        val properties = workflowOperationResult.properties

        val props = arrayOf(arrayOf("presenter_source_aspect", "16/9"), arrayOf("presenter_source_is_720", "true"), arrayOf<String>("presenter_source_is_1080", null))
        for (prop in props) {
            assertEquals(prop[1], properties[prop[0]])
        }
    }

}
