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

package org.opencastproject.graphql.datafetcher.user;

import org.opencastproject.graphql.datafetcher.ParameterDataFetcher;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.user.GqlUserList;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.SmartIterator;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import graphql.schema.DataFetchingEnvironment;

public class UserOffsetDataFetcher extends ParameterDataFetcher<GqlUserList> {

  private String filter;

  public UserOffsetDataFetcher withFilter(String filter) {
    this.filter = filter;
    return this;
  }

  @Override
  public GqlUserList get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    UserDirectoryService userDirectoryService = opencastContext.getService(UserDirectoryService.class);

    Integer limit = parseParam("limit", 1000, dataFetchingEnvironment);
    Integer offset = parseParam("offset", 0, dataFetchingEnvironment);

    // Filter users by filter criteria
    List<User> filteredUsers = new ArrayList<>();
    for (Iterator<User> i = userDirectoryService.getUsers(); i.hasNext();) {
      User user = i.next();

      // Filter list
      if (filter != null && !match(filter, user.getUsername(), user.getName(), user.getEmail(), user.getProvider())) {
        continue;
      }
      filteredUsers.add(user);
    }

    var total = filteredUsers.size();
    // Apply Limit and offset
    filteredUsers = new SmartIterator<User>(limit, offset).applyLimitAndOffset(filteredUsers);

    List<User> result = new ArrayList<>(filteredUsers);
    return new GqlUserList(result, (long) total, Long.valueOf(offset), Long.valueOf(limit));

  }

  public static boolean match(String searchStrings, String... text) {
    for (String searchString : StringUtils.split(searchStrings)) {
      for (String word : text) {
        if (StringUtils.indexOfIgnoreCase(word, searchString) >= 0) {
          return true;
        }
      }
    }
    return false;
  }

}

