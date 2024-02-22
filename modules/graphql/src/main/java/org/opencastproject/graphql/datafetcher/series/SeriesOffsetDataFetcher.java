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

package org.opencastproject.graphql.datafetcher.series;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.elasticsearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.graphql.datafetcher.ElasticsearchDataFetcher;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.OpencastErrorType;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.type.input.SeriesOrderByInput;
import org.opencastproject.graphql.type.output.GqlSeriesList;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

public class SeriesOffsetDataFetcher extends ElasticsearchDataFetcher<GqlSeriesList> {

  private User user;
  private boolean writeOnly;

  public SeriesOffsetDataFetcher withUser(User user) {
    this.user = user;
    return this;
  }

  public SeriesOffsetDataFetcher writeOnly(boolean writeOnly) {
    this.writeOnly = writeOnly;
    return this;
  }

  @Override
  public GqlSeriesList get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    SecurityService securityService = opencastContext.getService(SecurityService.class);
    ElasticsearchIndex searchIndex = opencastContext.getService(ElasticsearchIndex.class);

    SeriesSearchQuery seriesSearchQuery = new SeriesSearchQuery(
        securityService.getOrganization().getId(),
        Objects.requireNonNullElse(user, securityService.getUser())
    );

    seriesSearchQuery = addPaginationParams(seriesSearchQuery, dataFetchingEnvironment);

    seriesSearchQuery = addSeriesOrderByParams(seriesSearchQuery, dataFetchingEnvironment);

    seriesSearchQuery = addQueryParams(seriesSearchQuery, dataFetchingEnvironment);

    if (writeOnly) {
      seriesSearchQuery.withAction(Permissions.Action.WRITE);
    }

    try {
      SearchResult<Series> result = searchIndex.getByQuery(seriesSearchQuery);
      return new GqlSeriesList(result);
    } catch (SearchIndexException e) {
      throw new GraphQLRuntimeException(OpencastErrorType.InternalError, e);
    }
  }

  public SeriesSearchQuery addSeriesOrderByParams(
      SeriesSearchQuery seriesSearchQuery,
      final DataFetchingEnvironment environment
  ) {
    SeriesOrderByInput seriesOrderBy = parseObjectParam("orderBy", SeriesOrderByInput.class, environment);
    if (seriesOrderBy == null) {
      return seriesSearchQuery;
    }

    if (seriesOrderBy.getTitle() != null) {
      seriesSearchQuery.sortByTitle(seriesOrderBy.getTitle().getOrder());
    }
    if (seriesOrderBy.getSubject() != null) {
      seriesSearchQuery.sortBySubject(seriesOrderBy.getSubject().getOrder());
    }
    if (seriesOrderBy.getCreator() != null) {
      seriesSearchQuery.sortByCreator(seriesOrderBy.getCreator().getOrder());
    }
    if (seriesOrderBy.getPublishers() != null) {
      seriesSearchQuery.sortByPublishers(seriesOrderBy.getPublishers().getOrder());
    }
    if (seriesOrderBy.getContributors() != null) {
      seriesSearchQuery.sortByContributors(seriesOrderBy.getContributors().getOrder());
    }
    if (seriesOrderBy.getDescription() != null) {
      seriesSearchQuery.sortByDescription(seriesOrderBy.getDescription().getOrder());
    }
    if (seriesOrderBy.getLanguage() != null) {
      seriesSearchQuery.sortByLanguage(seriesOrderBy.getLanguage().getOrder());
    }
    if (seriesOrderBy.getRightHolder() != null) {
      seriesSearchQuery.sortByRightsHolder(seriesOrderBy.getRightHolder().getOrder());
    }
    if (seriesOrderBy.getLicense() != null) {
      seriesSearchQuery.sortByLicense(seriesOrderBy.getLicense().getOrder());
    }
    if (seriesOrderBy.getCreated() != null) {
      seriesSearchQuery.sortByCreatedDateTime(seriesOrderBy.getCreated().getOrder());
    }

    return seriesSearchQuery;
  }

}

