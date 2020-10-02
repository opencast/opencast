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

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
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
import org.opencastproject.serviceregistry.api.Incidents;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilBody;
import org.opencastproject.smil.entity.api.SmilHead;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Tests the {@link ComposerServiceImpl}.
 */

public class ProcessSmilTest {
  /** The sources file to test with */
  private File videoOnly = null;
  private File audioOnly = null;
  private File sourceAudioVideo1 = null;
  private File sourceAudioVideo2 = null;
  private Job job = null;
  private File workingDirectory = null;

  /** The composer service to test */
  private ComposerServiceImpl composerService = null;

  /** The service registry for job dispatching */
  private ServiceRegistry serviceRegistry = null;

  /** FFmpeg binary location */
  private static final String FFMPEG_BINARY = "ffmpeg";

  /** True to run the tests */
  private static boolean ffmpegInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ProcessSmilTest.class);
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

  @Before
  public void setUp() throws Exception {
 // Skip tests if FFmpeg is not installed

    Assume.assumeTrue(ffmpegInstalled);
    workingDirectory = FileSupport.getTempDirectory("processSmiltest");
    FileUtils.forceMkdir(workingDirectory);

    // Copy an existing media file to a temp file
    File f = new File("src/test/resources/av1.mp4");
    sourceAudioVideo1 = new File(workingDirectory, "av1.mp4");
    FileUtils.copyFile(f, sourceAudioVideo1);
    f = null;

    f = new File("src/test/resources/audiovideo.mov");
    sourceAudioVideo2 = new File(workingDirectory, "av2.mov");
    FileUtils.copyFile(f, sourceAudioVideo2);
    f = null;

    f = new File("src/test/resources/video.mp4");
    videoOnly = new File(workingDirectory, "video.mp4");
    FileUtils.copyFile(f, videoOnly);
    f = null;

    f = new File("src/test/resources/audio.mp3");
    audioOnly = new File(workingDirectory, "audio.mp3");
    FileUtils.copyFile(f, audioOnly);
    f = null;

    // create the needed mocks
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty((String) EasyMock.anyObject())).andReturn(FFMPEG_BINARY);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getId()).andReturn((long) 123456789).anyTimes();
    EasyMock.replay(job);

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
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        URI uri = (URI) EasyMock.getCurrentArguments()[0];
        String name = uri.getPath();
        logger.info("workspace Returns " + name);
        if (name.contains("av2"))
          return sourceAudioVideo2;
        if (name.contains("audio"))
          return audioOnly;
        else if (name.contains("video"))
          return videoOnly;
        return sourceAudioVideo1; // default
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
    File encodingProfile = new File("src/test/resources/encodingprofiles.properties");
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


    String sourceTrackXml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<track xmlns=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\" id=\"f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a\">\n"
            + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>";
    inspectedTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);

    // Create and populate the composer service
    composerService = new ComposerServiceImpl() {
      @Override
      protected List<Track> inspect(Job job, List<URI> uris) throws EncoderException {
        List<Track> results = new ArrayList<>(uris.size());
        uris.forEach(uri -> {
          results.add(inspectedTrack);
        });
        return results;
      }
    };

    IncidentService incidentService = EasyMock.createNiceMock(IncidentService.class);

    serviceRegistry = EasyMock.createMock(ServiceRegistry.class);     // To quiet the warnings
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
    EasyMock.expect(serviceRegistry.incident()).andAnswer(() -> {
        Incidents incidents = new Incidents(serviceRegistry, incidentService);
        return incidents;
    }).anyTimes();
    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace, incidentService, serviceRegistry);
    composerService.setServiceRegistry(serviceRegistry);
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
    FileUtils.deleteQuietly(sourceAudioVideo1);
    FileUtils.deleteQuietly(sourceAudioVideo2);
    FileUtils.deleteQuietly(audioOnly);
    FileUtils.deleteQuietly(videoOnly);
    FileUtils.forceDelete(workingDirectory);
  }

  // Convenience function to mock a param
  SmilMediaParam mockSmilMediaParam(String name, String value, String id) {
    SmilMediaParam param = EasyMock.createNiceMock(SmilMediaParam.class);
    EasyMock.expect(param.getName()).andReturn(name).anyTimes();
    EasyMock.expect(param.getValue()).andReturn(value).anyTimes();
    EasyMock.expect(param.getId()).andReturn(id).anyTimes();
    EasyMock.replay(param);
    return (param);
  }

  SmilMediaElement mockSmilMediaElement(URI src, Long clipBeginMS, Long clipEndMS, String paramGroupId)
          throws SmilException {
    SmilMediaElement elem = EasyMock.createNiceMock(SmilMediaElement.class);
    EasyMock.expect(elem.getParamGroup()).andReturn(paramGroupId).anyTimes();
    EasyMock.expect(elem.getSrc()).andReturn(src).anyTimes();
    EasyMock.expect(elem.isContainer()).andReturn(false).anyTimes();
    EasyMock.expect(elem.getClipBeginMS()).andReturn(clipBeginMS).anyTimes();
    EasyMock.expect(elem.getClipEndMS()).andReturn(clipEndMS).anyTimes();
    EasyMock.replay(elem);
    return (elem);
  }

  @Test
  public void testProcessSmilOneSegment() throws Exception {
    logger.info("testProcessSmilOneSegment");
    assertTrue(sourceAudioVideo1.isFile());
    String smil1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
            + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
            + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
            + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
            + "<param value='audiovideo1.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
            + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
            + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
            + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
            + "<param value='audiovideo2.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
            + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
            + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
            + "<video src='video.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='video.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>";
    // build a single media package to test with
    try {
      String paramGroupId = "pg-54da9288-36c0-4e9c-87a1-adb30562b814";

      List<SmilMediaParam> params1 = new ArrayList<>();
      params1.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
      params1.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
              "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
      params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));

      SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
      EasyMock.expect(group1.getParams()).andReturn(params1).anyTimes();
      EasyMock.expect(group1.getId()).andReturn(paramGroupId).anyTimes();
      EasyMock.replay(group1);

      List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
      paramGroups.add(group1);

      SmilHead head = EasyMock.createNiceMock(SmilHead.class);
      EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
      EasyMock.replay(head);

      SmilMediaElement sme1 = mockSmilMediaElement(sourceAudioVideo1.toURI(), 1000L, 5000L, paramGroupId);
      SmilMediaElement sme2 = mockSmilMediaElement(sourceAudioVideo1.toURI(), 1000L, 5000L,
              "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a");

      List<SmilMediaObject> objects = new ArrayList<>();
      objects.add(sme1);
      objects.add(sme2);

      SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
      EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
      EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
      EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
      EasyMock.replay(objectContainer);

      List<SmilMediaObject> containerObjects = new ArrayList<>();
      containerObjects.add(objectContainer);

      SmilBody body = EasyMock.createNiceMock(SmilBody.class);
      EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
      EasyMock.replay(body);

      Smil smil = EasyMock.createNiceMock(Smil.class);
      EasyMock.expect(smil.get(paramGroupId)).andReturn(group1).anyTimes();
      EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
      EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
      EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes();
      EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
      EasyMock.replay(smil);

      SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
      EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
      EasyMock.replay(response);

      SmilService smilService = EasyMock.createNiceMock(SmilService.class);
      EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
      EasyMock.replay(smilService);
      composerService.setSmilService(smilService);

      List<String> encodingProfiles = Arrays.asList("h264-low.http");
      Job job = composerService.processSmil(smil, paramGroupId, "0", encodingProfiles);
      List<Track> outputs = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
      assertNotNull(outputs);
      for (Track track : outputs) {
        logger.info("testProcessOneTrack got file:", track.getDescription());
      }
      assertTrue(outputs.size() == 1); // One for each profile
    } catch (EncoderException e) {
      // assertTrue("test complete".equals(e.getMessage()));
    }
  }

  @Test
  public void testProcessSmilVideoOnly() throws Exception {
    assertTrue(videoOnly.isFile());
    logger.info("testProcessSmilVideoOnly");
    String smil1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
            + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
            + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
            + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
            + "<param value='videoonly.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
            + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
            + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
            + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
            + "<param value='audio.mp3' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
            + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
            + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
            + "<video src='videoonly.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='audio.mp3' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>";
    // build a single media package to test with
    String paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814";
    String paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a";
    List<SmilMediaParam> params = new ArrayList<>();
    params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params.add(mockSmilMediaParam("track-src", "file:" + videoOnly.getPath(),
            "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    SmilMediaElement sme1 = mockSmilMediaElement(videoOnly.toURI(), 1000L, 5000L, paramGroupId1); // Only doing group1
    SmilMediaElement sme2 = mockSmilMediaElement(audioOnly.toURI(), 1000L, 5000L, paramGroupId2);

    List<SmilMediaObject> objects = new ArrayList<>();
    objects.add(sme1);
    objects.add(sme2);

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId1)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);
    List<String> encodingProfiles = Arrays.asList("h264-low.http");
    Job job = composerService.processSmil(smil, paramGroupId1, ComposerService.VIDEO_ONLY, encodingProfiles);
    List<Track> outputs = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    // Video Only - video.mp4 has no audio track
    assertNotNull(outputs);
    logger.info("testProcessSmilOneTrack got {} files", outputs);
    for (Track track : outputs) {
      logger.info("testProcessOneTrack got file:", track.getDescription());
    }
    assertTrue(outputs.size() == 1); // One for each profile
  }

  @Test
  public void testProcessSmilAudioOnly() throws Exception {
    assertTrue(audioOnly.isFile());
    logger.info("testProcessSmilAudioOnly");
    String smil1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
            + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
            + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
            + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
            + "<param value='videoonly.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
            + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
            + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
            + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
            + "<param value='audio.mp3' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
            + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
            + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
            + "<video src='videoonly.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='audio.mp3' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>";
    // build a single media package to test with
    try {
      String paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"; // Pick the presenter flavor
      String paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a";
      List<SmilMediaParam> params1 = new ArrayList<>();
      List<SmilMediaParam> params2 = new ArrayList<>();
      params1.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
      params1.add(mockSmilMediaParam("track-src", "file:" + audioOnly.getPath(),
              "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
      params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));
      params2.add(mockSmilMediaParam("track-id", "track-2", "param-1097ff2d-431f-497c-b186-5d8f2ca6c88f"));
      params2.add(mockSmilMediaParam("track-src", "file:" + audioOnly.getPath(),
              "param-c57e9beb-a67a-4a96-96a6-9ede29e653ec"));
      params2.add(
              mockSmilMediaParam("track-flavor", "presentation/work", "param-476ade36-9193-40bb-aae3-0c1028471797"));


      SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
      EasyMock.expect(group1.getParams()).andReturn(params1).anyTimes();
      EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
      EasyMock.replay(group1);

      List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
      paramGroups.add(group1);

      SmilHead head = EasyMock.createNiceMock(SmilHead.class);
      EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
      EasyMock.replay(head);

      SmilMediaElement sme1 = mockSmilMediaElement(audioOnly.toURI(), 1000L, 5000L, paramGroupId1);
      SmilMediaElement sme2 = mockSmilMediaElement(audioOnly.toURI(), 1000L, 5000L, paramGroupId2);

      List<SmilMediaObject> objects = new ArrayList<>();
      objects.add(sme1);
      objects.add(sme2);

      SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
      EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
      EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
      EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
      EasyMock.replay(objectContainer);

      List<SmilMediaObject> containerObjects = new ArrayList<>();
      containerObjects.add(objectContainer);

      SmilBody body = EasyMock.createNiceMock(SmilBody.class);
      EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
      EasyMock.replay(body);

      Smil smil = EasyMock.createNiceMock(Smil.class);
      EasyMock.expect(smil.get(paramGroupId1)).andReturn(group1).anyTimes();
      EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
      EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
      EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes();
      EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
      EasyMock.replay(smil);

      SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
      EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
      EasyMock.replay(response);

      SmilService smilService = EasyMock.createNiceMock(SmilService.class);
      EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
      EasyMock.replay(smilService);
      composerService.setSmilService(smilService);
      List<String> encodingProfiles = Arrays.asList("mp3audio.http");
      // Let processSmil know that there is no video
      Job job = composerService.processSmil(smil, paramGroupId1, ComposerService.AUDIO_ONLY, encodingProfiles);
      List<Track> outputs = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
      // Audio Only - video.mp4 has no audio track
      assertNotNull(outputs);
      logger.info("testProcessSmilOneTrack got {} files", outputs);
      for (Track track : outputs) {
        logger.info("testProcessOneTrack got file:", track.getDescription());
      }
      assertTrue(outputs.size() == 1); // One for each profile
    } catch (EncoderException e) {
      // assertTrue("test complete".equals(e.getMessage()));
    }
  }

  @Test(expected = EncoderException.class)
  public void testProcessSmilBadProfile() throws Exception {
    String smil1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
            + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
            + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
            + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
            + "<param value='audiovideo.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
            + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
            + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
            + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
            + "<param value='video.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
            + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
            + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
            + "<video src='video.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='video.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>";

    List<String> encodingProfiles = Arrays.asList("player-preview.http", "av.work"); // Should throw exception
    // Encoding profile must support visual or audiovisual
    String paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814";
    String paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a";
    List<SmilMediaParam> params = new ArrayList<>();
    params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
            "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    objects.add(mockSmilMediaElement(videoOnly.toURI(), 1000L, 5000L, paramGroupId1));
    objects.add(mockSmilMediaElement(videoOnly.toURI(), 1000L, 5000L, paramGroupId2));

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId1)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);
    composerService.processSmil(smil, paramGroupId1, "", encodingProfiles);
  }


  @Test
  public void testProcessSmilMultiSegment() throws Exception {
    assertTrue(sourceAudioVideo1.isFile());
    assertTrue(sourceAudioVideo2.isFile());
    logger.info("testProcessSmilMultiSegment");
    String smil1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
            + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
            + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
            + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
            + "<param value='audiovideo1.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
            + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
            + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
            + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
            + "<param value='audiovideo2.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
            + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
            + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
            + "<video src='audiovideo1.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='1000ms' clipBegin='0ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='audiovideo1.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='100"
            + "0ms' clipBegin='0ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/>"
            + "</par>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e77'>"
            + "<video src='audiovideo1.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='3500ms' clipBegin='2000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='audiovideo1.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='350"
            + "0ms' clipBegin='2000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/>"
            + "</par>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e79'>"
            + "<video src='audiovideo2.mov' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a1'/>"
            + "<video src='audiovideo2.mov' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d42'/>"
            + "</par></body></smil>";
    List<String> encodingProfiles = Arrays.asList("mp3audio.http", "h264-low.http", "h264-medium.http");
    // Encoding profile must support visual or audiovisual
    String paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"; // Pick the presenter flavor
    String paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a";

    List<SmilMediaParam> params1 = new ArrayList<>();
    List<SmilMediaParam> params2 = new ArrayList<>();
    params1.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params1.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
            "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));
    params2.add(mockSmilMediaParam("track-id", "track-2", "param-1097ff2d-431f-497c-b186-5d8f2ca6c88f"));
    params2.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo2.getPath(),
            "param-c57e9beb-a67a-4a96-96a6-9ede29e653ec"));
    params2.add(mockSmilMediaParam("track-flavor", "presentation/work", "param-476ade36-9193-40bb-aae3-0c1028471797"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params1).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group1);
    SmilMediaParamGroup group2 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group2.getParams()).andReturn(params2).anyTimes();
    EasyMock.expect(group2.getId()).andReturn(paramGroupId2).anyTimes();
    EasyMock.replay(group2);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);
    paramGroups.add(group2);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    // Second track is listed in different paramGroup
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 0L, 1000L, paramGroupId1));
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 2000L, 3500L, paramGroupId1));
    objects.add(mockSmilMediaElement(sourceAudioVideo2.toURI(), 1000L, 3500L, paramGroupId1));

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId1)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.get(paramGroupId2)).andReturn(group2).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);

    Job job = composerService.processSmil(smil, paramGroupId1, null, encodingProfiles);
    Assert.assertNotNull(job.getPayload());
    assertEquals(3, MediaPackageElementParser.getArrayFromXml(job.getPayload()).size());
  }

  @Test
  public void testProcessSmilMultiTrack() throws Exception {
    assertTrue(sourceAudioVideo1.isFile());
    assertTrue(sourceAudioVideo2.isFile());
    String sourceTrack1Xml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<track xmlns=\"http://mediapackage.opencastproject.org\" type=\"presenter/source\" id=\"f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a\">\n"
            + "<duration>7000</duration>" + "<mimetype>video/mpeg</mimetype>"
            + "       <url>audiovideo1.mp4</url>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>";
    Track track1 = (Track) MediaPackageElementParser.getFromXml(sourceTrack1Xml);
    String sourceTrack2Xml = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<track xmlns=\"http://mediapackage.opencastproject.org\" type=\"presenter/source\" id=\"f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a\">\n"
            + "<duration>7000</duration>" + "  <mimetype>video/mpeg</mimetype>" + "<url>audiovideo2.mov</url>"
            + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
            + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>";
    Track track2 = (Track) MediaPackageElementParser.getFromXml(sourceTrack2Xml);
    String smil1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
            + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
            + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
            + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
            + "<param value='audiovideo1.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
            + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
            + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
            + "<param value='audiovideo2.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
            + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
            + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
            + "<video src='audiovideo1.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='3000ms' clipBegin='0ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
            + "<video src='audiovideo1.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='300"
            + "0ms' clipBegin='0ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/>" + "</par>"
            + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e79'>"
            + "<video src='audiovideo2.mov' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a1'/>"
            + "<video src='audiovideo2.mov' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d42'/>"
            + "</par></body></smil>";
    String paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"; // Pick the presenter flavor
    // 2 tracks in the same group
    List<SmilMediaParam> params = new ArrayList<>();
    params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
            "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));
    params.add(mockSmilMediaParam("track-id", "track-2", "param-1097ff2d-431f-497c-b186-5d8f2ca6c88f"));
    params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo2.getPath(),
            "param-c57e9beb-a67a-4a96-96a6-9ede29e653ec"));
    params.add(mockSmilMediaParam("track-flavor", "presentation/work", "param-476ade36-9193-40bb-aae3-0c1028471797"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    // Second track is listed in same paramGroup
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 0L, 3000L, paramGroupId1));
    objects.add(mockSmilMediaElement(sourceAudioVideo2.toURI(), 5000L, 7500L, paramGroupId1));

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId1)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);
    // Code equivalence to the mock
    // SmilServiceImpl smilService = new SmilServiceImpl();
    // SmilResponse smilResponse = smilService.createNewSmil();
    // smilResponse = smilService.addParallel(smilResponse.getSmil());
    // SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
    // smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), track1, 0L, 3000L);
    // List<SmilMediaParamGroup> groups = smilResponse.getSmil().getHead().getParamGroups();
    // String paramGroupId = groups.get(0).getId();
    // smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), track2, 6000L, 8500L, paramGroupId);
    // Smil smil = smilResponse.getSmil();
    List<String> encodingProfiles = Arrays.asList("h264-low.http", "h264-medium.http", "h264-large.http");

    Job job = composerService.processSmil(smil, paramGroupId1, "", encodingProfiles);
    List<Track> outputs = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    assertNotNull(outputs);
    logger.info("testProcessSmilDirect got {} files", outputs);
    assertTrue(outputs.size() == 3); // One for each profile
  }

  @Test
  public void testProcessProdSysTracks() throws Exception {
    assertTrue(sourceAudioVideo1.isFile());
    assertTrue(sourceAudioVideo2.isFile());
    String prodsmil = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<smil xmlns='http://www.w3.org/ns/SMIL' baseProfile='Language' version='3.0' xml:id='s-aedc52a7-3207-49cf-8a9b-221ee8baba66'>"
            + "<head xml:id='h-d9ba75ce-2b50-458d-8919-7a92933a75a0'>"
            + "<meta content='17d14143-5bbf-4c60-b082-fbc401350691' name='media-package-id' xml:id='meta-a9d5fb98-af82-4b9e-abc2-abe5a9dd843c'/>"
            + "<meta content='300000ms' name='track-duration' xml:id='meta-1950b9b6-cd8f-43ac-9744-f6d8173ca171'/>"
            + "<paramGroup xml:id='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1'>"
            + "<param name='track-id' value='220f90fb-c764-40ac-b308-cd6731d22d2e' valuetype='data' xml:id='param-31a5a322-18ae-4659-b696-b3772d99651d'/>"
            + "<param name='track-src' value='audiovideo1.mp4' valuetype='data' xml:id='param-aed3f3db-7d68-4a76-9229-557940fb44be'/>"
            + "<param name='track-flavor' value='presenter/source' valuetype='data' xml:id='param-1c069d4a-23cd-45e3-b951-53de908b2b69'/>"
            + "</paramGroup></head>"
            + "<body xml:id='b-de826a33-7858-4172-a4a2-9cf1e9a53183'>"
            + "<par xml:id='par-c82881e2-f372-4a98-8cd4-97bc145cbfbe'>"
            + "<video paramGroup='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1' src='sourceAudioVideo1.mp4' clipBegin='0ms' clipEnd='10000ms' xml:id='v-2fc1b286-d87b-4393-a922-161af39a9f93'/>"
            + "</par>"
            + "<par xml:id='par-eb80f89d-0fce-466d-b0c5-48a30006b691'>"
            + "<video clipBegin='18000ms' clipEnd='30000ms' paramGroup='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1' src='sourceAudioVideo2.mp4' xml:id='v-d4d19997-f06b-4bdd-8e8c-d23907a971a8'/>"
            + "</par></body>" + "</smil>";
    // SmilResponse smilResponse = smilService.fromXml(prodsmil);
    String paramGroupId = "pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1"; // Pick the presenter flavor

    List<SmilMediaParam> params = new ArrayList<>();
    params.add(mockSmilMediaParam("track-id", "track-1", "param-31a5a322-18ae-4659-b696-b3772d99651d"));
    params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
            "param-aed3f3db-7d68-4a76-9229-557940fb44be"));
    params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-1c069d4a-23cd-45e3-b951-53de908b2b69"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    // Second track is not listed in paramGroup
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 0L, 3000L, paramGroupId));
    objects.add(mockSmilMediaElement(sourceAudioVideo2.toURI(), 5000L, 7500L, paramGroupId));

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(prodsmil).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);
    List<String> encodingProfiles = Arrays.asList("h264-low.http", "h264-medium.http", "h264-large.http");
    Job job = composerService.processSmil(smil, paramGroupId, "x", encodingProfiles);
    List<Track> outputs = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    assertNotNull(outputs);
    logger.info("testProcessSmilDirect got {} files", outputs);
    assertTrue(outputs.size() == 3); // One for each profile
  }

  @Test
  public void testProcessHLSTracks() throws Exception {
    assertTrue(sourceAudioVideo1.isFile());
    assertTrue(sourceAudioVideo2.isFile());
    String prodsmil = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<smil xmlns='http://www.w3.org/ns/SMIL' baseProfile='Language' version='3.0' xml:id='s-aedc52a7-3207-49cf-8a9b-221ee8baba66'>"
            + "<head xml:id='h-d9ba75ce-2b50-458d-8919-7a92933a75a0'>"
            + "<meta content='17d14143-5bbf-4c60-b082-fbc401350691' name='media-package-id' xml:id='meta-a9d5fb98-af82-4b9e-abc2-abe5a9dd843c'/>"
            + "<meta content='300000ms' name='track-duration' xml:id='meta-1950b9b6-cd8f-43ac-9744-f6d8173ca171'/>"
            + "<paramGroup xml:id='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1'>"
            + "<param name='track-id' value='220f90fb-c764-40ac-b308-cd6731d22d2e' valuetype='data' xml:id='param-31a5a322-18ae-4659-b696-b3772d99651d'/>"
            + "<param name='track-src' value='audiovideo1.mp4' valuetype='data' xml:id='param-aed3f3db-7d68-4a76-9229-557940fb44be'/>"
            + "<param name='track-flavor' value='presenter/source' valuetype='data' xml:id='param-1c069d4a-23cd-45e3-b951-53de908b2b69'/>"
            + "</paramGroup></head>"
            + "<body xml:id='b-de826a33-7858-4172-a4a2-9cf1e9a53183'>"
            + "<par xml:id='par-c82881e2-f372-4a98-8cd4-97bc145cbfbe'>"
            + "<video paramGroup='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1' src='sourceAudioVideo1.mp4' clipBegin='0ms' clipEnd='10000ms' xml:id='v-2fc1b286-d87b-4393-a922-161af39a9f93'/>"
            + "</par>"
            + "<par xml:id='par-eb80f89d-0fce-466d-b0c5-48a30006b691'>"
            + "<video clipBegin='18000ms' clipEnd='30000ms' paramGroup='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1' src='sourceAudioVideo2.mp4' xml:id='v-d4d19997-f06b-4bdd-8e8c-d23907a971a8'/>"
            + "</par></body>" + "</smil>";
    // SmilResponse smilResponse = smilService.fromXml(prodsmil);
    String paramGroupId = "pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1"; // Pick the presenter flavor

    List<SmilMediaParam> params = new ArrayList<>();
    params.add(mockSmilMediaParam("track-id", "track-1", "param-31a5a322-18ae-4659-b696-b3772d99651d"));
    params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
            "param-aed3f3db-7d68-4a76-9229-557940fb44be"));
    params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-1c069d4a-23cd-45e3-b951-53de908b2b69"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    // Second track is not listed in paramGroup
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 0L, 3000L, paramGroupId));
    objects.add(mockSmilMediaElement(sourceAudioVideo2.toURI(), 5000L, 7500L, paramGroupId));

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(prodsmil).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);
    List<String> encodingProfiles = Arrays.asList("h264-large.http", "h264-low.http", "multiencode-hls");
    Job job = composerService.processSmil(smil, paramGroupId, "x", encodingProfiles);
    List<Track> outputs = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    assertNotNull(outputs);
    logger.info("testProcessSmilDirect got {} files", outputs);
    assertTrue(outputs.size() == 2 + 2 + 1); // One segment and one variant for each profile + master
  }

  /**
   * Test method for
   * {@link org.opencastproject.composer.impl.ComposerServiceImpl#processSmil(org.opencastproject.smil.entity.api.Smil, String[])}
   */
  @Test
  public void testProcessSmilJob() throws Exception {
    String paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"; // Pick the presenter flavor
    List<SmilMediaParam> params = new ArrayList<>();
    params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1.getPath(),
            "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    // Second track is listed in same paramGroup
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 0L, 3000L, paramGroupId1));
    objects.add(mockSmilMediaElement(sourceAudioVideo1.toURI(), 5000L, 8300L, paramGroupId1));

    SmilMediaContainer objectContainer = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer.getElements()).andReturn(objects).anyTimes();
    EasyMock.replay(objectContainer);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(paramGroupId1)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn("").anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    composerService.setSmilService(smilService);

    // Encoding profile must support stream, visual or audiovisual - also does not like yadif for some edits
    List<String> encodingProfiles = Arrays.asList("h264-low.http", "h264-medium.http");

    Job job = composerService.processSmil(smil, paramGroupId1, "x", encodingProfiles);
    @SuppressWarnings("unchecked")
    List<Track> processedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    logger.debug("testProcessSmil got " + processedTracks.size() + " tracks");
    Assert.assertNotNull(processedTracks);
    Assert.assertEquals(2, processedTracks.size());
    for (Track processedTrack : processedTracks) {
      Assert.assertNotNull(processedTrack.getIdentifier());
      Assert.assertEquals(processedTrack.getMimeType(), MimeType.mimeType("video", "mpeg"));
    }

  }
}
