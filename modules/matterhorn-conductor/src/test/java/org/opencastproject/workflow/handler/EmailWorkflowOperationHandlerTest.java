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
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import junit.framework.Assert;

import org.easymock.classextension.EasyMock;
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
  private WorkflowInstanceImpl workflowInstance;
  private MediaPackage mp;
  private URI uriMP;
  private MimeMessage message;

  @Before
  public void setUp() throws Exception {
    System.out.println("setUp()");
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = EmailWorkflowOperationHandlerTest.class.getResource("/email_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    operationHandler = new EmailWorkflowOperationHandler();

    Properties empty = new Properties();
    message = new MimeMessage(Session.getInstance(empty));

    SmtpService smtpService = EasyMock.createMock(SmtpService.class);
    EasyMock.expect(smtpService.createMessage()).andReturn(message);
    smtpService.send(message);
    EasyMock.expectLastCall().once();
    EasyMock.replay(smtpService);
    operationHandler.setSmtpService(smtpService);

    workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    operation = new WorkflowOperationInstanceImpl("email", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);
  }

  @Test
  public void testDefaultBody() throws Exception {
    System.out.println("testDefaultBody");
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
    System.out.println("testTemplateInBody");
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

}
