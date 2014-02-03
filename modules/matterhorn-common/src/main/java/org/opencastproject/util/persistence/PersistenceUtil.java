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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Functions;
import org.osgi.service.component.ComponentContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

/**
 * Functions supporting persistence.
 */
public final class PersistenceUtil {
  private PersistenceUtil() {
  }

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>.
   * A {@link javax.persistence.spi.PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi service.
   * If you want to configure the factory please also register a map containing all properties under
   * the name <code>persistenceProps</code>. See {@link javax.persistence.spi.PersistenceProvider#createEntityManagerFactory(String, java.util.Map)}
   * for more information about config maps.
   *
   * @param emName
   *         name of the persistence unit
   */
  public static EntityManagerFactory newEntityManagerFactory(ComponentContext cc, String emName) {
    PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    final Map persistenceProps;
    Map pp = (Map) cc.locateService("persistenceProps");
    persistenceProps = pp != null ? pp : Collections.emptyMap();
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
  }

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>.
   * A {@link javax.persistence.spi.PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi service.
   * See {@link javax.persistence.spi.PersistenceProvider#createEntityManagerFactory(String, java.util.Map)}
   * for more information about config maps.
   *
   * @param emName
   *         name of the persistence unit
   * @param persistenceProps
   *         config map for the creation of an EntityManagerFactory
   */
  public static EntityManagerFactory newEntityManagerFactory(ComponentContext cc, String emName, Map persistenceProps) {
    PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
  }

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p/>
   * Runtime exceptions that occured are simply rethrown.
   * <p/>
   * Please note that calling {@link PersistenceEnv#tx(org.opencastproject.util.data.Function)} always creates a <em>new</em> transaction.
   * Transaction propagation is <em>not</em> supported.
   */
  public static PersistenceEnv newPersistenceEnvironment(final EntityManagerFactory emf) {
    return newPersistenceEnvironment(emf, Functions.<Exception>identity());
  }

  /** Create a new persistence environment. This method is the preferred way of creating a persitence environment. */
  public static PersistenceEnv newPersistenceEnvironment(PersistenceProvider persistenceProvider, String emName,
                                                         Map persistenceProps) {
    return newPersistenceEnvironment(persistenceProvider.createEntityManagerFactory(emName, persistenceProps));
  }

  /**
   * Shortcut for <code>newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))</code>.
   *
   * @see #newEntityManagerFactory(org.osgi.service.component.ComponentContext, String, java.util.Map)
   */
  public static PersistenceEnv newPersistenceEnvironment(ComponentContext cc, String emName, Map persistenceProps) {
    return newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps));
  }

  /**
   * Shortcut for <code>newPersistenceEnvironment(newEntityManagerFactory(cc, emName))</code>.
   *
   * @see #newEntityManagerFactory(org.osgi.service.component.ComponentContext, String)
   */
  public static PersistenceEnv newPersistenceEnvironment(ComponentContext cc, String emName) {
    return newPersistenceEnvironment(newEntityManagerFactory(cc, emName));
  }

  /**
   * Equip a persistence environment with an exception handler.
   *
   * @see #newPersistenceEnvironment(javax.persistence.EntityManagerFactory, org.opencastproject.util.data.Function)
   */
  public static <T extends Exception> PersistenceEnv equip(final PersistenceEnv penv,
                                                           final Function<Exception, T> exHandler) {
    return new PersistenceEnv() {
      @Override public <A> A tx(Function<EntityManager, A> transactional) {
        try {
          return penv.tx(transactional);
        } catch (Exception e) {
          return chuck(exHandler.apply(e));
        }
      }

      @Override public void close() {
        penv.close();
      }
    };
  }

  /**
   * Equip a persistence environment with an exception handler.
   *
   * @see #newPersistenceEnvironment(javax.persistence.EntityManagerFactory, org.opencastproject.util.data.Function)
   */
  public static <F> PersistenceEnv2<F> equip2(final PersistenceEnv penv, final Function<Exception, F> exHandler) {
    return new PersistenceEnv2<F>() {
      @Override public <A> Either<F, A> tx(Function<EntityManager, A> transactional) {
        try {
          return right(penv.tx(transactional));
        } catch (Exception e) {
          return left(exHandler.apply(e));
        }
      }

      @Override public void close() {
        penv.close();
      }
    };
  }

  private interface Transactional {
    <A> A tx(Function<EntityManager, A> transactional);
  }

  private static final ThreadLocal<Option<Transactional>> emStore = new ThreadLocal<Option<Transactional>>() {
    @Override protected Option<Transactional> initialValue() {
      return none();
    }
  };

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p/>
   * Runtime excpetions that occured are transformed by function <code>exHandler</code> and then thrown.
   * <p/>
   * Transaction propagation is supported per thread.
   */
  public static <T extends Exception> PersistenceEnv newPersistenceEnvironment(final EntityManagerFactory emf,
                                                                               final Function<Exception, T> exHandler) {
    final Transactional startTx = new Transactional() {
      @Override
      public <A> A tx(Function<EntityManager, A> transactional) {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        try {
          tx.begin();
          emStore.set(Option.<Transactional>some(new Transactional() {
            @Override public <A> A tx(Function<EntityManager, A> transactional) {
              return transactional.apply(em);
            }
          }));
          A ret = transactional.apply(em);
          tx.commit();
          return ret;
        } catch (RuntimeException e) {
          if (tx.isActive()) {
            tx.rollback();
          }
          // propagate exception
          return chuck(exHandler.apply(e));
        } finally {
          em.close();
          emStore.remove();
        }
      }
    };
    return new PersistenceEnv() {
      @Override public <A> A tx(Function<EntityManager, A> transactional) {
        return emStore.get().getOrElse(startTx).tx(transactional);
      }

      @Override public void close() {
        emf.close();
      }
    };
  }

  public static void closeQuietly(Connection c) {
    if (c != null) {
      try {
        c.close();
      } catch (SQLException ignore) {
      }
    }
  }

  /**
   * Test if a connection to the given data source can be established.
   *
   * @return none, if the connection could be established
   */
  public static Option<SQLException> testConnection(DataSource ds) {
    Connection connection = null;
    try {
      connection = ds.getConnection();
      return none();
    } catch (SQLException e) {
      return some(e);
    } finally {
      closeQuietly(connection);
    }
  }

  /**
   * Create a named query with a list of parameters. Values of type {@link Date} are recognized
   * and set as a timestamp ({@link TemporalType#TIMESTAMP}.
   */
  public static Query createNamedQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    final Query q = em.createNamedQuery(queryName);
    for (Tuple<String, ?> p : params) {
      final Object value = p.getB();
      if (value instanceof Date) {
        q.setParameter(p.getA(), (Date) p.getB(), TemporalType.TIMESTAMP);
      } else {
        q.setParameter(p.getA(), p.getB());
      }
    }
    return q;
  }

  /** Run an update query and ensure that at least one row got affected. */
  public static boolean runUpdate(EntityManager em, String queryName, Tuple<String, ?>... params) {
    return createNamedQuery(em, queryName, params).executeUpdate() > 0;
  }

  /** Run a query that should return a single result. */
  public static <A> Option<A> runSingleResultQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    try {
      return some((A) createNamedQuery(em, queryName, params).getSingleResult());
    } catch (NoResultException e) {
      return none();
    } catch (NonUniqueResultException e) {
      return none();
    }
  }

  /** Run a query that should return the first result of it. */
  public static <A> Option<A> runFirstResultQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    try {
      return some((A) createNamedQuery(em, queryName, params).setMaxResults(1).getSingleResult());
    } catch (NoResultException e) {
      return none();
    } catch (NonUniqueResultException e) {
      return none();
    }
  }

