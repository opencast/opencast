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

import static java.util.Objects.requireNonNull;
import static org.opencastproject.elasticsearch.api.SearchTerms.Quantifier.Any;

import org.opencastproject.elasticsearch.api.SearchQuery;
import org.opencastproject.elasticsearch.api.SearchTerms;
import org.opencastproject.elasticsearch.api.SearchTerms.Quantifier;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation for search queries.
 */
public class AbstractSearchQuery implements SearchQuery {

  /** The document types */
  protected List<String> types = new ArrayList<>();

  /** The list of fields to return */
  protected List<String> fields = null;

  /** True if the search text should be matched using wildcards */
  protected boolean fuzzySearch = true;

  /** Query terms */
  protected List<SearchTerms<String>> text = null;

  /** Filter terms */
  protected String filter = null;

  /** The query offset */
  protected int offset = -1;

  /** The query limit */
  protected int limit = -1;

  /** The map with the sort orders */
  private final Map<String, Order> sortOrders = new LinkedHashMap<String, Order>();

  /**
   * Creates a search query that is executed on all document types.
   */
  protected AbstractSearchQuery() {
    // Nothing to be done atm.
  }

  /**
   * Creates a search query that is executed on the given document type.
   *
   * @param documentType
   *          the document type
   */
  public AbstractSearchQuery(String documentType) {
    this();
    if (StringUtils.isNotBlank(documentType)) {
      this.types.add(documentType);
    }
  }

  @Override
  public SearchQuery withTypes(String... types) {
    this.types.addAll(Arrays.asList(types));
    return this;
  }

  @Override
  public String[] getTypes() {
    return types.toArray(new String[0]);
  }

  @Override
  public AbstractSearchQuery withField(String field) {
    if (fields == null) {
      fields = new ArrayList<>();
    }
    fields.add(field);
    return this;
  }

  @Override
  public AbstractSearchQuery withFields(String... fields) {
    for (String field : fields) {
      withField(field);
    }
    return this;
  }

  @Override
  public String[] getFields() {
    if (fields == null) {
      return new String[] {};
    }
    return fields.toArray(new String[0]);
  }

  @Override
  public SearchQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public SearchQuery withOffset(int offset) {
    this.offset = Math.max(0, offset);
    return this;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public SearchQuery withText(String text) {
    return withText(false, Any, text);
  }

  @Override
  public SearchQuery withText(boolean wildcardSearch, String text) {
    return withText(wildcardSearch, Any, text);
  }

  @Override
  public SearchQuery withText(boolean wildcardSearch, Quantifier quantifier, String... text) {
    if (quantifier == null) {
      throw new IllegalArgumentException("Quantifier must not be null");
    }
    if (text == null) {
      throw new IllegalArgumentException("Text must not be null");
    }

    // Make sure the collection is initialized
    if (this.text == null) {
      this.text = new ArrayList<>();
    }

    // Add the text to the search terms
    this.fuzzySearch = wildcardSearch;

    // Handle any quantifier
    if (text.length == 1 || Any.equals(quantifier)) {
      SearchTerms<String> terms = null;

      // Check if there is a default terms collection
      for (SearchTerms<String> t : this.text) {
        if (Quantifier.Any.equals(t.getQuantifier())) {
          terms = t;
          break;
        }
      }

      // Has there been a default terms collection?
      if (terms == null) {
        terms = new SearchTermsImpl<>(Quantifier.Any, text);
        this.text.add(terms);
      }

    // All quantifier
    } else {
      this.text.add(new SearchTermsImpl<>(quantifier, text));
    }
    return this;
  }

  @Override
  public Collection<SearchTerms<String>> getTerms() {
    if (text == null) {
      return Collections.emptyList();
    }
    return text;
  }

  @Override
  public String getQueryString() {
    if (text == null) {
      return null;
    }
    StringBuilder query = new StringBuilder();
    for (SearchTerms<String> s : text) {
      for (String t : s.getTerms()) {
        if (query.length() == 0) {
          query.append(" ");
        }
        query.append(t);
      }
    }
    return query.toString();
  }

  @Override
  public boolean isFuzzySearch() {
    return fuzzySearch;
  }

  @Override
  public SearchQuery withFilter(String filter) {
    this.filter = filter;
    return this;
  }

  @Override
  public String getFilter() {
    return filter;
  }

  @Override
  public SearchQuery withSortOrder(String field, Order order) {
    sortOrders.put(requireNonNull(field), requireNonNull(order));
    return this;
  }

  @Override
  public Map<String, Order> getSortOrders() {
    return Collections.unmodifiableMap(sortOrders);
  }

  @Override
  public Order getSortOrder(String field) {
    if (!sortOrders.containsKey(field)) {
      return Order.None;
    }

    return sortOrders.get(field);
  }

}
