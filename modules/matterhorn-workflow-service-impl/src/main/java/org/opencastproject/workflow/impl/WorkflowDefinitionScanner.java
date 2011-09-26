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

import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads, unloads, and reloads {@link WorkflowDefinition}s from "*workflow.xml" files in any of fileinstall's watch
 * directories.
 */
public class WorkflowDefinitionScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionScanner.class);

  /** An internal collection of workflows that we have installed */
  protected Map<File, WorkflowDefinition> installedWorkflows = new HashMap<File, WorkflowDefinition>();

  protected WorkflowService workflowService;

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
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
      ((WorkflowServiceImpl) workflowService).registerWorkflowDefinition(def);
      installedWorkflows.put(artifact, def);
    } catch (Exception e) {
      logger.warn("Unable to install workflow from {}, {}", artifact, e.getMessage());
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) throws Exception {
    // Since the artifact is gone, we can't open it to read its ID. So we look in the local map.
    WorkflowDefinition def = installedWorkflows.get(artifact);
    logger.info("Uninstalling workflow '{}' from file {}", def.getId(), artifact.getAbsolutePath());
    ((WorkflowServiceImpl) workflowService).unregisterWorkflowDefinition(def.getId());
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
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  public boolean canHandle(File artifact) {
    return "workflows".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".xml");
  }
}
