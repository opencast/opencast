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

import org.opencastproject.kernel.security.CustomPasswordEncoder;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.userdirectory.utils.UserDirectoryUtils;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

/**
 * Manages and locates users using JPA.
 */
public class JpaUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserAndRoleProvider.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** The user provider name */
  public static final String PROVIDER_NAME = "opencast";

  /** Username constant used in JSON formatted users */
  public static final String USERNAME = "username";

  /** Role constant used in JSON formatted users */
  public static final String ROLES = "roles";

  /** Encoding expected from all inputs */
  public static final String ENCODING = "UTF-8";

  /** The delimiter for the User cache */
  private static final String DELIMITER = ";==;";

  /** The security service */
  protected SecurityService securityService = null;

  /** Group provider */
  protected JpaGroupRoleProvider groupRoleProvider;

  /** A cache of users, which lightens the load on the SQL server */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** Password encoder for storing user passwords */
  private CustomPasswordEncoder passwordEncoder = new CustomPasswordEncoder();

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param groupRoleProvider
   *          the groupRoleProvider to set
   */
  void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("activate");

    // Setup the caches
    cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, Object>() {
      @Override
      public Object load(String id) {
        String[] key = id.split(DELIMITER);
        logger.trace("Loading user '{}':'{}' from database", key[0], key[1]);
        User user = loadUser(key[0], key[1]);
        return user == null ? nullToken : user;
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    ArrayList<Role> roles = new ArrayList<Role>();
    User user = loadUser(userName);
    if (user == null)
      return roles;
    roles.addAll(user.getRoles());
    return roles;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#findUsers(String, int, int)
   */
  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    String orgId = securityService.getOrganization().getId();
    List<JpaUser> users = UserDirectoryPersistenceUtil.findUsersByQuery(orgId, query, limit, offset, emf);
    return Monadics.mlist(users).map(addProviderName).iterator();
  }

  @Override
  public Iterator<User> findUsers(Collection<String> userNames) {
    String orgId = securityService.getOrganization().getId();
    List<JpaUser> users = UserDirectoryPersistenceUtil.findUsersByUserName(userNames, orgId, emf);
    return Monadics.mlist(users).map(addProviderName).iterator();
  }

  /**
   * List all users with insecure password hashes
   */
  public List<User> findInsecurePasswordHashes() {
    final String orgId = securityService.getOrganization().getId();
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      TypedQuery<User> q = em.createNamedQuery("User.findInsecureHash", User.class);
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    // This provider persists roles but is not authoritative for any roles, so return an empty set
    return new ArrayList<Role>().iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    String orgId = securityService.getOrganization().getId();
    Object user = cache.getUnchecked(userName.concat(DELIMITER).concat(orgId));
    if (user == nullToken) {
      return null;
    } else {
      return (User) user;
    }
  }

  @Override
  public Iterator<User> getUsers() {
    String orgId = securityService.getOrganization().getId();
    List<JpaUser> users = UserDirectoryPersistenceUtil.findUsers(orgId, 0, 0, emf);
    return Monadics.mlist(users).map(addProviderName).iterator();
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
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  /**
   * Loads a user from persistence
   *
   * @param userName
   *          the user name
   * @param organization
   *          the organization id
   * @return the loaded user or <code>null</code> if not found
   */
  public User loadUser(String userName, String organization) {
    JpaUser user = UserDirectoryPersistenceUtil.findUser(userName, organization, emf);
    return Option.option(user).map(addProviderName).getOrElseNull();
  }

  /**
   * Loads a user from persistence
   *
   * @param userId
   *          the user's id
   * @param organization
   *          the organization id
   * @return the loaded user or <code>null</code> if not found
   */
  public User loadUser(long userId, String organization) {
    JpaUser user = UserDirectoryPersistenceUtil.findUser(userId, organization, emf);
    return Option.option(user).map(addProviderName).getOrElseNull();
  }

  /**
   * Adds a user to the persistence
   *
   * @param user
   *          the user to add
   *
   * @throws org.opencastproject.security.api.UnauthorizedException
   *          if the user is not allowed to create other user with the given roles
   */
  public void addUser(JpaUser user) throws UnauthorizedException {
    addUser(user, false);
  }

  /**
   * Adds a user to the persistence
   *
   * @param user
   *          the user to add
   * @param passwordEncoded
   *          if the password is already encoded or should be encoded
   *
   * @throws org.opencastproject.security.api.UnauthorizedException
   *          if the user is not allowed to create other user with the given roles
   */
  public void addUser(JpaUser user, final boolean passwordEncoded) throws UnauthorizedException {
    if (!UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, user.getRoles()))
      throw new UnauthorizedException("The user is not allowed to set the admin role on other users");

    // Create a JPA user with an encoded password.
    String encodedPassword = passwordEncoded ? user.getPassword() : passwordEncoder.encodePassword(user.getPassword());

    // Only save internal roles
    Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRoles(filterRoles(user.getRoles()), emf);
    JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganization(
            (JpaOrganization) user.getOrganization(), emf);

    JpaUser newUser = new JpaUser(user.getUsername(), encodedPassword, organization, user.getName(), user.getEmail(),
            user.getProvider(), user.isManageable(), roles);

    // Then save the user
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      em.persist(newUser);
      tx.commit();
      cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), newUser);
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }

    updateGroupMembership(user);

  }

  /**
   * Updates a user to the persistence
   *
   * @param user
   *          the user to save
   * @throws NotFoundException
   * @throws org.opencastproject.security.api.UnauthorizedException
   *          if the current user is not allowed to update user with the given roles
   */
  public User updateUser(JpaUser user) throws NotFoundException, UnauthorizedException {
    return updateUser(user, false);
  }

  /**
   * Updates a user to the persistence
   *
   * @param user
   *          the user to save
   * @param passwordEncoded
   *          if the password is already encoded or should be encoded
   * @throws NotFoundException
   * @throws org.opencastproject.security.api.UnauthorizedException
   *          if the current user is not allowed to update user with the given roles
   */
  public User updateUser(JpaUser user, final boolean passwordEncoded) throws NotFoundException, UnauthorizedException {
    if (!UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, user.getRoles()))
      throw new UnauthorizedException("The user is not allowed to set the admin role on other users");

    JpaUser updateUser = UserDirectoryPersistenceUtil.findUser(user.getUsername(), user.getOrganization().getId(), emf);
    if (updateUser == null) {
      throw new NotFoundException("User " + user.getUsername() + " not found.");
    }

    logger.debug("updateUser({})", user.getUsername());

    if (!UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, updateUser.getRoles()))
      throw new UnauthorizedException("The user is not allowed to update an admin user");

    String encodedPassword;
    //only update Password if a value is set
    if (StringUtils.isEmpty(user.getPassword())) {
      encodedPassword = updateUser.getPassword();
    } else  {
      // Update an JPA user with an encoded password.
      if (passwordEncoded) {
        encodedPassword = user.getPassword();
      } else {
        encodedPassword = passwordEncoder.encodePassword(user.getPassword());
      }
    }

    // Only save internal roles
    Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRoles(filterRoles(user.getRoles()), emf);
    JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganization(
            (JpaOrganization) user.getOrganization(), emf);

    JpaUser updatedUser = UserDirectoryPersistenceUtil.saveUser(
            new JpaUser(user.getUsername(), encodedPassword, organization, user.getName(), user.getEmail(), user
                    .getProvider(), true, roles), emf);
    cache.put(user.getUsername() + DELIMITER + organization.getId(), updatedUser);

    updateGroupMembership(user);

    return updatedUser;
  }

  /**
   * Select only internal roles
   *
   * @param userRoles
   *          the user's full set of roles
   */
  private Set<JpaRole> filterRoles(Set<Role> userRoles) {
    Set<JpaRole> roles = new HashSet<JpaRole>();
    for (Role role : userRoles) {
      if (Role.Type.INTERNAL.equals(role.getType()) && !role.getName().startsWith(Group.ROLE_PREFIX)) {
        JpaRole jpaRole = (JpaRole) role;
        roles.add(jpaRole);
      }
    }
    return roles;
  }

  /**
   * Updates a user's groups based on assigned roles
   *
   * @param user
   *          the user for whom groups should be updated
   * @throws NotFoundException
   */
  private void updateGroupMembership(JpaUser user) {

    logger.debug("updateGroupMembership({}, roles={})", user.getUsername(), user.getRoles().size());

    List<String> internalGroupRoles = new ArrayList<String>();

    for (Role role : user.getRoles()) {
      if (Role.Type.GROUP.equals(role.getType())
          || (Role.Type.INTERNAL.equals(role.getType()) && role.getName().startsWith(Group.ROLE_PREFIX))) {
        internalGroupRoles.add(role.getName());
      }
    }

    groupRoleProvider.updateGroupMembershipFromRoles(user.getUsername(), user.getOrganization().getId(), internalGroupRoles);

  }

  /**
   * Delete the given user
   *
   * @param username
   *          the name of the user to delete
   * @param orgId
   *          the organization id
   * @throws NotFoundException
   *          if the requested user is not exist
   * @throws org.opencastproject.security.api.UnauthorizedException
   *          if you havn't permissions to delete an admin user (only admins may do that)
   * @throws Exception
   */
  public void deleteUser(String username, String orgId) throws NotFoundException, UnauthorizedException, Exception {
    User user = loadUser(username, orgId);
    if (user != null && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, user.getRoles()))
      throw new UnauthorizedException("The user is not allowed to delete an admin user");

    // Remove the user's group membership
    groupRoleProvider.updateGroupMembershipFromRoles(username, orgId, new ArrayList<String>());

    // Remove the user
    UserDirectoryPersistenceUtil.deleteUser(username, orgId, emf);

    cache.invalidate(username + DELIMITER + orgId);
  }

  /**
   * Adds a role to the persistence
   *
   * @param jpaRole
   *          the role
   */
  public void addRole(JpaRole jpaRole) {
    HashSet<JpaRole> roles = new HashSet<JpaRole>();
    roles.add(jpaRole);
    UserDirectoryPersistenceUtil.saveRoles(roles, emf);
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  private static org.opencastproject.util.data.Function<JpaUser, User> addProviderName = new org.opencastproject.util.data.Function<JpaUser, User>() {
    @Override
    public User apply(JpaUser a) {
      a.setProvider(PROVIDER_NAME);
      return a;
    }
  };

  @Override
  public long countUsers() {
    String orgId = securityService.getOrganization().getId();
    return UserDirectoryPersistenceUtil.countUsers(orgId, emf);
  }

  @Override
  public void invalidate(String userName) {
    String orgId = securityService.getOrganization().getId();
    cache.invalidate(userName + DELIMITER + orgId);
  }
}
