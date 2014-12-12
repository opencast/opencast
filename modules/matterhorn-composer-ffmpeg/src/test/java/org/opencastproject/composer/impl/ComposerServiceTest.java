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
package org.opencastproject.composer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.composer.layout.HorizontalCoverageLayoutSpec;
import org.opencastproject.composer.layout.LayoutManager;
import org.opencastproject.composer.layout.MultiShapeLayout;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.StreamHelper;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Tests the {@link ComposerServiceImpl}.
 */
public class ComposerServiceTest {
  /** The sources file to test with */
  private File source = null;
  private File sourceVideoOnly = null;
  private File sourceAudioOnly = null;
  private File sourceImage = null;

  /** The composer service to test */
  private ComposerServiceImpl composerService = null;

  /** The service registry for job dispatching */
  private ServiceRegistry serviceRegistry = null;

  /** FFmpeg binary location */
  private static final String FFMPEG_BINARY = "ffmpeg";

  /** File pointer to the testing dir to not pollute tmp */
  private static File testDir = new File("target");

  /** True to run the tests */
  private static boolean ffmpegInstalled = true;

  /** True to run the tests */
  private static boolean ffmpegInstalledGreaterVersion2 = false;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceTest.class);
  private Track inspectedTrack;

  /** Encoding profile scanner */
  private EncodingProfileScanner profileScanner;

  @BeforeClass
  public static void testForFFmpeg() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    Process p = null;
    try {
      p = new ProcessBuilder(FFMPEG_BINARY, "-version").start();
      StringBuffer buffer = new StringBuffer();
      stdout = new StreamHelper(p.getInputStream(), buffer);
      stderr = new StreamHelper(p.getErrorStream());
      int status = p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      System.out.println(buffer.toString());
      if (status != 0)
        throw new IllegalStateException();
      if (buffer.toString().startsWith("ffmpeg version 2"))
        ffmpegInstalledGreaterVersion2 = true;
    } catch (Throwable t) {
      logger.warn("Skipping image composer service tests due to unsatisifed or erroneus ffmpeg installation");
      ffmpegInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  private static File getFile(String path) throws Exception {
    return new File(ComposerServiceTest.class.getResource(path).toURI());
  }

  @Before
  public void setUp() throws Exception {
    if (!ffmpegInstalled)
      return;

    // Copy an existing media file to a temp file
    File f = getFile("/slidechanges.mov");
    source = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mov", testDir);
    FileUtils.copyFile(f, source);
    f = null;

    // Create another video only file
    f = getFile("/video.mp4");
    sourceVideoOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp4", testDir);
    FileUtils.copyFile(f, sourceVideoOnly);
    f = null;

    // Create another audio only file
    f = getFile("/audio.mp3");
    sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp3", testDir);
    FileUtils.copyFile(f, sourceAudioOnly);
    f = null;

    // Create an image file
    f = getFile("/image.jpg");
    sourceImage = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".jpg", testDir);
    FileUtils.copyFile(f, sourceImage);
    f = null;

    // create the needed mocks
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty((String) EasyMock.anyObject())).andReturn(FFMPEG_BINARY);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    JaxbOrganization org = new DefaultOrganization();
    HashSet<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org, ""));
    User user = new JaxbUser("admin", "test", org, roles);
    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(org).anyTimes();

    UserDirectoryService userDirectory = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(source).anyTimes();

    profileScanner = new EncodingProfileScanner();
    File encodingProfile = getFile("/encodingprofiles.properties");
    assertNotNull("Encoding profile must exist", encodingProfile);
    profileScanner.install(encodingProfile);

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace);

    // Create an encoding engine factory
    EncoderEngineFactoryImpl encoderEngineFactory = new EncoderEngineFactoryImpl();
    encoderEngineFactory.activate(cc);

    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>";
    inspectedTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);

    // Create and populate the composer service
    composerService = new ComposerServiceImpl() {
      @Override
      protected Job inspect(Job job, URI workspaceURI) throws EncoderException {
        Job inspectionJob = EasyMock.createNiceMock(Job.class);
        try {
          EasyMock.expect(inspectionJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(inspectedTrack));
        } catch (MediaPackageException e) {
          throw new RuntimeException(e);
        }
        EasyMock.replay(inspectionJob);
        return inspectionJob;
      }
    };
    serviceRegistry = new ServiceRegistryInMemoryImpl(composerService, securityService, userDirectory, orgDirectory,
            EasyMock.createNiceMock(IncidentService.class));
    composerService.setEncoderEngineFactory(encoderEngineFactory);
    composerService.setOrganizationDirectoryService(orgDirectory);
    composerService.setSecurityService(securityService);
    composerService.setServiceRegistry(serviceRegistry);
    composerService.setUserDirectoryService(userDirectory);
    composerService.setProfileScanner(profileScanner);
    composerService.setWorkspace(workspace);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(source);
    FileUtils.deleteQuietly(sourceVideoOnly);
    FileUtils.deleteQuietly(sourceAudioOnly);
    FileUtils.deleteQuietly(sourceImage);
  }

  @Test
  public void testConcurrentExecutionWithSameSource() throws Exception {
    if (!ffmpegInstalled)
      return;

    assertTrue(source.isFile());
    String sourceTrackXml = "<track id=\"track-1\" type=\"presentation/source\"><mimetype>video/quicktime</mimetype>"
            + "<url>http://localhost:8080/workflow/samples/camera.mpg</url>"
            + "<checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum><duration>14546</duration>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    List<Job> jobs = new ArrayList<Job>();
    for (int i = 0; i < 10; i++) {
      jobs.add(composerService.image(sourceTrack, "player-preview.http", 1D));
    }
    boolean success = new JobBarrier(serviceRegistry, jobs.toArray(new Job[jobs.size()])).waitForJobs().isSuccess();
    assertTrue(success);
    for (Job j : jobs) {
      // Always check the service registry for the latest version of the job
      Job job = serviceRegistry.getJob(j.getId());
      assertEquals(Job.Status.FINISHED, job.getStatus());
    }
  }

  @Test
  public void testEncode() throws Exception {
    if (!ffmpegInstalled)
      return;

    assertTrue(sourceVideoOnly.isFile());
    assertTrue(sourceAudioOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    MediaInspectionService inspect = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(inspect.inspect((URI) EasyMock.anyObject()))
            .andThrow(new MediaInspectionException("test complete")).anyTimes();
    EasyMock.replay(workspace, inspect);

    // build a single media package to test with
    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    try {
      composerService.encode(sourceTrack, "av.work");
    } catch (EncoderException e) {
      assertTrue("test complete".equals(e.getMessage()));
    }
  }

  @Test
  public void testEncode2() throws Exception {
    if (!ffmpegInstalled)
      return;

    assertTrue(sourceVideoOnly.isFile());
    assertTrue(sourceAudioOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    MediaInspectionService inspect = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(inspect.inspect((URI) EasyMock.anyObject()))
            .andThrow(new MediaInspectionException("test complete")).anyTimes();
    EasyMock.replay(workspace, inspect);

    String sourceTrackVideoXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>";
    Track sourceTrackVideo = (Track) MediaPackageElementParser.getFromXml(sourceTrackVideoXml);
    String sourceTrackAudioXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2b'>"
            + "       <mimetype>audio/mp3</mimetype>" + "       <url>audio.mp3</url>" + "       </track>";
    Track sourceTrackAudio = (Track) MediaPackageElementParser.getFromXml(sourceTrackAudioXml);

    try {
      composerService.encode(null, sourceTrackVideo, sourceTrackAudio, "av.work", null);
    } catch (IllegalArgumentException e) {
      assertTrue("The Job parameter must not be null".equals(e.getMessage()));
    }
  }
  
  @Test
  public void testParallelEncode() throws Exception {
    if (!ffmpegInstalled)
      return;

    assertTrue(sourceVideoOnly.isFile());
    assertTrue(sourceAudioOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    MediaInspectionService inspect = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(inspect.inspect((URI) EasyMock.anyObject()))
            .andThrow(new MediaInspectionException("test complete")).anyTimes();
    EasyMock.replay(workspace, inspect);

    // build a single media package to test with
    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    try {
      composerService.parallelEncode(sourceTrack, "parallel.http");
    } catch (EncoderException e) {
      assertTrue("test complete".equals(e.getMessage()));
    }
  }  

  /**
   * Test method for
   * {@link ComposerServiceImpl#composite(Dimension, LaidOutElement, LaidOutElement, Option, String, String)}
   */
  @Test
  public void testComposite() throws Exception {
    if (!ffmpegInstalledGreaterVersion2)
      return;

    // build a single media package to test with
    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);

    Dimension outputDimension = new Dimension(500, 500);

    List<HorizontalCoverageLayoutSpec> layouts = new ArrayList<HorizontalCoverageLayoutSpec>();
    layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
            .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}")));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
            .jsonObj("{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}")));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
            .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}")));

    List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>();
    shapes.add(0, Tuple.tuple(new Dimension(300, 300), layouts.get(0)));
    shapes.add(1, Tuple.tuple(new Dimension(200, 200), layouts.get(1)));

    MultiShapeLayout multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes);

    Option<LaidOutElement<Attachment>> watermarkOption = Option.<LaidOutElement<Attachment>> none();
    LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(sourceTrack, multiShapeLayout.getShapes()
            .get(0));
    LaidOutElement<Track> upperLaiedOutElement = new LaidOutElement<Track>(sourceTrack, multiShapeLayout.getShapes()
            .get(1));

    Job composite = composerService.composite(outputDimension, lowerLaidOutElement, upperLaiedOutElement,
            watermarkOption, "composite.work", "black");
    JobBarrier barrier = new JobBarrier(serviceRegistry, composite);
    if (!barrier.waitForJobs().isSuccess()) {
      Assert.fail("Composite job did not success!");
    }

    Track compositeTrack = (Track) MediaPackageElementParser.getFromXml(composite.getPayload());
    Assert.assertNotNull(compositeTrack);
    inspectedTrack.setIdentifier(compositeTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, compositeTrack);
  }

  /**
   * Test method for {@link ComposerServiceImpl#concat(String, Dimension, Track...)}
   */
  @Test
  public void testConcat() throws Exception {
    if (!ffmpegInstalledGreaterVersion2)
      return;

    // build two media package to test with
    String sourceTrack1Xml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>";
    Track sourceTrack1 = (Track) MediaPackageElementParser.getFromXml(sourceTrack1Xml);

    String sourceTrack2Xml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track type='presentation/source'" + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>slidechanges.mov</url>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>";
    Track sourceTrack2 = (Track) MediaPackageElementParser.getFromXml(sourceTrack2Xml);

    Dimension outputDimension = new Dimension(500, 500);

    Job concat = composerService.concat("concat.work", outputDimension, sourceTrack1, sourceTrack2);
    JobBarrier barrier = new JobBarrier(serviceRegistry, concat);
    if (!barrier.waitForJobs().isSuccess()) {
      Assert.fail("Concat job did not success!");
    }

    Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concat.getPayload());
    Assert.assertNotNull(concatTrack);
    inspectedTrack.setIdentifier(concatTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, concatTrack);
  }

  /**
   * Test method for
   * {@link org.opencastproject.composer.impl.ComposerServiceImpl#imageToVideo(org.opencastproject.mediapackage.Attachment, String, Long)}
   */
  @Test
  public void testImageToVideo() throws Exception {
    if (!ffmpegInstalled)
      return;

    assertTrue(sourceImage.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(sourceImage).anyTimes();
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(sourceImage.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    EncodingProfile imageToVideoProfile = profileScanner.getProfile("image-movie.work");

    Attachment attachement = AttachmentImpl.fromURI(sourceImage.toURI());

    attachement.setIdentifier("test image");

    Job imageToVideo = composerService.imageToVideo(attachement, imageToVideoProfile.getIdentifier(), 2L);
    JobBarrier barrier = new JobBarrier(serviceRegistry, imageToVideo);
    if (!barrier.waitForJobs().isSuccess()) {
      Assert.fail("ImageToVideo job did not success!");
    }

    Track imageToVideoTrack = (Track) MediaPackageElementParser.getFromXml(imageToVideo.getPayload());
    Assert.assertNotNull(imageToVideoTrack);

    inspectedTrack.setIdentifier(imageToVideoTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, imageToVideoTrack);
  }
}
