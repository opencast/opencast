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
package org.opencastproject.oaipmh.persistence;

import org.opencastproject.mediapackage.MediaPackage;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/** Query builder. */
public class QueryBuilder {
  private Optional<String> mediaPackageId = Optional.empty();
  private Optional<String> repositoryId = Optional.empty();
  private Optional<String> seriesId = Optional.empty();
  private Optional<Boolean> deleted = Optional.empty();
  private Optional<Date> modifiedAfter = Optional.empty();
  private Optional<Date> modifiedBefore = Optional.empty();
  private Optional<Integer> limit = Optional.empty();
  private Optional<Integer> offset = Optional.empty();
  private Optional<String> setSpec = Optional.empty();
  private List<OaiPmhSetDefinition> setDefinitions = new LinkedList<>();
  private boolean subsequentRequest = false;

  public static QueryBuilder query() {
    return new QueryBuilder();
  }

  public static QueryBuilder query(Query query) {
    QueryBuilder queryBuilder = new QueryBuilder();
    queryBuilder.mediaPackageId = query.getMediaPackageId();
    queryBuilder.repositoryId = query.getRepositoryId();
    queryBuilder.seriesId = query.getSeriesId();
    queryBuilder.deleted = query.isDeleted();
    queryBuilder.modifiedAfter = query.getModifiedAfter();
    queryBuilder.modifiedBefore = query.getModifiedBefore();
    queryBuilder.limit = query.getLimit();
    queryBuilder.offset = query.getOffset();
    queryBuilder.setDefinitions = query.getSetDefinitions();
    queryBuilder.setSpec = query.getSetSpec();
    queryBuilder.subsequentRequest = query.isSubsequentRequest();
    return queryBuilder;
  }

  /** Create a query for a certain repository. */
  public static QueryBuilder queryRepo(String repositoryId) {
    return new QueryBuilder().repositoryId(repositoryId);
  }

  public QueryBuilder mediaPackageId(Optional<String> mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    return this;
  }

  public QueryBuilder mediaPackageId(String mediaPackageId) {
    this.mediaPackageId = Optional.of(mediaPackageId);
    return this;
  }

  public QueryBuilder mediaPackageId(MediaPackage mediaPackage) {
    this.mediaPackageId = Optional.of(mediaPackage.getIdentifier().toString().toString());
    return this;
  }

  public QueryBuilder repositoryId(Optional<String> repositoryId) {
    this.repositoryId = repositoryId;
    return this;
  }

  public QueryBuilder repositoryId(String repositoryId) {
    this.repositoryId = Optional.of(repositoryId);
    return this;
  }

  public QueryBuilder seriesId(Optional<String> seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  public QueryBuilder seriesId(String seriesId) {
    this.seriesId = Optional.of(seriesId);
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedAfter(Optional<Date> modifiedAfter) {
    this.modifiedAfter = modifiedAfter;
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedAfter(Date modifiedAfter) {
    this.modifiedAfter = Optional.of(modifiedAfter);
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedBefore(Optional<Date> modifiedBefore) {
    this.modifiedBefore = modifiedBefore;
    return this;
  }

  /** The date is inclusive. */
  public QueryBuilder modifiedBefore(Date modifiedBefore) {
    this.modifiedBefore = Optional.of(modifiedBefore);
    return this;
  }

  public QueryBuilder isDeleted(boolean deleted) {
    this.deleted = Optional.of(deleted);
    return this;
  }

  public QueryBuilder limit(Optional<Integer> limit) {
    this.limit = limit;
    return this;
  }

  public QueryBuilder limit(Integer limit) {
    this.limit = Optional.of(limit);
    return this;
  }

  public QueryBuilder offset(Integer offset) {
    this.offset = Optional.of(offset);
    return this;
  }

  /** Defaults to false. */
  public QueryBuilder subsequentRequest(boolean subsequentRequest) {
    this.subsequentRequest = subsequentRequest;
    return this;
  }

  public QueryBuilder setDefinitions(List<OaiPmhSetDefinition> setDef) {
    this.setDefinitions = setDef;
    return this;
  }

  public QueryBuilder setSpec(String setSpec) {
    if (setSpec != null) {
      this.setSpec = Optional.of(setSpec);
    }
    return this;
  }

  /** Create the query. */
  public Query build() {
    final Optional<String> mediaPackageId = this.mediaPackageId;
    final Optional<String> repositoryId = this.repositoryId;
    final Optional<String> seriesId = this.seriesId;
    final Optional<Boolean> deleted = this.deleted;
    final Optional<Date> modifiedAfter = this.modifiedAfter;
    final Optional<Date> modifiedBefore = this.modifiedBefore;
    final Optional<Integer> limit = this.limit;
    final Optional<Integer> offset = this.offset;
    final Optional<String> setSpec = this.setSpec;
    final List<OaiPmhSetDefinition> setDefinitions = this.setDefinitions;
    final boolean subsequentRequest = this.subsequentRequest;

    return new Query() {
      @Override public Optional<String> getMediaPackageId() {
        return mediaPackageId;
      }

      @Override public Optional<String> getRepositoryId() {
        return repositoryId;
      }

      @Override public Optional<String> getSeriesId() {
        return seriesId;
      }

      @Override public Optional<Boolean> isDeleted() {
        return deleted;
      }

      @Override public Optional<Date> getModifiedAfter() {
        return modifiedAfter;
      }

      @Override public Optional<Date> getModifiedBefore() {
        return modifiedBefore;
      }

      @Override public Optional<Integer> getLimit() {
        return limit;
      }

      @Override public Optional<Integer> getOffset() {
        return offset;
      }

      @Override
      public List<OaiPmhSetDefinition> getSetDefinitions() {
        return setDefinitions;
      }

      @Override
      public Optional<String> getSetSpec() {
        return setSpec;
      }

      @Override public boolean isSubsequentRequest() {
        return subsequentRequest;
      }
    };
  }
}
