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
import org.opencastproject.presets.api.PresetProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow operation handler for setting default values.
 * <p>
 * In cases where a workflow parameters are not specified, e. g. during ad-hoc recordings, this operation handler helps
 * specify the default values.
 */
public class DefaultsWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DefaultsWorkflowOperationHandler.class);

  private PresetProvider presetProvider;

  void setPresetProvider(PresetProvider presetProvider) {
    this.presetProvider = presetProvider;
  }

  /**
   * Gets a series or organization preset if it is present.
   *
   * @param organization
   *          The organization to check for organization level presets.
   * @param seriesID
   *          The id of the series to check for series level presets.
   * @param key
   *          The key name for the preset to check for.
   * @return The preset if available, null if not.
   */
  private String getPreset(final String organizationId, final String seriesID, final String key) {
    // Check to see if the default value was set as a preset at the series or organization level
    try {
      if (presetProvider != null) {
        return presetProvider.getProperty(seriesID, key);
      }
    } catch (NotFoundException e) {
      logger.debug("No preset for key {} from organization {} and series {}. Using the default value if available.",
                   key, organizationId, seriesID);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Applying default values to {}", workflowInstance.getId());
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    Long id = workflowInstance.getId();
    String organizationId = workflowInstance.getOrganizationId();
    String seriesID = workflowInstance.getMediaPackage().getSeries();
    // Iterate over all configuration keys
    Map<String, String> properties = new HashMap<>();
    logger.debug("Getting properties for {} {} {}", id, organizationId, seriesID);
    for (String key : operation.getConfigurationKeys()) {
      String value = workflowInstance.getConfiguration(key);
      if (StringUtils.isBlank(value)) {
        // Check to see if the default value was set as a preset at the series or organization level
        String preset = getPreset(organizationId, seriesID, key);
        if (StringUtils.isNotBlank(preset)) {
          properties.put(key, preset);
          logger.debug("Configuration key '{}' of workflow {} is set to preset value '{}'", key, id, preset);
        } else {
          String defaultValue = operation.getConfiguration(key);
          properties.put(key, defaultValue);
          logger.debug("Configuration key '{}' of workflow {} is set to default value '{}' specified in workflow", key,
                  id, defaultValue);
        }
      } else {
        properties.put(key, value);
        logger.debug("Configuration key '{}' of workflow {} is set to '{}' specified in event.", key, id, value);
      }
    }
    return createResult(workflowInstance.getMediaPackage(), properties, Action.CONTINUE, 0);
  }

}
