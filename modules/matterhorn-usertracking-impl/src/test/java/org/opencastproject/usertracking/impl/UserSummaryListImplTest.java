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

import org.opencastproject.usertracking.api.UserSummary;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;

public class UserSummaryListImplTest {

  private UserSummary userSummary1 = null;
  private UserSummary userSummary2 = null;
  private UserSummary userSummary3 = null;

  @Before
  public void setUp() {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(0);

    userSummary1 = new UserSummaryImpl();
    userSummary1.setLast(c.getTime());
    userSummary1.setLength(1);
    userSummary1.setSessionCount(1);
    userSummary1.setUniqueMediapackages(1);
    userSummary1.setUserId("test1");

    c.add(Calendar.HOUR, 1);
    userSummary2 = new UserSummaryImpl();
    userSummary2.setLast(c.getTime());
    userSummary2.setLength(2);
    userSummary2.setSessionCount(2);
    userSummary2.setUniqueMediapackages(2);
    userSummary2.setUserId("test2");

    c.add(Calendar.HOUR, 1);
    userSummary3 = new UserSummaryImpl();
    userSummary3.setLast(c.getTime());
    userSummary3.setLength(3);
    userSummary3.setSessionCount(3);
    userSummary3.setUniqueMediapackages(3);
    userSummary3.setUserId("test1");
  }

  /**
   * Tests adding UserSummarys individually
   */
  @Test
  public void testIndividualAdding() {
    UserSummaryListImpl l1 = new UserSummaryListImpl();
    Assert.assertEquals(0, l1.getTotal());
    Assert.assertEquals(0, l1.getUserSummaries().size());

    Calendar c1 = Calendar.getInstance();
    c1.setTimeInMillis(0);
    Calendar c2 = (Calendar) c1.clone();
    c2.add(Calendar.HOUR, 1);
    Calendar c3 = (Calendar) c2.clone();
    c3.add(Calendar.HOUR, 1);

    l1.add(userSummary1);
    Assert.assertEquals(1, l1.getTotal());
    Assert.assertEquals(1, l1.getUserSummaries().size());
    Assert.assertEquals(c1.getTime(), l1.getUserSummaries().get(0).getLast());
    Assert.assertEquals(1, l1.getUserSummaries().get(0).getLength());
    Assert.assertEquals(1, l1.getUserSummaries().get(0).getSessionCount());
    Assert.assertEquals(1, l1.getUserSummaries().get(0).getUniqueMediapackages());
    Assert.assertEquals("test1", l1.getUserSummaries().get(0).getUserId());

    l1.add(userSummary2);
    Assert.assertEquals(2, l1.getTotal());
    Assert.assertEquals(2, l1.getUserSummaries().size());

    Assert.assertEquals(c1.getTime(), l1.getUserSummaries().get(0).getLast());
    Assert.assertEquals(1, l1.getUserSummaries().get(0).getLength());
    Assert.assertEquals(1, l1.getUserSummaries().get(0).getSessionCount());
    Assert.assertEquals(1, l1.getUserSummaries().get(0).getUniqueMediapackages());
    Assert.assertEquals("test1", l1.getUserSummaries().get(0).getUserId());

    Assert.assertEquals(c2.getTime(), l1.getUserSummaries().get(1).getLast());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getLength());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getSessionCount());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getUniqueMediapackages());
    Assert.assertEquals("test2", l1.getUserSummaries().get(1).getUserId());

    l1.add(userSummary3);
    Assert.assertEquals(2, l1.getTotal());
    Assert.assertEquals(2, l1.getUserSummaries().size());

    Assert.assertEquals(c3.getTime(), l1.getUserSummaries().get(0).getLast());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getLength());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getSessionCount());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getUniqueMediapackages());
    Assert.assertEquals("test1", l1.getUserSummaries().get(0).getUserId());

    Assert.assertEquals(c2.getTime(), l1.getUserSummaries().get(1).getLast());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getLength());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getSessionCount());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getUniqueMediapackages());
    Assert.assertEquals("test2", l1.getUserSummaries().get(1).getUserId());
  }

  /**
   * Tests adding UserSummarys in a collection
   */
  @Test
  public void testCollectionAdding() {
    UserSummaryListImpl l1 = new UserSummaryListImpl();
    Assert.assertEquals(0, l1.getTotal());
    Assert.assertEquals(0, l1.getUserSummaries().size());

    Calendar c1 = Calendar.getInstance();
    c1.setTimeInMillis(0);
    Calendar c2 = (Calendar) c1.clone();
    c2.add(Calendar.HOUR, 1);
    Calendar c3 = (Calendar) c2.clone();
    c3.add(Calendar.HOUR, 1);

    Collection<UserSummary> list = new LinkedList<UserSummary>();
    list.add(userSummary1);
    list.add(userSummary2);
    list.add(userSummary3);

    l1.add(list);

    Assert.assertEquals(2, l1.getTotal());
    Assert.assertEquals(2, l1.getUserSummaries().size());

    Assert.assertEquals(c3.getTime(), l1.getUserSummaries().get(0).getLast());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getLength());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getSessionCount());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getUniqueMediapackages());
    Assert.assertEquals("test1", l1.getUserSummaries().get(0).getUserId());

    Assert.assertEquals(c2.getTime(), l1.getUserSummaries().get(1).getLast());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getLength());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getSessionCount());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getUniqueMediapackages());
    Assert.assertEquals("test2", l1.getUserSummaries().get(1).getUserId());
  }

  /**
   * Tests adding UserSummarys in a collection, in the reverse order
   */
  @Test
  public void testCollectionAddingInReverse() {
    UserSummaryListImpl l1 = new UserSummaryListImpl();
    Assert.assertEquals(0, l1.getTotal());
    Assert.assertEquals(0, l1.getUserSummaries().size());

    Calendar c1 = Calendar.getInstance();
    c1.setTimeInMillis(0);
    Calendar c2 = (Calendar) c1.clone();
    c2.add(Calendar.HOUR, 1);
    Calendar c3 = (Calendar) c2.clone();
    c3.add(Calendar.HOUR, 1);

    Collection<UserSummary> list = new LinkedList<UserSummary>();
    list.add(userSummary3);
    list.add(userSummary2);
    list.add(userSummary1);

    l1.add(list);

    Assert.assertEquals(2, l1.getTotal());
    Assert.assertEquals(2, l1.getUserSummaries().size());

    Assert.assertEquals(c3.getTime(), l1.getUserSummaries().get(0).getLast());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getLength());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getSessionCount());
    Assert.assertEquals(4, l1.getUserSummaries().get(0).getUniqueMediapackages());
    Assert.assertEquals("test1", l1.getUserSummaries().get(0).getUserId());

    Assert.assertEquals(c2.getTime(), l1.getUserSummaries().get(1).getLast());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getLength());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getSessionCount());
    Assert.assertEquals(2, l1.getUserSummaries().get(1).getUniqueMediapackages());
    Assert.assertEquals("test2", l1.getUserSummaries().get(1).getUserId());
  }
}
