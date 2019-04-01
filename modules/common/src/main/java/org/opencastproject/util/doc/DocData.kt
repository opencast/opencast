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

package org.opencastproject.util.doc

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.LinkedHashMap
import java.util.Vector

/**
 * This is the document model class which is the basis for all doc data models
 */
open class DocData
/**
 * Create a new DocData object
 *
 * @param name
 * the name of the document (must be alphanumeric (includes _) and no spaces or special chars)
 * @param title
 * [OPTIONAL] the title of the document
 * @param notes
 * [OPTIONAL] an array of notes to add into the document
 */
(name: String, title: String, notes: Array<String>?) {

    /**
     * This is the document meta data
     */
    protected var meta: MutableMap<String, String>
    protected var notes: MutableList<String>

    /**
     * @return the default template path for this type of document data
     */
    open val defaultTemplatePath: String
        get() = TEMPLATE_DEFAULT

    init {
        var title = title
        if (!isValidName(name)) {
            throw IllegalArgumentException("name must be set and only alphanumeric")
        }
        if (isBlank(title)) {
            title = name
        }
        this.meta = LinkedHashMap()
        this.meta["name"] = name
        this.meta["title"] = title
        // notes
        this.notes = Vector(3)
        if (notes != null && notes.size > 0) {
            for (i in notes.indices) {
                this.notes.add(notes[i])
            }
        }
        logger.debug("Created new Doc: {}", name)
    }

    /**
     * @return the map version of the data in this doc data holder
     * @throws IllegalArgumentException
     * if the data cannot be turned into a valid map
     */
    open fun toMap(): Map<String, Any> {
        val m = LinkedHashMap<String, Any>()
        m["meta"] = this.meta
        m["notes"] = this.notes
        return m
    }

    /**
     * Add a note to the document
     *
     * @param note
     * the text of the note
     */
    fun addNote(note: String?) {
        if (note != null && "" != note) {
            this.notes.add(note)
        }
    }

    fun getMetaData(key: String): String {
        return meta[key]
    }

    fun getMeta(): Map<String, String> {
        return meta // never null
    }

    fun getNotes(): List<String> {
        return notes // never null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DocData::class.java!!)
        protected val TEMPLATE_DEFAULT = "/ui/restdocs/template.xhtml"

        fun isBlank(str: String?): Boolean {
            var blank = false
            if (str == null || "" == str) {
                blank = true
            }
            return blank
        }

        fun isValidName(name: String): Boolean {
            var valid = true
            if (isBlank(name)) {
                valid = false
            } else {
                if (!name.matches("^(\\w|-)+$".toRegex())) {
                    valid = false
                }
            }
            return valid
        }
    }

}
