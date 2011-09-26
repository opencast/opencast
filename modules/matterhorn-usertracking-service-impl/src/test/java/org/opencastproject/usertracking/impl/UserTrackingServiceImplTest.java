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
package org.opencastproject.usertracking.impl;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class UserTrackingServiceImplTest {
  private ComboPooledDataSource pooledDataSource = null;
  private UserTrackingServiceImpl service = null;

  @Before
  public void setUp() throws Exception {
    // Set up the database
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Set up the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    // Set up the annotation service
    service = new UserTrackingServiceImpl();
    service.setPersistenceProvider(new PersistenceProvider());
    service.setPersistenceProperties(props);
    service.activate();
  }

  @After
  public void tearDown() throws Exception {
    service = null;
  }

  @Test
  public void test() throws Exception {
    UserActionImpl userAction = new UserActionImpl();
    userAction.setInpoint(10);
    userAction.setOutpoint(20);
    userAction.setMediapackageId("mp");
    userAction.setSessionId("session123");
    userAction.setType("play_media");
    userAction.setUserId("me"); // TODO: move this into the service implementation
    
    service.addUserAction(userAction);
    Long id = userAction.getId();
    
    // Ensure that, once persisted, the user action has an ID
    Assert.assertNotNull(id);
    
    // Ensure that, once persisted, the user action has a date created
    Assert.assertNotNull(userAction.getCreated());

    UserActionImpl fromDb = (UserActionImpl)service.getUserAction(id);
    Assert.assertNotNull(fromDb);
    Assert.assertEquals(userAction.getMediapackageId(), fromDb.getMediapackageId());
    
  }

}