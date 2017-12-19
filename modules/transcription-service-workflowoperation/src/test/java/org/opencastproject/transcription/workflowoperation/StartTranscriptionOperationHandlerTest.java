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
package org.opencastproject.transcription.workflowoperation;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StartTranscriptionOperationHandlerTest {

  /** The operation handler to test */
  private StartTranscriptionOperationHandler operationHandler;

  /** The transcription service */
  private TranscriptionService service;

  /** The operation instance */
  private WorkflowOperationInstance operation;

  private MediaPackage mediaPackage;
  private WorkflowInstance workflowInstance;
  private Capture<Track> capturedTrack;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // Media package set up
    URI mediaPackageURI = StartTranscriptionOperationHandlerTest.class.getResource("/mp.xml").toURI();
    mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream());

    // Service registry set up
    Job job1 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job1.getId()).andReturn(1L);
    EasyMock.expect(job1.getPayload()).andReturn(null).anyTimes();
    EasyMock.expect(job1.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job1.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job1.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job1.getQueueTime()).andReturn(new Long(0));
    EasyMock.replay(job1);

    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job1);
    EasyMock.replay(serviceRegistry);

    // Transcription service set up
    service = EasyMock.createStrictMock(TranscriptionService.class);
    capturedTrack = Capture.newInstance();
    EasyMock.expect(service.startTranscription(EasyMock.anyObject(String.class), EasyMock.capture(capturedTrack)))
            .andReturn(null);
    EasyMock.replay(service);

    // Workflow set up
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("DCE-start-transcription");
    def.setPublished(true);
    workflowInstance = new WorkflowInstanceImpl(def, mediaPackage, null, null, null, null);
    workflowInstance.setId(1);
    operation = new WorkflowOperationInstanceImpl("start-transcript", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationList = new ArrayList<WorkflowOperationInstance>();
    operationList.add(operation);
    workflowInstance.setOperations(operationList);

    // Operation handler set up
    operationHandler = new StartTranscriptionOperationHandler();
    operationHandler.setTranscriptionService(service);
    operationHandler.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testStartSelectByFlavor() throws Exception {
    operation.setConfiguration(StartTranscriptionOperationHandler.SOURCE_FLAVOR, "audio/ogg");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("audioTrack1", capturedTrack.getValue().getIdentifier());
  }

  @Test
  public void testStartSelectByTag() throws Exception {
    operation.setConfiguration(StartTranscriptionOperationHandler.SOURCE_TAG, "transcript");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("audioTrack1", capturedTrack.getValue().getIdentifier());
  }

  @Test
  public void testStartSkipFlavor() throws Exception {
    // Make sure operation will be skipped if media package already contains the flavor passed
    operation.setConfiguration(StartTranscriptionOperationHandler.SKIP_IF_FLAVOR_EXISTS, "audio/ogg");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testStartDontSkipFlavor() throws Exception {
    operation.setConfiguration(StartTranscriptionOperationHandler.SOURCE_TAG, "transcript");
    // Make sure operation will NOT be skipped if media package does NOT contain the flavor passed
    operation.setConfiguration(StartTranscriptionOperationHandler.SKIP_IF_FLAVOR_EXISTS, "captions/timedtext");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("audioTrack1", capturedTrack.getValue().getIdentifier());
  }

  @Test(expected = WorkflowOperationException.class)
  public void testStartMissingConfiguration() throws Exception {
    operationHandler.start(workflowInstance, null);
  }
}
