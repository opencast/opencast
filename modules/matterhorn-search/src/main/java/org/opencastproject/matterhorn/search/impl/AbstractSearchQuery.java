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

import static java.util.Objects.requireNonNull;
import static org.opencastproject.matterhorn.search.SearchTerms.Quantifier.Any;

import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchTerms;
import org.opencastproject.matterhorn.search.SearchTerms.Quantifier;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Base implementation for search queries.
 */
public class AbstractSearchQuery implements SearchQuery {

  /** The document types */
  protected List<String> types = new ArrayList<String>();

  /** The list of fields to return */
  protected List<String> fields = null;

  /** Query configuration stack */
  protected Stack<Object> stack = new Stack<Object>();

  /** The object that needs to show up next */
  protected Class<?> expectation = null;

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

  /** The last method called */
  protected String lastMethod = null;

  /**
   * Creates a search query that is executed on all document types.
   */
  public AbstractSearchQuery() {
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
    if (StringUtils.isNotBlank(documentType))
      this.types.add(documentType);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.matterhorn.search.SearchQuery#withTypes(java.lang.String)
   */
  @Override
  public SearchQuery withTypes(String... types) {
    for (String type : types) {
      this.types.add(type);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.matterhorn.search.SearchQuery#getTypes()
   */
  @Override
  public String[] getTypes() {
    return types.toArray(new String[types.size()]);
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withField(String)
   */
  @Override
  public AbstractSearchQuery withField(String field) {
    if (fields == null)
      fields = new ArrayList<String>();
    fields.add(field);
    return this;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withFields(String...)
   */
  @Override
  public AbstractSearchQuery withFields(String... fields) {
    for (String field : fields) {
      withField(field);
    }
    return this;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#getFields()
   */
  @Override
  public String[] getFields() {
    if (fields == null)
      return new String[] {};
    return fields.toArray(new String[fields.size()]);
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withLimit(int)
   */
  @Override
  public SearchQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#getLimit()
   */
  @Override
  public int getLimit() {
    return limit;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withOffset(int)
   */
  @Override
  public SearchQuery withOffset(int offset) {
    this.offset = Math.max(0, offset);
    return this;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#getOffset()
   */
  @Override
  public int getOffset() {
    return offset;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withText(String)
   */
  @Override
  public SearchQuery withText(String text) {
    return withText(false, Any, text);
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withText(boolean, String)
   */
  @Override
  public SearchQuery withText(boolean wildcardSearch, String text) {
    return withText(wildcardSearch, Any, text);
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withText(boolean, Quantifier, String...)
   */
  @Override
  public SearchQuery withText(boolean wildcardSearch, Quantifier quantifier, String... text) {
    if (quantifier == null)
      throw new IllegalArgumentException("Quantifier must not be null");
    if (text == null)
      throw new IllegalArgumentException("Text must not be null");

    // Make sure the collection is initialized
    if (this.text == null)
      this.text = new ArrayList<SearchTerms<String>>();

    // Add the text to the search terms
    clearExpectations();
    this.fuzzySearch = wildcardSearch;
    with(this.text, quantifier, text);
    return this;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#getTerms()
   */
  @Override
  public Collection<SearchTerms<String>> getTerms() {
    if (text == null)
      return Collections.emptyList();
    return text;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#getQueryString()
   */
  @Override
  public String getQueryString() {
    if (text == null)
      return null;
    StringBuffer query = new StringBuffer();
    for (SearchTerms<String> s : text) {
      for (String t : s.getTerms()) {
        if (query.length() == 0)
          query.append(" ");
        query.append(t);
      }
    }
    return query.toString();
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#isFuzzySearch()
   */
  @Override
  public boolean isFuzzySearch() {
    return fuzzySearch;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#withFilter(String)
   */
  @Override
  public SearchQuery withFilter(String filter) {
    clearExpectations();
    this.filter = filter;
    return this;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchQuery#getFilter()
   */
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
    if (!sortOrders.containsKey(field))
      return Order.None;

    return sortOrders.get(field);
  }

  /**
   * Pushes the configuration object onto the stack.
   *
   * @param object
   *          the object
   */
  protected void configure(Object object) {
    stack.push(object);
  }

  /**
   * Sets the expectation to <code>c</code>, making sure that the next configuration object will either match
   * <code>c</code> in terms of class of throw an <code>IllegalStateException</code> if it doesn't.
   *
   * @param c
   *          the class type
   */
  protected void expect(Class<?> c) {
    lastMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
    this.expectation = c;
  }

  /**
   * This method is called if nothing should be expected by anyone. If this is not the case (e. g. some unfinished query
   * configuration is still in place) we throw an <code>IllegalStateException</code>.
   *
   * @throws IllegalStateException
   *           if some object is expected
   */
  protected void clearExpectations() throws IllegalStateException {
    if (expectation != null)
      throw new IllegalStateException("Query configuration expects " + expectation.getClass().getName());
    stack.clear();
  }

  /**
   * This method is called if a certain type of object is expected by someone. If this is not the case (e. g. query
   * configuration is in good shape, then someone tries to "finish" a configuration part) we throw an
   * <code>IllegalStateException</code>.
   *
   * @throws IllegalStateException
   *           if no or a different object is expected
   */
  protected void ensureExpectation(Class<?> c) throws IllegalStateException {
    if (expectation == null)
      throw new IllegalStateException("Malformed query configuration. No " + c.getClass().getName()
              + " is expected at this time");
    if (!expectation.getCanonicalName().equals(c.getCanonicalName()))
      throw new IllegalStateException("Malformed query configuration. Something of type " + c.getClass().getName()
              + " is expected at this time");
    expectation = null;
  }

  /**
   * Make sure that an object of type <code>c</code> is on the stack, throw an <code>IllegalStateException</code>
   * otherwise.
   *
   * @throws IllegalStateException
   *           if no object of type <code>c</code> was found on the stack
   */
  protected void ensureConfigurationObject(Class<?> c) throws IllegalStateException {
    for (Object o : stack) {
      if (c.isAssignableFrom(o.getClass()))
        return;
    }
    throw new IllegalStateException("Malformed query configuration. No " + c.getClass().getName()
            + " is expected at this time");
  }

  /**
   * Make sure that an array of type <code>c</code> is on the stack, throw an <code>IllegalStateException</code>
   * otherwise.
   *
   * @throws IllegalStateException
   *           if no array of type <code>c</code> was found on the stack
   */
  protected void ensureConfigurationArray(Class<?> c) throws IllegalStateException {
    for (Object o : stack) {
      if (o.getClass().isArray() && c.isAssignableFrom(o.getClass().getComponentType()))
        return;
    }
    throw new IllegalStateException("Malformed query configuration. No " + c.getClass().getName()
            + " is expected at this time");
  }

  /**
   * Utility method to add the given values to the list of search terms using the specified quantifier.
   *
   * @param searchTerms
   *          the terms
   * @param quantifier
   *          the quantifier
   * @param values
   *          the values
   * @return the extended search terms
   */
  protected <T extends Object> SearchTerms<T> with(List<SearchTerms<T>> searchTerms, Quantifier quantifier, T... values) {
    SearchTerms<T> terms = null;

    // Handle any quantifier
    if (values.length == 1 || Any.equals(quantifier)) {

      // Check if there is a default terms collection
      for (SearchTerms<T> t : searchTerms) {
        if (Quantifier.Any.equals(t.getQuantifier())) {
          terms = t;
          break;
        }
      }

      // Has there been a default terms collection?
      if (terms == null) {
        terms = new SearchTermsImpl<T>(Quantifier.Any, values);
        searchTerms.add(terms);
      }

      // Add the text
      for (T v : values) {
        terms.add(v);
      }
    }

    // All quantifier
    else {
      terms = new SearchTermsImpl<T>(quantifier, values);
      searchTerms.add(terms);
    }

    return terms;
  }

}
