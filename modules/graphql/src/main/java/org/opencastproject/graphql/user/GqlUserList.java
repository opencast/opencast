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

package org.opencastproject.graphql.user;

import org.opencastproject.graphql.type.output.OffsetPageInfo;
import org.opencastproject.security.api.User;

import java.util.List;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName(GqlUserList.TYPE_NAME)
@GraphQLNonNull
@GraphQLDescription("A list of users")
public class GqlUserList {

  public static final String TYPE_NAME = "UserList";

  private final List<User> searchResult;

  private final Long totalCount;

  private final Long offset;

  private final Long limit;

  public GqlUserList(List<User> searchResult) {
    this(searchResult, null, 0L, 1000L);
  }

  public GqlUserList(List<User> searchResult, Long totalCount, Long offset, Long limit) {
    this.searchResult = searchResult;
    this.totalCount = totalCount;
    this.offset = offset;
    this.limit = limit;
  }

  @GraphQLField
  @GraphQLNonNull
  public Long totalCount() {
    return this.totalCount;
  }

  @GraphQLField
  @GraphQLNonNull
  public OffsetPageInfo pageInfo() {
    var pageCount = totalCount == null ? searchResult.size() : ((totalCount + limit - 1) / limit);
    return new OffsetPageInfo(pageCount, limit, offset);
  }

  @GraphQLField
  @GraphQLNonNull
  public List<GqlUser> nodes() {
    return searchResult.stream()
        .map(GqlUser::new)
        .collect(Collectors.toList());
  }

}
