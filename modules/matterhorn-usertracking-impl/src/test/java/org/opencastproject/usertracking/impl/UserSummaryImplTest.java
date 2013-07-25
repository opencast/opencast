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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class UserSummaryImplTest {

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
   * Tests rapidly ingesting a user summary
   */
  @Test
  public void testIngest() {
    String userId = "test";
    long sessionCount = 4L;
    long mediapackageCount = 3L;
    long length = 40L;
    Date created = new Date();

    Object[] full = {userId, sessionCount, mediapackageCount, length, created};
    Object[] missingId = {1, sessionCount, mediapackageCount, length, created};
    Object[] missingIdwNull = {null, sessionCount, mediapackageCount, length, created};
    Object[] missingSession = {userId, "fail", mediapackageCount, length, created};
    Object[] missingSessionwNull = {userId, null, mediapackageCount, length, created};
    Object[] missingMediapackage = {userId, sessionCount, "fail", length, created};
    Object[] missingMediapackagewNull = {userId, sessionCount, null, length, created};
    Object[] missingLength = {userId, sessionCount, mediapackageCount, "fail", created};
    Object[] missingLengthwNull = {userId, sessionCount, mediapackageCount, null, created};
    Object[] missingCreated = {userId, sessionCount, mediapackageCount, length, "fail"};
    Object[] missingCreatedwNull = {userId, sessionCount, mediapackageCount, length, null};
    Object[] badData = {};

    verifyIngest(full, userId, sessionCount, mediapackageCount, length, created);
    verifyIngest(badData, "Empty UserId", 0, 0, 0, new Date());
    verifyIngestWithNull(missingId, missingIdwNull, "Empty UserId", sessionCount, mediapackageCount, length, created);
    verifyIngestWithNull(missingSession, missingSessionwNull, userId, 0, mediapackageCount, length, created);
    verifyIngestWithNull(missingMediapackage, missingMediapackagewNull, userId, sessionCount, 0, length, created);
    verifyIngestWithNull(missingLength, missingLengthwNull, userId, sessionCount, mediapackageCount, 0, created);
    verifyIngestWithNull(missingCreated, missingCreatedwNull, userId, sessionCount, mediapackageCount, length, new Date());
  }

  private void verifyIngestWithNull(Object[] withoutNull, Object[] withNull, String userId, long sessionCount, long mediapackageCount, long length, Date created) {
    verifyIngest(withoutNull, userId, sessionCount, mediapackageCount, length, created);
    verifyIngest(withNull, userId, sessionCount, mediapackageCount, length, created);
  }

  private void verifyIngest(Object[] bundle, String userId, long sessionCount, long mediapackageCount, long length, Date created) {
    UserSummaryImpl us = new UserSummaryImpl();
    us.ingest(bundle);
    Assert.assertEquals(userId, us.getUserId());
    Assert.assertEquals(sessionCount, us.getSessionCount());
    Assert.assertEquals(mediapackageCount, us.getUniqueMediapackages());
    Assert.assertEquals(length, us.getLength());
    //This is lame, but we don't want to have dates differing by a millisecond breaking things.
    Assert.assertTrue((created.getTime() - us.getLast().getTime()) <= 1000);
  }
}
