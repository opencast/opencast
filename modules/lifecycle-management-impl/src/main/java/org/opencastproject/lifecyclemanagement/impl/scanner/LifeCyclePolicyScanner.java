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
package org.opencastproject.lifecyclemanagement.impl.scanner;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyAccessControlEntryImpl;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.ReadinessIndicator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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

/**
 * Loads, unloads, and reloads {@link LifeCyclePolicy}s from "*policy.yml" files in any of fileinstall's watch
 * directories.
 */
@Component(
    property = {
        "service.description=LifeCycle Policy Scanner"
    },
    immediate = true,
    service = { ArtifactInstaller.class, LifeCyclePolicyScanner.class }
)
public class LifeCyclePolicyScanner implements ArtifactInstaller {

  private static final Logger logger = LoggerFactory.getLogger(LifeCyclePolicyScanner.class);

  private ObjectMapper mapper;

  /** An internal collection of artifact id, bind the policy definition files and their id */
  protected Map<File, String> artifactIds = new HashMap<>();

  /** List of artifact parsed with error */
  protected final List<File> artifactsWithError = new ArrayList<>();

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Tag to define if the policies definition has already been loaded */
  private boolean isLPSinitialized = false;

  private LifeCycleService lifeCycleService;
  protected SecurityService securityService;

  private PolicyFileNameFilter policyFilenameFilter;

  private User systemAdminUser;
  private Organization defaultOrganization;

