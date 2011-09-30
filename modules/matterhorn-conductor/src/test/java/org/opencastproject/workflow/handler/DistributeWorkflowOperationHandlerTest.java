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

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ANONYMOUS;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistributeWorkflowOperationHandlerTest {
  private DistributeWorkflowOperationHandler operationHandler;
  private ServiceRegistry serviceRegistry;
  private TestDistributionService service = null;

  private URI uriMP;
  private MediaPackage mp;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    uriMP = InspectWorkflowOperationHandler.class.getResource("/distribute_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    service = new TestDistributionService();

    User anonymous = new User("anonymous", DEFAULT_ORGANIZATION_ID, new String[] { DEFAULT_ORGANIZATION_ANONYMOUS });
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService);

    // set up the handler
    operationHandler = new DistributeWorkflowOperationHandler();
    operationHandler.setDistributionService(service);
    operationHandler.setServiceRegistry(serviceRegistry);

  }

  @Test
  public void testDistribute() throws Exception {
    String sourceTags = "engage,atom,rss";
    String targetTags = "engage,publish";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 3, result.getMediaPackage()
            .getTracks().length);
  }

  private WorkflowInstance getWorkflowInstance(String sourceTags, String targetTags) {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);

    operation.setConfiguration("source-tags", sourceTags);
    operation.setConfiguration("target-tags", targetTags);

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    return workflowInstance;
  }

  class TestDistributionService implements DistributionService, JobProducer {
    public static final String JOB_TYPE = "distribute";

    @Override
    public Job distribute(MediaPackage mediapackage, String elementId) throws DistributionException,
            MediaPackageException {
      try {
        return serviceRegistry.createJob(JOB_TYPE, "distribute",
                Arrays.asList(new String[] { MediaPackageParser.getAsXml(mediapackage), elementId }));
      } catch (ServiceRegistryException e) {
        throw new DistributionException(e);
      }
    }

    @Override
    public Job retract(MediaPackage mediapackage, String elementId) throws DistributionException {
      try {
        return serviceRegistry.createJob(JOB_TYPE, "retract",
                Arrays.asList(new String[] { MediaPackageParser.getAsXml(mediapackage), elementId }));
      } catch (ServiceRegistryException e) {
        throw new DistributionException(e);
      }
    }

    @Override
    public String getJobType() {
      return JOB_TYPE;
    }

    @Override
    public long countJobs(Status status) throws ServiceRegistryException {
      return serviceRegistry.getJobs(JOB_TYPE, status).size();
    }

    @Override
    public boolean acceptJob(Job job) throws ServiceRegistryException {
      MediaPackage mp = null;
      MediaPackageElement element = null;
      try {
        mp = MediaPackageParser.getFromXml(job.getArguments().get(0));
        String elementId = job.getArguments().get(1);
        element = mp.getElementById(elementId);
        job.setPayload(MediaPackageElementParser.getAsXml(element));
      } catch (MediaPackageException e1) {
        throw new ServiceRegistryException("Error serializing or deserializing");
      }
      job.setStatus(Status.FINISHED);
      try {
        serviceRegistry.updateJob(job);
        return true;
      } catch (NotFoundException e) {
        // not possible
      }
      return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.opencastproject.job.api.JobProducer#isReadyToAccept(org.opencastproject.job.api.Job)
     */
    @Override
    public boolean isReadyToAccept(Job job) throws ServiceRegistryException {
      return true;
    }

  }

}
