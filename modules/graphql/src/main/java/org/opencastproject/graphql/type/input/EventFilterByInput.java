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

package org.opencastproject.graphql.type.input;

import org.opencastproject.index.service.resources.list.query.EventListQuery;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(EventFilterByInput.TYPE_NAME)
@GraphQLDescription("Filter options for events")
public class EventFilterByInput {

  public static final String TYPE_NAME = "EventFilterByInput";

  @GraphQLField
  @GraphQLName(EventListQuery.FILTER_STATUS_NAME)
  @GraphQLDescription("Filter by event status")
  private EventStatus status;

  @GraphQLField
  @GraphQLDescription("Filter by series")
  private String seriesId;

  @GraphQLField
  @GraphQLDescription("Filter by published state")
  private Boolean published;

  public EventFilterByInput() {
  }

  public EventFilterByInput(
      @GraphQLName(EventListQuery.FILTER_STATUS_NAME) EventStatus status,
      String seriesId,
      Boolean published
  ) {
    this.status = status;
    this.seriesId = seriesId;
    this.published = published;
  }

  public EventStatus getStatus() {
    return status;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public Boolean getPublished() {
    return published;
  }

}
