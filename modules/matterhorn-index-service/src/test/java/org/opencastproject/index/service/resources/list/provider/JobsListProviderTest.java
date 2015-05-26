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

import static org.opencastproject.index.service.resources.list.provider.JobsListProvider.JobFilter.CONTRIBUTOR;
import static org.opencastproject.index.service.resources.list.provider.JobsListProvider.JobFilter.SERIES;
import static org.opencastproject.index.service.resources.list.provider.JobsListProvider.JobFilter.STATUS;
import static org.opencastproject.index.service.resources.list.provider.JobsListProvider.JobFilter.TITLE;
import static org.opencastproject.index.service.resources.list.provider.JobsListProvider.JobFilter.WORKFLOW;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.JobsListQueryImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workflow.api.WorkflowStateException;
import org.opencastproject.workflow.api.WorkflowStatistics;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobsListProviderTest {

  private JobsListProvider jobsListProvider;
  private Map<String, Object> resultList;
  private MediaPackageBuilder mpBuilder;
  private WorkflowService workflowService;
  private WorkflowSetImpl workflowSet;
  private Function<WorkflowQuery, Boolean> testWorkflowQuery;

  @Before
  public void setUp() throws Exception {
    jobsListProvider = new JobsListProvider();

    workflowSet = new WorkflowSetImpl();

    mpBuilder = new MediaPackageBuilderImpl();

    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");

    WorkflowInstanceImpl workflowInstanceImpl1 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage1"), 2L, null, null, new HashMap<String, String>());
    WorkflowInstanceImpl workflowInstanceImpl2 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage2"), 2L, null, null, new HashMap<String, String>());
    WorkflowInstanceImpl workflowInstanceImpl3 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage3"), 2L, null, null, new HashMap<String, String>());

    workflowInstanceImpl1.setState(WorkflowState.PAUSED);
    workflowInstanceImpl2.setState(WorkflowState.PAUSED);
    workflowInstanceImpl3.setState(WorkflowState.INSTANTIATED);

    workflowSet.addItem(workflowInstanceImpl1);
    workflowSet.addItem(workflowInstanceImpl2);
    workflowSet.addItem(workflowInstanceImpl3);

    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.getWorkflowInstances(EasyMock.anyObject(WorkflowQuery.class)))
            .andReturn(workflowSet).anyTimes();

    jobsListProvider.setWorkflowService(workflowService);
    jobsListProvider.activate(null);

    EasyMock.replay(workflowService);
  }

  @Test
  public void testListNames() throws ListProviderException {
    ResourceListQuery query = new JobsListQueryImpl();

    org.junit.Assert.assertEquals(3,
            jobsListProvider.getList(JobsListProvider.getListNameFromFilter(TITLE), query, null).size());

    org.junit.Assert.assertEquals(3, jobsListProvider.getList("jobs", query, null).size());
    org.junit.Assert.assertEquals(3, jobsListProvider.getList("non-existing-name", query, null).size());

    org.junit.Assert.assertEquals(2,
            jobsListProvider.getList(JobsListProvider.getListNameFromFilter(CONTRIBUTOR), query, null).size());

    org.junit.Assert.assertEquals(2,
            jobsListProvider.getList(JobsListProvider.getListNameFromFilter(STATUS), query, null).size());
    org.junit.Assert.assertEquals(1,
            jobsListProvider.getList(JobsListProvider.getListNameFromFilter(SERIES), query, null).size());
    org.junit.Assert.assertEquals(1,
            jobsListProvider.getList(JobsListProvider.getListNameFromFilter(WORKFLOW), query, null).size());
  }

  @Test
  public void testQueries() throws ListProviderException, WorkflowDatabaseException {
    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.getWorkflowInstances(EasyMock.anyObject(WorkflowQuery.class)))
            .andDelegateTo(getWorkflowServiceStub()).anyTimes();

    jobsListProvider.setWorkflowService(workflowService);
    jobsListProvider.activate(null);

    EasyMock.replay(workflowService);

    final JobsListQueryImpl query = new JobsListQueryImpl();
    query.setLimit(1);
    query.setOffset(1);
    query.withStatus(WorkflowState.FAILED);
    query.withText("test");

    testWorkflowQuery = new Function<WorkflowQuery, Boolean>() {
      @Override
      public Boolean apply(WorkflowQuery q) {
        Assert.assertEquals((long) (query.getLimit().get()), q.getCount());
        Assert.assertEquals((long) (query.getOffset().get()), q.getStartPage());
        return null;
      }
    };
    jobsListProvider.getList(JobsListProvider.getListNameFromFilter(TITLE), query, null).size();
  }

  private MediaPackage loadMpFromResource(String name) throws Exception {
    URL url = JobsListProviderTest.class.getResource("/" + name + ".xml");
    return mpBuilder.loadFromXml(url.openStream());
  }

  private WorkflowService getWorkflowServiceStub() {
    return new WorkflowService() {
      @Override
      public void addWorkflowListener(WorkflowListener listener) {
      }

      @Override
      public void removeWorkflowListener(WorkflowListener listener) {
      }

      @Override
      public void registerWorkflowDefinition(WorkflowDefinition workflow) throws WorkflowDatabaseException {
      }

      @Override
      public void unregisterWorkflowDefinition(String workflowDefinitionId) throws NotFoundException,
              WorkflowDatabaseException {
      }

      @Override
      public WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException,
              NotFoundException {
        return null;
      }

      @Override
      public WorkflowInstance getWorkflowById(long workflowId) throws WorkflowDatabaseException, NotFoundException,
              UnauthorizedException {
        return null;
      }

      @Override
      public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
        testWorkflowQuery.apply(query);
        return workflowSet;
      }

      @Override
      public WorkflowSet getWorkflowInstancesForAdministrativeRead(WorkflowQuery q) throws WorkflowDatabaseException,
              UnauthorizedException {
        return null;
      }

      @Override
      public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
              Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException {
        return null;
      }

      @Override
      public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
              Long parentWorkflowId, Map<String, String> properties) throws WorkflowDatabaseException,
              WorkflowParsingException, NotFoundException {
        return null;
      }

      @Override
      public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
              throws WorkflowDatabaseException, WorkflowParsingException {
        return null;
      }

      @Override
      public long countWorkflowInstances() throws WorkflowDatabaseException {
        return 0;
      }

      @Override
      public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
        return 0;
      }

      @Override
      public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
        return null;
      }

      @Override
      public WorkflowInstance stop(long workflowInstanceId) throws WorkflowException, NotFoundException,
              UnauthorizedException {
        return null;
      }

      @Override
      public void remove(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException,
              NotFoundException, UnauthorizedException, WorkflowStateException {
      }

      @Override
      public WorkflowInstance suspend(long workflowInstanceId) throws WorkflowException, NotFoundException,
              UnauthorizedException {
        return null;
      }

      @Override
      public WorkflowInstance resume(long workflowInstanceId) throws NotFoundException, WorkflowException,
              UnauthorizedException {
        return null;
      }

      @Override
      public WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties) throws NotFoundException,
              WorkflowException, IllegalStateException, UnauthorizedException {
        return null;
      }

      @Override
      public void update(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException {
      }

      @Override
      public List<WorkflowDefinition> listAvailableWorkflowDefinitions() throws WorkflowDatabaseException {
        return null;
      }

      @Override
      public void cleanupWorkflowInstances(int lifetime, WorkflowState state) throws WorkflowDatabaseException,
              UnauthorizedException {

      }

      @Override
      public void moveMissingCapturesFromUpcomingToFailedStatus(long buffer) throws WorkflowDatabaseException {
      }

      @Override
      public void moveMissingIngestsFromUpcomingToFailedStatus(long buffer) throws WorkflowDatabaseException {
      }
    };
  }

}
