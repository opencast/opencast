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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.presets.api.PresetProvider;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import org.apache.commons.lang.StringUtils;
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

  protected void setPresetProvider(PresetProvider presetProvider) {
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
   * @return The preset if available, none if not.
   */
  private Opt<String> getPreset(final Organization organization, final String seriesID, final String key) {
    // Check to see if the default value was set as a preset at the series or organization level
    return Opt.nul(presetProvider).bind(new Fn<PresetProvider, Opt<String>>() {
      @Override
      public Opt<String> ap(PresetProvider presetProvider) {
        try {
          return Opt.nul(presetProvider.getProperty(seriesID, key));
        } catch (NotFoundException e) {
          logger.debug(
                  "Unable to find preset with key {} from organization {} and series {} so we will be using the default value if it is available.",
                  new Object[] { key, organization, seriesID });
          return Opt.none();
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Applying default values to {}", workflowInstance.getId());
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    Long id = workflowInstance.getId();
    Organization organization = workflowInstance.getOrganization();
    String seriesID = workflowInstance.getMediaPackage().getSeries();
    // Iterate over all configuration keys
    Map<String, String> properties = new HashMap<String, String>();
    logger.debug("Getting properties for " + id + " " + organization + " " + seriesID);
    for (String key : operation.getConfigurationKeys()) {
      String value = workflowInstance.getConfiguration(key);
      if (StringUtils.trimToNull(value) == null) {
        // Check to see if the default value was set as a preset at the series or organization level
        Opt<String> preset = getPreset(organization, seriesID, key);
        if (preset.isSome() && StringUtils.trimToNull(preset.get()) != null) {
          properties.put(key, preset.get());
          logger.info("Configuration key '{}' of workflow {} is set to org or series preset value '{}'", new Object[] {
                  key, id, preset.get() });
        } else {
          String defaultValue = operation.getConfiguration(key);
          properties.put(key, defaultValue);
          logger.info("Configuration key '{}' of workflow {} is set to default value '{}' specified in workflow",
                  new Object[] { key, id, defaultValue });
        }
      } else {
        properties.put(key, value);
        logger.info("Configuration key '{}' of workflow {} is set to '{}' specified in event.", new Object[] { key, id, value });
      }
    }
    return createResult(workflowInstance.getMediaPackage(), properties, Action.CONTINUE, 0);
  }

}
