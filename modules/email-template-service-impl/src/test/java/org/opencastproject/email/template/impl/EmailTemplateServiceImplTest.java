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
package org.opencastproject.email.template.impl;

import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EmailTemplateServiceImplTest {

  private EmailTemplateServiceImpl service;
  private WorkflowInstanceImpl workflowInstance;
  private MediaPackage mp;
  private URI uriMP;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = EmailTemplateServiceImplTest.class.getResource("/email_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    service = new EmailTemplateServiceImpl();

    URI episodeURI = EmailTemplateServiceImplTest.class.getResource("/episode_dublincore.xml").toURI();
    URI seriesURI = EmailTemplateServiceImplTest.class.getResource("/series_dublincore.xml").toURI();
    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.read(new URI("episode_dublincore.xml")))
        .andReturn(new FileInputStream(new File(episodeURI)));
    EasyMock.expect(workspace.read(new URI("series_dublincore.xml")))
        .andReturn(new FileInputStream(new File(seriesURI)));
    EasyMock.replay(workspace);
    service.setWorkspace(workspace);

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
                    + "Workflow errors: <#list incident as inc><#list inc.details as de>${de.b} </#list></#list>");
    EasyMock.expect(templateScanner.getTemplate("templateSyntaxError")).andReturn("${mediaPackage");
    EasyMock.replay(templateScanner);
    service.setEmailTemplateScanner(templateScanner);

    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("wfdef");
    Map<String, String> props = new HashMap<String, String>();
    props.put("emailAddress", "user@domain.com");

    IncidentService is = EasyMock.createMock(IncidentService.class);

    /*
     * This is what we're building. EasyMock makes it ugly, of course /-------job1Tree / | / incident2 subtree |
     * incident1
     */

    // Create the tree and subtree
    IncidentTree subtree = EasyMock.createNiceMock(IncidentTree.class);
    IncidentTree job1Tree = EasyMock.createNiceMock(IncidentTree.class);

    // Create the incidents
    Incident incident1 = EasyMock.createNiceMock(Incident.class);
    List<Tuple<String, String>> details = new LinkedList<Tuple<String, String>>();
    Tuple<String, String> detail = new Tuple<String, String>("detail-type", "error in operation1");
    details.add(detail);
    EasyMock.expect(incident1.getDetails()).andReturn(details);

    Incident incident2 = EasyMock.createNiceMock(Incident.class);
    details = new LinkedList<Tuple<String, String>>();
    detail = new Tuple<String, String>("detail-type", "error in operation2");
    details.add(detail);
    EasyMock.expect(incident2.getDetails()).andReturn(details);

    // Link the incident and the subtree
    LinkedList<Incident> incidents = new LinkedList<Incident>();
    incidents.add(incident1);
    EasyMock.expect(subtree.getIncidents()).andReturn(incidents).anyTimes();
    // This is what Entwine wrote in the IncidentTree class, so rather than null we're using Immutables.nil()
    List<IncidentTree> subtreeDecendants = Collections.emptyList();
    EasyMock.expect(subtree.getDescendants()).andReturn(subtreeDecendants).anyTimes();

    // Link the incident and the parent tree
    incidents = new LinkedList<Incident>();
    incidents.add(incident2);
    EasyMock.expect(job1Tree.getIncidents()).andReturn(incidents).anyTimes();

    // Link the subtree and parent tree
    LinkedList<IncidentTree> subtreeList = new LinkedList<IncidentTree>();
    subtreeList.add(subtree);
    EasyMock.expect(job1Tree.getDescendants()).andReturn(subtreeList).anyTimes();

    service.setIncidentService(is);

    workflowInstance = new WorkflowInstanceImpl(def, null, null, null, null, props);
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);

    WorkflowOperationInstanceImpl failedOperation1 = new WorkflowOperationInstanceImpl("operation1",
            OperationState.FAILED);
    failedOperation1.setFailWorkflowOnException(true);
    failedOperation1.setId(1L);
    EasyMock.expect(is.getIncidentsOfJob(1L, true)).andReturn(subtree).anyTimes();
    WorkflowOperationInstanceImpl failedOperation2 = new WorkflowOperationInstanceImpl("operation2",
            OperationState.FAILED);
    failedOperation2.setFailWorkflowOnException(false);
    failedOperation1.setId(2L);
    EasyMock.expect(is.getIncidentsOfJob(2L, true)).andReturn(job1Tree).anyTimes();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("email", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationList = new ArrayList<WorkflowOperationInstance>();
    operationList.add(failedOperation1);
    operationList.add(failedOperation2);
    operationList.add(operation);
    workflowInstance.setOperations(operationList);

    EasyMock.replay(is, subtree, job1Tree, incident1, incident2);
  }

  @Test
  public void testTemplateContent() throws Exception {
    String templateName = "template";
    String templateContent = "This is the media package: ${mediaPackage.identifier}";

    String result = service.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", result);
  }

  @Test
  public void testTemplateUsingBasicFields() throws Exception {
    String templateName = "templateBasic";
    String templateContent = null;

    String result = service.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("Media package id: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557, workflow id: 1, "
            + "title: Test Media Package, series title: Fall 2013 Test, " + "date: 2013-11-19T15:20:00Z", result);
  }

  @Test
  public void testTemplateUsingCatalogFields() throws Exception {
    String templateName = "templateCatalog";
    String templateContent = null;

    String result = service.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("EPISODE creator: Rute Santos, isPartOf: 20140119997, "
            + "title: Test Media Package, created: 2013-11-19T15:20:00Z, "
            + "SERIES creator: Harvard Extension School, description: http://extension.harvard.edu, "
            + "subject: TEST E-19997", result);
  }

  @Test
  public void testWorkflowConfiguration() throws Exception {
    String templateName = "templateConfig";
    String templateContent = "This is an email address: ${workflowConfig['emailAddress']}";

    String result = service.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("This is an email address: user@domain.com", result);
  }

  @Test
  public void testFailedOperationAndErrors() throws Exception {
    String templateName = "templateFailed";
    String templateContent = null;

    String result = service.applyTemplate(templateName, templateContent, workflowInstance);
    Assert.assertEquals("Workflow failed in operation: operation1, "
            + "Workflow errors: error in operation1 error in operation2 ", result);
  }

  @Test(expected = Exception.class)
  public void testErrorInTemplate() throws Exception {
    String templateName = "templateSyntaxError";
    String templateContent = null;

    service.applyTemplate(templateName, templateContent, workflowInstance);
  }
}
