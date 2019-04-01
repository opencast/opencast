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

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.data.Option.option

import org.opencastproject.util.data.Function0

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Booleans

import java.util.Collections
import java.util.HashMap
import kotlin.collections.Map.Entry

import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

class XmlNamespaceContext
/**
 * Create a new namespace context with bindings from prefix to URI and bind the
 * default namespaces as described in the documentation of [javax.xml.namespace.NamespaceContext].
 */
(prefixToUri: Map<String, String>) : NamespaceContext {

    // prefix -> namespace URI
    private val prefixToUri = HashMap<String, String>()

    val bindings: List<XmlNamespaceBinding>
        get() = `$`<Entry<String, String>>(prefixToUri.entries).map(toBinding).toList()

    init {
        this.prefixToUri.putAll(prefixToUri)
        this.prefixToUri[XMLConstants.XML_NS_PREFIX] = XMLConstants.XML_NS_URI
        this.prefixToUri[XMLConstants.XMLNS_ATTRIBUTE] = XMLConstants.XMLNS_ATTRIBUTE_NS_URI
    }

    override fun getNamespaceURI(prefix: String): String {
        return Opt.nul(prefixToUri[prefix]).getOr(XMLConstants.NULL_NS_URI)
    }

    override fun getPrefix(uri: String): String {
        return `$`<Entry<String, String>>(prefixToUri.entries).find(Booleans.eq(RequireUtil.notNull(uri, "uri")).o(value)).map(key).orNull()
    }

    override fun getPrefixes(uri: String): Iterator<*> {
        return `$`<Entry<String, String>>(prefixToUri.entries).filter(Booleans.eq(uri).o(value)).map(key).iterator()
    }

    /** Create a new context with the given bindings added. Existing bindings will not be overwritten.  */
    fun add(vararg bindings: XmlNamespaceBinding): XmlNamespaceContext {
        return add(`$`(*bindings))
    }

    /** Create a new context with the given bindings added. Existing bindings will not be overwritten.  */
    fun add(bindings: XmlNamespaceContext): XmlNamespaceContext {
        return if (bindings.prefixToUri.size == DEFAULT_BINDINGS) {
            // bindings contains only the default bindings
            this
        } else {
            add(`$`(bindings.bindings))
        }
    }

    private fun add(bindings: Stream<XmlNamespaceBinding>): XmlNamespaceContext {
        return mk(bindings.append(bindings))
    }

    fun merge(precedence: NamespaceContext): NamespaceContext {
        return merge(this, precedence)
    }

    companion object {
        // the number of default bindings
        private val DEFAULT_BINDINGS = 2

        fun mk(prefixToUri: Map<String, String>): XmlNamespaceContext {
            return XmlNamespaceContext(prefixToUri)
        }

        fun mk(vararg bindings: XmlNamespaceBinding): XmlNamespaceContext {
            return mk(`$`(*bindings))
        }

        fun mk(prefix: String, namespaceUri: String): XmlNamespaceContext {
            return XmlNamespaceContext(Collections.singletonMap(prefix, namespaceUri))
        }

        fun mk(bindings: List<XmlNamespaceBinding>): XmlNamespaceContext {
            return mk(`$`(bindings))
        }

        fun mk(bindings: Stream<XmlNamespaceBinding>): XmlNamespaceContext {
            return XmlNamespaceContext(
                    bindings.foldl(
                            HashMap(),
                            object : Fn2<HashMap<String, String>, XmlNamespaceBinding, HashMap<String, String>>() {
                                override fun apply(
                                        prefixToUri: HashMap<String, String>, binding: XmlNamespaceBinding): HashMap<String, String> {
                                    prefixToUri[binding.prefix] = binding.namespaceURI
                                    return prefixToUri
                                }
                            }))
        }

        private val key = object : Fn<Entry<String, String>, String>() {
            override fun apply(e: Entry<String, String>): String {
                return e.key
            }
        }

        private val value = object : Fn<Entry<String, String>, String>() {
            override fun apply(e: Entry<String, String>): String {
                return e.value
            }
        }

        private val toBinding = object : Fn<Entry<String, String>, XmlNamespaceBinding>() {
            override fun apply(e: Entry<String, String>): XmlNamespaceBinding {
                return XmlNamespaceBinding(e.key, e.value)
            }
        }

        /** Merge `b` into `a` so that `b` takes precedence over `a`.  */
        fun merge(a: NamespaceContext, b: NamespaceContext): NamespaceContext {
            return object : NamespaceContext {
                override fun getNamespaceURI(prefix: String): String? {
                    val uri = b.getNamespaceURI(prefix)
                    return if (eq(XMLConstants.DEFAULT_NS_PREFIX, prefix) && eq(XMLConstants.NULL_NS_URI, uri)) {
                        a.getNamespaceURI(prefix)
                    } else {
                        uri
                    }
                }

                override fun getPrefix(uri: String): String {
                    return option(b.getPrefix(uri)).getOrElse(object : Function0<String>() {
                        override fun apply(): String {
                            return a.getPrefix(uri)
                        }
                    })
                }

                override fun getPrefixes(uri: String): Iterator<*> {
                    val prefixes = b.getPrefixes(uri)
                    return if (prefixes.hasNext()) {
                        prefixes
                    } else {
                        a.getPrefixes(uri)
                    }
                }
            }
        }
    }
}
