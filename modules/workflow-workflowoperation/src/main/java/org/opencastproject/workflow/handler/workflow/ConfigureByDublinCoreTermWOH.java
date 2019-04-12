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

import static org.opencastproject.metadata.dublincore.DublinCore.TERMS_NS_URI;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Take look in specified catalog for specified term, if the value matches the specified value add the target-tags
 */
public class ConfigureByDublinCoreTermWOH extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory.getLogger(ConfigureByDublinCoreTermWOH.class);

  /** Name of the configuration option that provides the catalog to examine */
  public static final String DCCATALOG_PROPERTY = "dccatalog";

  /** Name of the configuration option that provides Dublin Core term/element */
  public static final String DCTERM_PROPERTY = "dcterm";

  /** Name of the configuration option that provides the catalog to examine */
  public static final String EXTERNAL_CATALOG_PROPERTY = "externalcatalog";

  /** Name of the configuration option that provides term's default value if not present */
  public static final String DEFAULT_VALUE_PROPERTY = "default-value";

  /** Name of the configuration option that provides value to match */
  public static final String MATCH_VALUE_PROPERTY = "match-value";

  /** Name of the configuration option that provides the copy boolean we are looking for */
  public static final String COPY_PROPERTY = "copy";

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String configuredCatalog = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCCATALOG_PROPERTY));
    String configuredDCTerm = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCTERM_PROPERTY));
    String configuredExternalCatalog = StringUtils.trimToEmpty(currentOperation.getConfiguration(EXTERNAL_CATALOG_PROPERTY));
    String configuredDefaultValue = StringUtils.trimToNull(currentOperation.getConfiguration(DEFAULT_VALUE_PROPERTY));
    String configuredMatchValue = StringUtils.trimToEmpty(currentOperation.getConfiguration(MATCH_VALUE_PROPERTY));

    // Set Default
    EName dcterm = null;
    if (("").equals(configuredExternalCatalog)) {
      configuredExternalCatalog = "dublincore";
      dcterm = new EName(TERMS_NS_URI, configuredDCTerm);
    }

    logger.info("External catalog is " + configuredExternalCatalog);

    // Find Catalog
    Catalog[] catalogs = mediaPackage
            .getCatalogs(new MediaPackageElementFlavor(configuredExternalCatalog, StringUtils.lowerCase(configuredCatalog)));

    logger.info("found " + catalogs.length + " Metdata catalogs.");

    if (catalogs != null && catalogs.length > 0) {
      Boolean foundValue = false;

      // Find DCTerm
      List<DublinCoreValue> values = new LinkedList<>();
      for (Catalog catalog : catalogs) {
        DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);
          for (Map.Entry<EName, List<DublinCoreValue>> entry : dc.getValues().entrySet()) {
            if (dcterm != null && dcterm.equals(entry)) {
              values.addAll(entry.getValue());
            } else if (entry.getKey().getLocalName().equals(configuredDCTerm)) {
              values.addAll(entry.getValue());
            }
          }
      }
      // Match Value
      if (values.isEmpty()) {
        logger.info("no value found");
        // Use default
        if (configuredDefaultValue != null) {
          // check for direct character match or regex match
          foundValue = configuredDefaultValue.equals(configuredMatchValue)
                  || configuredDefaultValue.matches(configuredMatchValue);
        }
      } else {
        for (DublinCoreValue value : values) {
          foundValue = value.getValue().equals(configuredMatchValue)
                  || value.getValue().matches(configuredMatchValue);
          logger.info("found value " + value.getValue() + ", regex matched " + value.getValue().matches(configuredMatchValue));
          if (foundValue) break;
        }
      }

      if (foundValue) {
        Map<String, String> properties = new HashMap<>();

        for (String key : currentOperation.getConfigurationKeys()) {
          // Ignore this operations configuration
          if (DCCATALOG_PROPERTY.equals(key) || DCTERM_PROPERTY.equals(key) || DEFAULT_VALUE_PROPERTY.equals(key)
                  || MATCH_VALUE_PROPERTY.equals(key) || EXTERNAL_CATALOG_PROPERTY.equals(key)) {
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
