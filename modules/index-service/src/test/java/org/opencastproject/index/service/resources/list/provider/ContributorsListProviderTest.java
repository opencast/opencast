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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContributorsListProviderTest {

  private static final String ORG_ID = "org-id";
  private AbstractSearchIndex searchIndex;
  private UserDirectoryService userDirectoryService;
  private ContributorsListProvider contributorsListProvider;
  private SecurityService securityService;
  private ArrayList<User> users;
  private User user1;
  private User user2;
  private User user3;
  private User user4;
  private JpaOrganization organization;

  @Before
  public void setUp() throws Exception {
    String expectedFormat = "{\"organization\":\"mh_default\",\"username\":\"akm220\",\"presentation\":\"Adam McKenzie\"}";

    organization = new JpaOrganization(ORG_ID, "name", null, null, null, null);
    users = new ArrayList<User>();

    user1 = new JpaUser("user1", "pass", organization, "User 1", "email1", "provider1", true);
    user2 = new JpaUser("user2", "pass", organization, "User 2", "email1", "provider1", true);
    user3 = new JpaUser("user3", "pass", organization, null, "email1", "provider1", true);
    user4 = new JpaUser("user4", "pass", organization, "User 1", "email3", "provider1", true);

    users.add(user1);
    users.add(user2);
    users.add(user3);
    users.add(user4);

    userDirectoryService = new UserDirectoryService() {

      private Iterator<User> iterator;

      @Override
      public User loadUser(String userName) {
        return null;
      }

      @Override
      public Iterator<User> getUsers() {
        return null;
      }

      @Override
      public Iterator<User> findUsers(String query, int offset, int limit) {

        iterator = users.iterator();

        List<User> filteredList = new ArrayList<User>();
        int i = 0;
        while ((filteredList.size() < limit || limit == 0) && iterator.hasNext()) {
          User item = iterator.next();
          if (i++ >= offset) {
            filteredList.add(item);
          }
        }
        return filteredList.iterator();
      }

      @Override
      public long countUsers() {
        return users.size();
      }

      @Override
      public void invalidate(String userName) {
        return;
      }
    };

    List<String> contributors = new ArrayList<String>();
    contributors.add("User 1");
    contributors.add("User 5");

    searchIndex = EasyMock.createNiceMock(AbstractSearchIndex.class);
    EasyMock.expect(searchIndex.getTermsForField(EasyMock.anyString(), EasyMock.anyObject(Option.class)))
            .andReturn(contributors).anyTimes();

    contributorsListProvider = new ContributorsListProvider();
    contributorsListProvider.setUserDirectoryService(userDirectoryService);
    contributorsListProvider.setIndex(searchIndex);
    EasyMock.replay(searchIndex);
  }

  @Test
  public void testListNames() {
    Assert.assertTrue(Arrays.equals(ContributorsListProvider.NAMES, contributorsListProvider.getListNames()));
  }

  @Test
  public void testUsernamesList() {
    Map<String, String> list = contributorsListProvider.getList(ContributorsListProvider.NAMES_TO_USERNAMES, null);

    Assert.assertTrue(list.containsKey(user1.getUsername()));
    Assert.assertTrue(list.containsKey(user2.getUsername()));
    Assert.assertTrue(list.containsKey(user3.getUsername()));

    Assert.assertTrue(list.containsValue(user1.getName()));
    Assert.assertTrue(list.containsValue(user2.getName()));
    Assert.assertTrue(list.containsValue(user3.getUsername()));

    Assert.assertTrue(list.containsKey("User 5"));
    Assert.assertTrue(list.containsValue("User 5"));

    Assert.assertEquals(5, list.size());
  }

  @Test
  public void testListSimple() throws ListProviderException {
    Map<String, String> list = contributorsListProvider.getList(ContributorsListProvider.DEFAULT, null);

    Assert.assertTrue(list.containsKey(user1.getName()));
    Assert.assertTrue(list.containsKey(user2.getName()));

    Assert.assertTrue(list.containsValue(user1.getName()));
    Assert.assertTrue(list.containsValue(user2.getName()));

    Assert.assertTrue(list.containsKey("User 5"));
    Assert.assertTrue(list.containsValue("User 5"));

    Assert.assertEquals(3, list.size());
  }

  @Test
  public void testListQuery() {
    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.setLimit(0);
    query.setOffset(0);
    Assert.assertEquals(5, contributorsListProvider.getList(ContributorsListProvider.NAMES_TO_USERNAMES, query).size());
    query.setOffset(3);
    Assert.assertEquals(2, contributorsListProvider.getList(ContributorsListProvider.NAMES_TO_USERNAMES, query).size());
    query.setOffset(0);
    query.setLimit(1);
    Assert.assertEquals(1, contributorsListProvider.getList(ContributorsListProvider.NAMES_TO_USERNAMES, query).size());
  }
}
