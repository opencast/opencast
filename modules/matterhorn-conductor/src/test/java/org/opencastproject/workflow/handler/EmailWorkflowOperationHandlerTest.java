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

import org.opencastproject.kernel.mail.EmailTemplateScanner;
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
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
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

    URI episodeURI = EmailWorkflowOperationHandlerTest.class.getResource("/episode_dublincore.xml").toURI();
    URI seriesURI = EmailWorkflowOperationHandlerTest.class.getResource("/series_dublincore.xml").toURI();
    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.get(new URI("episode_dublincore.xml"))).andReturn(new File(episodeURI));
    EasyMock.expect(workspace.get(new URI("series_dublincore.xml"))).andReturn(new File(seriesURI));
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);

    EmailTemplateScanner templateScanner = EasyMock.createMock(EmailTemplateScanner.class);
    EasyMock.expect(templateScanner.getTemplate("template1")).andReturn(
            "This is the media package: ${mediaPackage.identifier}");
    EasyMock.expect(templateScanner.getTemplate("template2")).andReturn(
            "Media package id: ${mediaPackage.identifier}, workflow id: ${workflow.id}, "
                    + "title: ${mediaPackage.title}, series title: ${mediaPackage.seriesTitle}, "
                    + "date: ${mediaPackage.date?datetime?iso_utc}");
    EasyMock.expect(templateScanner.getTemplate("template3"))
            .andReturn(
                    "EPISODE creator: ${catalogs[\"episode\"][\"creator\"]}, isPartOf: ${catalogs[\"episode\"][\"isPartOf\"]}, "
                            + "title: ${catalogs[\"episode\"][\"title\"]}, created: ${catalogs[\"episode\"][\"created\"]}, "
                            + "SERIES creator: ${catalogs[\"series\"][\"creator\"]}, description: ${catalogs[\"series\"][\"description\"]}, "
                            + "subject: ${catalogs[\"series\"][\"subject\"]}");
    EasyMock.expect(templateScanner.getTemplate("templateSyntaxError")).andReturn("${mediaPackage");
    EasyMock.expect(templateScanner.getTemplate("template4")).andReturn(
            "<#if failedOperation?has_content>Workflow failed in operation: ${failedOperation.template}</#if>, "
                    + "Workflow errors: <#list workflow.errorMessages as er>${er} </#list>");
    EasyMock.replay(templateScanner);
    operationHandler.setEmailTemplateScanner(templateScanner);

    workflowInstance = new WorkflowInstanceImpl();
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
  public void testTemplateInFileUsingBasicFields() throws Exception {
    workflowInstance.setTitle("testTemplateInFileUsingBasicFields");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "template2");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals("this is the subject", message.getSubject());

    InternetAddress[] to = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
    Assert.assertEquals(1, to.length);
    Assert.assertEquals("somebody@hotmail.com", to[0].getAddress());
    Assert.assertEquals("Media package id: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557, workflow id: 1, "
            + "title: Test Media Package, series title: Fall 2013 Test, " + "date: 2013-11-19T15:20:00Z", message
            .getContent().toString());
  }

  @Test
  public void testTemplateInFileUsingCatalogFields() throws Exception {
    workflowInstance.setTitle("testTemplateInFileUsingCatalogFields");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "template3");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals("this is the subject", message.getSubject());

    InternetAddress[] to = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
    Assert.assertEquals(1, to.length);
    Assert.assertEquals("somebody@hotmail.com", to[0].getAddress());
    Assert.assertEquals("EPISODE creator: Rute Santos, isPartOf: 20140119997, "
            + "title: Test Media Package, created: 2013-11-19T15:20:00Z, "
            + "SERIES creator: Harvard Extension School, description: http://extension.harvard.edu, "
            + "subject: TEST E-19997", message.getContent().toString());
  }

  @Test
  public void testFailedOperationAndErrors() throws Exception {
    workflowInstance.setTitle("testFailedOperationAndErrors");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "template4");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals("this is the subject", message.getSubject());

    InternetAddress[] to = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
    Assert.assertEquals(1, to.length);
    Assert.assertEquals("somebody@hotmail.com", to[0].getAddress());
    Assert.assertEquals("Workflow failed in operation: operation1, "
            + "Workflow errors: error in operation1 error in operation2 ", message.getContent().toString());
  }

  @Test
  public void testErrorInTemplate() throws Exception {
    workflowInstance.setTitle("testErrorInTemplate");
    operation.setConfiguration(EmailWorkflowOperationHandler.TO_PROPERTY, "somebody@hotmail.com");
    operation.setConfiguration(EmailWorkflowOperationHandler.SUBJECT_PROPERTY, "this is the subject");
    operation.setConfiguration(EmailWorkflowOperationHandler.BODY_TEMPLATE_FILE_PROPERTY, "templateSyntaxError");

    try {
      WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    } catch (WorkflowOperationException woe) {
      return;
    }

    Assert.fail("Should have thrown a WorkflowOperationException!");
  }

}
