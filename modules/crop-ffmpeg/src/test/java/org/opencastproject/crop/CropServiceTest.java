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
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
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
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;

public class CropServiceTest {

  /** Video file to test. Has black bars on each side */
  public static final String MEDIA_RESOURCE = "/black.mp4";
  /** Duration of whole videos */
  public static final long MEDIA_DURATION = 40000L;

  /** Video file to test entire black slides. Should not be cropped */
  public static final String MEDIA_RESOURCE_BLACK = "/schwarzesVideoFinal.mp4";
  public static final long MEDIA_DURATION_BLACK = 6000L;

  /** Video file to test. Has black bars on all sides */
  public static final String MEDIA_RESOURCE_VERT_BARS = "/black43.mp4";
  public static final long MEDIA_DURATION_VERT_BARS = 40000L;



  /** The in-memory service registration */
  protected ServiceRegistry serviceRegistry = null;

  /** The corp services */
  protected CropServiceImpl cropService = null;

  /** Temporary media file */
  protected File tempFile = null;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {

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
    cropService.setSecurityService(securityService);
    cropService.setOrganizationDirectoryService(organizationDirectoryService);
    cropService.setUserDirectoryService(userDirectoryService);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(tempFile);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  @Test
  public void testCrop() throws Exception {
    Track track = createTrack(MEDIA_RESOURCE, MEDIA_DURATION);
    cropService.setWorkspace(createWorkspace(track));
    Job receipt = cropService.crop(track);
    Assert.assertNotNull(receipt);

    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    JobBarrier.Result result = jobBarrier.waitForJobs();
    Assert.assertTrue(result.isSuccess());
  }

  @Test
  public void testVerticalBars() throws Exception {
    Track track = createTrack(MEDIA_RESOURCE_VERT_BARS, MEDIA_DURATION_VERT_BARS);
    cropService.setWorkspace(createWorkspace(track));
    Job receipt = cropService.crop(track);
    Assert.assertNotNull(receipt);

    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    JobBarrier.Result result = jobBarrier.waitForJobs();
    Assert.assertTrue(result.isSuccess());
  }

  @Test
  public void testBlackVideo() throws Exception {
    Track track = createTrack(MEDIA_RESOURCE_BLACK, MEDIA_DURATION_BLACK);
    cropService.setWorkspace(createWorkspace(track));
    Job receipt = cropService.crop(track);
    Assert.assertNotNull(receipt);

    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    JobBarrier.Result result = jobBarrier.waitForJobs();
    Assert.assertTrue(result.isSuccess());
  }

  @Test
  public void testEncoderProblem() throws Exception {
    Track track = createTrack(MEDIA_RESOURCE, MEDIA_DURATION);
    cropService.setWorkspace(createWorkspace(track));
    Dictionary properties = new Hashtable();
    properties.put(CropServiceImpl.CROP_FFMPEG_REGEX, "");
    cropService.updated(properties);
    Job receipt = cropService.crop(track);
    Assert.assertNotNull(receipt);

    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    JobBarrier.Result result = jobBarrier.waitForJobs();
    Assert.assertFalse(result.isSuccess());
  }



  /**
   * Copies test files to the local file system.
   *
   * @throws Exception
   *           if setup fails
   */
  protected Track createTrack(String resourceName, long duration) throws Exception {
    TrackImpl track = TrackImpl.fromURI(CropServiceTest.class.getResource(resourceName).toURI());
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track.setMimeType(MimeTypes.MJPEG);
    track.addStream(new VideoStreamImpl());
    track.setDuration(duration);

    return track;
  }

  protected Workspace createWorkspace(Track track) throws Exception {
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(track.getURI()));
    tempFile = testFolder.newFile(getClass().getName() + ".xml");

    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyObject(InputStream.class))).andAnswer(() -> {
      InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
      IOUtils.copy(in, new FileOutputStream(tempFile));
      return tempFile.toURI();
    });
    EasyMock.replay(workspace);

    return workspace;
  }
}
