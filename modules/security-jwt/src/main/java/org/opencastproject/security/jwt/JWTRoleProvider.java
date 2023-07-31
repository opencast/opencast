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

package org.opencastproject.security.jwt;

import org.opencastproject.security.api.GroupProvider;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.api.AAIRoleProvider;
import org.opencastproject.userdirectory.api.UserReferenceProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Role provider for users authenticated based on JWTs.
 */
public class JWTRoleProvider implements AAIRoleProvider, GroupProvider {

  /** Security service */
  private final SecurityService securityService;

  /** User reference provider */
  private final UserReferenceProvider userReferenceProvider;

  public JWTRoleProvider(SecurityService securityService, UserReferenceProvider userReferenceProvider) {
    this.securityService = securityService;
    this.userReferenceProvider = userReferenceProvider;
  }

  @Override
  public Iterator<Role> getRoles() {
    JaxbOrganization organization = JaxbOrganization.fromOrganization(securityService.getOrganization());
    HashSet<Role> roles = new HashSet<>();
    roles.add(new JaxbRole(organization.getAnonymousRole(), organization));
    roles.addAll(securityService.getUser().getRoles());
    return roles.iterator();
  }

  @Override
  public List<Role> getRolesForUser(String userName) {
    ArrayList<Role> roles = new ArrayList<>();
    User user = userReferenceProvider.loadUser(userName);
    if (user != null) {
      roles.addAll(user.getRoles());
    }
    return roles;
  }

  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }
    HashSet<Role> foundRoles = new HashSet<Role>();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if (like(role.getName(), query) || like(role.getDescription(), query)) {
        foundRoles.add(role);
      }
    }
    return offsetLimitCollection(offset, limit, foundRoles).iterator();

  }

  /**
   * Slices a given hash set with a given offset and a given limit of entries.
   *
   * @param offset The offset.
   * @param limit The limit for the number of entries.
   * @param entries The original entries.
   * @return The sliced hash set.
   */
  private <T> HashSet<T> offsetLimitCollection(int offset, int limit, HashSet<T> entries) {
    HashSet<T> result = new HashSet<T>();
    int i = 0;
    for (T entry : entries) {
      if (limit != 0 && result.size() >= limit) {
        break;
      }
      if (i >= offset) {
        result.add(entry);
      }
      i++;
    }
    return result;
  }

  /**
   * Evaluates whether a given query matches on a string.
   *
   * @param string The string.
   * @param query The query.
   * @return <code>true</code> if the query matches, <code>false</code> otherwise.
   */
  private boolean like(String string, final String query) {
    if (string == null) {
      return false;
    }

    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

  @Override
  public List<Role> getRolesForGroup(String groupName) {
    return null;
  }

}
