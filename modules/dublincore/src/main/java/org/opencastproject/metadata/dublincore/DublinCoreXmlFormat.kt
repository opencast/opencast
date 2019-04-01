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

import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.XMLCatalogImpl
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry
import org.opencastproject.util.XmlNamespaceContext

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.annotation.ParametersAreNonnullByDefault
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Result
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXResult

/**
 * XML serialization of Dublin Core catalogs.
 */
@ParametersAreNonnullByDefault
class DublinCoreXmlFormat : DefaultHandler {

    /** The element content  */
    private var content = StringBuilder()

    /** The node attributes  */
    private var attributes: Attributes? = null

    private val dc = DublinCores.mkSimple()

    /**
     * Returns the element content.
     */
    private val andResetContent: String
        get() {
            val str = content.toString().trim { it <= ' ' }
            content = StringBuilder()
            return str
        }

    private constructor() {}

    // Option to instante catalog with intentionally emptied values
    // used to target removal of DublinCore catalog values during update.
    private constructor(includeEmpty: Boolean) {
        dc.includeEmpty(includeEmpty)
    }

    // SAX

    @Throws(TransformerException::class)
    private fun readImpl(node: Node): DublinCoreCatalog {
        val outputTarget = SAXResult(this)
        val t = TransformerFactory.newInstance().newTransformer()
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        t.transform(DOMSource(node), outputTarget)
        return dc
    }

    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    private fun readImpl(`in`: InputSource): DublinCoreCatalog {
        val factory = SAXParserFactory.newInstance()
        // no DTD
        factory.isValidating = false
        // namespaces!
        factory.isNamespaceAware = true
        // read document
        factory.newSAXParser().parse(`in`, this)
        return dc
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        super.characters(ch, start, length)
        content.append(ch, start, length)
    }

    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String, uri: String) {
        dc.addBindings(XmlNamespaceContext.mk(prefix, uri))
    }

    /**
     * Read `type` attribute from track or catalog element.
     *
     * @see org.xml.sax.helpers.DefaultHandler.startElement
     */
    @Throws(SAXException::class)
    override fun startElement(uri: String, localName: String, name: String, attributes: Attributes) {
        if (dc.rootTag == null) {
            dc.rootTag = EName(uri, localName)
        }
        this.attributes = attributes
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, name: String) {
        if (dc.rootTag != null) {
            dc.addElement(EName.mk(uri, localName), andResetContent, attributes!!)
        }
    }

    @Throws(SAXException::class)
    override fun error(e: SAXParseException) {
        logger.warn("Error parsing DublinCore catalog: " + e.message)
        super.error(e)
    }

    @Throws(SAXException::class)
    override fun fatalError(e: SAXParseException) {
        logger.warn("Fatal error parsing DublinCore catalog: " + e.message)
        super.fatalError(e)
    }

    @Throws(SAXException::class)
    override fun warning(e: SAXParseException) {
        logger.warn("Warning parsing DublinCore catalog: " + e.message)
        super.warning(e)
    }

    companion object {
        /** the logging facility provided by log4j  */
        private val logger = LoggerFactory.getLogger(DublinCoreXmlFormat::class.java)

        /**
         * Read an XML encoded catalog from a stream.
         *
         * @param xml
         * the input stream containing the DublinCore catalog
         * @return the catalog representation
         * @throws javax.xml.parsers.ParserConfigurationException
         * if setting up the parser failed
         * @throws org.xml.sax.SAXException
         * if an error occurred while parsing the document
         * @throws java.io.IOException
         * if the stream cannot be accessed in a proper way
         */
        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: InputStream): DublinCoreCatalog {
            return DublinCoreXmlFormat().readImpl(InputSource(xml))
        }

        /**
         * Read an XML encoded catalog from a file.
         *
         * @param xml
         * the file containing the DublinCore catalog
         * @return the catalog representation
         * @throws javax.xml.parsers.ParserConfigurationException
         * if setting up the parser failed
         * @throws org.xml.sax.SAXException
         * if an error occurred while parsing the document
         * @throws java.io.IOException
         * if the stream cannot be accessed in a proper way
         */
        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: File): DublinCoreCatalog {
            FileInputStream(xml).use { `in` -> return DublinCoreXmlFormat().readImpl(InputSource(`in`)) }
        }

        /**
         * Read an XML encoded catalog from a string.
         *
         * @param xml
         * the string containing the DublinCore catalog
         * @return the catalog representation
         * @throws javax.xml.parsers.ParserConfigurationException
         * if setting up the parser failed
         * @throws org.xml.sax.SAXException
         * if an error occurred while parsing the document
         * @throws java.io.IOException
         * if the stream cannot be accessed in a proper way
         */
        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: String): DublinCoreCatalog {
            return DublinCoreXmlFormat().readImpl(
                    InputSource(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))))
        }

        fun readOpt(xml: String): Opt<DublinCoreCatalog> {
            try {
                return Opt.some(read(xml))
            } catch (e: Exception) {
                return Opt.none()
            }

        }

        /** [.read] as a function, returning none on error.  */
        val readOptFromString: Fn<String, Opt<DublinCoreCatalog>> = object : Fn<String, Opt<DublinCoreCatalog>>() {
            override fun apply(xml: String): Opt<DublinCoreCatalog> {
                return readOpt(xml)
            }
        }

        @Throws(TransformerException::class)
        fun read(xml: Node): DublinCoreCatalog {
            return DublinCoreXmlFormat().readImpl(xml)
        }

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: InputSource): DublinCoreCatalog {
            return DublinCoreXmlFormat().readImpl(xml)
        }

        // Optional read to optionally instantiate catalog with intentionally empty elements
        // used to remove existing DublinCore catalog values during a merge update.
        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: String, includeEmptiedElements: Boolean): DublinCoreCatalog {
            return DublinCoreXmlFormat(includeEmptiedElements).readImpl(
                    InputSource(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))))
        }

        @Throws(TransformerException::class)
        fun read(xml: Node, includeEmptiedElements: Boolean): DublinCoreCatalog {
            return DublinCoreXmlFormat(includeEmptiedElements).readImpl(xml)
        }

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: InputSource, includeEmptiedElements: Boolean): DublinCoreCatalog {
            return DublinCoreXmlFormat(includeEmptiedElements).readImpl(xml)
        }

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        fun read(xml: File, includeEmptiedElements: Boolean): DublinCoreCatalog {
            FileInputStream(xml).use { `in` -> return DublinCoreXmlFormat(includeEmptiedElements).readImpl(InputSource(`in`)) }
        }

        /**
         * Merge values that are  new, changed, or emptied from the "from" catalog into the existing "into" catalog.
         *
         * @param fromCatalog contains targeted new values (new elements, changed element values, emptied element values).
         * @param intoCatalog is the existing catalog that merges in the targed changes.
         * @return the merged catalog
         */
        fun merge(fromCatalog: DublinCoreCatalog, intoCatalog: DublinCoreCatalog): DublinCoreCatalog {

            // If one catalog is null, return the other.
            if (fromCatalog == null) {
                return intoCatalog
            }
            if (intoCatalog == null) {
                return fromCatalog
            }

            val mergedCatalog = intoCatalog.clone() as DublinCoreCatalog
            val mergeEntries = fromCatalog.entriesSorted

            for (mergeEntry in mergeEntries) {
                // ignore root entry
                if (mergeEntry.eName == intoCatalog.rootTag)
                    continue

                // if language is provided, only overwrite existing of same language
                val lang = mergeEntry.getAttribute(XMLCatalogImpl.XML_LANG_ATTR)
                if (StringUtils.isNotEmpty(lang)) {
                    // Passing a null value will remove the exiting value
                    mergedCatalog.set(mergeEntry.eName, StringUtils.trimToNull(mergeEntry.value), lang)
                } else {
                    mergedCatalog.set(mergeEntry.eName, StringUtils.trimToNull(mergeEntry.value))
                }
            }
            return mergedCatalog
        }

        @Throws(ParserConfigurationException::class, TransformerException::class, IOException::class)
        fun writeDocument(dc: DublinCoreCatalog): Document {
            // Create the DOM document
            val doc: Document
            run {
                val docBuilderFactory = DocumentBuilderFactory.newInstance()
                docBuilderFactory.isNamespaceAware = true
                doc = docBuilderFactory.newDocumentBuilder().newDocument()
            }
            if (dc.rootTag != null) {
                val rootElement = doc.createElementNS(dc.rootTag!!.namespaceURI, dc.toQName(dc.rootTag!!))
                doc.appendChild(rootElement)
                for (element in dc.entriesSorted) {
                    rootElement.appendChild(element.toXml(doc))
                }
                return doc
            } else {
                throw RuntimeException("DublinCore catalog does not have a root tag.")
            }
        }

        fun writeString(dc: DublinCoreCatalog): String {
            try {
                return dc.toXmlString()
            } catch (e: IOException) {
                throw IllegalStateException(String.format("Error serializing the episode dublincore catalog %s.", dc), e)
            }

        }
    }
}
