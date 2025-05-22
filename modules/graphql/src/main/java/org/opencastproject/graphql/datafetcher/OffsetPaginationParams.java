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

public final class OffsetPaginationParams {

  public static final int DEFAULT_PAGE_SIZE = 20;

  private final Integer offset;
  private final Integer limit;

  private OffsetPaginationParams(final Builder builder) {
    this.offset = builder.offset;
    this.limit = builder.limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public Integer getLimit() {
    return limit;
  }

  public static class Builder {
    private Integer offset = 0;
    private Integer limit = DEFAULT_PAGE_SIZE;

    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(Integer offset) {
      this.offset = offset;
      return this;
    }

    public OffsetPaginationParams build() {
      return new OffsetPaginationParams(this);
    }

  }

}
