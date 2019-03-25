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

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.workflow.api.WorkflowOperationResult.Action.CONTINUE;
import static org.opencastproject.workflow.api.WorkflowOperationResult.Action.SKIP;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Workflow operation handler for importing workflow properties.
 */
public class ImportWorkflowPropertiesWOH extends AbstractWorkflowOperationHandler {

  /* Configuration options */
  public static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";
  public static final String KEYS_PROPERTY = "keys";

  private static final Logger logger = LoggerFactory.getLogger(ImportWorkflowPropertiesWOH.class);

  /* Service references */
  private Workspace workspace;

  /** OSGi DI */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance wi, JobContext context) throws WorkflowOperationException {
    logger.info("Start importing workflow properties for workflow {}", wi);
    final String sourceFlavor = getConfig(wi, SOURCE_FLAVOR_PROPERTY);
    Opt<Attachment> propertiesElem = loadPropertiesElementFromMediaPackage(
            MediaPackageElementFlavor.parseFlavor(sourceFlavor), wi);
    if (propertiesElem.isSome()) {
      Properties properties = loadPropertiesFromXml(workspace, propertiesElem.get().getURI());
      final Set<String> keys = $(getOptConfig(wi, KEYS_PROPERTY)).bind(Strings.splitCsv).toSet();
      return createResult(wi.getMediaPackage(), convertToWorkflowProperties(properties, keys), CONTINUE, 0);
    } else {
      logger.info("No attachment with workflow properties found, skipping...");
      return createResult(wi.getMediaPackage(), SKIP);
    }
  }

  static Opt<Attachment> loadPropertiesElementFromMediaPackage(MediaPackageElementFlavor sourceFlavor,
          WorkflowInstance wi) throws WorkflowOperationException {
    final MediaPackage mp = wi.getMediaPackage();
    final Attachment[] elements = mp.getAttachments(sourceFlavor);

    if (elements.length < 1) {
      logger.info("Cannot import workflow properties - no element with flavor '{}' found in media package '{}'",
              sourceFlavor, mp.getIdentifier());
      return Opt.none();
    } else if (elements.length > 1) {
      throw new WorkflowOperationException(format("Found more than one element with flavor '%s' in media package '%s'",
              sourceFlavor, mp.getIdentifier()));
    }

    return Opt.some(elements[0]);
  }

  static Properties loadPropertiesFromXml(Workspace workspace, URI uri) throws WorkflowOperationException {
    final Properties properties = new Properties();
    try {
      File propertiesFile = workspace.get(uri);
      try (InputStream is = new FileInputStream(propertiesFile)) {
        properties.loadFromXML(is);
        logger.debug("Properties loaded from {}", propertiesFile);
      }
    } catch (NotFoundException e) {
      throw new WorkflowOperationException(e);
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }
    return properties;
  }

  private Map<String, String> convertToWorkflowProperties(Properties properties, Set<String> keys) {
    Map<String, String> workflowProperties = new HashMap<String, String>();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      if (keys.isEmpty() || keys.contains(entry.getKey())) {
        workflowProperties.put((String) entry.getKey(), (String) entry.getValue());
      }
    }
    return workflowProperties;
  }
}
