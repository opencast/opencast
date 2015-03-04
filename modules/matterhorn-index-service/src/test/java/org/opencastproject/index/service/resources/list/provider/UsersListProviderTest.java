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
package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.userdirectory.JpaUser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UsersListProviderTest {

  private UserDirectoryService userDirectoryService;
  private UsersListProvider usersListProvider;
  private ArrayList<User> users;
  private User user1;
  private User user2;
  private User user3;
  private User user4;

  @Before
  public void setUp() throws Exception {

    users = new ArrayList<User>();

    user1 = new JpaUser("user1", "pass", null, "user1", "email1", "provider1", true);
    user2 = new JpaUser("user2", "pass", null, "user2", "email1", "provider1", true);
    user3 = new JpaUser("user3", "pass", null, "user3", "email1", "provider1", true);
    user4 = new JpaUser("user4", "pass", null, "user1", "email3", "provider1", true);

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
      public void invalidate(String userName) {
        return;
      }
    };

    usersListProvider = new UsersListProvider();
    usersListProvider.setUserDirectoryService(userDirectoryService);
  }

  @Test
  public void testListNames() {
    Assert.assertTrue(Arrays.equals(UsersListProvider.NAMES, usersListProvider.getListNames()));
  }

  @Test
  public void testListSimple() throws ListProviderException {
    Map<String, Object> list = usersListProvider.getList(UsersListProvider.NAME, null, null);
    Assert.assertTrue(list.containsKey(user1.getName()));
    Assert.assertTrue(list.containsKey(user2.getName()));
    Assert.assertTrue(list.containsKey(user3.getName()));
    Assert.assertEquals(3, list.size());

    list = usersListProvider.getList(UsersListProvider.USERNAME, null, null);
    Assert.assertTrue(list.containsKey(user1.getUsername()));
    Assert.assertTrue(list.containsKey(user2.getUsername()));
    Assert.assertTrue(list.containsKey(user3.getUsername()));
    Assert.assertTrue(list.containsKey(user4.getUsername()));
    Assert.assertEquals(4, list.size());

    list = usersListProvider.getList(UsersListProvider.EMAIL, null, null);
    Assert.assertTrue(list.containsKey(user1.getEmail()));
    Assert.assertTrue(list.containsKey(user4.getEmail()));
    Assert.assertEquals(2, list.size());

    list = usersListProvider.getList(UsersListProvider.USERDIRECTORY, null, null);
    Assert.assertTrue(list.containsKey(user1.getProvider()));
    Assert.assertEquals(1, list.size());
  }

  @Test
  public void testListQuery() {
    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.setLimit(0);
    query.setOffset(0);
    Assert.assertEquals(4, usersListProvider.getList(UsersListProvider.USERNAME, query, null).size());
    query.setOffset(3);
    Assert.assertEquals(1, usersListProvider.getList(UsersListProvider.USERNAME, query, null).size());
    query.setOffset(0);
    query.setLimit(1);
    Assert.assertEquals(1, usersListProvider.getList(UsersListProvider.USERNAME, query, null).size());
  }
}
