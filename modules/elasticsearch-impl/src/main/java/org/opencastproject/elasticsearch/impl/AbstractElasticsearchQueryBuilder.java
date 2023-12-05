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

package org.opencastproject.elasticsearch.impl;

import static org.opencastproject.elasticsearch.impl.IndexSchema.TEXT;
import static org.opencastproject.elasticsearch.impl.IndexSchema.TEXT_FUZZY;

import org.opencastproject.elasticsearch.api.SearchQuery;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Opencast implementation of the elastic search query builder.
 */
public abstract class AbstractElasticsearchQueryBuilder<T extends SearchQuery> implements QueryBuilder {

  /** Term queries on fields */
  private Map<String, Set<Object>> searchTerms = null;

  /** Fields that need to match all values */
  protected List<ValueGroup> groups = null;

  /** Fields that query a date range */
  private Set<DateRange> dateRanges = null;

  /** Filter expression */
  protected String filter = null;

  /** Text query */
  protected String text = null;

  /** Fuzzy text query */
  protected String fuzzyText = null;

  /** The original search query */
  private T query;

  /** The boolean query */
  private QueryBuilder queryBuilder = null;

  /**
   * Creates a new elastic search query based on the raw query.
   *
   * @param query
   *          the search query
   */
  public AbstractElasticsearchQueryBuilder(T query) {
    this.query = query;
    buildQuery(query);
    createQuery();
  }

  /**
   * Returns the original search query.
   *
   * @return the search query
   */
  public T getQuery() {
    return query;
  }

  public abstract void buildQuery(T query);

  /**
   * Create the actual query. We start with a query that matches everything, then move to the boolean conditions,
   * finally add filter queries.
   */
  private void createQuery() {

    queryBuilder = new MatchAllQueryBuilder();

    // The boolean query builder
    BoolQueryBuilder booleanQuery = new BoolQueryBuilder();

    // Terms
    if (searchTerms != null) {
      for (Map.Entry<String, Set<Object>> entry : searchTerms.entrySet()) {
        booleanQuery.must(new TermsQueryBuilder(entry.getKey(), entry.getValue().toArray(new Object[0])));
      }
      this.queryBuilder = booleanQuery;
    }

    // Date ranges
    if (dateRanges != null) {
      for (DateRange dr : dateRanges) {
        booleanQuery.must(dr.getQueryBuilder());
      }
      this.queryBuilder = booleanQuery;
    }

    // Text
    if (text != null) {
      QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(text).field(TEXT);
      booleanQuery.must(queryBuilder);
      this.queryBuilder = booleanQuery;
    }

    // Fuzzy text
    if (fuzzyText != null) {
      MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(
              new String[] {TEXT_FUZZY},
              new String[] {fuzzyText},
              null);
      booleanQuery.must(moreLikeThisQueryBuilder);
      this.queryBuilder = booleanQuery;
    }

    List<QueryBuilder> filters = new ArrayList<>();

    // Add filtering for AND terms
    if (groups != null) {
      for (ValueGroup group : groups) {
        filters.addAll(group.getFilterBuilders());
      }
    }

    // Filter expressions
    if (filter != null) {
      filters.add(QueryBuilders.termQuery(IndexSchema.TEXT, filter));
    }

    // Apply the filters
    if (!filters.isEmpty()) {
      for (QueryBuilder filter : filters) {
        booleanQuery.filter(filter);
      }
      this.queryBuilder = booleanQuery;
    }

  }

  /**
   * Stores <code>fieldValue</code> as a search term on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValues
   *          the field value
   */
  protected void and(String fieldName, Object... fieldValues) {

    // Make sure the data structures are set up accordingly
    if (searchTerms == null) {
      searchTerms = new HashMap<>();
    }

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // insert value
    searchTerms.computeIfAbsent(fieldName, k -> new HashSet<>())
            .addAll(Arrays.asList(fieldValues));
  }

  /**
   * Stores <code>fieldValue</code> as a search term on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param startDate
   *          the start date
   * @param endDate
   *          the end date
   */
  protected void and(String fieldName, Date startDate, Date endDate) {

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // Make sure the data structures are set up accordingly
    if (dateRanges == null) {
      dateRanges = new HashSet<>();
    }

    // Add the term
    dateRanges.add(new DateRange(fieldName, startDate, endDate));
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    return queryBuilder.toXContent(builder, params);
  }

  @Override
  public Query toQuery(QueryShardContext context) throws IOException {
    return queryBuilder.toQuery(context);
  }

  @Override
  public QueryBuilder queryName(String queryName) {
    return queryBuilder.queryName(queryName);
  }

  @Override
  public String queryName() {
    return queryBuilder.queryName();
  }

  @Override
  public float boost() {
    return queryBuilder.boost();
  }

  @Override
  public QueryBuilder boost(float boost) {
    return queryBuilder.boost(boost);
  }

  @Override
  public String getName() {
    return queryBuilder.getName();
  }

  @Override
  public String getWriteableName() {
    return queryBuilder.getWriteableName();
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    queryBuilder.writeTo(out);
  }

  @Override
  public QueryBuilder rewrite(QueryRewriteContext queryShardContext) throws IOException {
    return queryBuilder.rewrite(queryShardContext);
  }

  @Override
  public boolean isFragment() {
    return queryBuilder.isFragment();
  }

  /**
   * Utility class to hold date range specifications and turn them into elastic search queries.
   */
  public static final class DateRange {

    /** The field name */
    private String field;

    /** The start date */
    private Date startDate;

    /** The end date */
    private Date endDate;

    /**
     * Creates a new date range specification with the given field name, start and end dates. <code>null</code> may be
     * passed in for start or end dates that should remain unspecified.
     *
     * @param field
     *          the field name
     * @param start
     *          the start date
     * @param end
     *          the end date
     */
    DateRange(String field, Date start, Date end) {
      this.field = field;
      this.startDate = start;
      this.endDate = end;
    }

    /**
     * Returns the range query that is represented by this date range.
     *
     * @return the range query builder
     */
    QueryBuilder getQueryBuilder() {
      RangeQueryBuilder rqb = new RangeQueryBuilder(field);
      if (startDate != null) {
        rqb.from(DateTimeSupport.toUTC(startDate.getTime()));
      }
      if (endDate != null) {
        rqb.to(DateTimeSupport.toUTC(endDate.getTime()));
      }
      return rqb;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof DateRange
              && ((DateRange) obj).field.equals(field);
    }

    @Override
    public int hashCode() {
      return field.hashCode();
    }

  }

  /**
   * Stores a group of values which will later be added to the query using AND.
   */
  public static final class ValueGroup {

    /** The field name */
    private String field;

    /** The values to store */
    private Object[] values;

    /**
     * Creates a new value group for the given field and values.
     *
     * @param field
     *          the field name
     * @param values
     *          the values
     */
    public ValueGroup(String field, Object... values) {
      this.field = field;
      this.values = values;
    }

    /**
     * Returns the filter that will make sure only documents are returned that match all of the values at once.
     *
     * @return the filter builder
     */
    List<QueryBuilder> getFilterBuilders() {
      return Arrays.stream(values)
              .map((v) -> QueryBuilders.termQuery(field, v.toString()))
              .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ValueGroup
              && ((ValueGroup) obj).field.equals(field);
    }

    @Override
    public int hashCode() {
      return field.hashCode();
    }

  }

}
