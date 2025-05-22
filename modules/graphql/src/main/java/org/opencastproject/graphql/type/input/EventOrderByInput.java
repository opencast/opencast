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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(EventOrderByInput.TYPE_NAME)
@GraphQLDescription("Ordering options for events")
public class EventOrderByInput {

  public static final String TYPE_NAME = "EventOrderByInput";

  @GraphQLField
  @GraphQLName("title")
  private OrderDirection title;

  @GraphQLField
  @GraphQLName("presenters")
  private OrderDirection presenters;

  @GraphQLField
  @GraphQLName("startDate")
  private OrderDirection startDate;

  @GraphQLField
  @GraphQLName("endDate")
  private OrderDirection endDate;

  @GraphQLField
  @GraphQLName("workflowState")
  private OrderDirection workflowState;

  @GraphQLField
  @GraphQLName("eventStatus")
  private OrderDirection eventStatus;

  @GraphQLField
  @GraphQLName("seriesName")
  private OrderDirection seriesName;

  @GraphQLField
  @GraphQLName("location")
  private OrderDirection location;

  @GraphQLField
  @GraphQLName("technicalStartTime")
  private OrderDirection technicalStartTime;

  @GraphQLField
  @GraphQLName("technicalEndTime")
  private OrderDirection technicalEndTime;

  public EventOrderByInput() {

  }

  public EventOrderByInput(
      @GraphQLName("title") OrderDirection title,
      @GraphQLName("presenters") OrderDirection presenters,
      @GraphQLName("startDate") OrderDirection startDate,
      @GraphQLName("endDate") OrderDirection endDate,
      @GraphQLName("workflowState") OrderDirection workflowState,
      @GraphQLName("eventStatus") OrderDirection eventStatus,
      @GraphQLName("seriesName") OrderDirection seriesName,
      @GraphQLName("location") OrderDirection location,
      @GraphQLName("technicalStartTime") OrderDirection technicalStartTime,
      @GraphQLName("technicalEndTime") OrderDirection technicalEndTime
  ) {
    this.title = title;
    this.presenters = presenters;
    this.startDate = startDate;
    this.endDate = endDate;
    this.workflowState = workflowState;
    this.eventStatus = eventStatus;
    this.seriesName = seriesName;
    this.location = location;
    this.technicalStartTime = technicalStartTime;
    this.technicalEndTime = technicalEndTime;
  }

  public OrderDirection getTitle() {
    return title;
  }

  public OrderDirection getPresenters() {
    return presenters;
  }

  public OrderDirection getStartDate() {
    return startDate;
  }

  public OrderDirection getEndDate() {
    return endDate;
  }

  public OrderDirection getWorkflowState() {
    return workflowState;
  }

  public OrderDirection getSeriesName() {
    return seriesName;
  }

  public OrderDirection getLocation() {
    return location;
  }

  public OrderDirection getTechnicalStartTime() {
    return technicalStartTime;
  }

  public OrderDirection getTechnicalEndTime() {
    return technicalEndTime;
  }

  public OrderDirection getEventStatus() {
    return eventStatus;
  }
}
