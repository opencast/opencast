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

package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;
import org.opencastproject.usertracking.api.UserSession;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class UserActionListImplTest {

  /**
   * Tests that adding events to a UserActionList works using the basic .add method
   */
  @Test
  public void testBasicAddition() {
    UserSession us1 = new UserSessionImpl();
    us1.setSessionId("4");
    us1.setUserId("testing user");
    us1.setUserIp("127.0.0.1");

    UserSession us2 = new UserSessionImpl();
    us2.setSessionId("6");
    us2.setUserId("testing user 2");
    us2.setUserIp("127.0.0.5");

    UserAction ua1 = new UserActionImpl();
    ua1.setId(4L);
    ua1.setInpoint(90);
    ua1.setIsPlaying(true);
    ua1.setMediapackageId("false");
    ua1.setOutpoint(100);
    ua1.setSession(us1);
    ua1.setType("test");

    UserAction ua2 = new UserActionImpl();
    ua2.setId(6L);
    ua2.setInpoint(5);
    ua2.setIsPlaying(false);
    ua2.setMediapackageId("other");
    ua2.setOutpoint(20);
    ua2.setSession(us2);
    ua2.setType("other test");

    UserActionList ual1 = new UserActionListImpl();
    ual1.add(ua1);
    ual1.setTotal(1);

    Assertions.assertEquals(1, ual1.getTotal());
    Assertions.assertEquals(1, ual1.getUserActions().size());
    Assertions.assertEquals(new Long(4), ual1.getUserActions().get(0).getId());
    Assertions.assertEquals(90, ual1.getUserActions().get(0).getInpoint());
    Assertions.assertEquals(true, ual1.getUserActions().get(0).getIsPlaying());
    Assertions.assertEquals("false", ual1.getUserActions().get(0).getMediapackageId());
    Assertions.assertEquals(100, ual1.getUserActions().get(0).getOutpoint());
    Assertions.assertEquals("4", ual1.getUserActions().get(0).getSession().getSessionId());
    Assertions.assertEquals("test", ual1.getUserActions().get(0).getType());
    Assertions.assertEquals("testing user", ual1.getUserActions().get(0).getSession().getUserId());
    Assertions.assertEquals("127.0.0.1", ual1.getUserActions().get(0).getSession().getUserIp());
    Assertions.assertEquals(10, ual1.getUserActions().get(0).getLength());

    UserActionList ual2 = new UserActionListImpl();
    ual2.add(ua2);
    ual2.setTotal(1);

    Assertions.assertEquals(1, ual2.getTotal());
    Assertions.assertEquals(1, ual2.getUserActions().size());
    Assertions.assertEquals(new Long(6), ual2.getUserActions().get(0).getId());
    Assertions.assertEquals(5, ual2.getUserActions().get(0).getInpoint());
    Assertions.assertEquals(false, ual2.getUserActions().get(0).getIsPlaying());
    Assertions.assertEquals("other", ual2.getUserActions().get(0).getMediapackageId());
    Assertions.assertEquals(20, ual2.getUserActions().get(0).getOutpoint());
    Assertions.assertEquals("6", ual2.getUserActions().get(0).getSession().getSessionId());
    Assertions.assertEquals("other test", ual2.getUserActions().get(0).getType());
    Assertions.assertEquals("testing user 2", ual2.getUserActions().get(0).getSession().getUserId());
    Assertions.assertEquals("127.0.0.5", ual2.getUserActions().get(0).getSession().getUserIp());
    Assertions.assertEquals(15, ual2.getUserActions().get(0).getLength());
  }

  /**
   * Tests that adding events to a UserActionList works using the list add method
   */
  @Test
  public void testListAddition() {
    UserSession us1 = new UserSessionImpl();
    us1.setSessionId("4");
    us1.setUserId("testing user");
    us1.setUserIp("127.0.0.1");

    UserSession us2 = new UserSessionImpl();
    us2.setSessionId("6");
    us2.setUserId("testing user 2");
    us2.setUserIp("127.0.0.5");

    UserAction ua1 = new UserActionImpl();
    ua1.setId(4L);
    ua1.setInpoint(90);
    ua1.setIsPlaying(true);
    ua1.setMediapackageId("false");
    ua1.setOutpoint(100);
    ua1.setSession(us1);
    ua1.setType("test");

    UserAction ua2 = new UserActionImpl();
    ua2.setId(6L);
    ua2.setInpoint(5);
    ua2.setIsPlaying(false);
    ua2.setMediapackageId("other");
    ua2.setOutpoint(20);
    ua2.setSession(us2);
    ua2.setType("other test");

    List<UserAction> list = new LinkedList<UserAction>();
    list.add(ua1);
    list.add(ua2);

    UserActionList ual = new UserActionListImpl();
    ual.add(list);
    ual.setTotal(2);

    Assertions.assertEquals(2, ual.getTotal());
    Assertions.assertEquals(2, ual.getUserActions().size());
    Assertions.assertEquals(new Long(4), ual.getUserActions().get(0).getId());
    Assertions.assertEquals(90, ual.getUserActions().get(0).getInpoint());
    Assertions.assertEquals(true, ual.getUserActions().get(0).getIsPlaying());
    Assertions.assertEquals("false", ual.getUserActions().get(0).getMediapackageId());
    Assertions.assertEquals(100, ual.getUserActions().get(0).getOutpoint());
    Assertions.assertEquals("4", ual.getUserActions().get(0).getSession().getSessionId());
    Assertions.assertEquals("test", ual.getUserActions().get(0).getType());
    Assertions.assertEquals("testing user", ual.getUserActions().get(0).getSession().getUserId());
    Assertions.assertEquals("127.0.0.1", ual.getUserActions().get(0).getSession().getUserIp());


    Assertions.assertEquals(new Long(6), ual.getUserActions().get(1).getId());
    Assertions.assertEquals(5, ual.getUserActions().get(1).getInpoint());
    Assertions.assertEquals(false, ual.getUserActions().get(1).getIsPlaying());
    Assertions.assertEquals("other", ual.getUserActions().get(1).getMediapackageId());
    Assertions.assertEquals(20, ual.getUserActions().get(1).getOutpoint());
    Assertions.assertEquals("6", ual.getUserActions().get(1).getSession().getSessionId());
    Assertions.assertEquals("other test", ual.getUserActions().get(1).getType());
    Assertions.assertEquals("testing user 2", ual.getUserActions().get(1).getSession().getUserId());
    Assertions.assertEquals("127.0.0.5", ual.getUserActions().get(1).getSession().getUserIp());
    Assertions.assertEquals(15, ual.getUserActions().get(1).getLength());
  }
}
