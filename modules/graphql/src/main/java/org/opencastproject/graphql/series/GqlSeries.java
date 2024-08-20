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

import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.graphql.datafetcher.event.EventOffsetDataFetcher;
import org.opencastproject.graphql.datafetcher.series.CommonSeriesMetadataV2DataFetcher;
import org.opencastproject.graphql.event.GqlEventList;
import org.opencastproject.graphql.type.input.EventOrderByInput;
import org.opencastproject.graphql.type.output.GqlCommonSeriesMetadataV2;

import java.util.List;

import graphql.annotations.annotationTypes.GraphQLDataFetcher;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLID;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.schema.DataFetchingEnvironment;

@GraphQLName(GqlSeries.TYPE_NAME)
@GraphQLDescription("A series of episodes.")
public class GqlSeries {

  public static final String TYPE_NAME = "Series";

  private final Series series;

  public GqlSeries(Series series) {
    this.series = series;
  }

  @GraphQLID
  @GraphQLField
  @GraphQLNonNull
  public String id() {
    return series.getIdentifier();
  }

  @GraphQLField
  @GraphQLNonNull
  public String title() {
    return series.getTitle();
  }

  @GraphQLField
  public String description() {
    return series.getDescription();
  }

  @GraphQLField
  public List<String> organizers() {
    return series.getOrganizers();
  }

  @GraphQLField
  public List<String> contributors() {
    return series.getContributors();
  }

  @GraphQLField
  public String creator() {
    return series.getCreator();
  }

  @GraphQLField
  public String license() {
    return series.getLicense();
  }

  @GraphQLField
  public String created() {
    return series.getCreatedDateTime().toInstant().toString();
  }

  @GraphQLField
  public List<String> publishers() {
    return series.getPublishers();
  }

  @GraphQLField
  public String rightsHolder() {
    return series.getRightsHolder();
  }

  public Series getSeries() {
    return series;
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("A list of events under the owner.")
  public GqlEventList events(
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("query") String query,
      @GraphQLName("orderBy") EventOrderByInput orderBy,
      final DataFetchingEnvironment environment) {
    return new EventOffsetDataFetcher().withSeriesId(series.getIdentifier()).get(environment);
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Common metadata of the series.")
  @GraphQLDataFetcher(CommonSeriesMetadataV2DataFetcher.class)
  public GqlCommonSeriesMetadataV2 commonMetadataV2(final DataFetchingEnvironment environment) {
    return null;
  }

}
