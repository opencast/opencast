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


package org.opencastproject.workflow.handler.notification

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl

import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

class MediaPackagePostOperationHandlerTest {

    /** Represents a tuple of handler and instance, useful for return types  */
    private class InstanceAndHandler private constructor(private val workflowInstance: WorkflowInstanceImpl,
                                                         private val workflowHandler: WorkflowOperationHandler)

    /**
     * Creates a new workflow and readies the engine for processing
     */
    private fun createWorkflow(url: String, format: String): InstanceAndHandler {
        val handler = MediaPackagePostOperationHandler()

        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        val operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        val operationsList = ArrayList<WorkflowOperationInstance>()
        operationsList.add(operation)
        workflowInstance.operations = operationsList

        operation.setConfiguration("url", url)
        operation.setConfiguration("format", format)
        operation.setConfiguration("mediapackage.type", "workflow")
        return InstanceAndHandler(workflowInstance, handler)
    }

    @Test
    @Throws(Exception::class)
    fun testHTTPPostXML() {
        // create a dummy mediapackage
        val factory = MediaPackageBuilderFactory.newInstance()
        val builder = factory.newMediaPackageBuilder()
        val mp = builder!!.createNew(IdImpl("xyz"))
        mp.title = "test"
        mp.addContributor("lkiesow")
        mp.addContributor("lkiesow")

        /* Sending stuff to port 9 shound never return anything as the Discard
     * Protocol uses port 9 */
        val tuple = createWorkflow("http://127.0.0.1:9", "xml")
        val handler = tuple.workflowHandler as MediaPackagePostOperationHandler
        tuple.workflowInstance.mediaPackage = mp

        try {
            tuple.workflowHandler.start(tuple.workflowInstance, null)
            /* This should raise an exception. Something is wrong if not. */
            Assert.fail()
        } catch (e: WorkflowOperationException) {
            logger.info(e.toString())
        }

    }

    @Test
    @Throws(Exception::class)
    fun testHTTPPostJSON() {
        // create a dummy mediapackage
        val factory = MediaPackageBuilderFactory.newInstance()
        val builder = factory.newMediaPackageBuilder()
        val mp = builder!!.createNew(IdImpl("xyz"))
        mp.title = "test"
        mp.addContributor("lkiesow")
        mp.addContributor("lkiesow")

        /* Sending stuff to port 9 shound never return anything as the Discard
     * Protocol uses port 9 */
        val tuple = createWorkflow("http://127.0.0.1:9", "json")
        val handler = tuple.workflowHandler as MediaPackagePostOperationHandler
        tuple.workflowInstance.mediaPackage = mp

        try {
            tuple.workflowHandler.start(tuple.workflowInstance, null)
            /* This should raise an exception. Something is wrong if not. */
            Assert.fail()
        } catch (e: WorkflowOperationException) {
            logger.info(e.toString())
        }

    }

    companion object {

        /** the logging facility provided by log4j  */
        private val logger = LoggerFactory.getLogger(MediaPackagePostOperationHandlerTest::class.java.name)
    }

}
