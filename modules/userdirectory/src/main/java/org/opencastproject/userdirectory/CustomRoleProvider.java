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

import static org.opencastproject.security.api.UserProvider.ALL_ORGANIZATIONS;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.Role.Type;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * An in-memory role provider containing administratively-defined custom roles
 */
@Component(
    property = {
        "service.description=Provides custom roles"
    },
    immediate = true,
    service = { RoleProvider.class }
)
public class CustomRoleProvider implements RoleProvider {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CustomRoleProvider.class);

  /** Configuration key for the custom role list */
  public static final String CUSTOM_ROLES_KEY = "org.opencastproject.security.custom.roles";

  /** Configuration key for the custom role pattern */
  public static final String CUSTOM_ROLES_PATTERN_KEY = "org.opencastproject.security.custom.roles.pattern";

  /** The security service */
  protected SecurityService securityService = null;

  /** The list of custom roles */
  private Set<String> roles = null;

  /** The custom roles pattern */
  private Pattern rolematch = null;

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
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

    roles = new TreeSet<>();
    String customRoleList = StringUtils.trimToNull(cc.getBundleContext().getProperty(CUSTOM_ROLES_KEY));

    if (customRoleList != null) {
      List<String> items = Arrays.asList(customRoleList.split("\\s*,\\s*"));
      for (String item : items) {
        logger.debug("Adding custom role '{}'", item);
        roles.add(item);
      }
    }

    String rolePattern = StringUtils.trimToNull(cc.getBundleContext().getProperty(CUSTOM_ROLES_PATTERN_KEY));
    if (rolePattern != null) {
      try {
        rolematch = Pattern.compile(rolePattern);
      } catch (PatternSyntaxException e) {
        logger.warn("Invalid regular expression for custom roles pattern: {}", rolePattern);
      }
    }

    if (rolematch != null) {
      logger.info("CustomRoleProvider activated, {} custom role(s), custom role pattern {}", roles.size(), rolePattern);
    } else {
      logger.info("CustomRoleProvider activated, {} custom role(s)", roles.size());
    }
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
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    return Collections.emptyList();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    var organization = JaxbOrganization.fromOrganization(securityService.getOrganization());

    // Match the custom regular expression first if this is an ACL role query
    if ((target == Role.Target.ACL) && (rolematch != null)) {
      String exactQuery = StringUtils.removeEnd(query, "%");
      Matcher m = rolematch.matcher(exactQuery);
      if (m.matches()) {
        return Collections
            .singletonList((Role) new JaxbRole(exactQuery, organization, "Custom Role", Role.Type.EXTERNAL))
            .iterator();
      }
    }

    // Otherwise match on the custom roles specified in a list
    return roles.stream()
        .filter(role -> like(role, query))
        .skip(offset)
        .limit(limit > 0 ? limit : Long.MAX_VALUE)
        .map(role -> (Role) new JaxbRole(role, organization, "Custom Role", Type.INTERNAL))
        .iterator();
  }

  private static boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

}
