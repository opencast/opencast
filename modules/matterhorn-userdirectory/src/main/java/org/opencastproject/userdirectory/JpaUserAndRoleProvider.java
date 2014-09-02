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
package org.opencastproject.userdirectory;

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PasswordEncoder;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;

/**
 * Manages and locates users using JPA.
 */
public class JpaUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserAndRoleProvider.class);

  /** Username constant used in JSON formatted users */
  public static final String USERNAME = "username";

  /** Role constant used in JSON formatted users */
  public static final String ROLES = "roles";

  /** Encoding expected from all inputs */
  public static final String ENCODING = "UTF-8";

  /** The delimiter for the User cache */
  private static final String DELIMITER = ";==;";

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** A cache of users, which lightens the load on the SQL server */
  private ConcurrentMap<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  protected Map<String, Object> persistenceProperties;

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
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
    cache = new MapMaker().expireAfterWrite(1, TimeUnit.MINUTES).makeComputingMap(new Function<String, Object>() {
      public Object apply(String id) {
        String[] key = id.split(DELIMITER);
        logger.trace("Loading user '{}':'{}' from database", key[0], key[1]);
        User user = loadUser(key[0], key[1]);
        return user == null ? nullToken : user;
      }
    });

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.userdirectory", persistenceProperties);
  }

  /**
   * Callback for deactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
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
    List<JpaUser> usersIterator = UserDirectoryPersistenceUtil.findUsersByQuery(orgId, query, limit, offset, emf);
    return new ArrayList<User>(usersIterator).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    String orgId = securityService.getOrganization().getId();
    List<JpaRole> rolesIterator = UserDirectoryPersistenceUtil.findRolesByQuery(orgId, query, limit, offset, emf);
    return new ArrayList<Role>(rolesIterator).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    String orgId = securityService.getOrganization().getId();
    Object user = cache.get(userName.concat(DELIMITER).concat(orgId));
    if (user == nullToken) {
      return null;
    } else {
      return (User) user;
    }
  }

  @Override
  public Iterator<User> getUsers() {
    String orgId = securityService.getOrganization().getId();
    List<JpaUser> usersIterator = UserDirectoryPersistenceUtil.findUsers(orgId, 0, 0, emf);
    return new ArrayList<User>(usersIterator).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    String orgId = securityService.getOrganization().getId();
    List<JpaRole> rolesIterator = UserDirectoryPersistenceUtil.findRoles(orgId, 0, 0, emf);
    return new ArrayList<Role>(rolesIterator).iterator();
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
    return UserDirectoryPersistenceUtil.findUser(userName, organization, emf);
  }

  /**
   * Adds a user to the persistence
   *
   * @param user
   *          the user to add
   */
  public void addUser(JpaUser user) {
    // Create a JPA user with an encoded password.
    String encodedPassword = PasswordEncoder.encode(user.getPassword(), user.getUsername());
    Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRoles(user.getRoles(), emf);
    JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganization(
            (JpaOrganization) user.getOrganization(), emf);
    user = new JpaUser(user.getUsername(), encodedPassword, organization, roles);

    // Then save the user
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      em.persist(user);
      tx.commit();
    } finally {
      cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user);
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Updates a user to the persistence
   * 
   * @param user
   *          the user to save
   * @throws NotFoundException
   */
  public User updateUser(JpaUser user) throws NotFoundException {
    if (UserDirectoryPersistenceUtil.findUser(user.getUsername(), user.getOrganization().getId(), emf) == null)
      throw new NotFoundException("User " + user.getUsername() + " not found.");

    // Update an JPA user with an encoded password.
    String encodedPassword = PasswordEncoder.encode(user.getPassword(), user.getUsername());
    Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRoles(user.getRoles(), emf);
    JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganization(
            (JpaOrganization) user.getOrganization(), emf);

    user = UserDirectoryPersistenceUtil.saveUser(new JpaUser(user.getUsername(), encodedPassword, organization, roles),
            emf);
    cache.put(user.getUsername() + DELIMITER + organization.getId(), user);

    return user;
  }

  /**
   * Delete the given user
   * 
   * @param username
   *          the name of the user to delete
   * @param orgId
   *          the organization id
   * @throws NotFoundException
   * @throws Exception
   */
  public void deleteUser(String username, String orgId) throws NotFoundException, Exception {
    UserDirectoryPersistenceUtil.deleteUser(username, orgId, emf);
    cache.remove(username + DELIMITER + orgId);
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
}
