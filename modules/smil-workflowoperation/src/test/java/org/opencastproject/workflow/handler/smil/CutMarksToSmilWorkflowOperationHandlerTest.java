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
package org.opencastproject.workflow.handler.smil;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

public class CutMarksToSmilWorkflowOperationHandlerTest {

  private static final String SOURCE_MEDIA_FLAVORS = "source-media-flavors";
  private static final String SOURCE_MEDIA_FLAVORS_KEY = "source/presenter";
  private static final String SOURCE_JSON_FLAVOR = "source-json-flavor";
  private static final String SOURCE_JSON_FLAVOR_KEY = "json/times";
  private static final String TARGET_SMIL_FLAVOR = "target-smil-flavor";
  private static final String TARGET_SMIL_FLAVOR_KEY = "a/b";

  private static final String CORRECT_JSON_PATH = "/cutmarkstosmil_correct_1.json";
  private static final String FAULTY_JSON_PATH = "/cutmarkstosmil_faulty_1.json";
  private static final String SOME_TEST_VIDEO_PATH = "/testvideo_320x180.mp4";
  private static final String SOME_RESULT_SMIL_PATH = "/smil.smil";

  private CutMarksToSmilWorkflowOperationHandler handler;
  private SmilService smilService;

  private Workspace workspace;
  private WorkflowInstanceImpl workflow;
  private WorkflowOperationInstance instance;

  private URI mpSmilURI;

  @Before
  public void setUp() throws Exception {
    handler = new CutMarksToSmilWorkflowOperationHandler();

    /** Create Mocks **/
    instance = EasyMock.createNiceMock(WorkflowOperationInstanceImpl.class);
    EasyMock.expect(instance.getConfiguration(TARGET_SMIL_FLAVOR)).andReturn(TARGET_SMIL_FLAVOR_KEY).anyTimes();

    workspace = EasyMock.createNiceMock(Workspace.class);

    workflow = EasyMock.createNiceMock(WorkflowInstanceImpl.class);
    EasyMock.expect(workflow.getCurrentOperation()).andReturn(instance).anyTimes();

    handler.setWorkspace(workspace);

    smilServiceSetup();
  }

  /**
   * Must be called by every test method for test-specific setup
   * @param jsonPaths Paths to json files that will be added to the source flavor
   * @param trackFlavors Track flavor will be created if it does not exist yet, also a track will be added to the flavor
   * @throws Exception
   */
  private void specificSetUp(String[] jsonPaths, String[] trackFlavors) throws Exception {
    MediaPackage mediaPackage = new MediaPackageBuilderImpl().createNew();
    mediaPackage.setIdentifier(new IdImpl("123-456"));

    // Add JSON to mp
    for (String path : jsonPaths) {
      URI newURI = getClass().getResource(path).toURI();
      EasyMock.expect(workspace.get(newURI)).andReturn(new File(newURI)).anyTimes();    // To avoid NullPointerEx when grabbing Absolute Track Path
      mediaPackage.add(newURI, MediaPackageElement.Type.Catalog,
              MediaPackageElementFlavor.parseFlavor(SOURCE_JSON_FLAVOR_KEY));
    }

    // Add track with flavor to mp
    for (String flavor : trackFlavors) {
      TrackImpl track = new TrackImpl();
      track.setFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      // Make sure track contains a video
      VideoStreamImpl videoStream = new VideoStreamImpl("test1");
      track.setVideo(Arrays.asList(videoStream));
      URI trackURI = getClass().getResource(SOME_TEST_VIDEO_PATH).toURI();   // Absolute URI
      track.setURI(trackURI);
      track.setDuration(new Long(10000));
      mediaPackage.add(track);
    }

    EasyMock.expect(workflow.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.replay(workspace, workflow);
    handler.setWorkspace(workspace);

    EasyMock.expect(instance.getConfiguration(SOURCE_MEDIA_FLAVORS)).andReturn(String.join(",", trackFlavors)).anyTimes();
    EasyMock.expect(instance.getConfiguration(SOURCE_JSON_FLAVOR)).andReturn(SOURCE_JSON_FLAVOR_KEY).anyTimes();
    EasyMock.replay(instance);
  }

  /**
   * Initializes the smil service used for creating the final smil
   * A more complete example on how to mock the smil service can be found under:
   * modules/videoeditor-workflowoperation/src/test/java/org/opencastproject/workflow/handler/videoeditor/SmilServiceMock.java
   * @throws Exception
   */
  private void smilServiceSetup() throws Exception {
    mpSmilURI = getClass().getResource(SOME_RESULT_SMIL_PATH).toURI();

    SmilMediaContainer objectPar = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectPar.getId()).andReturn("test").anyTimes();
    EasyMock.replay(objectPar);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.toXML()).andReturn(mpSmilURI.toString()).anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(objectPar).anyTimes();
    EasyMock.replay(response);

    smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.createNewSmil((MediaPackage) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.expect(smilService.addParallel(smil)).andReturn(response).anyTimes();
    EasyMock.expect(smilService.addClips((Smil) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (Track[]) EasyMock.anyObject(), EasyMock.anyLong(), EasyMock.anyLong())).andReturn(response).anyTimes();
    EasyMock.expect(workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject(), (InputStream) EasyMock.anyObject())).andReturn(mpSmilURI);
    EasyMock.replay(smilService);
    handler.setSmilService(smilService);
  }

  @Test
  public void testComplete() throws Exception {
    String[] jsonPaths = new String[] {CORRECT_JSON_PATH};
    String[] trackFlavors = new String[] {SOURCE_MEDIA_FLAVORS_KEY};
    specificSetUp(jsonPaths, trackFlavors);

    WorkflowOperationResult result = handler.start(workflow, null);
    Assert.assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());

    MediaPackage mpNew = result.getMediaPackage();
    Catalog[] catalogs = mpNew.getCatalogs(MediaPackageElementFlavor.parseFlavor(TARGET_SMIL_FLAVOR_KEY));
    Assert.assertEquals(1, catalogs.length);
  }

  @Test
  public void testNoJSON() throws Exception {
    String[] jsonPaths = new String[] {};
    String[] trackFlavors = new String[] {SOURCE_MEDIA_FLAVORS_KEY};
    specificSetUp(jsonPaths, trackFlavors);

    WorkflowOperationResult result = handler.start(workflow, null);
    Assert.assertEquals(WorkflowOperationResult.Action.SKIP, result.getAction());
  }

  @Test
  public void testManyJSON() throws Exception {
    String[] jsonPaths = new String[] {CORRECT_JSON_PATH, FAULTY_JSON_PATH};
    String[] trackFlavors = new String[] {SOURCE_MEDIA_FLAVORS_KEY};
    specificSetUp(jsonPaths, trackFlavors);

    try {
      handler.start(workflow, null);
      Assert.fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void testFaultyJSON() throws Exception {
    String[] jsonPaths = new String[] {FAULTY_JSON_PATH};
    String[] trackFlavors = new String[] {SOURCE_MEDIA_FLAVORS_KEY};
    specificSetUp(jsonPaths, trackFlavors);

    try {
      handler.start(workflow, null);
      Assert.fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void testNoTracks() throws Exception {
    String[] jsonPaths = new String[] {CORRECT_JSON_PATH};
    String[] trackFlavors = new String[] {};
    specificSetUp(jsonPaths, trackFlavors);

    WorkflowOperationResult result = handler.start(workflow, null);
    Assert.assertEquals(WorkflowOperationResult.Action.SKIP, result.getAction());
  }
}
