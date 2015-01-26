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

import org.opencastproject.usertracking.api.FootprintList;
import org.opencastproject.usertracking.api.Report;
import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;
import org.opencastproject.usertracking.api.UserSession;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    service.destroy();
    service = null;
  }

  /**
   * Test to make sure that the detailed tracking switch works
   * @throws Exception
   */
  @Test
  public void testDetailedTracking() throws Exception {
    Properties props = new Properties();

    service.updated(null);
    Assert.assertFalse(service.getUserTrackingEnabled());

    service.updated(props);
    Assert.assertFalse(service.getUserTrackingEnabled());

    props.setProperty("org.opencastproject.usertracking.detailedtrack", "true");
    service.updated(props);
    Assert.assertTrue(service.getUserTrackingEnabled());

    props.setProperty("org.opencastproject.usertracking.detailedtrack", "false");
    service.updated(props);
    Assert.assertFalse(service.getUserTrackingEnabled());
  }

  /**
   * Test footprint functionality
   * @throws Exception
   */
  @Test
  public void testfootprints() throws Exception {
    FootprintList list = getFootprintList("mp", null, 1);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    list = getFootprintList("mp", "me", 1);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);

    //Create the initial viewer/user event
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 10, 20);

    //Sanity checks
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Assert that the correct things are in the correct places.
    list = getFootprintList("mp", null, 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 20, 0);
    list = getFootprintList("mp", "someone else", 1);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    list = getFootprintList("mp", "me", 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 20, 0);

    //Create a different viewer/user event
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session456", "mp", "someone else", "127.0.01", 560, 720);

    //Sanity checks
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Assert that the correct things are in the correct places.
    list = getFootprintList("mp", null, 5);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 20, 0);
    verifyFootprintViewsAndPositions(list, 3, 560, 1);
    verifyFootprintViewsAndPositions(list, 4, 720, 0);
    list = getFootprintList("mp", "someone else", 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 560, 1);
    verifyFootprintViewsAndPositions(list, 2, 720, 0);
    list = getFootprintList("mp", "me", 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 20, 0);

    //Update the first viewer/user event
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 20, 30);

    //Sanity checks
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Assert that the correct things are in the correct places.
    list = getFootprintList("mp", null, 5);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 30, 0);
    verifyFootprintViewsAndPositions(list, 3, 560, 1);
    verifyFootprintViewsAndPositions(list, 4, 720, 0);
    list = getFootprintList("mp", "someone else", 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 560, 1);
    verifyFootprintViewsAndPositions(list, 2, 720, 0);
    list = getFootprintList("mp", "me", 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 30, 0);

    //Skip the second viewer to a new point in the video
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session456", "mp", "someone else", "127.0.01", 950, 960);

    //Sanity checks
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Assert that the correct things are in the correct places.
    list = getFootprintList("mp", null, 7);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 30, 0);
    verifyFootprintViewsAndPositions(list, 3, 560, 1);
    verifyFootprintViewsAndPositions(list, 4, 720, 0);
    verifyFootprintViewsAndPositions(list, 5, 950, 1);
    verifyFootprintViewsAndPositions(list, 6, 960, 0);
    list = getFootprintList("mp", "someone else", 5);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 560, 1);
    verifyFootprintViewsAndPositions(list, 2, 720, 0);
    verifyFootprintViewsAndPositions(list, 3, 950, 1);
    verifyFootprintViewsAndPositions(list, 4, 960, 0);
    list = getFootprintList("mp", "me", 3);
    verifyFootprintViewsAndPositions(list, 0, 0, 0);
    verifyFootprintViewsAndPositions(list, 1, 10, 1);
    verifyFootprintViewsAndPositions(list, 2, 30, 0);
  }

  /**
   * Tests basic user action lists and reports
   * @throws Exception
   */
  @Test
  public void testBasicUserActionLists() throws Exception {
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 10, 20);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionLists(1, 0, 0, 1);
    verifyUserActionLists(1, 0, 1, 1);
    verifyUserActionLists(0, 1, 0, 1);
    verifyUserActionLists(0, 1, 1, 1);
    verifyUserActionLists(1, 0, 10, 1);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 20, 30);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionLists(1, 0, 0, 1);
    verifyUserActionLists(1, 0, 1, 1);
    verifyUserActionLists(0, 1, 0, 1);
    verifyUserActionLists(0, 1, 1, 1);
    verifyUserActionLists(1, 0, 10, 1);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 40, 50);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionLists(2, 0, 0, 2);
    verifyUserActionLists(1, 0, 1, 2);
    verifyUserActionLists(1, 1, 0, 2);
    verifyUserActionLists(1, 1, 1, 2);
    verifyUserActionLists(2, 0, 10, 2);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 50, 60);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionLists(2, 0, 0, 2);
    verifyUserActionLists(1, 0, 1, 2);
    verifyUserActionLists(1, 1, 0, 2);
    verifyUserActionLists(1, 1, 1, 2);
    verifyUserActionLists(2, 0, 10, 2);
  }

  /**
   * Tests user action lists and reports based on type
   * @throws Exception
   */
  @Test
  public void testUserActionListsByType() throws Exception {
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 10, 20);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 0, 1);
    verifyUserActionListsByType("other", 0, 0, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 1, 1);
    verifyUserActionListsByType("other", 0, 0, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 0, 1, 0, 1);
    verifyUserActionListsByType("other", 0, 1, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 0, 1, 1, 1);
    verifyUserActionListsByType("other", 0, 1, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 10, 1);
    verifyUserActionListsByType("other", 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 20, 30);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 0, 1);
    verifyUserActionListsByType("other", 0, 0, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 1, 1);
    verifyUserActionListsByType("other", 0, 0, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 0, 1, 0, 1);
    verifyUserActionListsByType("other", 0, 1, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 0, 1, 1, 1);
    verifyUserActionListsByType("other", 0, 1, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 10, 1);
    verifyUserActionListsByType("other", 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 40, 50);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 2, 0, 0, 2);
    verifyUserActionListsByType("other", 0, 0, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 1, 2);
    verifyUserActionListsByType("other", 0, 0, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 1, 0, 2);
    verifyUserActionListsByType("other", 0, 1, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 1, 1, 2);
    verifyUserActionListsByType("other", 0, 1, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 2, 0, 10, 2);
    verifyUserActionListsByType("other", 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 50, 60);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 2, 0, 0, 2);
    verifyUserActionListsByType("other", 0, 0, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 0, 1, 2);
    verifyUserActionListsByType("other", 0, 0, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 1, 0, 2);
    verifyUserActionListsByType("other", 0, 1, 0, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 1, 1, 1, 2);
    verifyUserActionListsByType("other", 0, 1, 1, 0);

    verifyUserActionListsByType(UserTrackingServiceImpl.FOOTPRINT_KEY, 2, 0, 10, 2);
    verifyUserActionListsByType("other", 0, 0, 10, 0);
  }

  /**
   * Tests user action lists and reports based on type and mediapackage
   * @throws Exception
   */
  @Test
  public void testUserActionListsByTypeAndMediapackage() throws Exception {
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 10, 20);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 0, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 1, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 0, 1, 0, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 0, 1, 1, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 10, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 20, 30);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 0, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 1, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 0, 1, 0, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 0, 1, 1, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 10, 1);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 40, 50);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 2, 0, 0, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 1, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 1, 0, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 1, 1, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 2, 0, 10, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 50, 60);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 2, 0, 0, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 0, 1, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 1, 0, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 0, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 0, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 1, 1, 1, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 1, 1, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 1, 1, 0);

    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "mp", 2, 0, 10, 2);
    verifyUserActionListsByTypeAndMediapackage(UserTrackingServiceImpl.FOOTPRINT_KEY, "not-mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "mp", 0, 0, 10, 0);
    verifyUserActionListsByTypeAndMediapackage("other", "not-mp", 0, 0, 10, 0);
  }

  /**
   * Tests user action lists and reports based on type and day
   * @throws Exception
   */
  @Test
  public void testUserActionsListsByTypeAndDay() throws Exception {
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 10, 20);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    DateFormat df = new SimpleDateFormat("yyyyMMdd");
    Calendar c1 = Calendar.getInstance();
    Calendar c2 = Calendar.getInstance();
    c2.add(Calendar.DAY_OF_YEAR, -1);

    String now = df.format(c1.getTime());
    String yesterday = df.format(c2.getTime());
    testUserActionListsByTypeAndDay(now, yesterday);
  }

  /**
   * Tests user action lists and reports based on type and detailed day+time
   * @throws Exception
   */
  @Test
  public void testUserActionsListsByTypeAndDayWithTime() throws Exception {
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 10, 20);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    Calendar c1 = Calendar.getInstance();
    Calendar c2 = Calendar.getInstance();
    c2.add(Calendar.DAY_OF_YEAR, -1);

    String now = df.format(c1.getTime());
    String yesterday = df.format(c2.getTime());
    testUserActionListsByTypeAndDay(now, yesterday);
  }

  /**
   * Wrapper which tests user action lists and reports based on type and day
   * @param now The current time (yyyyMMdd or yyyyMMddHHmm)
   * @param yesterday Yesterday (yyyyMMdd or yyyyMMddHHmm)
   * @throws Exception
   */
  private void testUserActionListsByTypeAndDay(String now, String yesterday) throws Exception {

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 0, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 1, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 0, 1, 0, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 0, 1, 1, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 10, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 20, 30);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 0, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 1, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 0, 1, 0, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 0, 1, 1, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 10, 1);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 40, 50);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 2, 0, 0, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 1, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 1, 0, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 1, 1, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 2, 0, 10, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 10, 0);

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session123", "mp", "me", "127.0.0.1", 50, 60);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 2, 0, 0, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 0, 1, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 1, 0, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 0, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 0, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 1, 1, 1, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 1, 1, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 1, 1, 0);

    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, now, 2, 0, 10, 2);
    verifyUserActionListsByTypeAndDay(UserTrackingServiceImpl.FOOTPRINT_KEY, yesterday, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", now, 0, 0, 10, 0);
    verifyUserActionListsByTypeAndDay("other", yesterday, 0, 0, 10, 0);
  }

  /**
   * Similar to the above, but tests to make sure things are working with other event types than footprints
   * @throws Exception
   */
  @Test
  public void testArbitraryAction() throws Exception {
    createAndVerifyUserAction("arbitraryType", "session123", "mp", "me", "127.0.0.1", 10, 20);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    verifyUserActionLists(1, 0, 0, 1);
    verifyUserActionLists(1, 0, 1, 1);
    verifyUserActionLists(0, 1, 0, 1);
    verifyUserActionLists(0, 1, 1, 1);
    verifyUserActionLists(1, 0, 10, 1);
  }

  /**
   * Tests to make sure reports with date restrictions work as expected.
   * @throws Exception
   */
  @Test
  public void testUserActionsByDay() throws Exception {
    Calendar c1 = Calendar.getInstance();
    Date nowD = c1.getTime();

    c1.add(Calendar.DAY_OF_YEAR, -1);
    Date yesterdayD = c1.getTime();

    c1.add(Calendar.DAY_OF_YEAR, 3);
    Date tomorrowD = c1.getTime();

    testUserActionsByDayWithFormat(yesterdayD, nowD, tomorrowD, "yyyyMMdd");
  }

  /**
   * Tests to make sure reports with date and time restrictions work as expected.
   * @throws Exception
   */
  @Test
  public void testUserActionsByDayWithTime() throws Exception {
    Calendar c1 = Calendar.getInstance();
    Date nowD = c1.getTime();

    c1.add(Calendar.DAY_OF_YEAR, -1);
    Date yesterdayD = c1.getTime();

    c1.add(Calendar.DAY_OF_YEAR, 3);
    Date tomorrowD = c1.getTime();

    testUserActionsByDayWithFormat(yesterdayD, nowD, tomorrowD, "yyyyMMddHHmm");
  }

  /**
   * Wrapper method to test user action reports based on different time formats
   * @throws Exception
   */
  private void testUserActionsByDayWithFormat(Date yesterdayD, Date todayD, Date tomorrowD, String format) throws Exception {
    DateFormat df = new SimpleDateFormat(format);
    String today = df.format(todayD);

    //Create an event that happens tomorrow, and check to make sure it does not appear in the stats for today
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session1", "mp", "me", "127.0.0.1", 0, 10, tomorrowD);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    verifyUserActionListsByDay(today, 0, 0, 0, 0);
    verifyUserActionListsByDay(today, 0, 0, 1, 0);
    verifyUserActionListsByDay(today, 0, 1, 0, 0);
    verifyUserActionListsByDay(today, 0, 1, 1, 0);

    //Create an event yesterday and check that it appears in the stats for today
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session1", "mp", "me", "127.0.0.1", 10, 20, yesterdayD);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    verifyUserActionListsByDay(today, 0, 0, 0, 0);
    verifyUserActionListsByDay(today, 0, 0, 1, 0);
    verifyUserActionListsByDay(today, 0, 1, 0, 0);
    verifyUserActionListsByDay(today, 0, 1, 1, 0);

    //Create an event today and check that it appears today
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session2", "mp", "me", "127.0.0.1", 20, 30, todayD);
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //verifyUserActionListsByDay(now, 0, 0, 0, 1);
    verifyUserActionListsByDay(today, 1, 0, 1, 1);
    verifyUserActionListsByDay(today, 0, 1, 0, 1);
    verifyUserActionListsByDay(today, 0, 1, 1, 1);

    //Create an event for another user, and check that it appears today
    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session3", "other", "someone else", "127.0.01", 20, 30, todayD);
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(1, service.getViews("other"));

    //verifyUserActionListsByDay(now, 0, 0, 0, 2);
    verifyUserActionListsByDay(today, 1, 0, 1, 2);
    verifyUserActionListsByDay(today, 1, 1, 0, 2);
    verifyUserActionListsByDay(today, 1, 1, 1, 2);
  }

  /**
   * Tests report generation
   * @throws Exception
   */
  @Test
  public void testDistinctEpisodeCount() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, -12);
    Date backHalf = cal.getTime();

    cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, 12);
    Date forwardHalf = cal.getTime();

    //Check against an empty session list
    Report rep = service.getReport(0, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session1", "mp", "me", "127.0.0.1", 20, 30, backHalf);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Sanity checks against a single viewer, mediapackage, and session

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session2", "mp", "me", "127.0.0.1", 40, 45, forwardHalf);
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Sanity checks against a single viewer, mediapackage, and two sessions

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(15, rep.getPlayed());
    Assert.assertEquals(2, rep.getViews());

    rep = service.getReport(1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(15, rep.getPlayed());
    Assert.assertEquals(2, rep.getViews());

    rep = service.getReport(1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session3", "mp", "someone else", "127.0.01", 50, 55, forwardHalf);
    Assert.assertEquals(3, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Sanity checks against two viewers, mediapackage, and three sessions

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(20, rep.getPlayed());
    Assert.assertEquals(3, rep.getViews());

    rep = service.getReport(1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(20, rep.getPlayed());
    Assert.assertEquals(3, rep.getViews());

    rep = service.getReport(1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session4", "other", "someone else", "127.0.01", 0, 10, forwardHalf);
    Assert.assertEquals(3, service.getViews("mp"));
    Assert.assertEquals(1, service.getViews("other"));

    //Sanity checks against two viewers, two mediapackages, and four sessions

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(2, rep.getTotal());
    Assert.assertEquals(30, rep.getPlayed());
    Assert.assertEquals(4, rep.getViews());

    rep = service.getReport(1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(2, rep.getTotal());
    Assert.assertEquals(30, rep.getPlayed());
    Assert.assertEquals(4, rep.getViews());

    rep = service.getReport(1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());
  }

  /**
   * Tests report generation with date and time restrictions
   * @throws Exception
   */
  @Test
  public void testDistinctEpisodeCountWithDateRangesWithTime() throws Exception {
    DateFormat df = new SimpleDateFormat("yyyyMMddHHmm");

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -2);
    String ystd = df.format(cal.getTime());

    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -1);
    Date backHalf = cal.getTime();

    cal = Calendar.getInstance();
    String now = df.format(cal.getTime());

    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 1);
    Date forwardHalf = cal.getTime();

    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 2);
    String tmw = df.format(cal.getTime());

    testDistinctEpisodeCountWithDateRanges(ystd, backHalf, now, forwardHalf, tmw);
  }

  /**
   * Tests report generation with date restrictions
   * @throws Exception
   */
  @Test
  public void testDistinctEpisodeCountWithDateRangesWithoutTime() throws Exception {
    DateFormat df = new SimpleDateFormat("yyyyMMdd");

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -2);
    String minusTwo = df.format(cal.getTime());

    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -1);
    Date minusOne = cal.getTime();

    cal = Calendar.getInstance();
    String today = df.format(cal.getTime());

    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 1);
    Date plusOne = cal.getTime();

    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 2);
    String plusTwo = df.format(cal.getTime());

    testDistinctEpisodeCountWithDateRanges(minusTwo, minusOne, today, plusOne, plusTwo);
  }

  /**
   * Wrapper function which actually tests report generation with date and time restrictions
   * @throws Exception
   */
  private void testDistinctEpisodeCountWithDateRanges(String minusTwo, Date minusOne, String today, Date plusOne, String plusTwo) throws Exception {

    //Check against an empty session list
    Report rep = service.getReport(minusTwo, plusTwo, 0, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session1", "mp", "me", "127.0.0.1", 20, 30, minusOne);
    Assert.assertEquals(1, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Sanity checks against a single viewer, mediapackage, and session

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(minusTwo, plusTwo, 0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(minusTwo, plusTwo, 1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(minusTwo, plusTwo, 0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(minusTwo, plusTwo, 1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(minusTwo, today, 0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(today, plusTwo, 1, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session2", "mp", "me", "127.0.0.1", 40, 45, plusOne);
    Assert.assertEquals(2, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Sanity checks against a single viewer, mediapackage, and two sessions

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(minusTwo, plusTwo, 0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(15, rep.getPlayed());
    Assert.assertEquals(2, rep.getViews());

    rep = service.getReport(minusTwo, plusTwo, 1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(minusTwo, plusTwo, 0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(15, rep.getPlayed());
    Assert.assertEquals(2, rep.getViews());

    rep = service.getReport(minusTwo, plusTwo, 1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(minusTwo, today, 0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(today, plusTwo, 0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(5, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session3", "mp", "someone else", "127.0.01", 50, 55, plusOne);
    Assert.assertEquals(3, service.getViews("mp"));
    Assert.assertEquals(0, service.getViews("other"));

    //Sanity checks against two viewers, mediapackage, and three sessions

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(20, rep.getPlayed());
    Assert.assertEquals(3, rep.getViews());

    rep = service.getReport(1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    rep = service.getReport(0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(20, rep.getPlayed());
    Assert.assertEquals(3, rep.getViews());

    rep = service.getReport(1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(0, rep.getTotal());
    Assert.assertEquals(0, rep.getPlayed());
    Assert.assertEquals(0, rep.getViews());

    createAndVerifyUserAction(UserTrackingServiceImpl.FOOTPRINT_KEY, "session4", "other", "someone else", "127.0.01", 0, 10, plusOne);
    Assert.assertEquals(3, service.getViews("mp"));
    Assert.assertEquals(1, service.getViews("other"));

    //Sanity checks against two viewers, two mediapackages, and four sessions

    //Test various limit and offset combinations
    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=328730
    rep = service.getReport(0, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(2, rep.getTotal());
    Assert.assertEquals(30, rep.getPlayed());
    Assert.assertEquals(4, rep.getViews());

    rep = service.getReport(1, 0);
    Assert.assertEquals(0, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());

    rep = service.getReport(0, 10);
    Assert.assertEquals(10, rep.getLimit());
    Assert.assertEquals(0, rep.getOffset());
    Assert.assertEquals(2, rep.getTotal());
    Assert.assertEquals(30, rep.getPlayed());
    Assert.assertEquals(4, rep.getViews());

    rep = service.getReport(1, 1);
    Assert.assertEquals(1, rep.getLimit());
    Assert.assertEquals(1, rep.getOffset());
    Assert.assertEquals(1, rep.getTotal());
    Assert.assertEquals(10, rep.getPlayed());
    Assert.assertEquals(1, rep.getViews());
  }

  /**
   * Gets the footprint list, performs some asserts
   * @throws Exception
   */
  private FootprintList getFootprintList(String mediapackageId, String userId, int expectedFootprints) {
    FootprintList list ;
    if (StringUtils.trim(userId) == null) {
      list = service.getFootprints(mediapackageId, null);
    } else {
      list = service.getFootprints(mediapackageId, userId);
    }
    //There's always a 0th footprint, so this should never be zero!
    Assert.assertEquals(expectedFootprints, list.getTotal());
    Assert.assertEquals(expectedFootprints, list.getFootprints().size());
    return list;
  }

  /**
   * Gets the footprint list, performs some asserts
   * @throws Exception
   */
  private void verifyFootprintViewsAndPositions(FootprintList list, int counter, int position, int views) {
    Assert.assertEquals(position, list.getFootprints().get(counter).getPosition());
    Assert.assertEquals(views, list.getFootprints().get(counter).getViews());
  }

  /**
   * Gets the user action list, performs some asserts
   * @throws Exception
   */
  private void verifyUserActionLists(int count, int offset, int limit, int total) {
    UserActionList ual = service.getUserActions(offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());
  }

  /**
   * Gets the user action list based on type, performs some asserts
   * @throws Exception
   */
  private void verifyUserActionListsByType(String type, int count, int offset, int limit, int total) {
    UserActionList ual = service.getUserActionsByType(type, offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());
  }

  /**
   * Gets the user action list based on type and mediapackage, performs some asserts
   * @throws Exception
   */
  private void verifyUserActionListsByTypeAndMediapackage(String type, String mp, int count, int offset, int limit, int total) {
    UserActionList ual = service.getUserActionsByTypeAndMediapackageId(type, mp, offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());

    //This method does the same thing, except it orders by creation date.
    ual = service.getUserActionsByTypeAndMediapackageIdByDate(type, mp, offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());

    //This method does the same thing, except it orders by the reverse of the creation date.
    ual = service.getUserActionsByTypeAndMediapackageIdByDescendingDate(type, mp, offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());
  }

  /**
   * Gets the user action list based on type and date, performs some asserts
   * @throws Exception
   */
  private void verifyUserActionListsByTypeAndDay(String type, String day, int count, int offset, int limit, int total) {
    UserActionList ual = service.getUserActionsByTypeAndDay(type, day, offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());
  }

  /**
   * Gets the user action list based on date, performs some asserts
   * @throws Exception
   */
  private void verifyUserActionListsByDay(String day, int count, int offset, int limit, int total) {
    UserActionList ual = service.getUserActionsByDay(day, offset, limit);
    Assert.assertEquals(limit, ual.getLimit());
    Assert.assertEquals(offset, ual.getOffset());
    Assert.assertEquals(total, ual.getTotal());
    Assert.assertEquals(count, ual.getUserActions().size());
  }

  /**
   * Creates and verifies a user action with the current date
   * @throws Exception
   */
  private UserAction createAndVerifyUserAction(String type, String sessionId, String mediapackageId, String userId, String userIp, int inpoint, int outpoint) throws Exception {
    return createAndVerifyUserAction(type, sessionId, mediapackageId, userId, userIp, inpoint, outpoint, new Date());
  }

  /**
   * Creates and verifies a user action with an arbitrary date
   * @throws Exception
   */
  private UserAction createAndVerifyUserAction(String type, String sessionId, String mediapackageId, String userId, String userIp, int inpoint, int outpoint, Date createdDate) throws Exception {
    UserSession userSession = createUserSession(sessionId, userId, userIp);
    UserAction userAction = createUserAction(type, mediapackageId, inpoint, outpoint, createdDate, userSession);
    if (UserTrackingServiceImpl.FOOTPRINT_KEY.equals(type)) {
      userAction = service.addUserFootprint(userAction, userSession);
    } else {
      userAction = service.addUserTrackingEvent(userAction, userSession);
    }
    Long id = userAction.getId();

    // Ensure that, once persisted, the user action has an ID
    Assert.assertNotNull(id);

    //Sanity checks
    UserActionImpl fromDb = (UserActionImpl) service.getUserAction(id);
    Assert.assertNotNull(fromDb);
    Assert.assertNotNull(fromDb.getCreated());
    Assert.assertEquals(id, fromDb.getId());
    Assert.assertEquals(userAction.getMediapackageId(), fromDb.getMediapackageId());
    Assert.assertEquals(userAction.getSession().getSessionId(), fromDb.getSession().getSessionId());
    Assert.assertEquals(userAction.getType(), fromDb.getType());
    Assert.assertEquals(userAction.getSession().getUserId(), fromDb.getSession().getUserId());
    Assert.assertEquals(userAction.getSession().getUserIp(), fromDb.getSession().getUserIp());
    Assert.assertEquals(userAction.getInpoint(), fromDb.getInpoint());
    Assert.assertEquals(userAction.getOutpoint(), fromDb.getOutpoint());
    Assert.assertEquals(userAction.getIsPlaying(), fromDb.getIsPlaying());
    Assert.assertEquals(userAction.getSession().getUserIp(), fromDb.getSession().getUserIp());
    return userAction;
  }

  /**
   * Creates a user session
   * @throws Exception
   */
  private UserSession createUserSession(String sessionId, String userId, String userIp) {
    UserSession userSession = new UserSessionImpl();
    userSession.setSessionId(sessionId);
    userSession.setUserId(userId);
    userSession.setUserIp(userIp);
    return userSession;
  }

  /**
   * Creates a user action with an arbitrary date
   * @throws Exception
   */
  private UserAction createUserAction(String type, String mediapackageId, int inpoint, int outpoint, Date createdDate, UserSession userSession) {
    UserAction userAction = new UserActionImpl();
    userAction.setInpoint(inpoint);
    userAction.setOutpoint(outpoint);
    userAction.setMediapackageId(mediapackageId);
    userAction.setSession(userSession);
    userAction.setType(type);
    ((UserActionImpl) userAction).setCreated(createdDate);
    return userAction;
  }
}
