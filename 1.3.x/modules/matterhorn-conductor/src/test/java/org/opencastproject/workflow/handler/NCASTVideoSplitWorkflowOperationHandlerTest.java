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

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class NCASTVideoSplitWorkflowOperationHandlerTest {
  private static final Logger logger = LoggerFactory.getLogger(NCASTVideoSplitWorkflowOperationHandlerTest.class);

  private NCASTVideoSplitWorkflowOperationHandler operationHandler;

  private Workspace workspace;
  private ComponentContext componentContext;
  private BundleContext bundleContext;
  private Hashtable<String, String> dictionary;
  private MediaInspectionService inspectionService;
  private MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
  private ServiceRegistry serviceRegistry;

  @Before
  public void setUp() throws Exception {
    // test resources
    File testFile = new File("src/test/resources/split.mp4");
    Dictionary<String, String> componentProperties = new Hashtable<String, String>();
    componentProperties.put(WorkflowService.WORKFLOW_OPERATION_PROPERTY, "");
    componentProperties.put(Constants.SERVICE_DESCRIPTION, "");

    URI uriMP = NCASTVideoSplitWorkflowOperationHandler.class.getResource("/split_with_video.xml").toURI();

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(uriMP);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(testFile);
    URI returnedFile1URI = new URI("/tmp/split1_split.mp4");
    URI returnedFile2URI = new URI("/tmp/split2_split.mp4");
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(returnedFile1URI);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(returnedFile2URI);
    EasyMock.replay(workspace);

    // set up the component and bundle contexts.
    bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.storage.dir")).andReturn("/tmp");
    EasyMock.replay(bundleContext);
    componentContext = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(componentContext.getProperties()).andReturn(dictionary);
    EasyMock.expect(componentContext.getProperties()).andReturn(dictionary);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext);
    EasyMock.replay(componentContext);

  }

  @Test
  public void testSplitWithNoVideoTracks() throws Exception {
    // build the mediapackage
    URI uriMP = NCASTVideoSplitWorkflowOperationHandler.class.getResource("/split_without_tracks.xml").toURI();
    MediaPackage mpWithoutTracks = builder.loadFromXml(uriMP.toURL().openStream());
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-media-flavor", "presentation/source");

    try {
      operationHandler = new NCASTVideoSplitWorkflowOperationHandler();
    } catch (Throwable e) {
      logger.warn("Gstreamer not properly installed... skipping {}", this.getClass().getName());
      return;
    }
    operationHandler.setWorkspace(workspace);
    operationHandler.activate(componentContext);
    try {
      getWorkflowOperationResult(mpWithoutTracks, configurations);
      Assert.fail();
    } catch (WorkflowOperationException exc) {
      // It should exception here, as there are no Quicktime tracks
    }
  }

  @Test
  public void testSplitWithWrongVideoMimeType() throws Exception {
    // build the start mediapackage
    URI uriMP = NCASTVideoSplitWorkflowOperationHandler.class.getResource("/split_with_wrong_video_mimetype.xml")
            .toURI();
    MediaPackage mpWithWrongVideoMimeType = builder.loadFromXml(uriMP.toURL().openStream());
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-media-flavor", "presentation/source");

    try {
      operationHandler = new NCASTVideoSplitWorkflowOperationHandler();
    } catch (Throwable e) {
      logger.warn("Gstreamer not properly installed... skipping {}", this.getClass().getName());
      return;
    }
    operationHandler.setWorkspace(workspace);
    operationHandler.activate(componentContext);
    try {
      getWorkflowOperationResult(mpWithWrongVideoMimeType, configurations);
      Assert.fail();
    } catch (WorkflowOperationException exc) {
      // It should exception here, as there are no Quicktime tracks
    }
  }

  @Test
  public void testSplitWithDefinedVideo() throws Exception {
    Map<String, String> configurations = new HashMap<String, String>();
    WorkflowOperationResult result = null;

    // build the start mediapackage
    URI uriMP = NCASTVideoSplitWorkflowOperationHandler.class.getResource("/split_with_video.xml").toURI();
    MediaPackage mpWithVideo = builder.loadFromXml(uriMP.toURL().openStream());

    Track[] splitTracks = null;
    // build the resultant mediapackage, get the tracks.
    uriMP = NCASTVideoSplitWorkflowOperationHandler.class.getResource("/split_with_video_and_tracks.xml").toURI();
    splitTracks = builder.loadFromXml(uriMP.toURL().openStream()).getTracks();

    // Set up the jobs to be returned
    Job job1 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job1.getPayload()).andReturn(MediaPackageElementParser.getAsXml(splitTracks[1])).anyTimes();
    EasyMock.expect(job1.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job1.getId()).andReturn(new Long(1));
    EasyMock.replay(job1);

    Job job2 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job2.getPayload()).andReturn(MediaPackageElementParser.getAsXml(splitTracks[2])).anyTimes();
    EasyMock.expect(job2.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job2.getId()).andReturn(new Long(2));
    EasyMock.replay(job2);

    // Build the service registry
    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(new Long(1))).andReturn(job1);
    EasyMock.expect(serviceRegistry.getJob(new Long(2))).andReturn(job2);
    EasyMock.replay(serviceRegistry);

    // Set up the inspection service.
    inspectionService = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(inspectionService.inspect((URI) new URI("/tmp/split1_split.mp4"))).andReturn(job1).anyTimes();
    EasyMock.expect(inspectionService.inspect((URI) new URI("/tmp/split2_split.mp4"))).andReturn(job2).anyTimes();
    EasyMock.replay(inspectionService);

    try {
      operationHandler = new NCASTVideoSplitWorkflowOperationHandler();
    } catch (Throwable e) {
      logger.warn("Gstreamer not properly installed... skipping {}", this.getClass().getName());
      return;
    }
    operationHandler.setWorkspace(workspace);
    operationHandler.activate(componentContext);
    operationHandler.setInspectionService(inspectionService);
    operationHandler.setServiceRegistry(serviceRegistry);
    try {
      configurations.put("source-media-flavor", "presentation/source");
      result = getWorkflowOperationResult(mpWithVideo, configurations);
    } catch (WorkflowOperationException exc) {
      Assert.fail("Exception happened! Video should have split successfully!\n" + exc.getMessage());
    }
    Track[] tracks = result.getMediaPackage().getTracks();
    Assert.assertEquals("tmp/split1_split.mp4", tracks[1].getURI().toString());
    Assert.assertEquals("tmp/split2_split.mp4", tracks[2].getURI().toString());
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setId(new Long(1));
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }
}
