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


package org.opencastproject.matterhorn.search;

import java.util.Collection;

/**
 * Interface for a data structure that is wrapping a group of search terms.
 */
public interface SearchTerms<T extends Object> {

  /** The search quantifier */
  public enum Quantifier {
    All, Any
  };

  /**
   * Adds a term to this list of terms.
   * 
   * @param term
   *          the new term
   */
  void add(T term);

  /**
   * Returns the terms.
   * 
   * @return the terms
   */
  Collection<T> getTerms();

  /**
   * Returns <code>true</code> if <code>term</code> is contained in the list of
   * terms.
   * 
   * @param term
   *          the term
   * @return <code>true</code> if <code>term</code> is contained
   */
  boolean contains(T term);

  /**
   * Returns the number of terms.
   * 
   * @return the number of terms
   */
  int size();

  /**
   * Returns this group's quantifier.
   * 
   * @return the quantifier
   */
  Quantifier getQuantifier();

}
