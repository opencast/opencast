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

package org.opencastproject.composer.impl;

import static org.easymock.EasyMock.capture;
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
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
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
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Tests the {@link ComposerServiceImpl}.
 */
public class ComposerServiceTest {
  /** The sources file to test with */
  private File sourceVideoOnly = null;
  private File sourceAudioOnly = null;
  private File sourceImage = null;

  /** The composer service to test */
  private ComposerServiceImpl composerService = null;

  /** FFmpeg binary location */
  private static final String FFMPEG_BINARY = "ffmpeg";

  /** File pointer to the testing dir to not pollute tmp */
  private static File testDir = new File("target");

  /** True to run the tests */
  private static boolean ffmpegInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceTest.class);
  private Track sourceVideoTrack;
  private Track sourceAudioTrack;
  private Track inspectedTrack;

  /** Encoding profile scanner */
  private EncodingProfileScanner profileScanner;

  @BeforeClass
  public static void testForFFmpeg() {
    try {
      Process p = new ProcessBuilder(FFMPEG_BINARY, "-version").start();
      if (p.waitFor() != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping composer tests due to missing ffmpeg");
      ffmpegInstalled = false;
    }
  }

  private static File getFile(final String path) throws Exception {
    return new File(ComposerServiceTest.class.getResource(path).toURI());
  }

  @Before
  public void setUp() throws Exception {
    // Skip tests if FFmpeg is not installed
    Assume.assumeTrue(ffmpegInstalled);

    // Create video only file
    File f = getFile("/video.mp4");
    sourceVideoOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp4", testDir);
    FileUtils.copyFile(f, sourceVideoOnly);

    // Create another audio only file
    f = getFile("/audio.mp3");
    sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp3", testDir);
    FileUtils.copyFile(f, sourceAudioOnly);

    // Create an image file
    f = getFile("/image.jpg");
    sourceImage = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".jpg", testDir);
    FileUtils.copyFile(f, sourceImage);

    // create the needed mocks
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(EasyMock.anyString())).andReturn(FFMPEG_BINARY);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    JaxbOrganization org = new DefaultOrganization();
    HashSet<JaxbRole> roles = new HashSet<>();
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
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();

    profileScanner = new EncodingProfileScanner();
    File encodingProfile = getFile("/encodingprofiles.properties");
    assertNotNull("Encoding profile must exist", encodingProfile);
    profileScanner.install(encodingProfile);

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace);

    // Create an encoding engine factory

    inspectedTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
            ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_video.xml"), Charset.defaultCharset()));
    sourceVideoTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
            ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_video.xml"), Charset.defaultCharset()));
    sourceAudioTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
            ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_audio.xml"), Charset.defaultCharset()));

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


    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    final Capture<String> type = EasyMock.newCapture();
    final Capture<String> operation = EasyMock.newCapture();
    final Capture<List<String>> args = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
            .andAnswer(() -> {
              // you could do work here to return something different if you needed.
              Job job = new JobImpl(0);
              job.setJobType(type.getValue());
              job.setOperation(operation.getValue());
              job.setArguments(args.getValue());
              job.setPayload(composerService.process(job));
              return job;
            }).anyTimes();
    composerService.setServiceRegistry(serviceRegistry);
    composerService.setProfileScanner(profileScanner);
    composerService.setWorkspace(workspace);

    EasyMock.replay(serviceRegistry);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(sourceVideoOnly);
    FileUtils.deleteQuietly(sourceAudioOnly);
    FileUtils.deleteQuietly(sourceImage);
  }

  @Test
  public void testConcurrentExecutionWithSameSource() throws Exception {
    assertTrue(sourceVideoOnly.isFile());
    List<Job> jobs = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      jobs.add(composerService.image(sourceVideoTrack, "player-preview.http", 1D));
    }
    for (Job j : jobs) {
      MediaPackageElementParser.getFromXml(j.getPayload());
    }
  }

  @Test
  public void testEncode() throws Exception {
    assertTrue(sourceVideoOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.encode(sourceVideoTrack, "av.work");
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  @Test
  public void testParallelEncode() throws Exception {
    assertTrue(sourceVideoOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    // Prepare job
    Job job = composerService.parallelEncode(sourceVideoTrack, "parallel.http");
    assertEquals(3, MediaPackageElementParser.getArrayFromXml(job.getPayload()).size());
  }

  @Test
  public void testTrim() throws Exception {
    assertTrue(sourceVideoOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.trim(sourceVideoTrack, "trim.work", 0, 100);
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  @Test
  public void testMux() throws Exception {
    assertTrue(sourceVideoOnly.isFile());
    assertTrue(sourceAudioOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).once();
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceAudioOnly).once();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.mux(sourceVideoTrack, sourceAudioTrack, "mux-av.work");
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  @Test
  public void testWatermark() throws Exception {
    assertTrue(sourceVideoOnly.isFile());
    assertTrue(sourceImage.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).once();
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceAudioOnly).once();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.watermark(sourceVideoTrack, sourceImage.getAbsolutePath(), "watermark.branding");
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  @Test
  public void testConvertImage() throws Exception {
    assertTrue(sourceImage.isFile());

    Attachment imageAttachment = (Attachment) MediaPackageElementParser.getFromXml(IOUtils.toString(
            ComposerServiceTest.class.getResourceAsStream("/composer_test_source_attachment_image.xml"),
            Charset.defaultCharset()));

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceImage).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceImage.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.convertImage(imageAttachment, "image-conversion.http");
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  /**
   * Test method for
   * {@link ComposerServiceImpl#composite(Dimension, Option, LaidOutElement, Option, String, String)}
   */
  @Test
  public void testComposite() throws Exception {
    if (!ffmpegInstalled)
      return;

    Dimension outputDimension = new Dimension(500, 500);

    List<HorizontalCoverageLayoutSpec> layouts = new ArrayList<HorizontalCoverageLayoutSpec>();
    layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
            .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}")));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
            .jsonObj("{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}")));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
            .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}")));

    List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<>();
    shapes.add(0, Tuple.tuple(new Dimension(300, 300), layouts.get(0)));
    shapes.add(1, Tuple.tuple(new Dimension(200, 200), layouts.get(1)));

    MultiShapeLayout multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes);

    Option<LaidOutElement<Attachment>> watermarkOption = Option.<LaidOutElement<Attachment>> none();
    LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(0));
    LaidOutElement<Track> upperLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(1));

    Job composite = composerService.composite(outputDimension, Option.option(lowerLaidOutElement), upperLaidOutElement,
            watermarkOption, "composite.work", "black");
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
    Dimension outputDimension = new Dimension(500, 500);
    Job concat = composerService.concat("concat.work", outputDimension, sourceVideoTrack, sourceVideoTrack);
    Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concat.getPayload());
    Assert.assertNotNull(concatTrack);
    inspectedTrack.setIdentifier(concatTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, concatTrack);
  }

  /**
   * Test method for {@link ComposerServiceImpl#concat(String, Dimension, float, Track...)}
   */
  @Test
  public void testConcatWithFrameRate() throws Exception {
    Dimension outputDimension = new Dimension(500, 500);
    Job concat = composerService.concat("concat.work", outputDimension, 20.0f, sourceVideoTrack, sourceVideoTrack);
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
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceImage).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceImage.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    EncodingProfile imageToVideoProfile = profileScanner.getProfile("image-movie.work");

    Attachment attachment = AttachmentImpl.fromURI(sourceImage.toURI());
    attachment.setIdentifier("test image");

    Job imageToVideo = composerService.imageToVideo(attachment, imageToVideoProfile.getIdentifier(), 1L);
    Track imageToVideoTrack = (Track) MediaPackageElementParser.getFromXml(imageToVideo.getPayload());
    Assert.assertNotNull(imageToVideoTrack);

    inspectedTrack.setIdentifier(imageToVideoTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, imageToVideoTrack);
  }
}
