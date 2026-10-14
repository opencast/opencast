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

package org.opencastproject.userdirectory;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.OsgiUtil;

import com.google.common.base.CharMatcher;

import org.apache.commons.lang3.BooleanUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The user id role provider assigns the user id role.
 */
@Component(
    property = {
        "service.description=Provides the user id role"
    },
    immediate = true,
    service = { RoleProvider.class, UserIdRoleProvider.class, ManagedService.class }
)
public class UserIdRoleProvider implements RoleProvider, ManagedService {


  private static final String ROLE_USER = "ROLE_USER";

  private static final String ROLE_USER_PREFIX_KEY = "role.user.prefix";
  private static final String DEFAULT_ROLE_USER_PREFIX = "ROLE_USER_";

  private static final String SANITIZE_KEY = "sanitize";
  private static final boolean DEFAULT_SANITIZE = true;

  private static final CharMatcher SAFE_USERNAME = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'))
          .or(CharMatcher.inRange('0', '9')).negate().precomputed();

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserIdRoleProvider.class);

  /** The security service */
  protected SecurityService securityService = null;

  private static String userRolePrefix = DEFAULT_ROLE_USER_PREFIX;
  private static boolean sanitize = DEFAULT_SANITIZE;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  public static boolean isSanitize() {
    return sanitize;
  };

  public static String getUserRolePrefix() {
    return userRolePrefix;
  };

  public static String getUserIdRole(String userName) {
    if (sanitize) {
      userName = SAFE_USERNAME.replaceFrom(userName, "_").toUpperCase();
    }
    return userRolePrefix.concat(userName);
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    Organization organization = securityService.getOrganization();
    List<Role> roles = new ArrayList<Role>();
    roles.add(new JaxbRole(
        getUserIdRole(userName),
        JaxbOrganization.fromOrganization(organization),
        "The user id role",
        Role.Type.SYSTEM
    ));
    roles.add(new JaxbRole(
        ROLE_USER,
        JaxbOrganization.fromOrganization(organization),
        "The authenticated user role",
        Role.Type.SYSTEM
    ));
    return Collections.unmodifiableList(roles);
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String,Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    return findRoles(query, target, offset, limit, null);
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int, Boolean)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit, Boolean hasUser) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    // These roles are not meaningful for users/groups
    if (target == Role.Target.USER) {
      return Collections.emptyIterator();
    }

    logger.debug("findRoles(query={} offset={} limit={} hasUser={})", query, offset, limit, hasUser);

    HashSet<Role> foundRoles = new HashSet<Role>();
    Organization organization = securityService.getOrganization();

    // ROLE_USER never resolves to an actual user account, so it's irrelevant to a hasUser=true request -- the
    // aggregator would filter it out anyway, but there's no reason to add it just to have it discarded.
    if (!Boolean.TRUE.equals(hasUser) && like(ROLE_USER, query)) {
      foundRoles.add(new JaxbRole(
          ROLE_USER,
          JaxbOrganization.fromOrganization(organization),
          "The authenticated user role",
          Role.Type.SYSTEM
      ));
    }

    // This provider is the only one that can ever produce a role satisfying hasUser=false -- skip the
    // (potentially expensive) user enumeration below entirely for that case, we already know none of it applies.
    if (Boolean.FALSE.equals(hasUser)) {
      return foundRoles.iterator();
    }

    // Include user id roles if the caller explicitly wants them (hasUser=true, regardless of the query's shape --
    // we were told this search is about users), or -- when hasUser is unset, preserving the original behaviour
    // for callers that don't know about hasUser -- only if there is no real search text to filter by (a
    // wildcard-only query) or the query specifically targets the user id role prefix (iterating through users
    // may be slow, so this guard avoids doing so for general, not-obviously-user-related searches).
    if (!Boolean.TRUE.equals(hasUser)
        && !"%".equals(query)
        && !query.startsWith(userRolePrefix)) {
      return foundRoles.iterator();
    }

    // The query is already a properly-shaped search pattern (e.g. "%adm%"), so reuse it as-is to search
    // usernames, unless it's anchored with the user id role prefix, in which case strip that prefix first to
    // get the username pattern it actually refers to.
    String userQuery = query.startsWith(userRolePrefix) ? query.substring(userRolePrefix.length()) : query;

    Iterator<User> users = userDirectoryService.findUsers(userQuery, offset, limit);
    while (users.hasNext()) {
      User u = users.next();
      // We exclude the digest user, but then add the global ROLE_USER above
      if (!"system".equals(u.getProvider())) {
        foundRoles.add(new JaxbRole(
            getUserIdRole(u.getUsername()),
            JaxbOrganization.fromOrganization(u.getOrganization()),
            "User id role",
            Role.Type.SYSTEM
        ));
      }
    }

    return foundRoles.iterator();
  }

  private static boolean like(String string, final String query) {
    if (string == null) {
      return false;
    }
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    Optional<String> userPrefixProperty = OsgiUtil.getOptCfg(properties, ROLE_USER_PREFIX_KEY);
    if (userPrefixProperty.isPresent()) {
      userRolePrefix = userPrefixProperty.get();
      logger.info("Using configured userRole prefix '{}'", userRolePrefix);
    } else {
      userRolePrefix = DEFAULT_ROLE_USER_PREFIX;
      logger.info("Using default userRole prefix '{}'", userRolePrefix);
    }

    Optional<String> sanitizeProperty = OsgiUtil.getOptCfg(properties, SANITIZE_KEY);
    if (sanitizeProperty.isPresent()) {
      sanitize = BooleanUtils.toBoolean(sanitizeProperty.get());
      logger.info("Using configured will sanitize user names '{}'", sanitize);
    } else {
      sanitize = DEFAULT_SANITIZE;
      logger.info("Using default for sanitizing user names '{}'", sanitize);
    }
  }

}
