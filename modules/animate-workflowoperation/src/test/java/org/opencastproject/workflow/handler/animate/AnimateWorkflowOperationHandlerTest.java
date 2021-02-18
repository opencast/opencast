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

package org.opencastproject.workflow.handler.animate;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;

import org.opencastproject.animate.api.AnimateService;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

public class AnimateWorkflowOperationHandlerTest {

  private AnimateWorkflowOperationHandler handler;
  private WorkflowInstanceImpl workflow;
  private WorkflowOperationInstance instance;
  private File file;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {

    handler = new AnimateWorkflowOperationHandler() {
      @Override
      protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
        JobBarrier.Result result = EasyMock.createNiceMock(JobBarrier.Result.class);
        EasyMock.expect(result.isSuccess()).andReturn(true).anyTimes();
        EasyMock.replay(result);
        return result;
      }
    };

    file = new File(getClass().getResource("/dc-episode.xml").toURI());

    MediaPackage mediaPackage = new MediaPackageBuilderImpl().createNew();
    mediaPackage.setIdentifier(new IdImpl("123-456"));

    InputStream in = new FileInputStream(file);
    Catalog catalog = DublinCores.read(in);
    catalog.setFlavor(MediaPackageElements.EPISODE);
    //catalog.setURI(getClass().getResource("/dc-episode.xml").toURI());
    mediaPackage.add(catalog);

    instance = EasyMock.createNiceMock(WorkflowOperationInstanceImpl.class);
    EasyMock.expect(instance.getConfiguration("target-flavor")).andReturn("a/b").anyTimes();
    EasyMock.expect(instance.getConfiguration("target-tags")).andReturn("a,b,c").anyTimes();

    workflow = EasyMock.createMock(WorkflowInstanceImpl.class);
    EasyMock.expect(workflow.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflow.getCurrentOperation()).andReturn(instance).anyTimes();

    Job job = new JobImpl(0);
    job.setPayload(file.getAbsolutePath());

    AnimateService animateService = EasyMock.createMock(AnimateService.class);
    EasyMock.expect(animateService.animate(anyObject(), anyObject(), anyObject())).andReturn(job);

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put(anyString(), anyString(), anyString(), anyObject())).andReturn(file.toURI())
            .anyTimes();
    EasyMock.expect(workspace.read(anyObject()))
            .andAnswer(() -> getClass().getResourceAsStream("/dc-episode.xml")).anyTimes();
    workspace.cleanup(anyObject(Id.class));
    EasyMock.expectLastCall();
    workspace.delete(anyObject(URI.class));
    EasyMock.expectLastCall();

    job = new JobImpl(1);
    job.setPayload(MediaPackageElementParser.getAsXml(new TrackImpl()));
    MediaInspectionService mediaInspectionService = EasyMock.createMock(MediaInspectionService.class);
    EasyMock.expect(mediaInspectionService.enrich(anyObject(), anyBoolean())).andReturn(job).once();

    EasyMock.replay(animateService, workspace, workflow, mediaInspectionService);

    handler.setAnimateService(animateService);
    handler.setMediaInspectionService(mediaInspectionService);
    handler.setWorkspace(workspace);
  }

  @Test
  public void testStart() throws Exception {
    EasyMock.expect(instance.getConfiguration("animation-file")).andReturn(file.getAbsolutePath()).anyTimes();
    EasyMock.expect(instance.getConfiguration("fps")).andReturn("24").anyTimes();
    EasyMock.replay(instance);
    Assert.assertTrue(handler.start(workflow, null).allowsContinue());
  }

  @Test
  public void testNoAnimation() {
    EasyMock.replay(instance);
    try {
      handler.start(workflow, null);
    } catch (WorkflowOperationException e) {
      return;
    }
    // We expect this to fail and the test should never reach this point
    Assert.fail();
  }

  @Test
  public void testCustomCmdArgs() throws Exception {
    EasyMock.expect(instance.getConfiguration("animation-file")).andReturn(file.getAbsolutePath()).anyTimes();
    EasyMock.expect(instance.getConfiguration("cmd-args")).andReturn("-t ffmpeg -w 160 -h 90").anyTimes();
    EasyMock.replay(instance);
    Assert.assertTrue(handler.start(workflow, null).allowsContinue());
  }

}
