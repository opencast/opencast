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

import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.osgi.service.component.ComponentContext;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;
/** Functions supporting persistence. */

/**
 * Functions supporting persistence.
 */
public final class PersistenceUtil {
  private PersistenceUtil() {
  }

  public static final Map<String, Object> NO_PERSISTENCE_PROPS = Collections
          .unmodifiableMap(new HashMap<String, Object>());

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>. A
   * {@link javax.persistence.spi.PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi
   * service. If you want to configure the factory please also register a map containing all properties under the name
   * <code>persistenceProps</code>. See
   * {@link javax.persistence.spi.PersistenceProvider#createEntityManagerFactory(String, java.util.Map)} for more
   * information about config maps.
   *
   * @param emName
   *          name of the persistence unit
   */
  public static EntityManagerFactory newEntityManagerFactory(ComponentContext cc, String emName) {
    PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    final Map persistenceProps;
    Map pp = (Map) cc.locateService("persistenceProps");
    persistenceProps = pp != null ? pp : Collections.emptyMap();
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
  }

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>. A
   * {@link javax.persistence.spi.PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi
   * service. See {@link javax.persistence.spi.PersistenceProvider#createEntityManagerFactory(String, java.util.Map)}
   * for more information about config maps.
   *
   * @param emName
   *          name of the persistence unit
   * @param persistenceProps
   *          config map for the creation of an EntityManagerFactory
   */
  public static EntityManagerFactory newEntityManagerFactory(ComponentContext cc, String emName, Map persistenceProps) {
    PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
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

  /** Create a new entity manager or return none, if the factory has already been closed. */
  public static Option<EntityManager> createEntityManager(EntityManagerFactory emf) {
    try {
      return some(emf.createEntityManager());
    } catch (IllegalStateException ex) {
      // factory is already closed
      return none();
    }
  }

  /**
   * Equip a persistence environment with an exception handler.
   *
   * @see #newPersistenceEnvironment(javax.persistence.EntityManagerFactory, org.opencastproject.util.data.Function)
   */
  public static <F> PersistenceEnv2<F> equip2(final PersistenceEnv penv, final Function<Exception, F> exHandler) {
    return new PersistenceEnv2<F>() {
      @Override
      public <A> Either<F, A> tx(Function<EntityManager, A> transactional) {
        try {
          return Either.right(penv.tx(transactional));
        } catch (Exception e) {
          return Either.left(exHandler.apply(e));
        }
      }

      @Override
      public void close() {
        penv.close();
      }
    };
  }

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p/>
   * Transaction propagation is supported on a per thread basis.
   *
   * @deprecated use {@link PersistenceEnvs#persistenceEnvironment(EntityManagerFactory)}
   */
  public static PersistenceEnv newPersistenceEnvironment(final EntityManagerFactory emf) {
    return PersistenceEnvs.persistenceEnvironment(emf);
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
   * Create a named query with a list of parameters. Values of type {@link Date} are recognized and set as a timestamp (
   * {@link TemporalType#TIMESTAMP}.
   *
   * @deprecated use {@link Queries#named#query(EntityManager, String, Class, Object[])}
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

  /**
   * Run an update (UPDATE or DELETE) query and ensure that at least one row got affected.
   *
   * @deprecated use {@link Queries#named#update(EntityManager, String, Object[])}
   */
  public static boolean runUpdate(EntityManager em, String queryName, Tuple<String, ?>... params) {
    return createNamedQuery(em, queryName, params).executeUpdate() > 0;
  }

  /**
   * Run a query (SELECT) that should return a single result.
   *
   * @deprecated use {@link Queries#named#findSingle(EntityManager, String, Object[])}
   */
  public static <A> Option<A> runSingleResultQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    try {
      return some((A) createNamedQuery(em, queryName, params).getSingleResult());
    } catch (NoResultException e) {
      return none();
    } catch (NonUniqueResultException e) {
      return none();
    }
  }

  /**
   * Run a query that should return the first result of it.
   *
   * @deprecated use {@link Queries#named#findFirst(EntityManager, String, Object[])}
   */
  public static <A> Option<A> runFirstResultQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    try {
      return some((A) createNamedQuery(em, queryName, params).setMaxResults(1).getSingleResult());
    } catch (NoResultException e) {
      return none();
    } catch (NonUniqueResultException e) {
      return none();
    }
  }

  /**
   * Execute a <code>COUNT(x)</code> query.
   *
   * @deprecated use {@link Queries#named#count(EntityManager, String, Object[])}
   */
  public static long runCountQuery(EntityManager em, String queryName, Tuple<String, ?>... params) {
    return ((Number) createNamedQuery(em, queryName, params).getSingleResult()).longValue();
  }

  /** @deprecated use {@link Queries#find(Class, Object)} */
  public static <A> Function<EntityManager, Option<A>> findById(final Class<A> clazz, final Object primaryKey) {
    return new Function<EntityManager, Option<A>>() {
      @Override
      public Option<A> apply(EntityManager em) {
        return option(em.find(clazz, primaryKey));
      }
    };
  }

  /**
   * Find a single object.
   *
   * @param params
   *          the query parameters
   * @param toA
   *          map to the desired result object
   * @deprecated
   */
  public static <A, B> Option<A> find(EntityManager em, final Function<B, A> toA, final String queryName,
          final Tuple<String, ?>... params) {
    return PersistenceUtil.<B> runSingleResultQuery(em, queryName, params).map(toA);
  }

  /**
   * Find multiple objects.
   *
   * @deprecated use {@link Queries#named#findAll(EntityManager, String, Object[])}
   */
  public static <A> List<A> findAll(EntityManager em, final String queryName, final Tuple<String, ?>... params) {
    return (List<A>) createNamedQuery(em, queryName, params).getResultList();
  }

  /**
   * Find multiple objects with optional pagination.
   *
   * @deprecated use {@link Queries#named#findAll(EntityManager, String, Option, Option, Object[])}
   */
  public static <A> List<A> findAll(EntityManager em, final String queryName, Option<Integer> offset,
          Option<Integer> limit, final Tuple<String, ?>... params) {
    final Query q = createNamedQuery(em, queryName, params);
    for (Integer x : offset)
      q.setFirstResult(x);
    for (Integer x : limit)
      q.setMaxResults(x);
    return (List<A>) q.getResultList();
  }

  /**
   * Find multiple objects.
   *
   * @param params
   *          the query parameters
   * @param toA
   *          map to the desired result object
   * @deprecated use {@link Queries#named#findAll(EntityManager, String, Object[])} instead
   */
  public static <A, B> List<A> findAll(EntityManager em, final Function<B, A> toA, final String queryName,
          final Tuple<String, ?>... params) {
    return mlist((List<B>) createNamedQuery(em, queryName, params).getResultList()).map(toA).value();
  }

  /**
   * Find multiple objects with optional pagination.
   *
   * @param params
   *          the query parameters
   * @param toA
   *          map to the desired result object
   * @deprecated use {@link Queries#named#findAll(EntityManager, String, Option, Option, Object[])} instead
   */
  public static <A, B> List<A> findAll(EntityManager em, final Function<B, A> toA, Option<Integer> offset,
          Option<Integer> limit, final String queryName, final Tuple<String, ?>... params) {
    final Query q = createNamedQuery(em, queryName, params);
    for (Integer x : offset)
      q.setFirstResult(x);
    for (Integer x : limit)
      q.setMaxResults(x);
    return mlist((List<B>) q.getResultList()).map(toA).value();
  }

  /**
   * Create function to persist object <code>a</code> using {@link EntityManager#persist(Object)}.
   *
   * @deprecated use {@link Queries#persist(A)}
   */
  public static <A> Function<EntityManager, A> persist(final A a) {
    return new Function<EntityManager, A>() {
      @Override
      public A apply(EntityManager em) {
        em.persist(a);
        return a;
      }
    };
  }

  /**
   * Create function to merge an object <code>a</code> with the persisten context of the given entity manage.
   *
   * @deprecated use {@link Queries#merge(A)}
   */
  public static <A> Function<EntityManager, A> merge(final A a) {
    return new Function<EntityManager, A>() {
      @Override
      public A apply(EntityManager em) {
        em.merge(a);
        return a;
      }
    };
  }

  public static EntityManagerFactory newEntityManagerFactory(String emName, String vendor, String driver, String url,
          String user, String pwd, Map<String, ?> persistenceProps, PersistenceProvider pp) {
    // Set up the database
    final ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
    try {
      pooledDataSource.setDriverClass(driver);
    } catch (PropertyVetoException e) {
      throw new RuntimeException(e);
    }
    pooledDataSource.setJdbcUrl(url);
    pooledDataSource.setUser(user);
    pooledDataSource.setPassword(pwd);

    // Set up the persistence properties
    final Map<String, Object> props = new HashMap<String, Object>(persistenceProps);
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.target-database", vendor);

    final EntityManagerFactory emf = pp.createEntityManagerFactory(emName, props);
    if (emf == null) {
      throw new Error("Cannot create entity manager factory for persistence unit " + emName
              + ". Maybe you misspelled the name of the persistence unit?");
    }
    return emf;
  }

  /**
   * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
   *
   * @param emName
   *          name of the persistence unit (see META-INF/persistence.xml)
   */
  public static EntityManagerFactory newTestEntityManagerFactory(String emName) {
    return newEntityManagerFactory(
            emName,
            "Auto",
            "org.h2.Driver",
            "jdbc:h2:./target/db" + System.currentTimeMillis(),
            "sa",
            "sa",
            map(tuple("eclipselink.ddl-generation", "create-tables"),
                    tuple("eclipselink.ddl-generation.output-mode", "database")),
            new org.eclipse.persistence.jpa.PersistenceProvider());
  }

  /**
   * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
   * testing purposes.
   *
   * @param emName
   *          name of the persistence unit (see META-INF/persistence.xml)
   */
  public static PersistenceEnv newTestPersistenceEnv(String emName) {
    return newPersistenceEnvironment(newTestEntityManagerFactory(emName));
  }
}
