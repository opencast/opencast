/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.archive.base;

import org.opencastproject.archive.api.Query;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.util.Date;

import static org.opencastproject.util.ReflectionUtil.bcall;
import static org.opencastproject.util.ReflectionUtil.call;
import static org.opencastproject.util.ReflectionUtil.xfer;

/** Mutable builder for {@link org.opencastproject.archive.api.Query}s. */
public final class QueryBuilder extends QueryBuilderBase<QueryBuilder> {
  private QueryBuilder() {
  }

  public static QueryBuilder query() {
    return new QueryBuilder();
  }

  /** Create a builder from a plain {@link org.opencastproject.archive.api.Query}. */
  public static <A extends QueryBuilderBase> A query(final Query source, final A target) {
    return xfer(target, Query.class, new Function<A, Query>() {
      @Override public Query apply(final A target) {
        return new Query() {
          @Override public Option<String> getMediaPackageId() {
            return call(target.mediaPackageId(source.getMediaPackageId()));
          }

          @Override public Option<String> getSeriesId() {
            return call(target.seriesId(source.getSeriesId()));
          }

          @Override public Option<String> getOrganizationId() {
            return call(target.organizationId(source.getOrganizationId()));
          }

          @Override public boolean isOnlyLastVersion() {
            return bcall(target.onlyLastVersion(source.isOnlyLastVersion()));
          }

          @Override public Option<Integer> getLimit() {
            return call(target.limit(source.getLimit()));
          }

          @Override public Option<Integer> getOffset() {
            return call(target.offset(source.getOffset()));
          }

          @Override public Option<Date> getArchivedAfter() {
            return call(target.archivedAfter(source.getArchivedAfter()));
          }

          @Override public Option<Date> getArchivedBefore() {
            return call(target.archivedBefore(source.getArchivedBefore()));
          }

          @Override public boolean isIncludeDeleted() {
            return bcall(target.includeDeleted(source.isIncludeDeleted()));
          }

          @Override public Option<Date> getDeletedAfter() {
            return call(target.deletedAfter(source.getDeletedAfter()));
          }

          @Override public Option<Date> getDeletedBefore() {
            return call(target.deletedBefore(source.getDeletedBefore()));
          }
        };
      }
    });
  }

  /** Create a builder from a plain {@link org.opencastproject.archive.api.Query}. */
  public static QueryBuilder query(final Query source) {
    return query(source, new QueryBuilder());
  }
}
