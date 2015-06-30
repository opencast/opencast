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

package org.opencastproject.scheduler.impl.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.util.NotFoundException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests persistent storage.
 *
 */
public class SchedulerServiceDatabaseImplTest {

  private ComboPooledDataSource pooledDataSource;
  private SchedulerServiceDatabaseImpl schedulerDatabase;
  private DublinCoreCatalogService dcService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    schedulerDatabase.setPersistenceProvider(new PersistenceProvider());
    schedulerDatabase.setPersistenceProperties(props);
    dcService = new DublinCoreCatalogService();
    schedulerDatabase.setDublinCoreService(dcService);
    schedulerDatabase.activate(null);
  }

  @Test
  public void testStoringAndDeleting() throws Exception {
    DublinCoreCatalog catalog = dcService.newInstance();
    catalog.add(DublinCore.PROPERTY_TITLE, "Test");
    catalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");

    schedulerDatabase.storeEvents(catalog);
    schedulerDatabase.deleteEvent(1);
  }

  @Test
  public void testMergingAndRetrieving() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "Test");
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Test");
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "2");

    schedulerDatabase.storeEvents(firstCatalog);
    schedulerDatabase.updateEvent(firstCatalog);
    assertTrue("Should contain only one event", schedulerDatabase.getAllEvents().length == 1);

    schedulerDatabase.storeEvents(secondCatalog);
    assertTrue("Should contain two events", schedulerDatabase.getAllEvents().length == 2);
  }

  @Test
  public void testMetadataAdding() throws Exception {
    DublinCoreCatalog catalog = dcService.newInstance();
    catalog.add(DublinCore.PROPERTY_TITLE, "Test");
    catalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");

    Properties properties = new Properties();
    properties.put("properties.test", "test");

    schedulerDatabase.storeEvents(catalog);
    schedulerDatabase.updateEventWithMetadata(1, properties);

    Properties caProperties = schedulerDatabase.getEventMetadata(1);
    assertNotNull("Metadata properties should be stored", caProperties);

    try {
      schedulerDatabase.updateEventWithMetadata(2, properties);
      fail("Should fail with not found exception");
    } catch (NotFoundException e) {
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    schedulerDatabase.deactivate(null);
    pooledDataSource.close();
  }

}
