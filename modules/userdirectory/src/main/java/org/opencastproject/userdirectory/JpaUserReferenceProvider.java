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

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUserReference;
import org.opencastproject.userdirectory.api.AAIRoleProvider;
import org.opencastproject.userdirectory.api.UserReferenceProvider;
import org.opencastproject.util.NotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Manages and locates users references using JPA.
 */
@Component(
  property = {
    "service.description=Provides a user reference directory"
  },
  immediate = true,
  service = { UserProvider.class, RoleProvider.class, UserReferenceProvider.class }
)
public class JpaUserReferenceProvider implements UserReferenceProvider, UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserReferenceProvider.class);

  public static final String PROVIDER_NAME = "matterhorn-reference";

  /** Username constant used in JSON formatted users */
  public static final String USERNAME = "username";

  /** Role constant used in JSON formatted users */
  public static final String ROLES = "roles";

  /** Encoding expected from all inputs */
  public static final String ENCODING = "UTF-8";

  /** The security service */
  protected SecurityService securityService = null;

  /** Group Role provider */
  protected JpaGroupRoleProvider groupRoleProvider;

  /** Role provider */
  protected AAIRoleProvider roleProvider;

  /** The delimiter for the User cache */
  private static final String DELIMITER = ";==;";

  /** A cache of users, which lightens the load on the SQL server */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected final Object nullToken = new Object();

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** OSGi DI */
  @Reference(name = "entityManagerFactory", target = "(osgi.unit.name=org.opencastproject.common)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param groupRoleProvider
   *          the GroupRoleProvider to set
   */
  @Reference(name = "groupRoleProvider")
  public void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  @Activate
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
    if (roleProvider != null) {
      return roleProvider.getRolesForUser(userName);
    }

    ArrayList<Role> roles = new ArrayList<Role>();
    User user = loadUser(userName);
    if (user != null) {
      roles.addAll(user.getRoles());
    }
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
      users.add(userRef.toUser(PROVIDER_NAME));
    }
    return users.iterator();
  }

  @Override
  public Iterator<User> findUsers(Collection<String> userNames) {
    String orgId = securityService.getOrganization().getId();
    List<User> users = new ArrayList<>();
    final List<JpaUserReference> usersByName = findUsersByUserName(orgId, userNames, emf);
    for (JpaUserReference userRef : usersByName) {
      users.add(userRef.toUser(PROVIDER_NAME));
    }
    return users.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (roleProvider == null) {
      return Collections.<Role> emptyList().iterator();
    }
    return roleProvider.findRoles(query, target, offset, limit);
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
      return userReference.toUser(PROVIDER_NAME);
    return null;
  }

  @Override
  public Iterator<User> getUsers() {
    String orgId = securityService.getOrganization().getId();
    List<User> users = new ArrayList<User>();
    for (JpaUserReference userRef : findUserReferences(orgId, 0, 0, emf)) {
      users.add(userRef.toUser(PROVIDER_NAME));
    }
    return users.iterator();
  }

  /**
   * Return the roles
   *
   * @return the roles
   */
  public Iterator<Role> getRoles() {
    if (roleProvider == null) {
      return Collections.<Role> emptyList().iterator();
    }
    return roleProvider.getRoles();
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
      cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user.toUser(PROVIDER_NAME));
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
   * {@inheritDoc}
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
      cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user.toUser(PROVIDER_NAME));
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
   * Updates a user's groups based on assigned roles
   *
   * @param user
   *          the user for whom groups should be updated
   * @throws NotFoundException
   */
  private void updateGroupMembership(JpaUserReference user) {

    logger.debug("updateGroupMembership({}, roles={})", user.getUsername(), user.getRoles().size());

    List<String> internalGroupRoles = new ArrayList<String>();

    for (Role role : user.getRoles()) {
      if (Role.Type.GROUP.equals(role.getType())
          || (Role.Type.INTERNAL.equals(role.getType()) && role.getName().startsWith(Group.ROLE_PREFIX))) {
        internalGroupRoles.add(role.getName());
      }
    }

    groupRoleProvider.updateGroupMembershipFromRoles(user.getUsername(), user.getOrganization().getId(), internalGroupRoles, "ROLE_GROUP_AAI_");
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
   * Returns user references for specific user names (and an organization)
   * @param orgId The organization to search for
   * @param names The names to search for
   * @param emf the entity manager factory
   * @return the user references list
   */
  private List<JpaUserReference> findUsersByUserName(String orgId, Collection<String> names, EntityManagerFactory emf) {
    if (names.isEmpty()) {
      return Collections.<JpaUserReference>emptyList();
    }
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("UserReference.findAllByUserNames");
      q.setParameter("org", orgId);
      q.setParameter("names", names);
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
  public long countUsers() {
    String orgId = securityService.getOrganization().getId();
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("UserReference.countAll");
      q.setParameter("org", orgId);
      return ((Number) q.getSingleResult()).longValue();
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

  public void setRoleProvider(RoleProvider roleProvider) {
    this.roleProvider = (AAIRoleProvider) roleProvider;
  }

}
