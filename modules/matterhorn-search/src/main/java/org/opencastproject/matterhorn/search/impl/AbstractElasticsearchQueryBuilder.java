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


package org.opencastproject.matterhorn.search.impl;

import static org.opencastproject.matterhorn.search.impl.IndexSchema.TEXT;
import static org.opencastproject.matterhorn.search.impl.IndexSchema.TEXT_FUZZY;

import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FuzzyLikeThisQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matterhorn implementation of the elastic search query builder.
 */
public abstract class AbstractElasticsearchQueryBuilder<T extends SearchQuery> implements QueryBuilder {

  /** Term queries on fields */
  private Map<String, Set<Object>> searchTerms = null;

  /** Negative term queries on fields */
  private Map<String, Set<Object>> negativeSearchTerms = null;

  /** Fields that must be empty */
  private Set<String> emptyFields = null;

  /** Fields that need to match all values */
  protected List<ValueGroup> groups = null;

  /** Fields that must not be empty */
  private Set<String> nonEmptyFields = null;

  /** Fields that query a date range */
  private Set<DateRange> dateRanges = null;

  /** Filter expression */
  protected String filter = null;

  /** Text query */
  protected String text = null;

  /** Fuzzy text query */
  protected String fuzzyText = null;

  /** The original search query */
  private T query = null;

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

