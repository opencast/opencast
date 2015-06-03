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
package org.opencastproject.adminui.userdirectory;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamOp;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/roles.txt");
      roles = new TreeSet<String>(IOUtils.readLines(in, "UTF-8"));
    } catch (IOException e) {
      logger.error("Unable to read available roles: {}", ExceptionUtils.getStackTrace(e));
    } finally {
      IOUtils.closeQuietly(in);
    }
    logger.info("Activated Admin UI roles role provider");
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    Organization organization = securityService.getOrganization();
    return Stream.$(roles).map(toRole._2(organization)).iterator();
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
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    Organization organization = securityService.getOrganization();
    return Stream.$(roles).filter(filterByName._2(query)).drop(offset)
            .apply(limit > 0 ? StreamOp.<String> id().take(limit) : StreamOp.<String> id()).map(toRole._2(organization))
            .iterator();
  }

  private static boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

  private static final Fn2<String, Organization, Role> toRole = new Fn2<String, Organization, Role>() {
    @Override
    public Role ap(String role, Organization organization) {
      return new JaxbRole(role, JaxbOrganization.fromOrganization(organization));
    }
  };

  private static final Fn2<String, String, Boolean> filterByName = new Fn2<String, String, Boolean>() {
    @Override
    public Boolean ap(String role, String query) {
      return like(role, query);
    }
  };
}
