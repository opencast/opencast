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
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamOp;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(InMemoryUserAndRoleProvider.class);

  public static final String PROVIDER_NAME = "system";

  /** The digest users */
  public static final String DIGEST_USER_NAME = "System User";
  public static final String CAPTURE_AGENT_USER_NAME = "Capture Agent";

  /** Configuration key for the digest users */
  public static final String DIGEST_USER_KEY = "org.opencastproject.security.digest.user";
  public static final String CAPTURE_AGENT_USER_KEY = "org.opencastproject.security.capture_agent.user";

  /** Configuration key for the digest password */
  public static final String DIGEST_PASSWORD_KEY = "org.opencastproject.security.digest.pass";
  public static final String CAPTURE_AGENT_PASSWORD_KEY = "org.opencastproject.security.capture_agent.pass";

  /**
   * System password set by default in the configuration file.
   * Note that this is not set if it is not defined in the configuration file.
   * */
  private static final String DIGEST_PASSWORD_DEFAULT_CONFIGURATION = "CHANGE_ME";

  /** Configuration key for optional additional roles for the capture agent user */
  public static final String CAPTURE_AGENT_EXTRA_ROLES_KEY = "org.opencastproject.security.capture_agent.roles";

  /** The list of in-memory users */
  private final List<User> inMemoryUsers = new ArrayList<User>();

  /** The organization directory */
  protected OrganizationDirectoryService orgDirectoryService;

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

    // Create the digest user
    createSystemUsers();
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * Creates the system digest user.
   */
  private void createSystemUsers() {
    for (Organization organization : orgDirectoryService.getOrganizations()) {
      JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

      // Create the digest auth users with clear text passwords

      // Role set for the system user
      Set<JaxbRole> roleList = new HashSet<JaxbRole>();
      for (String roleName : SecurityConstants.GLOBAL_SYSTEM_ROLES) {
        roleList.add(new JaxbRole(roleName, jaxbOrganization));
      }

      // Create the system user
      if (digestUsername != null && digestUserPass != null) {
        logger.info("Creating the system digest user '{}'", digestUsername);
        User digestUser = new JaxbUser(digestUsername, digestUserPass, DIGEST_USER_NAME, null, getName(), true,
                jaxbOrganization, roleList);
        inMemoryUsers.add(digestUser);
      }

      String caUsername = organization.getProperties().get(CAPTURE_AGENT_USER_KEY);
      String caUserPass = organization.getProperties().get(CAPTURE_AGENT_PASSWORD_KEY);
      if (caUsername != null && caUserPass != null) {
        // Role set for the capture agent user
        Set<JaxbRole> caRoleList = new HashSet<>();
        for (String roleName : SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLES) {
          caRoleList.add(new JaxbRole(roleName, jaxbOrganization));
        }

        // Add the organization anonymous role to the capture agent user
        caRoleList.add(new JaxbRole(organization.getAnonymousRole(), jaxbOrganization));

        String caExtraRoles = organization.getProperties().get(CAPTURE_AGENT_EXTRA_ROLES_KEY);
        // Add any extra custom roles to the CA user
        if (caExtraRoles != null) {
          List<String> items = Arrays.asList(caExtraRoles.split("\\s*,\\s*"));
          for (String item : items) {
            logger.debug("Adding custom role '{}' to capture agent user {}", item, caUsername);
            caRoleList.add(new JaxbRole(item, jaxbOrganization));
          }
        }

        // Create the capture agent user
        logger.info("Creating the capture agent digest user '{}'", caUsername);
        User caUser = new JaxbUser(caUsername, caUserPass, CAPTURE_AGENT_USER_NAME, null, getName(), true,
                jaxbOrganization, caRoleList);
        inMemoryUsers.add(caUser);
      }
    }
  }

  @Override
  public Iterator<User> getUsers() {
    return Stream.$(inMemoryUsers).filter(filterByCurrentOrg()).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    Stream<Role> roles = Stream.empty();
    for (User user : Stream.$(inMemoryUsers).filter(filterByCurrentOrg())) {
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
    for (User user : Stream.$(inMemoryUsers).filter(filterByCurrentOrg())) {
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
    for (User user : Stream.$(inMemoryUsers).filter(filterByCurrentOrg())) {
      if (like(user.getUsername(), query))
        users = users.append(Stream.single(user)).sort(userComparator);
    }
    return users.drop(offset).apply(limit > 0 ? StreamOp.<User> id().take(limit) : StreamOp.<User> id()).iterator();
  }

  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    // Find all roles from the role providers
    Stream<Role> roles = Stream.empty();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if ((like(role.getName(), query) || like(role.getDescription(), query))
          && !(target == Role.Target.ACL && GLOBAL_SUDO_ROLE.equals(role.getName())))
        roles = roles.append(Stream.single(role)).sort(roleComparator);
    }

    return roles.drop(offset).apply(limit > 0 ? StreamOp.<Role> id().take(limit) : StreamOp.<Role> id()).iterator();
  }

  private boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    if (null != string) {
      return p.matcher(string).matches();
    } else {
      return false;
    }
  }

  @Override
  public long countUsers() {
    return Stream.$(inMemoryUsers).filter(filterByCurrentOrg()).getSizeHint();
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

  private Fn<User, Boolean> filterByCurrentOrg() {
    return new Fn<User, Boolean>() {
      @Override
      public Boolean apply(User user) {
        return user.getOrganization().equals(securityService.getOrganization());
      }
    };
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  void setOrganizationDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
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
