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

package org.opencastproject.util.persistencefn;

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;

import org.opencastproject.util.data.Collections;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.ProductBuilder;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Products;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

/**
 * Functions supporting persistence.
 */
public final class PersistenceUtil {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceUtil.class);

  private PersistenceUtil() {
  }

  public static final Map<String, Object> NO_PERSISTENCE_PROPS = java.util.Collections.emptyMap();

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>. A
   * {@link javax.persistence.spi.PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi
   * service. If you want to configure the factory please also register a map containing all properties under the name
   * <code>persistenceProps</code>. See
   * {@link javax.persistence.spi.PersistenceProvider#createEntityManagerFactory(String, java.util.Map)} for more
   * information about config maps.
   *
   * @param emName
   *         name of the persistence unit
   */
  public static EntityManagerFactory mkEntityManagerFactory(ComponentContext cc, String emName) {
    final PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    final Map pp = (Map) cc.locateService("persistenceProps");
    final Map persistenceProps = pp != null ? pp : NO_PERSISTENCE_PROPS;
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
  }

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>. A
   * {@link javax.persistence.spi.PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi
   * service. See {@link javax.persistence.spi.PersistenceProvider#createEntityManagerFactory(String, java.util.Map)}
   * for more information about config maps.
   *
   * @param emName
   *         name of the persistence unit
   * @param persistenceProps
   *         config map for the creation of an EntityManagerFactory
   */
  public static EntityManagerFactory mkEntityManagerFactory(ComponentContext cc, String emName, Map persistenceProps) {
    final PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
  }

  public static EntityManagerFactory mkEntityManagerFactory(
          String emName,
          String vendor,
          String driver,
          String url,
          String user,
          String pwd,
          Map<String, ?> persistenceProps,
          PersistenceProvider pp) {
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
    final Map<String, Object> props = new HashMap<>(persistenceProps);
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.target-database", vendor);

    final EntityManagerFactory emf = pp.createEntityManagerFactory(emName, props);
    if (emf == null) {
      throw new Error("Cannot create entity manager factory for persistence unit " + emName
                              + ". Maybe you misspelled the name of the persistence unit?");
    }
    return emf;
  }

  /** Create a new entity manager or return none, if the factory has already been closed. */
  public static Opt<EntityManager> mkEntityManager(EntityManagerFactory emf) {
    try {
      return Opt.some(emf.createEntityManager());
    } catch (IllegalStateException ex) {
      // factory is already closed
      return Opt.none();
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  //
  // test entity manager
  //

  private static final ProductBuilder P = com.entwinemedia.fn.Products.E;
  private static final Map<String, String> TEST_ENTITY_MANAGER_PROPS = $(
          P.p2("eclipselink.ddl-generation", "create-tables"),
          P.p2("eclipselink.ddl-generation.output-mode", "database"))
          .group(Products.<String>p2_1(), Products.<String>p2_2());
  private static final Map<String, String> TEST_ENTITY_MANAGER_LOGGING_PROPS = $(
          P.p2("eclipselink.logging.level.sql", "FINE"),
          P.p2("eclipselink.logging.parameters", "true"))
          .group(Products.<String>p2_1(), Products.<String>p2_2());

  /**
   * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
   * If you want to turn on SQL logging see {@link #mkTestEntityManagerFactory(String)}
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static EntityManagerFactory mkTestEntityManagerFactory(String emName) {
    return mkTestEntityManagerFactory(emName, false);
  }

  /**
   * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   * @param withSqlLogging
   *         turn on EclipseLink SQL logging
   */
  public static EntityManagerFactory mkTestEntityManagerFactory(String emName, boolean withSqlLogging) {
    return mkEntityManagerFactory(
            emName,
            "Auto",
            "org.h2.Driver",
            "jdbc:h2:./target/db" + System.currentTimeMillis(),
            "sa",
            "sa",
            withSqlLogging
                    ? Collections.merge(TEST_ENTITY_MANAGER_PROPS, TEST_ENTITY_MANAGER_LOGGING_PROPS)
                    : TEST_ENTITY_MANAGER_PROPS,
            mkTestPersistenceProvider());
  }

  /**
   * Create an entity manager for unit tests configured by the following system properties.
   * <ul>
   * <li>-Dtest-database-url, JDBC URL, defaults to H2 in-memory database</li>
   * <li>-Dtest-database-user, defaults to 'matterhorn'</li>
   * <li>-Dtest-database-password, defaults to 'matterhorn'</li>
   * <li>-Dsql-logging=[true|false], defaults to 'false', turns on SQL logging to the console</li>
   * <li>-Dkeep-database=[true|false], defaults to 'false', keep an existing database or recreate at startup. Not used with H2.</li>
   * </ul>
   * Currently only MySQL is recognized.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static EntityManagerFactory mkTestEntityManagerFactoryFromSystemProperties(String emName) {
    final Opt<String> dbUrl = Opt.nul(System.getProperty("test-database-url"));
    final boolean withSqlLogging = Boolean.getBoolean("sql-logging");
    final boolean keepDatabase = Boolean.getBoolean("keep-database");
    final String dbUser = Opt.nul(System.getProperty("test-database-user")).getOr("matterhorn");
    final String dbPwd = Opt.nul(System.getProperty("test-database-password")).getOr("matterhorn");
    for (String url : dbUrl) {
      try {
        final String dbType = new URI(new URI(url).getSchemeSpecificPart()).getScheme();
        if (eq("mysql", dbType)) {
          logger.info(format("Use MySQL database\n"
                                     + " test-database-url=%s\n"
                                     + " test-database-user=%s\n"
                                     + " test-database-password=%s\n"
                                     + " sql-logging=%s\n"
                                     + " keep-database=%s",
                             url, dbUser, dbPwd, withSqlLogging, keepDatabase));
          return mkMySqlTestEntityManagerFactory(emName, url, dbUser, dbPwd, withSqlLogging, keepDatabase);
        }
      } catch (Exception ignore) {
      }
    }
    logger.info(format("Use H2 database\n sql-logging=%s", withSqlLogging));
    return mkTestEntityManagerFactory(emName, withSqlLogging);
  }

  /**
   * Create a new entity manager factory using a MySQL database for testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   * @param jdbcUri
   *         the JDBC URI of the database, e.g. jdbc:mysql://localhost/test_database
   * @param user
   *         the database user
   * @param pwd
   *         the user's password
   * @param withSqlLogging
   *         turn on SQL logging
   * @param keepDatabase
   *         recreate or keep the database at startup
   */
  public static EntityManagerFactory mkMySqlTestEntityManagerFactory(
          String emName,
          String jdbcUri,
          String user,
          String pwd,
          boolean withSqlLogging,
          boolean keepDatabase) {
    final Map<String, String> testEntityManagerProps = new HashMap<>();
    testEntityManagerProps.put("eclipselink.ddl-generation", keepDatabase ? "none" : "drop-and-create-tables");
    testEntityManagerProps.put("eclipselink.ddl-generation.output-mode", "database");
    if (withSqlLogging) {
      testEntityManagerProps.putAll(PersistenceUtil.TEST_ENTITY_MANAGER_LOGGING_PROPS);
    }
    return PersistenceUtil.mkEntityManagerFactory(
            emName,
            "MySql",
            "com.mysql.jdbc.Driver",
            jdbcUri,
            user,
            pwd,
            testEntityManagerProps,
            PersistenceUtil.mkTestPersistenceProvider());
  }

  //
  //
  //

  /** Create a new persistence provider for unit tests. */
  public static PersistenceProvider mkTestPersistenceProvider() {
    return new org.eclipse.persistence.jpa.PersistenceProvider();
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
  public static Opt<SQLException> testConnection(DataSource ds) {
    try (Connection tryConnect = ds.getConnection()) {
      return Opt.none();
    } catch (SQLException e) {
      return Opt.some(e);
    }
  }

  /**
   * Try to get the database metadata from an entity manager. Return none, if the metadata
   * cannot be retrieved, e.g. because the entity manager does not expose them.
   */
  public static Opt<DatabaseMetaData> getDatabaseMetadata(EntityManager em) {
    try {
      final Connection c = em.unwrap(Connection.class);
      return Opt.some(c.getMetaData());
    } catch (Exception e) {
      return Opt.none();
    }
  }

  /** {@link #getDatabaseMetadata(EntityManager)} as a function. */
  public static final Fn<EntityManager, Opt<DatabaseMetaData>> getDatabaseMetadata = new Fn<EntityManager, Opt<DatabaseMetaData>>() {
    @Override public Opt<DatabaseMetaData> apply(EntityManager em) {
      return getDatabaseMetadata(em);
    }
  };

  /**
   * Get the database vendor from some metadata.
   *
   * @return {@link DatabaseVendor#UNKNOWN} in case the database is not known or an arbitrary error occurs
   */
  public static DatabaseVendor getVendor(DatabaseMetaData m) {
    try {
      switch (m.getDatabaseProductName()) {
        case "H2":
          return DatabaseVendor.H2;
        case "MySQL":
          return DatabaseVendor.MYSQL;
        case "Postgres":
          return DatabaseVendor.POSTGRES;
        default:
          return DatabaseVendor.UNKNOWN;
      }
    } catch (SQLException e) {
      return DatabaseVendor.UNKNOWN;
    }
  }

  public enum DatabaseVendor {
    H2, MYSQL, POSTGRES, UNKNOWN
  }
}
