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

package org.opencastproject.oaipmh;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.Ignore;

import javax.persistence.EntityManagerFactory;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

// Ignore this class since it doesn't contain any tests, just helpers
@Ignore
public final class TestUtil {

  private TestUtil() {
  }

  /**
   * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
   *
   * @param emName
   *          name of the persistence unit (see META-INF/persistence.xml)
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

    PersistenceProvider pp = new PersistenceProvider();
    return pp.createEntityManagerFactory(emName, persistenceProps);
  }
}
