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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProbeResolutionWorkflowOperationHandlerTest {

  private ProbeResolutionWorkflowOperationHandler operationHandler;

  @Before
  public void setUp() throws Exception {
    operationHandler = new ProbeResolutionWorkflowOperationHandler();
  }

  @Test
  public void testGetResolutions() {
    assertTrue(operationHandler.getResolutions("").isEmpty());

    List<Fraction> res = operationHandler.getResolutions("320x240,1280x720, 1920x1080");
    assertEquals(Fraction.getFraction(320, 240), res.get(0));
    assertEquals(Fraction.getFraction(1280, 720), res.get(1));
    assertEquals(Fraction.getFraction(1920, 1080), res.get(2));
  }

  @Test
  public void testStart() throws MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    VideoStreamImpl videoStream = new VideoStreamImpl("234");
    videoStream.setFrameWidth(1280);
    videoStream.setFrameHeight(720);
    TrackImpl track = new TrackImpl();
    track.setFlavor(MediaPackageElementFlavor.parseFlavor("presenter/source"));
    track.addStream(videoStream);

    JobContext jobContext = EasyMock.createMock(JobContext.class);
    EasyMock.replay(jobContext);

    WorkflowOperationInstance operationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    String[][] config = {
            { ProbeResolutionWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source"},
            { ProbeResolutionWorkflowOperationHandler.OPT_VAR_PREFIX + "aspect", "1280x720,1280x700"},
            { ProbeResolutionWorkflowOperationHandler.OPT_VAL_PREFIX + "aspect", "16/9"},
            { ProbeResolutionWorkflowOperationHandler.OPT_VAR_PREFIX + "is_720", "1280x720,1280x700"},
            { ProbeResolutionWorkflowOperationHandler.OPT_VAR_PREFIX + "is_1080", "1920x1080"}};
    Set<String> keys = new HashSet<>();
    for (String[] cfg: config) {
      keys.add(cfg[0]);
      EasyMock.expect(operationInstance.getConfiguration(cfg[0])).andReturn(cfg[1]).anyTimes();
    }
    EasyMock.expect(operationInstance.getConfigurationKeys()).andReturn(keys).anyTimes();
    EasyMock.replay(operationInstance);

    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(operationInstance).anyTimes();
    EasyMock.replay(workflowInstance);

    // With no matching track
    assertEquals(null, operationHandler.start(workflowInstance, jobContext).getProperties());

    // With matching track
    mediaPackage.add(track);
    WorkflowOperationResult workflowOperationResult = operationHandler.start(workflowInstance, jobContext);
    Map<String, String> properties = workflowOperationResult.getProperties();

    String[][] props = {
            {"presenter_source_aspect", "16/9"},
            {"presenter_source_is_720", "true"},
            {"presenter_source_is_1080", null}};
    for (String[] prop: props) {
      assertEquals(prop[1], properties.get(prop[0]));
    }
  }

}
