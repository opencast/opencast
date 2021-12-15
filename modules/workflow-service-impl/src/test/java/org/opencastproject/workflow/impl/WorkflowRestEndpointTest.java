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


package org.opencastproject.workflow.impl;

import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.JaxbWorkflowInstance;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.endpoint.WorkflowRestService;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class WorkflowRestEndpointTest {

  private WorkflowRestService restService;
  private WorkflowInstance workflow;

  @Before
  public void setUp() throws Exception {
    // Create a workflow for the service to return
    workflow = new WorkflowInstance();
    workflow.setTitle("a workflow instance");
    workflow.setId(1);

    // Mock up the behavior of the workflow service
    WorkflowService service = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(service.listAvailableWorkflowDefinitions()).andReturn(new ArrayList<WorkflowDefinition>());
    EasyMock.expect(service.getWorkflowById(EasyMock.anyLong())).andThrow(new NotFoundException()).times(2)
            .andReturn(workflow);
    EasyMock.replay(service);

    // Set up the rest endpoint
    restService = new WorkflowRestService();
    restService.setService(service);
    restService.activate(null);
  }

  @Test
  public void testGetWorkflowInstance() throws Exception {
    try {
      restService.getWorkflowAsJson(-1);
      Assert.fail("This should have thrown a not found exception");
    } catch (NotFoundException e) {
      // expected
    }

    try {
      restService.getWorkflowAsXml(-1);
      Assert.fail("This should have thrown a not found exception");
    } catch (NotFoundException e) {
      // expected
    }

    JaxbWorkflowInstance xmlResponse = restService.getWorkflowAsXml(1);
    Assert.assertNotNull(xmlResponse);
  }
}
