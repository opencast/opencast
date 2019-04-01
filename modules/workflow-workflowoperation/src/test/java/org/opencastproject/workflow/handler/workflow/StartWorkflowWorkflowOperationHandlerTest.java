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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.workflow.handler.workflow.StartWorkflowWorkflowOperationHandler.MEDIA_PACKAGE_ID;
import static org.opencastproject.workflow.handler.workflow.StartWorkflowWorkflowOperationHandler.WORKFLOW_DEFINITION;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;
import com.google.common.collect.Lists;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class StartWorkflowWorkflowOperationHandlerTest {

  private StartWorkflowWorkflowOperationHandler operationHandler;
  private AssetManager assetManager;
  private WorkflowService workflowService;
  private WorkflowOperationInstanceImpl operation;
  private WorkflowInstanceImpl workflowInstance;
  private static final String MP_ID = "c3066908-39e3-44b1-842a-9ae93ef8d314";
  private static final String WD_ID = "test-workflow";

  @Before
  public void setUp() throws Exception {
    assetManager = createNiceMock(AssetManager.class);
    expect(assetManager.getMediaPackage(anyString())).andReturn(Opt.none()).anyTimes();

    workflowService = createNiceMock(WorkflowService.class);

    replay(assetManager, workflowService);

    operationHandler = new StartWorkflowWorkflowOperationHandler();
    operationHandler.setAssetManager(assetManager);
    operationHandler.setWorkflowService(workflowService);

    operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("start-workflow");
    operation.setState(OperationState.RUNNING);
    operation.setConfiguration(MEDIA_PACKAGE_ID, MP_ID);
    operation.setConfiguration(WORKFLOW_DEFINITION, WD_ID);
    operation.setConfiguration("workflowConfigurations", "true");
    operation.setConfiguration("key", "value");

    workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setOperations(Lists.newArrayList(operation));
  }

  @Test(expected = WorkflowOperationException.class)
  public void testNoMediaPackage() throws Exception {
    operationHandler.start(workflowInstance, null);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testNoWorkflowDefinition() throws Exception {
    reset(workflowService);
    expect(workflowService.getWorkflowDefinitionById(WD_ID)).andThrow(new NotFoundException());
    replay(workflowService);

    operationHandler.start(workflowInstance, null);
  }

  @Test
  public void testStartWorkflow() throws Exception {
    // Media Package
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.setIdentifier(new IdImpl(MP_ID));

    // Asset Manager
    reset(assetManager);
    expect(assetManager.getMediaPackage(anyString())).andReturn(Opt.some(mp));

    // Workflow Service
    WorkflowDefinition wd = new WorkflowDefinitionImpl();
    wd.setId(WD_ID);

    Capture<Map<String, String>> wProperties = newCapture();

    reset(workflowService);
    expect(workflowService.getWorkflowDefinitionById(WD_ID)).andReturn(wd);
    expect(workflowService.start(eq(wd), eq(mp), capture(wProperties))).andReturn(null);

    replay(assetManager, workflowService);

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);

    verify(assetManager, workflowService);

    assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());
    assertEquals(2, wProperties.getValue().size());
    assertEquals("true", wProperties.getValue().get("workflowConfigurations"));
    assertEquals("value", wProperties.getValue().get("key"));
  }
}
