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

package org.opencastproject.workflow.handler.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang3.math.Fraction;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalyzeTracksWorkflowOperationHandlerTest {

  private Logger logger = LoggerFactory.getLogger(AnalyzeTracksWorkflowOperationHandlerTest.class);

  private AnalyzeTracksWorkflowOperationHandler operationHandler;

  @Before
  public void setUp() throws Exception {
    operationHandler = new AnalyzeTracksWorkflowOperationHandler();
  }

  @Test
  public void testGetNearestResolution() throws Exception {
    Fraction frac43 = Fraction.getFraction(4, 3);
    Fraction frac169 = Fraction.getFraction(16, 9);
    List<Fraction> aspects = new ArrayList<>();
    aspects.add(frac43);
    aspects.add(frac169);

    int[][] aspect43 = {{640, 480},{768, 576}, {720, 576}, {703, 576}, {720, 576}, {720, 480}, {240, 180}, {638, 512},
            {704, 576}, {756, 576}, {800, 600}, {1024, 768}, {1280, 1023}, {1016, 768}, {1280, 1024}};
    int[][] aspect169 = {{1024, 576}, {1280, 720}, {1068, 600}, {1248, 702}, {1278, 720}};

    for (int[] resArr: aspect43) {
      Fraction res = Fraction.getFraction(resArr[0], resArr[1]);
      Fraction aspect = operationHandler.getNearestAspectRatio(res, aspects);
      logger.info("res: {} -> aspect: {} | expected 4/3", res, aspect);
      assertEquals(frac43, aspect);
    }

    for (int[] resArr: aspect169) {
      Fraction res = Fraction.getFraction(resArr[0], resArr[1]);
      Fraction aspect = operationHandler.getNearestAspectRatio(res, aspects);
      logger.info("res: {} -> aspect: {} | expected 16/9", res, aspect);
      assertEquals(frac169, aspect);
    }
  }

  @Test
  public void testGetAspectRatio() {
    List<Fraction> a = operationHandler.getAspectRatio("4/3,16/9");
    assertEquals(Fraction.getFraction(4, 3), a.get(0));
    assertEquals(Fraction.getFraction(16, 9), a.get(1));
    assertTrue(operationHandler.getAspectRatio("").isEmpty());
  }

  @Test
  public void testStart() throws MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    VideoStreamImpl videoStream = new VideoStreamImpl("234");
    videoStream.setFrameWidth(1280);
    videoStream.setFrameHeight(720);
    videoStream.setFrameRate(30.0f);
    TrackImpl track = new TrackImpl();
    track.setFlavor(MediaPackageElementFlavor.parseFlavor("presenter/source"));
    track.addStream(videoStream);

    JobContext jobContext = EasyMock.createMock(JobContext.class);
    EasyMock.replay(jobContext);

    WorkflowOperationInstance operationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    String[][] config = {
            {AnalyzeTracksWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source"},
            {AnalyzeTracksWorkflowOperationHandler.OPT_VIDEO_ASPECT, "4/3,16/9"}};
    for (String[] cfg: config) {
      EasyMock.expect(operationInstance.getConfiguration(cfg[0])).andReturn(cfg[1]).anyTimes();
    }
    EasyMock.expect(operationInstance.getConfiguration(AnalyzeTracksWorkflowOperationHandler.OPT_FAIL_NO_TRACK))
            .andReturn("true");
    EasyMock.expect(operationInstance.getConfiguration(AnalyzeTracksWorkflowOperationHandler.OPT_FAIL_NO_TRACK))
            .andReturn("false").anyTimes();
    EasyMock.replay(operationInstance);

    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getId()).andReturn(0L).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(operationInstance).anyTimes();
    EasyMock.replay(workflowInstance);

    // With no matching track (should fail)
    try {
      operationHandler.start(workflowInstance, jobContext);
      fail();
    } catch (WorkflowOperationException e) {
      logger.info("Fail on no tracks works");
    }
    WorkflowOperationResult workflowOperationResult = operationHandler.start(workflowInstance, jobContext);
    Map<String, String> properties = workflowOperationResult.getProperties();
    assertTrue(properties.isEmpty());

    // With matching track
    mediaPackage.add(track);
    workflowOperationResult = operationHandler.start(workflowInstance, jobContext);
    properties = workflowOperationResult.getProperties();

    String[][] props = {
            {"presenter_source_media", "true"},
            {"presenter_source_audio", "false"},
            {"presenter_source_aspect", "16/9"},
            {"presenter_source_resolution_y", "720"},
            {"presenter_source_resolution_x", "1280"},
            {"presenter_source_aspect_snap", "16/9"},
            {"presenter_source_video", "true"},
            {"presenter_source_framerate", "30.0"}};
    for (String[] prop: props) {
      assertEquals(prop[1], properties.get(prop[0]));
    }
  }

}
