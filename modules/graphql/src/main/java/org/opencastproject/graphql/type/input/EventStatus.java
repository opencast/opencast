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
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(EventStatus.TYPE_NAME)
@GraphQLDescription("Filter options for events")
public enum EventStatus {

  RECORDING_FAILURE("EVENTS.EVENTS.STATUS.RECORDING_FAILURE"),
  PENDING("EVENTS.EVENTS.STATUS.PENDING"),
  RECORDING("EVENTS.EVENTS.STATUS.RECORDING"),
  PAUSED("EVENTS.EVENTS.STATUS.PAUSED"),
  INGESTING("EVENTS.EVENTS.STATUS.INGESTING"),
  PROCESSING_FAILURE("EVENTS.EVENTS.STATUS.PROCESSING_FAILURE"),
  SCHEDULED("EVENTS.EVENTS.STATUS.SCHEDULED"),
  PROCESSING_CANCELLED("EVENTS.EVENTS.STATUS.PROCESSING_CANCELLED"),
  PROCESSING("EVENTS.EVENTS.STATUS.PROCESSING"),
  PROCESSED("EVENTS.EVENTS.STATUS.PROCESSED");

  public static final String TYPE_NAME = "EventFilter";

  private final String status;

  EventStatus(String status) {
    this.status = status;
  }

  public String getFilterValue() {
    return this.status;
  }

}

