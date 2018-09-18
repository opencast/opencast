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
package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
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
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

public class ProcessSmilWorkflowOperationHandlerTest {
  private ProcessSmilWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private MediaPackage mp2;
  private Job job;
  private Track[] encodedTracks;
  private Track[] encodedTracks2;
  private EncodingProfile[] profileList;
  private static final Logger logger = LoggerFactory.getLogger(ProcessSmilWorkflowOperationHandlerTest.class);
  // mock services and objects
  private EncodingProfile profile = null;
  private EncodingProfile profile2 = null;
  private EncodingProfile profile3 = null;
  private ComposerService composerService = null;
  private SmilService smilService = null;
  private Workspace workspace = null;
  private File workingDirectory = null;
  private File smilfile = null;

  // constant metadata values
  private static final String PROFILE_ID = "flash.http";
  private static final String PROFILE_ID2 = "x264.http";
  private static final String PROFILE_ID3 = "aac.http";
  private static final String SOURCE_PRESENTER_TRACK_ID = "compose-workflow-operation-test-source-presenter-track-id";
  private static final String ENCODED_PRESENTER_TRACK_ID = "compose-workflow-operation-test-trimmed-presenter-track-id";
  private static final String SOURCE_PRESENTATION_TRACK_ID = "compose-workflow-operation-test-source-presentation-track-id";
  private static final String ENCODED_PRESENTATION_TRACK_ID = "compose-workflow-operation-test-trimmed-presentation-track-id";

  // <operation
  // id="processsmil"
  // if="${trimHold}"
  // fail-on-error="true"
  // exception-handler-workflow="error"
  // description="takes a smil edit and transcode to all final formats concurrently">
  // <configurations>
  // <configuration key="source-flavors">presenter/*;presentation/*</configuration>
  // <configuration key="smil-flavor">smil/smil</configuration>
  // <configuration key="target-flavors">presenter/delivery;presentation/delivery</configuration>
  // <configuration key="target-tags">engage;engage</configuration>
  // <configuration key="encoding-profile">flash-vga.http;h264-low.http</configuration>
  // <configuration key="tag-with-profile">true</configuration>
  // </configurations>
  // </operation>
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

