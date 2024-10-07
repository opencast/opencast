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

package org.opencastproject.graphql.series;

import org.opencastproject.graphql.datafetcher.series.SeriesOffsetDataFetcher;
import org.opencastproject.graphql.type.input.SeriesOrderByInput;
import org.opencastproject.graphql.user.GqlCurrentUser;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(GqlCurrentUser.class)
public class SeriesUserExtension {

  private final GqlCurrentUser currentUser;

  public SeriesUserExtension(GqlCurrentUser currentUser) {
    this.currentUser = currentUser;
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
    return new SeriesOffsetDataFetcher().withUser(currentUser.getUser()).writeOnly(true).get(environment);
  }

}
