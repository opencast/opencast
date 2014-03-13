/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler;

import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailWorkflowOperationHandlerTest {
  private WorkflowOperationInstance operation;
  private EmailWorkflowOperationHandler operationHandler;
  private WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
  private MediaPackage mp;
  private URI uriMP;
  private MimeMessage message;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = EmailWorkflowOperationHandlerTest.class.getResource("/email_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    operationHandler = new EmailWorkflowOperationHandler();

    Properties empty = new Properties();
    message = new MimeMessage(Session.getInstance(empty));

    // Note: actual template substitution tests can be found in SmtpServiceTest (kernel bundle)
    SmtpService smtpService = EasyMock.createMock(SmtpService.class);
    EasyMock.expect(
            smtpService.applyTemplate("testTemplateInBody_2_body",
                    "This is the media package: ${mediaPackage.identifier}", workflowInstance)).andReturn(
            "This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557");
    EasyMock.expect(smtpService.applyTemplate("template1", null, workflowInstance)).andReturn(
            "This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557");
    EasyMock.expect(smtpService.applyTemplate("templateSyntaxError", null, workflowInstance)).andThrow(
            new RuntimeException());
    EasyMock.expect(smtpService.createMessage()).andReturn(message);
    smtpService.send(message);
    EasyMock.expectLastCall().once();
    EasyMock.replay(smtpService);
    operationHandler.setSmtpService(smtpService);

    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
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
    String[] errorMessages = new String[] { "error in operation1", "error in operation2" };
    workflowInstance.setErrorMessages(errorMessages);
  }

  @Test
  public void testDefaultBody() throws Exception {
    workflowInstance.setTitle("testDefaultBody");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals("this is the subject", message.getSubject());

    InternetAddress[] to = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
    Assert.assertEquals(1, to.length);
    Assert.assertEquals("somebody@hotmail.com", to[0].getAddress());
    Assert.assertEquals("Test Media Package(3e7bb56d-2fcc-4efe-9f0e-d6e56422f557)", message.getContent().toString());
  }

  @Test
  public void testTemplateInBody() throws Exception {
    workflowInstance.setTitle("testTemplateInBody");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_PROPERTY,
            "This is the media package: ${mediaPackage.identifier}");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals("this is the subject", message.getSubject());

    InternetAddress[] to = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
    Assert.assertEquals(1, to.length);
    Assert.assertEquals("somebody@hotmail.com", to[0].getAddress());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", message.getContent()
            .toString());
  }

  @Test
  public void testTemplateInFile() throws Exception {
    workflowInstance.setTitle("testTemplateInFile");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "template1");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals("this is the subject", message.getSubject());

    InternetAddress[] to = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
    Assert.assertEquals(1, to.length);
    Assert.assertEquals("somebody@hotmail.com", to[0].getAddress());
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", message.getContent()
            .toString());
  }

  @Test
  public void testErrorInTemplate() throws Exception {
    workflowInstance.setTitle("testErrorInTemplate");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "templateSyntaxError");

    try {
      operationHandler.start(workflowInstance, null);
    } catch (WorkflowOperationException woe) {
      return;
    }

    Assert.fail("Should have thrown a WorkflowOperationException!");
  }
}
