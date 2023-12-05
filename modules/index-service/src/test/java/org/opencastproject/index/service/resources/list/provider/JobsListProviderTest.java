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

package org.opencastproject.index.service.resources.list.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.index.service.resources.list.query.JobsListQuery;
import org.opencastproject.job.api.Job;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class JobsListProviderTest {

  private JobsListProvider jobsListProvider;
  private WorkflowService workflowService;

  private List<WorkflowDefinition> workflowDefinitions;

  @Before
  public void setUp() throws Exception {
    jobsListProvider = new JobsListProvider();
    workflowDefinitions = new ArrayList<WorkflowDefinition>();

    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");
    workflowDefinitions.add(wfD);
    wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Quick");
    wfD.setId("quick");
    workflowDefinitions.add(wfD);

    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.listAvailableWorkflowDefinitions())
            .andReturn(workflowDefinitions).anyTimes();

    jobsListProvider.setWorkflowService(workflowService);
    jobsListProvider.activate(null);

    EasyMock.replay(workflowService);
  }

  @Test
  public void testStatusListName() throws ListProviderException, WorkflowDatabaseException {
    ResourceListQuery query = new JobsListQuery();

    assertEquals(4, jobsListProvider.getList(JobsListProvider.LIST_STATUS, query).size());
    for (Entry<String, String> entry : jobsListProvider.getList(JobsListProvider.LIST_STATUS, query).entrySet()) {
      try {
        Job.Status.valueOf(entry.getKey());
      } catch (IllegalArgumentException ex) {
        fail("Can not parse job state");
      }

      assertTrue(StringUtils.startsWith(entry.getValue(), JobsListProvider.JOB_STATUS_FILTER_PREFIX));
    }
  }

  @Test
  public void testWorkflowListName() throws ListProviderException, WorkflowDatabaseException {
    ResourceListQuery query = new JobsListQuery();

    assertEquals(workflowDefinitions.size(),
            jobsListProvider.getList(JobsListProvider.LIST_WORKFLOW, query).size());

    for (Entry<String, String> entry : jobsListProvider.getList(
            JobsListProvider.LIST_WORKFLOW, query).entrySet()) {

      boolean match = false;
      for (WorkflowDefinition wfD : workflowDefinitions) {
        if (StringUtils.equals(wfD.getId(), entry.getKey())
                && StringUtils.equals(wfD.getTitle(), entry.getValue())) {
          match = true;
          break;
        }
      }
      assertTrue(match);
    }
  }
}