  /** Execute a <code>count(x)</code> query. */
  public static long runCountQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    return ((Number) createNamedQuery(em, queryName, params).getSingleResult()).longValue();
  }

  public static <A> Function<EntityManager, Option<A>> findById(final Class<A> clazz, final Object primaryKey) {
    return new Function<EntityManager, Option<A>>() {
      @Override public Option<A> apply(EntityManager em) {
        return option(em.find(clazz, primaryKey));
      }
    };
  }

  /**
   * Find a single object.
   *
   * @param params
   *         the query parameters
   * @param toA
   *         map to the desired result object
   * @deprecated
   */
  public static <A, B> Option<A> find(EntityManager em, final Function<B, A> toA, final String queryName,
                                      final Tuple<String, ?>... params) {
    return PersistenceUtil.<B>runSingleResultQuery(em, queryName, params).map(toA);
  }

  /** Find multiple objects. */
  public static <A> List<A> findAll(EntityManager em, final String queryName, final Tuple<String, ?>... params) {
    return (List<A>) createNamedQuery(em, queryName, params).getResultList();
  }

  /** Find multiple objects with optional pagination. */
  public static <A> List<A> findAll(EntityManager em, final String queryName,
                                    Option<Integer> offset, Option<Integer> limit,
                                    final Tuple<String, ?>... params) {
    final Query q = createNamedQuery(em, queryName, params);
    for (Integer x : offset) q.setFirstResult(x);
    for (Integer x : limit) q.setMaxResults(x);
    return (List<A>) q.getResultList();
  }

  /**
   * Find multiple objects.
   *
   * @param params
   *         the query parameters
   * @param toA
   *         map to the desired result object
   * @deprecated use {@link #findAll(javax.persistence.EntityManager, String, org.opencastproject.util.data.Tuple[])} instead
   */
  public static <A, B> List<A> findAll(EntityManager em, final Function<B, A> toA, final String queryName,
                                       final Tuple<String, ?>... params) {
    return mlist((List<B>) createNamedQuery(em, queryName, params).getResultList()).map(toA).value();
  }

  /**
   * Find multiple objects with optional pagination.
   *
   * @param params
   *         the query parameters
   * @param toA
   *         map to the desired result object
   * @deprecated use {@link #findAll(javax.persistence.EntityManager, String, org.opencastproject.util.data.Option, org.opencastproject.util.data.Option, org.opencastproject.util.data.Tuple[])} instead
   */
  public static <A, B> List<A> findAll(EntityManager em, final Function<B, A> toA,
                                       Option<Integer> offset, Option<Integer> limit,
                                       final String queryName, final Tuple<String, ?>... params) {
    final Query q = createNamedQuery(em, queryName, params);
    for (Integer x : offset) q.setFirstResult(x);
    for (Integer x : limit) q.setMaxResults(x);
    return mlist((List<B>) q.getResultList()).map(toA).value();
  }

  /** Create function to persist object <code>a</code> using {@link EntityManager#persist(Object)}. */
  public static <A> Function<EntityManager, A> persist(final A a) {
    return new Function<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        em.persist(a);
        return a;
      }
    };
  }

  /** Create function to merge an object <code>a</code> with the persisten context of the given entity manage. */
  public static <A> Function<EntityManager, A> merge(final A a) {
    return new Function<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        em.merge(a);
        return a;
      }
    };
  }

  /**
   * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static EntityManagerFactory newTestEntityManagerFactory(String emName) {
    // Set up the database
    ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
    try {
      pooledDataSource.setDriverClass("org.h2.Driver");
    } catch (PropertyVetoException e) {
      throw new RuntimeException(e);
    }
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Set up the persistence properties
    Map<String, Object> persistenceProps = new HashMap<String, Object>();
    persistenceProps.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    persistenceProps.put("eclipselink.ddl-generation", "create-tables");
    persistenceProps.put("eclipselink.ddl-generation.output-mode", "database");

    PersistenceProvider pp = new org.eclipse.persistence.jpa.PersistenceProvider();
    return option(pp.createEntityManagerFactory(emName, persistenceProps))
            .getOrElse(Option.<EntityManagerFactory>error("Cannot create entity manager factory. Maybe you mispelled the name of the persistence unit?"));
  }

  /**
   * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
   * testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static PersistenceEnv newTestPersistenceEnv(String emName) {
    return newPersistenceEnvironment(newTestEntityManagerFactory(emName));
  }
}
