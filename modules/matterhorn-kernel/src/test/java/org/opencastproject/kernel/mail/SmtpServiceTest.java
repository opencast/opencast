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
package org.opencastproject.kernel.mail;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmtpServiceTest {
  private SmtpService smtpService;
  private WorkflowInstanceImpl workflowInstance;
  private MediaPackage mp;
  private URI uriMP;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = SmtpServiceTest.class.getResource("/email_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    smtpService = new SmtpService();

    URI episodeURI = SmtpServiceTest.class.getResource("/episode_dublincore.xml").toURI();
    URI seriesURI = SmtpServiceTest.class.getResource("/series_dublincore.xml").toURI();
    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.get(new URI("episode_dublincore.xml"))).andReturn(new File(episodeURI));
    EasyMock.expect(workspace.get(new URI("series_dublincore.xml"))).andReturn(new File(seriesURI));
    EasyMock.replay(workspace);
    smtpService.setWorkspace(workspace);

    EmailTemplateScanner templateScanner = EasyMock.createMock(EmailTemplateScanner.class);
    EasyMock.expect(templateScanner.getTemplate("templateBasic")).andReturn(
            "Media package id: ${mediaPackage.identifier}, workflow id: ${workflow.id}, "
                    + "title: ${mediaPackage.title}, series title: ${mediaPackage.seriesTitle}, "
                    + "date: ${mediaPackage.date?datetime?iso_utc}");
    EasyMock.expect(templateScanner.getTemplate("templateCatalog"))
            .andReturn(
                    "EPISODE creator: ${catalogs[\"episode\"][\"creator\"]}, isPartOf: ${catalogs[\"episode\"][\"isPartOf\"]}, "
                            + "title: ${catalogs[\"episode\"][\"title\"]}, created: ${catalogs[\"episode\"][\"created\"]}, "
                            + "SERIES creator: ${catalogs[\"series\"][\"creator\"]}, description: ${catalogs[\"series\"][\"description\"]}, "
                            + "subject: ${catalogs[\"series\"][\"subject\"]}");
    EasyMock.expect(templateScanner.getTemplate("templateFailed")).andReturn(
            "<#if failedOperation?has_content>Workflow failed in operation: ${failedOperation.template}</#if>, "
                    + "Workflow errors: <#list workflow.errorMessages as er>${er} </#list>");
    EasyMock.expect(templateScanner.getTemplate("templateSyntaxError")).andReturn("${mediaPackage");
    EasyMock.expect(templateScanner.getTemplate("templateNotFound")).andReturn(null);
    EasyMock.replay(templateScanner);
    smtpService.bindEmailTemplateScanner(templateScanner);

    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("wfdef");
    Map<String, String> props = new HashMap<String, String>();
    props.put("emailAddress", "user@domain.com");
    workflowInstance = new WorkflowInstanceImpl(def, null, null, null, null, props);
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl failedOperation1 = new WorkflowOperationInstanceImpl("operation1",
            OperationState.FAILED);
    failedOperation1.setFailWorkflowOnException(true);
    WorkflowOperationInstanceImpl failedOperation2 = new WorkflowOperationInstanceImpl("operation2",
            OperationState.FAILED);
    failedOperation2.setFailWorkflowOnException(false);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("email", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationList = new ArrayList<WorkflowOperationInstance>();
    operationList.add(failedOperation1);
    operationList.add(failedOperation2);
    operationList.add(operation);
    workflowInstance.setOperations(operationList);
    String[] errorMessages = new String[] { "error in operation1", "error in operation2" };
    workflowInstance.setErrorMessages(errorMessages);
  }

  @Test
  public void testTemplateContent() throws Exception {
    String templateName = "template";
    String templateContent = "This is the media package: ${mediaPackage.identifier}";

    String result = smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", result);
  }

  @Test
  public void testTemplateUsingBasicFields() throws Exception {
    String templateName = "templateBasic";
    String templateContent = null;

    String result = smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("Media package id: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557, workflow id: 1, "
            + "title: Test Media Package, series title: Fall 2013 Test, " + "date: 2013-11-19T15:20:00Z", result);
  }

  @Test
  public void testTemplateUsingCatalogFields() throws Exception {
    String templateName = "templateCatalog";
    String templateContent = null;

    String result = smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("EPISODE creator: Rute Santos, isPartOf: 20140119997, "
            + "title: Test Media Package, created: 2013-11-19T15:20:00Z, "
            + "SERIES creator: Harvard Extension School, description: http://extension.harvard.edu, "
            + "subject: TEST E-19997", result);
  }

  @Test
  public void testWorkflowConfiguration() throws Exception {
    String templateName = "templateConfig";
    String templateContent = "This is an email address: ${workflowConfig['emailAddress']}";

    String result = smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("This is an email address: user@domain.com", result);
  }

  @Test
  public void testFailedOperationAndErrors() throws Exception {
    String templateName = "templateFailed";
    String templateContent = null;

    String result = smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("Workflow failed in operation: operation1, "
            + "Workflow errors: error in operation1 error in operation2 ", result);
  }

  @Test
  public void testErrorInTemplate() throws Exception {
    String templateName = "templateSyntaxError";
    String templateContent = null;

    try {
      smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    } catch (Exception pe) {
      return;
    }

    Assert.fail("Should have thrown an exception!");
  }

  @Test
  public void testTemplateNotFound() throws Exception {
    String templateName = "templateNotFound";
    String templateContent = null;

    try {
      smtpService.applyTemplate(templateName, templateContent, workflowInstance);
    } catch (IllegalArgumentException e) {
      return;
    }

    Assert.fail("Should have thrown an IllegalArgumentException exception!");
  }

}
