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
package org.opencastproject.kernel.userdirectory;

import static org.opencastproject.security.api.UserProvider.ALL_ORGANIZATIONS;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.api.UserProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Federates user and role providers, and exposes a spring UserDetailsService so user lookups can be used by spring
 * security.
 */
public class UserAndRoleDirectoryServiceImpl implements UserDirectoryService, UserDetailsService, RoleDirectoryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserAndRoleDirectoryServiceImpl.class);

  /** A non-obvious password to allow a Spring User to be instantiated for CAS authenticated users having no password */
  private static final String DEFAULT_PASSWORD = "4b3e4b30-718c-11e2-bcfd-0800200c9a66";

  /** The list of user providers */
  protected List<UserProvider> userProviders = new ArrayList<UserProvider>();

  /** The list of role providers */
  protected List<RoleProvider> roleProviders = new ArrayList<RoleProvider>();

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * Adds a user provider.
   * 
   * @param userProvider
   *          the user provider to add
   */
  protected synchronized void addUserProvider(UserProvider userProvider) {
    logger.debug("Adding {} to the list of user providers", userProvider);
    userProviders.add(userProvider);
  }

  /**
   * Remove a user provider.
   * 
   * @param userProvider
   *          the user provider to remove
   */
  protected synchronized void removeUserProvider(UserProvider userProvider) {
    logger.debug("Removing {} from the list of user providers", userProvider);
    roleProviders.remove(userProvider);
  }

  /**
   * Adds a role provider.
   * 
   * @param roleProvider
   *          the role provider to add
   */
  protected synchronized void addRoleProvider(RoleProvider roleProvider) {
    logger.debug("Adding {} to the list of role providers", roleProvider);
    roleProviders.add(roleProvider);
  }

  /**
   * Remove a role provider.
   * 
   * @param roleProvider
   *          the role provider to remove
   */
  protected synchronized void removeRoleProvider(RoleProvider roleProvider) {
    logger.debug("Removing {} from the list of role providers", roleProvider);
    roleProviders.remove(roleProvider);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  public String[] getRoles() {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }
    SortedSet<String> roles = new TreeSet<String>();
    for (RoleProvider roleProvider : roleProviders) {
      for (String role : roleProvider.getRoles()) {
        String currentOrgId = org.getId();
        String providerOrgId = roleProvider.getOrganization();
        if (!currentOrgId.equals(providerOrgId) && !ALL_ORGANIZATIONS.equals(providerOrgId)) {
          continue;
        }
        roles.add(role);
      }
    }
    return roles.toArray(new String[roles.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserDirectoryService#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) throws IllegalStateException {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }
    String orgId = org.getId();
    // Collect all of the roles known from each of the user providers for this user
    User user = null;
    for (UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !orgId.equals(providerOrgId)) {
        continue;
      }
      User providerUser = userProvider.loadUser(userName);
      if (providerUser == null) {
        continue;
      }
      if (user == null) {
        user = providerUser;
      } else {
        user = mergeUsers(user, providerUser);
      }
    }
    return user;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException,
          org.springframework.dao.DataAccessException {
    User user = loadUser(userName);
    if (user == null) {
      throw new UsernameNotFoundException(userName);
    } else {
      Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
      for (String role : user.getRoles()) {
        authorities.add(new SimpleGrantedAuthority(role));
      }

      // Add additional roles from role providers
      for (RoleProvider roleProvider : roleProviders) {
        String[] rolesForUser = roleProvider.getRolesForUser(userName);
        for (String role : rolesForUser)
          authorities.add(new SimpleGrantedAuthority(role));
      }

      authorities.add(new SimpleGrantedAuthority(securityService.getOrganization().getAnonymousRole()));
      // need a non null password to instantiate org.springframework.security.core.userdetails.User
      // but CAS authenticated users have no password
      String password = user.getPassword() == null ? DEFAULT_PASSWORD : user.getPassword();
      return new org.springframework.security.core.userdetails.User(user.getUserName(), password, true, true, true,
          true, authorities);
    }
  }

  /**
   * Merges two representations of a user, as returned by two different user providers. The set or roles from the
   * provided users will be merged into one set.
   * 
   * @param user1
   *          the first user to merge
   * @param user2
   *          the second user to merge
   * @return a user with a merged set of roles
   */
  protected User mergeUsers(User user1, User user2) {
    String[] roles1 = user1.getRoles();
    String[] roles2 = user2.getRoles();

    String[] roles = new String[(roles1.length + roles2.length)];
    for (int i = 0; i < roles.length; i++) {
      roles[i] = i < roles1.length ? roles1[i] : roles2[i - roles1.length];
    }
    String userName = user1.getUserName();
    String organization = user1.getOrganization();
    String password = user1.getPassword() == null ? user2.getPassword() : user1.getPassword();
    return new User(userName, password, organization, roles);
  }

  /**
   * Sets the security service
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
