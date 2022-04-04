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

public final class Queries {
  private Queries() {
  }

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

  public static final QueriesBase nativeQuery = new QueriesBase() {
    @Override
    protected Query createQuery(EntityManager em, String sql, Object... params) {
      return configureQuery(em.createNativeQuery(sql), params);
    }
  };

  public abstract static class TypedQueriesBase extends QueriesBase {
    public <T> Function<EntityManager, T> findById(Class<T> clazz, Object id) {
      return em -> em.find(clazz, id);
    }

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

    public <T> Function<EntityManager, T> find(String q, Class<T> clazz, Object... params) {
      return em -> createTypedQuery(em, q, clazz, params).getSingleResult();
    }

    public <T> Function<EntityManager, Optional<T>> findOpt(String q, Class<T> clazz, Object... params) {
      return em -> {
        try {
          return Optional.of(createTypedQuery(em, q, clazz, params).getSingleResult());
        } catch (NoResultException | NonUniqueResultException e) {
          return Optional.empty();
        }
      };
    }

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
    public Function<EntityManager, Object> find(String q, Object... params) {
      return em -> createQuery(em, q, params).getSingleResult();
    }

    public Function<EntityManager, Optional<Object>> findOpt(String q, Object... params) {
      return em -> {
        try {
          return Optional.of(createQuery(em, q, params).getSingleResult());
        } catch (NoResultException | NonUniqueResultException e) {
          return Optional.empty();
        }
      };
    }

    public Function<EntityManager, List> findAll(String q, Object... params) {
      return em -> createQuery(em, q, params).getResultList();
    }

    public Function<EntityManager, Integer> update(String q, Object... params) {
      return em -> createQuery(em, q, params).executeUpdate();
    }

    public Function<EntityManager, Integer> delete(String q, Object... params) {
      return em -> createQuery(em, q, params).executeUpdate();
    }

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

    public <E> Consumer<EntityManager> remove(final E entity) {
      return em -> {
        em.remove(entity);
      };
    }

    public <E> Function<EntityManager, E> persist(final E entity) {
      return em -> {
        em.persist(entity);
        return entity;
      };
    }

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
