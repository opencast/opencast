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
package org.opencastproject.util.persistence;

import org.joda.time.base.AbstractInstant;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

/** JPA query constructors. */
// CHECKSTYLE:OFF
public final class Queries {
  private Queries() {
  }

  /** {@link javax.persistence.EntityManager#find(Class, Object)} as a function wrapping the result into an Option. */
  public static <A> Function<EntityManager, Option<A>> find(final Class<A> clazz, final Object primaryKey) {
    return new Function<EntityManager, Option<A>>() {
      @Override public Option<A> apply(EntityManager em) {
        return option(em.find(clazz, primaryKey));
      }
    };
  }

  /** {@link javax.persistence.EntityManager#persist(Object)} as a function. */
  public static <A> Function<EntityManager, A> persist(final A a) {
    return new Function<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        em.persist(a);
        return a;
      }
    };
  }

  /** {@link javax.persistence.EntityManager#merge(Object)} as a function. */
  public static <A> Function<EntityManager, A> merge(final A a) {
    return new Function<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        return em.merge(a);
      }
    };
  }

  public static <A> Function<EntityManager, A> remove(final A a) {
    return new Function<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        em.remove(a);
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }
    };
  }

  /**
   * Set a list of named parameters on a query.
   * <p/>
   * Values of type {@link java.util.Date} and {@link org.joda.time.base.AbstractInstant}
   * are recognized and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
   */
  public static <A extends Query> A setParams(A q, Tuple<String, ?>... params) {
    for (Tuple<String, ?> p : params) {
      final Object value = p.getB();
      if (value instanceof Date) {
        q.setParameter(p.getA(), (Date) value, TemporalType.TIMESTAMP);
      }
      if (value instanceof AbstractInstant) {
        q.setParameter(p.getA(), ((AbstractInstant) value).toDate(), TemporalType.TIMESTAMP);
      } else {
        q.setParameter(p.getA(), p.getB());
      }
    }
    return q;
  }

  /**
   * Set a list of positional parameters on a query.
   * <p/>
   * Values of type {@link java.util.Date} and {@link org.joda.time.base.AbstractInstant}
   * are recognized and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
   */
  public static <A extends Query> A setParams(A q, Object... params) {
    for (int i = 0; i < params.length; i++) {
      final Object value = params[i];
      if (value instanceof Date) {
        q.setParameter(i, (Date) value, TemporalType.TIMESTAMP);
      }
      if (value instanceof AbstractInstant) {
        q.setParameter(i, ((AbstractInstant) value).toDate(), TemporalType.TIMESTAMP);
      } else {
        q.setParameter(i, value);
      }
    }
    return q;
  }

  // -------------------------------------------------------------------------------------------------------------------

  /** Named queries with support for named parameters. */
  public static final TypedQueriesBase<Tuple<String, ?>> named = new TypedQueriesBase<Tuple<String, ?>>() {
    @Override public Query query(EntityManager em, String q, Tuple<String, ?>... params) {
      return setParams(em.createNamedQuery(q), params);
    }

    @Override
    public <A> TypedQuery<A> query(EntityManager em, String q, Class<A> type, Tuple<String, ?>... params) {
      return setParams(em.createNamedQuery(q, type), params);
    }
  };

  /** JPQL queries with support for named parameters. */
  public static final TypedQueriesBase<Tuple<String, ?>> jpql = new TypedQueriesBase<Tuple<String, ?>>() {
    @Override public Query query(EntityManager em, String q, Tuple<String, ?>... params) {
      return setParams(em.createQuery(q), params);
    }

    @Override
    public <A> TypedQuery<A> query(EntityManager em, String q, Class<A> type, Tuple<String, ?>... params) {
      return setParams(em.createQuery(q, type), params);
    }
  };

  /** Native SQL queries. Only support positional parameters. */
  public static final QueriesBase<Object> sql = new QueriesBase<Object>() {
    @Override public Query query(EntityManager em, String q, Object... params) {
      return setParams(em.createNativeQuery(q), params);
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static abstract class TypedQueriesBase<P> extends QueriesBase<P> {
    protected TypedQueriesBase() {
    }

    /**
     * Create a typed query from <code>q</code> with a list of parameters.
     * Values of type {@link java.util.Date} are recognized
     * and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
     */
    public abstract <A> TypedQuery<A> query(EntityManager em,
                                            String queryName,
                                            Class<A> type,
                                            Tuple<String, ?>... params);

    /** Find multiple entities. */
    public <A> List<A> findAll(EntityManager em,
                               final String q,
                               final Class<A> type,
                               final Tuple<String, ?>... params) {
      return query(em, q, type, params).getResultList();
    }

    /** {@link #findAll(EntityManager, String, Class, Tuple[])} as a function. */
    public <A> Function<EntityManager, List<A>> findAll(final String q,
                                                        final Class<A> type,
                                                        final Tuple<String, ?>... params) {
      return new Function<EntityManager, List<A>>() {
        @Override public List<A> apply(EntityManager em) {
          return findAll(em, q, type, params);
        }
      };
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static abstract class QueriesBase<P> {
    protected QueriesBase() {
    }

    /**
     * Create a query from <code>q</code> with a list of parameters.
     * Values of type {@link java.util.Date} are recognized
     * and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
     */
    public abstract Query query(EntityManager em, String q, P... params);

    /** Run an update (UPDATE or DELETE) query and ensure that at least one row got affected. */
    public boolean update(EntityManager em, String q, P... params) {
      return query(em, q, params).executeUpdate() > 0;
    }

    /** {@link #update(EntityManager, String, Object[])} as a function. */
    public Function<EntityManager, Boolean> update(final String q, final P... params) {
      return new Function<EntityManager, Boolean>() {
        @Override public Boolean apply(EntityManager em) {
          return update(em, q, params);
        }
      };
    }

    /**
     * Run a SELECT query that should return a single result.
     *
     * @return some value if the query yields exactly one result, none otherwise
     */
    public <A> Option<A> findSingle(final EntityManager em,
                                    final String q,
                                    final P... params) {
      try {
        return some((A) query(em, q, params).getSingleResult());
      } catch (NoResultException e) {
        return none();
      } catch (NonUniqueResultException e) {
        return none();
      }
    }

    /** {@link #findSingle(EntityManager, String, Object[])} as a function. */
    public <A> Function<EntityManager, Option<A>> findSingle(final String q,
                                                             final P... params) {
      return new Function<EntityManager, Option<A>>() {
        @Override public Option<A> apply(EntityManager em) {
          return findSingle(em, q, params);
        }
      };
    }

    /** Run a SELECT query and return only the first result item. */
    public <A> Option<A> findFirst(final EntityManager em,
                                   final String q,
                                   final P... params) {
      try {
        return some((A) query(em, q, params).setMaxResults(1).getSingleResult());
      } catch (NoResultException e) {
        return none();
      } catch (NonUniqueResultException e) {
        return none();
      }
    }

    /** {@link #findSingle(EntityManager, String, Object[])} as a function. */
    public <A> Function<EntityManager, Option<A>> findFirst(final String q,
                                                            final P... params) {
      return new Function<EntityManager, Option<A>>() {
        @Override public Option<A> apply(EntityManager em) {
          return findFirst(em, q, params);
        }
      };
    }

    /** Run a COUNT(x) query. */
    public long count(final EntityManager em, final String q, final P... params) {
      return (Long) query(em, q, params).getSingleResult();
    }

    /** {@link #count(EntityManager, String, Object[])} as a function. */
    public Function<EntityManager, Long> count(final String q, final P... params) {
      return new Function<EntityManager, Long>() {
        @Override public Long apply(EntityManager em) {
          return count(em, q, params);
        }
      };
    }

    /** Find multiple entities. */
    public <A> List<A> findAll(EntityManager em, final String q, final P... params) {
      return (List<A>) query(em, q, params).getResultList();
    }

    /** {@link #findAll(EntityManager, String, Object[])} as a function. */
    public <A> Function<EntityManager, List<A>> findAll(final String q,
                                                        final P... params) {
      return new Function<EntityManager, List<A>>() {
        @Override public List<A> apply(EntityManager em) {
          return findAll(em, q, params);
        }
      };
    }

    /** Find multiple entities and wrap the in the list monad. */
    public <A> Monadics.ListMonadic<A> findAllM(EntityManager em, final String q, final P... params) {
      return mlist(this.<A>findAll(em, q, params));
    }

    /** {@link #findAllM(EntityManager, String, Object[])} as a function. */
    public <A> Function<EntityManager, Monadics.ListMonadic<A>> findAllM(final String q,
                                                                         final P... params) {
      return new Function<EntityManager, Monadics.ListMonadic<A>>() {
        @Override public Monadics.ListMonadic<A> apply(EntityManager em) {
          return findAllM(em, q, params);
        }
      };
    }

    /** Find multiple objects with optional pagination. */
    public <A> List<A> findAll(final EntityManager em,
                               final String q,
                               final Option<Integer> offset,
                               final Option<Integer> limit,
                               final P... params) {
      final Query query = query(em, q, params);
      for (Integer x : offset) query.setFirstResult(x);
      for (Integer x : limit) query.setMaxResults(x);
      return (List<A>) query.getResultList();
    }

    /** {@link #findAll(EntityManager, String, Option, Option, Object[])} as a function. */
    public <A> Function<EntityManager, List<A>> findAll(final String q,
                                                        final Option<Integer> offset,
                                                        final Option<Integer> limit,
                                                        final P... params) {
      return new Function<EntityManager, List<A>>() {
        @Override public List<A> apply(EntityManager em) {
          return findAll(em, q, offset, limit, params);
        }
      };
    }

    /** Find multiple objects with optional pagination wrapped in the list monad. */
    public <A> Monadics.ListMonadic<A> findAllM(final EntityManager em,
                                                final String q,
                                                final Option<Integer> offset,
                                                final Option<Integer> limit,
                                                final P... params) {
      return mlist(this.<A>findAll(em, q, offset, limit, params));
    }

    /** {@link #findAllM(EntityManager, String, Option, Option, Object[])} as a function. */
    public <A> Function<EntityManager, Monadics.ListMonadic<A>> findAllM(final String q,
                                                                         final Option<Integer> offset,
                                                                         final Option<Integer> limit,
                                                                         final P... params) {
      return new Function<EntityManager, Monadics.ListMonadic<A>>() {
        @Override public Monadics.ListMonadic<A> apply(EntityManager em) {
          return findAllM(em, q, offset, limit, params);
        }
      };
    }

    /** Find multiple objects with pagination. */
    public <A> List<A> findAll(final EntityManager em,
                               final String q,
                               final int offset,
                               final int limit,
                               final P... params) {
      final Query query = query(em, q, params);
      query.setFirstResult(offset);
      query.setMaxResults(limit);
      return (List<A>) query.getResultList();
    }

    public <A> Function<EntityManager, List<A>> findAll(final String q,
                                                        final int offset,
                                                        final int limit,
                                                        final P... params) {
      return new Function<EntityManager, List<A>>() {
        @Override public List<A> apply(EntityManager em) {
          return findAll(em, q, offset, limit, params);
        }
      };
    }

    /** Find multiple objects with pagination wrapped in the list monad. */
    public <A> Monadics.ListMonadic<A> findAllM(final EntityManager em,
                                                final String q,
                                                final int offset,
                                                final int limit,
                                                final P... params) {
      return mlist(this.<A>findAll(em, q, offset, limit, params));
    }

    /** {@link #findAllM(javax.persistence.EntityManager, String, int, int, Object[])} as a function. */
    public <A> Function<EntityManager, Monadics.ListMonadic<A>> findAllM(final String q,
                                                                         final int offset,
                                                                         final int limit,
                                                                         final P... params) {
      return new Function<EntityManager, Monadics.ListMonadic<A>>() {
        @Override public Monadics.ListMonadic<A> apply(EntityManager em) {
          return findAllM(em, q, offset, limit, params);
        }
      };
    }
  }
}
