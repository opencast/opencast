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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

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

public class CloneWorkflowOperationHandlerTest {

  private CloneWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;

  // mock services and objects
  private Workspace workspace = null;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = getClass().getResource("/clone_mediapackage.xml").toURI();

    mp = builder.loadFromXml(uriMP.toURL().openStream());

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);

    // set up service
    operationHandler = new CloneWorkflowOperationHandler();

    // Prepare file to returne from workflow
    File videoFile = new File(getClass().getResource("/av.mov").toURI());
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(videoFile).anyTimes();
    EasyMock.replay(workspace);

    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testSingleSourceFlavor() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CloneWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "presentation/source");
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/target");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("presentation/target");
    Assert.assertTrue(result.getMediaPackage().getElementsByFlavor(newFlavor).length == 1);
  }

  @Test
  public void testWildcardSourceFlavor() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CloneWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source");
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/target");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("*/target");
    Assert.assertTrue(result.getMediaPackage().getElementsByFlavor(newFlavor).length == 2);
  }

  @Test
  public void testSpecificTargetFlavorType() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CloneWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source");
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "targettype/target");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("targettype/target");
    Assert.assertTrue(result.getMediaPackage().getElementsByFlavor(newFlavor).length == 2);
  }

  @Test
  public void testWildcardTargetFlavorSubtype() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CloneWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source");
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "targettype/*");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("targettype/source");
    Assert.assertEquals(2, result.getMediaPackage().getElementsByFlavor(newFlavor).length);
  }

  @Test
  public void testWildcardTargetFlavorTypeAndSubtype() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<>();
    configurations.put(CloneWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source");
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/*");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("presentation/source");
    Assert.assertEquals(2, result.getMediaPackage().getElementsByFlavor(newFlavor).length);


    newFlavor = MediaPackageElementFlavor.parseFlavor("presenter/source");
    Assert.assertEquals(2, result.getMediaPackage().getElementsByFlavor(newFlavor).length);
  }

  @Test
  public void testTagsAsSourceSelector() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CloneWorkflowOperationHandler.OPT_SOURCE_TAGS, "first");
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/target");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("*/target");
    Assert.assertEquals(1, result.getMediaPackage().getElementsByFlavor(newFlavor).length);
  }

  @Test
  public void testNoSourceFlavor() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(CloneWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/target");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.SKIP, result.getAction());
    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("*/target");
    Assert.assertEquals(0, result.getMediaPackage().getElementsByFlavor(newFlavor).length);
  }


  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstance workflowInstance = new WorkflowInstance();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstance operation = new WorkflowOperationInstance("op", OperationState.RUNNING);
    operation.setTemplate("clone");
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
