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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSetImpl;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestJobEndpoint extends JobEndpoint {

  private ServiceRegistry serviceRegistry;
  private WorkflowService workflowService;
  private MediaPackageBuilderImpl mpBuilder;

  public TestJobEndpoint() throws Exception {
    mpBuilder = new MediaPackageBuilderImpl();
    this.serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    this.workflowService = EasyMock.createNiceMock(WorkflowService.class);
    JaxbJob job = new JaxbJob(12L);

    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");

    WorkflowSetImpl workflowSet = new WorkflowSetImpl();

    WorkflowInstanceImpl workflowInstanceImpl1 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage1"), 2L, null, null, new HashMap<String, String>());
    WorkflowInstanceImpl workflowInstanceImpl2 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage2"), 2L, null, null, new HashMap<String, String>());
    WorkflowInstanceImpl workflowInstanceImpl3 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage3"), 2L, null, null, new HashMap<String, String>());

    workflowInstanceImpl1.setId(1);
    workflowInstanceImpl2.setId(2);
    workflowInstanceImpl3.setId(3);

    workflowSet.addItem(workflowInstanceImpl1);
    workflowSet.addItem(workflowInstanceImpl2);
    workflowSet.addItem(workflowInstanceImpl3);

    workflowSet.setTotalCount(3);

    List<Job> jobs = new ArrayList<Job>();
    jobs.add(createJob(1, Status.RUNNING, ComposerService.JOB_TYPE, "test"));
    jobs.add(createJob(2, Status.RUNNING, WorkflowService.JOB_TYPE, "START_WORKFLOW"));
    jobs.add(createJob(3, Status.RUNNING, WorkflowService.JOB_TYPE, "RESUME"));
    jobs.add(createJob(4, Status.RUNNING, MediaInspectionService.JOB_TYPE, "Inspect"));

    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.expect(workflowService.getWorkflowInstances(EasyMock.anyObject(WorkflowQuery.class)))
            .andReturn(workflowSet).anyTimes();
    EasyMock.expect(workflowService.countWorkflowInstances()).andReturn(workflowSet.size()).anyTimes();
    EasyMock.expect(serviceRegistry.getJobs(EasyMock.anyString(), EasyMock.anyObject(Status.class))).andReturn(jobs)
            .anyTimes();

    EasyMock.replay(workflowService);
    EasyMock.replay(serviceRegistry);

    this.setServiceRegistry(serviceRegistry);
    this.setWorkflowService(workflowService);
    this.activate(null);
  }

  private Job createJob(int id, Status status, String jobType, String operation) throws Exception {
    Date date = new Date(DateTimeSupport.fromUTC("2014-06-05T09:15:56Z"));
    JaxbJob job = new JaxbJob();
    job.setId(id);
    job.setStatus(status);
    job.setJobType(jobType);
    job.setOperation(operation);
    job.setCreator("testuser");
    job.setProcessingHost("host");
    job.setDateCreated(date);
    job.setDateStarted(date);
    return job;
  }

  private MediaPackage loadMpFromResource(String name) throws Exception {
    URL test = JobEndpointTest.class.getResource("/" + name + ".xml");
    URI publishedMediaPackageURI = test.toURI();
    return mpBuilder.loadFromXml(publishedMediaPackageURI.toURL().openStream());
  }
}
