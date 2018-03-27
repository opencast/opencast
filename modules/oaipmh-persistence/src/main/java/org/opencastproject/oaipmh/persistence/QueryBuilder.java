/**
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
package org.opencastproject.oaipmh.persistence;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Option;

import java.util.Date;

/** Query builder. */
public class QueryBuilder {
  private Option<String> mediaPackageId = none();
  private Option<String> repositoryId = none();
  private Option<String> seriesId = none();
  private Option<Boolean> deleted = none();
  private Option<Date> modifiedAfter = none();
  private Option<Date> modifiedBefore = none();
  private Option<Integer> limit = none();
  private Option<Integer> offset = none();
  private boolean subsequentRequest = false;

  public static QueryBuilder query() {
    return new QueryBuilder();
  }

  /** Create a query for a certain repository. */
  public static QueryBuilder queryRepo(String repositoryId) {
    return new QueryBuilder().repositoryId(repositoryId);
  }

  public QueryBuilder mediaPackageId(Option<String> mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    return this;
  }

  public QueryBuilder mediaPackageId(String mediaPackageId) {
    this.mediaPackageId = some(mediaPackageId);
    return this;
  }

  public QueryBuilder mediaPackageId(MediaPackage mediaPackage) {
    this.mediaPackageId = some(mediaPackage.getIdentifier().compact().toString());
    return this;
  }

  public QueryBuilder repositoryId(Option<String> repositoryId) {
    this.repositoryId = repositoryId;
    return this;
  }

  public QueryBuilder repositoryId(String repositoryId) {
    this.repositoryId = some(repositoryId);
    return this;
  }

  public QueryBuilder seriesId(Option<String> seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  public QueryBuilder seriesId(String seriesId) {
    this.seriesId = some(seriesId);
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedAfter(Option<Date> modifiedAfter) {
    this.modifiedAfter = modifiedAfter;
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedAfter(Date modifiedAfter) {
    this.modifiedAfter = some(modifiedAfter);
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedBefore(Option<Date> modifiedBefore) {
    this.modifiedBefore = modifiedBefore;
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedBefore(Date modifiedBefore) {
    this.modifiedBefore = some(modifiedBefore);
    return this;
  }

  public QueryBuilder isDeleted(boolean deleted) {
    this.deleted = some(deleted);
    return this;
  }

  public QueryBuilder limit(Option<Integer> limit) {
    this.limit = limit;
    return this;
  }

  public QueryBuilder limit(Integer limit) {
    this.limit = some(limit);
    return this;
  }

  public QueryBuilder offset(Integer offset) {
    this.offset = some(offset);
    return this;
  }

  /** Defaults to false. */
  public QueryBuilder subsequentRequest(boolean subsequentRequest) {
    this.subsequentRequest = subsequentRequest;
    return this;
  }

  /** Create the query. */
  public Query build() {
    final Option<String> mediaPackageId = this.mediaPackageId;
    final Option<String> repositoryId = this.repositoryId;
    final Option<String> seriesId = this.seriesId;
    final Option<Boolean> deleted = this.deleted;
    final Option<Date> modifiedAfter = this.modifiedAfter;
    final Option<Date> modifiedBefore = this.modifiedBefore;
    final Option<Integer> limit = this.limit;
    final Option<Integer> offset = this.offset;
    final boolean subsequentRequest = this.subsequentRequest;

    return new Query() {
      @Override public Option<String> getMediaPackageId() {
        return mediaPackageId;
      }

      @Override public Option<String> getRepositoryId() {
        return repositoryId;
      }

      @Override public Option<String> getSeriesId() {
        return seriesId;
      }

      @Override public Option<Boolean> isDeleted() {
        return deleted;
      }

      @Override public Option<Date> getModifiedAfter() {
        return modifiedAfter;
      }

      @Override public Option<Date> getModifiedBefore() {
        return modifiedBefore;
      }

      @Override public Option<Integer> getLimit() {
        return limit;
      }

      @Override public Option<Integer> getOffset() {
        return offset;
      }

      @Override public boolean isSubsequentRequest() {
        return subsequentRequest;
      }
    };
  }
}
