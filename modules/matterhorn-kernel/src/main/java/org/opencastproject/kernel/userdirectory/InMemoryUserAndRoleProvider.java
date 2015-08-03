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

package org.opencastproject.kernel.userdirectory;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.PasswordEncoder;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamOp;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An in-memory user directory containing the users and roles used by the system.
 */
public class InMemoryUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(InMemoryUserAndRoleProvider.class);

  /** The roles associated with the matterhorn system account */
  public static final String[] MH_SYSTEM_ROLES = new String[] { GLOBAL_ADMIN_ROLE };

  public static final String PROVIDER_NAME = "matterhorn-in-memory";

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

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  private List<User> getAdminUsers(Organization org) {
    List<User> users = new ArrayList<User>();

    JaxbOrganization organization = JaxbOrganization.fromOrganization(org);
    JaxbRole orgAdminRole = new JaxbRole(org.getAdminRole(), organization);
    JaxbRole orgAnonymousRole = new JaxbRole(org.getAnonymousRole(), organization);

    // Create the digest auth user with a clear text password
    Set<JaxbRole> roleList = new HashSet<JaxbRole>();
    roleList.add(orgAdminRole);
    roleList.add(orgAnonymousRole);
    for (String roleName : MH_SYSTEM_ROLES) {
      roleList.add(new JaxbRole(roleName, organization));
    }

    // Add the roles as defined in the system configuration
    if (StringUtils.isNotBlank(adminUserRoles)) {
      for (String roleName : StringUtils.split(adminUserRoles, ',')) {
        roleList.add(new JaxbRole(StringUtils.trim(roleName), organization));
      }
    }

    User digestUser = new JaxbUser(digestUsername, digestUserPass, "Digest User", null, getName(), true, organization,
            roleList);
    users.add(digestUser);

    // Create the admin user with an encoded password for use in the UI, if necessary
    if (adminUsername != null && adminUserPass != null) {

      // Encode the password
      String encodedPass = PasswordEncoder.encode(adminUserPass, adminUsername);

      JaxbUser adminUser = new JaxbUser(adminUsername, encodedPass, "Admin User", null, getName(), true, organization,
              roleList);
      users.add(adminUser);
    }

    return users;
  }

  @Override
  public Iterator<User> getUsers() {
    return getAdminUsers(securityService.getOrganization()).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    Stream<Role> roles = Stream.empty();
    for (User user : getAdminUsers(securityService.getOrganization())) {
      roles = roles.append(user.getRoles()).sort(roleComparator);
    }
    return roles.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    for (User user : getAdminUsers(securityService.getOrganization())) {
      if (user.getUsername().equals(userName))
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
  public List<Role> getRolesForUser(String userName) {
    User user = loadUser(userName);
    if (user == null)
      return Collections.emptyList();
    return Collections.unmodifiableList(new ArrayList<Role>(user.getRoles()));
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    // Find all users from the user providers
    Stream<User> users = Stream.empty();
    for (User user : getAdminUsers(securityService.getOrganization())) {
      if (like(user.getUsername(), query))
        users = users.append(Stream.single(user)).sort(userComparator);
    }
    return users.drop(offset).apply(limit > 0 ? StreamOp.<User>id().take(limit) : StreamOp.<User>id()).iterator();
  }

  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    // Find all roles from the role providers
    Stream<Role> roles = Stream.empty();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if (like(role.getName(), query) || like(role.getDescription(), query))
        roles = roles.append(Stream.single(role)).sort(roleComparator);
    }
    return roles.drop(offset).apply(limit > 0 ? StreamOp.<Role>id().take(limit) : StreamOp.<Role>id()).iterator();
  }

  private boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

  @Override
  public void invalidate(String userName) {
    // nothing to do
  }

  private static final Comparator<Role> roleComparator = new Comparator<Role>() {
    @Override
    public int compare(Role role1, Role role2) {
      return role1.getName().compareTo(role2.getName());
    }
  };

  private static final Comparator<User> userComparator = new Comparator<User>() {
    @Override
    public int compare(User user1, User user2) {
      return user1.getUsername().compareTo(user2.getUsername());
    }
  };

}
