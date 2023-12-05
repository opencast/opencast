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

package org.opencastproject.db;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.base.AbstractInstant;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

/**
 * Helper class defining common query functions that can be used with DBSession query execution methods.
 */
public final class Queries {
  private Queries() {
  }

  /**
   * Execute a typed named query.
   */
  public static final TypedQueriesBase namedQuery = new TypedQueriesBase() {
    @Override
    protected Query createQuery(EntityManager em, String queryName, Object... params) {
      return configureQuery(em.createNamedQuery(queryName), params);
    }

    @Override
    protected <T> TypedQuery<T> createTypedQuery(EntityManager em, String queryName, Class<T> clazz, Object... params) {
      return configureQuery(em.createNamedQuery(queryName, clazz), params);
    }
  };

  /**
   * Execute a native SQL query.
   */
  public static final QueriesBase nativeQuery = new QueriesBase() {
    @Override
    protected Query createQuery(EntityManager em, String sql, Object... params) {
      return configureQuery(em.createNativeQuery(sql), params);
    }
  };

  public abstract static class TypedQueriesBase extends QueriesBase {
    /**
     * Find entity by its id.
     *
     * @param clazz Entity class.
     * @param id ID of the entity.
     * @return The entity or null if not found.
     * @param <T> Entity type.
     */
    public <T> Function<EntityManager, T> findById(Class<T> clazz, Object id) {
      return em -> em.find(clazz, id);
    }

    /**
     * Find entity by its id.
     *
     * @param clazz Entity class.
     * @param id ID of the entity.
     * @return An Optional with the entity or an empty Optional if not found.
     * @param <T> Entity type.
     */
    public <T> Function<EntityManager, Optional<T>> findByIdOpt(Class<T> clazz, Object id) {
      return em -> {
        try {
          T e = em.find(clazz, id);
          if (e == null) {
            return Optional.empty();
          }
          return Optional.of(e);
        } catch (NoResultException e) {
          return Optional.empty();
        }
      };
    }

    /**
     * Execute a named query and return a single result.
     *
     * @param q Name of the query.
     * @param clazz Entity class.
     * @param params Parameters passed to the query.
     * @return The entity. An exception is thrown if not found.
     * @param <T> Entity type.
     */
    public <T> Function<EntityManager, T> find(String q, Class<T> clazz, Object... params) {
      return em -> createTypedQuery(em, q, clazz, params).getSingleResult();
    }

    /**
     * Execute a named query and return a single result.
     *
     * @param q Name of the query.
     * @param clazz Entity class.
     * @param params Parameters passed to the query.
     * @return An Optional with the entity or an empty Optional if not found.
     * @param <T> Entity type.
     */
    public <T> Function<EntityManager, Optional<T>> findOpt(String q, Class<T> clazz, Object... params) {
      return em -> {
        try {
          return Optional.of(createTypedQuery(em, q, clazz, params).getSingleResult());
        } catch (NoResultException | NonUniqueResultException e) {
          return Optional.empty();
        }
      };
    }

    /**
     * Execute a named query and return all results.
     *
     * @param q Name of the query.
     * @param clazz Entity class.
     * @param params Parameters passed to the query.
     * @return A list of entities.
     * @param <T> Entity type.
     */
    public <T> Function<EntityManager, List<T>> findAll(String q, Class<T> clazz, Object... params) {
      return em -> createTypedQuery(em, q, clazz, params).getResultList();
    }

    protected abstract <T> TypedQuery<T> createTypedQuery(EntityManager em, String queryName, Class<T> clazz,
        Object... params);

    protected <T> TypedQuery<T> configureQuery(TypedQuery<T> q, Object... params) {
      return (TypedQuery<T>) configureQuery((Query) q, params);
    }
  }

  public abstract static class QueriesBase {
    /**
     * Execute a named query and return a single result.
     *
     * @param q Name of the query.
     * @param params Parameters passed to the query.
     * @return The entity. An exception is thrown if not found.
     */
    public Function<EntityManager, Object> find(String q, Object... params) {
      return em -> createQuery(em, q, params).getSingleResult();
    }

