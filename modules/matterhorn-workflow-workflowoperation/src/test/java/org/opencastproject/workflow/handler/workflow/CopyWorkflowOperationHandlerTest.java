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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CopyWorkflowOperationHandlerTest {

  private CopyWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private File videoFile;

  // mock services and objects
  private Workspace workspace = null;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = getClass().getResource("/copy_mediapackage.xml").toURI();

    mp = builder.loadFromXml(uriMP.toURL().openStream());

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);

    // set up service
    operationHandler = new CopyWorkflowOperationHandler();

    // Prepare file to returne from workflow
    videoFile = new File(getClass().getResource("/av.mov").toURI());
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(videoFile).anyTimes();
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testSuccessfullCopy() throws Exception {
    String copyTargetDirectory = videoFile.getParent();
    String copyFileName = "testCopy";
    StringBuilder sb = new StringBuilder().append(copyTargetDirectory).append("/").append(copyFileName).append(".mov");

    File copy = new File(sb.toString());

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS, "presentation/source");
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_TAGS, "first");
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY, copyTargetDirectory);
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_FILENAME, copyFileName);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertTrue(copy.exists());
  }

  @Test
  public void testOptionalFilename() throws Exception {
    File copy = new File(UrlSupport.concat(videoFile.getParent(), "copy", videoFile.getName()));
    String copyTargetDirectory = copy.getParent();
    FileUtils.forceMkdir(copy);

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS, "presentation/source");
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_TAGS, "first");
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY, copyTargetDirectory);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertTrue(copy.exists());
  }

  @Test
  public void testCopyWithNoElementMatchingFlavorsTags() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS, "no-video/source");
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_TAGS, "engage,rss");
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY, videoFile.getParent());
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_FILENAME, "testCopy");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testCopyWithTwoElementsMatchingFlavorsTags() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS, "*/source");
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY, videoFile.getParent());
    configurations.put(CopyWorkflowOperationHandler.OPT_TARGET_FILENAME, "testCopy");

    // run the operation handler
    try {
      WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
      Assert.assertEquals(Action.CONTINUE, result.getAction());
    } catch (WorkflowOperationException e) {
      Assert.fail("The workflow should ");
    }
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("copy");
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
