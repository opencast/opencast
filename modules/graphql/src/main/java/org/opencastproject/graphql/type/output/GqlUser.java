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

package org.opencastproject.graphql.type.output;

import static org.opencastproject.userdirectory.UserIdRoleProvider.getUserIdRole;

import org.opencastproject.graphql.datafetcher.event.EventOffsetDataFetcher;
import org.opencastproject.graphql.datafetcher.series.SeriesOffsetDataFetcher;
import org.opencastproject.graphql.type.input.EventOrderByInput;
import org.opencastproject.graphql.type.input.SeriesOrderByInput;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import java.util.List;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.schema.DataFetchingEnvironment;

public class GqlUser {

  private final User user;

  public GqlUser(User user) {
    this.user = user;
  }

  @GraphQLField
  public String username() {
    return user.getUsername();
  }

  @GraphQLField
  public String name() {
    return user.getName();
  }

  @GraphQLField
  public String email() {
    return user.getEmail();
  }

  @GraphQLField
  public String provider() {
    return user.getProvider();
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("A list of series under the owner.")
  public GqlSeriesList mySeries(
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("query") String query,
      @GraphQLName("orderBy") SeriesOrderByInput orderBy,
      final DataFetchingEnvironment environment) {
    return new SeriesOffsetDataFetcher().withUser(user).writeOnly(true).get(environment);
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("A list of events under the owner.")
  public GqlEventList myEvents(
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("query") String query,
      @GraphQLName("orderBy") EventOrderByInput orderBy,
      final DataFetchingEnvironment environment) {
    return new EventOffsetDataFetcher().withUser(user).writeOnly(true).get(environment);
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("A list of roles assigned to the user.")
  public List<String> roles() {
    return user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("User r")
  public String userRole() {
    return getUserIdRole(user.getUsername());
  }

}
