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

package org.opencastproject.series.api

import org.apache.commons.lang3.StringUtils

import java.util.Date

/**
 * Query object used for storing search parameters.
 *
 */
class SeriesQuery {

    /** Maximum number of results returned  */
    protected var count: Int = 0
    /** Start page number  */
    protected var startPage: Int = 0
    /** Free text search  */
    protected var text: String
    /** Series id search  */
    protected var seriesId: String
    /** Option for an inexact series id match  */
    /**
     * Option for partial matches on a series Id. The default is exact match.
     *
     * @return true for allowing fuzzy match on series Id, false for exact match
     */
    /**
     * Set the fuzzyMatch boolean flag
     *
     * @param fuzzyMatch
     */
    var isFuzzyMatch: Boolean = false
    /** Series title search  */
    protected var seriesTitle: String
    /** Creator search  */
    protected var creator: String
    /** Contributor search  */
    protected var contributor: String
    /** Language search  */
    protected var language: String
    /** License search  */
    protected var license: String
    /** Subject search  */
    protected var subject: String
    /** Publisher search  */
    protected var publisher: String
    /** Abstract search  */
    /**
     * Get abstract
     *
     * @return
     */
    var abstract: String
        protected set
    /** Description search  */
    protected var description: String
    /** Created from search  */
    protected var createdFrom: Date
    /** Created to search  */
    protected var createdTo: Date
    /** Available from search  */
    protected var availableFrom: Date
    /** Available to search  */
    protected var availableTo: Date
    /** Rights holder search  */
    protected var rightsHolder: String
    /**
     * Show only series for which the current user may edit the series (true), otherwise show series for which the current
     * user can contribute content toward the series
     */
    /**
     * Whether the results for this query should contain only series that the current user can edit.
     *
     * @return
     */
    /**
     * Set the edit flag.
     *
     * @param edit
     */
    var isEdit: Boolean = false
    /** Sort by field  */
    /**
     * Get sort field
     *
     * @return
     */
    var sort = Sort.TITLE
        protected set
    /** Sort order  */
    /**
     * Whether sort order is ascending
     *
     * @return
     */
    var isSortAscending = true
        protected set

    /** Sort fields  */
    enum class Sort {
        TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, TEMPORAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE, IDENTIFIER
    }

