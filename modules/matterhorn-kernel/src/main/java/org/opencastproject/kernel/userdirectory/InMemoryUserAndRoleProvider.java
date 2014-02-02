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
package org.opencastproject.kernel.userdirectory;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.PasswordEncoder;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An in-memory user directory containing the users and roles used by the system.
 */
public class InMemoryUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(InMemoryUserAndRoleProvider.class);

  /** The roles associated with the matterhorn system account */
  public static final String[] MH_SYSTEM_ROLES = new String[] { GLOBAL_ADMIN_ROLE };

  protected OrganizationDirectoryService orgDirectoryService;

  protected SecurityService securityService;

  private String digestUsername;
  private String digestUserPass;
  private String adminUsername;
  private String adminUserPass;
  private String adminUserRoles;

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
  }

  /**
   * Sets a reference to the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback to activate the component.
   *
   * @param cc
   *          the declarative services component context
   */
  protected void activate(ComponentContext cc) {
    digestUsername = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
    digestUserPass = cc.getBundleContext().getProperty("org.opencastproject.security.digest.pass");
    adminUsername = StringUtils
            .trimToNull(cc.getBundleContext().getProperty("org.opencastproject.security.admin.user"));
    adminUserPass = StringUtils
            .trimToNull(cc.getBundleContext().getProperty("org.opencastproject.security.admin.pass"));
    adminUserRoles = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.admin.roles"));
  }

  private List<User> getAdminUsers(Organization organization) {
    List<User> users = new ArrayList<User>();

    String orgAdminRole = organization.getAdminRole();
    String orgAnonymousRole = organization.getAnonymousRole();

    // Create the digest auth user with a clear text password
    Set<String> roleList = new HashSet<String>();
    roleList.add(orgAdminRole);
    roleList.add(orgAnonymousRole);
    for (String roleName : MH_SYSTEM_ROLES) {
      roleList.add(roleName);
    }

    // Add the roles as defined in the system configuration
    if (StringUtils.isNotBlank(adminUserRoles)) {
      for (String roleName : StringUtils.split(adminUserRoles, ',')) {
        roleList.add(StringUtils.trim(roleName));
      }
    }

    User digestUser = new User(digestUsername, digestUserPass, organization.getId(), roleList.toArray(new String[roleList.size()]));
    users.add(digestUser);

    // Create the admin user with an encoded password for use in the UI, if necessary
    if (adminUsername != null && adminUserPass != null) {

      // Encode the password
      String encodedPass = PasswordEncoder.encode(adminUserPass, adminUsername);

      User adminUser = new User(adminUsername, encodedPass, organization.getId(), roleList.toArray(new String[roleList.size()]));
      users.add(adminUser);
    }

    return users;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public String[] getRoles() {
    Set<String> roles = new HashSet<String>();
    for (User user : getAdminUsers(securityService.getOrganization())) {
      roles.addAll(Arrays.asList(user.getRoles()));
    }
    return roles.toArray(new String[roles.size()]);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    for (User user : getAdminUsers(securityService.getOrganization())) {
      if (user.getUserName().equals(userName))
        return user;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return ALL_ORGANIZATIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public String[] getRolesForUser(String userName) {
    return new String[] {};
  }

}
