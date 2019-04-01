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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.matterhorn.search

/**
 * Interface for a data structure that is wrapping a group of search terms.
 */
interface SearchTerms<T : Any> {

    /**
     * Returns the terms.
     *
     * @return the terms
     */
    val terms: Collection<T>

    /**
     * Returns this group's quantifier.
     *
     * @return the quantifier
     */
    val quantifier: Quantifier

    /** The search quantifier  */
    enum class Quantifier {
        All, Any
    }

    /**
     * Adds a term to this list of terms.
     *
     * @param term
     * the new term
     */
    fun add(term: T)

    /**
     * Returns `true` if `term` is contained in the list of
     * terms.
     *
     * @param term
     * the term
     * @return `true` if `term` is contained
     */
    operator fun contains(term: T): Boolean

    /**
     * Returns the number of terms.
     *
     * @return the number of terms
     */
    fun size(): Int

}
