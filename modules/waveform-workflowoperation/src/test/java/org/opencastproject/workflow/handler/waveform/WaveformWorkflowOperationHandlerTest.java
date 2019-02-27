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

package org.opencastproject.workflow.handler.waveform;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.waveform.api.WaveformService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;

public class WaveformWorkflowOperationHandlerTest {

  private TrackImpl track;
  private WaveformWorkflowOperationHandler handler;
  private WorkflowInstanceImpl workflow;
  private WorkflowOperationInstance instance;

  @Before
  public void setUp() throws Exception {

    handler = new WaveformWorkflowOperationHandler() {
      @Override
      protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
        JobBarrier.Result result = EasyMock.createNiceMock(JobBarrier.Result.class);
        EasyMock.expect(result.isSuccess()).andReturn(true).anyTimes();
        EasyMock.replay(result);
        return result;
      }
    };

    track = new TrackImpl();
    track.setFlavor(MediaPackageElementFlavor.parseFlavor("xy/source"));
    track.setAudio(Arrays.asList(null, null));

    MediaPackageBuilder builder = new MediaPackageBuilderImpl();
    MediaPackage mediaPackage = builder.createNew();
    mediaPackage.setIdentifier(new IdImpl("123-456"));
    mediaPackage.add(track);

    instance = EasyMock.createNiceMock(WorkflowOperationInstanceImpl.class);
    EasyMock.expect(instance.getConfiguration("target-flavor")).andReturn("*/*").anyTimes();
    EasyMock.expect(instance.getConfiguration("target-tags")).andReturn("a,b,c").anyTimes();

    workflow = EasyMock.createNiceMock(WorkflowInstanceImpl.class);
    EasyMock.expect(workflow.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflow.getCurrentOperation()).andReturn(instance).anyTimes();

    Attachment payload = new AttachmentImpl();
    payload.setIdentifier("x");
    payload.setFlavor(MediaPackageElementFlavor.parseFlavor("xy/source"));
    Job job = new JobImpl(0);
    job.setPayload(MediaPackageElementParser.getAsXml(payload));

    WaveformService waveformService = EasyMock.createNiceMock(WaveformService.class);
    EasyMock.expect(waveformService.createWaveformImage(EasyMock.anyObject(), EasyMock.anyInt(), EasyMock.anyInt(),
      EasyMock.anyInt(), EasyMock.anyInt(), EasyMock.anyString())).andReturn(job);

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);

    EasyMock.replay(waveformService, workspace, workflow);

    handler.setWaveformService(waveformService);
    handler.setWorkspace(workspace);
  }

  @Test
  public void testStart() throws Exception {
    EasyMock.expect(instance.getConfiguration("source-flavor")).andReturn("*/source").anyTimes();
    EasyMock.replay(instance);
    Assert.assertTrue(handler.start(workflow, null).allowsContinue());
  }

  @Test
  public void testNoTracks() throws Exception {
    EasyMock.expect(instance.getConfiguration("source-flavor")).andReturn("*/nothing").anyTimes();
    EasyMock.replay(instance);
    Assert.assertTrue(handler.start(workflow, null).allowsContinue());
  }

  @Test
  public void testNoAudio() throws Exception {
    track.setAudio(new LinkedList<>());
    EasyMock.expect(instance.getConfiguration("source-flavor")).andReturn("*/source").anyTimes();
    EasyMock.replay(instance);
    Assert.assertTrue(handler.start(workflow, null).allowsContinue());
  }

  @Test
  public void testMissingSource() throws Exception {
    EasyMock.replay(instance);
    try {
      handler.start(workflow, null);
      Assert.fail();
    } catch (WorkflowOperationException e) {
      Assert.assertTrue(e.getMessage().startsWith("Required property "));
    }
  }

}
