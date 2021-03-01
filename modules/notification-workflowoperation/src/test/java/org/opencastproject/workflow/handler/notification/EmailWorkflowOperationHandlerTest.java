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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.workflow.handler.notification;

import org.opencastproject.email.template.api.EmailTemplateService;
import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EmailWorkflowOperationHandlerTest {
  private WorkflowOperationInstance operation;
  private EmailWorkflowOperationHandler operationHandler;
  private final WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
  private MediaPackage mp;
  private URI uriMP;
  // private MimeMessage message;

  private static final String DEFAULT_TO = "somebody@testemail.com";
  private static final String DEFAULT_CC = "carboncopy@testemail.com";
  private static final String DEFAULT_BCC = "blindcarboncopy@testemail.com";
  private static final String DEFAULT_SUBJECT = "This is a subject";
  private static final String USER_NAME = "username";
  private static final String USER_NAME_EMAIL = "username@testemail.com";
  private static final String USER_NAME_NO_EMAIL = "username_no_email";

  private Capture<String[]> capturedTo;
  private Capture<String[]> capturedCC;
  private Capture<String[]> capturedBCC;
  private Capture<String> capturedSubject;
  private Capture<String> capturedBody;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = EmailWorkflowOperationHandlerTest.class.getResource("/email_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    operationHandler = new EmailWorkflowOperationHandler();

    EmailTemplateService emailTemplateService = EasyMock.createMock(EmailTemplateService.class);
    EasyMock.expect(emailTemplateService.applyTemplate("DCE_workflow_2_body",
            "This is the media package: ${mediaPackage.identifier}", workflowInstance, ", "))
            .andReturn("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557");
    EasyMock.expect(emailTemplateService.applyTemplate("template1", null, workflowInstance, ", "))
            .andReturn("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557");
    EasyMock.expect(emailTemplateService.applyTemplate("templateNotFound", null, workflowInstance, ", "))
            .andReturn("TEMPLATE NOT FOUND!");
    EasyMock.replay(emailTemplateService);
    operationHandler.setEmailTemplateService(emailTemplateService);

    SmtpService smtpService = EasyMock.createMock(SmtpService.class);
    capturedTo = Capture.newInstance();
    capturedCC = Capture.newInstance();
    capturedBCC = Capture.newInstance();
    capturedSubject = Capture.newInstance();
    capturedBody = Capture.newInstance();
    smtpService.send(EasyMock.capture(capturedTo), EasyMock.capture(capturedCC), EasyMock.capture(capturedBCC),
            EasyMock.capture(capturedSubject), EasyMock.capture(capturedBody), EasyMock.anyBoolean());
    EasyMock.expectLastCall().once();
    EasyMock.replay(smtpService);
    operationHandler.setSmtpService(smtpService);

    User user = EasyMock.createNiceMock(User.class);
    EasyMock.expect(user.getEmail()).andReturn(DEFAULT_TO).anyTimes();
    User emailUser = EasyMock.createNiceMock(User.class);
    EasyMock.expect(emailUser.getEmail()).andReturn(DEFAULT_TO).anyTimes();
    User noEmailUser = EasyMock.createNiceMock(User.class);
    EasyMock.expect(noEmailUser.getEmail()).andReturn(null).anyTimes();
    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(USER_NAME)).andReturn(user).anyTimes();
    EasyMock.expect(userDirectoryService.loadUser(USER_NAME_EMAIL)).andReturn(emailUser).anyTimes();
    EasyMock.expect(userDirectoryService.loadUser(USER_NAME_NO_EMAIL)).andReturn(noEmailUser).anyTimes();
    EasyMock.replay(user, emailUser, noEmailUser, userDirectoryService);
    operationHandler.setUserDirectoryService(userDirectoryService);

    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setTemplate("DCE-workflow");
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl failedOperation1 = new WorkflowOperationInstanceImpl("operation1",
            OperationState.FAILED);
    failedOperation1.setFailWorkflowOnException(true);
    WorkflowOperationInstanceImpl failedOperation2 = new WorkflowOperationInstanceImpl("operation2",
            OperationState.FAILED);
    failedOperation2.setFailWorkflowOnException(false);
    operation = new WorkflowOperationInstanceImpl("email", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationList = new ArrayList<WorkflowOperationInstance>();
    operationList.add(failedOperation1);
    operationList.add(failedOperation2);
    operationList.add(operation);
    workflowInstance.setOperations(operationList);
  }

  @Test
  public void testDefaultBody() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedTo.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_CC}, capturedCC.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_BCC}, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("Test Media Package (3e7bb56d-2fcc-4efe-9f0e-d6e56422f557)", capturedBody.getValue());
  }

  @Test
  public void testTemplateInBody() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
            "This is the media package: ${mediaPackage.identifier}");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedTo.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_CC}, capturedCC.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_BCC}, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody.getValue());
  }

  @Test
  public void testTemplateInFile() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "template1");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedTo.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_CC}, capturedCC.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_BCC}, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody.getValue());
  }

  @Test
  public void testTemplateNotFound() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, DEFAULT_TO);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, DEFAULT_CC);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, DEFAULT_BCC);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "templateNotFound");

    operationHandler.start(workflowInstance, null);

    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedTo.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_CC}, capturedCC.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_BCC}, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("TEMPLATE NOT FOUND!", capturedBody.getValue());
  }

  @Test
  public void testDestinationAsUserName() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, USER_NAME);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, USER_NAME);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
            "This is the media package: ${mediaPackage.identifier}");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedTo.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedCC.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody.getValue());
  }

  @Test
  public void testManyDestinationEmails() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY,
            USER_NAME + "," + DEFAULT_BCC + " " + DEFAULT_CC);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, null);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, null);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
            "This is the media package: ${mediaPackage.identifier}");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO, DEFAULT_BCC, DEFAULT_CC}, capturedTo.getValue());
    Assert.assertEquals(0, capturedCC.getValue().length);
    Assert.assertEquals(0, capturedBCC.getValue().length);
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody.getValue());
  }

  @Test
  public void testUserNameIsAnEmail() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, USER_NAME_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, USER_NAME_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
            "This is the media package: ${mediaPackage.identifier}");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedTo.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedCC.getValue());
    Assert.assertArrayEquals(new String[] {DEFAULT_TO}, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody.getValue());
  }

  @Test(expected = WorkflowOperationException.class)
  public void testUserNoEmail() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME_NO_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.CC_PROPERTY, USER_NAME_NO_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.BCC_PROPERTY, USER_NAME_NO_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, DEFAULT_SUBJECT);
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
            "This is the media package: ${mediaPackage.identifier}");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals(DEFAULT_TO, capturedTo.getValue());
    Assert.assertEquals(DEFAULT_TO, capturedCC.getValue());
    Assert.assertEquals(DEFAULT_TO, capturedBCC.getValue());
    Assert.assertEquals(DEFAULT_SUBJECT, capturedSubject.getValue());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", capturedBody.getValue());
  }

  @Test
  public void testSeparators() throws Exception {
    final String separator = "~|~";
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, USER_NAME_EMAIL + separator + USER_NAME_EMAIL);
    operation.setConfiguration(EmailWorkflowOperationHandler.ADDRESS_SEPARATOR_PROPERTY, separator);

    operationHandler.start(workflowInstance, null);
    Assert.assertArrayEquals(new String[] {DEFAULT_TO, DEFAULT_TO}, capturedTo.getValue());
  }

  @Test
  public void testNoRecipient() throws Exception {
    operation.setConfiguration(EmailWorkflowOperationHandler.SKIP_INVALID_ADDRESS_PROPERTY, "true");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

}
