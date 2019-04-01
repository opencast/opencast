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

package org.opencastproject.mediapackage

import java.lang.String.format
import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.EqualsUtil.hash

import org.opencastproject.util.RequireUtil

import org.apache.commons.lang3.StringUtils

import java.io.Serializable
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.xml.XMLConstants

/**
 * An XML <dfn>Expanded Name</dfn>, cf. [W3C definition](http://www.w3.org/TR/xml-names11/#dt-expname).
 *
 *
 * Expanded names in XML consists of a namespace name (URI) and a local part. In opposite to <dfn>Qualified Names</dfn>,
 * cf. [W3C definition](http://www.w3.org/TR/xml-names11/#dt-qualname) - which are made from an optional
 * prefix and the local part - expanded names are *not* subject to namespace interpretation.
 *
 *
 * Please see [http://www.w3.org/TR/xml-names/](http://www.w3.org/TR/xml-names/) for a complete definition
 * and reference.
 */
class EName
/**
 * Create a new expanded name.
 *
 * @param namespaceURI
 * the name of the namespace this EName belongs to. If set to [javax.xml.XMLConstants.NULL_NS_URI],
 * this name does not belong to any namespace. Use this option with care.
 * @param localName
 * the local part of the name. Must not be empty.
 */
(
        /**
         * Return the namespace name. Usually the name will be a URI.
         *
         * @return the namespace name or [javax.xml.XMLConstants.NULL_NS_URI] if the name does not belong to a namespace
         */
        val namespaceURI: String,
        /** Return the local part of the name.  */
        val localName: String) : Serializable, Comparable<EName> {

    init {
        RequireUtil.notNull(namespaceURI, "namespaceURI")
        RequireUtil.notEmpty(localName, "localName")
    }

    /**
     * Check, if this name belongs to a namespace, i.e. its namespace URI is not
     * [javax.xml.XMLConstants.NULL_NS_URI].
     */
    fun hasNamespace(): Boolean {
        return XMLConstants.NULL_NS_URI != namespaceURI
    }

    override fun hashCode(): Int {
        return hash(namespaceURI, localName)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is EName && eqFields((that as EName?)!!)
    }

    private fun eqFields(that: EName): Boolean {
        return eq(localName, that.localName) && eq(namespaceURI, that.namespaceURI)
    }

    /** Return a W3C compliant string representation `{namespaceURI}localname`.  */
    override fun toString(): String {
        return format("{%s}%s", namespaceURI, localName)
    }

    override fun compareTo(o: EName): Int {
        val r = namespaceURI.compareTo(o.namespaceURI)
        return if (r == 0) {
            localName.compareTo(o.localName)
        } else {
            r
        }
    }

    companion object {
        private const val serialVersionUID = -5494762745288614634L

        /**
         * A pattern to parse strings as ENames. A String representing an EName may start with a Namespace among curly braces
         * ("{" and "}") and then it must contain a local name *without* any curly braces.
         */
        private val pattern = Pattern.compile("^(?:\\{(?<namespace>[^{}\\s]*)\\})?(?<localname>[^{}\\s]+)$")

        fun mk(namespaceURI: String, localName: String): EName {
            return EName(namespaceURI, localName)
        }

        /**
         * Create a new expanded name which does not belong to a namespace. The namespace name is set to
         * [javax.xml.XMLConstants.NULL_NS_URI].
         */
        fun mk(localName: String): EName {
            return EName(XMLConstants.NULL_NS_URI, localName)
        }

        /**
         * Parse a W3C compliant string representation `{namespaceURI}localname`.
         *
         * A String representing an EName may start with a namespace among curly braces ("{" and "}") and then it must contain
         * a local name *without* any blank characters or curly braces.
         *
         * This is a superset of the character restrictions defined by the XML standard, where neither namespaces nor local
         * names may contain curly braces or spaces.
         *
         * Examples:
         *
         *
         *  * {http://my-namespace}mylocalname
         *  * {}localname-with-explicit-empty-namespace
         *  * localname-without-namespace
         *
         *
         * Incorrect examples:
         *
         *
         *  * {namespace-only}
         *  * contains{curly}braces
         *
         *
         * @param strEName
         * A [java.lang.String] representing an `EName`
         * @param defaultNameSpace
         * A NameSpace to apply if the provided `String` does not have any. Please note that a explicit empty
         * NameSpace **is** a NameSpace. If this argument is blank or `null`, it has no effect.
         */
        @Throws(IllegalArgumentException::class)
        fun fromString(strEName: String, defaultNameSpace: String?): EName {
            val m = pattern.matcher(strEName)

            if (m.matches()) {
                return if (StringUtils.isNotBlank(defaultNameSpace) && m.group("namespace") == null)
                    EName(defaultNameSpace, m.group("localname"))
                else
                    EName(StringUtils.trimToEmpty(m.group("namespace")), m.group("localname"))
            }
            throw IllegalArgumentException(format("Cannot parse '%s' as EName", strEName))
        }

        /**
         * Parse a W3C compliant string representation `{namespaceURI}localname`.
         *
         * A String representing an EName may start with a namespace among curly braces ("{" and "}") and then it must contain
         * a local name *without* any curly braces.
         *
         * This is a superset of the character restrictions defined by the XML standard, where neither namespaces nor local
         * names may contain curly braces.
         *
         * Examples:
         *
         *
         *  * {http://my-namespace}mylocalname
         *  * localname-without-namespace
         *
         *
         * Incorrect examples:
         *
         *
         *  * {namespace-only}
         *  * contains{curly}braces
         *
         *
         * @param strEName
         * A [java.lang.String] representing an `EName`
         */
        @Throws(IllegalArgumentException::class)
        fun fromString(strEName: String): EName {
            return EName.fromString(strEName, null)
        }
    }
}
