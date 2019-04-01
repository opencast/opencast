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

import org.opencastproject.matterhorn.search.Language
import org.opencastproject.matterhorn.search.SearchMetadata

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

/**
 * Default implementation for the [SearchMetadata].
 */
class SearchMetadataImpl<T> : SearchMetadata<T> {

    /** The name of this metadata item  */
    protected var name: String? = null

    /** Values  */
    protected var values: MutableList<T>? = ArrayList()

    /** Localized values  */
    protected var localizedValues: MutableMap<Language, List<T>>? = HashMap()

    /** True to add the values to the fulltext index  */
    protected var addToText = false

    /**
     * Creates a new metadata object with the given name and values.
     *
     * @param name
     * the metadata name
     * @param values
     * the language neutral values
     * @param localizedValues
     * the localized values
     * @param addToText
     * `true` to add these items to the user facing fulltext index
     */
    constructor(name: String, values: MutableList<T>, localizedValues: MutableMap<Language, List<T>>, addToText: Boolean) {
        this.name = name
        this.values = values
        this.localizedValues = localizedValues
        this.addToText = addToText
    }

    /**
     * Creates a new metadata item with the given name.
     *
     * @param name
     * the name
     */
    constructor(name: String) {
        this.name = name
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.getName
     */
    override fun getName(): String? {
        return name
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.isLocalized
     */
    override fun isLocalized(): Boolean {
        return localizedValues != null && localizedValues!!.size > 0
    }

    /**
     * Adds `value` to the list of language neutral values.
     *
     * @param language
     * the language
     * @param v
     * the value
     */
    override fun addLocalizedValue(language: Language, v: T) {
        if (localizedValues == null)
            localizedValues = HashMap()
        var values: MutableList<T>? = localizedValues!![language]
        if (values == null)
            values = ArrayList()
        if (!values.contains(v))
            values.add(v)
        localizedValues!![language] = values
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.getLocalizedValues
     */
    override fun getLocalizedValues(): Map<Language, List<T>> {
        return if (localizedValues == null) emptyMap() else localizedValues
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.addValue
     */
    override fun addValue(v: T) {
        if (values == null)
            values = ArrayList()
        if (!values!!.contains(v))
            values!!.add(v)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.getValues
     */
    override fun getValues(): List<T> {
        return if (values == null) emptyList() else values
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.getValue
     */
    override fun getValue(): T? {
        return if (values == null || values!!.size == 0) null else values!![0]
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.setAddToText
     */
    override fun setAddToText(addToText: Boolean) {
        this.addToText = addToText
    }

    /**
     * {@inheritDoc}
     */
    override fun addToText(): Boolean {
        return addToText
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchMetadata.clear
     */
    override fun clear() {
        if (values != null)
            values!!.clear()
        if (localizedValues != null)
            localizedValues!!.clear()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return name!!.hashCode()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        return if (obj !is SearchMetadata<*>) false else name == obj.name
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String? {
        return name
    }

}
