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

import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class UserActionListImplTest {

  /**
   * Tests that adding events to a UserActionList works using the basic .add method
   */
  @Test
  public void testBasicAddition() {
    UserAction ua1 = new UserActionImpl();
    ua1.setId(4L);
    ua1.setInpoint(90);
    ua1.setIsPlaying(true);
    ua1.setMediapackageId("false");
    ua1.setOutpoint(100);
    ua1.setSessionId("4");
    ua1.setType("test");
    ua1.setUserId("testing user");
    ua1.setUserIp("127.0.0.1");
    
    UserAction ua2 = new UserActionImpl();
    ua2.setId(6L);
    ua2.setInpoint(5);
    ua2.setIsPlaying(false);
    ua2.setMediapackageId("other");
    ua2.setOutpoint(20);
    ua2.setSessionId("6");
    ua2.setType("other test");
    ua2.setUserId("testing user 2");
    ua2.setUserIp("127.0.0.5");

    UserActionList ual1 = new UserActionListImpl();
    ual1.add(ua1);
    ual1.setTotal(1);

    Assert.assertEquals(1, ual1.getTotal());
    Assert.assertEquals(1, ual1.getUserActions().size());
    Assert.assertEquals(new Long(4), ual1.getUserActions().get(0).getId());
    Assert.assertEquals(90, ual1.getUserActions().get(0).getInpoint());
    Assert.assertEquals(true, ual1.getUserActions().get(0).getIsPlaying());
    Assert.assertEquals("false", ual1.getUserActions().get(0).getMediapackageId());
    Assert.assertEquals(100, ual1.getUserActions().get(0).getOutpoint());
    Assert.assertEquals("4", ual1.getUserActions().get(0).getSessionId());
    Assert.assertEquals("test", ual1.getUserActions().get(0).getType());
    Assert.assertEquals("testing user", ual1.getUserActions().get(0).getUserId());
    Assert.assertEquals("127.0.0.1", ual1.getUserActions().get(0).getUserIp());
    Assert.assertEquals(10, ual1.getUserActions().get(0).getLength());
    
    UserActionList ual2 = new UserActionListImpl();
    ual2.add(ua2);
    ual2.setTotal(1);

    Assert.assertEquals(1, ual2.getTotal());
    Assert.assertEquals(1, ual2.getUserActions().size());
    Assert.assertEquals(new Long(6), ual2.getUserActions().get(0).getId());
    Assert.assertEquals(5, ual2.getUserActions().get(0).getInpoint());
    Assert.assertEquals(false, ual2.getUserActions().get(0).getIsPlaying());
    Assert.assertEquals("other", ual2.getUserActions().get(0).getMediapackageId());
    Assert.assertEquals(20, ual2.getUserActions().get(0).getOutpoint());
    Assert.assertEquals("6", ual2.getUserActions().get(0).getSessionId());
    Assert.assertEquals("other test", ual2.getUserActions().get(0).getType());
    Assert.assertEquals("testing user 2", ual2.getUserActions().get(0).getUserId());
    Assert.assertEquals("127.0.0.5", ual2.getUserActions().get(0).getUserIp());
    Assert.assertEquals(15, ual2.getUserActions().get(0).getLength());
  }

  /**
   * Tests that adding events to a UserActionList works using the list add method
   */
  @Test
  public void testListAddition() {
    UserAction ua1 = new UserActionImpl();
    ua1.setId(4L);
    ua1.setInpoint(90);
    ua1.setIsPlaying(true);
    ua1.setMediapackageId("false");
    ua1.setOutpoint(100);
    ua1.setSessionId("4");
    ua1.setType("test");
    ua1.setUserId("testing user");
    ua1.setUserIp("127.0.0.1");
    
    UserAction ua2 = new UserActionImpl();
    ua2.setId(6L);
    ua2.setInpoint(5);
    ua2.setIsPlaying(false);
    ua2.setMediapackageId("other");
    ua2.setOutpoint(20);
    ua2.setSessionId("6");
    ua2.setType("other test");
    ua2.setUserId("testing user 2");
    ua2.setUserIp("127.0.0.5");

    List<UserAction> list = new LinkedList<UserAction>();
    list.add(ua1);
    list.add(ua2);

    UserActionList ual = new UserActionListImpl();
    ual.add(list);
    ual.setTotal(2);

    Assert.assertEquals(2, ual.getTotal());
    Assert.assertEquals(2, ual.getUserActions().size());
    Assert.assertEquals(new Long(4), ual.getUserActions().get(0).getId());
    Assert.assertEquals(90, ual.getUserActions().get(0).getInpoint());
    Assert.assertEquals(true, ual.getUserActions().get(0).getIsPlaying());
    Assert.assertEquals("false", ual.getUserActions().get(0).getMediapackageId());
    Assert.assertEquals(100, ual.getUserActions().get(0).getOutpoint());
    Assert.assertEquals("4", ual.getUserActions().get(0).getSessionId());
    Assert.assertEquals("test", ual.getUserActions().get(0).getType());
    Assert.assertEquals("testing user", ual.getUserActions().get(0).getUserId());
    Assert.assertEquals("127.0.0.1", ual.getUserActions().get(0).getUserIp());


    Assert.assertEquals(new Long(6), ual.getUserActions().get(1).getId());
    Assert.assertEquals(5, ual.getUserActions().get(1).getInpoint());
    Assert.assertEquals(false, ual.getUserActions().get(1).getIsPlaying());
    Assert.assertEquals("other", ual.getUserActions().get(1).getMediapackageId());
    Assert.assertEquals(20, ual.getUserActions().get(1).getOutpoint());
    Assert.assertEquals("6", ual.getUserActions().get(1).getSessionId());
    Assert.assertEquals("other test", ual.getUserActions().get(1).getType());
    Assert.assertEquals("testing user 2", ual.getUserActions().get(1).getUserId());
    Assert.assertEquals("127.0.0.5", ual.getUserActions().get(1).getUserIp());
    Assert.assertEquals(15, ual.getUserActions().get(1).getLength());
  }
}
