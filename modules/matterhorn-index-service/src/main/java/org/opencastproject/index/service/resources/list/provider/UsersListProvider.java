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

import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UsersListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "USERS";

  public static final String DEFAULT = PROVIDER_PREFIX;
  public static final String USERNAME = PROVIDER_PREFIX + ".USERNAME";
  public static final String NAME = PROVIDER_PREFIX + ".NAME";
  public static final String EMAIL = PROVIDER_PREFIX + ".EMAIL";
  public static final String ROLE = PROVIDER_PREFIX + ".ROLE";
  public static final String INVERSE = PROVIDER_PREFIX + ".INVERSE";
  public static final String USERDIRECTORY = PROVIDER_PREFIX + ".USERDIRECTORY";

  protected static final String[] NAMES = { PROVIDER_PREFIX, USERNAME, NAME, EMAIL, ROLE, USERDIRECTORY, INVERSE };

  private static final Logger logger = LoggerFactory.getLogger(UsersListProvider.class);

  private UserDirectoryService userDirectoryService;

  protected void activate(BundleContext bundleContext) {
    logger.info("Users list provider activated!");
  }

  /** OSGi callback for users services. */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
    Map<String, Object> usersList = new HashMap<String, Object>();
    int offset = 0;
    int limit = 0;

    if (query != null) {
      if (query.getLimit().isSome())
        limit = query.getLimit().get();

      if (query.getOffset().isSome())
        offset = query.getOffset().get();
    }

    Iterator<User> users = userDirectoryService.findUsers("%", offset, limit);

    while (users.hasNext()) {
      User u = users.next();
      if (EMAIL.equals(listName) && StringUtils.isNotBlank(u.getEmail())) {
        usersList.put(u.getEmail(), u.getEmail());
      } else if (USERNAME.equals(listName) && StringUtils.isNotBlank(u.getUsername())) {
        usersList.put(u.getUsername(), u.getUsername());
      } else if (USERDIRECTORY.equals(listName) && StringUtils.isNotBlank(u.getProvider())) {
        usersList.put(u.getProvider(), u.getProvider());
      } else if (NAME.equals(listName) && StringUtils.isNotBlank(u.getName())) {
        usersList.put(u.getName(), u.getName());
      } else if (INVERSE.equals(listName)) {
        String name = u.getName();
        usersList.put(StringUtils.isBlank(name) ? u.getUsername() : name, u.getUsername());
      } else if (DEFAULT.equals(listName)) {
        String name = u.getName();
        usersList.put(u.getUsername(), StringUtils.isBlank(name) ? u.getUsername() : name);
      } else if (ROLE.equals(listName) && u.getRoles().size() > 0) {
        for (Role role : u.getRoles()) {
          usersList.put(role.getName(), role.getName());
        }
      }
    }
    return usersList;
  }
}
