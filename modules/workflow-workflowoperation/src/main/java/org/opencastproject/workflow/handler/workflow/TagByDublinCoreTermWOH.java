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
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Take look in specified catalog for specified term, if the value matches the specified value add the target-tags
 */
public class TagByDublinCoreTermWOH extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory.getLogger(TagByDublinCoreTermWOH.class);
  private static final String PLUS = "+";
  private static final String MINUS = "-";

  /** Name of the configuration option that provides the source flavors we are looking for */
  public static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";

  /** Name of the configuration option that provides the source tags we are looking for */
  public static final String SOURCE_TAGS_PROPERTY = "source-tags";

  /** Name of the configuration option that provides the target flavors we are looking for */
  public static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Name of the configuration option that provides the target tags we are looking for */
  public static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** Name of the configuration option that provides the catalog to examine */
  public static final String DCCATALOG_PROPERTY = "dccatalog";

  /** Name of the configuration option that provides Dublin Core term/element */
  public static final String DCTERM_PROPERTY = "dcterm";

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

    String configuredSourceFlavors = StringUtils
            .trimToEmpty(currentOperation.getConfiguration(SOURCE_FLAVORS_PROPERTY));
    String configuredSourceTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(SOURCE_TAGS_PROPERTY));
    String configuredCatalog = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCCATALOG_PROPERTY));
    String configuredDCTerm = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCTERM_PROPERTY));
    String configuredDefaultValue = StringUtils.trimToNull(currentOperation.getConfiguration(DEFAULT_VALUE_PROPERTY));
    String configuredMatchValue = StringUtils.trimToEmpty(currentOperation.getConfiguration(MATCH_VALUE_PROPERTY));
    String configuredTargetFlavor = StringUtils.trimToNull(currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY));
    String configuredTargetTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TAGS_PROPERTY));
    boolean copy = BooleanUtils.toBoolean(currentOperation.getConfiguration(COPY_PROPERTY));

    String[] sourceTags = StringUtils.split(configuredSourceTags, ",");
    String[] targetTags = StringUtils.split(configuredTargetTags, ",");
    String[] sourceFlavors = StringUtils.split(configuredSourceFlavors, ",");

    SimpleElementSelector elementSelector = new SimpleElementSelector();
    for (String flavor : sourceFlavors) {
      elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceTags) {
      elementSelector.addTag(tag);
    }

    List<String> removeTags = new ArrayList<>();
    List<String> addTags = new ArrayList<>();
    List<String> overrideTags = new ArrayList<>();

    for (String tag : targetTags) {
      if (tag.startsWith(MINUS)) {
        removeTags.add(tag);
      } else if (tag.startsWith(PLUS)) {
        addTags.add(tag);
      } else {
        overrideTags.add(tag);
      }
    }

    // Find Catalog
    Catalog[] catalogs = mediaPackage.getCatalogs(new MediaPackageElementFlavor("dublincore", StringUtils.lowerCase(configuredCatalog)));

    if (catalogs != null && catalogs.length > 0) {
      Boolean foundValue = false;
      EName dcterm = new EName(TERMS_NS_URI, configuredDCTerm);

      // Find DCTerm
      for (Catalog catalog : catalogs) {
        DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);
        // Match Value
        List<DublinCoreValue> values = dc.get(dcterm);
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
        if (copy) {
          logger.info("Retagging mediapackage elements as a copy");
        } else {
          logger.info("Retagging mediapackage elements");
        }

        Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, false);
        for (MediaPackageElement e : elements) {
          MediaPackageElement element = e;

          if (copy) {
            element = (MediaPackageElement) e.clone();
            element.setIdentifier(null);
            element.setURI(e.getURI()); // use the same URI as the original
          }
          if (configuredTargetFlavor != null) {
            element.setFlavor(MediaPackageElementFlavor.parseFlavor(configuredTargetFlavor));
          }

          if (overrideTags.size() > 0) {
            element.clearTags();
            for (String tag : overrideTags) {
              element.addTag(tag);
            }
          } else {
            for (String tag : removeTags) {
              element.removeTag(tag.substring(MINUS.length()));
            }
            for (String tag : addTags) {
              element.addTag(tag.substring(PLUS.length()));
            }
          }

          if (copy) {
            mediaPackage.addDerived(element, e);
          }
        }
      } // if foundValue
    } // if catalogs

    return createResult(mediaPackage, Action.CONTINUE);
  }
}
