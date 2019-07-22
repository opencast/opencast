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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
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
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
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

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComplexCmdsEncoderEngineTest.class);
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
    return new File(ComplexCmdsEncoderEngineTest.class.getResource(path).toURI());
  }

  @Before
  public void setUp() throws Exception {
    // Skip tests if FFmpeg is not installed
    Assume.assumeTrue(ffmpegInstalled);
    engine = new EncoderEngine(FFMPEG_BINARY);

    File f = getFile("/video.mp4");
    sourceVideoOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp4", workingDirectory);
    FileUtils.copyFile(f, sourceVideoOnly);

    // Create another audio only file
    f = getFile("/audio.mp3");
    sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp3", workingDirectory);
    FileUtils.copyFile(f, sourceAudioOnly);

    f = getFile("/audiovideo.mov");
    sourceAudioVideo = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mov", workingDirectory);
    FileUtils.copyFile(f, sourceAudioVideo);

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

    workspace = EasyMock.createNiceMock(Workspace.class);
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

    EasyMock.expect(workspace.get((URI) EasyMock.anyObject(), anyBoolean())).andAnswer(new IAnswer<File>() {
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
        return null;
      }

      @Override
      public Job enrich(MediaPackageElement original, boolean override, Map<String, String> options)
              throws MediaInspectionException, MediaPackageException {
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

    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    final Capture<String> type = EasyMock.newCapture();
    final Capture<String> operation = EasyMock.newCapture();
    final Capture<List<String>> args = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
            .andAnswer(() -> {
              Job job = new JobImpl(0);
              job.setJobType(type.getValue());
              job.setOperation(operation.getValue());
              job.setArguments(args.getValue());
              job.setPayload(composerService.process(job));
              return job;
            }).anyTimes();
    EasyMock.replay(serviceRegistry);

    // Create and populate the composer service
    composerService = new ComposerServiceImpl() {
      @Override
      protected List<Track> inspect(Job job, List<URI> uris) throws EncoderException {
        List<Track> tracks = new ArrayList<Track>(uris.size());
        uris.forEach(uri -> {
          tracks.add(inspectedTrack);
        });
        return tracks;
      }
    };

    IncidentService incidents = EasyMock.createNiceMock(IncidentService.class);
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
      return;
  }


  @Test
  public void testConcatEdit() throws Exception {
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
  }

  @Test
  public void testConcatEdit2segments() throws Exception {
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
  }

  // When edit points are out of order in the SMIL
  @Test
  public void testConcatEditReorderSegments() throws Exception {
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
  }

  // Single input, Single output, No Filter
  @Test
  public void testConcatEditAudioNoSplitNoFilter() throws Exception {
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
  }

  // Single input, Two outputs, No Edit, No transition
  @Test
  public void testmultiTrimConcatNoEdit() throws Exception {
    URL sourceUrl = getClass().getResource("/audiovideo.mov"); // Video Only
    File sourceFile1 = new File(workingDirectory, "audiovideo.mov");
    FileUtils.copyURLToFile(sourceUrl, sourceFile1);
    EncodingProfile[] eprofiles = { profileScanner.getProfile("h264-low.http"),
            profileScanner.getProfile("h264-medium.http")};
    File[] files = { sourceFile1 };
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(files), null, Arrays.asList(eprofiles), 0, true, true);
    assertTrue(outputs.size() == eprofiles.length);
    for (int i = 0; i < eprofiles.length; i++)
      assertTrue(outputs.get(i).length() > 0);
  }

  @Test
  public void testRawMultiEncode() throws EncoderException {
    if (!ffmpegInstalled)
      return;
    List<EncodingProfile> profiles = new ArrayList<EncodingProfile>();
    profiles.add(profileScanner.getProfile("h264-low.http"));
    profiles.add(profileScanner.getProfile("flash.rtmp"));
    profiles.add(profileScanner.getProfile("h264-medium.http"));
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(sourceAudioVideo), null, profiles, 0, true, true);
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
    // create encoder process.
    // no special working dir is set which means the working dir of the
    // current java process is used
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(sourceAudioVideo), null, profiles, 0, true, false);
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
    List<File> outputs = engine.multiTrimConcat(Arrays.asList(sourceAudioVideo), null, profiles, 0, true, false);
    assertTrue(outputs.size() == profiles.size());
    for (int i = 0; i < profiles.size(); i++) {
      assertTrue(outputs.get(i).exists());
      assertTrue(outputs.get(i).length() > 0);
    }
  }

  @Test
  public void testMultiEncodeJob() throws Exception {
    if (!ffmpegInstalled)
      return;
    String[] profiles = { "h264-low.http", "flash.rtmp", "h264-medium.http" };
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(
            IOUtils.toString(ComposerServiceTest.class.getResourceAsStream("/composer_test_source_track_video.xml"),
                    Charset.defaultCharset()));

    Job multiencode = composerService.multiEncode(sourceTrack, Arrays.asList(profiles));
    @SuppressWarnings("unchecked")
    List<Track> processedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(multiencode.getPayload());
    Assert.assertNotNull(processedTracks);
    Assert.assertEquals(profiles.length, processedTracks.size()); // Same number of outputs as profiles
    for (Track processedTrack : processedTracks) {
      Assert.assertNotNull(processedTrack.getIdentifier());
    }
  }
}
