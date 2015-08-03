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

package org.opencastproject.workflow.handler.distribution;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler;

@Ignore
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

    DefaultOrganization defaultOrganization = new DefaultOrganization();
    User anonymous = new JaxbUser("anonymous", "test", defaultOrganization, new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, defaultOrganization));
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
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(
            authorizationService.getAclAttachments((MediaPackage) EasyMock.anyObject(),
                    (Option<AclScope>) EasyMock.anyObject())).andReturn(new ArrayList<Attachment>()).anyTimes();
    EasyMock.replay(authorizationService);

    // set up the handler
    operationHandler = new DistributeWorkflowOperationHandler();
    operationHandler.setDistributionService(service);
    operationHandler.setServiceRegistry(serviceRegistry);
    operationHandler.setAuthorizationService(authorizationService);
  }

  @Test
  public void testEmptyDistribute() throws Exception {
    String sourceTags = "bla";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, null, null, null);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 2, result.getMediaPackage()
            .getTracks().length);
  }

  @Test
  public void testDistributeWithTagsOnly() throws Exception {
    String sourceTags = "engage,atom,rss";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, null, null, null);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 3, result.getMediaPackage()
            .getTracks().length);
  }

  @Test
  public void testDistributeWithFlavorOnly() throws Exception {
    WorkflowInstance workflowInstance = getWorkflowInstance(null, null, "presentation/source", null);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 3, result.getMediaPackage()
            .getTracks().length);
  }

  @Test
  public void testDistributeWithFlavorAndTags() throws Exception {
    String sourceTags = "engage,atom,rss";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, null, "presentation/source", null);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.getMediaPackage()
            .getTracks().length);
  }

  @Test
  public void testDistributeWithPriority() throws Exception {
    String targetTags = "xavier";
    WorkflowInstance workflowInstance = getWorkflowInstance(null, targetTags, null,
            "presentation/source,presenter/source");
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    MediaPackageElement[] elementsByTag = result.getMediaPackage().getElementsByTag(targetTags);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, elementsByTag.length);
    boolean presentationCheck = false;
    boolean presenterCheck = false;
    for (MediaPackageElement element : elementsByTag) {
      if (element.getFlavor().eq("presentation/source"))
        presentationCheck = true;
      if (element.getFlavor().eq("presenter/source"))
        presenterCheck = true;
    }
    Assert.assertTrue(presentationCheck);
    Assert.assertFalse(presenterCheck);
  }

  @Test
  public void testDistributeWithExcludeTag() throws Exception {
    String sourceTags = "engage,atom,-rss";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, null, null, null);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 3, result.getMediaPackage()
            .getTracks().length);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDistributeWithPriorityAndOther() throws Exception {
    WorkflowInstance workflowInstance = getWorkflowInstance(null, null, "rss", "presentation/source");
    operationHandler.start(workflowInstance, null);
  }

  @Test
  public void testDistributeWithNoParameters() throws Exception {
    WorkflowInstance workflowInstance = getWorkflowInstance(null, null, null, null);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  private WorkflowInstance getWorkflowInstance(String sourceTags, String targetTags, String sourceFlavors,
          String sourcePriorityFlavors) {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);

    operation.setConfiguration("source-tags", sourceTags);
    operation.setConfiguration("source-flavors", sourceFlavors);
    operation.setConfiguration("source-priority-flavors", sourcePriorityFlavors);
    operation.setConfiguration("target-tags", targetTags);

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    return workflowInstance;
  }

  class TestDistributionService implements DistributionService, JobProducer {
    public static final String JOB_TYPE = "distribute";

    @Override
    public Job distribute(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException,
            MediaPackageException {
      try {
        return serviceRegistry.createJob(JOB_TYPE, "distribute",
                Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), elementId));
      } catch (ServiceRegistryException e) {
        throw new DistributionException(e);
      }
    }

    @Override
    public Job retract(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException {
      try {
        return serviceRegistry.createJob(JOB_TYPE, "retract",
                Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), elementId));
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
    public void acceptJob(Job job) throws ServiceRegistryException {
      MediaPackage mp = null;
      MediaPackageElement element = null;
      try {
        mp = MediaPackageParser.getFromXml(job.getArguments().get(0));
        String elementId = job.getArguments().get(2);
        element = mp.getElementById(elementId);
        job.setPayload(MediaPackageElementParser.getAsXml(element));
      } catch (MediaPackageException e1) {
        throw new ServiceRegistryException("Error serializing or deserializing");
      }
      job.setStatus(Status.FINISHED);
      try {
        serviceRegistry.updateJob(job);
      } catch (NotFoundException e) {
        // not possible
      }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobProducer#isReadyToAcceptJobs(String)
     */
    @Override
    public boolean isReadyToAcceptJobs(String operation) throws ServiceRegistryException {
      return true;
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
