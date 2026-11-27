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
import org.opencastproject.usertracking.api.UserSession;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    Assertions.assertEquals(Long.valueOf(4), ua.getId());
    Assertions.assertEquals(90, ua.getInpoint());
    Assertions.assertEquals(true, ua.getIsPlaying());
    Assertions.assertEquals("false", ua.getMediapackageId());
    Assertions.assertEquals(100, ua.getOutpoint());
    Assertions.assertEquals("4", ua.getSession().getSessionId());
    Assertions.assertEquals("test", ua.getType());
    Assertions.assertEquals("testing user", ua.getSession().getUserId());
    Assertions.assertEquals("127.0.0.1", ua.getSession().getUserIp());
    Assertions.assertEquals(10, ua.getLength());
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

    Assertions.assertEquals(Long.valueOf(4), ua.getId());
    Assertions.assertEquals(100, ua.getInpoint());
    Assertions.assertEquals(true, ua.getIsPlaying());
    Assertions.assertEquals("false", ua.getMediapackageId());
    Assertions.assertEquals(90, ua.getOutpoint());
    Assertions.assertEquals("4", ua.getSession().getSessionId());
    Assertions.assertEquals("test", ua.getType());
    Assertions.assertEquals("testing user", ua.getSession().getUserId());
    Assertions.assertEquals("127.0.0.1", ua.getSession().getUserIp());
    Assertions.assertEquals(-10, ua.getLength());
  }

}
