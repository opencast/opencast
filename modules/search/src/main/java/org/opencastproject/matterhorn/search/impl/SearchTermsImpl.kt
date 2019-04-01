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


package org.opencastproject.matterhorn.search.impl

import org.opencastproject.matterhorn.search.SearchTerms

import java.util.ArrayList

/**
 * Implementation of a list of search terms.
 */
class SearchTermsImpl<T>
/**
 * Creates a list of search terms, to be queried using the given quantifier.
 *
 * @param quantifier
 * the quantifier
 * @param values
 * the initial values
 */
(
        /** The quantifier  */
        protected var quantifier: SearchTerms.Quantifier, vararg values: T) : SearchTerms<T> {

    /** The search terms  */
    protected var terms: MutableList<T> = ArrayList()

    init {
        for (value in values) {
            if (!this.terms.contains(value))
                this.terms.add(value)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchTerms.add
     */
    override fun add(term: T) {
        if (!this.terms.contains(term))
            this.terms.add(term)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchTerms.getTerms
     */
    override fun getTerms(): Collection<T> {
        return terms
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchTerms.contains
     */
    override fun contains(term: T): Boolean {
        return terms.contains(term)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchTerms.size
     */
    override fun size(): Int {
        return terms.size
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchTerms.getQuantifier
     */
    override fun getQuantifier(): SearchTerms.Quantifier {
        return quantifier
    }

}
