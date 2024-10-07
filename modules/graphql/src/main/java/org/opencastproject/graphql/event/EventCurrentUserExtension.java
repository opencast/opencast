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

package org.opencastproject.graphql.event;

import org.opencastproject.graphql.datafetcher.event.EventOffsetDataFetcher;
import org.opencastproject.graphql.type.input.EventOrderByInput;
import org.opencastproject.graphql.user.GqlCurrentUser;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(GqlCurrentUser.class)
public class EventCurrentUserExtension {

  private final GqlCurrentUser currentUser;

  public EventCurrentUserExtension(GqlCurrentUser currentUser) {
    this.currentUser = currentUser;
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
    return new EventOffsetDataFetcher().withUser(currentUser.getUser()).writeOnly(true).get(environment);
  }


}
