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
package org.opencastproject.videoeditor.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.ffmpeg.FFmpegAnalyzer;
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
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;
import org.opencastproject.smil.impl.SmilServiceImpl;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test class for video editor
 */
public class VideoEditorTest {

  private static final Logger logger = LoggerFactory.getLogger(VideoEditorTest.class);
  /** FFmpeg binary location */
  private static final String FFMPEG_BINARY = "ffmpeg";
  /** SMIL file to run the editing */
  protected static Smil smil = null;
  protected static String storageDir = "/tmp";

  /** Videos file to test. 2 videos of different framerate, must have same resolution */
  protected static final String mediaResource = "/testresources/testvideo_320x180.mp4";// 320x180, 30fps h264

  /** Duration of first and second movie */
  protected static final long movieDuration = 217650L; //3:37.65 seconds

  /** The smil service */
  protected static SmilServiceImpl smilService = null;

  /** The in-memory service registration */
  protected ServiceRegistry serviceRegistry = null;

  /** The video editor */
  protected VideoEditorServiceImpl veditor = null;

  /** The media url */
  protected static TrackImpl track1 = null;
  protected static TrackImpl track2 = null;

  /** Temp storage in workspace */
  protected File tempFile1 = null;

  /** output track */
  protected Track inspectedTrack = null;


  /**
   * Copies test files to the local file system, since jmf is not able to access movies from the resource section of a
   * bundle.
   * 
   * @throws Except
   *           if setup fails
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    /* Set up the 2 tracks for merging */
    track1 = TrackImpl.fromURI(VideoEditorTest.class.getResource(mediaResource).toURI());
    track1.setIdentifier("track-1");
    track1.setFlavor(new MediaPackageElementFlavor("source", "presentater"));
    track1.setMimeType(MimeTypes.MJPEG);
    track1.addStream(new VideoStreamImpl());
    track1.setDuration(new Long(movieDuration));

    track2 = TrackImpl.fromURI(VideoEditorTest.class.getResource(mediaResource).toURI());
    track2.setIdentifier("track-2");
    track2.setFlavor(new MediaPackageElementFlavor("source", "presentater"));
    track2.setMimeType(MimeTypes.MJPEG);
    track2.addStream(new VideoStreamImpl());
    track2.setDuration(new Long(movieDuration));

    /* create a smil file with the 2 tracks - note that the 2 clips have the same paramGroupId */
    smilService = new SmilServiceImpl();
    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addParallel(smilResponse.getSmil());
    SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
    smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), track1, 1000L, 11000L);

    List<SmilMediaParamGroup> groups = smilResponse.getSmil().getHead().getParamGroups();
    String paramGroupId = groups.get(0).getId();

    smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), track2, 1000L, 12000L, paramGroupId);
    smil = smilResponse.getSmil();
  }

  /**
   * Setup for the video editor service, including creation of a mock workspace and all dependencies.
   *
   * @throws Exception
   *           if setup fails
   */
  @Before
  public void setUp() throws Exception {

    tempFile1 = File.createTempFile(getClass().getName(), ".mp4"); // output file

    /* mock the workspace for the input/output file */
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) track1.getURI())).andReturn(new File(track1.getURI())).anyTimes();
    EasyMock.expect(workspace.get((URI) track2.getURI())).andReturn(new File(track2.getURI())).anyTimes();
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
                      @Override
                      public URI answer() throws Throwable {
                          InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
                          IOUtils.copy(in, new FileOutputStream(tempFile1));
                          return tempFile1.toURI();
                      }
                    });

    /* mock the role/org/security dependencies */
    User anonymous = new JaxbUser("anonymous", "test", new DefaultOrganization(), new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, new DefaultOrganization()));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
    .andReturn(organization).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();

    /* mock the osgi init for the video editor itself */
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty("org.opencastproject.storage.dir")).andReturn(storageDir).anyTimes();
    EasyMock.expect(bc.getProperty("org.opencastproject.composer.ffmpegpath")).andReturn(FFMPEG_BINARY).anyTimes();
    EasyMock.expect(bc.getProperty(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG)).andReturn("ffprobe").anyTimes();
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc,cc,workspace,userDirectoryService,organizationDirectoryService,securityService);

    /* mock inspector output so that the job will alway pass */
    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<track type='presentation/source' id='deadbeef-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "<audio id='audio-1'><encoder type='MP3 (MPEG audio layer 3)'/><channels>2</channels>"
            + "<bitrate>96000.0</bitrate></audio><video id='video-1'><device/>"
            + "<encoder type='FLV / Sorenson Spark / Sorenson H.263 (Flash Video)'/>"
            + "<bitrate>512000.0</bitrate><framerate>15.0</framerate>"
            + "<resolution>854x480</resolution></video>"
            + "<mimetype>video/mpeg</mimetype><url>video.mp4</url></track>";

    inspectedTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    veditor = new VideoEditorServiceImpl() {
    @Override
      protected Job inspect(Job job, URI workspaceURI) throws MediaInspectionException, ProcessFailedException {
        Job inspectionJob = EasyMock.createNiceMock(Job.class);
        try {
          EasyMock.expect(inspectionJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(inspectedTrack));
        } catch (MediaPackageException e) {
          throw new MediaInspectionException(e);
        }
        EasyMock.replay(inspectionJob);
        return inspectionJob;
      }
    };

    /* set up video editor */
    veditor.activate(cc);
    veditor.setWorkspace(workspace);
    veditor.setSecurityService(securityService);
    veditor.setUserDirectoryService(userDirectoryService);
    veditor.setSmilService(smilService);
    veditor.setOrganizationDirectoryService(organizationDirectoryService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(veditor, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    veditor.setServiceRegistry(serviceRegistry);

  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(tempFile1);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  /**
   * Run the smil file and test that file is created
   */
  @Test
  public void testAnalyze() throws Exception {
    List<Job> receipts = veditor.processSmil(smil);
    logger.debug("SMIL is " +  smil.toXML());
    Job receipt;
    Iterator<Job> it = receipts.iterator();
    while (it.hasNext()) {
      receipt = it.next();
      JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 2000, receipt); // wait for task to finish
      jobBarrier.waitForJobs();
      assertNotNull("Audiovisual content expected", receipt.getPayload());
      assertTrue("Merged File exists", tempFile1.exists());
      assertTrue("Merged video is the correct size", tempFile1.length() > 100000);  // roughly 4424149
      logger.info("Resulting file size {} ", tempFile1.length());
    }
  }
}
