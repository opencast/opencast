/*
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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.JaxbHostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
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
  private MediaPackageBuilderImpl mpBuilder;

  public TestJobEndpoint() throws Exception {
    mpBuilder = new MediaPackageBuilderImpl();
    this.serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    Job job = new JobImpl(12L);

    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");

    final WorkflowSetImpl workflowSet = new WorkflowSetImpl();

    WorkflowInstance workflowInstance1 = new WorkflowInstance(wfD,
            loadMpFromResource("jobs_mediapackage1"), null, null, new HashMap<String, String>());
    WorkflowInstance workflowInstance2 = new WorkflowInstance(wfD,
            loadMpFromResource("jobs_mediapackage2"), null, null, new HashMap<String, String>());
    WorkflowInstance workflowInstance3 = new WorkflowInstance(wfD,
            loadMpFromResource("jobs_mediapackage3"), null, null, new HashMap<String, String>());

    workflowInstance1.setId(1);
    workflowInstance2.setId(2);
    workflowInstance3.setId(3);

    workflowSet.addItem(workflowInstance1);
    workflowSet.addItem(workflowInstance2);
    workflowSet.addItem(workflowInstance3);

    List<HostRegistration> hosts = new ArrayList<>();
    hosts.add(new JaxbHostRegistration("host1", "1.1.1.1", "node1", 100000, 8, 8, true, false));
    hosts.add(new JaxbHostRegistration("host2", "1.1.1.2", "node2", 400000, 4, 8, true, true));
    hosts.add(new JaxbHostRegistration("host3", "1.1.1.2", "node3", 400000, 4, 8, true, true));

    List<Job> jobs = new ArrayList<>();
    jobs.add(createJob(1, Status.RUNNING, "org.opencastproject.composer", "test",
            "2014-06-05T09:10:00Z", "2014-06-05T09:10:00Z", "testuser1", "host1"));
    jobs.add(createJob(2, Status.RUNNING, WorkflowService.JOB_TYPE, "START_WORKFLOW",
            "2014-06-05T09:16:00Z", "2014-06-05T09:16:00Z", "testuser1", "host3"));
    jobs.add(createJob(3, Status.RUNNING, WorkflowService.JOB_TYPE, "RESUME",
            "2014-06-05T09:11:11Z", "2014-06-05T09:11:11Z", "testuser2", "host3"));
    jobs.add(createJob(4, Status.RUNNING, "org.opencastproject.inspection", "Inspect",
            "2014-06-05T09:16:00Z", "2014-06-05T09:16:00Z", "testuser1", "host2"));
    jobs.add(createJob(5, Status.RUNNING, "org.opencastproject.composer", "Encode",
            "2014-06-05T09:05:00Z", "2014-06-05T09:05:00Z", "testuser3", "host1"));


    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(hosts).anyTimes();
    EasyMock.expect(serviceRegistry.getActiveJobs()).andReturn(jobs).anyTimes();

    EasyMock.replay(serviceRegistry);

    this.setServiceRegistry(serviceRegistry);
    this.activate(null);
  }

  private Job createJob(int id, Status status, String jobType, String operation,
          String created, String started, String creator, String hostname) throws Exception {
    Date createdDate = new Date(DateTimeSupport.fromUTC(created));
    Date startedDate = new Date(DateTimeSupport.fromUTC(started));
    Job job = new JobImpl(id);
    job.setStatus(status);
    job.setJobType(jobType);
    job.setOperation(operation);
    job.setCreator(creator);
    job.setProcessingHost(hostname);
    job.setDateCreated(createdDate);
    job.setDateStarted(startedDate);
    return job;
  }

  private MediaPackage loadMpFromResource(String name) throws Exception {
    URL test = JobEndpointTest.class.getResource("/" + name + ".xml");
    URI publishedMediaPackageURI = test.toURI();
    return mpBuilder.loadFromXml(publishedMediaPackageURI.toURL().openStream());
  }
}
