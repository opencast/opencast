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

package org.opencastproject.opensearch.impl;

import static org.opencastproject.opensearch.impl.IndexSchema.SEARCH_FIELD_NAME_EXTENSION;

import org.opencastproject.opensearch.api.SearchQuery;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.index.query.MatchNoneQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryRewriteContext;
import org.opensearch.index.query.QueryShardContext;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.index.query.SpanNearQueryBuilder;
import org.opensearch.index.query.SpanQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.index.query.WildcardQueryBuilder;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Opencast implementation of the OpenSearch query builder.
 */
public abstract class AbstractOpenSearchQueryBuilder<T extends SearchQuery> implements QueryBuilder {

  /** Score boost for documents containing the search text as a phrase of complete words */
  private static final float PHRASE_MATCH_BOOST = 3.0f;

  /** Score boost for documents containing the search text as a phrase whose last word may be a prefix */
  private static final float PHRASE_PREFIX_MATCH_BOOST = 0.5f;

  /** Score boost for documents whose title starts with the search text (the last word may be a prefix) */
  private static final float TITLE_START_MATCH_BOOST = 10.0f;

  /**
   * Up to this many tokens, the search text is also ranked by phrase and title start. Tokens are counted like the
   * index splits words, including camel case ("AnalysisTwo" is two). The queries expand to a clause per token and
   * field, so longer texts would exceed the clause limit of OpenSearch and fail the search.
   */
  private static final int MAX_RANKING_TOKENS = 10;

  /** Up to this many tokens, the search is also matched with typos in the last word and words in different fields. */
  private static final int MAX_FUZZY_TOKENS = 6;

  /** Stricter than AUTO, which allows typos in short words and numbers ("2023" matches "2024"). */
  private static final Fuzziness WORD_FUZZINESS = Fuzziness.build("AUTO:4,8");

  /** The default weights of fields in text searches: a match in the title counts three times as much. */
  public static final Map<String, Float> DEFAULT_FIELD_BOOSTS = Map.of("title", 3.0f);

  /** Term queries on fields */
  private Map<String, Set<Object>> searchTerms = null;
  private Map<String, Set<Object>> searchTermsNot = null;

  /** Wildcard query on fields */
  private Map<String, String> wildcardTerms = null;
  private Map<String, String> wildcardTermsNot = null;

  /** Fields that need to match all values */
  protected List<ValueGroup> groups = null;

  /** Fields that query a date range */
  private Set<DateRange> dateRanges = null;

  private Map<String, String> datesGreaterThan = null;

  private Map<String, String> datesLessThan = null;

  /** Filter expression */
  protected String filter = null;

  /** Text query */
  protected String text = null;

  protected List<String> additionalMultiQueryFields = new ArrayList<>();

  /** Fuzzy text query */
  protected boolean fuzzy = false;

  /** Score boosts of fields when searching by text, by field name */
  private final Map<String, Float> fieldBoosts;

  /** The original search query */
  private final T query;

  /** The boolean query */
  private QueryBuilder queryBuilder = null;

  /**
   * Creates a new OpenSearch query based on the raw query.
   *
   * @param query
   *          the search query
   */
  public AbstractOpenSearchQueryBuilder(T query) {
    this(query, DEFAULT_FIELD_BOOSTS);
  }

