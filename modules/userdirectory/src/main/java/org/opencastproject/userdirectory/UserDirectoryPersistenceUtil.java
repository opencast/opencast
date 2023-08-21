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

import org.opencastproject.security.api.Role;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.function.ThrowingConsumer;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.EntityManager;
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
   * @return the persisted roles
   */
  public static Function<EntityManager, Set<JpaRole>> saveRolesQuery(Set<? extends Role> roles) {
    return em -> {
      Set<JpaRole> updatedRoles = new HashSet<>();
      // Save or update roles
      for (Role role : roles) {
        JpaRole jpaRole = (JpaRole) role;
        saveOrganizationQuery(jpaRole.getJpaOrganization()).apply(em);
        Optional<JpaRole> findRole = findRoleQuery(jpaRole.getName(), jpaRole.getOrganizationId()).apply(em);
        if (findRole.isEmpty()) {
          em.persist(jpaRole);
          updatedRoles.add(jpaRole);
        } else {
          findRole.get().setDescription(jpaRole.getDescription());
          updatedRoles.add(em.merge(findRole.get()));
        }
      }
      return updatedRoles;
    };
  }

  /**
   * Persist an organization
   *
   * @param organization
   *          the organization to persist
   * @return the persisted organization
   */
  public static Function<EntityManager, JpaOrganization> saveOrganizationQuery(JpaOrganization organization) {
    return em -> {
      Optional<JpaOrganization> dbOrganization = findOrganizationQuery(organization).apply(em);
      if (dbOrganization.isEmpty()) {
        em.persist(organization);
        return organization;
      } else {
        return em.merge(dbOrganization.get());
      }
    };
  }

  /**
   * Persist an user
   *
   * @param user
   *          the user to persist
   * @return the persisted organization
   */
  public static Function<EntityManager, JpaUser> saveUserQuery(JpaUser user) {
    return em -> {
      Optional<JpaUser> dbUser = findUserQuery(user.getUsername(), user.getOrganization().getId()).apply(em);
      if (dbUser.isEmpty()) {
        em.persist(user);
        return user;
      } else {
        user.setId(dbUser.get().getId());
        return em.merge(user);
      }
    };
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
   * @return the group list
   */
  public static Function<EntityManager, List<JpaGroup>> findGroupsQuery(String organization, int limit, int offset) {
    return em -> {
      TypedQuery<JpaGroup> query = em.createNamedQuery("Group.findAll", JpaGroup.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      query.setParameter("organization", organization);
      return query.getResultList();
    };
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
   * @return the group list
   * @throws IllegalArgumentException
   */
  public static Function<EntityManager, Long> countTotalGroupsQuery(String orgId, Optional<String> nameFilter,
      Optional<String> textFilter) {
    return em -> {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      final CriteriaQuery<Long> query = cb.createQuery(Long.class);
      Root<JpaGroup> group = query.from(JpaGroup.class);
      query.select(cb.count(group));

      addWhereToQuery(query, cb, group, orgId, nameFilter, textFilter);

      TypedQuery<Long> typedQuery = em.createQuery(query);
      return typedQuery.getSingleResult();
    };
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
  private static <E> void addWhereToQuery(CriteriaQuery<E> query, CriteriaBuilder cb, Root<JpaGroup> group,
          String orgId, Optional<String> nameFilter, Optional<String> textFilter) {
    List<Predicate> conditions = new ArrayList<>();
    conditions.add(cb.equal(group.join("organization").get("id"), orgId));

    // exact match, case sensitive
    if (nameFilter.isPresent()) {
      conditions.add(cb.equal(group.get("name"), nameFilter.get()));
    }
    // not exact match, case-insensitive, each token needs to match at least one field
    if (textFilter.isPresent()) {
      List<Predicate> fulltextConditions = new ArrayList<>();
      String[] tokens = textFilter.get().split("\\s+");
      for (String token: tokens) {
        List<Predicate> fieldConditions = new ArrayList<>();
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
        fulltextConditions.add(cb.or(fieldConditions.toArray(new Predicate[0])));
      }
      // all token have to match something
      // (different to fulltext search for Elasticsearch, where only one token has to match!)
      conditions.add(cb.and(fulltextConditions.toArray(new Predicate[0])));
    }
    query.where(cb.and(conditions.toArray(new Predicate[0])));
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
   * @return the group list
   */
  public static Function<EntityManager, List<JpaGroup>> findGroupsQuery(String orgId, Optional<Integer> limit,
      Optional<Integer> offset, Optional<String> nameFilter, Optional<String> textFilter,
      Set<SortCriterion> sortCriteria) {
    return em -> {
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
    };
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
   * @return the roles list
   */
  public static Function<EntityManager, List<JpaRole>> findRolesQuery(String organization, int limit, int offset) {
    return em -> {
      TypedQuery<JpaRole> q = em.createNamedQuery("Role.findAll", JpaRole.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      q.setParameter("org", organization);
      return q.getResultList();
    };
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
   * @return the roles list
   */
  public static Function<EntityManager, List<JpaRole>> findRolesByQuery(String orgId, String query, int limit,
      int offset) {
    return em -> {
      TypedQuery<JpaRole> q = em.createNamedQuery("Role.findByQuery", JpaRole.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    };
  }

  /**
   * Returns all user groups from the persistence unit as a list
   *
   * @param userName
   *          the user name
   * @param orgId
   *          the user's organization
   * @return the group list
   */
  public static Function<EntityManager, List<JpaGroup>> findGroupsByUserQuery(String userName, String orgId) {
    return namedQuery.findAll(
        "Group.findByUser",
        JpaGroup.class,
        Pair.of("username", userName),
        Pair.of("organization", orgId)
    );
  }

  /**
   * Returns the persisted organization by the given organization
   *
   * @param organization
   *          the organization
   * @return the organization or <code>null</code> if not found
   */
  public static Function<EntityManager, Optional<JpaOrganization>> findOrganizationQuery(JpaOrganization organization) {
    return namedQuery.findOpt(
        "Organization.findById",
        JpaOrganization.class,
        Pair.of("id", organization.getId())
    );
  }

  /**
   * Return specific users by their user names
   * @param userNames list of user names
   * @param organizationId organization to search for
   * @return the list of users that was found
   */
  public static Function<EntityManager, List<JpaUser>> findUsersByUserNameQuery(Collection<String> userNames,
      String organizationId) {
    return em -> {
      if (userNames.isEmpty()) {
        return Collections.emptyList();
      }
      TypedQuery<JpaUser> q = em.createNamedQuery("User.findAllByUserNames", JpaUser.class);
      q.setParameter("names", userNames);
      q.setParameter("org", organizationId);
      return q.getResultList();
    };
  }

  /**
   * Returns the persisted user by the user name and organization id
   *
   * @param userName
   *          the user name
   * @param organizationId
   *          the organization id
   * @return the user or <code>null</code> if not found
   */
  public static Function<EntityManager, Optional<JpaUser>> findUserQuery(String userName, String organizationId) {
    return namedQuery.findOpt(
        "User.findByUsername",
        JpaUser.class,
        Pair.of("u", userName),
        Pair.of("org", organizationId)
    );
  }

  /**
   * Returns the persisted user by the user id and organization id
   *
   * @param id
   *          the user's unique id
   * @param organizationId
   *          the organization id
   * @return the user or <code>null</code> if not found
   */
  public static Function<EntityManager, Optional<JpaUser>> findUserQuery(long id, String organizationId) {
    return namedQuery.findOpt(
        "User.findByIdAndOrg",
        JpaUser.class,
        Pair.of("id", id),
        Pair.of("org", organizationId)
    );
  }

  /**
   * Returns the total of users
   *
   * @param organizationId
   *          the organization id
   * @return the total number of users
   */
  public static Function<EntityManager, Long> countUsersQuery(String organizationId) {
    return namedQuery.find(
        "User.countAllByOrg",
        Long.class,
        Pair.of("org", organizationId)
    );
  }

  /**
   * Returns the total number of users
   *
   * @return the total number of users
   */
  public static Function<EntityManager, Long> countUsersQuery() {
    return namedQuery.find("User.countAll", Long.class);
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
   * @return the users list
   */
  public static Function<EntityManager, List<JpaUser>> findUsersByQuery(String orgId, String query, int limit,
      int offset) {
    return em -> {
      TypedQuery<JpaUser> q = em.createNamedQuery("User.findByQuery", JpaUser.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    };
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
   * @return the users list
   */
  public static Function<EntityManager, List<JpaUser>> findUsersQuery(String orgId, int limit, int offset) {
    return em -> {
      TypedQuery<JpaUser> q = em.createNamedQuery("User.findAll", JpaUser.class)
          .setMaxResults(limit)
          .setFirstResult(offset);
      q.setParameter("org", orgId);
      return q.getResultList();
    };
  }

  /**
   * Returns the persisted role by the name and organization id
   *
   * @param name
   *          the role name
   * @param organization
   *          the organization id
   * @return the user or <code>null</code> if not found
   */
  public static Function<EntityManager, Optional<JpaRole>> findRoleQuery(String name, String organization) {
    return namedQuery.findOpt(
        "Role.findByName",
        JpaRole.class,
        Pair.of("name", name),
        Pair.of("org", organization)
    );
  }

  /**
   * Returns the persisted group by the group id and organization id
   *
   * @param groupId
   *          the group id
   * @param orgId
   *          the organization id
   * @return the group or <code>null</code> if not found
   */
  public static Function<EntityManager, Optional<JpaGroup>> findGroupQuery(String groupId, String orgId) {
    return namedQuery.findOpt(
        "Group.findById",
        JpaGroup.class,
        Pair.of("groupId", groupId),
        Pair.of("organization", orgId)
    );
  }

  /**
   * Returns the persisted group by the group role name and organization id
   *
   * @param role
   *          the role name
   * @param orgId
   *          the organization id
   * @return the group or <code>null</code> if not found
   */
  public static Function<EntityManager, Optional<JpaGroup>> findGroupByRoleQuery(String role, String orgId) {
    return namedQuery.findOpt(
        "Group.findByRole",
        JpaGroup.class,
        Pair.of("role", role),
        Pair.of("organization", orgId)
    );
  }

  public static ThrowingConsumer<EntityManager, NotFoundException> removeGroupQuery(String groupId, String orgId) {
    return em -> {
      Optional<JpaGroup> group = findGroupQuery(groupId, orgId).apply(em);
      if (group.isEmpty()) {
        throw new NotFoundException("Group with ID " + groupId + " does not exist");
      }
      em.remove(em.merge(group.get()));
    };
  }

  /**
   * Delete the user with given name in the given organization
   *
   * @param username
   *          the name of the user to delete
   * @param orgId
   *          the organization id
   */
  public static ThrowingConsumer<EntityManager, NotFoundException> deleteUserQuery(String username, String orgId) {
    return em -> {
      Optional<JpaUser> user = findUserQuery(username, orgId).apply(em);
      if (user.isEmpty()) {
        throw new NotFoundException("User with name " + username + " does not exist");
      }
      em.remove(em.merge(user.get()));
    };
  }
}
