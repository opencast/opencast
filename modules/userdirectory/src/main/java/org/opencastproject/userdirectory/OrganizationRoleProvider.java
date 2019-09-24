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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.Role.Type;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The organization role provider returning the admin and anonymous role from the current organization.
 */
public class OrganizationRoleProvider implements RoleProvider {

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  private Iterator<Role> getRoles() {
    Organization organization = securityService.getOrganization();
    List<Role> roles = new ArrayList<Role>();
    // The GLOBAL_ADMIN_ROLE is provided by the InMemoryUserAndRoleProvider
    if (!GLOBAL_ADMIN_ROLE.equals(organization.getAdminRole())) {
      roles.add(new JaxbRole(organization.getAdminRole(), JaxbOrganization.fromOrganization(organization), "", Type.INTERNAL));
    }
    roles.add(new JaxbRole(organization.getAnonymousRole(), JaxbOrganization.fromOrganization(organization), "", Type.SYSTEM));
    return roles.iterator();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    return Collections.emptyList();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization organization = securityService.getOrganization();
    HashSet<Role> foundRoles = new HashSet<Role>();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      // Anonymous roles are not relevant for adding to users or groups
      if ((target == Role.Target.USER) && role.getName().equals(organization.getAnonymousRole()))
        continue;
      if (like(role.getName(), query) || like(role.getDescription(), query))
        foundRoles.add(role);
    }
    return offsetLimitCollection(offset, limit, foundRoles).iterator();
  }

  private <T> HashSet<T> offsetLimitCollection(int offset, int limit, HashSet<T> entries) {
    HashSet<T> result = new HashSet<T>();
    int i = 0;
    for (T entry : entries) {
      if (limit != 0 && result.size() >= limit)
        break;
      if (i >= offset)
        result.add(entry);
      i++;
    }
    return result;
  }

  private boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

}
