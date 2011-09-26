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
package org.opencastproject.workflow.impl;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for the implementation at {@link WorkflowServiceDaoSolrImpl}.
 */
public class WorkflowServiceSolrIndexTest {

  private WorkflowServiceSolrIndex dao = null;

  private AccessControlList acl = new AccessControlList();

  @Before
  public void setUp() throws Exception {
    SecurityService securityService = new SecurityServiceStub();

    // Create a job with a workflow as its payload
    List<Job> jobs = new ArrayList<Job>();
    JaxbJob job = new JaxbJob();
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    workflow.setId(123);
    workflow.setCreator(securityService.getUser());
    workflow.setState(WorkflowState.INSTANTIATED);
    workflow.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    job.setPayload(WorkflowParser.toXml(workflow));
    jobs.add(job);

    // Mock up the service registry to return the job
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.count(WorkflowService.JOB_TYPE, null)).andReturn(new Long(1));
    EasyMock.expect(serviceRegistry.getJobs(WorkflowService.JOB_TYPE, null)).andReturn(jobs);
    EasyMock.expect(serviceRegistry.getJob(123)).andReturn(job);
    EasyMock.replay(serviceRegistry);

    AuthorizationService authzService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authzService.getAccessControlList((MediaPackage) EasyMock.anyObject())).andReturn(acl).anyTimes();
    EasyMock.replay(authzService);

    // Now create the dao
    dao = new WorkflowServiceSolrIndex();
    dao.solrRoot = PathSupport.concat("target", Long.toString(System.currentTimeMillis()));
    dao.setServiceRegistry(serviceRegistry);
    dao.setAuthorizationService(authzService);
    dao.setSecurityService(securityService);
    dao.activate();
  }

  @After
  public void tearDown() throws Exception {
    dao.deactivate();
    FileUtils.deleteDirectory(new File(dao.solrRoot));
    dao = null;
  }

  /**
   * Tests whether a simple query is built correctly
   */
  @Test
  public void testBuildSimpleQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withMediaPackage("123").withSeriesId("series1");
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "oc_org:mh_default_org AND mediapackageid:123 AND seriesid:series1 AND oc_org:"
            + DEFAULT_ORG_ADMIN.getOrganization() + " AND (oc_creator:" + DEFAULT_ORG_ADMIN.getUserName()
            + " OR oc_acl_read:ROLE_ADMIN)";
    assertEquals(expected, solrQuery);
  }

  /**
   * Tests whether the query is built properly, using OR rather than AND, when supplying multiple inclusive states
   */
  @Test
  public void testBuildMultiStateQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withState(WorkflowState.RUNNING)
            .withState(WorkflowState.PAUSED);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "oc_org:mh_default_org AND seriesid:series1 AND (state:running OR state:paused) AND oc_org:"
            + DEFAULT_ORG_ADMIN.getOrganization() + " AND (oc_creator:" + DEFAULT_ORG_ADMIN.getUserName()
            + " OR oc_acl_read:ROLE_ADMIN)";
    assertEquals(expected, solrQuery);
  }

  /**
   * Tests whether the query is built using AND rather than OR when supplying multiple excluded states
   */
  @Test
  public void testBuildNegativeStatesQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withoutState(WorkflowState.RUNNING)
            .withoutState(WorkflowState.PAUSED);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "oc_org:mh_default_org AND seriesid:series1 AND (-state:running AND -state:paused AND *:*)"
            + " AND oc_org:" + DEFAULT_ORG_ADMIN.getOrganization() + " AND (oc_creator:"
            + DEFAULT_ORG_ADMIN.getUserName() + " OR oc_acl_read:ROLE_ADMIN)";
    assertEquals(expected, solrQuery);
  }

  /**
   * Tests whether the query is built using *:* when supplying a single excluded state
   */
  @Test
  public void testBuildNegativeStateQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withoutState(WorkflowState.RUNNING);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "oc_org:mh_default_org AND seriesid:series1 AND (-state:running AND *:*) AND oc_org:"
            + DEFAULT_ORG_ADMIN.getOrganization() + " AND (oc_creator:" + DEFAULT_ORG_ADMIN.getUserName()
            + " OR oc_acl_read:ROLE_ADMIN)";
    assertEquals(expected, solrQuery);
  }

}
