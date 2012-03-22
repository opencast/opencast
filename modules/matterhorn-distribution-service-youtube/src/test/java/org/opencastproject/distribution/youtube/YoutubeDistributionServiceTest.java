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
package org.opencastproject.distribution.youtube;

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ANONYMOUS;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;

import org.opencastproject.deliver.youtube.YoutubeConfiguration;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

public class YoutubeDistributionServiceTest {

  private YoutubeDistributionService service = null;

  private MediaPackage mp = null;

  private ServiceRegistry serviceRegistry = null;

  @Before
  public void setUp() throws Exception {
    File mediaPackageRoot = new File("./target/test-classes");
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    builder.setSerializer(new DefaultMediaPackageSerializerImpl(mediaPackageRoot));
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/mediapackage.xml");
      mp = builder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    service = new YoutubeDistributionService();

    User anonymous = new User("anonymous", DEFAULT_ORGANIZATION_ID, new String[] { DEFAULT_ORGANIZATION_ANONYMOUS });
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    workflow.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    WorkflowSetImpl workflowSet = new WorkflowSetImpl();
    workflowSet.addItem(workflow);

    WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.getWorkflowInstances((WorkflowQuery) EasyMock.anyObject())).andReturn(workflowSet)
            .anyTimes();
    EasyMock.replay(workflowService);
    service.setWorkflowService(workflowService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService);
    service.setServiceRegistry(serviceRegistry);

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    service.setWorkspace(workspace);

    YoutubeDistributionService.config = YoutubeConfiguration.getInstance();
    YoutubeDistributionService.config.setUserId("asdf");
    YoutubeDistributionService.config.setPassword("asdf");
    YoutubeDistributionService.config.setClientId("abcde");
    YoutubeDistributionService.config.setKeywords("UCB");
    YoutubeDistributionService.config.setDeveloperKey("asdf");
    YoutubeDistributionService.config.setCategory("Education");
    YoutubeDistributionService.config.setUploadUrl("http://uploads.gdata.youtube.com/feeds/api/users/default/uploads");
    YoutubeDistributionService.config.setVideoPrivate(false);

    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "dublincore.xml"));
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "media.mov"));
    EasyMock.replay(workspace);
  }

  @After
  public void tearDown() throws Exception {
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  @Test
  public void testDistributeNotTrack() throws Exception {
    Job job = service.distribute(mp, "catalog-1");
    Assert.assertNull(job);
    job = service.distribute(mp, "notes");
    Assert.assertNull(job);
  }

  @Test
  public void testDistributeAuthFailed() throws Exception {
    Job job = service.distribute(mp, "track-1");
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 500, job);
    jobBarrier.waitForJobs();
    Assert.assertEquals(Status.FAILED, job.getStatus());
  }

  @Test
  public void testRetractFailed() throws Exception {
    Job job = service.retract(mp, "track-1");
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 500, job);
    jobBarrier.waitForJobs();
    Assert.assertEquals(Status.FAILED, job.getStatus());
  }

}
