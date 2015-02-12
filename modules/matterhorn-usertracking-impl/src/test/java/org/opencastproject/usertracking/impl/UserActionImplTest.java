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
import org.opencastproject.usertracking.api.UserSession;

import org.junit.Assert;
import org.junit.Test;

public class UserActionImplTest {

  /**
   * Ensures a positive length user action calculates its length correctly.
   */
  @Test
  public void testForwardLength() {
    UserSession us = new UserSessionImpl();
    us.setSessionId("4");
    us.setUserId("testing user");
    us.setUserIp("127.0.0.1");

    UserAction ua = new UserActionImpl();
    ua.setId(4L);
    ua.setInpoint(90);
    ua.setIsPlaying(true);
    ua.setMediapackageId("false");
    ua.setOutpoint(100);
    ua.setSession(us);
    ua.setType("test");

    Assert.assertEquals(new Long(4), ua.getId());
    Assert.assertEquals(90, ua.getInpoint());
    Assert.assertEquals(true, ua.getIsPlaying());
    Assert.assertEquals("false", ua.getMediapackageId());
    Assert.assertEquals(100, ua.getOutpoint());
    Assert.assertEquals("4", ua.getSession().getSessionId());
    Assert.assertEquals("test", ua.getType());
    Assert.assertEquals("testing user", ua.getSession().getUserId());
    Assert.assertEquals("127.0.0.1", ua.getSession().getUserIp());
    Assert.assertEquals(10, ua.getLength());
  }

  /**
   * Ensures a negative length user action calculates its length correctly.
   */
  @Test
  public void testBackwardLength() {
    UserSession us = new UserSessionImpl();
    us.setSessionId("4");
    us.setUserId("testing user");
    us.setUserIp("127.0.0.1");

    UserAction ua = new UserActionImpl();
    ua.setId(4L);
    ua.setInpoint(100);
    ua.setIsPlaying(true);
    ua.setMediapackageId("false");
    ua.setOutpoint(90);
    ua.setSession(us);
    ua.setType("test");

    Assert.assertEquals(new Long(4), ua.getId());
    Assert.assertEquals(100, ua.getInpoint());
    Assert.assertEquals(true, ua.getIsPlaying());
    Assert.assertEquals("false", ua.getMediapackageId());
    Assert.assertEquals(90, ua.getOutpoint());
    Assert.assertEquals("4", ua.getSession().getSessionId());
    Assert.assertEquals("test", ua.getType());
    Assert.assertEquals("testing user", ua.getSession().getUserId());
    Assert.assertEquals("127.0.0.1", ua.getSession().getUserIp());
    Assert.assertEquals(-10, ua.getLength());
  }

}
