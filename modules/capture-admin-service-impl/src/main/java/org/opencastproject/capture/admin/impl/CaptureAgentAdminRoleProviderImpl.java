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

package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.util.SecurityUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The capture agent admin role provider provides a role for each registered capture agent
 */
public class CaptureAgentAdminRoleProviderImpl implements RoleProvider {

  private SecurityService securityService;

  /**
   * @param service
   *          the securityService to set
   */
  public void setSecurityService(final SecurityService service) {
    this.securityService = service;
  }


  private CaptureAgentStateService captureAgentService;

  public void setCaptureAgentStateService(final CaptureAgentStateService service) {
    this.captureAgentService = service;
  }

  private Role generateCaRole(final String name) {
    final String roleName = SecurityUtil.getCaptureAgentRole(name);
    final JaxbOrganization organization = JaxbOrganization.fromOrganization(securityService.getOrganization());
    final String description = "Role for capture agent \"" + name + "\"";
    final Role.Type system = Role.Type.INTERNAL;
    return new JaxbRole(roleName, organization, description, system);
  }

  /**
   * @see RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    return getRolesStream().iterator();
  }

  private Stream<Role> getRolesStream() {
    return this.captureAgentService.getKnownAgents()
            .keySet()
            .stream()
            .map(this::generateCaRole);
  }

  /**
   * @see RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(final String userName) {
    return Collections.emptyList();
  }

  /**
   * @see RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see RoleProvider#findRoles(String,Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(final String query, final Role.Target target, final int offset, final int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    // These roles are not meaningful for use in ACLs
    if (target == Role.Target.ACL) {
      return Collections.emptyIterator();
    }

    Stream<Role> roleStream = getRolesStream()
            .filter(e -> like(e.getName(), query) || like(e.getDescription(), query))
            .skip(offset);
    if (limit != 0) {
      roleStream = roleStream.limit(limit);
    }
    return roleStream.iterator();
  }

  // This is taken liberally from ConfigurableLoginHandler. This really needs to go into a separate interface.
  private static boolean like(final String string, final String query) {
    final String regex = query.replace("_", ".").replace("%", ".*?");
    final Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }
}
