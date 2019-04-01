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


package org.opencastproject.util

import org.opencastproject.util.EqualsUtil.eqObj
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.none

import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * This class implements the mime type. Note that mime types should not be instantiated directly but be retreived from
 * the mime type registry [MimeTypes].
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "mimetype", namespace = "http://mediapackage.opencastproject.org")
@XmlJavaTypeAdapter(MimeType.Adapter::class)
class MimeType
/**
 * Creates a new mime type with the given type and subtype.
 *
 * @param type
 * the major type
 * @param subtype
 * minor type
 */
private constructor(
        /** String representation of type  */
        /**
         * Returns the major type of this mimetype.
         *
         *
         * For example, if the mimetype is ISO Motion JPEG 2000 which is represented as `video/mj2`, this method
         * will return `video`.
         *
         * @return the type
         */
        val type: String,
        /** String representation of subtype  */
        /**
         * Returns the minor type of this mimetype.
         *
         *
         * For example, if the mimetype is ISO Motion JPEG 2000 which is represented as `video/mj2`, this method
         * will return `mj2`.
         *
         * @return the subtype
         */
        val subtype: String,
        /** List of suffixes, the first is the main one.  */
        private val suffixes: List<String>,
        /** Alternate representations for type/subtype  */
        private val equivalents: List<MimeType>,
        /** Main description  */
        /**
         * Returns the mime type description.
         *
         * @return the description
         */
        val description: Option<String>,
        /** The mime type flavor  */
        /**
         * Returns the flavor of this mime type.
         *
         *
         * A flavor is a hint on a specialized variant of a general mime type. For example, a dublin core file will have a
         * mime type of `text/xml`. Adding a flavor of `mpeg-7` gives an additional hint on the file
         * contents.
         *
         * @return the file's flavor
         */
        val flavor: Option<String>,
        /** The mime type flavor description  */
        /**
         * Returns the flavor description.
         *
         * @return the flavor description
         */
        val flavorDescription: Option<String>) : Comparable<MimeType>, Serializable {

    /**
     * Returns the main suffix for this mime type, that identifies files containing data of this flavor.
     *
     *
     * For example, files with the suffix `mj2` will contain data of type `video/mj2`.
     *
     * @return the file suffix
     */
    val suffix: Option<String>
        get() = mlist(suffixes).headOpt()

    /** [.eq] as a function.  */
    // CHECKSTYLE:OFF
    val eq: Function<MimeType, Boolean> = object : Function<MimeType, Boolean>() {
        override fun apply(other: MimeType): Boolean {
            return eq(other)
        }
    }

    /**
     * Returns the registered suffixes for this mime type, that identify files containing data of this flavor. Note that
     * the list includes the main suffix returned by `getSuffix()`.
     *
     *
     * For example, files containing ISO Motion JPEG 2000 may have file suffixes `mj2` and `mjp2`.
     *
     * @return the registered file suffixes
     */
    fun getSuffixes(): Array<String> {
        return suffixes.toTypedArray<String>()
    }

    /**
     * Returns `true` if the mimetype supports the specified suffix.
     *
     * @return `true` if the suffix is supported
     */
    fun supportsSuffix(suffix: String): Boolean {
        return suffixes.contains(suffix.toLowerCase())
    }

    /**
     * Returns `true` if the file has the given flavor associated.
     *
     * @return `true` if the file has that flavor
     */
    fun hasFlavor(flavor: String?): Boolean {
        return flavor?.equals(flavor, ignoreCase = true) ?: false
    }

    /**
     * Returns the MimeType as a string of the form `type/subtype`
     */
    @Deprecated("use {@link #toString()} instead")
    fun asString(): String {
        return toString()
    }

    /** Two mime types are considered equal if type and subtype are equal.  */
    fun eq(other: MimeType): Boolean {
        return eq(other.type, other.subtype)
    }

    /** Two mime types are considered equal if type and subtype are equal.  */
    fun eq(type: String, subtype: String): Boolean {
        return this.type.equals(type, ignoreCase = true) && this.subtype.equals(subtype, ignoreCase = true)
    }
    // CHECKSTYLE:ON

    /**
     * Returns `true` if this mime type is an equivalent for the specified type and subtype.
     *
     *
     * For example, a gzipped file may have both of these mime types defined, `application/x-compressed` or
     * `application/x-gzip`.
     *
     * @return `true` if this mime type is equal
     */
    fun isEquivalentTo(type: String, subtype: String): Boolean {
        return eq(type, subtype) || mlist(equivalents).exists(eq)
    }

    /**
     * @see java.lang.Comparable.compareTo
     */
    override fun compareTo(m: MimeType): Int {
        return toString().compareTo(m.toString())
    }

    /**
     * Returns the MimeType as a string of the form `type/subtype`
     */
    override fun toString(): String {
        return "$type/$subtype"
    }

    override fun hashCode(): Int {
        return EqualsUtil.hash(type, subtype)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is MimeType && eqFields((that as MimeType?)!!)
    }

    private fun eqFields(that: MimeType): Boolean {
        return eqObj(this.type, that.type) && eqObj(this.subtype, that.subtype)
    }

    internal class Adapter : XmlAdapter<String, MimeType>() {
        @Throws(Exception::class)
        override fun marshal(mimeType: MimeType): String {
            return mimeType.type + "/" + mimeType.subtype
        }

        @Throws(Exception::class)
        override fun unmarshal(str: String): MimeType? {
            try {
                return MimeTypes.parseMimeType(str)
            } catch (e: Exception) {
                logger.info("unable to parse mimetype {}", str)
                return null
            }

        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MimeType::class.java!!)

        /** Serial version UID  */
        private const val serialVersionUID = -2895494708659187394L

        fun mimeType(type: String, subtype: String, suffixes: List<String>,
                     equivalents: List<MimeType>,
                     description: Option<String>,
                     flavor: Option<String>, flavorDescription: Option<String>): MimeType {
            return MimeType(type, subtype, suffixes, equivalents, description, flavor, flavorDescription)
        }

        fun mimeType(type: String, subtype: String, suffix: String): MimeType {
            return MimeType(type, subtype, list(suffix), Collections.nil(), none(""), none(""), none(""))
        }

        fun mimeType(type: String, subtype: String): MimeType {
            return MimeType(type, subtype, Collections.nil(), Collections.nil(), none(""), none(""), none(""))
        }
    }
}
