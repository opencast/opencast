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
package org.opencastproject.workflow.impl;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.util.ReadinessIndicator;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowParser;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Loads, unloads, and reloads {@link WorkflowDefinition}s from "*workflow.xml" files in any of fileinstall's watch
 * directories.
 */
public class WorkflowDefinitionScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionScanner.class);

  /** An internal collection of workflows that we have installed */
  protected Map<String, WorkflowDefinition> installedWorkflows = new HashMap<String, WorkflowDefinition>();

  /** An internal collection of artifact id, bind the workflow definition files and their id */
  protected Map<File, String> artifactIds = new HashMap<File, String>();

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Sum of definition files currently installed */
  private int sumInstalledFiles = 0;

  /**
   * OSGi callback on component activation.
   * 
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
    this.bundleCtx = ctx;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) throws Exception {
    logger.info("Installing workflow from file {}", artifact.getAbsolutePath());
    InputStream stream = null;
    try {
      stream = new FileInputStream(artifact);
      WorkflowDefinition def = WorkflowParser.parseWorkflowDefinition(stream);
      if (def.getOperations().size() == 0)
        logger.warn("Workflow '{}' has no operations", def.getId());
      artifactIds.put(artifact, def.getId());
      putWokflowDefinition(def.getId(), def);
      sumInstalledFiles++;
    } catch (Exception e) {
      logger.warn("Unable to install workflow from {}, {}", artifact, e.getMessage());
    } finally {
      IOUtils.closeQuietly(stream);
    }

    // Determine the number of available profiles
    String[] filesInDirectory = artifact.getParentFile().list(new FilenameFilter() {
      public boolean accept(File arg0, String name) {
        return name.endsWith(".xml");
      }
    });

    // Once all profiles have been loaded, announce readiness
    if (filesInDirectory.length == sumInstalledFiles) {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(ARTIFACT, "workflowdefinition");
      logger.debug("Indicating readiness of workflow definitions");
      bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
      logger.info("All {} workflow definitions installed", filesInDirectory.length);
    } else {
      logger.info("{} of {} workflow definitions installed", sumInstalledFiles, filesInDirectory.length);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) throws Exception {
    // Since the artifact is gone, we can't open it to read its ID. So we look in the local map.
    WorkflowDefinition def = removeWofklowDefinition(artifactIds.remove(artifact));
    logger.info("Uninstalling workflow '{}' from file {}", def.getId(), artifact.getAbsolutePath());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }

  /**
   * Gets the workflow definitions with the given id.
   * 
   * @param id
   * @return the workflow definition if exist or null
   */
  public WorkflowDefinition getWorkflowDefinition(String id) {
    return installedWorkflows.get(id);
  }

  /**
   * Get the list of installed workflow definitions.
   * 
   * @return the collection of installed workflow definitions id
   */
  public Map<String, WorkflowDefinition> getWorkflowDefinitions() {
    return installedWorkflows;
  }

  /**
   * Add the given workflow definition to the installed workflow definition id.
   * 
   * @param id
   *          the id of the workflow definition to add
   * @param wfd
   *          the workflow definition id
   */
  public void putWokflowDefinition(String id, WorkflowDefinition wfd) {
    installedWorkflows.put(id, wfd);
  }

  /**
   * Remove the workflow definition with the given id from the installed definition list.
   * 
   * @param id
   *          the workflow definition id
   * @return the removed workflow definition
   */
  public WorkflowDefinition removeWofklowDefinition(String id) {
    return installedWorkflows.remove(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  public boolean canHandle(File artifact) {
    return "workflows".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".xml");
  }
}
