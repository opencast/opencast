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

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.userdirectory.JpaRole;
import org.opencastproject.userdirectory.JpaUser;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestUsersEndpoint extends UsersEndpoint {

  private ParticipationManagementDatabase pmService;
  private UserDirectoryService userDirectoryService;
  private static ArrayList<User> users;
  private User user1;
  private User user2;
  private User user3;
  private User user4;

  public TestUsersEndpoint() throws Exception {

    pmService = EasyMock.createNiceMock(ParticipationManagementDatabase.class);
    userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    users = new ArrayList<User>();

    JpaOrganization organization = new JpaOrganization("org", "org", new HashMap<String, Integer>(), "ADMIN",
            "ANONYMOUS", new HashMap<String, String>());

    Set<JpaRole> roles = new HashSet<JpaRole>();
    roles.add(new JpaRole("ADMIN", organization));
    roles.add(new JpaRole("USER", organization));

    user1 = new JpaUser("user1", "pass", organization, "user1", "email1", "provider1", true, roles);
    user2 = new JpaUser("user2", "pass", organization, "user2", "email2", "provider1", true);
    user3 = new JpaUser("user3", "pass", organization, "user3", "email3", "provider1", true);
    user4 = new JpaUser("user4", "pass", organization, "user1", "email4", "provider1", true);

    Person person1 = new Person(1L, user1.getName(), user1.getEmail(), new ArrayList<PersonType>());
    Person person2 = new Person(2L, user2.getName(), user2.getEmail(), new ArrayList<PersonType>());
    Person person3 = new Person(3L, user3.getName(), user3.getEmail(), new ArrayList<PersonType>());
    Person person4 = new Person(4L, user4.getName(), user4.getEmail(), new ArrayList<PersonType>());

    users.add(user1);
    users.add(user2);
    users.add(user3);
    users.add(user4);

    List<Period> periods1 = new ArrayList<Period>();
    periods1.add(new Period(Option.some(12L), fromUTC("2012-12-12T12:12:12Z"), fromUTC("2025-12-24T12:12:12Z"), Option
            .<String> none(), Option.<String> none()));
    periods1.add(new Period(Option.some(14L), fromUTC("2026-12-12T12:12:12Z"), fromUTC("2026-12-12T16:12:12Z"), Option
            .<String> none(), Option.<String> none()));
    List<Period> periods2 = new ArrayList<Period>();
    periods2.add(new Period(Option.some(12L), fromUTC("2028-12-12T12:12:12Z"), fromUTC("2029-12-24T12:12:12Z"), Option
            .<String> none(), Option.<String> none()));
    periods2.add(new Period(Option.some(14L), fromUTC("2006-12-12T12:12:12Z"), fromUTC("2006-12-12T16:12:12Z"), Option
            .<String> none(), Option.<String> none()));

    List<Blacklist> blacklist1 = new ArrayList<Blacklist>();
    blacklist1.add(new Blacklist(person1, periods1));
    List<Blacklist> blacklist2 = new ArrayList<Blacklist>();
    blacklist2.add(new Blacklist(person3, periods2));
    blacklist2.add(new Blacklist(person4, periods2));
    List<Blacklist> blacklist3 = new ArrayList<Blacklist>();

    EasyMock.expect(userDirectoryService.getUsers()).andReturn(users.iterator()).anyTimes();
    EasyMock.expect(userDirectoryService.findUsers(EasyMock.anyString(), EasyMock.anyInt(), EasyMock.anyInt()))
            .andDelegateTo(new TestUsers()).anyTimes();
    EasyMock.expect(pmService.getPerson(user1.getEmail())).andReturn(person1).anyTimes();
    EasyMock.expect(pmService.getPerson(user2.getEmail())).andReturn(person2).anyTimes();
    EasyMock.expect(pmService.getPerson(user3.getEmail())).andReturn(person3).anyTimes();
    EasyMock.expect(pmService.getPerson(user4.getEmail())).andReturn(person4).anyTimes();
    EasyMock.expect(pmService.findBlacklists(person1)).andReturn(blacklist1).anyTimes();
    EasyMock.expect(pmService.findBlacklists(person3)).andReturn(blacklist2).anyTimes();
    EasyMock.expect(pmService.findBlacklists(person4)).andReturn(blacklist2).anyTimes();
    EasyMock.expect(pmService.findBlacklists(person2)).andReturn(blacklist3).anyTimes();

    EasyMock.replay(pmService);
    EasyMock.replay(userDirectoryService);

    this.setParticipationPersistence(pmService);
    this.setUserDirectoryService(userDirectoryService);

    this.setSecurityService(null);
    this.setJpaUserAndRoleProvider(null);
  }

  private static Date fromUTC(String utcDate) throws IllegalStateException, ParseException {
    return new Date(DateTimeSupport.fromUTC(utcDate));
  }

  public class TestUsers implements UserDirectoryService {

    @Override
    public Iterator<User> getUsers() {
      return null;
    }

    @Override
    public User loadUser(String userName) {
      return null;
    }

    @Override
    public Iterator<User> findUsers(String query, int offset, int limit) {
      return new SmartIterator<User>(limit, offset).applyLimitAndOffset(users).iterator();
    }

    @Override
    public void invalidate(String userName) {
      return;
    }

  }

}