  /** Creates a new OpenSearch query based on the raw query, with the weights of fields in text searches. */
  public AbstractOpenSearchQueryBuilder(T query, Map<String, Float> fieldBoosts) {
    this.query = query;
    this.fieldBoosts = fieldBoosts;
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
        booleanQuery.filter(new TermsQueryBuilder(entry.getKey(), entry.getValue().toArray(new Object[0])));
      }
      this.queryBuilder = booleanQuery;
    }

    // Terms
    if (searchTermsNot != null) {
      for (Map.Entry<String, Set<Object>> entry : searchTermsNot.entrySet()) {
        booleanQuery.mustNot(new TermsQueryBuilder(entry.getKey(), entry.getValue().toArray(new Object[0])));
      }
      this.queryBuilder = booleanQuery;
    }

    // Wildcard
    if (wildcardTerms != null) {
      for (Map.Entry<String, String> entry : wildcardTerms.entrySet()) {
        booleanQuery.must(new WildcardQueryBuilder(entry.getKey(), entry.getValue()));
      }
      this.queryBuilder = booleanQuery;
    }

    // Wildcard
    if (wildcardTermsNot != null) {
      for (Map.Entry<String, String> entry : wildcardTermsNot.entrySet()) {
        booleanQuery.mustNot(new WildcardQueryBuilder(entry.getKey(), entry.getValue()));
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

    // Dates greater/less than
    if (datesGreaterThan != null) {
      for (Map.Entry<String, String> entry : datesGreaterThan.entrySet()) {
        RangeQueryBuilder rqb = new RangeQueryBuilder(entry.getKey());
        booleanQuery.must(rqb.gt(entry.getValue()));
      }
      this.queryBuilder = booleanQuery;
    }

    if (datesLessThan != null) {
      for (Map.Entry<String, String> entry : datesLessThan.entrySet()) {
        RangeQueryBuilder rqb = new RangeQueryBuilder(entry.getKey());
        booleanQuery.must(rqb.lt(entry.getValue()));
      }
      this.queryBuilder = booleanQuery;
    }

    // Text
    if (text != null) {
      MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(text);
      queryBuilder.field("*" + SEARCH_FIELD_NAME_EXTENSION, 1.2f);
      // Search-as-you-type generates useful subfields for autocompletion, we should use it
      // https://docs.opensearch.org/1.3/field-types/supported-field-types/search-as-you-type/
      queryBuilder.field("*" + SEARCH_FIELD_NAME_EXTENSION + "._2gram", 1.2f);
      queryBuilder.field("*" + SEARCH_FIELD_NAME_EXTENSION + "._3gram", 1.2f);
      queryBuilder.field("*" + SEARCH_FIELD_NAME_EXTENSION + "._4gram", 1.2f);
      additionalMultiQueryFields.forEach(field -> queryBuilder.field(field, 1.0f));
      queryBuilder.type(MultiMatchQueryBuilder.Type.BOOL_PREFIX);
      queryBuilder.operator(Operator.AND);
      if (fuzzy) {
        queryBuilder.fuzziness(Fuzziness.AUTO);
      }
      // The prefix query scores every match the same, so rank the results by optional phrase queries. Only the better
      // of phrase and phrase prefix counts, and the latter is weighted low: it favors long words over the exact one.
      // The title is weighted above other fields, as a short value elsewhere would otherwise outrank it.
      // Lenient, as not every "*.search" field is a text field and phrase queries fail on the others.
      List<String> words = Arrays.stream(text.split("[^\\p{L}\\p{N}]+")).filter(word -> !word.isEmpty())
          .collect(Collectors.toList());
      // Count tokens like the index does, which also splits camel case words
      long tokens = Arrays.stream(text.split("[^\\p{L}\\p{N}]+|(?<=\\p{Ll})(?=\\p{Lu})"))
          .filter(token -> !token.isEmpty()).count();
      boolean rankText = tokens <= MAX_RANKING_TOKENS;
      boolean fuzzyText = tokens <= MAX_FUZZY_TOKENS;
      // Numbers are not matched with typos (a text of only symbols has no words at all)
      boolean lastWordHasLetters = !words.isEmpty() && words.get(words.size() - 1).matches(".*\\p{L}.*");
      BoolQueryBuilder textQuery = QueryBuilders.boolQuery();
      // A document matches if any of these alternatives matches
      List<QueryBuilder> alternatives = new ArrayList<>();
      alternatives.add(queryBuilder);
      if (fuzzy && fuzzyText && lastWordHasLetters) {
        // The prefix query does not tolerate typos in the last word, so also accept documents containing all words
        // with typos. Scored, so that these results are ordered by relevance too.
        alternatives.add(fuzzyWordsQuery());
      }
      if (fuzzyText && words.size() > 1) {
        // The queries above need all words in the same field. People also search for e.g. a word of the title and
        // the name of the presenter, so also accept documents where the words are spread over different fields.
        alternatives.add(wordsInAnyFieldQuery(words));
      }
      if (alternatives.size() == 1) {
        textQuery.must(queryBuilder);
      } else {
        BoolQueryBuilder anyAlternative = QueryBuilders.boolQuery().minimumShouldMatch(1);
        alternatives.forEach(anyAlternative::should);
        textQuery.must(anyAlternative);
      }
      if (rankText) {
        DisMaxQueryBuilder phraseQuery = QueryBuilders.disMaxQuery();
        phraseQuery.add(searchableFieldsQuery(text, MultiMatchQueryBuilder.Type.PHRASE).boost(PHRASE_MATCH_BOOST));
        phraseQuery.add(searchableFieldsQuery(text, MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
            .boost(PHRASE_PREFIX_MATCH_BOOST));
        textQuery.should(phraseQuery);
        // Rank titles starting with the text before those which merely contain it
        textQuery.should(titleStartQuery());
      }
      booleanQuery.minimumShouldMatch(1);
      booleanQuery.should(textQuery);
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
      filters.add(QueryBuilders.termQuery("*" + SEARCH_FIELD_NAME_EXTENSION, filter));
    }

    // Apply the filters
    if (!filters.isEmpty()) {
      for (QueryBuilder filter : filters) {
        booleanQuery.filter(filter);
      }
      this.queryBuilder = booleanQuery;
    }

  }

  /** Matches titles starting with the search text, the last word as a prefix. Only ranks, so constant score. */
  private QueryBuilder titleStartQuery() {
    // Approximates the analysis of the field: where it deviates, the title is still found, but not boosted
    String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD)
        .replaceAll("\\p{M}+", "").replace("ß", "ss");
    List<String> terms = Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
        .filter(term -> !term.isEmpty())
        .collect(Collectors.toList());
    if (terms.isEmpty()) {
      return new MatchNoneQueryBuilder();
    }
    String field = "title" + SEARCH_FIELD_NAME_EXTENSION;
    int last = terms.size() - 1;
    SpanQueryBuilder lastWord = QueryBuilders.spanMultiTermQueryBuilder(
        QueryBuilders.prefixQuery(field, terms.get(last)));
    SpanQueryBuilder title = lastWord;
    if (last > 0) {
      SpanNearQueryBuilder words = QueryBuilders.spanNearQuery(QueryBuilders.spanTermQuery(field, terms.get(0)), 0);
      terms.subList(1, last).forEach(term -> words.addClause(QueryBuilders.spanTermQuery(field, term)));
      title = words.addClause(lastWord).inOrder(true);
    }
    return QueryBuilders.constantScoreQuery(QueryBuilders.spanFirstQuery(title, terms.size()))
        .boost(TITLE_START_MATCH_BOOST);
  }

  /** Matches fields containing all words of the search text, each of them with typos allowed. */
  private MultiMatchQueryBuilder fuzzyWordsQuery() {
    return searchableFieldsQuery(text, MultiMatchQueryBuilder.Type.BEST_FIELDS)
        .operator(Operator.AND)
        .fuzziness(WORD_FUZZINESS)
        // A fixed first letter is much cheaper
        .prefixLength(1);
  }

  /** Matches documents containing every word in any field, not necessarily the same one. */
  private QueryBuilder wordsInAnyFieldQuery(List<String> words) {
    BoolQueryBuilder query = QueryBuilders.boolQuery();
    for (int i = 0; i < words.size(); i++) {
      String word = words.get(i);
      if (i == words.size() - 1) {
        query.must(searchableFieldsQuery(word, MultiMatchQueryBuilder.Type.BOOL_PREFIX));
      } else {
        MultiMatchQueryBuilder wordQuery = searchableFieldsQuery(word, MultiMatchQueryBuilder.Type.BEST_FIELDS);
        if (fuzzy && word.matches(".*\\p{L}.*")) {
          wordQuery.fuzziness(WORD_FUZZINESS).prefixLength(1);
        }
        query.must(wordQuery);
      }
    }
    return query;
  }

  /** Creates a lenient query of the given type on all searchable fields, weighted by the configured boosts. */
  private MultiMatchQueryBuilder searchableFieldsQuery(String query, MultiMatchQueryBuilder.Type type) {
    MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(query)
        .field("*" + SEARCH_FIELD_NAME_EXTENSION)
        .type(type)
        .lenient(true);
    fieldBoosts.forEach((field, boost) -> {
      if (boost != 1.0f) {
        queryBuilder.field(field + SEARCH_FIELD_NAME_EXTENSION, boost);
      }
    });
    return queryBuilder;
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
   * @param fieldValues
   *          the field value
   */
  protected void andNot(String fieldName, Object... fieldValues) {

    // Make sure the data structures are set up accordingly
    if (searchTermsNot == null) {
      searchTermsNot = new HashMap<>();
    }

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // insert value
    searchTermsNot.computeIfAbsent(fieldName, k -> new HashSet<>())
        .addAll(Arrays.asList(fieldValues));
  }

  /**
   * Stores <code>fieldValue</code> as a wildcard term on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValue
   *          the field value
   */
  protected void andWildcard(String fieldName, String fieldValue) {

    // Make sure the data structures are set up accordingly
    if (wildcardTerms == null) {
      wildcardTerms = new HashMap<>();
    }

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // insert value
    wildcardTerms.put(fieldName, fieldValue);
  }

  /**
   * Stores <code>fieldValue</code> as a wildcard term on the <code>fieldName</code> field.
   *
   * @param fieldName
   *          the field name
   * @param fieldValue
   *          the field value
   */
  protected void andWildcardNot(String fieldName, String fieldValue) {

    // Make sure the data structures are set up accordingly
    if (wildcardTermsNot == null) {
      wildcardTermsNot = new HashMap<>();
    }

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // insert value
    wildcardTermsNot.put(fieldName, fieldValue);
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

  protected void andDateGreaterThan(String fieldName, String date) {

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // Make sure the data structures are set up accordingly
    if (datesGreaterThan == null) {
      datesGreaterThan = new HashMap();
    }

    // Add the term
    datesGreaterThan.put(fieldName, date);
  }

  protected void andDateLessThan(String fieldName, String date) {

    // Fix the field name, just in case
    fieldName = StringUtils.trim(fieldName);

    // Make sure the data structures are set up accordingly
    if (datesLessThan == null) {
      datesLessThan = new HashMap();
    }

    // Add the term
    datesLessThan.put(fieldName, date);
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
  public QueryBuilder filter(QueryBuilder filter) {
    return queryBuilder.filter(filter);
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
   * Utility class to hold date range specifications and turn them into OpenSearch queries.
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
