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

package org.opencastproject.workflow.handler.crop;

import org.opencastproject.crop.api.CropService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropWorkflowOperationHandlerTest {
  private CropWorkflowOperationHandler operationHandler;

  private MediaPackage mp;
  private Job job;
  private Track[] tracks;

  private Workspace workspace = null;
  private CropService cropService;

  public void setUp(final String resourceName) throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = getClass().getResource(resourceName).toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    tracks = mp.getTracks();

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(uriMP);
    EasyMock.replay(workspace);

    // set up mock receipt
    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getPayload()).andReturn(MediaPackageElementParser.getAsXml(tracks[0])).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job.getQueueTime()).andReturn(new Long(0)).anyTimes();
    EasyMock.replay(job);

    // set up mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job);
    EasyMock.replay(serviceRegistry);

    cropService = EasyMock.createNiceMock(CropService.class);
    EasyMock.expect(cropService.crop((Track) EasyMock.anyObject())).andReturn(job);
    EasyMock.replay(cropService);

    // set up service
    operationHandler = new CropWorkflowOperationHandler();
    operationHandler.setJobBarrierPollingInterval(0);
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
    operationHandler.setCropService(cropService);
  }

  @Test
  public void testWorkflow() throws Exception {
    setUp("/crop_mediapackage.xml");

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/delivery");

    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    MediaPackage mediaPackage = result.getMediaPackage();
    Track[] tracks = mediaPackage.getTracks();
    Assert.assertEquals(2, tracks.length);
    Track trackCropped = tracks[1];
    Assert.assertEquals("presenter/delivery", trackCropped.getFlavor().toString());
    Assert.assertArrayEquals(targetTags.split("\\W"), trackCropped.getTags());
  }

  @Test
  public void testNoVideo() throws Exception {
    setUp("/crop_mediapackage_no_video.xml");

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor", "presentation/source");

    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());
    Assert.assertNull(result.getMediaPackage());
  }


  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowInstance.WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation
        = new WorkflowOperationInstanceImpl("op", WorkflowOperationInstance.OperationState.RUNNING);
    operation.setState(WorkflowOperationInstance.OperationState.RUNNING);
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
