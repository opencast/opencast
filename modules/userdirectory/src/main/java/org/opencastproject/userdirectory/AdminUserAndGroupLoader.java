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
package org.opencastproject.userdirectory;

import static org.opencastproject.userdirectory.InMemoryUserAndRoleProvider.PROVIDER_NAME;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryListener;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Effect0;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * User and group loader to create a system administrator group for each tenant along with a user named after the
 * organization.
 */
public class AdminUserAndGroupLoader implements OrganizationDirectoryListener {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AdminUserAndGroupLoader.class);

  /** The administrator user configuration option */
  public static final String OPT_ADMIN_USER = "org.opencastproject.security.admin.user";

  /** The administrator password configuration option */
  public static final String OPT_ADMIN_PASSWORD = "org.opencastproject.security.admin.pass";

  /**
   * The administrator password set by default in the configuration file.
   * Note that this is not set if it is not defined in the configuration file.
   **/
  private static final String DEFAULT_ADMIN_PASSWORD_CONFIGURATION = "opencast";

  /** The administrator email configuration option */
  public static final String OPT_ADMIN_EMAIL = "org.opencastproject.admin.email";

  /** The administrator roles configuration option */
  public static final String OPT_ADMIN_ROLES = "org.opencastproject.security.admin.roles";

  /** The administrator group's suffix */
  public static final String SYSTEM_ADMIN_GROUP_SUFFIX = "_SYSTEM_ADMINS";

  /** Path to the list of roles */
  public static final String ROLES_PATH_PREFIX = "/roles";

  /** The path to the organization admin's list of roles */
  public static final String SYSTEM_ADMIN_FILE = "system-admins";

  /** The configuration value of the administrator username */
  private String adminUserName = null;

  /** The configuration value of the administrator password */
  private String adminPassword = null;

  /** The configuration value of the administrator email */
  private String adminEmail = null;

  /** The configuration value of the administrator roles */
  private String adminRoles = null;

  /** User and role provider */
  protected JpaUserAndRoleProvider userAndRoleProvider;

  /** Group provider */
  protected JpaGroupRoleProvider groupRoleProvider;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The security service to use to run as the context for adding the groups */
  protected SecurityService securityService;

  /** The component context */
  protected ComponentContext componentCtx = null;

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.debug("Activating admin group loader");
    BundleContext bundleCtx = cc.getBundleContext();
    adminUserName = StringUtils.trimToNull(bundleCtx.getProperty(OPT_ADMIN_USER));
    adminPassword = StringUtils.trimToNull(bundleCtx.getProperty(OPT_ADMIN_PASSWORD));
    adminEmail = StringUtils.trimToNull(bundleCtx.getProperty(OPT_ADMIN_EMAIL));
    adminRoles = StringUtils.trimToNull(bundleCtx.getProperty(OPT_ADMIN_ROLES));

    if (DEFAULT_ADMIN_PASSWORD_CONFIGURATION.equals(adminPassword)) {
    logger.warn("\n"
            + "######################################################\n"
            + "#                                                    #\n"
            + "# WARNING: Opencast still uses the default admin     #\n"
            + "#          credentials. Never do this in production. #\n"
            + "#                                                    #\n"
            + "#          To change the password, edit the key      #\n"
            + "#          org.opencastproject.security.admin.pass   #\n"
            + "#          in custom.properties.                     #\n"
            + "#                                                    #\n"
            + "######################################################");
    }

    // Keep a reference to the component context
    componentCtx = cc;

    // Create the administration user and group for each organization
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      createSystemAdministratorUserAndGroup(organization);
    }
  }

  /**
   * Creates a JpaOrganization from an organization
   *
   * @param org
   *          the organization
   */
  private JpaOrganization fromOrganization(Organization org) {
    if (org instanceof JpaOrganization)
      return (JpaOrganization) org;
    return new JpaOrganization(org.getId(), org.getName(), org.getServers(), org.getAdminRole(), org.getAnonymousRole(),
            org.getProperties());
  }

  /**
   * Creates initial groups for system administrators per organization.
   *
   * @param organization
   *          the organization
   * @throws IOException
   *           if loading of the role lists fails
   * @throws IllegalStateException
   *           if the specified role list is unavailable
   */
  private void createSystemAdministratorUserAndGroup(final Organization organization) {

    if ((adminUserName == null) || (adminPassword == null)) {
      logger.info("The administrator user and group loader is disabled.");
      return;
    }

    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(componentCtx, organization), new Effect0() {
      @Override
      protected void run() {
        try {
          JpaOrganization org = fromOrganization(organizationDirectoryService.getOrganization(organization.getId()));

          // Make sure the administrator exists for this organization. Note that the user will gain its roles through
          // membership in the administrator group
          JpaUser adminUser = (JpaUser) userAndRoleProvider.loadUser(adminUserName);
          boolean userExists = adminUser != null;
          // Add roles according to the system configuration
          Set<JpaRole> adminRolesSet = new HashSet<JpaRole>();
          if (adminRoles != null) {
            for (String r : StringUtils.split(adminRoles, ',')) {
              String roleId = StringUtils.trimToNull(r);
              if (roleId != null) {
                adminRolesSet.add(new JpaRole(roleId, org));
              }
            }
          }
          String adminUserFullName = organization.getName().concat(" Administrator");
          adminUser = new JpaUser(adminUserName, adminPassword, org, adminUserFullName, adminEmail, PROVIDER_NAME,
                                  false, adminRolesSet);
          if (userExists) {
            userAndRoleProvider.updateUser(adminUser);
            logger.info("Administrator user for '{}' updated", org.getId());
          } else {
            userAndRoleProvider.addUser(adminUser);
            logger.info("Administrator user for '{}' created", org.getId());
          }

          // System administrator group
          String adminGroupId = org.getId().toUpperCase().concat(SYSTEM_ADMIN_GROUP_SUFFIX);
          JpaGroup systemAdminGroup = (JpaGroup) groupRoleProvider.loadGroup(adminGroupId, org.getId());
          Set<JpaRole> systemAdminRoles = new HashSet<JpaRole>();
          Set<String> systemAdminRolesIds = new HashSet<String>();

          // Add global system roles as defined in the code base
          for (String role : SecurityConstants.GLOBAL_SYSTEM_ROLES) {
            systemAdminRoles.add(new JpaRole(role, org));
            systemAdminRolesIds.add(role);
          }

          // Add roles as defined in the code base
          for (String role : loadGroupRoles(SYSTEM_ADMIN_FILE)) {
            systemAdminRoles.add(new JpaRole(role, org));
            systemAdminRolesIds.add(role);
          }

          // Add roles as defined by the organization
          if (StringUtils.isNotBlank(org.getAdminRole())) {
            systemAdminRoles.add(new JpaRole(org.getAdminRole(), org));
            systemAdminRolesIds.add(org.getAdminRole());
          }
          if (StringUtils.isNotBlank(org.getAnonymousRole())) {
            systemAdminRoles.add(new JpaRole(org.getAnonymousRole(), org));
            systemAdminRolesIds.add(org.getAnonymousRole());
          }

          // Add roles according to the system configuration
          if (adminRoles != null) {
            for (String r : StringUtils.split(adminRoles, ',')) {
              String roleId = StringUtils.trimToNull(r);
              if (roleId != null) {
                systemAdminRoles.add(new JpaRole(roleId, org));
                systemAdminRolesIds.add(roleId);
              }
            }
          }

          // Make sure the organization administrator is part of this group
          Set<String> groupMembers = new HashSet<String>();
          groupMembers.add(adminUserName);

          // Create the group
          String adminGroupName = org.getName().concat(" System Administrators");
          String adminGroupDescription = "System administrators of '" + org.getName() + "'";
          if (systemAdminGroup == null) {
            logger.info("Creating {}'s system administrator group", org.getId());
            systemAdminGroup = new JpaGroup(adminGroupId, org, adminGroupName, adminGroupDescription, systemAdminRoles);
            systemAdminGroup.setMembers(groupMembers);
            groupRoleProvider.addGroup(systemAdminGroup);
          } else {
            logger.info("Updating roles of {}'s system administrator group", org.getId());
            groupMembers.addAll(systemAdminGroup.getMembers());
            groupRoleProvider.updateGroup(adminGroupId, adminGroupName, adminGroupDescription,
                    StringUtils.join(systemAdminRolesIds, ','), StringUtils.join(groupMembers, ','));
          }

        } catch (NotFoundException e) {
          logger.error("Unable to load system administrator group because {}", ExceptionUtils.getStackTrace(e));
        } catch (IllegalStateException e) {
          logger.error("Unable to load system administrator group because {}", ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
          logger.error("Unable to load system administrator group because {}", ExceptionUtils.getStackTrace(e));
        } catch (Throwable t) {
          logger.error("Unable to load system administrator group because {}", ExceptionUtils.getStackTrace(t));
        }
      }
    });
  }

  /**
   * Loads the set of roles from the properties file, located at {@link #MEMBERS_PATH_PREFIX}.
   *
   * @param roleFileName
   *          name of the properties file containing the roles
   * @return the set of roles
   */
  private Set<String> loadGroupRoles(String roleFileName) throws IllegalStateException, IOException {
    String propertiesFile = UrlSupport.concat(ROLES_PATH_PREFIX, roleFileName);

    InputStream rolesIS = null;
    try {
      // Load the properties
      rolesIS = AdminUserAndGroupLoader.class.getResourceAsStream(propertiesFile);
      if (null == rolesIS) {
        return new TreeSet<>(); // if file doesn't exits assume it's empty
      }
      Stream<String> stream = Stream.$(IOUtils.readLines(rolesIS)).filter(new Fn<String, Boolean>() {
        @Override
        public Boolean apply(String line) {
          if (StringUtils.trimToEmpty(line).startsWith("#"))
            return false;
          return true;
        }
      });
      return new TreeSet<>(stream.toSet());
    } catch (IOException e) {
      logger.error("Error loading system roles from file {}", propertiesFile);
      throw e;
    } finally {
      IOUtils.closeQuietly(rolesIS);
    }

  }

  @Override
  public void organizationRegistered(Organization organization) {
    createSystemAdministratorUserAndGroup(organization);
  }

  @Override
  public void organizationUnregistered(Organization organization) {
    // Nothing to do
  }

  @Override
  public void organizationUpdated(Organization organization) {
    // Nothing to do
  }

  /**
   * OSGi callback for declarative services.
   *
   * @param groupRoleProvider
   *          the groupRoleProvider to set
   */
  void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /**
   * OSGi callback for declarative services.
   *
   * @param userAndRoleProvider
   *          the user and role provider to set
   */
  void setUserAndRoleProvider(JpaUserAndRoleProvider userAndRoleProvider) {
    this.userAndRoleProvider = userAndRoleProvider;
  }

  /**
   * OSGi callback for declarative services.
   *
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
    this.organizationDirectoryService.addOrganizationDirectoryListener(this);
  }

  /**
   * OSGi callback for declarative services.
   *
   * @param securityService
   *          the security service
   */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