  /**
   * {@inheritDoc}
   *
   * @see org.elasticsearch.index.query.BaseQueryBuilder#doXContent(org.elasticsearch.common.xcontent.XContentBuilder,
   *      org.elasticsearch.common.xcontent.ToXContent.Params)
   */
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
        Set<Object> values = entry.getValue();
        if (values.size() == 1)
          booleanQuery.must(new TermsQueryBuilder(entry.getKey(), values.iterator().next()));
        else
          booleanQuery.must(new TermsQueryBuilder(entry.getKey(), values.toArray(new String[values.size()])));
      }
      this.queryBuilder = booleanQuery;
    }

    // Negative terms
    if (negativeSearchTerms != null) {
      for (Map.Entry<String, Set<Object>> entry : negativeSearchTerms.entrySet()) {
        Set<Object> values = entry.getValue();
        if (values.size() == 1)
          booleanQuery.mustNot(new TermsQueryBuilder(entry.getKey(), values.iterator().next()));
        else
          booleanQuery.mustNot(new TermsQueryBuilder(entry.getKey(), values.toArray(new String[values.size()])));
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
      QueryStringQueryBuilder queryBuilder = QueryBuilders.queryString(text).field(TEXT);
      booleanQuery.must(queryBuilder);
      this.queryBuilder = booleanQuery;
    }

    // Fuzzy text
    if (fuzzyText != null) {
      FuzzyLikeThisQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyLikeThisQuery(TEXT_FUZZY).likeText(fuzzyText);
      booleanQuery.must(fuzzyQueryBuilder);
      this.queryBuilder = booleanQuery;
    }

    QueryBuilder unfilteredQuery = queryBuilder;
    List<FilterBuilder> filters = new ArrayList<FilterBuilder>();

    // Add filtering for AND terms
    if (groups != null) {
      for (ValueGroup group : groups) {
        filters.addAll(group.getFilterBuilders());
      }
    }

    // Non-Empty fields
    if (nonEmptyFields != null) {
      for (String field : nonEmptyFields) {
        filters.add(FilterBuilders.existsFilter(field));
      }
    }

    // Empty fields
    if (emptyFields != null) {
      for (String field : emptyFields) {
        filters.add(FilterBuilders.missingFilter(field));
      }
    }

    // Filter expressions
    if (filter != null) {
      filters.add(FilterBuilders.termFilter(IndexSchema.TEXT, filter));
    }

    // Apply the filters
    if (filters.size() == 1) {
      this.queryBuilder = QueryBuilders.filteredQuery(unfilteredQuery, filters.get(0));
    } else if (filters.size() > 1) {
      FilterBuilder andFilter = FilterBuilders.andFilter(filters.toArray(new FilterBuilder[filters.size()]));
      this.queryBuilder = QueryBuilders.filteredQuery(unfilteredQuery, andFilter);
    }

  }

  /**
   * Stores <code>fieldValue</code> as a search term on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValue
   *          the field value
   * @param clean
   *          <code>true</code> to escape solr special characters in the field value
   */
  protected void and(String fieldName, Object fieldValue, boolean clean) {

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // Make sure the data structures are set up accordingly
    if (searchTerms == null)
      searchTerms = new HashMap<String, Set<Object>>();
    Set<Object> termValues = searchTerms.get(fieldName);
    if (termValues == null) {
      termValues = new HashSet<Object>();
      searchTerms.put(fieldName, termValues);
    }

    // Add the term
    termValues.add(fieldValue);
  }

  /**
   * Stores <code>fieldValues</code> as search terms on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValues
   *          the field value
   * @param clean
   *          <code>true</code> to escape solr special characters in the field value
   */
  protected void and(String fieldName, Object[] fieldValues, boolean clean) {
    for (Object v : fieldValues) {
      and(fieldName, v, clean);
    }
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
    if (dateRanges == null)
      dateRanges = new HashSet<DateRange>();

    // Add the term
    DateRange dateRange = new DateRange(fieldName, startDate, endDate);
    dateRanges.add(dateRange);
  }

  /**
   * Stores <code>fieldValue</code> as a negative search term on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValue
   *          the field value
   * @param clean
   *          <code>true</code> to escape solr special characters in the field value
   */
  protected void andNot(String fieldName, Object fieldValue, boolean clean) {

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // Make sure the data structures are set up accordingly
    if (negativeSearchTerms == null)
      negativeSearchTerms = new HashMap<String, Set<Object>>();
    Set<Object> termValues = negativeSearchTerms.get(fieldName);
    if (termValues == null) {
      termValues = new HashSet<Object>();
      negativeSearchTerms.put(fieldName, termValues);
    }

    // Add the term
    termValues.add(fieldValue);
  }

  /**
   * Stores <code>fieldValues</code> as negative search terms on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValues
   *          the field value
   * @param clean
   *          <code>true</code> to escape solr special characters in the field value
   */
  protected void andNot(String fieldName, Object[] fieldValues, boolean clean) {
    for (Object v : fieldValues) {
      andNot(fieldName, v, clean);
    }
  }

  /**
   * Encodes the field name as part of the AND clause of a solr query: <tt>AND -fieldName : [* TO *]</tt>.
   *
   * @param fieldName
   *          the field name
   */
  protected void andEmpty(String fieldName) {
    if (emptyFields == null)
      emptyFields = new HashSet<String>();
    emptyFields.add(StringUtils.trim(fieldName));
  }

  /**
   * Encodes the field name as part of the AND clause of a solr query: <tt>AND fieldName : ["" TO *]</tt>.
   *
   * @param fieldName
   *          the field name
   */
  protected void andNotEmpty(String fieldName) {
    if (nonEmptyFields == null)
      nonEmptyFields = new HashSet<String>();
    nonEmptyFields.add(StringUtils.trim(fieldName));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.elasticsearch.common.xcontent.ToXContent#toXContent(org.elasticsearch.common.xcontent.XContentBuilder,
   *      org.elasticsearch.common.xcontent.ToXContent.Params)
   */
  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    return queryBuilder.toXContent(builder, params);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.elasticsearch.index.query.QueryBuilder#buildAsBytes()
   */
  @Override
  public BytesReference buildAsBytes() throws ElasticsearchException {
    return queryBuilder.buildAsBytes();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.elasticsearch.index.query.QueryBuilder#buildAsBytes(org.elasticsearch.common.xcontent.XContentType)
   */
  @Override
  public BytesReference buildAsBytes(XContentType contentType) {
    return queryBuilder.buildAsBytes(contentType);
  }

  /**
   * Utility class to hold date range specifications and turn them into elastic search queries.
   */
  public static final class DateRange {

    /** The field name */
    private String field = null;

    /** The start date */
    private Date startDate = null;

    /** The end date */
    private Date endDate = null;

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
    public DateRange(String field, Date start, Date end) {
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
      if (startDate != null)
        rqb.from(DateTimeSupport.toUTC(startDate.getTime()));
      if (endDate != null)
        rqb.to(DateTimeSupport.toUTC(endDate.getTime()));
      return rqb;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DateRange) {
        return ((DateRange) obj).field.equals(field);
      }
      return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
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
    private String field = null;

    /** The values to store */
    private Object[] values = null;

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
    List<FilterBuilder> getFilterBuilders() {
      List<FilterBuilder> filters = new ArrayList<FilterBuilder>(values.length);
      for (Object v : values) {
        filters.add(FilterBuilders.termFilter(field, v.toString()));
      }
      return filters;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DateRange) {
        return ((DateRange) obj).field.equals(field);
      }
      return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return field.hashCode();
    }

  }

}
