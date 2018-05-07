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
package org.opencastproject.crop;

import org.opencastproject.crop.impl.CropServiceImpl;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
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
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.tika.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

public class CropServiceTest {

  /** Video file to test. Has black bars on each side */
  protected static final String mediaResource = "/black.mp4";

  /** Video file to test entire black slides. Shoudn't not be cropped */
  protected static final String mediaResource1 = "/schwarzesVideoFinal.mp4";

  /** Duration of whole videos */
  protected static final long mediaDuration = 40000L;
  protected static final long mediaDuration1 = 6000L;

  /** The in-memory service registration */
  protected ServiceRegistry serviceRegistry = null;
  protected ServiceRegistry serviceRegistry1 = null;

  /** The corp services */
  protected CropServiceImpl cropService = null;
  protected CropServiceImpl cropService1 = null;

  protected Mpeg7CatalogService mpeg7Service = null;
  protected Mpeg7CatalogService mpeg7Service1 = null;

  /** The media url */
  protected static TrackImpl track = null;
  protected static TrackImpl track1 = null;

  protected File tempFile = null;
  protected File tempFile1 = null;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * Copies test files to the local file system.
   *
   * @throws Exception
   *           if setup fails
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    track = TrackImpl.fromURI(CropServiceTest.class.getResource(mediaResource).toURI());
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track.setMimeType(MimeTypes.MJPEG);
    track.addStream(new VideoStreamImpl());
    track.setDuration(mediaDuration);

    track1 = TrackImpl.fromURI(CropServiceTest.class.getResource(mediaResource1).toURI());
    track1.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track1.setMimeType(MimeTypes.MJPEG);
    track1.addStream(new VideoStreamImpl());
    track1.setDuration(mediaDuration1);
  }

  @Before
  public void setUp() throws Exception {
    mpeg7Service = new Mpeg7CatalogService();
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(track.getURI()));
    tempFile = testFolder.newFile(getClass().getName() + ".xml");
    EasyMock.expect(workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
      @Override
      public URI answer() throws Throwable {
        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
        IOUtils.copy(in, new FileOutputStream(tempFile));
        return tempFile.toURI();
      }
    });
    EasyMock.replay(workspace);

    mpeg7Service1 = new Mpeg7CatalogService();
    Workspace workspace1 = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace1.get((URI) EasyMock.anyObject())).andReturn(new File(track1.getURI()));
    tempFile1 = testFolder.newFile(getClass().getName() + "-1.xml");

    EasyMock.expect(workspace1.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
      @Override
      public URI answer() throws Throwable {
        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
        IOUtils.copy(in, new FileOutputStream(tempFile1));
        return tempFile1.toURI();
      }
    });
    EasyMock.replay(workspace1);

    User anonymous = new JaxbUser("anonymous", "test", new DefaultOrganization(), new JaxbRole(DefaultOrganization
            .DEFAULT_ORGANIZATION_ANONYMOUS, new DefaultOrganization()));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject())).andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    cropService = new CropServiceImpl();
    serviceRegistry = new ServiceRegistryInMemoryImpl(cropService, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    cropService.setServiceRegistry(serviceRegistry);
    cropService.setMpeg7CatalogService(mpeg7Service);
    cropService.setWorkspace(workspace);
    cropService.setSecurityService(securityService);
    cropService.setOrganizationDirectoryService(organizationDirectoryService);
    cropService.setUserDirectoryService(userDirectoryService);

    cropService1 = new CropServiceImpl();
    serviceRegistry1 = new ServiceRegistryInMemoryImpl(cropService1, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    cropService1.setServiceRegistry(serviceRegistry1);
    cropService1.setMpeg7CatalogService(mpeg7Service1);
    cropService1.setWorkspace(workspace1);
    cropService1.setSecurityService(securityService);
    cropService1.setUserDirectoryService(userDirectoryService);
    cropService1.setOrganizationDirectoryService(organizationDirectoryService);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(tempFile);
    FileUtils.deleteQuietly(tempFile1);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
    ((ServiceRegistryInMemoryImpl) serviceRegistry1).dispose();
  }

  @Test
  public void testCrop() throws Exception {

    Job receipt = cropService.crop(track);
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    jobBarrier.waitForJobs();
    Assert.assertNotNull(receipt);

    Job receipt1 = cropService1.crop(track1);
    JobBarrier jobBarrier1 = new JobBarrier(null, serviceRegistry1, 1000, receipt1);
    jobBarrier1.waitForJobs();
    Assert.assertNotNull(receipt1);


  }

}
