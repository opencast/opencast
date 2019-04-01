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
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.ProtocolVersion
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.HashMap

class HttpNotificationWorkflowOperationHandlerTest {

    private var operationHandler: HttpNotificationWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null

    // mock services and objects
    private var client: TrustedHttpClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        val uriMP = javaClass.getResource("/concat_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP.toURL().openStream())

        // set up mock trusted http client
        client = EasyMock.createNiceMock<TrustedHttpClient>(TrustedHttpClient::class.java)
        val response = BasicHttpResponse(BasicStatusLine(ProtocolVersion("HTTP", 1, 1),
                HttpStatus.SC_ACCEPTED, ""))
        EasyMock.expect(client!!.execute(EasyMock.anyObject<Any>() as HttpUriRequest, EasyMock.anyInt(), EasyMock.anyInt()))
                .andReturn(response)
        EasyMock.replay(client!!)

        // set up service
        operationHandler = HttpNotificationWorkflowOperationHandler()
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    // todo test does not pass
    fun testSuccessfulNotification() {

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[HttpNotificationWorkflowOperationHandler.OPT_URL_PATH] = "http://www.host-does-not-exist.com"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_NOTIFICATION_SUBJECT] = "test"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_NOTIFICATION_SUBJECT] = "test message"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_MAX_RETRY] = "2"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_TIMEOUT] = Integer.toString(10 * 1000)

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(result.action, Action.CONTINUE)
    }

    @Test
    @Throws(Exception::class)
    fun testNotificationFailedAfterOneTry() {
        client = EasyMock.createNiceMock<TrustedHttpClient>(TrustedHttpClient::class.java)
        val response = BasicHttpResponse(BasicStatusLine(ProtocolVersion("HTTP", 1, 1),
                HttpStatus.SC_NOT_FOUND, ""))
        EasyMock.expect(client!!.execute(EasyMock.anyObject<Any>() as HttpPut, EasyMock.anyInt(), EasyMock.anyInt())).andReturn(
                response)
        EasyMock.replay(client!!)

        // set up service
        operationHandler = HttpNotificationWorkflowOperationHandler()

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[HttpNotificationWorkflowOperationHandler.OPT_URL_PATH] = "http://127.0.0.1:9"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_NOTIFICATION_SUBJECT] = "test"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_MAX_RETRY] = "0"
        configurations[HttpNotificationWorkflowOperationHandler.OPT_TIMEOUT] = Integer.toString(10)

        // run the operation handler
        try {
            getWorkflowOperationResult(mp, configurations)
            Assert.fail("Operation handler should have thrown an exception!")
        } catch (e: WorkflowOperationException) {
            Assert.assertTrue("Exception thrown as expected by the operation handler", true)
        }

    }

    @Throws(WorkflowOperationException::class)
    private fun getWorkflowOperationResult(mp: MediaPackage?, configurations: Map<String, String>): WorkflowOperationResult {
        // Add the mediapackage to a workflow instance
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        operation.template = "http-notify"
        operation.state = OperationState.RUNNING
        for (key in configurations.keys) {
            operation.setConfiguration(key, configurations[key])
        }

        val operationsList = ArrayList<WorkflowOperationInstance>()
        operationsList.add(operation)
        workflowInstance.operations = operationsList

        // Run the media package through the operation handler, ensuring that metadata gets added
        return operationHandler!!.start(workflowInstance, null)
    }

}
