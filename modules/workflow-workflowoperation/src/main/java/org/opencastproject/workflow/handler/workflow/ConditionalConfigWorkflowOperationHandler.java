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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.conditionparser.WorkflowConditionInterpreter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ConditionalConfigWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  public static final String CONFIGURATION_NAME = "configuration-name";
  public static final String CONDITION_PREFIX = "condition-";
  public static final String VALUE_PREFIX = "value-";
  public static final String NO_MATCH = "no-match";

  private static final Logger logger = LoggerFactory.getLogger(ConditionalConfigWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    Map<String, String> properties = new HashMap<>();

    // Get name of wf configuration to be set
    String wfConfigName = operation.getConfiguration(CONFIGURATION_NAME);
    if (StringUtils.isEmpty(wfConfigName)) {
      logger.warn("No workflow configuration was set because {} was not passed. Operation skipped.",
              CONFIGURATION_NAME);
      return createResult(Action.SKIP);
    }

    String value = "false";
    // Loop through all "condition-" keys
    for (String key : operation.getConfigurationKeys().stream().filter(x -> x.startsWith(CONDITION_PREFIX))
            .sorted().collect(Collectors.toList())) {
      // Evaluate condition
      String condition = operation.getConfiguration(key);
      if (StringUtils.isEmpty(condition)) {
        continue;
      }
      boolean result = false;
      try {
        result = WorkflowConditionInterpreter.interpret(condition.trim());
        logger.debug("Evaluate condition: {}, result: {}", condition, result);
      } catch (IllegalArgumentException e) {
        logger.error("Invalid condition to evaluate: {}.", condition);
        throw new WorkflowOperationException(String.format("Invalid condition: %s", condition.trim()), e);
      }
      // If condition true, set variable and return
      if (result) {
        String valuePropName = VALUE_PREFIX + key.substring(CONDITION_PREFIX.length());
        value = operation.getConfiguration(valuePropName);
        if (StringUtils.isEmpty(value)) {
          logger.error("Condition {}  evaluated to true, but no value informed to set {} variable in {}",
                  condition.trim(), wfConfigName, valuePropName);
          throw new WorkflowOperationException(
                  String.format("Condition: %s does not have a value set.", condition.trim()));
        }
         // Replace workflow configuration, even if already set.
        properties.put(wfConfigName, value.trim());
        logger.debug("Configuration key '{}' of workflow {} is set to value '{}'", wfConfigName, id, value.trim());
        return createResult(workflowInstance.getMediaPackage(), properties, Action.CONTINUE, 0);
      }
    }

    // If we got here, no condition was evaluated true so we use the no-match configuration if there
    String noMatch = operation.getConfiguration(NO_MATCH);
    if (StringUtils.isNotEmpty(NO_MATCH)) {
      properties.put(wfConfigName, noMatch);
      logger.debug("Configuration key '{}' of workflow {} is set to value '{}'", wfConfigName, id, noMatch);
      return createResult(workflowInstance.getMediaPackage(), properties, Action.CONTINUE, 0);
    }
    return createResult(Action.SKIP);
  }

}
