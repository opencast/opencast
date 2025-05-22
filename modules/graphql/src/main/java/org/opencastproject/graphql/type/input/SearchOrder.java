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
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName(SearchOrder.TYPE_NAME)
@GraphQLDescription("The order to use when sorting search results.")
public class SearchOrder {
  public static final String TYPE_NAME = "SearchOrder";

  @GraphQLField
  @GraphQLNonNull
  @GraphQLName("direction")
  private OrderDirection direction;

  @GraphQLField
  @GraphQLNonNull
  @GraphQLName("field")
  private String field;

  public SearchOrder() {

  }

  public SearchOrder(@GraphQLName("field") String field, @GraphQLName("direction") OrderDirection direction) {
    this.field = field;
    this.direction = direction;
  }

  public OrderDirection getDirection() {
    return direction;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public void setDirection(OrderDirection direction) {
    this.direction = direction;
  }
}
