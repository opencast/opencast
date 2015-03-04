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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * Manages and locates users references using JPA.
 */
public class JpaUserReferenceProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserReferenceProvider.class);

  public static final String PROVIDER_NAME = "matterhorn-reference";

  /** Username constant used in JSON formatted users */
  public static final String USERNAME = "username";

  /** Role constant used in JSON formatted users */
  public static final String ROLES = "roles";

  /** Encoding expected from all inputs */
  public static final String ENCODING = "UTF-8";

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider = null;

  /** The security service */
  protected SecurityService securityService = null;

  protected Map<String, Object> persistenceProperties;

  /** The delimiter for the User cache */
  private static final String DELIMITER = ";==;";

  /** A cache of users, which lightens the load on the SQL server */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected final Object nullToken = new Object();

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

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
    cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, Object>() {
      @Override
      public Object load(String id) {
        String[] key = id.split(DELIMITER);
        logger.trace("Loading user '{}':'{}' from reference database", key[0], key[1]);
        User user = loadUser(key[0], key[1]);
        return user == null ? nullToken : user;
      }
    });

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.userdirectory", persistenceProperties);
  }

  /**
   * Callback for inactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
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
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    ArrayList<Role> roles = new ArrayList<Role>();
    User user = loadUser(userName);
    if (user != null)
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
    List<User> users = new ArrayList<User>();
    for (JpaUserReference userRef : findUserReferencesByQuery(orgId, query, limit, offset, emf)) {
      users.add(userRef.toUser());
    }
    return users.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    // The roles are returned from the JpaUserAndRoleProvider
    return Collections.<Role> emptyList().iterator();
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

  /**
   * Loads a user from persistence
   *
   * @param userName
   *          the user name
   * @param organization
   *          the organization id
   * @return the loaded user or <code>null</code> if not found
   */
  private User loadUser(String userName, String organization) {
    JpaUserReference userReference = findUserReference(userName, organization, emf);
    if (userReference != null)
      return userReference.toUser();
    return null;
  }

  @Override
  public Iterator<User> getUsers() {
    String orgId = securityService.getOrganization().getId();
    List<User> users = new ArrayList<User>();
    for (JpaUserReference userRef : findUserReferences(orgId, 0, 0, emf)) {
      users.add(userRef.toUser());
    }
    return users.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    // The roles are returned from the JpaUserAndRoleProvider
    return Collections.<Role> emptyList().iterator();
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
   * @see org.opencastproject.security.api.UserReferenceProvider#addUserReference(org.opencastproject.security.api.User,
   *      String)
   */
  public void addUserReference(JpaUserReference user, String mechanism) {
    // Create a JPA user with an encoded password.
    Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRoles(user.getRoles(), emf);
    JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganization(
            (JpaOrganization) user.getOrganization(), emf);
    JpaUserReference userReference = new JpaUserReference(user.getUsername(), user.getName(), user.getEmail(),
            mechanism, new Date(), organization, roles);

    // Then save the user reference
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaUserReference foundUserRef = findUserReference(user.getUsername(), user.getOrganization().getId(), emf);
      if (foundUserRef == null) {
        em.persist(userReference);
      } else {
        throw new IllegalStateException("User '" + user.getUsername() + "' already exists");
      }
      tx.commit();
      cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user.toUser());
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * @see org.opencastproject.security.api.UserReferenceProvider#updateUserReference(org.opencastproject.security.api.User)
   */
  public void updateUserReference(JpaUserReference user) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaUserReference foundUserRef = findUserReference(user.getUsername(), user.getOrganization().getId(), emf);
      if (foundUserRef == null) {
        throw new IllegalStateException("User '" + user.getUsername() + "' does not exist");
      } else {
        foundUserRef.setName(user.getName());
        foundUserRef.setEmail(user.getEmail());
        foundUserRef.setLastLogin(new Date());
        foundUserRef.setRoles(UserDirectoryPersistenceUtil.saveRoles(user.getRoles(), emf));
        em.merge(foundUserRef);
      }
      tx.commit();
      cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user.toUser());
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted user reference by the user name and organization id
   *
   * @param userName
   *          the user name
   * @param organizationId
   *          the organization id
   * @return the user or <code>null</code> if not found
   */
  public JpaUserReference findUserReference(String userName, String organizationId) {
    return findUserReference(userName, organizationId, emf);
  }

  /**
   * Returns the persisted user reference by the user name and organization id
   *
   * @param userName
   *          the user name
   * @param organizationId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the user or <code>null</code> if not found
   */
  private JpaUserReference findUserReference(String userName, String organizationId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("UserReference.findByUsername");
      q.setParameter("u", userName);
      q.setParameter("org", organizationId);
      return (JpaUserReference) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of user references by a search query if set or all user references if search query is
   * <code>null</code>
   *
   * @param orgId
   *          the organization identifier
   * @param query
   *          the query to search
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the user references list
   */
  @SuppressWarnings("unchecked")
  private List<JpaUserReference> findUserReferencesByQuery(String orgId, String query, int limit, int offset,
          EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("UserReference.findByQuery").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all user references
   *
   * @param orgId
   *          the organization identifier
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the user references list
   */
  @SuppressWarnings("unchecked")
  private List<JpaUserReference> findUserReferences(String orgId, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("UserReference.findAll").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void invalidate(String userName) {
    String orgId = securityService.getOrganization().getId();
    cache.invalidate(userName.concat(DELIMITER).concat(orgId));
  }

}
