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

package org.opencastproject.adminui.userdirectory;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.Role.Type;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The admin UI roles role provider.
 */
public class UIRolesRoleProvider implements RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UIRolesRoleProvider.class);

  /** The security service */
  protected SecurityService securityService = null;

  private Set<String> roles;

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  protected void activate(ComponentContext cc) {
    try (InputStream in = getClass().getResourceAsStream("/roles.txt")) {
      roles = new TreeSet<>(IOUtils.readLines(in, "UTF-8"));
    } catch (IOException e) {
      logger.error("Unable to read available roles", e);
    }
    logger.info("Activated Admin UI roles role provider");
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

    // These roles are not meaningful for use in ACLs
    if (target == Role.Target.ACL) {
      return Collections.emptyIterator();
    }

    JaxbOrganization organization = JaxbOrganization.fromOrganization(securityService.getOrganization());
    return roles.stream()
      .filter((role) -> like(role, query))
      .skip(offset)
      .limit(limit > 0 ? limit : roles.size())
      .map((role) -> toRole(role, organization))
      .iterator();
  }

  private static boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

  private Role toRole(final String role, final JaxbOrganization organization) {
    return new JaxbRole(role, organization, "AdminNG UI Role", Type.INTERNAL);
  }

}
