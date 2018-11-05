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
package org.opencastproject.external.userdirectory;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * The external group loader
 */
public class ExternalGroupLoader {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ExternalGroupLoader.class);

  /** The external applications group suffix */
  public static final String EXTERNAL_GROUP_SUFFIX = "_EXTERNAL_APPLICATIONS";

  /** Path to the list of roles */
  public static final String ROLES_PATH_PREFIX = "/roles";

  /** The path to the external applications list of roles */
  public static final String EXTERNAL_APPLICATIONS_ROLES_FILE = "external-applications";

  /** The group role provider */
  protected JpaGroupRoleProvider groupRoleProvider;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The security service to use to run as the context for adding the groups */
  protected SecurityService securityService;

  /**
   * @param groupRoleProvider
   *          the groupRoleProvider to set
   */
  public void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.debug("Activate external group loader");

    createDefaultGroups(cc);
  }

  /**
   * Creates initial groups for external applications per organization
   *
   * @throws IllegalStateException
   *           if a specified role or user list is unavailable
   */
  private void createDefaultGroups(ComponentContext cc) {
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), () -> {
        try {
          Organization testOrg = organizationDirectoryService.getOrganization(organization.getId());
          if (!(testOrg instanceof JpaOrganization)) {
            logger.info("Note: Ignoring organization with id " + testOrg.getId() + " because it is not a JpaOrganization");
            return;
          }
          JpaOrganization org = (JpaOrganization) testOrg;

          // External Applications
          String externalApplicationsGroupId = org.getId().toUpperCase().concat(EXTERNAL_GROUP_SUFFIX);
          JpaGroup externalApplicationGroup = (JpaGroup) groupRoleProvider.loadGroup(externalApplicationsGroupId,
                  org.getId());
          if (externalApplicationGroup == null) {
            String externalApplicationsGroupname = org.getName().concat(" External Applications");
            String externalApplicationsGroupDescription = "External application users of '" + org.getName() + "'";
            Set<JpaRole> roles = new HashSet<JpaRole>();
            for (String role : loadRoles(EXTERNAL_APPLICATIONS_ROLES_FILE)) {
              roles.add(new JpaRole(role, org));
            }
            roles.add(new JpaRole(SecurityConstants.GLOBAL_SUDO_ROLE, org));
            externalApplicationGroup = new JpaGroup(externalApplicationsGroupId, org, externalApplicationsGroupname,
                    externalApplicationsGroupDescription, roles, new HashSet<String>());
            groupRoleProvider.addGroup(externalApplicationGroup);
          }
        } catch (NotFoundException | IllegalStateException | IOException | UnauthorizedException e) {
          logger.error("Unable to load external API groups", e);
        }
      });
    }
  }

  /**
   * Loads the set of roles from the properties file.
   *
   * @param roleFileName
   *          name of the properties file containing the roles
   * @return the set of roles
   */
  private Set<String> loadRoles(String roleFileName) throws IllegalStateException, IOException {
    String propertiesFile = UrlSupport.concat(ROLES_PATH_PREFIX, roleFileName);

      // Load the properties
    try (InputStream rolesIS = getClass().getResourceAsStream(propertiesFile)) {
      return new TreeSet<>(IOUtils.readLines(rolesIS, UTF_8));
    } catch (IOException e) {
      logger.error("Error loading roles from file {}", propertiesFile);
      throw e;
    }

  }

}
