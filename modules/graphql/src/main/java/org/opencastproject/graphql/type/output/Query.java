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

import org.opencastproject.graphql.datafetcher.event.EventDataFetcher;
import org.opencastproject.graphql.datafetcher.event.EventOffsetDataFetcher;
import org.opencastproject.graphql.datafetcher.series.SeriesDataFetcher;
import org.opencastproject.graphql.datafetcher.series.SeriesOffsetDataFetcher;
import org.opencastproject.graphql.datafetcher.user.CurrentUserDataFetcher;
import org.opencastproject.graphql.type.input.EventOrderByInput;
import org.opencastproject.graphql.type.input.SeriesOrderByInput;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.schema.DataFetchingEnvironment;

@GraphQLName(Query.TYPE_NAME)
public class Query {
  public static final String TYPE_NAME = "Query";

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("The current user")
  public static GqlUser currentUser(
      final DataFetchingEnvironment environment
  ) {
    return new CurrentUserDataFetcher().get(environment);
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Returns series list")
  public static GqlSeriesList allSeries(
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("query") String query,
      @GraphQLName("orderBy") SeriesOrderByInput orderBy,
      final DataFetchingEnvironment environment) {
    return new SeriesOffsetDataFetcher().get(environment);
  }

  @GraphQLField
  @GraphQLDescription("Returns series list")
  public static GqlSeries seriesById(
      @GraphQLName("id")@GraphQLNonNull String id,
      final DataFetchingEnvironment environment) {
    return new SeriesDataFetcher(id).get(environment);
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Returns event list")
  public static GqlEventList allEvents(
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("query") String query,
      @GraphQLName("orderBy") EventOrderByInput orderBy,
      final DataFetchingEnvironment environment) {
    return new EventOffsetDataFetcher().get(environment);
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("A event by id")
  public static GqlEvent eventById(
      @GraphQLName("id")@GraphQLNonNull String id,
      final DataFetchingEnvironment environment
  ) {
    return new EventDataFetcher(id).get(environment);

  }


  @Override
  public String toString() {
    return TYPE_NAME;
  }

}

