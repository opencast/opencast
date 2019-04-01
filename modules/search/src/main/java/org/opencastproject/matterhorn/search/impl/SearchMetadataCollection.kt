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

import java.text.MessageFormat
import java.util.ArrayList
import java.util.HashMap

/**
 * Wrapper that facilitates in posting business objects to the search index.
 *
 *
 * This implementation provides utility methods that will ease handling of objects such as dates or users and help
 * prevent posting of `null` values.
 */
class SearchMetadataCollection
/**
 * Creates a new resource metadata collection for the given document type.
 *
 * @param identifier
 * the document identifier
 * @param documentType
 * the document type
 */
(identifier: String?, documentType: String) : Collection<SearchMetadata<*>> {

    /** The metadata  */
    protected var metadata: MutableMap<String, SearchMetadata<*>> = HashMap()

    /** Returns the document identifier  */
    /**
     * Returns the document identifier.
     *
     * @return the identifier
     */
    /**
     * Sets the document identifier.
     *
     * @param identifier
     * the identifier
     */
    var identifier: String? = null

    /** Returns the document type  */
    /**
     * Returns the document type that determines where the document is posted to the index.
     *
     * @return the document type
     */
    var documentType: String? = null
        protected set

    /**
     * Creates a new resource metadata collection for the given document type.
     *
     *
     * Make sure to set the identifier after the fact using [.setIdentifier].
     *
     * @param documentType
     * the document type
     */
    constructor(documentType: String) : this(null, documentType) {}

    init {
        this.identifier = identifier
        this.documentType = documentType
    }

    /**
     * Adds the field and its value to the search index.
     *
     * @param fieldName
     * the field name
     * @param fieldValue
     * the value
     * @param addToText
     * `true` to add the contents to the fulltext field as well
     */
    fun addField(fieldName: String?, fieldValue: Any?, addToText: Boolean) {
        if (fieldName == null)
            throw IllegalArgumentException("Field name cannot be null")
        if (fieldName.contains("."))
            throw IllegalArgumentException("Field name may not contain '.'")
        if (fieldValue == null)
            return

        var m: SearchMetadata<Any>? = metadata[fieldName] as SearchMetadata<Any>
        if (m == null) {
            m = SearchMetadataImpl(fieldName)
            metadata[fieldName] = m
        }

        m.setAddToText(addToText)

        if (fieldValue.javaClass.isArray) {
            val fieldValues = fieldValue as Array<Any>?
            for (v in fieldValues!!) {
                m.addValue(v)
            }
        } else {
            m.addValue(fieldValue)
        }
    }

    /**
     * Returns the localized field name, which is the original field name extended by an underscore and the language
     * identifier.
     *
     * @param fieldName
     * the field name
     * @param language
     * the language
     * @return the localized field name
     */
    protected fun getLocalizedFieldName(fieldName: String, language: Language): String {
        return MessageFormat.format(fieldName, language.identifier)
    }

    /**
     * Returns the metadata as a list of [SearchMetadata] items.
     *
     * @return the metadata items
     */
    fun getMetadata(): List<SearchMetadata<*>> {
        return ArrayList(metadata.values)
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.add
     */
    override fun add(e: SearchMetadata<*>): Boolean {
        return metadata.put(e.name, e) != null
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.addAll
     */
    override fun addAll(c: Collection<SearchMetadata<*>>): Boolean {
        for (m in c) {
            metadata[m.name] = m
        }
        return true
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.clear
     */
    override fun clear() {
        metadata.clear()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.contains
     */
    override operator fun contains(o: Any): Boolean {
        return metadata.values.contains(o)
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.containsAll
     */
    override fun containsAll(c: Collection<*>): Boolean {
        for (o in c) {
            if (!metadata.values.contains(o))
                return false
        }
        return true
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.isEmpty
     */
    override fun isEmpty(): Boolean {
        return metadata.isEmpty()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.iterator
     */
    override fun iterator(): Iterator<SearchMetadata<*>> {
        val result = ArrayList<SearchMetadata<*>>()
        result.addAll(metadata.values)
        return result.iterator()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.remove
     */
    override fun remove(o: Any): Boolean {
        return metadata.remove(o) != null
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.removeAll
     */
    override fun removeAll(c: Collection<*>): Boolean {
        var removed = false
        for (o in c)
            removed = removed or (metadata.remove(o) != null)
        return removed
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.retainAll
     */
    override fun retainAll(c: Collection<*>): Boolean {
        var removed = false
        for (m in metadata.values) {
            if (!c.contains(m)) {
                metadata.remove(m)
                removed = true
            }
        }
        return removed
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.size
     */
    override fun size(): Int {
        return metadata.size
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Collection.toArray
     */
    override fun toArray(): Array<Any> {
        return metadata.values.toTypedArray()
    }

    /**
     * Returns the metadata keys and the metadata items as a map for convenient access of search metadata by key.
     *
     * @return the map
     */
    fun toMap(): Map<String, SearchMetadata<*>> {
        return metadata
    }

    /**
     * @see java.util.Collection.toArray
     */
    override fun <T> toArray(arg0: Array<T>): Array<T> {
        return metadata.values.toTypedArray<SearchMetadataImpl<*>>() as Array<T>
    }

}
