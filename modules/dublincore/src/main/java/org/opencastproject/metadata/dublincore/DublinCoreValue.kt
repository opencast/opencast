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

package org.opencastproject.metadata.dublincore

import java.lang.String.format
import org.opencastproject.util.EqualsUtil.eq

import org.opencastproject.mediapackage.EName
import org.opencastproject.util.EqualsUtil
import org.opencastproject.util.RequireUtil

import com.entwinemedia.fn.data.Opt

import java.io.Serializable

import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.concurrent.Immutable

/**
 * Representation of a DublinCore conforming property value.
 *
 *
 * See [http://dublincore.org/documents/dc-xml-guidelines/](http://dublincore.org/documents/dc-xml-guidelines/) for
 * further details.
 */
@Immutable
@ParametersAreNonnullByDefault
class DublinCoreValue
/**
 * Create a new Dublin Core value.
 *
 * @param value
 * the value
 * @param language
 * the language (two letter ISO 639)
 * @param encodingScheme
 * the encoding scheme used to encode the value
 */
(value: String, language: String, encodingScheme: Opt<EName>) : Serializable {

    /**
     * Return the value of the property.
     */
    val value: String
    /**
     * Return the language.
     */
    val language: String
    /**
     * Return the encoding scheme.
     */
    val encodingScheme: Opt<EName>

    init {
        this.value = RequireUtil.notNull(value, "value")
        this.language = RequireUtil.notNull(language, "language")
        this.encodingScheme = RequireUtil.notNull(encodingScheme, "encodingScheme")
    }

    fun hasEncodingScheme(): Boolean {
        return encodingScheme.isSome
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is DublinCoreValue && eqFields((that as DublinCoreValue?)!!)
    }

    private fun eqFields(that: DublinCoreValue): Boolean {
        return eq(value, that.value) && eq(language, that.language) && eq(encodingScheme, that.encodingScheme)
    }

    override fun hashCode(): Int {
        return EqualsUtil.hash(value, language, encodingScheme)
    }

    override fun toString(): String {
        return format("DublinCoreValue(%s,%s,%s)", value, language, encodingScheme)
    }

    companion object {
        private const val serialVersionUID = 7660583858714438266L

        /**
         * Create a new Dublin Core value.
         *
         * @param value
         * the value
         * @param language
         * the language (two letter ISO 639)
         * @param encodingScheme
         * the encoding scheme used to encode the value
         */
        fun mk(value: String, language: String, encodingScheme: Opt<EName>): DublinCoreValue {
            return DublinCoreValue(value, language, encodingScheme)
        }

        /**
         * Create a new Dublin Core value.
         *
         * @param value
         * the value
         * @param language
         * the language (two letter ISO 639)
         * @param encodingScheme
         * the encoding scheme used to encode the value
         */
        fun mk(value: String, language: String, encodingScheme: EName): DublinCoreValue {
            return DublinCoreValue(value, language, Opt.some(encodingScheme))
        }

        /**
         * Creates a new Dublin Core value without an encoding scheme.
         *
         * @param value
         * the value
         * @param language
         * the language (two letter ISO 639)
         */
        fun mk(value: String, language: String): DublinCoreValue {
            return DublinCoreValue(value, language, Opt.none())
        }

        /**
         * Create a new Dublin Core value with the language set to undefined and no particular encoding scheme.
         *
         * @param value
         * the value
         * @see org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_UNDEFINED
         */
        fun mk(value: String): DublinCoreValue {
            return DublinCoreValue(value, DublinCore.LANGUAGE_UNDEFINED, Opt.none())
        }
    }
}
