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
package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.XACMLUtils;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

public class AclScanner implements ArtifactInstaller {

  /** The directory name that has the XACML file defining the Acl **/
  public static final String ACL_DIRECTORY = "acl";

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(AclScanner.class);

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The Access Control service */
  private AclServiceFactory aclServiceFactory;

  /**
   * A map linking the Acl file name concatenate with the organization id {@code filename_organizationId} to the related
   * managed Acl Id
   */
  private Map<String, Long> managedAcls = new HashMap<String, Long>();

  /**
   * OSGI service activation method
   */
  void activate(BundleContext ctx) {
    logger.info("Activated Acl scanner");
  }

  /**
   * OSGI deactivation method
   */
  void deactivate(BundleContext ctx) {
    logger.info("Deactivated Acl scanner");
  }

  /** OSGi DI. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi callback for setting persistence. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return ACL_DIRECTORY.equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".xml");
  }

  /**
   * Add an ACL based upon an XACML file to all the organizations.
   *
   * @param artifact
   *          The File representing the XACML File.
   * @throws IOException
   * @throws JAXBException
   */
  private void addAcl(File artifact) throws IOException, JAXBException {
    List<Organization> organizations = organizationDirectoryService.getOrganizations();

    logger.debug("Adding Acl {}", artifact.getAbsolutePath());

    String fileName = FilenameUtils.removeExtension(artifact.getName());
    AccessControlList acl = parseToAcl(artifact);
    Option<ManagedAcl> managedAcl = Option.<ManagedAcl> none();

    // Add the Acl to all the organizations
    for (Organization org : organizations) {

      // If there are already (not-default) Acl defined for this organization, we skip this one.
      for (ManagedAcl a : getAclService(org).getAcls()) {
        if (managedAcls.get(generateAclId(a.getName(), org)) == null) {
          logger.debug(
                  "The Acl {} will be not added to the organisation {} as it already contains other not-default Acls.",
                  fileName, org.getName());
          continue;
        }
      }

      managedAcl = getAclService(org).createAcl(acl, fileName);
      if (managedAcl.isSome()) {
        managedAcls.put(generateAclId(fileName, org), managedAcl.get().getId());
        logger.debug("Acl from XACML file {} has been added for the organisation {}", fileName, org.getName());
      } else {
        logger.info("The Acl from the file {} has already been added to the organisation {}.", fileName, org.getName());
      }
    }
  }

  /**
   * Update an ACL based upon an XACML file on all the organizations.
   *
   * @param artifact
   *          The File representing the XACML File.
   * @throws IOException
   * @throws JAXBException
   */
  private void updateAcl(File artifact) throws IOException, JAXBException {
    List<Organization> organizations = organizationDirectoryService.getOrganizations();

    logger.debug("Updating Acl {}", artifact.getAbsolutePath());

    String fileName = FilenameUtils.removeExtension(artifact.getName());
    AccessControlList acl = parseToAcl(artifact);

    // Update the Acl on all the organizations
    for (Organization org : organizations) {
      Long id = managedAcls.get(generateAclId(fileName, org));
      if (id != null) {
        // If the Acl Id is in the managedAcls map, we update the Acl
        if (!getAclService(org).updateAcl(new ManagedAclImpl(id, fileName, org.getId(), acl))) {
          logger.warn("No Acl found with the id {} for the organisation {}.", id, org.getName());
        } else {
          logger.debug("Acl from XACML file {} has been updated for the organisation {}", fileName, org.getName());
        }
      } else {
        logger.info("The XACML file {} has not been added to the organisation {} and will therefore not be updated",
                fileName, org.getName());
      }
    }
  }

  /**
   * Remove an ACL based upon an XACML file from all the organizations.
   *
   * @param artifact
   *          The File representing the XACML File.
   * @throws IOException
   * @throws JAXBException
   */
  private void removeAcl(File artifact) throws IOException, JAXBException {
    List<Organization> organizations = organizationDirectoryService.getOrganizations();

    logger.debug("Removing Acl {}", artifact.getAbsolutePath());

    String fileName = FilenameUtils.removeExtension(artifact.getName());

    // Remove the Acl on all the organizations
    for (Organization org : organizations) {
      Long id = managedAcls.get(generateAclId(fileName, org));
      if (id != null) {
        try {
          getAclService(org).deleteAcl(id);
        } catch (NotFoundException e) {
          logger.warn("Unable to delete managec acl {}: Managed acl already deleted!", id);
        } catch (AclServiceException e) {
          logger.error("Unable to delete managed acl {}: {}", id, ExceptionUtils.getStackTrace(e));
        }
      } else {
        logger.debug("No Acl found with the id {}.", id);
      }
    }
  }

  /**
   * Generate an Acl Id with the XACML filename and the organization id
   *
   * @param fileName
   *          the XACML file
   * @param org
   *          the organization
   * @return the generated Acl Id
   */
  private String generateAclId(String fileName, Organization org) {
    return fileName + "_" + org.getId();
  }

  /**
   * Parse the given XACML file into an Access Control List
   *
   * @param artifact
   * @return
   * @throws FileNotFoundException
   * @throws JAXBException
   */
  private AccessControlList parseToAcl(File artifact) throws FileNotFoundException, JAXBException {
    FileInputStream in = null;
    AccessControlList acl = null;

    try {
      in = new FileInputStream(artifact);
      acl = XACMLUtils.parseXacml(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    return acl;
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
