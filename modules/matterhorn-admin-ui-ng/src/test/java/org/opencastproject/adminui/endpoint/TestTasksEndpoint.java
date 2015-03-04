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
package org.opencastproject.adminui.endpoint;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestTasksEndpoint extends TasksEndpoint {

  private WorkflowService workflowService;

  public TestTasksEndpoint() throws Exception {
    this.workflowService = EasyMock.createNiceMock(WorkflowService.class);

    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");
    wfD.addTag("archive");

    WorkflowDefinitionImpl wfD2 = new WorkflowDefinitionImpl();
    wfD2.setTitle("Full HTML5");
    wfD2.setId("full-html5");
    wfD2.setDescription("Test description");
    wfD2.setConfigurationPanel("<h2>Test</h2>");
    wfD2.addTag("archive");

    WorkflowDefinitionImpl wfD3 = new WorkflowDefinitionImpl();
    wfD3.setTitle("Hidden");
    wfD3.setId("hidden");

    WorkflowInstanceImpl wI1 = new WorkflowInstanceImpl();
    wI1.setTitle(wfD.getTitle());
    wI1.setTemplate(wfD.getId());
    wI1.setId(5);
    WorkflowInstanceImpl wI2 = new WorkflowInstanceImpl();
    wI2.setTitle(wfD2.getTitle());
    wI2.setTemplate(wfD2.getId());
    wI2.setId(10);

    EasyMock.expect(workflowService.listAvailableWorkflowDefinitions()).andReturn(Arrays.asList(wfD, wfD2, wfD3))
            .anyTimes();
    EasyMock.expect(workflowService.getWorkflowDefinitionById(EasyMock.anyString())).andReturn(wfD).anyTimes();
    EasyMock.replay(workflowService);

    List<WorkflowInstance> instanceList = new ArrayList<WorkflowInstance>();
    instanceList.add(wI1);
    instanceList.add(wI2);

    Archive<?> archive = EasyMock.createNiceMock(Archive.class);
    EasyMock.expect(
            archive.applyWorkflow(EasyMock.anyObject(ConfiguredWorkflow.class), EasyMock.anyObject(UriRewriter.class),
                    EasyMock.anyObject(List.class))).andReturn(instanceList);
    EasyMock.replay(archive);

    HttpMediaPackageElementProvider httpMediaPackageElementProvider = EasyMock
            .createNiceMock(HttpMediaPackageElementProvider.class);
    EasyMock.replay(httpMediaPackageElementProvider);

    this.setWorkflowService(workflowService);
    this.setArchive(archive);
    this.setHttpMediaPackageElementProvider(httpMediaPackageElementProvider);
    this.activate(null);
  }

}