    /**
     * Set search by license
     *
     * @param license
     * @return
     */
    fun setLicense(license: String): SeriesQuery {
        if (StringUtils.isNotBlank(license)) {
            this.license = license.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by abstract
     *
     * @param seriesAbstract
     * @return
     */
    fun setSeriesAbstract(seriesAbstract: String): SeriesQuery {
        if (StringUtils.isNotBlank(seriesAbstract)) {
            this.abstract = seriesAbstract.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set maximum number of results
     *
     * @param count
     * @return
     */
    fun setCount(count: Int): SeriesQuery {
        this.count = count
        return this
    }

    /**
     * Set start page
     *
     * @param startPage
     * @return
     */
    fun setStartPage(startPage: Int): SeriesQuery {
        this.startPage = startPage
        return this
    }

    /**
     * Set search by created from
     *
     * @param createdFrom
     * @return
     */
    fun setCreatedFrom(createdFrom: Date): SeriesQuery {
        this.createdFrom = createdFrom
        return this
    }

    /**
     * Set search by created to
     *
     * @param createdTo
     * @return
     */
    fun setCreatedTo(createdTo: Date): SeriesQuery {
        this.createdTo = createdTo
        return this
    }

    /**
     * Set search over all text fields
     *
     * @param text
     * @return
     */
    fun setText(text: String): SeriesQuery {
        if (StringUtils.isNotBlank(text)) {
            this.text = text.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by series id
     *
     * @param seriesId
     * @return
     */
    fun setSeriesId(seriesId: String): SeriesQuery {
        if (StringUtils.isNotBlank(seriesId)) {
            this.seriesId = seriesId.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by series title
     *
     * @param seriesTitle
     * @return
     */
    fun setSeriesTitle(seriesTitle: String): SeriesQuery {
        if (StringUtils.isNotBlank(seriesTitle)) {
            this.seriesTitle = seriesTitle.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by creator
     *
     * @param creator
     * @return
     */
    fun setCreator(creator: String): SeriesQuery {
        if (StringUtils.isNotBlank(creator)) {
            this.creator = creator.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by contributor
     *
     * @param contributor
     * @return
     */
    fun setContributor(contributor: String): SeriesQuery {
        if (StringUtils.isNotBlank(contributor)) {
            this.contributor = contributor.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by language
     *
     * @param language
     * @return
     */
    fun setLanguage(language: String): SeriesQuery {
        if (StringUtils.isNotBlank(language)) {
            this.language = language.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by subject
     *
     * @param subject
     * @return
     */
    fun setSubject(subject: String): SeriesQuery {
        if (StringUtils.isNotBlank(subject)) {
            this.subject = subject.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by publisher
     *
     * @param publisher
     * @return
     */
    fun setPublisher(publisher: String): SeriesQuery {
        if (StringUtils.isNotBlank(subject)) {
            this.publisher = publisher.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by description
     *
     * @param description
     * @return
     */
    fun setDescription(description: String): SeriesQuery {
        if (StringUtils.isNotBlank(subject)) {
            this.description = description.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set search by available from
     *
     * @param availableFrom
     * @return
     */
    fun setAvailableFrom(availableFrom: Date): SeriesQuery {
        this.availableFrom = availableFrom
        return this
    }

    /**
     * Set search by available to
     *
     * @param availableTo
     * @return
     */
    fun setAvailableTo(availableTo: Date): SeriesQuery {
        this.availableTo = availableTo
        return this
    }

    /**
     * Set search by rights holder
     *
     * @param rightsHolder
     * @return
     */
    fun setRightsHolder(rightsHolder: String): SeriesQuery {
        if (StringUtils.isNotBlank(subject)) {
            this.rightsHolder = rightsHolder.trim { it <= ' ' }
        }
        return this
    }

    /**
     * Set sort field with sort order
     *
     * @param sort
     * @param ascending
     * @return
     */
    @JvmOverloads
    fun withSort(sort: Sort, ascending: Boolean = true): SeriesQuery {
        this.sort = sort
        this.isSortAscending = ascending
        return this
    }

    /**
     * Get result count
     *
     * @return
     */
    fun getCount(): Int {
        return count
    }

    /**
     * Get start page
     *
     * @return
     */
    fun getStartPage(): Int {
        return startPage
    }

    /**
     * Get text
     *
     * @return
     */
    fun getText(): String {
        return text
    }

    /**
     * Get series id
     *
     * @return
     */
    fun getSeriesId(): String {
        return seriesId
    }

    /**
     * Get series title
     *
     * @return
     */
    fun getSeriesTitle(): String {
        return seriesTitle
    }

    /**
     * Get creator
     *
     * @return
     */
    fun getCreator(): String {
        return creator
    }

    /**
     * Get contributor
     *
     * @return
     */
    fun getContributor(): String {
        return contributor
    }

    /**
     * Get language
     *
     * @return
     */
    fun getLanguage(): String {
        return language
    }

    /**
     * Get license
     *
     * @return
     */
    fun getLicense(): String {
        return license
    }

    /**
     * Get subject
     *
     * @return
     */
    fun getSubject(): String {
        return subject
    }

    /**
     * Get publisher
     *
     * @return
     */
    fun getPublisher(): String {
        return publisher
    }

    /**
     * Get description
     *
     * @return
     */
    fun getDescription(): String {
        return description
    }

    /**
     * Get created from
     *
     * @return
     */
    fun getCreatedFrom(): Date {
        return createdFrom
    }

    /**
     * Get created to
     *
     * @return
     */
    fun getCreatedTo(): Date {
        return createdTo
    }

    /**
     * Get available from
     *
     * @return
     */
    fun getAvailableFrom(): Date {
        return availableFrom
    }

    /**
     * Get available to
     *
     * @return
     */
    fun getAvailableTo(): Date {
        return availableTo
    }

    /**
     * Get rights holder
     *
     * @return
     */
    fun getRightsHolder(): String {
        return rightsHolder
    }

}
/**
 * Set sort field with ascending order
 *
 * @param sort
 * @return
 */
