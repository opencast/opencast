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

import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
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
import org.opencastproject.util.MimeType;
import org.opencastproject.util.StreamHelper;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Tests the {@link ComposerServiceImpl}.
 */

public class ComplexCmdsEncoderEngineTest {
  /** The sources file to test with */
  private File sourceVideoOnly = null;
  private File sourceAudioOnly = null;
  private File sourceAudioVideo = null;
  private File sourceMuxed = null;
  private Job job = null;
  private Workspace workspace = null;

  /** encoding profiles to use */
  private final String multiProfile = "demux.work";
  private ArrayList<String> multiProfiles = new ArrayList<String>();
  /** The composer service to test */
  private ComposerServiceImpl composerService = null;
  private EncoderEngine engine = null;
  /** The service registry for job dispatching */
  private ServiceRegistry serviceRegistry = null;

  /** File pointer to the testing dir */
  private static File workingDirectory = new File("target");

  /** FFmpeg binary location and engine */
  private static final String FFMPEG_BINARY = "ffmpeg";

  /** True to run the tests */
  private static boolean ffmpegInstalled = true;

  /** True to run the tests */
  private static boolean ffmpegInstalledGreaterVersion2 = false;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComplexCmdsEncoderEngineTest.class);
  private Track inspectedTrack;

  /** Encoding profile scanner */
  private EncodingProfileScanner profileScanner;

  @BeforeClass
  public static void testForFFmpeg() {
    StreamHelper stderr = null;
    StreamHelper stdout = null;
    Process p = null;
    try {
      p = new ProcessBuilder(FFMPEG_BINARY, "-version").start();
      StringBuffer buffer = new StringBuffer();
      stdout = new StreamHelper(p.getInputStream(), buffer);
      stderr = new StreamHelper(p.getErrorStream());
      int status = p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      logger.info(buffer.toString());
      if (status != 0)
        throw new IllegalStateException();
      String ffmpegOutput = buffer.toString();
      logger.info(ffmpegOutput);
      if (ffmpegOutput.startsWith("ffmpeg version ") && Integer.parseInt(ffmpegOutput.substring(15, 16)) > 1)
        ffmpegInstalledGreaterVersion2 = true;
    } catch (Throwable t) {
      logger.warn("Skipping processSmil composer service tests due to unsatisifed or erroneus ffmpeg installation");
      ffmpegInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  private static File getFile(final String path) throws Exception {
    return new File(ComplexCmdsEncoderEngineTest.class.getResource(path).toURI());
  }

  @Before
  public void setUp() throws Exception {
    if (!ffmpegInstalledGreaterVersion2)
      return;
    BasicConfigurator.configure();
    engine = new EncoderEngine(FFMPEG_BINARY);

    // Copy an existing media file to a temp file
    File f = getFile("/muxed_15.avi");
    sourceMuxed = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".avi", workingDirectory);
    FileUtils.copyFile(f, sourceMuxed);


    f = getFile("/video.mp4");
    sourceVideoOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp4", workingDirectory);
    FileUtils.copyFile(f, sourceVideoOnly);

    // Create another audio only file f = getFile("/audio.mp3"); sourceAudioOnly =
    f = getFile("/audio.mp3");
    sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp3", workingDirectory);
    FileUtils.copyFile(f, sourceAudioOnly);

    f = getFile("/audiovideo.mov");
    sourceAudioVideo = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mov", workingDirectory);
    FileUtils.copyFile(f, sourceAudioVideo);

    multiProfiles.add(multiProfile);
    // create the needed mocks
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty((String) EasyMock.anyObject())).andReturn(FFMPEG_BINARY);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getId()).andReturn((long) 123456789).anyTimes();
    EasyMock.replay(job);

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

    /* Workspace */ workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        URI uri = (URI) EasyMock.getCurrentArguments()[0];
        String name = uri.getPath();
        logger.info("workspace Returns " + name);
        if (name.contains("mux"))
          return sourceMuxed;
        else if (name.contains("audiovideo"))
          return sourceAudioVideo;
        else if (name.contains("audio"))
          return sourceAudioOnly;
        else if (name.contains("video"))
          return sourceVideoOnly;
        return sourceAudioVideo; // default
      }
    }).anyTimes();

    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
                      @Override
                      public URI answer() throws Throwable {
                        File f = new File(workingDirectory, (String) EasyMock.getCurrentArguments()[1]);
                        FileOutputStream out = new FileOutputStream(f);
                        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
                        IOUtils.copy(in, out);
                        return (f.toURI());
                      }
                    }).anyTimes();

    profileScanner = new EncodingProfileScanner();
    File encodingProfile = getFile("/encodingprofiles.properties");
    Assert.assertNotNull("Encoding profile must exist", encodingProfile);
    profileScanner.install(encodingProfile);

    MediaInspectionService inspectionService = new MediaInspectionService() {
      @Override
      public Job inspect(URI workspaceURI) throws MediaInspectionException {
        Job inspectionJob = EasyMock.createNiceMock(Job.class);
        EasyMock.expect(inspectionJob.getStatus()).andReturn(Status.FINISHED).anyTimes();
        try {
          EasyMock.expect(inspectionJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(inspectedTrack));
        } catch (MediaPackageException e) {
          throw new RuntimeException(e);
        }
        EasyMock.replay(inspectionJob);
        return inspectionJob;
      }

      @Override
      public Job enrich(MediaPackageElement original, boolean override) throws MediaInspectionException,
      MediaPackageException {
        return null;
      }

      @Override
      public Job inspect(URI uri, Map<String, String> options) throws MediaInspectionException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Job enrich(MediaPackageElement original, boolean override, Map<String, String> options)
              throws MediaInspectionException, MediaPackageException {
        // TODO Auto-generated method stub
        return null;
      }
    };

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace);

    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + " <track xmlns=\"http://mediapackage.opencastproject.org\" type='presentation/source'"
            + " id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>" + " <mimetype>video/avi</mimetype>"
            + " <url>muxed.avi</url>" + "       </track>";
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

      @Override
      protected boolean inspectionSuccessful(JobBarrier any, String reason) {
        return (true);
      }
    };

    IncidentService incidents = EasyMock.createNiceMock(IncidentService.class);
    serviceRegistry = new ServiceRegistryInMemoryImpl(composerService, securityService, userDirectory, orgDirectory, incidents);
    composerService.setOrganizationDirectoryService(orgDirectory);
    composerService.setSecurityService(securityService);
    composerService.setServiceRegistry(serviceRegistry);
    composerService.setUserDirectoryService(userDirectory);
    composerService.setProfileScanner(profileScanner);
    composerService.setWorkspace(workspace);
    composerService.setMediaInspectionService(inspectionService);
  }

  @After
  public void tearDown() throws Exception {
    if (!ffmpegInstalledGreaterVersion2)
      return;
  }

  @Test
  public void testRawMultiEncode() throws EncoderException {
    if (!ffmpegInstalled)
      return;
    // EncodingProfile profile = profileScanner.getProfile(multiProfile);
    List<EncodingProfile> profiles = new ArrayList<EncodingProfile>();
    profiles.add(profileScanner.getProfile("h264-low.http"));
    profiles.add(profileScanner.getProfile("flash.rtmp"));
    profiles.add(profileScanner.getProfile("h264-medium.http"));
    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceAudioVideo.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceAudioVideo.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    params.put("out.name", "test");
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    List<File> outputs = engine.multiOutputCmd(sourceAudioVideo, profiles, params, true, true);
    assertTrue(outputs.size() == profiles.size());
    for (int i = 0; i < profiles.size(); i++) {
      assertTrue(outputs.get(i).exists());
      assertTrue(outputs.get(i).length() > 0);
    }
  }

  @Test
  public void testRawMultiEncodeNoAudio() throws EncoderException {
    if (!ffmpegInstalled)
      return;
    // EncodingProfile profile = profileScanner.getProfile(multiProfile);
    List<EncodingProfile> profiles = new ArrayList<EncodingProfile>();
    profiles.add(profileScanner.getProfile("h264-low.http"));
    profiles.add(profileScanner.getProfile("flash.rtmp"));
    profiles.add(profileScanner.getProfile("h264-medium.http"));
    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceVideoOnly.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceVideoOnly.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    params.put("out.name", "test");
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    List<File> outputs = engine.multiOutputCmd(sourceAudioVideo, profiles, params, true, false);
    assertTrue(outputs.size() == profiles.size());
    for (int i = 0; i < profiles.size(); i++) {
      assertTrue(outputs.get(i).exists());
      assertTrue(outputs.get(i).length() > 0);
    }
  }

  @Test
  public void testMultiEncodeSingleProfile() throws Exception {
    if (!ffmpegInstalled)
      return;
    assertTrue(sourceAudioVideo.isFile());
    // Set up workspace
    List<EncodingProfile> profiles = new ArrayList<EncodingProfile>();
    profiles.add(profileScanner.getProfile("h264-low.http"));
    // Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceAudioVideo.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceAudioVideo.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    params.put("out.name", "test");
    List<File> outputs = engine.multiOutputCmd(sourceAudioVideo, profiles, params, true, true);
    assertTrue(outputs.size() == profiles.size());
    for (int i = 0; i < profiles.size(); i++) {
      assertTrue(outputs.get(i).exists());
      assertTrue(outputs.get(i).length() > 0);
    }
  }

  @Test
  public void testMultiEncode() throws Exception {
    if (!ffmpegInstalled)
      return;
    assertTrue(sourceAudioVideo.isFile());
    // Set up workspace
    String[] profiles = { "h264-low.http", "flash.rtmp", "h264-medium.http" };
    // Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceAudioVideo.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceAudioVideo.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    params.put("out.name", "test");
    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<track xmlns=\"http://mediapackage.opencastproject.org\" type='presentation/source'"
            + " id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "<audio><bitrate>1920</bitrate></audio>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video>"
            + "       <mimetype>video/avi</mimetype>" + "       <url>audiovideo.mov</url>" + "       </track>";
    Track sourceTrackVideo = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    try {
      List<Track> tracks = composerService.multiEncode(job, sourceTrackVideo, Arrays.asList(profiles), params);
      for (Track processedTrack : tracks) {
        Assert.assertNotNull(processedTrack.getIdentifier());
        logger.debug("encode Tracks " + processedTrack.getIdentifier() + "  = " + processedTrack.getMimeType());
        // Assert.assertEquals(processedTrack.getMimeType(), MimeType.mimeType("video", "mp4"));
      }
    } catch (EncoderException e) {
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.composer.impl.ComposerServiceImpl#processSmil(org.opencastproject.smil.entity.api.Smil, String[])}
   */
  @Test
  public void testMultiEncodeJob() throws Exception {
    if (!ffmpegInstalledGreaterVersion2)
      return;
    String[] profiles = { "h264-low.http", "flash.rtmp", "h264-medium.http" };
    String sourceTrack1Xml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<track xmlns=\"http://mediapackage.opencastproject.org\" type='presentation/source'"
            + " id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "<duration>7000</duration>"
            + "<mimetype>video/mpeg</mimetype>" + "<url>video.mp4</url>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrack1Xml);

    Job multiencode = composerService.multiEncode(sourceTrack, Arrays.asList(profiles));

    JobBarrier barrier = new JobBarrier(null, serviceRegistry, multiencode);

    if (!barrier.waitForJobs().isSuccess()) {
      Assert.fail("multiEncode job did not success!");
    }
    @SuppressWarnings("unchecked")
    List<Track> processedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(multiencode.getPayload());
    Assert.assertNotNull(processedTracks);
    Assert.assertEquals(profiles.length, processedTracks.size());
    for (Track processedTrack : processedTracks) {
      Assert.assertNotNull(processedTrack.getIdentifier());
      // Assert.assertEquals(processedTrack.getMimeType(), MimeType.mimeType("video", "mp4"));
    }

  }

  @Test
  public void testDemux() throws Exception {
    if (!ffmpegInstalled)
      return;
    assertTrue(sourceMuxed.isFile());
    // build a single media package to test with
    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "       <track xmlns=\"http://mediapackage.opencastproject.org\" type='presentation/source'"
            + "       id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>muxed.avi</url>" + "       </track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    Job demux = composerService.demux(sourceTrack, "demux.work");
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, demux);

    if (!barrier.waitForJobs().isSuccess()) {
      Assert.fail("multiEncode job did not success!");
    }
    @SuppressWarnings("unchecked")
    List<Track> processedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(demux.getPayload());
    Assert.assertNotNull(processedTracks);
    Assert.assertEquals(2, processedTracks.size());
    for (Track processedTrack : processedTracks) {
      Assert.assertNotNull(processedTrack.getIdentifier());
      Assert.assertEquals(processedTrack.getMimeType(), MimeType.mimeType("video", "mp4"));
    }
  }

  @Test
  public void testRawDemux() throws EncoderException {
    if (!ffmpegInstalled)
      return;
    // EncodingProfile profile = profileScanner.getProfile(multiProfile);
    EncodingProfile profile = profileScanner.getProfile(multiProfile);
    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceMuxed.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceMuxed.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    params.put("out.name", "test");
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    List<File> outputs = engine.demux(sourceMuxed, profile, params);
    assertTrue(outputs.size() == 2);
    for (int i = 0; i < 2; i++) {
      assertTrue(outputs.get(i).exists());
      assertTrue(outputs.get(i).length() > 0);
    }
  }

  @Test
  public void testConcatEdit() throws Exception {
    if (!ffmpegInstalled)
      return;
    URL sourceUrl = getClass().getResource("/audiovideo.mov");
    File sourceFile1 = new File(workingDirectory, "audiovideo.mov");
    FileUtils.copyURLToFile(sourceUrl, sourceFile1);
    URL sourceUrl1 = sourceUrl;
    File sourceFile2 = sourceFile1;
    FileUtils.copyURLToFile(sourceUrl1, sourceFile2);
    EncodingProfile[] eprofiles = { profileScanner.getProfile("h264-low.http"),
            profileScanner.getProfile("h264-medium.http"), profileScanner.getProfile("h264-large.http"),
            profileScanner.getProfile("flash.rtmp") };

    File[] files = { sourceFile1, sourceFile2 };

    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceFile1.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceFile1.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    ArrayList<Long> edits = new ArrayList<Long>();
    edits.add((long) 0);
    edits.add((long) 1700);
    edits.add((long) 9000);
    edits.add((long) 1);
    edits.add((long) 1500);
    edits.add((long) 7500);
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(files), edits, Arrays.asList(eprofiles), 2000); // Concat
                                                                                                              // 2
    // input files
    // into 2 output
    // formats
    assertTrue(outputs.size() == eprofiles.length);
    for (int i = 0; i < eprofiles.length; i++) {
      assertTrue(outputs.get(i).exists());
      assertTrue(outputs.get(i).length() > 0);
    }
    // TODO: inspect the file
  }

  @Test
  public void testConcatEdit2segments() throws Exception {
    if (!ffmpegInstalled)
      return;
    logger.info("testConcatEdit2segment");
    URL sourceUrl = getClass().getResource("/audiovideo.mov");
    File sourceFile1 = new File(workingDirectory, "audiovideo.mov");
    FileUtils.copyURLToFile(sourceUrl, sourceFile1);
    URL sourceUrl1 = sourceUrl; // getClass().getResource("/slidechanges.mov");
    File sourceFile2 = sourceFile1; // new File(workingDirectory, "slidechanges.mov");
    FileUtils.copyURLToFile(sourceUrl1, sourceFile2);
    EncodingProfile[] eprofiles = { profileScanner.getProfile("h264-low.http"),
            profileScanner.getProfile("h264-medium.http"), profileScanner.getProfile("h264-large.http") };

    File[] files = { sourceFile1, sourceFile2 };

    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceFile1.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceFile1.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    ArrayList<Long> edits = new ArrayList<Long>();
    edits.add((long) 0);
    edits.add((long) 0);
    edits.add((long) 2500); // These 2 edits will be merged
    edits.add((long) 0);
    edits.add((long) 3000);
    edits.add((long) 5500);
    edits.add((long) 1);
    edits.add((long) 8000);
    edits.add((long) 10500);
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(files), edits, Arrays.asList(eprofiles), 1000); // Concat
                                                                                                              // 2
    // input files
    // into 2 output
    // formats
    assertTrue(outputs.size() == eprofiles.length);
    for (int i = 0; i < eprofiles.length; i++)
      assertTrue(outputs.get(i).length() > 0);
    // TODO: inspect the file
  }

  // When edit points are out of order in the SMIL
  @Test
  public void testConcatEditReorderSegments() throws Exception {
    if (!ffmpegInstalled)
      return;
    URL sourceUrl = getClass().getResource("/audiovideo.mov");
    File sourceFile1 = new File(workingDirectory, "audiovideo.mov");
    FileUtils.copyURLToFile(sourceUrl, sourceFile1);
    EncodingProfile[] eprofiles = { profileScanner.getProfile("h264-low.http"),
            profileScanner.getProfile("h264-medium.http") };
    File[] files = { sourceFile1 };

    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceFile1.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceFile1.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    ArrayList<Long> edits = new ArrayList<Long>();
    edits.add((long) 0);
    edits.add((long) 0);
    edits.add((long) 2500);
    edits.add((long) 0); // This is out of order
    edits.add((long) 8000);
    edits.add((long) 10500);
    edits.add((long) 0);
    edits.add((long) 3000);
    edits.add((long) 5500);
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(files), edits, Arrays.asList(eprofiles), 0);
    assertTrue(outputs.size() == eprofiles.length);
    for (int i = 0; i < eprofiles.length; i++)
      assertTrue(outputs.get(i).length() > 0);
  }

  // Single input, Single output, Filter
  @Test
  public void testConcatEditVideoNoSplitFilter() throws Exception {
    if (!ffmpegInstalled)
      return;
    URL sourceUrl = getClass().getResource("/video.mp4"); // Video Only
    File sourceFile1 = new File(workingDirectory, "video.mp4");
    FileUtils.copyURLToFile(sourceUrl, sourceFile1);

    EncodingProfile[] eprofiles = { profileScanner.getProfile("h264-low.http") }; // ,
                                                                                  // profileScanner.getProfile("h264-medium.http")
                                                                                  // };

    File[] files = { sourceFile1 };

    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceFile1.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceFile1.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    ArrayList<Long> edits = new ArrayList<Long>();
    edits.add((long) 0);
    edits.add((long) 0);
    edits.add((long) 2500); // These 2 edits will be merged
    edits.add((long) 0);
    edits.add((long) 3000);
    edits.add((long) 5500);
    edits.add((long) 0);
    edits.add((long) 9000);
    edits.add((long) 17500);
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(files), edits, Arrays.asList(eprofiles), 1000, true,
            false); // Video
    // Only
    assertTrue(outputs.size() == eprofiles.length);
    for (int i = 0; i < eprofiles.length; i++)
      assertTrue(outputs.get(i).length() > 0);
    // TODO: inspect the file
  }

  // Single input, Single output, No Filter
  @Test
  public void testConcatEditAudioNoSplitNoFilter() throws Exception {
    if (!ffmpegInstalled)
      return;
    URL sourceUrl = getClass().getResource("/audio.mp3"); // Video Only
    File sourceFile1 = new File(workingDirectory, "audio.mp3");
    FileUtils.copyURLToFile(sourceUrl, sourceFile1);
    EncodingProfile[] eprofiles = { profileScanner.getProfile("mp3audio.http") };

    File[] files = { sourceFile1 };
    Map<String, String> params = new HashMap<String, String>();
    String outDir = sourceFile1.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(sourceFile1.getName());
    params.put("out.file.basename", outFileName);
    params.put("out.dir", outDir);
    ArrayList<Long> edits = new ArrayList<Long>();
    edits.add((long) 0);
    edits.add((long) 0);
    edits.add((long) 5500); // in ms
    edits.add((long) 0);
    edits.add((long) 9000);
    edits.add((long) 17500);
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(files), edits, Arrays.asList(eprofiles), 0, false, true); // Audio
    assertTrue(outputs.size() == eprofiles.length);
    for (int i = 0; i < eprofiles.length; i++)
      assertTrue(outputs.get(i).length() > 0);
    // TODO: inspect the file
  }
}
