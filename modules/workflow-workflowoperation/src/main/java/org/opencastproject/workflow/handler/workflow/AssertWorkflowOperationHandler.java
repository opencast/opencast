/*
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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.conditionparser.WorkflowConditionInterpreter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Operation to assert preconditions
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Assertion Workflow Operation Handler",
        "workflow.operation=assert"
    }
)
public class AssertWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AssertWorkflowOperationHandler.class);

  /** Assert that config key prefix */
  public static final String THAT_PREFIX = "that-";

  public static final String TRUE_PREFIX = "true-";

  public static final String FALSE_PREFIX = "false-";


  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    for (String key : currentOperation.getConfigurationKeys().stream()
        .filter(x -> x.startsWith(THAT_PREFIX) || x.startsWith(TRUE_PREFIX) || x.startsWith(FALSE_PREFIX)).sorted()
        .collect(Collectors.toList())) {

      String assertion = currentOperation.getConfiguration(key);

      boolean result = false;

      try {
        result = WorkflowConditionInterpreter.interpret(assertion.trim());
        logger.debug("Evaluate assertion {}: {}, result: {}", key, assertion, result);
      } catch (IllegalArgumentException e) {
        logger.error("Invalid assertion {}: {}", key, assertion);
        throw new WorkflowOperationException(String.format("Invalid assertion %s: %s", key, assertion), e);
      }

      // THAT and TRUE expects true
      boolean expectedResult = !key.startsWith(FALSE_PREFIX);

      if (result != expectedResult) {
        logger.error("Assertion {} [{}] failed.", key, assertion);
        currentOperation.setState(WorkflowOperationInstance.OperationState.FAILED);
        throw new WorkflowOperationException(String.format("Assertion %s [%s] failed.", key, assertion));
      }
    }

    return createResult(WorkflowOperationResult.Action.CONTINUE);
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }
}
