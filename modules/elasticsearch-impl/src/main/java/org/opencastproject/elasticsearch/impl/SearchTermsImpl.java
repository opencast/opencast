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

import org.opencastproject.elasticsearch.api.SearchTerms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of a list of search terms.
 */
public class SearchTermsImpl<T> implements SearchTerms<T> {

  /** The quantifier */
  protected Quantifier quantifier;

  /** The search terms */
  protected List<T> terms = new ArrayList<>();

  /**
   * Creates a list of search terms, to be queried using the given quantifier.
   *
   * @param quantifier
   *          the quantifier
   * @param values
   *          the initial values
   */
  public SearchTermsImpl(Quantifier quantifier, T... values) {
    this.quantifier = quantifier;
    for (T value : values) {
      if (!this.terms.contains(value)) {
        this.terms.add(value);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see SearchTerms#add(java.lang.Object)
   */
  @Override
  public void add(T term) {
    if (!this.terms.contains(term)) {
      this.terms.add(term);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see SearchTerms#getTerms()
   */
  @Override
  public Collection<T> getTerms() {
    return terms;
  }

  /**
   * {@inheritDoc}
   * 
   * @see SearchTerms#contains(java.lang.Object)
   */
  @Override
  public boolean contains(T term) {
    return terms.contains(term);
  }

  /**
   * {@inheritDoc}
   * 
   * @see SearchTerms#size()
   */
  @Override
  public int size() {
    return terms.size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see SearchTerms#getQuantifier()
   */
  @Override
  public Quantifier getQuantifier() {
    return quantifier;
  }

}
