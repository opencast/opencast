/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.workflow.handler;

import org.opencastproject.gstreamer.service.api.GStreamerLaunchException;
import org.opencastproject.gstreamer.service.api.GStreamerService;
import org.opencastproject.job.api.JaxbJobContext;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.classextension.EasyMock;
import org.gstreamer.Gst;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class ExportWorkflowOperationHandlerTest {

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(ExportWorkflowOperationHandlerTest.class.getName());

  /** True if the environment provides the tools needed for the test suite */
  private static boolean gstreamerInstalled = true;

  private ExportWorkflowOperationHandler exportWorkflowOperationHandler = new ExportWorkflowOperationHandler();
  
  private static Workspace workspace;
  
  private MediaPackage mediaPackage;

  private GStreamerService gstreamerService;
  
  private ServiceRegistry serviceRegistry;
  
  private URI movieFileURI;
  private URI complicatedFileURI;
  private URI withoutGStreamerURI;
  private URI withGStreamerURI;
  private URI randomXMLURI;
  
  
  /** Make sure that gstreamer is installed and available. **/
  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping gstreamer related tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setUp() throws NotFoundException, IOException, URISyntaxException, GStreamerLaunchException, MediaPackageException, ServiceRegistryException {
     if (!gstreamerInstalled) {
       return;
    }
    movieFileURI = ExportWorkflowOperationHandlerTest.class.getResource("/av.mov").toURI();
    complicatedFileURI = ExportWorkflowOperationHandlerTest.class.getResource(
            "/with.complicated.gstreamer.properties").toURI();
    withGStreamerURI = ExportWorkflowOperationHandlerTest.class.getResource("/with.gstreamer.properties").toURI();
    withoutGStreamerURI = ExportWorkflowOperationHandlerTest.class.getResource("/without.gstreamer.properties")
            .toURI();
    randomXMLURI = ExportWorkflowOperationHandlerTest.class.getResource("/compose_encode_mediapackage.xml").toURI();

    Attachment binaryFile = AttachmentImpl.fromURI(movieFileURI);
    Attachment withoutGStreamerFile = AttachmentImpl.fromURI(withoutGStreamerURI);
    Attachment withGStreamer = AttachmentImpl.fromURI(withGStreamerURI);
    Attachment randomXML = AttachmentImpl.fromURI(randomXMLURI);

    Attachment[] attachments = { binaryFile, withoutGStreamerFile, randomXML, withGStreamer };

    Track movieTrack = TrackImpl.fromURI(movieFileURI);
    movieTrack.setIdentifier("track-1");
    Track[] tracks = { movieTrack };
    
    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.clone()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI));
    EasyMock.expect(workspace.get(withoutGStreamerURI)).andReturn(new File(withoutGStreamerURI));
    EasyMock.expect(workspace.get(withGStreamerURI)).andReturn(new File(withGStreamerURI));
    EasyMock.expect(workspace.get(randomXMLURI)).andReturn(new File(randomXMLURI));
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI)).anyTimes();
    EasyMock.expect(workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock.anyObject())).andReturn(movieFileURI);
    EasyMock.replay(workspace);

    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getStatus()).andReturn(Status.FINISHED).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(10L).anyTimes();
    EasyMock.replay(job);
    
    gstreamerService = EasyMock.createNiceMock(GStreamerService.class);
    EasyMock.expect(gstreamerService.launch((MediaPackage)EasyMock.anyObject(), (String)EasyMock.anyObject(), (String)EasyMock.anyObject())).andReturn(job).anyTimes();
    EasyMock.replay(gstreamerService);
    
    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.replay(serviceRegistry);
  }

  /**
   * Test it correctly processes attachments
   * 
   * @throws Exception
   */

  @Test
  public void testAttachmentProcessing() throws Exception {
    if (!gstreamerInstalled) {
       return; 
    }
    exportWorkflowOperationHandler = new ExportWorkflowOperationHandler();
    exportWorkflowOperationHandler.setWorkspace(workspace);
    exportWorkflowOperationHandler.setGStreamerService(gstreamerService);
    exportWorkflowOperationHandler.setServiceRegistry(serviceRegistry);
    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    exportWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
  }

  @Test
  public void noGStreamerLineInProperties() throws NotFoundException, IOException {
     if (!gstreamerInstalled) {
       return;
    }
    Attachment binaryFile = AttachmentImpl.fromURI(movieFileURI);
    Attachment withoutGStreamerFile = AttachmentImpl.fromURI(withoutGStreamerURI);
    Attachment randomXML = AttachmentImpl.fromURI(randomXMLURI);

    Attachment[] attachments = { binaryFile, withoutGStreamerFile, randomXML };

    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI));
    EasyMock.expect(workspace.get(withoutGStreamerURI)).andReturn(new File(withoutGStreamerURI));
    EasyMock.expect(workspace.get(randomXMLURI)).andReturn(new File(randomXMLURI));
    EasyMock.replay(workspace);

    exportWorkflowOperationHandler = new ExportWorkflowOperationHandler();
    exportWorkflowOperationHandler.setWorkspace(workspace);
    exportWorkflowOperationHandler.setGStreamerService(gstreamerService);
    exportWorkflowOperationHandler.setServiceRegistry(serviceRegistry);
    
    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    try {
      exportWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
      Assert.fail();
    } catch (WorkflowOperationException e) {

    }
  }

  @Test
  public void noTracksAvailable() throws NotFoundException, IOException {
     if (!gstreamerInstalled) {
       return;
    }
    Attachment withGStreamer = AttachmentImpl.fromURI(withGStreamerURI);
    Attachment[] attachments = { withGStreamer };

    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(withGStreamerURI)).andReturn(new File(withGStreamerURI));
    EasyMock.replay(workspace);

    exportWorkflowOperationHandler = new ExportWorkflowOperationHandler();
    exportWorkflowOperationHandler.setWorkspace(workspace);
    exportWorkflowOperationHandler.setGStreamerService(gstreamerService);
    exportWorkflowOperationHandler.setServiceRegistry(serviceRegistry);
    
    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    try {
      exportWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
      Assert.fail();
    } catch (WorkflowOperationException e) {

    }
  }

  @Test
  public void complicatedGStreamerLineInProperties() throws NotFoundException, IOException, WorkflowOperationException {
    if (!gstreamerInstalled) {
       return;
    }
    Attachment complicatedGStreamerFile = AttachmentImpl.fromURI(complicatedFileURI);
    Attachment[] attachments = { complicatedGStreamerFile };
    Track movieTrack = TrackImpl.fromURI(movieFileURI);
    movieTrack.setIdentifier("track-1");
    Track[] tracks = { movieTrack };
    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(complicatedFileURI)).andReturn(new File(complicatedFileURI));
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI)).anyTimes();
    EasyMock.replay(workspace);

    exportWorkflowOperationHandler = new ExportWorkflowOperationHandler();
    exportWorkflowOperationHandler.setWorkspace(workspace);
    exportWorkflowOperationHandler.setGStreamerService(gstreamerService);
    exportWorkflowOperationHandler.setServiceRegistry(serviceRegistry);
    
    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);

    exportWorkflowOperationHandler.resume(workflowInstance, jobContext, null);

  }
}
