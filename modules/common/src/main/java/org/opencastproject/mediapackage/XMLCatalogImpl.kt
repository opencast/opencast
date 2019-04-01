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

import com.entwinemedia.fn.Stream.`$`
import java.lang.String.format
import javax.xml.XMLConstants.DEFAULT_NS_PREFIX
import javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI
import javax.xml.XMLConstants.XMLNS_ATTRIBUTE
import javax.xml.XMLConstants.XML_NS_URI
import org.opencastproject.util.EqualsUtil.hash

import org.opencastproject.util.RequireUtil
import org.opencastproject.util.XmlNamespaceBinding
import org.opencastproject.util.XmlNamespaceContext

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fns
import com.entwinemedia.fn.P2
import com.entwinemedia.fn.fns.Booleans

import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.Attributes

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import kotlin.collections.Map.Entry

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * This is a basic implementation for handling simple catalogs of metadata. It provides utility methods to store
 * key-value data.
 *
 *
 * For a definition of the terms <dfn>expanded name</dfn>, <dfn>qualified name</dfn> or <dfn>QName</dfn>, <dfn>namespace
 * prefix</dfn>, <dfn>local part</dfn> and <dfn>local name</dfn>, please see [http://www.w3.org/TR/REC-xml-names](http://www.w3.org/TR/REC-xml-names)
 *
 *
 * By default the following namespace prefixes are bound:
 *
 *  * xml - http://www.w3.org/XML/1998/namespace
 *  * xmlns - http://www.w3.org/2000/xmlns/
 *  * xsi - http://www.w3.org/2001/XMLSchema-instance
 *
 *
 *
 * <h3>Limitations</h3>
 * XMLCatalog supports only *one* prefix binding per namespace name, so you cannot create documents like the
 * following using XMLCatalog:
 *
 * <pre>
 * &lt;root xmlns:x=&quot;http://x.demo.org&quot; xmlns:y=&quot;http://x.demo.org&quot;&gt;
 * &lt;x:elem&gt;value&lt;/x:elem&gt;
 * &lt;y:elem&gt;value&lt;/y:elem&gt;
 * &lt;/root&gt;
</pre> *
 *
 * However, reading of those documents is supported.
 */
abstract class XMLCatalogImpl
/**
 * Create an empty catalog and register the [javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI]
 * namespace.
 */
protected constructor() : CatalogImpl(), XMLCatalog {

    /** To marshaling empty fields to remove existing values during merge, default is not to marshal empty elements  */
    protected var includeEmpty = false

    /** Key (QName) value meta data  */
    protected val data: MutableMap<EName, List<CatalogEntry>> = HashMap()

    /** Namespace - prefix bindings  */
    protected var bindings: XmlNamespaceContext

    protected val entriesSorted: List<CatalogEntry>
        get() = `$`(data.values)
                .bind(Fns.id())
                .sort(catalogEntryComparator)
                .toList()

    init {
        bindings = XmlNamespaceContext.mk(XSI_NS_PREFIX, W3C_XML_SCHEMA_INSTANCE_NS_URI)
    }

    protected fun addBinding(binding: XmlNamespaceBinding) {
        bindings = bindings.add(binding)
    }

    /**
     * Clears the catalog.
     */
    protected open fun clear() {
        data.clear()
    }

    /**
     * Adds the element to the metadata collection.
     *
     * @param element
     * the expanded name of the element
     * @param value
     * the value
     */
    protected fun addElement(element: EName?, value: String) {
        if (element == null)
            throw IllegalArgumentException("Expanded name must not be null")

        addElement(CatalogEntry(element, value, NO_ATTRIBUTES))
    }

    /**
     * Adds the element with the `xml:lang` attribute to the metadata collection.
     *
     * @param element
     * the expanded name of the element
     * @param value
     * the value
     * @param language
     * the language identifier (two letter ISO 639)
     */
    protected fun addLocalizedElement(element: EName, value: String, language: String) {
        RequireUtil.notNull(element, "expanded name")
        RequireUtil.notNull(language, "language")

        val attributes = HashMap<EName, String>(1)
        attributes[XML_LANG_ATTR] = language
        addElement(CatalogEntry(element, value, attributes))
    }

    /**
     * Adds the element with the `xsi:type` attribute to the metadata collection.
     *
     * @param value
     * the value
     * @param type
     * the element type
     */
    protected fun addTypedElement(element: EName, value: String, type: EName) {
        RequireUtil.notNull(element, "expanded name")
        RequireUtil.notNull(type, "type")

        val attributes = HashMap<EName, String>(1)
        attributes[XSI_TYPE_ATTR] = toQName(type)
        addElement(CatalogEntry(element, value, attributes))
    }

    /**
     * Adds an element with the `xml:lang` and `xsi:type` attributes to the metadata collection.
     *
     * @param element
     * the expanded name of the element
     * @param value
     * the value
     * @param language
     * the language identifier (two letter ISO 639)
     * @param type
     * the element type
     */
    protected fun addTypedLocalizedElement(element: EName?, value: String, language: String?, type: EName?) {
        if (element == null)
            throw IllegalArgumentException("EName name must not be null")
        if (type == null)
            throw IllegalArgumentException("Type must not be null")
        if (language == null)
            throw IllegalArgumentException("Language must not be null")

        val attributes = HashMap<EName, String>(2)
        attributes[XML_LANG_ATTR] = language
        attributes[XSI_TYPE_ATTR] = toQName(type)
        addElement(CatalogEntry(element, value, attributes))
    }

    /**
     * Adds an element with attributes to the catalog.
     *
     * @param element
     * the expanded name of the element
     * @param value
     * the element's value
     * @param attributes
     * the attributes. May be null
     */
    protected open fun addElement(element: EName?, value: String, attributes: Attributes?) {
        if (element == null)
            throw IllegalArgumentException("Expanded name must not be null")

        val attributeMap = HashMap<EName, String>()
        if (attributes != null) {
            for (i in 0 until attributes.length) {
                attributeMap[EName(attributes.getURI(i), attributes.getLocalName(i))] = attributes.getValue(i)
            }
        }
        addElement(CatalogEntry(element, value, attributeMap))
    }

    /**
     * Adds the catalog element to the list of elements.
     *
     * @param element
     * the element
     */
    private fun addElement(element: CatalogEntry?) {

        // Option includeEmpty allows marshaling empty elements
        // for deleting existing values during a catalog merge
        if (element == null)
            return
        if (StringUtils.trimToNull(element.value) == null && !includeEmpty)
            return
        var values: MutableList<CatalogEntry>? = data[element.eName]
        if (values == null) {
            values = ArrayList()
            data[element.eName] = values
        }
        values.add(element)
    }

    /**
     * Completely removes an element.
     *
     * @param element
     * the expanded name of the element
     */
    protected fun removeElement(element: EName) {
        removeValues(element, null, true)
    }

    /**
     * Removes all entries in a certain language from an element.
     *
     * @param element
     * the expanded name of the element
     * @param language
     * the language code (two letter ISO 639) or null to *only* remove entries without an
     * `xml:lang` attribute
     */
    protected fun removeLocalizedValues(element: EName, language: String) {
        removeValues(element, language, false)
    }

    /**
     * Removes values from an element or the complete element from the catalog.
     *
     * @param element
     * the expanded name of the element
     * @param language
     * the language code (two letter ISO 639) to remove or null to remove entries without language code
     * @param all
     * true - remove all entries for that element. This parameter overrides the language parameter.
     */
    private fun removeValues(element: EName, language: String?, all: Boolean) {
        if (all) {
            data.remove(element)
        } else {
            val entries = data[element]
            if (entries != null) {
                val i = entries.iterator()
                while (i.hasNext()) {
                    val entry = i.next()
                    if (equal(language, entry.getAttribute(XML_LANG_ATTR))) {
                        i.remove()
                    }
                }
            }
        }
    }

    /**
     * Returns the values that are associated with the specified key.
     *
     * @param element
     * the expanded name of the element
     * @return the elements
     */
    protected open fun getValues(element: EName): Array<CatalogEntry> {
        val values = data[element]
        return if (values != null && values.size > 0) {
            values.toTypedArray<CatalogEntry>()
        } else arrayOf()
    }

    /**
     * Returns the values that are associated with the specified key.
     *
     * @param element
     * the expanded name of the element
     * @return all values of the element or an empty list if this element does not exist or does not have any values
     */
    protected fun getValuesAsList(element: EName): List<CatalogEntry> {
        val values = data[element]
        return values ?: Collections.EMPTY_LIST
    }

    /**
     * Returns the values that are associated with the specified key.
     *
     * @param element
     * the expandend name of the element
     * @param language
     * a language code or null to get values without `xml:lang` attribute
     * @return all values of the element
     */
    protected fun getLocalizedValuesAsList(element: EName, language: String): List<CatalogEntry> {
        val values = data[element]

        if (values != null) {
            val filtered = ArrayList<CatalogEntry>()
            for (value in values) {
                if (equal(language, value.getAttribute(XML_LANG_ATTR))) {
                    filtered.add(value)
                }
            }
            return filtered
        } else {
            return Collections.EMPTY_LIST
        }
    }

    /**
     * Returns the first value that is associated with the specified name.
     *
     * @param element
     * the expanded name of the element
     * @return the first value
     */
    protected fun getFirstValue(element: EName): CatalogEntry? {
        val elements = data[element]
        return if (elements != null && elements.size > 0) {
            elements[0]
        } else null
    }

    /**
     * Returns the first element that is associated with the specified name and attribute.
     *
     * @param element
     * the expanded name of the element
     * @param attributeEName
     * the expanded attribute name
     * @param attributeValue
     * the attribute value
     * @return the first value
     */
    protected fun getFirstValue(element: EName, attributeEName: EName, attributeValue: String): CatalogEntry? {
        val elements = data[element]
        if (elements != null) {
            for (entry in elements) {
                val v = entry.getAttribute(attributeEName)
                if (equal(attributeValue, v))
                    return entry
            }
        }
        return null
    }

    /**
     * Returns the first value that is associated with the specified name and language.
     *
     * @param element
     * the expanded name of the element
     * @param language
     * the language identifier or null to get only elements without `xml:lang` attribute
     * @return the first value
     */
    protected fun getFirstLocalizedValue(element: EName, language: String): CatalogEntry? {
        return getFirstValue(element, XML_LANG_ATTR, language)
    }

    /**
     * Returns the first value that is associated with the specified name and language.
     *
     * @param element
     * the expanded name of the element
     * @param type
     * the `xsi:type` value
     * @return the element
     */
    protected fun getFirstTypedValue(element: EName, type: String): CatalogEntry? {
        return getFirstValue(element, XSI_TYPE_ATTR, type)
    }

    /**
     * Tests two objects for equality.
     */
    protected fun equal(a: Any?, b: Any?): Boolean {
        return a == null && b == null || a != null && a == b
    }

    /**
     * Creates an xml document root and returns it.
     *
     * @return the document
     * @throws ParserConfigurationException
     * If the xml parser environment is not correctly configured
     */
    @Throws(ParserConfigurationException::class)
    protected fun newDocument(): Document {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.isNamespaceAware = true
        val docBuilder = docBuilderFactory.newDocumentBuilder()
        return docBuilder.newDocument()
    }

    /**
     * Serializes the given xml document to the associated file. Please note that this method does *not* close the
     * output stream. Anyone using this method is responsible for doing it by itself.
     *
     * @param document
     * the document
     * @param docType
     * the document type definition (dtd)
     * @throws TransformerException
     * if serialization fails
     */
    @Throws(TransformerException::class, IOException::class)
    protected fun saveToXml(document: Node, docType: String?, out: OutputStream) {
        val streamResult = StreamResult(out)
        val tf = TransformerFactory.newInstance()
        val serializer = tf.newTransformer()
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        if (docType != null)
            serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType)
        serializer.setOutputProperty(OutputKeys.INDENT, "yes")
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        serializer.transform(DOMSource(document), streamResult)
        out.flush()
    }

    /**
     * @see org.opencastproject.mediapackage.AbstractMediaPackageElement.toManifest
     */
    @Throws(MediaPackageException::class)
    override fun toManifest(document: Document, serializer: MediaPackageSerializer?): Node {
        return super.toManifest(document, serializer)
    }

    /**
     * Get a prefix from [.bindings] but throw a [NamespaceBindingException] if none found.
     */
    protected fun getPrefix(namespaceURI: String): String {
        val prefix = bindings.getPrefix(namespaceURI)
        return prefix
                ?: throw NamespaceBindingException(format("Namespace URI %s is not bound to a prefix", namespaceURI))
    }

    /**
     * @see org.opencastproject.mediapackage.XMLCatalog.includeEmpty
     */
    override fun includeEmpty(includeEmpty: Boolean) {
        this.includeEmpty = includeEmpty
    }

    /**
     * Transform an expanded name to a qualified name based on the registered binding.
     *
     * @param eName
     * the expanded name to transform
     * @return the qualified name, e.g. `dcterms:title`
     * @throws NamespaceBindingException
     * if the namespace name is not bound to a prefix
     */
    protected open fun toQName(eName: EName): String {
        return if (eName.hasNamespace()) {
            toQName(getPrefix(eName.namespaceURI), eName.localName)
        } else {
            eName.localName
        }
    }

    /**
     * Transform an qualified name consisting of prefix and local part to an expanded name, based on the registered
     * binding.
     *
     * @param prefix
     * the prefix
     * @param localName
     * the local part
     * @return the expanded name
     * @throws NamespaceBindingException
     * if the namespace name is not bound to a prefix
     */
    protected fun toEName(prefix: String, localName: String): EName {
        return EName(bindings.getNamespaceURI(prefix), localName)
    }

    /**
     * Transform a qualified name to an expanded name, based on the registered binding.
     *
     * @param qName
     * the qualified name, e.g. `dcterms:title` or `title`
     * @return the expanded name
     * @throws NamespaceBindingException
     * if the namespace name is not bound to a prefix
     */
    protected open fun toEName(qName: String): EName {
        val parts = splitQName(qName)
        return EName(bindings.getNamespaceURI(parts[0]), parts[1])
    }

    internal fun mkCatalogEntry(name: EName, value: String, attributes: Map<EName, String>): CatalogEntry {
        return CatalogEntry(name, value, attributes)
    }

    /**
     * Element representation.
     */
    inner class CatalogEntry
    /**
     * Creates a new catalog element representation with name, value and attributes.
     *
     * @param value
     * the element value
     * @param attributes
     * the element attributes
     */
    (
            /**
             * Returns the expanded name of the entry.
             */
            val eName: EName,
            /**
             * Returns the element value.
             *
             * @return the value
             */
            val value: String?, attributes: Map<EName, String>) : XmlElement, Comparable<CatalogEntry>, Serializable {

        /** The attributes of this element  */
        private val attributes: Map<EName, String>

        /**
         * Returns the qualified name of the entry as a string. The namespace of the entry has to be bound to a prefix for
         * this method to succeed.
         */
        val qName: String
            get() = toQName(eName)

        init {
            this.attributes = HashMap(attributes)
        }

        /**
         * Returns `true` if the element contains attributes.
         *
         * @return `true` if the element contains attributes
         */
        fun hasAttributes(): Boolean {
            return attributes.size > 0
        }

        /**
         * Returns the element's attributes.
         *
         * @return the attributes
         */
        fun getAttributes(): Map<EName, String> {
            return Collections.unmodifiableMap(attributes)
        }

        /**
         * Returns `true` if the element contains an attribute with the given name.
         *
         * @return `true` if the element contains the attribute
         */
        fun hasAttribute(name: EName): Boolean {
            return attributes.containsKey(name)
        }

        /**
         * Returns the attribute value for the given attribute.
         *
         * @return the attribute or null
         */
        fun getAttribute(name: EName): String {
            return attributes[name]
        }

        override fun hashCode(): Int {
            return hash(eName, value)
        }

        override fun equals(that: Any?): Boolean {
            return this === that || that is CatalogEntry && eqFields(that as CatalogEntry?)
        }

        private fun eqFields(that: CatalogEntry): Boolean {
            return this.compareTo(that) == 0
        }

        /**
         * Returns the XML representation of this entry.
         *
         * @param document
         * the document
         * @return the xml node
         */
        override fun toXml(document: Document): Node {
            val node = document.createElement(toQName(eName))
            // Write prefix binding to document root element
            bindNamespaceFor(document, eName)

            val keySet = ArrayList(attributes.keys)
            Collections.sort(keySet)
            for (attrEName in keySet) {
                val value = attributes[attrEName]
                if (attrEName.hasNamespace()) {
                    // Write prefix binding to document root element
                    bindNamespaceFor(document, attrEName)
                    if (XSI_TYPE_ATTR == attrEName) {
                        // Special treatment for xsi:type attributes
                        try {
                            val typeName = toEName(value)
                            bindNamespaceFor(document, typeName)
                        } catch (ignore: NamespaceBindingException) {
                            // Type is either not a QName or its namespace is not bound.
                            // We decide to gently ignore those cases.
                        }

                    }
                }
                node.setAttribute(toQName(attrEName), value)
            }
            if (value != null) {
                node.appendChild(document.createTextNode(value))
            }
            return node
        }

        /**
         * Compare two catalog entries. Comparison order:
         * - e_name
         * - number of attributes (less come first)
         * - attribute comparison (e_name -&gt; value)
         */
        override fun compareTo(o: CatalogEntry): Int {
            var c: Int
            c = eName.compareTo(o.eName)
            if (c != 0) {
                return c
            } else { // compare attributes
                c = attributes.size - o.attributes.size
                return if (c != 0) {
                    c
                } else {
                    `$`(attributes.entries).sort(attributeComparator)
                            .zip(`$`(o.attributes.entries).sort(attributeComparator))
                            .map(object : Fn<P2<Entry<EName, String>, Entry<EName, String>>, Int>() {
                                override fun apply(`as`: P2<Entry<EName, String>, Entry<EName, String>>): Int? {
                                    return attributeComparator.compare(`as`.get1(), `as`.get2())
                                }
                            })
                            .find(Booleans.ne(0))
                            .getOr(0)
                }
            }
        }

        /**
         * Writes a namespace binding for catalog entry `name` to the documents root element.
         * `xmlns:prefix="namespace"`
         */
        private fun bindNamespaceFor(document: Document, name: EName) {
            val root = document.firstChild as Element
            val namespace = name.namespaceURI
            // Do not bind the "xml" namespace. It is bound by default
            if (XML_NS_URI != namespace) {
                root.setAttribute(XMLNS_ATTRIBUTE + ":" + this@XMLCatalogImpl.getPrefix(name.namespaceURI),
                        name.namespaceURI)
            }
        }

        override fun toString(): String? {
            return value
        }

        companion object {

            /** The serial version UID  */
            private const val serialVersionUID = 7195298081966562710L
        }
    }

    // --------------------------------------------------------------------------------------------

    // --

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.XMLCatalog.toXml
     */
    @Throws(IOException::class)
    override fun toXml(out: OutputStream, format: Boolean) {
        try {
            val doc = this.toXml()
            val domSource = DOMSource(doc)
            val result = StreamResult(out)
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.transform(domSource, result)
        } catch (e: ParserConfigurationException) {
            throw IOException("unable to parse document")
        } catch (e: TransformerException) {
            throw IOException("unable to transform dom to a stream")
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.XMLCatalog.toXmlString
     */
    @Throws(IOException::class)
    override fun toXmlString(): String {
        val out = ByteArrayOutputStream()
        toXml(out, true)
        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

    companion object {
        private val serialVersionUID = -7580292199527168951L

        /** Expanded name of the XML language attribute `xml:lang`.  */
        val XML_LANG_ATTR = EName(XML_NS_URI, "lang")

        /** Namespace prefix for XML schema instance.  */
        val XSI_NS_PREFIX = "xsi"

        /**
         * Expanded name of the XSI type attribute.
         *
         *
         * See [http://www.w3.org/TR/xmlschema-1/#xsi_type](http://www.w3.org/TR/xmlschema-1/#xsi_type) for the
         * definition.
         */
        val XSI_TYPE_ATTR = EName(W3C_XML_SCHEMA_INSTANCE_NS_URI, "type")

        /**
         * Splits a QName into its parts.
         *
         * @param qName
         * the qname to split
         * @return an array of prefix (0) and local part (1). The prefix is "" if the qname belongs to the default namespace.
         */
        private fun splitQName(qName: String): Array<String> {
            val parts = qName.split(":".toRegex(), 3).toTypedArray()
            when (parts.size) {
                1 -> return arrayOf(DEFAULT_NS_PREFIX, parts[0])
                2 -> return parts
                else -> throw IllegalArgumentException("Local name must not contain ':'")
            }
        }

        /**
         * Returns a "prefixed name" consisting of namespace prefix and local name.
         *
         * @param prefix
         * the namespace prefix, may be `null`
         * @param localName
         * the local name
         * @return the "prefixed name" `prefix:localName`
         */
        private fun toQName(prefix: String?, localName: String): String {
            val b = StringBuilder()
            if (prefix != null && DEFAULT_NS_PREFIX != prefix) {
                b.append(prefix)
                b.append(":")
            }
            b.append(localName)
            return b.toString()
        }

        // --------------------------------------------------------------------------------------------

        private val NO_ATTRIBUTES = HashMap<EName, String>()

        internal fun doCompareTo(k1: EName, v1: String, k2: EName, v2: String): Int {
            val c = k1.compareTo(k2)
            return if (c != 0) c else v1.compareTo(v2)
        }

        private val attributeComparator = Comparator<Entry<EName, String>> { o1, o2 -> doCompareTo(o1.key, o1.value, o2.key, o2.value) }

        private val catalogEntryComparator = Comparator<CatalogEntry> { o1, o2 -> o1.compareTo(o2) }
    }

}
