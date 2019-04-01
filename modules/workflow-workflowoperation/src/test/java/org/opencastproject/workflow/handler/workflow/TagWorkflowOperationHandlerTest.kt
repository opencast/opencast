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

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.Track
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.ArrayList

/**
 * Test class for [TagWorkflowOperationHandler]
 */
class TagWorkflowOperationHandlerTest {

    private var operationHandler: TagWorkflowOperationHandler? = null
    private var mp: MediaPackage? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.loadFromXml(this.javaClass.getResourceAsStream("/archive_mediapackage.xml"))

        // set up the handler
        operationHandler = TagWorkflowOperationHandler()
    }

    @Test
    @Throws(Exception::class)
    fun testAllTagsFlavors() {
        var instance = WorkflowInstanceImpl()
        var ops: MutableList<WorkflowOperationInstance> = ArrayList()
        var operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp

        operation.setConfiguration(TagWorkflowOperationHandler.SOURCE_FLAVORS_PROPERTY, "presenter/source")
        operation.setConfiguration(TagWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "presenter/test")
        operation.setConfiguration(TagWorkflowOperationHandler.TARGET_TAGS_PROPERTY, "tag1,tag2")
        operation.setConfiguration(TagWorkflowOperationHandler.COPY_PROPERTY, "false")

        var result = operationHandler!!.start(instance, null)
        var resultingMediapackage = result.mediaPackage

        var track = resultingMediapackage.getTrack("track-2")
        Assert.assertEquals("presenter/test", track.flavor.toString())
        Assert.assertEquals("tag1", track.tags[0])
        Assert.assertEquals("tag2", track.tags[1])

        instance = WorkflowInstanceImpl()
        ops = ArrayList()
        operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = resultingMediapackage

        operation.setConfiguration(TagWorkflowOperationHandler.SOURCE_TAGS_PROPERTY, "tag1")
        operation.setConfiguration(TagWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "presenter/source")
        operation.setConfiguration(TagWorkflowOperationHandler.TARGET_TAGS_PROPERTY, "-tag1,+tag3")
        operation.setConfiguration(TagWorkflowOperationHandler.COPY_PROPERTY, "false")

        result = operationHandler!!.start(instance, null)
        resultingMediapackage = result.mediaPackage

        track = resultingMediapackage.getTrack("track-2")
        Assert.assertEquals("presenter/source", track.flavor.toString())
        Assert.assertEquals("tag2", track.tags[0])
        Assert.assertEquals("tag3", track.tags[1])
    }

}
