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

package org.opencastproject.adminui.endpoint;

import static org.opencastproject.pm.api.Person.person;

import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Blacklistable;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestBlacklistEndpoint extends BlacklistEndpoint {
  /** The start date to use that is before the actual start date, it is 9:00:00pm Feb 1st, 2012 GMT */
  public static final Date BEFORE_START_DATE = new Date(1328130000000L);
  /** The end date to use that is before the actual start date for tests, it is 10:00:00pm Feb 1st, 2012 GMT */
  public static final Date BEFORE_END_DATE = new Date(1328089500000L);
  /** The start date to use for tests, it is 10:01:06pm Feb 1st, 2012 GMT */
  public static final Date START_DATE = new Date(1328133666000L);
  /** The end date to use for tests, it is 11:01:06pm Feb 1st, 2012 GMT */
  public static final Date END_DATE = new Date(1328137266000L);
  /** The start date to use for tests, it is 11:05:00pm Feb 1st, 2012 GMT */
  public static final Date AFTER_START_DATE = new Date(1328137500000L);
  /** The end date to use for tests, it is 11:45:00pm Feb 1st, 2012 GMT */
  public static final Date AFTER_END_DATE = new Date(1328139900000L);

  public TestBlacklistEndpoint() throws Exception {
    User user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    ParticipationManagementDatabase persistence = EasyMock.createNiceMock(ParticipationManagementDatabase.class);
    // TODO prepare persistence for tests
    preparePersistence(persistence);
    EasyMock.replay(persistence);

    this.setParticipationPersistence(persistence);
    this.activate(null);
  }

  private void preparePersistence(ParticipationManagementDatabase pm) throws Exception {
    pm.deletePeriod(0L);
    EasyMock.expectLastCall().andThrow(new NotFoundException());

    Person blackListedPerson = person("Blacklisted Person", "blacklisted@test.ch");
    Person nonBlacklistedPerson = person("Non Blacklisted", "nonblacklisted@test.ch");
    EasyMock.expect(pm.getPersons()).andReturn(Collections.list(blackListedPerson, nonBlacklistedPerson)).anyTimes();

    Course blacklistedCourseOne = new Course("Blacklisted Course 1");
    blacklistedCourseOne.setDescription("Blacklisted Course Description 1");
    blacklistedCourseOne.setName("Blacklisted Course Name 1");
    Course blacklistedCourseTwo = new Course("Blacklisted Course 2");
    blacklistedCourseTwo.setDescription("Blacklisted Course Description 2");
    blacklistedCourseTwo.setName("Blacklisted Course Name 2");
    Course blacklistedCourseThree = new Course("Blacklisted Course 3");
    blacklistedCourseThree.setDescription("Blacklisted Course Description 3");
    blacklistedCourseThree.setName("Blacklisted Course Name 3");
    EasyMock.expect(pm.getCourses())
            .andReturn(Collections.list(blacklistedCourseOne, blacklistedCourseTwo, blacklistedCourseThree)).anyTimes();

    Room blacklistedRoom = new Room("Black listed room");
    Room nonBlacklistedRoom = new Room("Non Black listed room");
    EasyMock.expect(pm.getRooms()).andReturn(Collections.list(blacklistedRoom, nonBlacklistedRoom)).anyTimes();

    CaptureAgent blackListedCaptureAgent = new CaptureAgent(blacklistedRoom, "blacklistedCA");
    CaptureAgent nonBlackListedCaptureAgent = new CaptureAgent(blacklistedRoom, "nonBlacklistedCA");

    List<Person> emptyPersonList = new ArrayList<Person>();
    List<Person> blacklistedStaff = new ArrayList<Person>();
    blacklistedStaff.add(blackListedPerson);
    List<Person> nonBlacklistedStaff = new ArrayList<Person>();
    nonBlacklistedStaff.add(nonBlacklistedPerson);

    // Recordings black listed by the person in their staff.
    Recording firstPersonRecording = Recording.recording("110", "Blacklisted By Person 1", blacklistedStaff,
            blacklistedCourseOne, nonBlacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);
    Recording secondPersonRecording = Recording.recording("111", "Blacklisted By Person 2", blacklistedStaff,
            blacklistedCourseOne, nonBlacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);
    Recording thirdPersonRecording = Recording.recording("112", "Blacklisted By Person 3", blacklistedStaff,
            blacklistedCourseTwo, nonBlacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);
    Recording fourPersonRecording = Recording.recording("113", "Blacklisted By Person 4", blacklistedStaff,
            blacklistedCourseThree, nonBlacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);

    // Recordings black listed by their room.
    Recording firstRoomRecording = Recording.recording("114", "Blacklisted By Room 1", nonBlacklistedStaff,
            blacklistedCourseOne, blacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);
    Recording secondRoomRecording = Recording.recording("115", "Blacklisted By Room 2", nonBlacklistedStaff,
            blacklistedCourseOne, blacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);
    Recording thirdRoomRecording = Recording.recording("116", "Blacklisted By Room 3", nonBlacklistedStaff,
            blacklistedCourseTwo, blacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);

    // Recording that is before the start time.
    Recording beforeTimeRecording = Recording.recording("117", "Before Start Time", blacklistedStaff,
            blacklistedCourseTwo, blacklistedRoom, new Date(), BEFORE_START_DATE, BEFORE_END_DATE, emptyPersonList,
            blackListedCaptureAgent);

    // Recording that is after the start time.
    Recording afterTimeRecording = Recording.recording("118", "After Start Time", blacklistedStaff,
            blacklistedCourseTwo, blacklistedRoom, new Date(), AFTER_START_DATE, AFTER_END_DATE, emptyPersonList,
            blackListedCaptureAgent);

    Recording notBlacklistedRecording = Recording.recording("119", "Not Blacklisted", nonBlacklistedStaff,
            blacklistedCourseTwo, nonBlacklistedRoom, new Date(), START_DATE, END_DATE, emptyPersonList,
            nonBlackListedCaptureAgent);

    EasyMock.expect(pm.getBlacklistByPeriodId(EasyMock.anyLong())).andReturn(new Blacklist(1L, blackListedPerson))
            .anyTimes();
    EasyMock.expect(pm.getPeriod(EasyMock.anyLong())).andReturn(Period.period(new Date(), new Date())).anyTimes();
    EasyMock.expect(pm.getRoom(EasyMock.anyLong())).andReturn(blacklistedRoom).anyTimes();
    EasyMock.expect(pm.getPerson(EasyMock.anyLong())).andReturn(blackListedPerson).anyTimes();
    EasyMock.expect(pm.updatePeriod(EasyMock.anyObject(Period.class))).andReturn(Period.period(new Date(), new Date()))
            .anyTimes();
    EasyMock.expect(pm.updateBlacklist(EasyMock.anyObject(Blacklist.class)))
            .andReturn(new Blacklist(1L, blackListedPerson)).anyTimes();
    EasyMock.expect(pm.findBlacklists(EasyMock.anyObject(Blacklistable.class)))
            .andReturn(Collections.list(new Blacklist(1L, blackListedPerson))).anyTimes();
  }
}
