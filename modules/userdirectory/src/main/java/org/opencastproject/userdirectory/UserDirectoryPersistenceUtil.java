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

import org.opencastproject.security.api.Role;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Utility class for user directory persistence methods
 */
public final class UserDirectoryPersistenceUtil {

  private UserDirectoryPersistenceUtil() {
  }

  /**
   * Persist a set of roles
   *
   * @param roles
   *          the roles to persist
   * @param emf
   *          the entity manager factory
   * @return the persisted roles
   */
  public static Set<JpaRole> saveRoles(Set<? extends Role> roles, EntityManagerFactory emf) {
    Set<JpaRole> updatedRoles = new HashSet<JpaRole>();
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Save or update roles
      for (Role role : roles) {
        JpaRole jpaRole = (JpaRole) role;
        saveOrganization(jpaRole.getJpaOrganization(), emf);
        JpaRole findRole = findRole(jpaRole.getName(), jpaRole.getOrganizationId(), emf);
        if (findRole == null) {
          em.persist(jpaRole);
          updatedRoles.add(jpaRole);
        } else {
          findRole.setDescription(jpaRole.getDescription());
          updatedRoles.add(em.merge(findRole));
        }
      }
      tx.commit();
      return updatedRoles;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Persist an organization
   *
   * @param organization
   *          the organization to persist
   * @param emf
   *          the entity manager factory
   * @return the persisted organization
   */
  public static JpaOrganization saveOrganization(JpaOrganization organization, EntityManagerFactory emf) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaOrganization org = findOrganization(organization, emf);
      if (org == null) {
        em.persist(organization);
      } else {
        organization = em.merge(org);
      }
      tx.commit();
      return organization;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Persist an user
   * 
   * @param user
   *          the user to persist
   * @param emf
   *          the entity manager factory
   * @return the persisted organization
   */
  public static JpaUser saveUser(JpaUser user, EntityManagerFactory emf) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaUser u = findUser(user.getUsername(), user.getOrganization().getId(), emf);
      if (u == null) {
        em.persist(user);
      } else {
        user.setId(u.getId());
        user = em.merge(user);
      }
      tx.commit();
      return user;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all groups from the persistence unit as a list
   *
   * @param organization
   *          the organization
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the group list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaGroup> findGroups(String organization, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Group.findAll").setMaxResults(limit).setFirstResult(offset);
      query.setParameter("organization", organization);
      return query.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Count how many groups there are in total fitting the filter criteria.
   *
   * @param orgId
   *          the organization id
   * @param nameFilter
   *          filter by group name (optional)
   * @param textFilter
   *          fulltext filter (optional)
   * @param emf
   *          the entity manager factory
   * @return the group list
   * @throws IllegalArgumentException
   */
  public static long countTotalGroups(String orgId, Optional<String> nameFilter, Optional<String> textFilter,
          EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      final CriteriaQuery<Long> query = cb.createQuery(Long.class);
      Root<JpaGroup> group = query.from(JpaGroup.class);
      query.select(cb.count(group));

      addWhereToQuery(query, cb, group, orgId, nameFilter, textFilter);

      TypedQuery<Long> typedQuery = em.createQuery(query);
      return typedQuery.getSingleResult();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Add where clauses to groups query.
   *
   * @param query
   *         the query
   * @param cb
   *          the criteria builder
   * @param group
   *          the table
   * @param orgId
   *          the organization id
   * @param nameFilter
   *          filter by group name (optional)
   * @param textFilter
   *          fulltext filter (optional)
   */
  private static void addWhereToQuery(CriteriaQuery query, CriteriaBuilder cb, Root<JpaGroup> group,
          String orgId, Optional<String> nameFilter, Optional<String> textFilter) {
    List<Predicate> conditions = new ArrayList();
    conditions.add(cb.equal(group.join("organization").get("id"), orgId));

    // exact match, case sensitive
    if (nameFilter.isPresent()) {
      conditions.add(cb.equal(group.get("name"), nameFilter.get()));
    }
    // not exact match, case-insensitive, each token needs to match at least one field
    if (textFilter.isPresent()) {
      List<Predicate> fulltextConditions = new ArrayList();
      String[] tokens = textFilter.get().split("\\s+");
      for (String token: tokens) {
        List<Predicate> fieldConditions = new ArrayList();
        Expression<String> literal = cb.literal("%" + token + "%");

        fieldConditions.add(cb.like(cb.lower(group.get("groupId")), cb.lower(literal)));
        fieldConditions.add(cb.like(cb.lower(group.get("name")), cb.lower(literal)));
        fieldConditions.add(cb.like(cb.lower(group.get("description")), cb.lower(literal)));
        fieldConditions.add(cb.like(cb.lower(group.get("role")), cb.lower(literal)));
        fieldConditions.add(cb.like(cb.lower(group.<JpaGroup, String>joinSet("members", JoinType.LEFT)),
                cb.lower(literal)));
        fieldConditions.add(cb.like(cb.lower(group.<JpaGroup, JpaRole>joinSet("roles", JoinType.LEFT).get("name")),
                cb.lower(literal)));

        // token needs to match at least one field
        fulltextConditions.add(cb.or(fieldConditions.toArray(new Predicate[fieldConditions.size()])));
      }
      // all token have to match something
      // (different to fulltext search for Elasticsearch, where only one token has to match!)
      conditions.add(cb.and(fulltextConditions.toArray(new Predicate[fulltextConditions.size()])));
    }
    query.where(cb.and(conditions.toArray(new Predicate[conditions.size()])));
  }

  /**
   * Get group list by criteria.
   *
   * @param orgId
   *          the organization id
   * @param limit
   *          the limit (optional)
   * @param offset
   *          the offset (optional)
   * @param nameFilter
   *          filter by group name (optional)
   * @param textFilter
   *          fulltext filter (optional)
   * @param sortCriteria
   *          the sorting criteria (name, role or description)
   * @param emf
   *          the entity manager factory
   * @return the group list
   * @throws IllegalArgumentException
   */
  public static List<JpaGroup> findGroups(String orgId, Optional<Integer> limit, Optional<Integer> offset,
          Optional<String> nameFilter, Optional<String> textFilter, Set<SortCriterion> sortCriteria,
          EntityManagerFactory emf) throws IllegalArgumentException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      final CriteriaQuery<JpaGroup> query = cb.createQuery(JpaGroup.class);
      Root<JpaGroup> group = query.from(JpaGroup.class);
      query.select(group);
      query.distinct(true);

      // filter
      addWhereToQuery(query, cb, group, orgId, nameFilter, textFilter);

      // sort
      List<Order> orders = new ArrayList<>();
      for (SortCriterion criterion : sortCriteria) {
        switch(criterion.getFieldName()) {
          case "name":
          case "description":
          case "role":
            Expression expression = group.get(criterion.getFieldName());
            if (criterion.getOrder() == SortCriterion.Order.Ascending) {
              orders.add(cb.asc(expression));
            } else if (criterion.getOrder() == SortCriterion.Order.Descending) {
              orders.add(cb.desc(expression));
            }
            break;
          default:
            throw new IllegalArgumentException("Sorting criterion " + criterion.getFieldName() + " is not supported "
                    + "for groups.");
        }
      }
      query.orderBy(orders);

      TypedQuery<JpaGroup> typedQuery = em.createQuery(query);
      if (limit.isPresent()) {
        typedQuery.setMaxResults(limit.get());
      }
      if (offset.isPresent()) {
        typedQuery.setFirstResult(offset.get());
      }

      return typedQuery.getResultList();

    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all roles from the persistence unit as a list
   *
   * @param organization
   *          the organization
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the roles list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaRole> findRoles(String organization, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Role.findAll").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("org", organization);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of roles by a search query if set or all roles if search query is <code>null</code>
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
   * @return the roles list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaRole> findRolesByQuery(String orgId, String query, int limit, int offset,
          EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Role.findByQuery").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all user groups from the persistence unit as a list
   *
   * @param userName
   *          the user name
   * @param orgId
   *          the user's organization
   * @param emf
   *          the entity manager factory
   * @return the group list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaGroup> findGroupsByUser(String userName, String orgId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Group.findByUser");
      query.setParameter("username", userName);
      query.setParameter("organization", orgId);
      return query.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted organization by the given organization
   *
   * @param organization
   *          the organization
   * @param emf
   *          the entity manager factory
   * @return the organization or <code>null</code> if not found
   */
  public static JpaOrganization findOrganization(JpaOrganization organization, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Organization.findById");
      query.setParameter("id", organization.getId());
      return (JpaOrganization) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Return specific users by their user names
   * @param userNames list of user names
   * @param organizationId organization to search for
   * @param emf the entity manager factory
   * @return the list of users that was found
   */
  public static List<JpaUser> findUsersByUserName(Collection<String> userNames, String organizationId, EntityManagerFactory emf) {
    if (userNames.isEmpty()) {
      return Collections.<JpaUser>emptyList();
    }
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findAllByUserNames");
      q.setParameter("names", userNames);
      q.setParameter("org", organizationId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted user by the user name and organization id
   *
   * @param userName
   *          the user name
   * @param organizationId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the user or <code>null</code> if not found
   */
  public static JpaUser findUser(String userName, String organizationId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findByUsername");
      q.setParameter("u", userName);
      q.setParameter("org", organizationId);
      return (JpaUser) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted user by the user id and organization id
   *
   * @param id
   *          the user's unique id
   * @param organizationId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the user or <code>null</code> if not found
   */
  public static JpaUser findUser(long id, String organizationId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findByIdAndOrg");
      q.setParameter("id", id);
      q.setParameter("org", organizationId);
      return (JpaUser) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the total of users
   *
   * @param organizationId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the total number of users
   */
  public static long countUsers(String organizationId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.countAllByOrg");
      q.setParameter("org", organizationId);
      return ((Number) q.getSingleResult()).longValue();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the total number of users
   *
   * @param emf the entity manager factory
   * @return the total number of users
   */
  public static long countUsers(EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.countAll");
      return ((Number) q.getSingleResult()).longValue();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of users by a search query if set or all users if search query is <code>null</code>
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
   * @return the users list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaUser> findUsersByQuery(String orgId, String query, int limit, int offset,
          EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findByQuery").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of users by a search query if set or all users if search query is <code>null</code>
   *
   * @param orgId,
   *          the organization id
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the users list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaUser> findUsers(String orgId, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findAll").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted role by the name and organization id
   *
   * @param name
   *          the role name
   * @param organization
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the user or <code>null</code> if not found
   */
  public static JpaRole findRole(String name, String organization, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Role.findByName");
      query.setParameter("name", name);
      query.setParameter("org", organization);
      return (JpaRole) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted group by the group id and organization id
   *
   * @param groupId
   *          the group id
   * @param orgId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the group or <code>null</code> if not found
   */
  public static JpaGroup findGroup(String groupId, String orgId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Group.findById");
      q.setParameter("groupId", groupId);
      q.setParameter("organization", orgId);
      return (JpaGroup) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted group by the group role name and organization id
   *
   * @param role
   *          the role name
   * @param orgId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the group or <code>null</code> if not found
   */
  public static JpaGroup findGroupByRole(String role, String orgId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Group.findByRole");
      q.setParameter("role", role);
      q.setParameter("organization", orgId);
      return (JpaGroup) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }


  public static void removeGroup(String groupId, String orgId, EntityManagerFactory emf) throws NotFoundException,
  Exception {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaGroup group = findGroup(groupId, orgId, emf);
      if (group == null) {
        throw new NotFoundException("Group with ID " + groupId + " does not exist");
      }
      em.remove(em.merge(group));
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Delete the user with given name in the given organization
   * 
   * @param username
   *          the name of the user to delete
   * @param orgId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @throws NotFoundException
   * @throws Exception
   */
  public static void deleteUser(String username, String orgId, EntityManagerFactory emf) throws NotFoundException,
  Exception {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaUser user = findUser(username, orgId, emf);
      if (user == null) {
        throw new NotFoundException("User with name " + username + " does not exist");
      }
      em.remove(em.merge(user));
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      em.close();
    }
  }

}
