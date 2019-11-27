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

import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
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

  public static final String NAME = PROVIDER_PREFIX + ".NAME"; // Username : Name
  public static final String NAME_AND_EMAIL = PROVIDER_PREFIX + ".NAME.AND.EMAIL"; // Username: Name <Email>
  public static final String NAME_AND_USERNAME = PROVIDER_PREFIX + ".NAME.AND.USERNAME"; // Username: Name (Username)
  public static final String USERNAME = PROVIDER_PREFIX + ".USERNAME"; // UserName: UserName
  public static final String EMAIL = PROVIDER_PREFIX + ".EMAIL"; // UserName: Email

  public static final String NAME_ONLY = PROVIDER_PREFIX + ".NAME.ONLY"; // Name : Name
  public static final String EMAIL_ONLY = PROVIDER_PREFIX + ".EMAIL.ONLY"; // Email: Email
  public static final String ROLE_ONLY = PROVIDER_PREFIX + ".ROLE.ONLY"; // Role: Role
  public static final String USERDIRECTORY_ONLY = PROVIDER_PREFIX + ".USERDIRECTORY.ONLY"; // UserDirectory: UserDirectory

  protected static final String[] NAMES = { NAME, NAME_AND_EMAIL, NAME_AND_USERNAME, USERNAME, EMAIL, NAME_ONLY,
          EMAIL_ONLY, ROLE_ONLY, USERDIRECTORY_ONLY };

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
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> usersList = new HashMap<>();
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
        usersList.put(u.getUsername(), u.getEmail());
      } else if (USERNAME.equals(listName) && StringUtils.isNotBlank(u.getUsername())) {
        usersList.put(u.getUsername(), u.getUsername());
      } else if (NAME.equals(listName) && StringUtils.isNotBlank(u.getName())) {
        usersList.put(u.getUsername(), u.getName());
      } else if (NAME.equals(listName)
              || NAME_AND_EMAIL.equals(listName)
              || NAME_AND_USERNAME.equals(listName)) {
        usersList.put(u.getUsername(), createDisplayName(u, listName));
      } else if (NAME_ONLY.equals(listName) && StringUtils.isNotBlank(u.getName())) {
        usersList.put(u.getName(), u.getName());
      } else if (EMAIL_ONLY.equals(listName) && StringUtils.isNotBlank(u.getEmail())) {
        usersList.put(u.getEmail(), u.getEmail());
      } else if (USERDIRECTORY_ONLY.equals(listName) && StringUtils.isNotBlank(u.getProvider())) {
        usersList.put(u.getProvider(), u.getProvider());
      } else if (ROLE_ONLY.equals(listName) && u.getRoles().size() > 0) {
        for (Role role : u.getRoles()) {
          usersList.put(role.getName(), role.getName());
        }
      }
    }
    return usersList;
  }

  /**
   * Returns the name of the user as to be displayed in user interfaces.
   *
   * @param user
   *          the user a displayable name should be generated for
   * @param listName
   *          the list name displayable names should be generated for
   * @return name
   *          a non-null string containing the name of the user to be displayed in user interfaces
   */
  private String createDisplayName(User user, String listName) {
    assert ((user != null) && (user.getUsername() != null));
    String name = StringUtils.isNotBlank(user.getName()) ? user.getName() : user.getUsername();
    if (StringUtils.isNotBlank(user.getEmail()) && NAME_AND_EMAIL.equals(listName)) {
      name = name + " <" + user.getEmail() + ">";
    } else if (NAME_AND_USERNAME.equals(listName)) {
      name = name + " (" + user.getUsername() + ")";
    }
    assert (name != null);
    return name;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return false;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
