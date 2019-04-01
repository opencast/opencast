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

import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.EqualsUtil.hash

import java.io.Serializable

/**
 * Declaration of an XML namespace binding which is the association of a prefix to a namespace URI (namespace name).
 *
 *
 * See [W3C specification](http://www.w3.org/TR/xml-names11/#sec-namespaces) for details.
 */
class XmlNamespaceBinding
/**
 * Bind a prefix to a namespace URI (namespace name).
 *
 * @param prefix a prefix or the empty string ([javax.xml.XMLConstants.DEFAULT_NS_PREFIX]) to bind
 * the default namespace
 * @param namespaceURI Either a URI or the empty string ([javax.xml.XMLConstants.NULL_NS_URI]).
 * See [Declaring Namespaces](http://www.w3.org/TR/REC-xml-names/#ns-decl)
 * for details about namespace declarations.
 */
(prefix: String, namespaceURI: String) : Serializable {

    val prefix: String
    val namespaceURI: String

    init {
        this.prefix = RequireUtil.notNull(prefix, "prefix")
        this.namespaceURI = RequireUtil.notNull(namespaceURI, "namespaceURI")
    }

    override fun hashCode(): Int {
        return hash(prefix, namespaceURI)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is XmlNamespaceBinding && eqFields((that as XmlNamespaceBinding?)!!)
    }

    private fun eqFields(that: XmlNamespaceBinding): Boolean {
        return eq(prefix, that.prefix) && eq(namespaceURI, that.namespaceURI)
    }

    companion object {
        private const val serialVersionUID = -3189348197739705012L

        /**
         * Constructor method.
         *
         * @see org.opencastproject.util.XmlNamespaceBinding.XmlNamespaceBinding
         */
        fun mk(prefix: String, namespaceURI: String): XmlNamespaceBinding {
            return XmlNamespaceBinding(prefix, namespaceURI)
        }
    }
}
