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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_SUDO_ROLE;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An in-memory user directory containing the users and roles used by the system.
 */
public class InMemoryUserAndRoleProvider implements UserProvider, RoleProvider, ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(InMemoryUserAndRoleProvider.class);

  public static final String PROVIDER_NAME = "system";

  /** The digest users */
  public static final String DIGEST_USER_NAME = "System User";
  public static final String CAPTURE_AGENT_USER_NAME = "Capture Agent";

  /** Configuration key for the digest users */
  public static final String DIGEST_USER_KEY = "org.opencastproject.security.digest.user";
  public static final String CAPTURE_AGENT_USER_PREFIX = "capture_agent.user.";

  /** Configuration key for the digest password */
  public static final String DIGEST_PASSWORD_KEY = "org.opencastproject.security.digest.pass";

  /**
   * System password set by default in the configuration file.
   * Note that this is not set if it is not defined in the configuration file.
   * */
  private static final String DIGEST_PASSWORD_DEFAULT_CONFIGURATION = "CHANGE_ME";

  /** Configuration key for optional additional roles for the capture agent user */
  public static final String CAPTURE_AGENT_ROLES_PREFIX = "capture_agent.roles.";

  /** The list of in-memory users */
  private final Map<String, List<User>> inMemoryUsers = new ConcurrentHashMap<>();

  /**
   * List of capture agent users.
   * <ul>
   *   <li>the organization id is used as key.</li>
   *   <li>the value is a list of capture agent user for that organization with each user being represented by a list
   *       of the username and password, followed by additional roles</li>
   * </ul>
   **/
  private Map<String, List<List<String>>> captureAgentUsers = new ConcurrentHashMap<>();

  /** The security service */
  protected SecurityService securityService;

  private String digestUsername;
  private String digestUserPass;

  /**
   * Callback to activate the component.
   *
   * @param cc
   *          the declarative services component context
   */
  protected void activate(ComponentContext cc) {
    digestUsername = StringUtils.trimToNull(cc.getBundleContext().getProperty(DIGEST_USER_KEY));
    if (digestUsername == null) {
      logger.warn("Digest username has not been configured ({})", DIGEST_USER_KEY);
    }

    digestUserPass = StringUtils.trimToNull(cc.getBundleContext().getProperty(DIGEST_PASSWORD_KEY));
    if (digestUserPass == null) {
      logger.warn("Digest password has not been configured ({})", DIGEST_PASSWORD_KEY);
    } else if (DIGEST_PASSWORD_DEFAULT_CONFIGURATION.equals(digestUserPass)) {
      logger.warn("\n"
              + "######################################################\n"
              + "#                                                    #\n"
              + "# WARNING: Opencast still uses the default system    #\n"
              + "#          credentials. Never do this in production. #\n"
              + "#                                                    #\n"
              + "#          To change the password, edit the key      #\n"
              + "#          org.opencastproject.security.digest.pass  #\n"
              + "#          in custom.properties.                     #\n"
              + "#                                                    #\n"
              + "######################################################");
    }
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      captureAgentUsers.clear();
      // enforce reload of users if the configuration has changed
      inMemoryUsers.clear();
      return;
    }

    Map<String, List<List<String>>> newCAUsers = new ConcurrentHashMap<>();

    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      final String key = keys.nextElement();
      // skip non user definition keys
      if (!key.startsWith(CAPTURE_AGENT_USER_PREFIX)) {
        continue;
      }
      final String[] orgUser = key.substring(CAPTURE_AGENT_USER_PREFIX.length()).split("\\.");
      if (orgUser.length != 2) {
        logger.warn("Ignoring invalid capture agent user definition. Should be {}.<organization>.<username>, was {}",
                CAPTURE_AGENT_USER_PREFIX, key);
      }
      final String orgId = orgUser[0];
      final String username = orgUser[1];
      final String password = Objects.toString(properties.get(key), null);
      if (password == null) {
        continue;
      }

      // check for extra roles
      final String rolesStr = Objects.toString(properties.get(CAPTURE_AGENT_ROLES_PREFIX + orgId + '.' + username), "");
      final String[] roles = StringUtils.split(rolesStr, ", ");
      final List<String> userData = new ArrayList<>();
      userData.add(username);
      userData.add(password);
      userData.addAll(Arrays.asList(roles));
      if (!newCAUsers.containsKey(orgId)) {
        newCAUsers.put(orgId, new ArrayList<>());
      }
      newCAUsers.get(orgId).add(userData);
    }

    // update list of CA users
    captureAgentUsers = newCAUsers;
    // enforce reload of users if the configuration has changed
    inMemoryUsers.clear();
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * Get a list of all users for the current organization, loading the users from configuration if they had not been
   * loaded before.
   *
   * @return List of users.
   */
  private List<User> getOrganizationUsers() {
    final Organization organization = securityService.getOrganization();
    final List<User> users = inMemoryUsers.get(organization.getId());
    if (users == null) {
      return createSystemUsers(organization);
    }
    return users;
  }

  /**
   * Creates the digest users.
   *
   * @param organization
   *          Organization to set users for
   * @return List of all users
   */
  private synchronized List<User> createSystemUsers(final Organization organization) {
    List<User> users = inMemoryUsers.get(organization.getId());
    if (users != null) {
      logger.trace("Organization users have already been initialized. Aborting.");
      return users;
    }
    users = new ArrayList<>();
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

    // Create the system user
    if (digestUsername != null && digestUserPass != null) {

      // Role set for the system user
      Set<JaxbRole> roleList = new HashSet<>();
      for (String roleName : SecurityConstants.GLOBAL_SYSTEM_ROLES) {
        roleList.add(new JaxbRole(roleName, jaxbOrganization));
      }
      User digestUser = new JaxbUser(digestUsername, digestUserPass, DIGEST_USER_NAME, null, getName(),
              jaxbOrganization, roleList);
      users.add(digestUser);
      logger.info("Added system digest user '{}' for organization '{}'", digestUsername, organization.getId());
    }

    for (List<String> userData: captureAgentUsers.getOrDefault(organization.getId(), new ArrayList<>())) {

      final String username = userData.get(0);
      final String password = userData.get(1);

      // Role set for the capture agent user
      Set<JaxbRole> caRoleList = new HashSet<>();
      // Add the organization anonymous role to the capture agent user
      caRoleList.add(new JaxbRole(organization.getAnonymousRole(), jaxbOrganization));
      // Add global CA user roles
      Arrays.stream(SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLES)
              .forEach((role) -> caRoleList.add(new JaxbRole(role, jaxbOrganization)));
      // Add additional custom rules
      userData.stream().skip(2).forEach((role) -> caRoleList.add(new JaxbRole(role, jaxbOrganization)));

      // Create the capture agent user
      logger.info("Creating the capture agent digest user '{}'", username);
      User caUser = new JaxbUser(username, password, CAPTURE_AGENT_USER_NAME, null, getName(),
              jaxbOrganization, caRoleList);
      users.add(caUser);
    }

    inMemoryUsers.put(organization.getId(), users);
    return users;
  }

  @Override
  public Iterator<User> getUsers() {
    return getOrganizationUsers().iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    return getOrganizationUsers().stream()
            .filter(user -> user.getUsername().equals(userName))
            .findFirst()
            .orElse(null);
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
    return getOrganizationUsers().stream()
            .filter(user -> user.getUsername().equals(userName))
            .flatMap(user -> user.getRoles().stream())
            .collect(Collectors.toList());
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    // Find all users from the user providers
    return getOrganizationUsers().stream()
            .filter(user -> like(user.getUsername(), query))
            .sorted(Comparator.comparing(User::getUsername))
            .skip(offset).limit(limit <= 0 ? Long.MAX_VALUE : limit)
            .iterator();
  }

  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    // Find all roles from the role providers
    return getOrganizationUsers().stream()
            .flatMap(user -> user.getRoles().stream())
            .filter(role -> (like(role.getName(), query) || like(role.getDescription(), query))
                    && !(target == Role.Target.ACL && GLOBAL_SUDO_ROLE.equals(role.getName())))
            .sorted(Comparator.comparing(Role::getName))
            .skip(offset).limit(limit <= 0 ? Long.MAX_VALUE : limit)
            .iterator();
  }

  private boolean like(final String string, final String query) {
    if (string == null) {
      return false;
    }
    final String regex = query.replace("_", ".").replace("%", ".*?");
    final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return pattern.matcher(string).matches();
  }

  @Override
  public long countUsers() {
    return getOrganizationUsers().size();
  }

  @Override
  public void invalidate(String userName) {
    // nothing to do
  }

  /**
   * Sets a reference to the security service.
   *
   * @param securityService
   *          the security service
   */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
