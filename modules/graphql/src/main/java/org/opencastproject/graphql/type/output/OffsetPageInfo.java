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

import org.opencastproject.elasticsearch.api.SearchResult;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;

public class OffsetPageInfo {

  private final Long pageCount;
  private final Long limit;
  private final Long offset;

  public OffsetPageInfo(Long pageCount, Long limit, Long offset) {
    this.pageCount = pageCount;
    this.limit = limit;
    this.offset = offset;
  }

  public static OffsetPageInfo from(SearchResult<?> searchResult) {
    return new OffsetPageInfo(
        (searchResult.getHitCount() + searchResult.getLimit() - 1) / searchResult.getLimit(),
        searchResult.getLimit(), searchResult.getOffset());
  }

  @GraphQLField
  @GraphQLNonNull
  public Long pageCount() {
    return pageCount;
  }

  @GraphQLField
  @GraphQLNonNull
  public Long limit() {
    return limit;
  }

  @GraphQLField
  @GraphQLNonNull
  public Long offset() {
    return offset;
  }

}