    /**
     * Execute a named query and return a single result.
     *
     * @param q Name of the query.
     * @param params Parameters passed to the query.
     * @return An Optional with the entity or an empty Optional if not found.
     */
    public Function<EntityManager, Optional<Object>> findOpt(String q, Object... params) {
      return em -> {
        try {
          return Optional.of(createQuery(em, q, params).getSingleResult());
        } catch (NoResultException | NonUniqueResultException e) {
          return Optional.empty();
        }
      };
    }

    /**
     * Execute a named query and return all results.
     *
     * @param q Name of the query.
     * @param params Parameters passed to the query.
     * @return A list of entities.
     */
    public Function<EntityManager, List> findAll(String q, Object... params) {
      return em -> createQuery(em, q, params).getResultList();
    }

    /**
     * Execute a named update query.
     *
     * @param q Name of the query.
     * @param params Parameters passed to the query.
     * @return The number of updated entities.
     */
    public Function<EntityManager, Integer> update(String q, Object... params) {
      return em -> createQuery(em, q, params).executeUpdate();
    }

    /**
     * Execute a named delete query.
     *
     * @param q Name of the query.
     * @param params Parameters passed to the query.
     * @return The number of deleted entities.
     */
    public Function<EntityManager, Integer> delete(String q, Object... params) {
      return em -> createQuery(em, q, params).executeUpdate();
    }

    /**
     * Create or update passed entity.
     *
     * @param entity Entity to create or update.
     * @return Created or updated entity.
     * @param <E> Entity type.
     */
    public <E> Function<EntityManager, E> persistOrUpdate(final E entity) {
      return em -> {
        final Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        if (id == null) {
          em.persist(entity);
          return entity;
        } else {
          @SuppressWarnings("unchecked")
          final E dto = (E) em.find(entity.getClass(), id);
          if (dto == null) {
            em.persist(entity);
            return entity;
          } else {
            return em.merge(entity);
          }
        }
      };
    }

    /**
     * Delete passed entity.
     *
     * @param entity Entity to delete.
     * @return Deleted entity.
     * @param <E> Entity type.
     */
    public <E> Consumer<EntityManager> remove(final E entity) {
      return em -> {
        em.remove(entity);
      };
    }

    /**
     * Create passed entity.
     *
     * @param entity Entity to create.
     * @return Created entity.
     * @param <E> Entity type.
     */
    public <E> Function<EntityManager, E> persist(final E entity) {
      return em -> {
        em.persist(entity);
        return entity;
      };
    }

    /**
     * Create passed entity.
     *
     * @param entity Entity to create.
     * @return Optional with the created entity.
     * @param <E> Entity type.
     */
    public <E> Function<EntityManager, Optional<E>> persistOpt(final E entity) {
      return em -> {
        em.persist(entity);
        return Optional.of(entity);
      };
    }

    protected abstract Query createQuery(EntityManager em, String q, Object... params);

    protected Query configureQuery(Query q, Object... params) {
      for (int i = 0; i < params.length; i++) {
        Object p = params[i];

        if (p instanceof Pair) { // named parameters
          Pair<String, ?> pair = (Pair<String, ?>) p;
          String key = pair.getKey();
          Object value = pair.getValue();
          if (value instanceof Date) {
            q.setParameter(key, (Date) value, TemporalType.TIMESTAMP);
          } else if (value instanceof Calendar) {
            q.setParameter(key, (Calendar) value, TemporalType.TIMESTAMP);
          } else if (value instanceof AbstractInstant) {
            q.setParameter(key, ((AbstractInstant) value).toDate(), TemporalType.TIMESTAMP);
          } else {
            q.setParameter(key, value);
          }
        } else { // positional parameters
          if (p instanceof Date) {
            q.setParameter(i + 1, (Date) p, TemporalType.TIMESTAMP);
          } else if (p instanceof AbstractInstant) {
            q.setParameter(i + 1, ((AbstractInstant) p).toDate(), TemporalType.TIMESTAMP);
          } else {
            q.setParameter(i + 1, p);
          }
        }
      }

      return q;
    }
  }
}
