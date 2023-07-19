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

package org.opencastproject.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

public final class DBTestEnv {
  private static final DBSessionFactoryImpl dbSessionFactory = new DBSessionFactoryImpl();

  static {
    dbSessionFactory.setMaxTransactionRetries(2);
  }

  private DBTestEnv() {
  }

  public static DBSession newDBSession(String emName) {
    return newDBSession(newEntityManagerFactory(emName));
  }

  public static DBSession newDBSession(EntityManagerFactory emf) {
    return dbSessionFactory.createSession(emf);
  }

  /**
   * @return A {@code DBSessionFactory} singleton.
   */
  public static DBSessionFactory getDbSessionFactory() {
    return dbSessionFactory;
  }

  /**
   * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
   *
   * @param emName name of the persistence unit (see META-INF/persistence.xml)
   */
  public static EntityManagerFactory newEntityManagerFactory(String emName) {
    Map<String, String> persistenceProperties = new HashMap<>();
    persistenceProperties.put(PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.DROP_AND_CREATE);
    persistenceProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE, PersistenceUnitProperties.DDL_DATABASE_GENERATION);
    return newEntityManagerFactory(emName, "Auto", "org.h2.Driver", "jdbc:h2:./target/db" + System.currentTimeMillis(),
        "sa", "sa", persistenceProperties, newPersistenceProvider());
  }

  /**
   * Create a new entity manager factory for testing purposes.
   *
   * @param emName name of the persistence unit (see META-INF/persistence.xml)
   * @param vendor DB vendor name.
   * @param driver DB driver name.
   * @param url DB URL.
   * @param user DB user name.
   * @param pwd DB password.
   * @param persistenceProps Persistence properties.
   * @param pp JPA implementation.
   */
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

  /**
   * Create a new persistence provider for unit tests.
   */
  public static PersistenceProvider newPersistenceProvider() {
    return new org.eclipse.persistence.jpa.PersistenceProvider();
  }
}
