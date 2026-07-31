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
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Take look in specified catalog for specified term, if the value matches the specified value add the target-tags
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Configure by Dublin Core Term Workflow Handler",
        "workflow.operation=configure-by-dcterm"
    }
)
public class ConfigureByDublinCoreTermWOH extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory.getLogger(ConfigureByDublinCoreTermWOH.class);

  /** Name of the configuration option that provides the catalog to examine */
  public static final String DCCATALOG_PROPERTY = "dccatalog";

  /** Flavor type of the catalog in case it is not dublincore */
  public static final String DCCATALOG_TYPE_PROPERTY = "dccatalog-type";

  /** Name of the configuration option that provides Dublin Core term/element */
  public static final String DCTERM_PROPERTY = "dcterm";

  /** Name of the configuration option that provides term's default value if not present */
  public static final String DEFAULT_VALUE_PROPERTY = "default-value";

  /** Name of the configuration option that provides value to match */
  public static final String MATCH_VALUE_PROPERTY = "match-value";

  /** Name of the configuration option that provides the copy boolean we are looking for */
  public static final String COPY_PROPERTY = "copy";

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String configuredCatalog = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCCATALOG_PROPERTY));
    Optional<String> optConfiguredCatalogType = Optional.ofNullable(StringUtils.trimToNull(
        currentOperation.getConfiguration(DCCATALOG_TYPE_PROPERTY)));
    String catalogType = optConfiguredCatalogType.isPresent() ? optConfiguredCatalogType.get() : "dublincore";
    String configuredDCTerm = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCTERM_PROPERTY));
    String configuredDefaultValue = StringUtils.trimToNull(currentOperation.getConfiguration(DEFAULT_VALUE_PROPERTY));
    String configuredMatchValue = StringUtils.trimToEmpty(currentOperation.getConfiguration(MATCH_VALUE_PROPERTY));

    // Find Catalog
    Catalog[] catalogs = mediaPackage
            .getCatalogs(new MediaPackageElementFlavor(catalogType, StringUtils.lowerCase(configuredCatalog)));

    if (catalogs != null && catalogs.length > 0) {
      Boolean foundValue = false;

      // Find DCTerm
      for (Catalog catalog : catalogs) {
        DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);
        // Match Value
        List<DublinCoreValue> values = dc.getProperties().stream()
            .filter(property -> property.getLocalName().equals(configuredDCTerm))
            .flatMap(property -> dc.get(property).stream())
            .toList();
        if (values.isEmpty()) {
          // Use default
          if (configuredDefaultValue != null) {
            foundValue = configuredDefaultValue.equals(configuredMatchValue);
          }
        } else {
          foundValue = values.contains(DublinCoreValue.mk(configuredMatchValue));
        }
      }

      if (foundValue) {
        Map<String, String> properties = new HashMap<>();

        for (String key : currentOperation.getConfigurationKeys()) {
          // Ignore this operations configuration
          if (DCCATALOG_PROPERTY.equals(key) || DCTERM_PROPERTY.equals(key) || DEFAULT_VALUE_PROPERTY.equals(key)
                  || MATCH_VALUE_PROPERTY.equals(key)) {
            continue;
          }

          String value = currentOperation.getConfiguration(key);
          properties.put(key, value);
          logger.info("Configuration key '{}' of workflow {} is set to value '{}'", key, workflowInstance.getId(),
                  value);
        }

        return createResult(mediaPackage, properties, Action.CONTINUE, 0);

      } // if foundValue
    } // if catalogs

    return createResult(mediaPackage, Action.CONTINUE);
  }
}
