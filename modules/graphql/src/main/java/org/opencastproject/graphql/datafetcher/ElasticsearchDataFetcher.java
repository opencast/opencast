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

package org.opencastproject.graphql.datafetcher;

import org.opencastproject.elasticsearch.api.SearchQuery;
import org.opencastproject.graphql.type.input.SearchOrder;

import graphql.schema.DataFetchingEnvironment;

public abstract class ElasticsearchDataFetcher<T> extends ParameterDataFetcher<T> {

  public static final int DEFAULT_PAGE_SIZE = 20;

  protected <E extends SearchQuery> E addPaginationParams(E query, final DataFetchingEnvironment environment) {
    query.withLimit(parseParam("limit", DEFAULT_PAGE_SIZE, environment))
        .withOffset(parseParam("offset", 0, environment));

    return query;
  }

  protected <E extends SearchQuery> E addOrderByParams(E query, final DataFetchingEnvironment environment) {
    SearchOrder order = parseObjectParam("orderBy", SearchOrder.class, environment);
    if (order != null) {
      query.withSortOrder(order.getField(), order.getDirection().getOrder());
    }
    return query;
  }

  protected <E extends SearchQuery> E addQueryParams(E query, final DataFetchingEnvironment environment) {
    if (environment.containsArgument("query")) {
      query.withText("*" + parseParam("query", null, environment) + "*");
    }
    return query;
  }

}
