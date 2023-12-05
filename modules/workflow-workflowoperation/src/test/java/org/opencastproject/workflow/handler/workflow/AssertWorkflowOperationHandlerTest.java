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
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.XmlWorkflowParser;
import org.opencastproject.workflow.conditionparser.WorkflowConditionInterpreter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class AssertWorkflowOperationHandlerTest {
  private WorkflowInstance workflowInstance;
  private AssertWorkflowOperationHandler operationHandler;
  private WorkflowOperationInstance operation;

  @Before
  public void setUp() throws Exception {
    // Operation handler to be tested
    operationHandler = new AssertWorkflowOperationHandler();

    workflowInstance = new WorkflowInstance();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    // Media package does not matter for this test
    workflowInstance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testThatTrueFalseConditionTrue() throws Exception {
    operation = new WorkflowOperationInstance("assert", OperationState.RUNNING);
    operation.setConfiguration(AssertWorkflowOperationHandler.THAT_PREFIX + "-1", "${conditionThat} == 1");
    operation.setConfiguration(AssertWorkflowOperationHandler.TRUE_PREFIX + "-1", "${conditionTrue}");
    operation.setConfiguration(AssertWorkflowOperationHandler.FALSE_PREFIX + "-1", "${conditionFalse}");

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    workflowInstance = replaceVars(workflowInstance, "conditionThat", "1");
    workflowInstance = replaceVars(workflowInstance, "conditionTrue", "true");
    workflowInstance = replaceVars(workflowInstance, "conditionFalse", "false");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  @Test(expected = WorkflowOperationException.class)
  public void testTrueConditionFail() throws Exception {
    operation = new WorkflowOperationInstance("assert", OperationState.RUNNING);
    operation.setConfiguration(AssertWorkflowOperationHandler.THAT_PREFIX + "-1", "${conditionThat} == 1");
    operation.setConfiguration(AssertWorkflowOperationHandler.TRUE_PREFIX + "-1", "${conditionTrue}");
    operation.setConfiguration(AssertWorkflowOperationHandler.FALSE_PREFIX + "-1", "${conditionFalse}");

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    workflowInstance = replaceVars(workflowInstance, "conditionThat", "1");
    workflowInstance = replaceVars(workflowInstance, "conditionTrue", "false");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testFalseConditionFail() throws Exception {
    operation = new WorkflowOperationInstance("assert", OperationState.RUNNING);
    operation.setConfiguration(AssertWorkflowOperationHandler.TRUE_PREFIX + "-1", "${conditionTrue}");
    operation.setConfiguration(AssertWorkflowOperationHandler.FALSE_PREFIX + "-1", "${conditionFalse}");

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    workflowInstance = replaceVars(workflowInstance, "conditionTrue", "true");
    workflowInstance = replaceVars(workflowInstance, "conditionFalse", "true");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testThatConditionFail() throws Exception {
    operation = new WorkflowOperationInstance("assert", OperationState.RUNNING);
    operation.setConfiguration(AssertWorkflowOperationHandler.TRUE_PREFIX + "-1", "${conditionTrue}");
    operation.setConfiguration(AssertWorkflowOperationHandler.THAT_PREFIX + "-1", "${conditionThat} == 1");

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    workflowInstance = replaceVars(workflowInstance, "conditionThat", "-1");
    workflowInstance = replaceVars(workflowInstance, "conditionTrue", "true");

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
  }

  private WorkflowInstance replaceVars(WorkflowInstance wf, String key, String value) throws WorkflowParsingException {
    HashMap<String, String> wfConfig = new HashMap<String, String>();
    wfConfig.put(key, value);

    // Replace operation configuration with variables form wf config
    final Function<String, String> systemVariableGetter = k -> null;
    String xml = WorkflowConditionInterpreter.replaceVariables(XmlWorkflowParser.toXml(wf),
            systemVariableGetter, wfConfig, false);
    return XmlWorkflowParser.parseWorkflowInstance(xml);
  }

}
