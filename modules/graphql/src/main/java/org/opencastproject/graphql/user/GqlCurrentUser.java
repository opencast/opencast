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

import static org.opencastproject.userdirectory.UserIdRoleProvider.getUserIdRole;

import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import java.util.List;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName(GqlCurrentUser.TYPE_NAME)
@GraphQLDescription("Represents the current user.")
public class GqlCurrentUser {

  public static final String TYPE_NAME = "CurrentUser";

  private final User user;

  public GqlCurrentUser(User user) {
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
  @GraphQLNonNull
  @GraphQLDescription("A list of roles assigned to the user.")
  public List<String> roles() {
    return user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("The role of the user.")
  public String userRole() {
    return getUserIdRole(user.getUsername());
  }

  public User getUser() {
    return user;
  }

}
