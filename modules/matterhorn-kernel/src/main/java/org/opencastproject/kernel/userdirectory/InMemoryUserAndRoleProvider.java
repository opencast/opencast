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

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ADMIN;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;

import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.PasswordEncoder;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory user directory containing the users and roles used by the system.
 */
public class InMemoryUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The roles associated with the matterhorn system account */
  public static final String[] MH_SYSTEM_ROLES = new String[] { "ROLE_ADMIN", "ROLE_USER", DEFAULT_ORGANIZATION_ADMIN };

  /**
   * A collection of accounts internal to Matterhorn.
   */
  protected Map<String, User> internalAccounts = null;

  /**
   * Callback to activate the component.
   * 
   * @param cc
   *          the declarative services component context
   */
  protected void activate(ComponentContext cc) {
    internalAccounts = new HashMap<String, User>();

    // Create the digest auth user with a clear text password
    String digestUsername = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
    String digestUserPass = cc.getBundleContext().getProperty("org.opencastproject.security.digest.pass");
    User systemAccount = new User(digestUsername, digestUserPass, DEFAULT_ORGANIZATION_ID, MH_SYSTEM_ROLES);
    internalAccounts.put(digestUsername, systemAccount);

    // Create a demo user with an encoded password for use in the UI
    String adminUsername = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.admin.user"));
    String adminUserPass = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.admin.pass"));
    String adminUserRoles = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.admin.roles"));

    // Load the admin user, if necessary
    if (StringUtils.isNotBlank(adminUsername) && StringUtils.isNotBlank(adminUserPass)
            && StringUtils.isNotBlank(adminUserRoles)) {

      // Strip any whitespace from the roles
      String[] roleArray = StringUtils.split(adminUserRoles, ',');
      for (int i = 0; i < roleArray.length; i++)
        roleArray[i] = StringUtils.trim(roleArray[i]);

      // Encode the password
      String encodedPass = PasswordEncoder.encode(adminUserPass, adminUsername);

      // Add the user
      internalAccounts.put(adminUsername, new User(adminUsername, encodedPass, DEFAULT_ORGANIZATION_ID, roleArray));
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public String[] getRoles() {
    return MH_SYSTEM_ROLES;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    return internalAccounts.get(userName);
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
    return new String[0];
  }

}
