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

import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.conditionparser.WorkflowConditionInterpreter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class ConditionalConfigWorkflowOperationHandlerTest {
  private WorkflowInstance workflowInstance;
  private ConditionalConfigWorkflowOperationHandler operationHandler;
  private WorkflowOperationInstanceImpl operation;
  private static final String CONFIGURATION_VAR = "config";

  @Before
  public void setUp() throws Exception {
    // Operation handler to be tested
    operationHandler = new ConditionalConfigWorkflowOperationHandler();

    workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    // Media package does not matter for this test
    workflowInstance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());

    operation = new WorkflowOperationInstanceImpl("conditional-config", OperationState.RUNNING);
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.CONFIGURATION_NAME, CONFIGURATION_VAR);
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.CONDITION_PREFIX + "-1", "${step} == 1");
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.VALUE_PREFIX + "-1", "value 1");
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.CONDITION_PREFIX + "-2", "${step} == 2");
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.VALUE_PREFIX + "-2", "value 2");
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.CONDITION_PREFIX + "-3", "${step} == 3");
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.VALUE_PREFIX + "-3", "value 3");
    operation.setConfiguration(ConditionalConfigWorkflowOperationHandler.NO_MATCH, "value no-match");

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFirstConditionTrue() throws Exception {
    workflowInstance = replaceVars(workflowInstance, "step", "1");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("value 1", result.getProperties().get(CONFIGURATION_VAR));
  }

  @Test
  public void testThirdConditionTrue() throws Exception {
    workflowInstance = replaceVars(workflowInstance, "step", "3");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("value 3", result.getProperties().get(CONFIGURATION_VAR));
  }

  @Test
  public void testNoConditionTrue() throws Exception {
    workflowInstance = replaceVars(workflowInstance, "step", "99");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("value no-match", result.getProperties().get(CONFIGURATION_VAR));
  }

  private WorkflowInstance replaceVars(WorkflowInstance wf, String key, String value) throws WorkflowParsingException {
    HashMap<String, String> wfConfig = new HashMap<String, String>();
    wfConfig.put(key, value);

    // Replace operation configuration with variables form wf config
    final Function<String, String> systemVariableGetter = k -> null;
    String xml = WorkflowConditionInterpreter.replaceVariables(WorkflowParser.toXml(wf),
            systemVariableGetter, wfConfig, false);
    return WorkflowParser.parseWorkflowInstance(xml);
  }

}
