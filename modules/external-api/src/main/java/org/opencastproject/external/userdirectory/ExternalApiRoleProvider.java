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

package org.opencastproject.external.userdirectory;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.Role.Type;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The External API role provider.
 */
@Component(
    property = {
        "service.description=Provides the External API roles"
    },
    immediate = true,
    service = { RoleProvider.class }
)
public class ExternalApiRoleProvider implements RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ExternalApiRoleProvider.class);

  /** The security service */
  protected SecurityService securityService = null;

  private Set<String> roles;

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  protected void activate(ComponentContext cc) {
    String rolesFile = ExternalGroupLoader.ROLES_PATH_PREFIX + File.separator
        + ExternalGroupLoader.EXTERNAL_APPLICATIONS_ROLES_FILE;
    try (InputStream in = getClass().getResourceAsStream(rolesFile)) {
      roles = new TreeSet<>(IOUtils.readLines(in, UTF_8));
    } catch (IOException e) {
      logger.error("Unable to read available roles", e);
    }
    logger.info("Activated External API role provider");
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
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    // These roles are not meaningful for use in ACLs
    if (target == Role.Target.ACL) {
      return Collections.emptyIterator();
    }

    Organization organization = securityService.getOrganization();

    BiFunction<String, String, Boolean> filterByName = (role, q) -> like(role, q);

    BiFunction<String, Organization, Role> toRole = (role, org) ->
        new JaxbRole(role, JaxbOrganization.fromOrganization(org), "External API Role", Type.INTERNAL);

    // Java Stream of roles (assuming roles is a Collection<String> or similar)
    Stream<String> roleStream = roles.stream()
        .filter(role -> filterByName.apply(role, query));

    // Apply offset
    if (offset > 0) {
      roleStream = roleStream.skip(offset);
    }

    // Apply limit if positive
    if (limit > 0) {
      roleStream = roleStream.limit(limit);
    }

    // Map to Role objects
    Stream<Role> resultStream = roleStream.map(role -> toRole.apply(role, organization));

    return resultStream.iterator();
  }

  private static boolean like(String string, final String query) {
    if (string == null) {
      return false;
    }
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }
}
