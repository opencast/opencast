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
package org.opencastproject.publication.youtube;

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.deliver.youtube.YouTubeConfiguration;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.workspace.api.Workspace;

import java.io.File;
import java.net.URI;

public class YouTubePublicationServiceTest {

  private YouTubePublicationServiceImpl service = null;

  private MediaPackage mp = null;

  private ServiceRegistry serviceRegistry = null;

  @Before
  public void setUp() throws Exception {
    mp = MediaPackageSupport.loadFromClassPath("/mediapackage.xml");
    service = new YouTubePublicationServiceImpl();

    User anonymous = new User("anonymous", DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            new String[] { DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS });
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

    YouTubePublicationServiceImpl.config = YouTubeConfiguration.getInstance();
    YouTubePublicationServiceImpl.config.setUserId("asdf");
    YouTubePublicationServiceImpl.config.setPassword("asdf");
    YouTubePublicationServiceImpl.config.setClientId("abcde");
    YouTubePublicationServiceImpl.config.setKeywords("UCB");
    YouTubePublicationServiceImpl.config.setDeveloperKey("asdf");
    YouTubePublicationServiceImpl.config.setCategory("Education");
    YouTubePublicationServiceImpl.config
            .setUploadUrl("http://uploads.gdata.youtube.com/feeds/api/users/default/uploads");
    YouTubePublicationServiceImpl.config.setVideoPrivate(false);

    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(getClass().getResource("/dublincore.xml").toURI()));
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(getClass().getResource("/media.mov").toURI()));
    EasyMock.replay(workspace);
  }

  @After
  public void tearDown() throws Exception {
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  @Test
  public void testPublishAuthFailed() throws Exception {
    Job job = service.publish(mp, mp.getTrack("track-1"));
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 500, job);
    jobBarrier.waitForJobs();
    Assert.assertEquals(Status.FAILED, job.getStatus());
  }

  @Test
  public void testRetractFailed() throws Exception {
    Job job = service.retract(mp);
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 500, job);
    jobBarrier.waitForJobs();
    Assert.assertNull(job.getPayload());
    Assert.assertEquals(Status.FINISHED, job.getStatus());
  }

}
