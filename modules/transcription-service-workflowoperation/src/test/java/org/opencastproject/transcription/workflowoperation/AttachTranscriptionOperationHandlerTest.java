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

import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
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
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AttachTranscriptionOperationHandlerTest {

  /** The operation handler to test */
  private AttachTranscriptionOperationHandler operationHandler;

  /** The transcription service */
  private TranscriptionService service;

  /** The operation instance */
  private WorkflowOperationInstance operation;

  private MediaPackage mediaPackage;
  private WorkflowInstance workflowInstance;
  private Job job1;
  private Job job2;
  private CaptionService captionService;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // Media package set up
    URI mediaPackageURI = StartTranscriptionOperationHandlerTest.class.getResource("/mp.xml").toURI();
    mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream());
    URI catalogURI = StartTranscriptionOperationHandlerTest.class.getResource("/catalog_mpe.xml").toURI();
    String catalogXml = FileUtils.readFileToString(new File(catalogURI));
    Catalog catalog = (Catalog) MediaPackageElementParser.getFromXml(catalogXml);
    URI attachmentURI = StartTranscriptionOperationHandlerTest.class.getResource("/attachment_mpe.xml").toURI();
    String attachmentXml = FileUtils.readFileToString(new File(attachmentURI));
    Attachment attachment = (Attachment) MediaPackageElementParser.getFromXml(attachmentXml);

    // Service registry set up
    job1 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job1.getId()).andReturn(1L);
    EasyMock.expect(job1.getPayload()).andReturn(catalogXml).anyTimes();
    EasyMock.expect(job1.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job1.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job1.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job1.getQueueTime()).andReturn(new Long(0));
    EasyMock.replay(job1);

    job2 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job2.getId()).andReturn(2L);
    EasyMock.expect(job2.getPayload()).andReturn(attachmentXml).anyTimes();
    EasyMock.expect(job2.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job2.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job2.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job2.getQueueTime()).andReturn(new Long(0));
    EasyMock.replay(job2);

    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job1);
    EasyMock.expect(serviceRegistry.getJob(2L)).andReturn(job2);
    EasyMock.replay(serviceRegistry);

    // Transcription service set up
    service = EasyMock.createStrictMock(TranscriptionService.class);

    EasyMock.expect(service.getGeneratedTranscription("mpId1", "transcriptionJob")).andReturn(catalog);
    EasyMock.expect(service.getGeneratedTranscription("mpId2", "transcriptionJob")).andReturn(attachment);
    EasyMock.replay(service);

    // Caption service set up
    captionService = EasyMock.createNiceMock(CaptionService.class);

    // Workspace set up
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.moveTo(EasyMock.anyObject(URI.class), EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class), EasyMock.anyObject(String.class)))
            .andReturn(new URI("http://opencast.server.com/captions.xml")); // just something valid
    EasyMock.replay(workspace);

    // Workflow set up
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("DCE-start-transcription");
    def.setPublished(true);
    workflowInstance = new WorkflowInstanceImpl(def, mediaPackage, null, null, null, null);
    workflowInstance.setId(1);
    operation = new WorkflowOperationInstanceImpl("attach-transcript", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationList = new ArrayList<WorkflowOperationInstance>();
    operationList.add(operation);
    workflowInstance.setOperations(operationList);

    // Operation handler set up
    operationHandler = new AttachTranscriptionOperationHandler();
    operationHandler.setTranscriptionService(service);
    operationHandler.setServiceRegistry(serviceRegistry);
    operationHandler.setCaptionService(captionService);
    operationHandler.setWorkspace(workspace);
    operationHandler.setJobBarrierPollingInterval(1L);
  }

  @Test
  public void testStartDfxp() throws Exception {
    EasyMock.expect(captionService.convert(EasyMock.anyObject(Attachment.class), EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class), EasyMock.anyObject(String.class))).andReturn(job1);
    EasyMock.replay(captionService);

    operation.setConfiguration(AttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID, "transcriptionJob");
    operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_FLAVOR, "captions/timedtext");
    operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_TAG, "tag1,tag2");
    operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_CAPTION_FORMAT, "dfxp");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    MediaPackage updatedMp = result.getMediaPackage();
    Catalog[] catalogs = updatedMp.getCatalogs(MediaPackageElementFlavor.parseFlavor("captions/timedtext"));

    Assert.assertNotNull(catalogs);
    Assert.assertEquals(1, catalogs.length);
    Assert.assertNotNull(catalogs[0].getTags());
    Assert.assertEquals(2, catalogs[0].getTags().length);
    Assert.assertEquals("tag1", catalogs[0].getTags()[0]);
    Assert.assertEquals("tag2", catalogs[0].getTags()[1]);
  }

  @Test
  public void testStartWebVtt() throws Exception {
    EasyMock.expect(captionService.convert(EasyMock.anyObject(Attachment.class), EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class), EasyMock.anyObject(String.class))).andReturn(job2);
    EasyMock.replay(captionService);

    operation.setConfiguration(AttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID, "transcriptionJob");
    operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_FLAVOR, "captions/timedtext");
    operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_TAG, "tag1,tag2");
    operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_CAPTION_FORMAT, "webvtt");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    MediaPackage updatedMp = result.getMediaPackage();
    Attachment[] attachments = updatedMp.getAttachments(MediaPackageElementFlavor.parseFlavor("captions/timedtext"));

    Assert.assertNotNull(attachments);
    Assert.assertEquals(1, attachments.length);
    Assert.assertNotNull(attachments[0].getTags());
    Assert.assertEquals(2, attachments[0].getTags().length);
    Assert.assertEquals("tag1", attachments[0].getTags()[0]);
    Assert.assertEquals("tag2", attachments[0].getTags()[1]);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testStartMissingTargetFlavor() throws Exception {
    operationHandler.start(workflowInstance, null);
  }
}
