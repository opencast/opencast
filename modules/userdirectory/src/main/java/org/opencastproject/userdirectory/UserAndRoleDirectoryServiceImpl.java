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
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.security.api.GroupProvider;
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
import org.opencastproject.util.data.Tuple;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Federates user and role providers, and exposes a spring UserDetailsService so user lookups can be used by spring
 * security.
 */
public class UserAndRoleDirectoryServiceImpl implements UserDirectoryService, UserDetailsService, RoleDirectoryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserAndRoleDirectoryServiceImpl.class);

  /** A non-obvious password to allow a Spring User to be instantiated for CAS authenticated users having no password */
  private static final String DEFAULT_PASSWORD = "4b3e4b30-718c-11e2-bcfd-0800200c9a66";

  /** The configuration property for the user cache size */
  public static final String USER_CACHE_SIZE_KEY = "org.opencastproject.userdirectory.cache.size";

  /** The configuration property for the user cache expiry time */
  public static final String USER_CACHE_EXPIRY_KEY = "org.opencastproject.userdirectory.cache.expiry";

  /** The list of user providers */
  protected List<UserProvider> userProviders = new CopyOnWriteArrayList<>();

  /** The list of role providers */
  protected List<RoleProvider> roleProviders = new CopyOnWriteArrayList<>();

  /** The security service */
  protected SecurityService securityService = null;

  /** A token to store in the miss cache */
  private Object nullToken = new Object();

  private final CacheLoader<Tuple<String, String>, Object> userLoader = new CacheLoader<Tuple<String, String>, Object>() {
    @Override
    public Object load(Tuple<String, String> orgUser) {
      final User user = loadUser(orgUser);
      return user == null ? nullToken : user;
    }
  };

  /** The user cache */
  private LoadingCache<Tuple<String, String>, Object> cache;

  /** Size of the user cache */
  private int cacheSize = 200;

  /** Expiry time for elements in the user cache */
  private int cacheExpiryTimeInMinutes = 1;

  /**
   * Callback to activate the component.
   *
   * @param cc
   *          the declarative services component context
   */
  protected void activate(ComponentContext cc) {

    if (cc != null) {
      String stringValue = cc.getBundleContext().getProperty(USER_CACHE_SIZE_KEY);
      if (StringUtils.isNotEmpty(stringValue)) {
        try {
          cacheSize = Integer.parseInt(StringUtils.trimToNull(stringValue));
        } catch (Exception e) {
          logger.warn("Ignoring invalid value {} for user cache size", stringValue);
        }
      } else {
        logger.info("Using default value {} for user cache size", cacheSize);
      }

      stringValue = cc.getBundleContext().getProperty(USER_CACHE_EXPIRY_KEY);
      if (StringUtils.isNotBlank(stringValue)) {
        try {
          cacheExpiryTimeInMinutes = Integer.parseInt(StringUtils.trimToNull(stringValue));
        } catch (Exception e) {
          logger.warn("Ignoring invalid value {} for user cache expiry time", stringValue);
        }
      } else {
        logger.info("Using default value {} for user cache expiry time", cacheExpiryTimeInMinutes);
      }
    }

    // Create the user cache
    cache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpiryTimeInMinutes, TimeUnit.MINUTES).maximumSize(cacheSize).build(userLoader);

    logger.info("Activated UserAndRoleDirectoryService with user cache of size {}, expiry time {} minutes",
      cacheSize, cacheExpiryTimeInMinutes);

  }

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
    userProviders.remove(userProvider);
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
  public Iterator<User> getUsers() {
    final Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    // Get all users from the user providers
    final List<User> users = new ArrayList<>();
    for (final UserProvider userProvider : userProviders) {
      final String providerOrgId = userProvider.getOrganization();
      if (ALL_ORGANIZATIONS.equals(providerOrgId) || org.getId().equals(providerOrgId)) {
        userProvider.getUsers().forEachRemaining(users::add);
      }
    }
    return users.stream().sorted(Comparator.comparing(User::getUsername)).iterator();
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

  @Override
  public Iterator<User> loadUsers(Collection<String> userNames) {
    Organization org = securityService.getOrganization();
    Map<String, User> result = new HashMap<>(userNames.size());
    Set<String> remainingNames = new HashSet<>(userNames);
    for (UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId)) {
        continue;
      }
      for (Iterator<User> it = userProvider.findUsers(remainingNames); it.hasNext();) {
        User user = it.next();
        User priorUser = result.get(user.getUsername());
        if (priorUser != null) {
          result.put(user.getUsername(), mergeUsers(priorUser, user));
        } else {
          result.put(user.getUsername(), user);
        }
        // Return super users without merging to avoid unnecessary requests to other user providers
        if (InMemoryUserAndRoleProvider.PROVIDER_NAME.equals(userProvider.getName())) {
          remainingNames.remove(user.getUsername());
        }
      }
    }
    return result.values().iterator();
  }

  /** Load a user of an organization. */
  private User loadUser(Tuple<String, String> orgUser) {
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
    Set<JaxbRole> roles = new HashSet<>();
    for (Role role : user.getRoles()) {
      roles.add(JaxbRole.fromRole(role));
    }

    // Consult roleProviders if this is not an internal system user
    if (!InMemoryUserAndRoleProvider.PROVIDER_NAME.equals(user.getProvider())) {
      for (RoleProvider roleProvider : roleProviders) {
        for (Role role : roleProvider.getRolesForUser(user.getUsername())) {
          roles.add(JaxbRole.fromRole(role));
        }
      }
    }

    // Resolve any transitive roles granted via group membership
    Set<JaxbRole> derivedRoles = new HashSet<>();
    for (Role role : roles) {
      if (Role.Type.EXTERNAL_GROUP.equals(role.getType())) {
        // Load roles granted to this group
        logger.debug("Resolving transitive roles for user {} from external group {}", user.getUsername(), role.getName());
        for (RoleProvider roleProvider : roleProviders) {
          if (roleProvider instanceof GroupProvider) {
            List<Role> groupRoles = ((GroupProvider) roleProvider).getRolesForGroup(role.getName());
            if (groupRoles != null) {
              for (Role groupRole : groupRoles) {
                derivedRoles.add(JaxbRole.fromRole(groupRole));
              }
              logger.debug("Adding {} derived role(s) for user {} from internal group {}", derivedRoles.size(), user.getUsername(), role.getName());
            } else {
              logger.warn("Cannot resolve externallly provided group reference for user {} to internal group {}", user.getUsername(), role.getName());
            }
          }
        }
      }
    }
    roles.addAll(derivedRoles);

    // Create and return the final user
    JaxbUser mergedUser = new JaxbUser(user.getUsername(), user.getPassword(), user.getName(), user.getEmail(),
            user.getProvider(), JaxbOrganization.fromOrganization(user.getOrganization()), roles);
    mergedUser.setManageable(user.isManageable());
    return mergedUser;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  @Override
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
    User user = loadUser(userName);
    if (user == null)
      throw new UsernameNotFoundException(userName);

    // Store the user in the security service
    securityService.setUser(user);

    Set<GrantedAuthority> authorities = new HashSet<>();
    for (Role role : user.getRoles()) {
      authorities.add(new SimpleGrantedAuthority(role.getName()));
    }

    // Add additional roles from role providers
    if (!InMemoryUserAndRoleProvider.PROVIDER_NAME.equals(user.getProvider())) {
      for (RoleProvider roleProvider : roleProviders) {
        List<Role> rolesForUser = roleProvider.getRolesForUser(userName);
        for (Role role : rolesForUser)
          authorities.add(new SimpleGrantedAuthority(role.getName()));
      }
    }

    authorities.add(new SimpleGrantedAuthority(securityService.getOrganization().getAnonymousRole()));
    // need a non null password to instantiate org.springframework.security.core.userdetails.User
    // but CAS authenticated users have no password
    String password = user.getPassword() == null ? DEFAULT_PASSWORD : user.getPassword();
    return new org.springframework.security.core.userdetails.User(user.getUsername(), password, true, true,
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
  private User mergeUsers(User user1, User user2) {
    final Set<JaxbRole> mergedRoles = Stream.of(user1, user2)
            .flatMap((u) -> u.getRoles().stream())
            .map(JaxbRole::fromRole)
            .collect(Collectors.toSet());

    final String name = StringUtils.defaultIfBlank(user1.getName(), user2.getName());
    final String email = StringUtils.defaultIfBlank(user1.getEmail(), user2.getEmail());
    final String password = StringUtils.defaultString(user1.getPassword(), user2.getPassword());
    final boolean manageable = user1.isManageable() || user2.isManageable();

    final JaxbOrganization organization = JaxbOrganization.fromOrganization(user1.getOrganization());
    final String provider = StringUtils.join(Collections.nonNullList(user1.getProvider(), user2.getProvider()), ",");

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
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    // Find all users from the user providers
    final List<User> users = new ArrayList<>();
    for (final UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (ALL_ORGANIZATIONS.equals(providerOrgId) || org.getId().equals(providerOrgId)) {
        userProvider.findUsers(query, 0, 0).forEachRemaining(users::add);
      }
    }
    Stream<User> stream = users.stream().sorted(Comparator.comparing(User::getUsername)).skip(offset);
    if (limit > 0) {
      return stream.limit(limit).iterator();
    }
    return stream.iterator();
  }

  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    // Find all roles from the role providers
    final List<Role> roles = new ArrayList<>();
    for (RoleProvider roleProvider : roleProviders) {
      final String providerOrgId = roleProvider.getOrganization();
      if (ALL_ORGANIZATIONS.equals(providerOrgId) || org.getId().equals(providerOrgId)) {
        roleProvider.findRoles(query, target, 0, 0).forEachRemaining(roles::add);
      }
    }
    Stream<Role> stream = roles.stream().sorted(Comparator.comparing(Role::getName)).skip(offset);
    if (limit > 0) {
      return stream.limit(limit).iterator();
    }
    return stream.iterator();
  }

  @Override
  public long countUsers() {
    return userProviders.stream().mapToLong(UserProvider::countUsers).sum();
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

}
