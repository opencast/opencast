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

package org.opencastproject.composer.impl;

import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.layout.AbsolutePositionLayoutSpec;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.composer.layout.HorizontalCoverageLayoutSpec;
import org.opencastproject.composer.layout.LayoutManager;
import org.opencastproject.composer.layout.MultiShapeLayout;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementParser;
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
import org.opencastproject.util.MimeType;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.JsonParser;

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

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * Tests the {@link ComposerServiceImpl}.
 */
public class ComposerServiceTest {
  /** The sources file to test with */
  private File sourceVideoOnly = null;
  private File sourceAudioOnly = null;
  private File sourceAudioWithGap = null;
  private File sourceImage = null;
  private File sourceAVTS = null;

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
  private Track sourceAVTSTrack; // Test that this ffmpeg works with TS files
  private Track inspectedTrack;
  private Attachment watermarkImageAttachment;

  /** Encoding profile scanner */
  private EncodingProfileScanner profileScanner;

  @BeforeClass
  public static void testForFFmpeg() {
    try {
      Process p = new ProcessBuilder(FFMPEG_BINARY, "-version").start();
      if (p.waitFor() != 0) {
        throw new IllegalStateException();
      }
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

    f = getFile("/avts.mts");
    sourceAVTS = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mts", testDir);
    FileUtils.copyFile(f, sourceAVTS);

    // Create another audio only file
    f = getFile("/audio.mp3");
    sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp3", testDir);
    FileUtils.copyFile(f, sourceAudioOnly);

    // Audio file with a timestamp gap: 4 seconds long, but without packets between 1 and 3 seconds
    f = getFile("/audio_gap.webm");
    sourceAudioWithGap = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".webm", testDir);
    FileUtils.copyFile(f, sourceAudioWithGap);

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
    final Capture<String> capture = EasyMock.newCapture();
    EasyMock.expect(workspace.toSafeName(EasyMock.capture(capture))).andAnswer(capture::getValue).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.eq(false))).andReturn(sourceVideoOnly).anyTimes();

    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.eq(true))).andAnswer(() -> {
      File f1 = getFile("/video.mp4");
      File uniqueSourceVideo = File.createTempFile(FilenameUtils.getBaseName(f1.getName()), ".mp4", testDir);
      FileUtils.copyFile(f1, uniqueSourceVideo);
      return uniqueSourceVideo;
    }).anyTimes();

    profileScanner = new EncodingProfileScanner();
    File encodingProfile = getFile("/encodingprofiles.properties");
    assertNotNull("Encoding profile must exist", encodingProfile);
    profileScanner.install(encodingProfile);

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace);

    // Create an encoding engine factory

    inspectedTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
        ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_video.xml"),
        Charset.defaultCharset()));
    sourceVideoTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
        ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_video.xml"),
        Charset.defaultCharset()));
    sourceAudioTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
        ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_audio.xml"),
        Charset.defaultCharset()));
    watermarkImageAttachment = (Attachment) MediaPackageElementParser.getFromXml(IOUtils.toString(
        ComposerServiceTest.class.getResourceAsStream("/composer_test_watermark_attachment.xml"),
        Charset.defaultCharset()));
    sourceAVTSTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
        ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_transport_stream.xml"),
        Charset.defaultCharset()));

    // Create and populate the composer service
    composerService = new ComposerServiceImpl() {
      @Override
      protected List<Track> inspect(Job job, List<URI> uris, List<List<String>> tags) throws EncoderException {
        final var result = new ArrayList<Track>(uris.size());
        for (URI uri: uris) {
          result.add(inspectedTrack);
        }
        return result;
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
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.encode(sourceVideoTrack, "av.copy");
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  @Test
  public void testParallelEncode() throws Exception {
    assertTrue(sourceVideoOnly.isFile());

    // Need different media files
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).anyTimes();
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
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).anyTimes();
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
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).once();
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceAudioOnly).once();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(sourceVideoOnly.toURI()).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.mux(sourceVideoTrack, sourceAudioTrack, "mux-av.copy");
    MediaPackageElementParser.getFromXml(job.getPayload());
  }

  /**
   * Test method for {@link ComposerServiceImpl#mergeAudioTracks(String, List, List)}
   */
  @Test
  public void testMergeAudioTracks() throws Exception {
    // The source audio is 2 seconds long. Placed at 0 and 2 seconds, the result is 4 seconds long.
    File merged = mergeAudioTracks("audiomerge.work", Arrays.asList(0L, 2000L));
    assertEquals(4.0, getDuration(merged), 0.1);
    // The snippets don't overlap, so merging must not change the volume
    assertEquals(getMaxVolume(sourceAudioOnly), getMaxVolume(merged), 0.5);
  }

  /**
   * Test method for {@link ComposerServiceImpl#mergeAudioTracks(String, List, List)} with overlapping tracks
   */
  @Test
  public void testMergeOverlappingAudioTracks() throws Exception {
    File merged = mergeAudioTracks("audiomerge.work", Arrays.asList(0L, 1000L));
    assertEquals(3.0, getDuration(merged), 0.1);
  }

  /**
   * Test method for {@link ComposerServiceImpl#mergeAudioTracks(String, List, List)} with a timestamp gap in the
   * first track. Recordings of live streams (e.g. BigBlueButton) have such gaps where packets were lost or the
   * microphone was muted. amix takes its timestamps from the first track, so the gap must not shift the other tracks.
   */
  @Test
  public void testMergeAudioTracksWithGapInFirstTrack() throws Exception {
    // The track with the gap ends at 4 seconds, the second track (2 seconds) placed at 3 seconds ends at 5 seconds.
    // Without filling the gap, the second track would be shifted back by it and end at 7 seconds.
    File merged = mergeAudioTracks("audiomerge.work", Arrays.asList(0L, 3000L),
        Arrays.asList(sourceAudioWithGap, sourceAudioOnly));
    assertEquals(5.0, getDuration(merged), 0.1);
  }

  /**
   * Test method for {@link ComposerServiceImpl#mergeAudioTracks(String, List, List)} with a timestamp gap in a
   * track that is not the first one
   */
  @Test
  public void testMergeAudioTracksWithGapInSecondTrack() throws Exception {
    // The track with the gap placed at 1 second ends at 5 seconds. Without filling the gap, its audio after the gap
    // would move forward by it and end at 3 seconds.
    File merged = mergeAudioTracks("audiomerge.work", Arrays.asList(0L, 1000L),
        Arrays.asList(sourceAudioOnly, sourceAudioWithGap));
    assertEquals(5.0, getDuration(merged), 0.1);
  }

  /**
   * Test method for {@link ComposerServiceImpl#mergeAudioTracks(String, List, List)} without a configured
   * audio merge profile
   */
  @Test
  public void testMergeAudioTracksWithBuiltInProfile() throws Exception {
    // The built-in profile is created on activation
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc, cc);
    // The activation needs a service registry to read the job loads
    ServiceRegistry jobRegistry = composerService.getServiceRegistry();
    ServiceRegistry activationRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(activationRegistry.getHostRegistrations()).andReturn(new ArrayList<>()).anyTimes();
    EasyMock.replay(activationRegistry);
    composerService.setServiceRegistry(activationRegistry);
    composerService.activate(cc, new HashMap<>());
    composerService.setServiceRegistry(jobRegistry);
    composerService.setProfileScanner(new EncodingProfileScanner());

    File merged = mergeAudioTracks("audiomerge.work", Arrays.asList(0L, 2000L));
    assertEquals(4.0, getDuration(merged), 0.1);
  }

  /**
   * Test method for {@link ComposerServiceImpl#mergeAudioTracks(String, List, List)} with an unknown profile
   */
  @Test(expected = EncoderException.class)
  public void testMergeAudioTracksWithUnknownProfile() throws Exception {
    mergeAudioTracks("unknown.work", Arrays.asList(0L, 2000L));
  }

  /** Merges the audio source with itself at the given start times and returns the resulting file */
  private File mergeAudioTracks(String profileId, List<Long> startTimes) throws Exception {
    List<File> sources = new ArrayList<>();
    for (int i = 0; i < startTimes.size(); i++) {
      sources.add(sourceAudioOnly);
    }
    return mergeAudioTracks(profileId, startTimes, sources);
  }

  /** Merges the audio files at the given start times and returns the resulting file */
  private File mergeAudioTracks(String profileId, List<Long> startTimes, List<File> sources) throws Exception {
    File merged = File.createTempFile("merged", ".mkv", testDir);
    merged.deleteOnExit();

    // One track per source file, the workspace returns the file for the track's URI
    List<Track> tracks = new ArrayList<>();
    Map<URI, File> files = new HashMap<>();
    for (File source : sources) {
      Track track = (Track) sourceAudioTrack.clone();
      track.setURI(source.toURI());
      tracks.add(track);
      files.put(track.getURI(), source);
    }

    // Keep a copy of the merged file, as the composer deletes it after putting it into the workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andAnswer(() -> files.get((URI) EasyMock.getCurrentArguments()[0])).anyTimes();
    EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andAnswer(() -> {
              FileUtils.copyInputStreamToFile((InputStream) EasyMock.getCurrentArguments()[2], merged);
              return merged.toURI();
            }).anyTimes();
    composerService.setWorkspace(workspace);
    EasyMock.replay(workspace);

    Job job = composerService.mergeAudioTracks(profileId, startTimes, tracks);
    assertNotNull(MediaPackageElementParser.getFromXml(job.getPayload()));
    return merged;
  }

  private static double getDuration(File file) throws Exception {
    Matcher m = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)").matcher(analyzeAudio(file));
    assertTrue("FFmpeg did not report a duration for " + file, m.find());
    return Integer.parseInt(m.group(1)) * 3600 + Integer.parseInt(m.group(2)) * 60 + Double.parseDouble(m.group(3));
  }

  private static double getMaxVolume(File file) throws Exception {
    Matcher m = Pattern.compile("max_volume: (-?\\d+(\\.\\d+)?) dB").matcher(analyzeAudio(file));
    assertTrue("FFmpeg did not report a volume for " + file, m.find());
    return Double.parseDouble(m.group(1));
  }

  /** Runs FFmpeg's volume detection on the file and returns its output */
  private static String analyzeAudio(File file) throws Exception {
    Process p = new ProcessBuilder(FFMPEG_BINARY, "-hide_banner", "-nostdin", "-i", file.getAbsolutePath(),
        "-af", "volumedetect", "-f", "null", "-").redirectErrorStream(true).start();
    String output = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
    p.waitFor();
    return output;
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
   * {@link ComposerServiceImpl#composite(Dimension, Optional, LaidOutElement, Optional, String, String, String)}
   */
  @Test
  public void testComposite() throws Exception {
    if (!ffmpegInstalled) {
      return;
    }

    Dimension outputDimension = new Dimension(500, 500);

    List<HorizontalCoverageLayoutSpec> layouts = new ArrayList<HorizontalCoverageLayoutSpec>();
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},"
                + "\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}").getAsJsonObject()));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},"
                + "\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}").getAsJsonObject()));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},"
                + "\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}").getAsJsonObject()));

    List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<>();
    shapes.add(0, Tuple.tuple(new Dimension(300, 300), layouts.get(0)));
    shapes.add(1, Tuple.tuple(new Dimension(200, 200), layouts.get(1)));

    MultiShapeLayout multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes);

    Optional<LaidOutElement<Attachment>> watermarkOption = Optional.<LaidOutElement<Attachment>> empty();
    LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(0));
    LaidOutElement<Track> upperLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(1));

    Job composite = composerService.composite(outputDimension, Optional.ofNullable(lowerLaidOutElement),
        upperLaidOutElement, watermarkOption, "composite.work", "black", "both");
        //  null or "both" means that both tracks are checked for audio and both audio tracks
        // are mixed into the final composite if they exist

    Track compositeTrack = (Track) MediaPackageElementParser.getFromXml(composite.getPayload());
    Assert.assertNotNull(compositeTrack);
    inspectedTrack.setIdentifier(compositeTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, compositeTrack);
  }

  /**
   * Test method for {@link ComposerServiceImpl#concat(String, Dimension, boolean, Track...)}
   */
  @Test
  public void testConcatTransportStream() throws Exception {
    // Some versions of ffmpeg crashes
    Job concat = composerService.concat("concat.work", null, true, sourceAVTSTrack, sourceAVTSTrack);
    Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concat.getPayload());
    Assert.assertNotNull(concatTrack);
    inspectedTrack.setIdentifier(concatTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, concatTrack);
  }

  @Test
  public void testCompositeAudio() throws Exception {
    if (!ffmpegInstalled) {
      return;
    }

    Dimension outputDimension = new Dimension(500, 500);

    List<HorizontalCoverageLayoutSpec> layouts = new ArrayList<HorizontalCoverageLayoutSpec>();
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},"
                + "\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}").getAsJsonObject()));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},"
                + "\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}").getAsJsonObject()));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},"
                + "\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}").getAsJsonObject()));

    List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<>();
    shapes.add(0, Tuple.tuple(new Dimension(300, 300), layouts.get(0)));
    shapes.add(1, Tuple.tuple(new Dimension(200, 200), layouts.get(1)));

    MultiShapeLayout multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes);

    Optional<LaidOutElement<Attachment>> watermarkOption = Optional.<LaidOutElement<Attachment>> empty();
    LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(0));
    LaidOutElement<Track> upperLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(1));

    Job composite = composerService.composite(outputDimension, Optional.ofNullable(lowerLaidOutElement),
        upperLaidOutElement, watermarkOption, "composite.work", "black", "upper");
    Track compositeTrack = (Track) MediaPackageElementParser.getFromXml(composite.getPayload());
    Assert.assertNotNull(compositeTrack);
    inspectedTrack.setIdentifier(compositeTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, compositeTrack);
  }

  @Test
  public void testCompositeAudioAndWatermark() throws Exception {
    if (!ffmpegInstalled) {
      return;
    }

    Dimension outputDimension = new Dimension(500, 500);

    List<HorizontalCoverageLayoutSpec> layouts = new ArrayList<HorizontalCoverageLayoutSpec>();
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},"
                + "\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}").getAsJsonObject()));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},"
                + "\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}").getAsJsonObject()));
    layouts.add(Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(
                "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},"
                + "\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}").getAsJsonObject()));

    List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<>();
    shapes.add(0, Tuple.tuple(new Dimension(300, 300), layouts.get(0)));
    shapes.add(1, Tuple.tuple(new Dimension(200, 200), layouts.get(1)));

    MultiShapeLayout multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes);

    //Setting this to null to bypass complaints about image possibly not being initialized...
    BufferedImage image = null;
    try {
      image = ImageIO.read(sourceImage);
    } catch (Exception e) {
      fail("Unable to read the watermark image attachment");
    }
    Dimension imageDimension = Dimension.dimension(image.getWidth(), image.getHeight());
    List<Tuple<Dimension, AbsolutePositionLayoutSpec>> watermarkShapes =
        new ArrayList<Tuple<Dimension, AbsolutePositionLayoutSpec>>();
    AbsolutePositionLayoutSpec layout = Serializer.absolutePositionLayoutSpec((
            JsonParser.parseString(
                "{\"horizontalCoverage\":0.1,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},"
                + "\"offset\":{\"y\":0,\"x\":0},\"reference\":{\"left\":0.010,\"top\":0.01}}}").getAsJsonObject()));
    watermarkShapes.add(0, Tuple.tuple(imageDimension, layout));
    MultiShapeLayout watermarkLayout = LayoutManager.absoluteMultiShapeLayout(outputDimension,
            watermarkShapes);
    Optional<LaidOutElement<Attachment>> watermarkOption = Optional.of(new LaidOutElement<Attachment>(
        watermarkImageAttachment, watermarkLayout.getShapes().get(0)));

    LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(0));
    LaidOutElement<Track> upperLaidOutElement = new LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.getShapes()
            .get(1));

    Job composite = composerService.composite(outputDimension, Optional.ofNullable(lowerLaidOutElement),
        upperLaidOutElement, watermarkOption, "composite.work", "black", "upper");
    Track compositeTrack = (Track) MediaPackageElementParser.getFromXml(composite.getPayload());
    Assert.assertNotNull(compositeTrack);
    inspectedTrack.setIdentifier(compositeTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, compositeTrack);
  }

  @Test
  public void testRemapNames() throws Exception {
    long jobId = 1234567;
    final String[] outFiles = { "/master.m3u8", "/variant_0.m3u8", "/variant_1.m3u8", "/segment_0.mp4",
        "/segment_1.mp4" };
    List<File> testFiles = new ArrayList<File>();
    for (String of : outFiles) {
      File testFile = getFile(of);
      File f = new File(FilenameUtils.concat(testDir.getAbsolutePath(), testFile.getName()));
      FileUtils.copyFile(testFile, f);
      testFiles.add(f);
    }
    composerService.hlsFixReference(jobId, testFiles);
    for (int i = 0; i < 3; i++) {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(testFiles.get(i)));
      Stream<String> stream = bufferedReader.lines();
      stream.filter(line -> !line.startsWith("#") && !line.trim().isEmpty()).forEach(line -> {
        Assert.assertTrue(line.startsWith("1234567"));
      });
      stream = bufferedReader.lines();
      stream.filter(line -> line.startsWith("#EXT-X-MAP:URI=")).forEach(line -> {
        Assert.assertTrue(line.startsWith("#EXT-X-MAP:URI=1234567_"));
      });
      bufferedReader.close();
    }
  }

  /**
   * Test method for {@link ComposerServiceImpl#concat(String, Dimension, boolean, Track...)}
   */
  @Test
  public void testConcat() throws Exception {
    Dimension outputDimension = new Dimension(500, 500);
    Job concat = composerService.concat("concat.work", outputDimension, false, sourceVideoTrack, sourceVideoTrack);
    Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concat.getPayload());
    Assert.assertNotNull(concatTrack);
    inspectedTrack.setIdentifier(concatTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, concatTrack);
  }

  /**
   * Test method for {@link ComposerServiceImpl#concat(String, Dimension, float, boolean, Track...)}
   */
  @Test
  public void testConcatWithFrameRate() throws Exception {
    Dimension outputDimension = new Dimension(500, 500);
    Job concat = composerService.concat("concat.work", outputDimension, 20.0f, false, sourceVideoTrack,
        sourceVideoTrack);
    Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concat.getPayload());
    Assert.assertNotNull(concatTrack);
    inspectedTrack.setIdentifier(concatTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, concatTrack);
  }

  /**
   * Test method for {@link ComposerServiceImpl#concat(String, Dimension, float, boolean, Track...)}
  */
  @Test
  public void testConcatWithSameCodec() throws Exception {
    Job concat = composerService.concat("concat.work", null, true, sourceVideoTrack, sourceVideoTrack);
    Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concat.getPayload());
    Assert.assertNotNull(concatTrack);
    inspectedTrack.setIdentifier(concatTrack.getIdentifier());
    inspectedTrack.setMimeType(MimeType.mimeType("video", "mp4"));
    Assert.assertEquals(inspectedTrack, concatTrack);
  }

  /**
   * Test method for
   * {@link org.opencastproject.composer.impl.ComposerServiceImpl#imageToVideo(Attachment, String, double)}
   */
  @Test
  public void testImageToVideo() throws Exception {
    if (!ffmpegInstalled) {
      return;
    }

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
