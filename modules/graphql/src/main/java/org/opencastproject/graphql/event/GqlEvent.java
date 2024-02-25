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

import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.graphql.datafetcher.event.CommonEventMetadataDataFetcher;
import org.opencastproject.graphql.datafetcher.event.CommonEventMetadataV2DataFetcher;
import org.opencastproject.graphql.datafetcher.series.SeriesDataFetcher;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.series.GqlSeries;
import org.opencastproject.graphql.type.output.GqlCommonEventMetadata;
import org.opencastproject.graphql.type.output.GqlCommonEventMetadataV2;
import org.opencastproject.graphql.type.output.GqlPublication;
import org.opencastproject.workflow.api.WorkflowService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDataFetcher;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLID;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.schema.DataFetchingEnvironment;

@GraphQLName(GqlEvent.TYPE_NAME)
@GraphQLDescription("A series.")
public class GqlEvent {

  public static final String TYPE_NAME = "GqlEvent";

  private final Event event;

  public GqlEvent(Event event) {
    this.event = event;
  }

  @GraphQLID
  @GraphQLField
  @GraphQLNonNull
  public String id() {
    return this.event.getIdentifier();
  }

  @GraphQLID
  @GraphQLField
  @GraphQLNonNull
  public String title() {
    return this.event.getTitle();
  }

  @GraphQLField
  public String description() {
    return this.event.getDescription();
  }

  @GraphQLField
  public List<String> presenters() {
    return this.event.getPresenters();
  }

  @GraphQLField
  public List<String> contributors() {
    return this.event.getContributors();
  }

  @GraphQLField
  public String publisher() {
    return this.event.getPublisher();
  }

  @GraphQLField
  public String location() {
    return this.event.getLocation();
  }

  @GraphQLField
  public String license() {
    return this.event.getLicense();
  }

  @GraphQLField
  public String creator() {
    return this.event.getCreator();
  }

  @GraphQLField
  public String created() {
    return this.event.getCreated();
  }

  @GraphQLField
  public Long duration() {
    return this.event.getDuration();
  }

  @GraphQLField
  public String startDate() {
    return this.event.getRecordingStartDate();
  }

  @GraphQLField
  public String endDate() {
    return this.event.getRecordingEndDate();
  }

  @GraphQLField
  public String technicalStartTime() {
    return this.event.getTechnicalStartTime();
  }

  @GraphQLField
  public String technicalEndTime() {
    return this.event.getTechnicalEndTime();
  }

  @GraphQLField
  public String eventStatus() {
    return this.event.getEventStatus();
  }

  @GraphQLField
  public String displayableStatus() {
    var workflowService = OpencastContextManager.getCurrentContext().getService(WorkflowService.class);
    if (workflowService != null) {
      return this.event.getDisplayableStatus(workflowService.getWorkflowStateMappings());
    }
    return null;
  }

  @GraphQLField
  @GraphQLDescription("Return publications filterable by channel and tags")
  public List<GqlPublication> publications(
      @GraphQLName("channel") String channel,
      @GraphQLName("tags") List<String> tags) {
    return event.getPublications().stream()
        .filter(e -> !Objects.equals(e.getChannel(), "internal"))
        .filter(e -> channel == null || Objects.equals(e.getChannel(), channel))
        .filter(e -> tags == null || Arrays.stream(e.getTags()).anyMatch(tags::contains))
        .map(GqlPublication::new)
        .collect(Collectors.toList());
  }

  @GraphQLField
  public String seriesId() {
    return this.event.getSeriesId();
  }

  @GraphQLField
  public String seriesName() {
    return this.event.getSeriesName();
  }

  @GraphQLField
  public GqlSeries series(final DataFetchingEnvironment environment) {
    var seriesId = this.event.getSeriesId();
    if (seriesId == null) {
      return null;
    } else {
      return new SeriesDataFetcher(this.event.getSeriesId()).get(environment);
    }
  }

  @GraphQLField
  public Boolean hasPreview() {
    return this.event.hasPreview();
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Common metadata of the event.")
  @GraphQLDataFetcher(CommonEventMetadataDataFetcher.class)
  public GqlCommonEventMetadata commonMetadata(final DataFetchingEnvironment environment) {
    return null;
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Common metadata of the event.")
  @GraphQLDataFetcher(CommonEventMetadataV2DataFetcher.class)
  public GqlCommonEventMetadataV2 commonMetadataV2(final DataFetchingEnvironment environment) {
    return null;
  }


}
