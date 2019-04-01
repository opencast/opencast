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
import static org.opencastproject.workflow.handler.workflow.ImportWorkflowPropertiesWOH.loadPropertiesElementFromMediaPackage;
import static org.opencastproject.workflow.handler.workflow.ImportWorkflowPropertiesWOH.loadPropertiesFromXml;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * Workflow operation handler for exporting workflow properties.
 */
public class ExportWorkflowPropertiesWOH extends AbstractWorkflowOperationHandler {

  /** Configuration options */
  public static final String KEYS_PROPERTY = "keys";
  public static final String TARGET_FLAVOR_PROPERTY = "target-flavor";
  public static final String TARGET_TAGS_PROPERTY = "target-tags";

  public static final String DEFAULT_TARGET_FLAVOR = MediaPackageElements.PROCESSING_PROPERTIES.toString();
  public static final String EXPORTED_PROPERTIES_FILENAME = "processing-properties.xml";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ExportWorkflowPropertiesWOH.class);

  /** The workspace */
  private Workspace workspace;

  /** OSGi DI */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.info("Start exporting workflow properties for workflow {}", workflowInstance);
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final Set<String> keys = $(getOptConfig(workflowInstance, KEYS_PROPERTY)).bind(Strings.splitCsv).toSet();
    final String targetFlavorString = getOptConfig(workflowInstance, TARGET_FLAVOR_PROPERTY).getOr(DEFAULT_TARGET_FLAVOR);
    final Stream<String> targetTags = $(getOptConfig(workflowInstance, TARGET_TAGS_PROPERTY)).bind(Strings.splitCsv);
    final MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorString);

    // Read optional existing workflow properties from mediapackage
    Properties workflowProps = new Properties();
    Opt<Attachment> existingPropsElem = loadPropertiesElementFromMediaPackage(targetFlavor, workflowInstance);
    if (existingPropsElem.isSome()) {
      workflowProps = loadPropertiesFromXml(workspace, existingPropsElem.get().getURI());

      // Remove specified keys
      for (String key : keys)
        workflowProps.remove(key);
    }

    // Extend with specified properties
    for (String key : workflowInstance.getConfigurationKeys()) {
      if (keys.isEmpty() || keys.contains(key))
        workflowProps.put(key, workflowInstance.getConfiguration(key));
    }

    // Store properties as an attachment
    Attachment attachment;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      workflowProps.storeToXML(out, null, "UTF-8");
      String elementId = UUID.randomUUID().toString();
      URI uri = workspace.put(mediaPackage.getIdentifier().compact(), elementId, EXPORTED_PROPERTIES_FILENAME,
              new ByteArrayInputStream(out.toByteArray()));
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      attachment = (Attachment) builder.elementFromURI(uri, Attachment.TYPE, targetFlavor);
      attachment.setMimeType(MimeTypes.XML);
    } catch (IOException e) {
      logger.error("Unable to store workflow properties as Attachment with flavor '{}': {}", targetFlavorString,
              ExceptionUtils.getStackTrace(e));
      throw new WorkflowOperationException("Unable to store workflow properties as Attachment", e);
    }

    // Add the target tags
    for (String tag : targetTags) {
      logger.trace("Tagging with '{}'", tag);
      attachment.addTag(tag);
    }

    // Update attachment
    if (existingPropsElem.isSome())
      mediaPackage.remove(existingPropsElem.get());
    mediaPackage.add(attachment);

    logger.info("Added properties from {} as Attachment with flavor {}", workflowInstance, targetFlavorString);

    logger.debug("Workflow properties: {}", propertiesAsString(workflowProps));

    return createResult(mediaPackage, null, Action.CONTINUE, 0);
  }

  /** Serialize the properties into a string. */
  private String propertiesAsString(Properties properties) {
    StringWriter writer = new StringWriter();
    properties.list(new PrintWriter(writer));
    return writer.getBuffer().toString();
  }

}
