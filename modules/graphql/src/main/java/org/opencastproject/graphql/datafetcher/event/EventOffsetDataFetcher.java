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

package org.opencastproject.graphql.datafetcher.event;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.graphql.datafetcher.ElasticsearchDataFetcher;
import org.opencastproject.graphql.event.GqlEventList;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.OpencastErrorType;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.type.input.EventOrderByInput;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import java.util.Arrays;
import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

public class EventOffsetDataFetcher extends ElasticsearchDataFetcher<GqlEventList> {

  private User user;

  private String seriesId;

  private boolean writeOnly;

  public EventOffsetDataFetcher withUser(User user) {
    this.user = user;
    return this;
  }

  public EventOffsetDataFetcher withSeriesId(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  public EventOffsetDataFetcher writeOnly(boolean writeOnly) {
    this.writeOnly = writeOnly;
    return this;
  }

  @Override
  public GqlEventList get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    SecurityService securityService = opencastContext.getService(SecurityService.class);
    ElasticsearchIndex searchIndex = opencastContext.getService(ElasticsearchIndex.class);

    EventSearchQuery eventSearchQuery = new EventSearchQuery(
        securityService.getOrganization().getId(),
        Objects.requireNonNullElse(user, securityService.getUser())
    );

    eventSearchQuery = addPaginationParams(eventSearchQuery, dataFetchingEnvironment);

    eventSearchQuery = addEventOrderByParams(eventSearchQuery, dataFetchingEnvironment);

    eventSearchQuery = addQueryParams(eventSearchQuery, dataFetchingEnvironment);

    if (seriesId != null) {
      eventSearchQuery.withSeriesId(seriesId);
    }

    if (writeOnly) {
      eventSearchQuery.withAction(Permissions.Action.WRITE);
    }

    try {
      SearchResult<Event> result = searchIndex.getByQuery(eventSearchQuery);
      Arrays.stream(result.getItems()).forEach(item -> item.getSource()
          .updatePreview(opencastContext.getConfiguration().eventPreviewSubtype()));
      return new GqlEventList(result);
    } catch (SearchIndexException e) {
      throw new GraphQLRuntimeException(OpencastErrorType.InternalError, e);
    }
  }

  public EventSearchQuery addEventOrderByParams(
      EventSearchQuery eventSearchQuery,
      final DataFetchingEnvironment environment
  ) {
    EventOrderByInput eventOrderBy = parseObjectParam("orderBy", EventOrderByInput.class, environment);

    if (eventOrderBy == null) {
      return eventSearchQuery;
    }

    if (eventOrderBy.getTitle() != null) {
      eventSearchQuery.sortByTitle(eventOrderBy.getTitle().getOrder());
    }
    if (eventOrderBy.getPresenters() != null) {
      eventSearchQuery.sortByPresenter(eventOrderBy.getPresenters().getOrder());
    }
    if (eventOrderBy.getSeriesName() != null) {
      eventSearchQuery.sortBySeriesName(eventOrderBy.getSeriesName().getOrder());
    }
    if (eventOrderBy.getTechnicalStartTime() != null) {
      eventSearchQuery.sortByTechnicalStartDate(eventOrderBy.getTechnicalStartTime().getOrder());
    }
    if (eventOrderBy.getTechnicalEndTime() != null) {
      eventSearchQuery.sortByTechnicalEndDate(eventOrderBy.getTechnicalEndTime().getOrder());
    }
    if (eventOrderBy.getStartDate() != null) {
      eventSearchQuery.sortByStartDate(eventOrderBy.getStartDate().getOrder());
    }
    if (eventOrderBy.getEndDate() != null) {
      eventSearchQuery.sortByEndDate(eventOrderBy.getEndDate().getOrder());
    }
    if (eventOrderBy.getWorkflowState() != null) {
      eventSearchQuery.sortByWorkflowState(eventOrderBy.getWorkflowState().getOrder());
    }
    if (eventOrderBy.getLocation() != null) {
      eventSearchQuery.sortByLocation(eventOrderBy.getLocation().getOrder());
    }
    if (eventOrderBy.getEventStatus() != null) {
      eventSearchQuery.sortByEventStatus(eventOrderBy.getEventStatus().getOrder());
    }
    return eventSearchQuery;
  }
}
