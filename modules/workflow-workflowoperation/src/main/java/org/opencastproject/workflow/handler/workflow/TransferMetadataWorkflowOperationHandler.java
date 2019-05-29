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
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The workflow definition for handling "transfer-metadata" operations
 */
public class TransferMetadataWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static Logger logger = LoggerFactory.getLogger(TransferMetadataWorkflowOperationHandler.class);

  /** Reference to the workspace */
  private Workspace workspace = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running transfer-metadata workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    Configuration configuration = new Configuration(workflowInstance.getCurrentOperation());

    // select source metadata catalog
    Metadata sourceMetadata;
    try {
      sourceMetadata = new Metadata(mediaPackage, configuration.sourceFlavor);
    } catch (Metadata.NoMetadataFoundException e) {
      logger.debug("No catalog with flavor {}. Skipping operation.", configuration.sourceFlavor);
      return createResult(mediaPackage, Action.SKIP);
    }
    Metadata targetMetadata;
    try {
      targetMetadata = new Metadata(mediaPackage, configuration.targetFlavor);
    } catch (Metadata.NoMetadataFoundException e) {
      throw new WorkflowOperationException(e);
    }

    // fail if the target exists and we did not configure to override it
    if (targetMetadata.dcCatalog.get(configuration.targetElement).size() > 0 && !configuration.force) {
      throw new WorkflowOperationException("The target metadata field already exists and forcing was not configured");
    }

    // either transfer all values or generate one field by joining all values.
    // all language information will get lost if we join the values.
    if (configuration.concatDelimiter == null) {
      final List<DublinCoreValue> values = sourceMetadata.dcCatalog.get(configuration.sourceElement);
      targetMetadata.dcCatalog.set(configuration.targetElement, values);
      logger.info("Transferred {} metadata elements", values.size());
    } else {
      final String value = sourceMetadata.dcCatalog.get(configuration.sourceElement)
              .stream()
              .map(DublinCoreValue::getValue)
              .collect(Collectors.joining(configuration.concatDelimiter));
      targetMetadata.dcCatalog.set(configuration.targetElement, value);
      logger.info("Transferred concatenated metadata element(s)");
    }

    // set prefix used for the target element's namespace
    if (configuration.targetPrefix != null) {
      final XmlNamespaceContext namespace = XmlNamespaceContext.mk(configuration.targetPrefix,
              configuration.targetElement.getNamespaceURI());
      targetMetadata.dcCatalog.addBindings(namespace);
    }

    try {
      targetMetadata.save();
    } catch (IOException e) {
      throw new WorkflowOperationException("Error saving updated metadata catalog", e);
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }

  private class Metadata {
    private final MediaPackage mediaPackage;
    private final Catalog catalog;
    private final DublinCoreCatalog dcCatalog;

    /**
     * Initialize Metadata object by selecting a specified metadata catalog from a given media package.
     *
     * @param mediaPackage
     *          Media package to work on
     * @param flavor
     *          Flavor specifying the catalog to select
     * @throws NoMetadataFoundException
     *          Could not find catalog with given flavor
     */
    Metadata(final MediaPackage mediaPackage, final MediaPackageElementFlavor flavor) throws NoMetadataFoundException {
      this.mediaPackage = mediaPackage;

      Catalog[] catalogs = mediaPackage.getCatalogs(flavor);
      if (catalogs.length < 1) {
        throw new NoMetadataFoundException();
      }

      if (catalogs.length > 1) {
        logger.warn("More than one metadata catalog of flavor {} found; using the first one", flavor);
      }
      catalog = catalogs[0];
      dcCatalog = DublinCoreUtil.loadDublinCore(workspace, catalog);
    }

    /**
     * Save the modified metadata and update the media package to refer to the new catalog.
     *
     * @throws IOException
     *          Error storing the metadata catalog
     */
    private void save() throws IOException {
      String filename = FilenameUtils.getName(catalog.getURI().toString());
      InputStream stream = IOUtils.toInputStream(dcCatalog.toXmlString(), StandardCharsets.UTF_8);
      catalog.setURI(workspace.put(mediaPackage.getIdentifier().toString(), catalog.getIdentifier(), filename, stream));
      catalog.setChecksum(null);
    }

    /**
     * Exception to throw if no catalog with a specified flavor exists in the media package.
     */
    class NoMetadataFoundException extends Exception {
    }
  }

  /**
   * Storage for this operation's configuration
   */
  private class Configuration {
    private final MediaPackageElementFlavor sourceFlavor;
    private final MediaPackageElementFlavor targetFlavor;
    private final EName sourceElement;
    private final EName targetElement;
    private final boolean force;
    private final String concatDelimiter;
    private final String targetPrefix;

    /**
     * Load configuration from given operation.
     *
     * @param operation
     *          Operation to load configuration from
     * @throws IllegalArgumentException
     *          Invalid configuration
     */
    Configuration(WorkflowOperationInstance operation)
            throws IllegalArgumentException {
      // required
      // will throw IllegalArgumentException if not defined
      sourceFlavor = MediaPackageElementFlavor.parseFlavor(operation.getConfiguration("source-flavor"));
      targetFlavor = MediaPackageElementFlavor.parseFlavor(operation.getConfiguration("target-flavor"));
      sourceElement = EName.fromString(operation.getConfiguration("source-element"));
      targetElement = EName.fromString(operation.getConfiguration("target-element"));

      // optional arguments
      force = BooleanUtils.toBoolean(operation.getConfiguration("force"));
      concatDelimiter = operation.getConfiguration("concat");
      targetPrefix = operation.getConfiguration("target-prefix");
    }
  }

  /** OSGi callback to inject the workspace */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
