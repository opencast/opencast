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

package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component(
    immediate = true,
    service = { ArtifactInstaller.class, AclScanner.class },
    property = {
        "service.description=Acl Scanner"
    }
)
public class AclScanner implements ArtifactInstaller {

  /** The directory name that holds the ACL files **/
  public static final String ACL_DIRECTORY = "acl";

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(AclScanner.class);

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The Access Control service */
  private AclServiceFactory aclServiceFactory;

  private SecurityService securityService;

  /**
   * Should we update override ACLs in DB on template changes?
   * All user defined changes will be lost.
   */
  private boolean updateExisting = true;

  /**
   * OSGI service activation method
   */
  @Activate
  void activate(BundleContext ctx) {
    logger.info("Activated Acl scanner");
  }

  /**
   * OSGI deactivation method
   */
  @Deactivate
  void deactivate(BundleContext ctx) {
    logger.info("Deactivated Acl scanner");
  }

  /** OSGi DI. */
  @Reference
  void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi callback for setting persistence. */
  @Reference
  void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /** OSGi DI */
  @Reference
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return ACL_DIRECTORY.equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".json");
  }

  /**
   * Add ACL from configuration directory to all organizations.
   *
   * @param artifact
   *          The File representing the ACL.
   * @throws IOException
   * @throws AccessControlParsingException
   */
  private void addAcl(File artifact) throws IOException, AccessControlParsingException {
    List<Organization> organizations = organizationDirectoryService.getOrganizations();

    logger.debug("Adding Acl {}", artifact.getAbsolutePath());

    String fileName = FilenameUtils.removeExtension(artifact.getName());
    AccessControlList acl = parseToAcl(artifact);

    // Add the Acl to all the organizations
    for (Organization org : organizations) {
      securityService.setOrganization(org);
      AclService aclService = getAclService(org);
      Optional<ManagedAcl> managedAcl = aclService.getAcl(fileName);
      if (managedAcl.isEmpty()) {
        aclService.createAcl(acl, fileName);
        logger.debug("Acl from '{}' has been added for the organisation {}", fileName, org.getName());
      } else {
        logger.debug("Acl from '{}' has already been added to the organisation {}. Lets update it.",
            fileName, org.getName());
        aclService.updateAcl(new ManagedAclImpl(managedAcl.get().getId(), fileName, org.getId(), acl));
        logger.debug("Acl from file {} has been updated for the organisation {}", fileName, org.getName());
      }
    }
  }

  /**
   * Update ACL from configuration directory in all organizations.
   *
   * @param artifact
   *          The File representing the ACL.
   * @throws IOException
   * @throws AccessControlParser
   */
  private void updateAcl(File artifact) throws IOException, AccessControlParsingException {
    List<Organization> organizations = organizationDirectoryService.getOrganizations();

    logger.debug("Updating Acl {}", artifact.getAbsolutePath());

    String fileName = FilenameUtils.removeExtension(artifact.getName());
    AccessControlList acl = parseToAcl(artifact);

    // Update the Acl on all the organizations
    for (Organization org : organizations) {
      securityService.setOrganization(org);
      AclService aclService = getAclService(org);
      Optional<ManagedAcl> managedAcl = aclService.getAcl(fileName);
      if (managedAcl.isPresent()) {
        // If the Acl Id is in the managedAcls map, we update the Acl
        if (!aclService.updateAcl(new ManagedAclImpl(managedAcl.get().getId(), fileName, org.getId(), acl))) {
          logger.warn("No Acl found with the id {} for the organisation {}.", managedAcl.get().getId(), org.getName());
        } else {
          logger.debug("Acl from file {} has been updated for the organisation {}", fileName, org.getName());
        }
      } else {
        managedAcl = aclService.createAcl(acl, fileName);
        logger.debug("Acl from '{}' has been added for the organisation {}", fileName, org.getName());
      }
    }
  }

  /**
   * Remove ACL from configuration directory from all the organizations.
   *
   * @param artifact
   *          The File representing the ACL.
   */
  private void removeAcl(File artifact) {
    List<Organization> organizations = organizationDirectoryService.getOrganizations();

    logger.debug("Removing Acl {}", artifact.getAbsolutePath());

    String fileName = FilenameUtils.removeExtension(artifact.getName());

    // Remove the Acl on all the organizations
    for (Organization org : organizations) {
      securityService.setOrganization(org);
      AclService aclService = getAclService(org);
      Optional<ManagedAcl> managedAcl = aclService.getAcl(fileName);
      if (managedAcl.isPresent()) {
        try {
          getAclService(org).deleteAcl(managedAcl.get().getId());
        } catch (NotFoundException e) {
          logger.debug("Unable to delete managed acl {}. Managed acl already deleted!", managedAcl.get().getId());
        } catch (AclServiceException e) {
          logger.error("Unable to delete managed acl {}", managedAcl.get().getId(), e);
        }
      } else {
        logger.debug("No Acl matching file {} found.", fileName);
      }
    }
  }

  /**
   * Generate an Acl Id from ACL filename and organization id
   *
   * @param fileName
   *          the ACL file
   * @param org
   *          the organization
   * @return the generated Acl Id
   */
  private String generateAclId(String fileName, Organization org) {
    return fileName + "_" + org.getId();
  }

  /**
   * Parse the given ACL JSON file into an Access Control List
   *
   * @param artifact JSON file
   * @return Parsed access control list
   * @throws IOException Error accessing the ACL file
   * @throws AccessControlParsingException Error parsing the ACL files
   */
  private AccessControlList parseToAcl(File artifact)
          throws IOException, AccessControlParsingException {
    try (FileInputStream in = new FileInputStream(artifact)) {
      return AccessControlParser.parseAcl(in);
    }
  }

  private AclService getAclService(Organization organization) {
    return aclServiceFactory.serviceFor(organization);
  }

  @Override
  public void install(File artifact) throws Exception {
    logger.info("Installing Acl {}", artifact.getAbsolutePath());
    addAcl(artifact);
  }

  @Override
  public void update(File artifact) throws Exception {
    logger.info("Updating Acl {}", artifact.getAbsolutePath());
    updateAcl(artifact);
  }

  @Override
  public void uninstall(File artifact) throws Exception {
    logger.info("Removing Acl {}", artifact.getAbsolutePath());
    removeAcl(artifact);
  }
}
