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

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

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

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.common)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param groupRoleProvider
   *          the GroupRoleProvider to set
   */
  @Reference
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
    cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<>() {
      @Override
      public Object load(String id) {
        String[] key = id.split(DELIMITER);
        logger.trace("Loading user '{}':'{}' from reference database", key[0], key[1]);
        User user = loadUserFromDB(key[0], key[1]);
        return user == null ? nullToken : user;
      }
    });

    // Set up persistence
    db = dbSessionFactory.createSession(emf);
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

    ArrayList<Role> roles = new ArrayList<>();
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
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }
    String orgId = securityService.getOrganization().getId();
    return db.exec(findUserReferencesByQueryQuery(orgId, query, limit, offset)).stream()
        .map(ref -> ref.toUser(PROVIDER_NAME))
        .collect(Collectors.toList())
        .iterator();
  }

  @Override
  public Iterator<User> findUsers(Collection<String> userNames) {
    String orgId = securityService.getOrganization().getId();
    return db.exec(findUsersByUserNameQuery(orgId, userNames)).stream()
        .map(ref -> ref.toUser(PROVIDER_NAME))
        .collect(Collectors.toList())
        .iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (roleProvider == null) {
      return Collections.emptyIterator();
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
    return loadUserFromCache(userName, orgId);
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
  private User loadUserFromDB(String userName, String organization) {
    return db.exec(findUserReferenceQuery(userName, organization))
        .map(ref -> ref.toUser(PROVIDER_NAME))
        .orElse(null);
  }

  /**
   * Loads a user from cache
   *
   * @param userName
   *          the user name
   * @param organization
   *          the organization id
   * @return the loaded user or <code>null</code> if not found
   */
  private User loadUserFromCache(String userName, String organization) {
    Object user = cache.getUnchecked(userName.concat(DELIMITER).concat(organization));
    if (user == nullToken) {
      return null;
    } else {
      return (User) user;
    }
  }

  @Override
  public Iterator<User> getUsers() {
    String orgId = securityService.getOrganization().getId();
    return db.exec(findUserReferences(orgId, 0, 0)).stream()
        .map(ref -> ref.toUser(PROVIDER_NAME))
        .collect(Collectors.toList())
        .iterator();
  }

  /**
   * Return the roles
   *
   * @return the roles
   */
  public Iterator<Role> getRoles() {
    if (roleProvider == null) {
      return Collections.emptyIterator();
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
    db.execTx(em -> {
      // Create a JPA user with an encoded password.
      Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRolesQuery(user.getRoles()).apply(em);
      JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganizationQuery(
          (JpaOrganization) user.getOrganization()).apply(em);
      JpaUserReference userReference = new JpaUserReference(user.getUsername(), user.getName(), user.getEmail(),
          mechanism, user.getLastLogin(), organization, roles);

      // Then save the user reference
      Optional<JpaUserReference> foundUserRef = findUserReferenceQuery(user.getUsername(),
          user.getOrganization().getId()).apply(em);
      if (foundUserRef.isPresent()) {
        throw new IllegalStateException("User '" + user.getUsername() + "' already exists");
      }
      em.persist(userReference);
    });
    // There is still a race when this method is executed multiple times. However, the user reference is unlikely to be
    // different.
    cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user.toUser(PROVIDER_NAME));
    updateGroupMembership(user);
  }

  /**
   * {@inheritDoc}
   */
  public void updateUserReference(JpaUserReference user) {
    db.execTx(em -> {
      Optional<JpaUserReference> foundUserRef = findUserReferenceQuery(user.getUsername(),
          user.getOrganization().getId()).apply(em);
      if (foundUserRef.isEmpty()) {
        throw new IllegalStateException("User '" + user.getUsername() + "' does not exist");
      }
      foundUserRef.get().setName(user.getName());
      foundUserRef.get().setEmail(user.getEmail());
      foundUserRef.get().setLastLogin(user.getLastLogin());
      foundUserRef.get().setRoles(UserDirectoryPersistenceUtil.saveRolesQuery(user.getRoles()).apply(em));
      em.merge(foundUserRef.get());
    });
    // There is still a race when this method is executed multiple times. However, the user reference is unlikely to be
    // different.
    cache.put(user.getUsername() + DELIMITER + user.getOrganization().getId(), user.toUser(PROVIDER_NAME));
    updateGroupMembership(user);
  }

  /**
   * Updates a user's groups based on assigned roles
   *
   * @param user
   *          the user for whom groups should be updated
   */
  private void updateGroupMembership(JpaUserReference user) {
    logger.debug("updateGroupMembership({}, roles={})", user.getUsername(), user.getRoles().size());
    List<String> internalGroupRoles = new ArrayList<>();

    for (Role role : user.getRoles()) {
      if (Role.Type.GROUP.equals(role.getType())
          || (Role.Type.INTERNAL.equals(role.getType()) && role.getName().startsWith(Group.ROLE_PREFIX))) {
        internalGroupRoles.add(role.getName());
      }
    }

    groupRoleProvider.updateGroupMembershipFromRoles(
        user.getUsername(),
        user.getOrganization().getId(),
        internalGroupRoles
    );
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
    return db.exec(findUserReferenceQuery(userName, organizationId))
        .orElse(null);
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
  private Function<EntityManager, Optional<JpaUserReference>> findUserReferenceQuery(String userName,
      String organizationId) {
    return namedQuery.findOpt(
        "UserReference.findByUsername",
        JpaUserReference.class,
        Pair.of("u", userName),
        Pair.of("org", organizationId)
    );
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
   * @return the user references list
   */
  private Function<EntityManager, List<JpaUserReference>> findUserReferencesByQueryQuery(String orgId, String query,
      int limit, int offset) {
    return em -> {
      TypedQuery<JpaUserReference> q = em.createNamedQuery("UserReference.findByQuery", JpaUserReference.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    };
  }

  /**
   * Returns user references for specific user names (and an organization)
   * @param orgId The organization to search for
   * @param names The names to search for
   * @return the user references list
   */
  private Function<EntityManager, List<JpaUserReference>> findUsersByUserNameQuery(String orgId,
      Collection<String> names) {
    return em -> {
      if (names.isEmpty()) {
        return Collections.emptyList();
      }
      TypedQuery<JpaUserReference> q = em.createNamedQuery("UserReference.findAllByUserNames", JpaUserReference.class);
      q.setParameter("org", orgId);
      q.setParameter("names", names);
      return q.getResultList();
    };
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
   * @return the user references list
   */
  private Function<EntityManager, List<JpaUserReference>> findUserReferences(String orgId, int limit, int offset) {
    return em -> {
      TypedQuery<JpaUserReference> q = em.createNamedQuery("UserReference.findAll", JpaUserReference.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      q.setParameter("org", orgId);
      return q.getResultList();
    };
  }

  @Override
  public long countUsers() {
    String orgId = securityService.getOrganization().getId();
    return db.exec(namedQuery.find(
        "UserReference.countAll",
        Number.class,
        Pair.of("org", orgId)
    )).longValue();
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
