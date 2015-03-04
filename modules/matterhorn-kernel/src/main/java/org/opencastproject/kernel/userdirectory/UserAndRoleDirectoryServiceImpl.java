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
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamOp;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

  /** A token to store in the miss cache */
  private Object nullToken = new Object();

  private final CacheLoader<Tuple<String, String>, Object> userLoader = new CacheLoader<Tuple<String, String>, Object>() {
    @Override
    public Object load(Tuple<String, String> orgUser) {
      User user = loadUser.apply(orgUser);
      return user == null ? nullToken : user;
    }
  };

  private final LoadingCache<Tuple<String, String>, Object> cache = CacheBuilder.newBuilder()
          .expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(200).build(userLoader);

  /**
   * Adds a user provider.
   *
   * @param userProvider
   *          the user provider to add
   */
  protected synchronized void addUserProvider(UserProvider userProvider) {
    logger.debug("Adding {} to the list of user providers", userProvider);
    if (InMemoryUserAndRoleProvider.PROVIDER_NAME.equals(userProvider.getName())) {
      userProviders.add(0, userProvider);
    } else {
      userProviders.add(userProvider);
    }
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
   * @see org.opencastproject.security.api.UserDirectoryService#getUsers()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Iterator<User> getUsers() {
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    // Find all users from the user providers
    Stream<User> users = Stream.empty();
    for (final UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      users = users.append(IteratorUtils.toList(userProvider.getUsers())).sort(userComparator);
    }
    return users.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Iterator<Role> getRoles() {
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    Stream<Role> roles = Stream.empty();
    for (RoleProvider roleProvider : roleProviders) {
      String providerOrgId = roleProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      roles = roles.append(IteratorUtils.toList(roleProvider.getRoles())).sort(roleComparator);
    }
    return roles.iterator();
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

    Object user = cache.getUnchecked(tuple(org.getId(), userName));
    if (user == nullToken) {
      cache.invalidate(tuple(org.getId(), userName));
      return null;
    } else {
      return (User) user;
    }
  }

  /** Load a user of an organization. */
  private final Function<Tuple<String, String>, User> loadUser = new Function<Tuple<String, String>, User>() {
    @Override
    public User apply(Tuple<String, String> orgUser) {
      // Collect all of the roles known from each of the user providers for this user
      User user = null;
      for (UserProvider userProvider : userProviders) {
        String providerOrgId = userProvider.getOrganization();
        if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !orgUser.getA().equals(providerOrgId)) {
          continue;
        }
        User providerUser = userProvider.loadUser(orgUser.getB());
        if (providerUser == null) {
          continue;
        }

        User tmpUser = JaxbUser.fromUser(providerUser);
        if (user == null) {
          user = tmpUser;
        } else {
          user = mergeUsers(user, tmpUser);
        }

        // Return super users without merging to avoid unnecessary requests to other user providers
        if (InMemoryUserAndRoleProvider.PROVIDER_NAME.equals(userProvider.getName())) {
          user = tmpUser;
          break;
        }
      }

      if (user == null)
        return null;

      // Add additional roles from role providers
      Set<JaxbRole> roles = new HashSet<JaxbRole>();
      for (Role role : user.getRoles()) {
        roles.add(JaxbRole.fromRole(role));
      }

      for (RoleProvider roleProvider : roleProviders) {
        for (Role role : roleProvider.getRolesForUser(user.getUsername())) {
          roles.add(JaxbRole.fromRole(role));
        }
      }

      // Create and return the final user
      JaxbUser mergedUser = new JaxbUser(user.getUsername(), user.getPassword(), user.getName(), user.getEmail(),
              user.getProvider(), user.canLogin(), JaxbOrganization.fromOrganization(user.getOrganization()), roles);
      mergedUser.setManageable(user.isManageable());
      return mergedUser;
    }
  };

  /**
   * {@inheritDoc}
   *
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  @Override
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException,
          org.springframework.dao.DataAccessException {
    User user = loadUser(userName);
    if (user == null)
      throw new UsernameNotFoundException(userName);

    // Store the user in the security service
    securityService.setUser(user);

    Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
    for (Role role : user.getRoles()) {
      authorities.add(new SimpleGrantedAuthority(role.getName()));
    }

    // Add additional roles from role providers
    for (RoleProvider roleProvider : roleProviders) {
      List<Role> rolesForUser = roleProvider.getRolesForUser(userName);
      for (Role role : rolesForUser)
        authorities.add(new SimpleGrantedAuthority(role.getName()));
    }

    authorities.add(new SimpleGrantedAuthority(securityService.getOrganization().getAnonymousRole()));
    // need a non null password to instantiate org.springframework.security.core.userdetails.User
    // but CAS authenticated users have no password
    String password = user.getPassword() == null ? DEFAULT_PASSWORD : user.getPassword();
    return new org.springframework.security.core.userdetails.User(user.getUsername(), password, user.canLogin(), true,
            true, true, authorities);

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
    HashSet<JaxbRole> mergedRoles = new HashSet<JaxbRole>();
    for (Role role : user1.getRoles()) {
      mergedRoles.add(JaxbRole.fromRole(role));
    }
    for (Role role : user2.getRoles()) {
      mergedRoles.add(JaxbRole.fromRole(role));
    }

    String name = StringUtils.isNotBlank(user1.getName()) ? user1.getName() : user2.getName();
    String email = StringUtils.isNotBlank(user1.getEmail()) ? user1.getEmail() : user2.getEmail();
    String password = user1.getPassword() == null ? user2.getPassword() : user1.getPassword();
    boolean manageable = user1.isManageable() || user2.isManageable() ? true : false;

    JaxbOrganization organization = JaxbOrganization.fromOrganization(user1.getOrganization());
    String provider = StringUtils.join(Collections.list(user1.getProvider(), user2.getProvider()), ",");

    JaxbUser jaxbUser = new JaxbUser(user1.getUsername(), password, name, email, provider, organization, mergedRoles);
    jaxbUser.setManageable(manageable);
    return jaxbUser;
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

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    // Find all users from the user providers
    Stream<User> users = Stream.empty();
    for (final UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      users = users.append(IteratorUtils.toList(userProvider.findUsers(query, 0, 0))).sort(userComparator);
    }
    return users.drop(offset).apply(limit > 0 ? StreamOp.<User>id().take(limit) : StreamOp.<User>id()).iterator();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    // Find all roles from the role providers
    Stream<Role> roles = Stream.empty();
    for (RoleProvider roleProvider : roleProviders) {
      String providerOrgId = roleProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      roles = roles.append(IteratorUtils.toList(roleProvider.findRoles(query, 0, 0))).sort(roleComparator);
    }
    return roles.drop(offset).apply(limit > 0 ? StreamOp.<Role>id().take(limit) : StreamOp.<Role>id()).iterator();
  }

  @Override
  public void invalidate(String userName) {
    for (UserProvider userProvider : userProviders) {
      userProvider.invalidate(userName);
    }

    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    cache.invalidate(tuple(org.getId(), userName));
    logger.trace("Invalidated user {} from user directories", userName);
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

}
