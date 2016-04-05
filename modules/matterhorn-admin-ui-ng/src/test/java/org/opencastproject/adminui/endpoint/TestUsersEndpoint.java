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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.SmartIterator;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestUsersEndpoint extends UsersEndpoint {

  private UserDirectoryService userDirectoryService;
  private static ArrayList<User> users;
  private User user1;
  private User user2;
  private User user3;
  private User user4;

  public TestUsersEndpoint() throws Exception {

    userDirectoryService = createNiceMock(UserDirectoryService.class);
    users = new ArrayList<User>();

    JpaOrganization organization = new JpaOrganization("org", "org", new HashMap<String, Integer>(), "ADMIN",
            "ANONYMOUS", new HashMap<String, String>());

    Set<JpaRole> roles = new HashSet<JpaRole>();
    roles.add(new JpaRole("ADMIN", organization));
    roles.add(new JpaRole("USER", organization));

    user1 = new JpaUser("user1", "pass", organization, "User1", "email1", "provider1", true, roles);
    user2 = new JpaUser("user2", "pass", organization, "user2", "email2", "provider1", true);
    user3 = new JpaUser("user3", "pass", organization, "User3", "email3", "provider1", true);
    user4 = new JpaUser("user4", "pass", organization, "user4", "email4", "provider1", true);

    users.add(user1);
    users.add(user2);
    users.add(user3);
    users.add(user4);

    expect(userDirectoryService.getUsers()).andStubReturn(users.iterator());
    EasyMock.expect(userDirectoryService.findUsers(EasyMock.anyString(), EasyMock.anyInt(), EasyMock.anyInt()))
            .andDelegateTo(new TestUsers()).anyTimes();
    replay(userDirectoryService);
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
    public long countUsers() {
      return 0;
    }

    @Override
    public void invalidate(String userName) {
      return;
    }

  }

}
