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
 * Resource metadata models a piece of metadata that describes one aspect of a
 * resource, e. g. the title.
 */
interface SearchMetadata<T> {

    /**
     * Returns the name of this metadata item.
     *
     * @return the name
     */
    val name: String

    /**
     * Returns the values for this metadata item, mapped to their respective
     * languages.
     *
     * @return the localized values
     */
    val localizedValues: Map<Language, List<T>>

    /**
     * Returns `true` if this metadata item has been localized.
     *
     * @return `true` if the metadata item is localized
     */
    val isLocalized: Boolean

    /**
     * Returns a list of all all non-localized values. In order to retrieve
     * localized values for this metadata field, use [.getLocalizedValues]
     * .
     *
     * @return the list of language neutral values
     */
    val values: List<T>

    /**
     * Returns the first value of the available values or `null` if no
     * value is available.
     *
     * @return the first value
     * @see .getValues
     */
    val value: T

    /**
     * Adds `value` to the list of language neutral values.
     *
     * @param language
     * the language
     * @param v
     * the value
     */
    fun addLocalizedValue(language: Language, v: T)

    /**
     * Adds `value` to the list of language neutral values.
     *
     * @param v
     * the value
     */
    fun addValue(v: T)

    /**
     * Removes all values currently in the metadata container.
     */
    fun clear()

    /**
     * Adds the metadata values to the user facing fulltext index.
     *
     * @param addToFulltext
     * `true` to add the values to the fulltext index
     */
    fun setAddToText(addToFulltext: Boolean)

    /**
     * Returns `true` if the values should be added to the user facing
     * fulltext search index.
     *
     * @return `true` if the metadata values should be added to the
     * fulltext index
     */
    fun addToText(): Boolean

}
