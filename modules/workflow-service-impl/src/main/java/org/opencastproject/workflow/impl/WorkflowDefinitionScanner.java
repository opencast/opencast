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

package org.opencastproject.workflow.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.ReadinessIndicator;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowIdentifier;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowStateMapping;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads, unloads, and reloads {@link WorkflowDefinition}s from "*workflow.xml" files in any of fileinstall's watch
 * directories.
 */
@Component(
  property = {
    "service.description=Workflow Definition Scanner"
  },
  immediate = true,
  service = { ArtifactInstaller.class, WorkflowDefinitionScanner.class }
)
public class WorkflowDefinitionScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionScanner.class);

  /** An internal collection of workflows that we have installed */
  protected Map<WorkflowIdentifier, WorkflowDefinition> installedWorkflows = new HashMap<>();

  /** All workflow state mappings which are configured for the workflow defintions */
  protected Map<String, Set<WorkflowStateMapping>> workflowStateMappings = new HashMap<>();

  /** An internal collection of artifact id, bind the workflow definition files and their id */
  protected Map<File, WorkflowIdentifier> artifactIds = new HashMap<>();

  /** List of artifact parsed with error */
  protected List<File> artifactsWithError = new ArrayList<>();

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Tag to define if the the workflows definition have already been loaded */
  private boolean isWFSinitialized = false;

  /** The current workflow definition being installed */
  private WorkflowDefinition currentWFD = null;

  private OrganizationDirectoryService organizationDirectoryService;

  @Reference(name = "index")
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * OSGi callback on component activation. private boolean initialized = true;
   *
   * /** OSGi callback on component activation.
   *
   * @param ctx
   *          the bundle context
   */
  @Activate
  void activate(BundleContext ctx) {
    this.bundleCtx = ctx;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) {
    WorkflowDefinition def = currentWFD;

    // If the current workflow definition is null, it means this is a first install and not an update...
    if (def == null) {
      // ... so we have to load the definition first
      def = parseWorkflowDefinitionFile(artifact);

      if (def == null) {
        logger.warn("Unable to install workflow from '{}'", artifact.getName());
        artifactsWithError.add(artifact);
        return;
      }
    }

    // Is there a workflow with the exact same ID, but a different file name? Then ignore.
    final WorkflowIdentifier workflowIdentifier = new WorkflowIdentifier(def.getId(), def.getOrganization());
    for (Map.Entry<File, WorkflowIdentifier> fileWithIdentifier : artifactIds.entrySet()) {
      if (fileWithIdentifier.getValue().equals(workflowIdentifier) && !fileWithIdentifier.getKey().equals(artifact)) {
        logger.warn("Workflow with identifier '{}' already registered in file '{}', ignoring", workflowIdentifier,
                fileWithIdentifier.getKey());
        artifactsWithError.add(artifact);
        return;
      }
    }

    logger.debug("Installing workflow from file '{}'", artifact.getName());
    artifactsWithError.remove(artifact);
    artifactIds.put(artifact, workflowIdentifier);
    putWorkflowDefinition(workflowIdentifier, def);

    // Determine the number of available profiles
    String[] filesInDirectory = artifact.getParentFile().list((arg0, name) -> name.endsWith(".xml"));
    if (filesInDirectory == null) {
      throw new RuntimeException("error retrieving files from directory \"" + artifact.getParentFile() + "\"");
    }

    logger.info("Workflow definition '{}' from file '{}' installed", workflowIdentifier, artifact.getName());

    // Once all profiles have been loaded, announce readiness
    if ((filesInDirectory.length - artifactsWithError.size()) == artifactIds.size() && !isWFSinitialized) {
      logger.info("{} Workflow definitions loaded, activating Workflow service", filesInDirectory.length - artifactsWithError.size());
      Dictionary<String, String> properties = new Hashtable<>();
      properties.put(ARTIFACT, "workflowdefinition");
      logger.debug("Indicating readiness of workflow definitions");
      bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
      isWFSinitialized = true;
    }
    workflowStateMappings.put(def.getId(), def.getStateMappings());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) {
    // Since the artifact is gone, we can't open it to read its ID. So we look in the local map.
    WorkflowIdentifier identifier = artifactIds.remove(artifact);
    if (identifier != null) {
      removeWorkflowDefinition(identifier);
      logger.info("Uninstalling workflow definition '{}' from file '{}'", identifier, artifact.getName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  public void update(File artifact) {
    currentWFD = parseWorkflowDefinitionFile(artifact);

    if (currentWFD != null) {
      uninstall(artifact);
      install(artifact);
      currentWFD = null;
    }
  }

  private boolean organizationExists(final String organization) {
    return organizationDirectoryService.getOrganizations().stream().anyMatch(org -> org.getId().equals(organization));
  }

  /**
   * Parse the given workflow definition file and return the related workflow definition
   *
   * @param artifact
   *          The workflow definition file to parse
   * @return the workflow definition if the given contained a valid one, or null if the file can not be parsed.
   */
  public WorkflowDefinition parseWorkflowDefinitionFile(File artifact) {
    try (InputStream stream = new FileInputStream(artifact)) {
      WorkflowDefinition def = WorkflowParser.parseWorkflowDefinition(stream);
      if (def.getOperations().size() == 0)
        logger.warn("Workflow '{}' has no operations", def.getId());
      if (def.getOrganization() != null && !organizationExists(def.getOrganization())) {
        throw new RuntimeException("invalid organization '" + def.getOrganization() + "'");
      }
      return def;
    } catch (Exception e) {
      logger.warn("Unable to parse workflow from file '{}', {}", artifact.getName(), e.getMessage());
      return null;
    }
  }

  private boolean userCanAccessWorkflow(final User user, final WorkflowIdentifier wfi) {
    final WorkflowDefinition wd = installedWorkflows.get(wfi);
    return userCanAccessWorkflowDefinition(user, wd);
  }

  private boolean userCanAccessWorkflowDefinition(final User user, final WorkflowDefinition wd) {
    return wd.getRoles().isEmpty() || user.hasRole(GLOBAL_ADMIN_ROLE) || wd.getRoles().stream()
            .anyMatch(user::hasRole);
  }

  /**
   * Return available workflow definitions
   *
   * This method finds workflows that are either globally defined or have the correct organization/roles
   * set.
   * @param organization The organization to check for (must not be <code>null</code>)
   * @param user The user to check for (must not be <code>null</code>)
   * @return A stream of available organizations
   */
  public Stream<WorkflowDefinition> getAvailableWorkflowDefinitions(final Organization organization, final User user) {
    return installedWorkflows.keySet().stream()
            .filter(wfi -> wfi.getOrganization() == null || wfi.getOrganization().equals(organization.getId()))
            .filter(wfi -> userCanAccessWorkflow(user, wfi))
            .map(WorkflowIdentifier::getId)
            .distinct()
            .map(identifier -> getWorkflowDefinition(user, new WorkflowIdentifier(identifier, organization.getId())));
  }

  /**
   * Return the workflow definition for a given workflow identifier
   *
   * This method tries to get the workflow using the exact identifier and falls back to the global workflow (without
   * the organization) if that fails.
   *
   * @param user The user to check for
   * @param workflowIdentifier The workflow identifier
   * @return Either <code>null</code> if no workflow is found for this identifier, or the workflow definition.
   */
  public WorkflowDefinition getWorkflowDefinition(final User user, final WorkflowIdentifier workflowIdentifier) {
    final WorkflowDefinition result = installedWorkflows.get(workflowIdentifier);
    if (result != null && userCanAccessWorkflowDefinition(user, result)) {
      return result;
    }
    return installedWorkflows.get(new WorkflowIdentifier(workflowIdentifier.getId(), null));
  }

  /**
   * Add the given workflow definition to the installed workflow definition id.
   *
   * @param identifier
   *          the identifier of the workflow definition to add
   * @param wfd
   *          the workflow definition
   */
  public void putWorkflowDefinition(WorkflowIdentifier identifier, WorkflowDefinition wfd) {
    installedWorkflows.put(identifier, wfd);
  }

  /**
   * Remove the workflow definition with the given id from the installed definition list.
   *
   * @param identifier
   *          the workflow definition identifier
   * @return the removed workflow definition
   */
  public WorkflowDefinition removeWorkflowDefinition(WorkflowIdentifier identifier) {
    return installedWorkflows.remove(identifier);
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
