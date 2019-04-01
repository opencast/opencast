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

import org.opencastproject.email.template.api.EmailTemplateService
import org.opencastproject.kernel.mail.SmtpService
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList

class EmailWorkflowOperationHandlerTest {
    private var operation: WorkflowOperationInstance? = null
    private var operationHandler: EmailWorkflowOperationHandler? = null
    private val workflowInstance = WorkflowInstanceImpl()
    private var mp: MediaPackage? = null
    private var uriMP: URI? = null

    private var capturedTo: Capture<String>? = null
    private var capturedCC: Capture<String>? = null
    private var capturedBCC: Capture<String>? = null
    private var capturedSubject: Capture<String>? = null
    private var capturedBody: Capture<String>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        uriMP = EmailWorkflowOperationHandlerTest::class.java.getResource("/email_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP!!.toURL().openStream())

        operationHandler = EmailWorkflowOperationHandler()

        val emailTemplateService = EasyMock.createMock<EmailTemplateService>(EmailTemplateService::class.java)
        EasyMock.expect(emailTemplateService.applyTemplate("DCE_workflow_2_body",
                "This is the media package: \${mediaPackage.identifier}", workflowInstance))
                .andReturn("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557")
        EasyMock.expect(emailTemplateService.applyTemplate("template1", null!!, workflowInstance))
                .andReturn("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557")
        EasyMock.expect(emailTemplateService.applyTemplate("templateNotFound", null!!, workflowInstance))
                .andReturn("TEMPLATE NOT FOUND!")
        EasyMock.replay(emailTemplateService)
        operationHandler!!.setEmailTemplateService(emailTemplateService)

        val smtpService = EasyMock.createMock<SmtpService>(SmtpService::class.java)
        capturedTo = Capture.newInstance<String>()
        capturedCC = Capture.newInstance<String>()
        capturedBCC = Capture.newInstance<String>()
        capturedSubject = Capture.newInstance<String>()
        capturedBody = Capture.newInstance<String>()
        smtpService.send(EasyMock.capture(capturedTo), EasyMock.capture(capturedCC), EasyMock.capture(capturedBCC),
                EasyMock.capture(capturedSubject), EasyMock.capture(capturedBody), EasyMock.anyBoolean())
        EasyMock.expectLastCall<Any>().once()
        EasyMock.replay(smtpService)
        operationHandler!!.setSmtpService(smtpService)

        val user = EasyMock.createNiceMock<User>(User::class.java)
        EasyMock.expect(user.email).andReturn(DEFAULT_TO).anyTimes()
        val emailUser = EasyMock.createNiceMock<User>(User::class.java)
        EasyMock.expect(emailUser.email).andReturn(DEFAULT_TO).anyTimes()
        val noEmailUser = EasyMock.createNiceMock<User>(User::class.java)
        EasyMock.expect(noEmailUser.email).andReturn(null).anyTimes()
        val userDirectoryService = EasyMock.createNiceMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectoryService.loadUser(USER_NAME)).andReturn(user).anyTimes()
        EasyMock.expect(userDirectoryService.loadUser(USER_NAME_EMAIL)).andReturn(emailUser).anyTimes()
        EasyMock.expect(userDirectoryService.loadUser(USER_NAME_NO_EMAIL)).andReturn(noEmailUser).anyTimes()
        EasyMock.replay(user, emailUser, noEmailUser, userDirectoryService)
        operationHandler!!.setUserDirectoryService(userDirectoryService)

        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.template = "DCE-workflow"
        workflowInstance.mediaPackage = mp
        val failedOperation1 = WorkflowOperationInstanceImpl("operation1",
                OperationState.FAILED)
        failedOperation1.isFailWorkflowOnException = true
        val failedOperation2 = WorkflowOperationInstanceImpl("operation2",
                OperationState.FAILED)
        failedOperation2.isFailWorkflowOnException = false
        operation = WorkflowOperationInstanceImpl("email", OperationState.RUNNING)
        val operationList = ArrayList<WorkflowOperationInstance>()
        operationList.add(failedOperation1)
        operationList.add(failedOperation2)
        operationList.add(operation)
        workflowInstance.operations = operationList
    }

    @Test
    @Throws(Exception::class)
    fun testDefaultBody() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_CC, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_BCC, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("Test Media Package(3e7bb56d-2fcc-4efe-9f0e-d6e56422f557)", capturedBody!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateInBody() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
                "This is the media package: \${mediaPackage.identifier}")

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_CC, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_BCC, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateInFile() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "template1")

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_CC, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_BCC, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateNotFound() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "templateNotFound")

        operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_CC, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_BCC, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("TEMPLATE NOT FOUND!", capturedBody!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testDestinationAsUserName() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, USER_NAME)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, USER_NAME)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
                "This is the media package: \${mediaPackage.identifier}")

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_TO, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_TO, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testManyDestinationEmails() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY,
                "$USER_NAME,$DEFAULT_BCC $DEFAULT_CC")
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, null)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, null)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
                "This is the media package: \${mediaPackage.identifier}")

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals("$DEFAULT_TO,$DEFAULT_BCC,$DEFAULT_CC", capturedTo!!.value)
        Assert.assertNull(capturedCC!!.value)
        Assert.assertNull(capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testUserNameIsAnEmail() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME_EMAIL)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, USER_NAME_EMAIL)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, USER_NAME_EMAIL)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
                "This is the media package: \${mediaPackage.identifier}")

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_TO, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_TO, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody!!.value)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testUserNoEmail() {
        operation!!.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME_NO_EMAIL)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, USER_NAME_NO_EMAIL)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, USER_NAME_NO_EMAIL)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT)
        operation!!.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
                "This is the media package: \${mediaPackage.identifier}")

        val result = operationHandler!!.start(workflowInstance, null)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(DEFAULT_TO, capturedTo!!.value)
        Assert.assertEquals(DEFAULT_TO, capturedCC!!.value)
        Assert.assertEquals(DEFAULT_TO, capturedBCC!!.value)
        Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject!!.value)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody!!.value)
    }

    companion object {
        // private MimeMessage message;

        private val DEFAULT_TO = "somebody@testemail.com"
        private val DEFAULT_CC = "carboncopy@testemail.com"
        private val DEFAULT_BCC = "blindcarboncopy@testemail.com"
        private val DEFAULT_SUBJECT = "This is a subject"
        private val USER_NAME = "username"
        private val USER_NAME_EMAIL = "username@testemail.com"
        private val USER_NAME_NO_EMAIL = "username_no_email"
    }

}