  @Reference
  protected void addLifeCycleService(LifeCycleService service) {
    lifeCycleService = service;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  void activate(BundleContext ctx) {
    this.bundleCtx = ctx;
    this.defaultOrganization = new DefaultOrganization();
    String systemAdminUserName = ctx.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    this.systemAdminUser = SecurityUtil.createSystemUser(systemAdminUserName, defaultOrganization);

    this.policyFilenameFilter = new PolicyFileNameFilter("lifecyclepolicies", ".*\\.(yaml|yml)$");

    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    SimpleModule sm = new SimpleModule();
    sm.addAbstractTypeMapping(LifeCyclePolicyAccessControlEntry.class, LifeCyclePolicyAccessControlEntryImpl.class);
    om.registerModule(sm);
    mapper = om;

    lifeCycleService.deleteAllLifeCyclePoliciesCreatedByConfig(defaultOrganization.getId());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) {
    // TODO: Changing policies (by changing the file or restarting the service), causes the policies to be
    //   reinstalled as new policies, meaning they will activate again even if they already activated before.
    //   They also create new tasks, even for events that already had tasks created.
    //   Fix this.
    synchronized (artifactsWithError) {
      LifeCyclePolicyImpl policy = parseLifeCyclePolicyFile(artifact);
      if (policy == null) {
        logger.warn("Unable to install policy from '{}'", artifact.getName());
        artifactsWithError.add(artifact);
      } else {
        installLifeCyclePolicy(artifact, policy);
      }

      // Determine the number of available profiles
      String[] filesInDirectory = artifact.getParentFile().list(policyFilenameFilter);
      if (filesInDirectory == null) {
        throw new RuntimeException("error retrieving files from directory \"" + artifact.getParentFile() + "\"");
      }

      // Once all profiles have been loaded, announce readiness
      if ((filesInDirectory.length - artifactsWithError.size()) == artifactIds.size() && !isLPSinitialized) {
        logger.info("{} LifeCycle policies loaded", filesInDirectory.length - artifactsWithError.size());
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(ARTIFACT, "lifecyclepolicy");
        logger.debug("Indicating readiness of lifecycle policies");
        bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
        isLPSinitialized = true;
      }
    }
  }

  private void installLifeCyclePolicy(File artifact, LifeCyclePolicyImpl policy) {
    synchronized (artifactsWithError) {
      // Is there a policy with the exact same ID, but a different file name? Then ignore.
      final String policyIdentifier = policy.getId();
      for (Map.Entry<File, String> fileWithIdentifier : artifactIds.entrySet()) {
        if (fileWithIdentifier.getValue().equals(policyIdentifier) && !fileWithIdentifier.getKey().equals(artifact)) {
          logger.warn("Policy with identifier '{}' already registered in file '{}', ignoring", policyIdentifier,
              fileWithIdentifier.getKey());
          artifactsWithError.add(artifact);
          return;
        }
      }

      logger.debug("Installing policy from file '{}'", artifact.getName());
      artifactsWithError.remove(artifact);
      artifactIds.put(artifact, policyIdentifier);
      putLifeCyclePolicy(policy);

      logger.info("LifeCycle policy '{}' from file '{}' installed", policyIdentifier, artifact.getName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) {
    // Since the artifact is gone, we can't open it to read its ID. So we look in the local map.
    String identifier = artifactIds.remove(artifact);
    if (identifier != null) {
      removeLifeCyclePolicy(identifier);
      logger.info("Uninstalling lifecycle policy '{}' from file '{}'", identifier, artifact.getName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  public void update(File artifact) {
    LifeCyclePolicyImpl policy = parseLifeCyclePolicyFile(artifact);

    if (policy != null) {
      uninstall(artifact);
      installLifeCyclePolicy(artifact, policy);
    }
  }

  /**
   * Parse the given lifecycle policy file and return the related lifecycle policy
   *
   * @param artifact
   *          The lifecycle policy file to parse
   * @return the lifecycle policy if the given contained a valid one, or null if the file can not be parsed.
   */
  public LifeCyclePolicyImpl parseLifeCyclePolicyFile(File artifact) {
    try (InputStream stream = new FileInputStream(artifact)) {
      LifeCyclePolicyImpl policy;

      policy = mapper.readValue(stream, LifeCyclePolicyImpl.class);
      policy.setCreatedFromConfig(true);

      if (!lifeCycleService.checkValidity(policy)) {
        throw new RuntimeException("Invalid policy " + policy.getId() + "");
      }

      return policy;
    } catch (Exception e) {
      logger.warn("Unable to parse policy from file '{}', {}", artifact.getName(), e.getMessage());
      return null;
    }
  }

  /**
   * Add the given lifecycle policy to the installed lifecycle policy id.
   *
   * @param policy
   *          the lifecycle policy
   */
  public void putLifeCyclePolicy(LifeCyclePolicyImpl policy) {
    SecurityUtil.runAs(securityService, defaultOrganization, systemAdminUser, () -> {
      try {
        lifeCycleService.getLifeCyclePolicyById(policy.getId(), policy.getOrganization());
        lifeCycleService.updateLifeCyclePolicy(policy);
      } catch (NotFoundException e) {
        try {
          lifeCycleService.createLifeCyclePolicy(policy);
        } catch (UnauthorizedException ex) {
          logger.error("System admin user not authorized to add lifecycle policy to database. By all accounts "
              + "this should not have happened");
          throw new RuntimeException(e);
        }
      } catch (UnauthorizedException e) {
        logger.error("System admin user not authorized to add lifecycle policy to database. By all accounts "
            + "this should not have happened");
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Remove the lifecycle policy with the given id from the installed definition list.
   *
   * @param identifier
   *          the lifecycle policy identifier
   * @return the removed lifecycle policy
   */
  public void removeLifeCyclePolicy(String identifier) {
    SecurityUtil.runAs(securityService, defaultOrganization, systemAdminUser, () -> {
      try {
        lifeCycleService.deleteLifeCyclePolicy(identifier);
      } catch (NotFoundException e) {
        logger.warn("Attempted to delete life cycle policy with id " + identifier + " from datbaase, but policy "
            + "was not found.");
      } catch (UnauthorizedException e) {
        logger.error("System admin user not authorized to add lifecycle policy to database. By all accounts "
            + "this should not have happened");
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  public boolean canHandle(File artifact) {
    return policyFilenameFilter.accept(artifact.getParentFile(), artifact.getName());
  }
}
