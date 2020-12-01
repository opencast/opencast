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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;

import javax.sql.DataSource;

/** Registers {@link DataSource}. */
public class Activator implements BundleActivator {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);

  /** The default max idle time for the connection pool */
  private static final int DEFAULT_MAX_IDLE_TIME = 3600;

  private String rootDir;
  private ServiceRegistration<?> datasourceRegistration;
  private ComboPooledDataSource pooledDataSource;

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    // Use the configured storage directory
    rootDir = bundleContext.getProperty("org.opencastproject.storage.dir") + File.separator + "db";

    // Register the data source, defaulting to an embedded H2 database if DB configurations are not specified
    String jdbcDriver = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.driver"),
            "org.h2.Driver");
    String jdbcUrl = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.url"),
            "jdbc:h2:" + rootDir);
    if ("org.h2.Driver".equals(jdbcDriver)) {
      logger.warn("\n"
          + "######################################################\n"
          + "#                                                    #\n"
          + "# WARNING: Opencast is using an H2 database.         #\n"
          + "#          Never do this in production.              #\n"
          + "#                                                    #\n"
          + "#          For more information about database       #\n"
          + "#          configuration, see:                       #\n"
          + "#                                                    #\n"
          + "#          https://docs.opencast.org                 #\n"
          + "#                                                    #\n"
          + "######################################################");
    }
    String jdbcUser = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.user"), "sa");
    String jdbcPass = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pass"), "sa");

    Integer maxPoolSize = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.size"));
    Integer minPoolSize = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pool.min.size"));
    Integer acquireIncrement = getConfigProperty(
            bundleContext.getProperty("org.opencastproject.db.jdbc.pool.acquire.increment"));
    Integer maxStatements = getConfigProperty(
            bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.statements"));
    Integer loginTimeout = getConfigProperty(
            bundleContext.getProperty("org.opencastproject.db.jdbc.pool.login.timeout"));
    Integer maxIdleTime = getConfigProperty(
            bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.idle.time"));
    Integer maxConnectionAge = getConfigProperty(
            bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.connection.age"));

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass(jdbcDriver);
    pooledDataSource.setJdbcUrl(jdbcUrl);
    pooledDataSource.setUser(jdbcUser);
    pooledDataSource.setPassword(jdbcPass);
    if (minPoolSize != null)
      pooledDataSource.setMinPoolSize(minPoolSize);
    if (maxPoolSize != null)
      pooledDataSource.setMaxPoolSize(maxPoolSize);
    if (acquireIncrement != null)
      pooledDataSource.setAcquireIncrement(acquireIncrement);
    if (maxStatements != null)
      pooledDataSource.setMaxStatements(maxStatements);
    if (loginTimeout != null)
      pooledDataSource.setLoginTimeout(loginTimeout);

    // maxIdleTime should not be zero, otherwise the connection pool will hold on to stale connections
    // that have been closed by the database.
    if (maxIdleTime != null)
      pooledDataSource.setMaxIdleTime(maxIdleTime);
    else if (pooledDataSource.getMaxIdleTime() == 0) {
        logger.debug("Setting database connection pool max.idle.time to default of {}", DEFAULT_MAX_IDLE_TIME);
        pooledDataSource.setMaxIdleTime(DEFAULT_MAX_IDLE_TIME);
    }

    if (maxConnectionAge != null)
      pooledDataSource.setMaxConnectionAge(maxConnectionAge);

    Connection connection = null;
    try {
      logger.info("Testing connectivity to database at {}", jdbcUrl);
      connection = pooledDataSource.getConnection();
      Hashtable<String, String> dsProps = new Hashtable<>();
      dsProps.put("osgi.jndi.service.name", "jdbc/opencast");
      datasourceRegistration = bundleContext.registerService(DataSource.class.getName(), pooledDataSource, dsProps);
    } catch (SQLException e) {
      logger.error("Connection attempt to {} failed", jdbcUrl);
      logger.error("Exception: ", e);
      throw e;
    } finally {
      if (connection != null)
        connection.close();
    }

    logger.info("Database connection pool established at {}", jdbcUrl);
    logger.info("Database connection pool parameters: max.size={}, min.size={}, max.idle.time={}",
      pooledDataSource.getMaxPoolSize(), pooledDataSource.getMinPoolSize(), pooledDataSource.getMaxIdleTime());
    Statement statement = pooledDataSource.getConnection().createStatement();

    double random = Math.random() * 1000000;
    String tableName = "oc_temp_" + random;
    try {
      runUpdate(statement, "CREATE TABLE " + tableName + " ( id BIGINT NOT NULL, test BIGINT, PRIMARY KEY (id) );");
      runUpdate(statement, "INSERT INTO " + tableName + " VALUES (" + random + ", 0);");
      runUpdate(statement, "UPDATE " + tableName + " SET test = " + random + ";");
      ResultSet rs = statement.executeQuery("SELECT FROM " + tableName + ";");
      while (rs.next()) {
        long id = rs.getLong("id");
        long test = rs.getLong("test");
        if (id != random || test != random) {
          throw new RuntimeException("Unable to verify updating a table functions correctly");
        }
      }
      runUpdate(statement, "DELETE FROM " + tableName + " WHERE id = " + random + ";");
    } catch (Exception e) {
      throw new RuntimeException("Unable to verify SQL credentials have required permissions!");
    } finally {
      try {
        runUpdate(statement, "DROP TABLE " + tableName + ";");
      } catch (Exception e) {
        logger.warn("Unable to delete temp table {}, please remove this yourself!", tableName);
      }
    }

  }

  private void runUpdate(Statement statement, String sql) throws RuntimeException, SQLException {
    int affected = statement.executeUpdate(sql);
    if (affected != 1) {
      throw new RuntimeException("Unable to update on a testing table, check that your database user has the right permissions!");
    }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    logger.info("Shutting down database");
    if (datasourceRegistration != null)
      datasourceRegistration.unregister();
    logger.info("Shutting down connection pool");
    DataSources.destroy(pooledDataSource);
  }

  private String getConfigProperty(String config, String defaultValue) {
    return config == null ? defaultValue : config;
  }

  private Integer getConfigProperty(String config) {
    return config == null ? null : Integer.parseInt(config);
  }

}