  SmilService mockSmilService() throws SmilException, MalformedURLException, JAXBException, SAXException {
    String paramGroupId1 = "pg-e823179d-f606-4975-8972-6d06feb92f04";
    String paramGroupId2 = "pg-acc84414-82b8-427f-8453-8b655101df22";
    File video1 = new File("fooVideo1.flv");
    File video2 = new File("fooVideo2.flv");
    smilfile = new File("src/test/resources/smil.smil");

    List<SmilMediaParam> params1 = new ArrayList<>();
    params1.add(mockSmilMediaParam("track-id", "b7c4f480-dd22-4f82-bb58-c4f218051059",
            "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params1.add(mockSmilMediaParam("track-src", "fooVideo1.flv", "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));
    List<SmilMediaParam> params2 = new ArrayList<>();
    params2.add(mockSmilMediaParam("track-id", "144b489b-c498-4d11-9a63-1ab96a7ec0b1",
            "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"));
    params2.add(mockSmilMediaParam("track-src", "fooVideo2.flv", "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"));
    params2.add(mockSmilMediaParam("track-flavor", "presentation/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"));

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params1).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group1);
    SmilMediaParamGroup group2 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group2.getParams()).andReturn(params2).anyTimes();
    EasyMock.expect(group2.getId()).andReturn(paramGroupId1).anyTimes();
    EasyMock.replay(group2);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);
    paramGroups.add(group2);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    List<SmilMediaObject> objects = new ArrayList<>();
    objects.add(mockSmilMediaElement(video1.toURI(), 0L, 3000L, paramGroupId1));
    objects.add(mockSmilMediaElement(video2.toURI(), 0L, 3000L, paramGroupId2));
    objects.add(mockSmilMediaElement(video1.toURI(), 4000L, 7000L, paramGroupId1));
    objects.add(mockSmilMediaElement(video2.toURI(), 4000L, 7000L, paramGroupId2));

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
    EasyMock.expect(smil.toXML()).andReturn("").anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    return (smilService);
  }

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    workingDirectory = FileSupport.getTempDirectory("processSmiltest");
    FileUtils.forceMkdir(workingDirectory);
    smilfile = new File("src/test/resources/smil.smil");

    // set up mock smil service
    SmilService smilService = mockSmilService();
    // smilService.fromXml(FileUtils.readFileToString(smilFile, "UTF-8"))

    // set up mock profile
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(PROFILE_ID).anyTimes();
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(MediaType.Stream).anyTimes();
    EasyMock.expect(profile.getOutputType()).andReturn(MediaType.AudioVisual).anyTimes();
    EasyMock.expect(profile.getSuffix()).andReturn("-v.flv").anyTimes();

    profile2 = EasyMock.createNiceMock(EncodingProfile.class); // Video Only
    EasyMock.expect(profile2.getIdentifier()).andReturn(PROFILE_ID2).anyTimes();
    EasyMock.expect(profile2.getApplicableMediaType()).andReturn(MediaType.Visual).anyTimes();
    EasyMock.expect(profile2.getOutputType()).andReturn(MediaType.Visual).anyTimes();
    EasyMock.expect(profile2.getSuffix()).andReturn("-v.mp4").anyTimes();

    profile3 = EasyMock.createNiceMock(EncodingProfile.class); // different suffix
    EasyMock.expect(profile3.getIdentifier()).andReturn(PROFILE_ID3).anyTimes();
    EasyMock.expect(profile3.getApplicableMediaType()).andReturn(MediaType.Audio).anyTimes();
    EasyMock.expect(profile3.getOutputType()).andReturn(MediaType.Audio).anyTimes();
    EasyMock.expect(profile3.getSuffix()).andReturn("-a.mp4").anyTimes();
    profileList = new EncodingProfile[] { profile, profile2, profile3 };
    EasyMock.replay(profile, profile2, profile3);

    // AV both tracks
    final URI uriMP = InspectWorkflowOperationHandler.class.getResource("/process_smil_mediapackage.xml").toURI();
    // AV presenter, V only presentation
    final URI uriMP2 = InspectWorkflowOperationHandler.class.getResource("/process_smil_mediapackage2.xml").toURI();
    // AV single AV return
    final URI uriMPEncode = InspectWorkflowOperationHandler.class.getResource("/process_smil_result_mediapackage.xml")
            .toURI();
    // AV 2 AV tracks return
    final URI uriMPEncode2 = InspectWorkflowOperationHandler.class.getResource("/process_smil_result2_mediapackage.xml")
            .toURI();

    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mp2 = builder.loadFromXml(uriMP2.toURL().openStream());
    MediaPackage mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream());
    MediaPackage mpEncode2 = builder.loadFromXml(uriMPEncode2.toURL().openStream());
    encodedTracks = mpEncode.getTracks();
    encodedTracks2 = mpEncode2.getTracks();

    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getPayload()).andReturn(MediaPackageElementParser.getArrayAsXml(Arrays.asList(encodedTracks)))
            .anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getDateCreated()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getDateStarted()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(new Long(10)).anyTimes();
    EasyMock.replay(job);
    Job job2 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job2.getPayload()).andReturn(MediaPackageElementParser.getArrayAsXml(Arrays.asList(encodedTracks2)))
            .anyTimes();
    EasyMock.expect(job2.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job2.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job2.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job2.getQueueTime()).andReturn(new Long(13));
    EasyMock.replay(job2);
    // set up mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job2);
    EasyMock.replay(serviceRegistry);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.getProfile(PROFILE_ID)).andReturn(profile).anyTimes();
    EasyMock.expect(composerService.getProfile(PROFILE_ID2)).andReturn(profile2).anyTimes();
    EasyMock.expect(composerService.getProfile(PROFILE_ID3)).andReturn(profile3).anyTimes();
    EasyMock.expect(composerService.processSmil((Smil) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(), EasyMock.<List<String>> anyObject())).andReturn(job);
    EasyMock.expect(composerService.processSmil((Smil) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            EasyMock.anyObject(), EasyMock.<List<String>> anyObject())).andReturn(job2);
    EasyMock.replay(composerService);

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
              @Override
              public URI answer() throws Throwable {
                String name;
                try { // media file should be returned "as is"
                  // URI uri = (URI) EasyMock.getCurrentArguments()[0];
                  name = (String) EasyMock.getCurrentArguments()[3];
                  String ext = FilenameUtils.getExtension(name);
                  if (ext.matches("[fm][pol][v43]")) {
                    return new URI(name);
                  }
                } catch (Exception e) {
                }
                return uriMP; // default
              }
            }).anyTimes();

    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        String name;
        try {
          URI uri = (URI) EasyMock.getCurrentArguments()[0];
          name = uri.getPath();
          if (name.contains("smil.smil"))
            return smilfile;
        } catch (Exception e) {
          name = uriMP.getPath();
        }
        return new File(name); // default
      }
    }).anyTimes();

    EasyMock.expect(workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
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
    EasyMock.replay(workspace);

    operationHandler = new ProcessSmilWorkflowOperationHandler();
    operationHandler.setSmilService(smilService);
    operationHandler.setJobBarrierPollingInterval(0);
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
    operationHandler.setComposerService(composerService);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(workingDirectory);
  }

  @Test
  public void testProcessSmilOneTrackOneSection() throws Exception {
    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<>();
    configurations.put("source-flavors", "presenter/*");
    configurations.put("smil-flavor", "smil/smil");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavors", "presenter/livery");
    configurations.put("encoding-profiles", PROFILE_ID);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
    Assert.assertEquals("presenter/livery", trackEncoded.getFlavor().toString());
    String[] mytags = targetTags.split(",");
    Arrays.sort(mytags);
    Assert.assertArrayEquals(mytags, trackEncoded.getTags());
    Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.getReference().getIdentifier()); // reference the
                                                                                                 // correct flavor
    Assert.assertEquals(10, result.getTimeInQueue()); // Tracking queue time
  }

  @Test
  public void testProcessSmilOneTrackOneSectionNoTagsNoTargetFlavor() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<>();
    configurations.put("source-flavors", "presenter/*");
    configurations.put("smil-flavor", "smil/smil");
    configurations.put("target-flavors", "presenter/deli");
    configurations.put("encoding-profiles", PROFILE_ID);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
    Assert.assertEquals("presenter/deli", trackEncoded.getFlavor().toString());
    Assert.assertEquals(0, trackEncoded.getTags().length);
    Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.getReference().getIdentifier()); // reference the
                                                                                                 // correct flavor
    Assert.assertEquals(10, result.getTimeInQueue()); // Tracking queue time
    Track trackPresentationEncoded = mpNew.getTrack(ENCODED_PRESENTATION_TRACK_ID);
    Assert.assertNull(trackPresentationEncoded); // not encoded
  }

  @Test
  public void testProcessSmilTwoSectionsNoTagsNoTargetFlavor() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<>();
    configurations.put("source-flavors", "presenter/work;presentation/work");
    configurations.put("smil-flavor", "smil/smil");
    configurations.put("encoding-profiles", PROFILE_ID); // 2 jobs for the 2 sources

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackPresenterEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
    Assert.assertNull(trackPresenterEncoded.getFlavor());
    // Assert.assertEquals("presenter/delivery", trackEncoded.getFlavor().toString());
    Assert.assertEquals(0, trackPresenterEncoded.getTags().length);

    Track trackPresentationEncoded = mpNew.getTrack(ENCODED_PRESENTATION_TRACK_ID);
    Assert.assertEquals(0, trackPresentationEncoded.getTags().length);
  }

  @Test
  public void testProcessSmilTwoTrackAllSections() throws Exception {
    // operation configuration
    String targetTags = "engage," + PROFILE_ID + ",rss";
    Map<String, String> configurations = new HashMap<>();
    configurations.put("source-flavors", "*/work"); // should do 2 flavors as 2 jobs
    configurations.put("smil-flavor", "smil/smil");
    configurations.put("target-tags", "engage;rss"); // should collapse the tags
    configurations.put("target-flavors", "*/delivery");
    configurations.put("encoding-profiles", PROFILE_ID);
    configurations.put("tag-with-profile", "true");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    // Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
    MediaPackageElement[] mpelems = mpNew
            .getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presentation/delivery"));
    logger.info("Encoded tracks are : " + Arrays.toString(mpelems));
    Assert.assertEquals(mpelems.length, 2); // Hack because of the mock
    Track trackEncoded = (Track) mpelems[0];
    String[] mytags = trackEncoded.getTags();
    Arrays.sort(mytags);
    logger.info("Encoded tracks are tagged: {} should be {}", Arrays.toString(trackEncoded.getTags()), targetTags);
    Assert.assertArrayEquals(targetTags.split(","), mytags);

    Assert.assertEquals(SOURCE_PRESENTATION_TRACK_ID, trackEncoded.getReference().getIdentifier());

    mpelems = mpNew.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/delivery"));
    Assert.assertEquals(mpelems.length, 1);
    trackEncoded = (Track) mpelems[0];
    Assert.assertEquals("presenter/delivery", trackEncoded.getFlavor().toString());
    Assert.assertArrayEquals(targetTags.split(","), trackEncoded.getTags());
    Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.getReference().getIdentifier());
    Assert.assertEquals(ENCODED_PRESENTER_TRACK_ID, trackEncoded.getIdentifier());
  }

  @Test
  public void testProcessSmilOneTrackVideoOnly() throws Exception {
    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<>();
    configurations.put("source-flavors", "presenter/*");
    configurations.put("smil-flavor", "smil/smil");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavors", "*/delivery");
    configurations.put("encoding-profiles", PROFILE_ID);
    configurations.put("tag-with-profile", "true");

    // run the operation handler with video only mediapackage
    WorkflowOperationResult result = getWorkflowOperationResult(mp2, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    // Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
    MediaPackageElement[] mpelems = mpNew
            .getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/delivery"));
    Assert.assertEquals(mpelems.length, 1);
    Track trackEncoded = (Track) mpelems[0];
    targetTags += "," + PROFILE_ID;
    String[] mytags = targetTags.split(",");
    Arrays.sort(mytags);
    Assert.assertArrayEquals(mytags, trackEncoded.getTags());
    Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.getReference().getIdentifier());
  }

  @Test
  public void testProcessSmilTwoTrackTwoSections() throws Exception {
    // operation configuration
    String targetTags = "preview,rss";
    String targetTags2 = "archive,engage";
    Map<String, String> configurations = new HashMap<>();
    configurations.put("source-flavors", "presenter/*;presentation/*"); // 2 sections
    configurations.put("smil-flavor", "smil/smil");
    configurations.put("target-tags", targetTags + ";" + targetTags2); // different tags
    configurations.put("target-flavors", "*/delivery");
    configurations.put("encoding-profiles", PROFILE_ID + ";" + PROFILE_ID2 + "," + PROFILE_ID3); // different profiles
    configurations.put("tag-with-profile", "true");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    // Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
    MediaPackageElement[] mpelems = mpNew
            .getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presentation/delivery"));
    Assert.assertEquals(mpelems.length, 2); // Should have 2 tracks here
    logger.info("Encoded presentation tracks are : " + Arrays.toString(mpelems));
    Track trackEncoded = (Track) mpelems[0]; // This is not ordered
    if (trackEncoded.getURI().toString().endsWith(profile3.getSuffix())) {
      targetTags2 += "," + profile3.getIdentifier();
    } else if (trackEncoded.getURI().toString().endsWith(profile2.getSuffix())) {
      targetTags2 += "," + profile2.getIdentifier();
    }
    String[] mytags = trackEncoded.getTags();
    Arrays.sort(mytags);
    logger.info("Encoded presentation tracks are tagged: " + Arrays.toString(mytags) + " == " + targetTags2);
    Assert.assertArrayEquals(targetTags2.split(","), mytags);
    Assert.assertEquals(SOURCE_PRESENTATION_TRACK_ID, trackEncoded.getReference().getIdentifier());
    Assert.assertEquals(ENCODED_PRESENTATION_TRACK_ID, trackEncoded.getIdentifier());

    mpelems = mpNew.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/delivery"));
    Assert.assertEquals(mpelems.length, 1);
    trackEncoded = (Track) mpelems[0];
    logger.info("Encoded presenter tracks are : " + Arrays.toString(mpelems));
    Assert.assertEquals("presenter/delivery", trackEncoded.getFlavor().toString());
    targetTags += "," + profile.getIdentifier();
    logger.info(
            "Encoded presenter tracks are tagged: " + Arrays.toString(trackEncoded.getTags()) + " == " + targetTags);
    Assert.assertEquals(new HashSet<String>(Arrays.asList(targetTags.split(","))),
            new HashSet<String>(Arrays.asList(trackEncoded.getTags())));
    // Assert.assertArrayEquals(targetTags.split("\\W"), trackEncoded.getTags());
    Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.getReference().getIdentifier());
    Assert.assertEquals(ENCODED_PRESENTER_TRACK_ID, trackEncoded.getIdentifier());
    Assert.assertEquals(23, result.getTimeInQueue()); // Tracking queue time
  }

  @Test
  public void testComposeMissingProfile() throws Exception {
    // set up mock profile
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(PROFILE_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andReturn(MediaType.Stream);
    profileList = new EncodingProfile[] { profile };
    EasyMock.replay(profile);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.listProfiles()).andReturn(profileList);
    EasyMock.expect(composerService.encode((Track) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(job);
    EasyMock.expect(composerService.processSmil((Smil) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            EasyMock.anyString(), (List<String>) EasyMock.anyObject())).andReturn(job);
    EasyMock.replay(composerService);
    operationHandler.setComposerService(composerService);
    operationHandler.setSmilService(smilService);
    Map<String, String> configurations = new HashMap<>();
    try {
      // no encoding profile
      configurations.put("source-flavors", "presenter/work");
      configurations.put("smil-flavor", "smil/smil");
      configurations.put("target-flavors", "presenter/delivery");
      getWorkflowOperationResult(mp, configurations);
      Assert.fail("Since encoding profile is not specified exception should be thrown");
    } catch (WorkflowOperationException e) {
      // expecting exception
    }

  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("process-smil");
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }

}
