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

import org.opencastproject.matterhorn.search.impl.IndexSchema.FUZZY_FIELDNAME_EXTENSION

import org.opencastproject.matterhorn.search.Language
import org.opencastproject.matterhorn.search.SearchMetadata

import java.text.MessageFormat
import java.util.ArrayList
import java.util.HashMap

/**
 * Document that encapsulates business objects and offers support for adding those objects to a search index.
 */
class ElasticsearchDocument
/**
 * Creates a new elastic search document based on the id, the document type and the metadata.
 *
 *
 * Note that the type needs to map to an Elasticsearch document type mapping.
 *
 * @param id
 * the resource identifier.
 * @param type
 * the document type
 * @param resource
 * the resource metadata
 */
(id: String, type: String, resource: List<SearchMetadata<*>>) : HashMap<String, Any>() {

    /** The document identifier  */
    /**
     * Returns the identifier.
     *
     * @return the identifier
     */
    val uid: String? = null

    /** The document type  */
    /**
     * Returns the document type.
     *
     * @return the type
     */
    val type: String? = null

    init {
        this.uid = id
        this.type = type
        for (entry in resource) {
            val metadataKey = entry.name
            put(metadataKey, entry.values)

            // TODO Not sure what to use for localizedFulltextFieldName
            if (entry.addToText())
                addToFulltext(entry, IndexSchema.TEXT, IndexSchema.TEXT)
        }
    }

    /**
     * Adds the resource metadata to the designated fulltext fields.
     *
     * @param item
     * the metadata item
     * @param fulltextFieldName
     * the fulltext field name
     * @param localizedFulltextFieldName
     * the localized fulltext field name
     */
    private fun addToFulltext(item: SearchMetadata<*>, fulltextFieldName: String, localizedFulltextFieldName: String) {

        // Get existing fulltext entries
        var fulltext: MutableCollection<String>? = get(fulltextFieldName) as Collection<String>
        if (fulltext == null) {
            fulltext = ArrayList()
            put(fulltextFieldName, fulltext)
            put(fulltextFieldName + FUZZY_FIELDNAME_EXTENSION, fulltext)
        }

        // Language neutral elements
        for (value in item.values) {
            if (value.javaClass.isArray) {
                val fieldValues = value as Array<Any>
                for (v in fieldValues) {
                    fulltext.add(v.toString())
                }
            } else {
                fulltext.add(value.toString())
            }
        }

        // Add localized metadata values
        for (language in item.localizedValues.keys) {
            // Get existing fulltext entries
            val localizedFieldName = MessageFormat.format(localizedFulltextFieldName, language.identifier)
            val localizedFulltext = get(localizedFieldName) as Collection<String>
            if (fulltext == null) {
                fulltext = ArrayList()
                put(localizedFieldName, fulltext)
            }
            val values = item.localizedValues[language]
            for (value in values) {
                if (value.javaClass.isArray()) {
                    val fieldValues = value as Array<Any>
                    for (v in fieldValues) {
                        localizedFulltext.add(v.toString())
                    }
                } else {
                    localizedFulltext.add(value.toString())
                }
            }
        }

    }

    companion object {

        /** Serial version uid  */
        private val serialVersionUID = 2687550418831284487L
    }

}
