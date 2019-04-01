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

import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.data.Either

import com.entwinemedia.fn.data.ImmutableIteratorBase

import org.apache.commons.io.IOUtils
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.NoSuchElementException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** XML utility functions.  */
object XmlUtil {
    private val nsDbf: DocumentBuilderFactory
    private val dbf: DocumentBuilderFactory

    init {
        nsDbf = DocumentBuilderFactory.newInstance()
        nsDbf.isNamespaceAware = true
        //
        dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = false
    }

    /** Namespace aware parsing of `src`.  */
    fun parseNs(src: InputSource): Either<Exception, Document> {
        try {
            val docBuilder = nsDbf.newDocumentBuilder()
            return right(docBuilder.parse(src))
        } catch (e: Exception) {
            return left(e)
        }

    }

    /** Namespace aware parsing of `xml`.  */
    fun parseNs(xml: String): Either<Exception, Document> {
        return parseNs(fromXmlString(xml))
    }

    /** Parsing of `src` without namespaces.  */
    fun parse(src: InputSource): Either<Exception, Document> {
        try {
            val docBuilder = dbf.newDocumentBuilder()
            return right(docBuilder.parse(src))
        } catch (e: Exception) {
            return left(e)
        }

    }

    /**
     * Writes an xml representation to a stream.
     *
     * @param doc
     * the document
     * @param out
     * the output stream
     * @throws IOException
     * if there is an error transforming the dom to a stream
     */
    @Throws(IOException::class)
    fun toXml(doc: Document, out: OutputStream) {
        try {
            val domSource = DOMSource(doc)
            val result = StreamResult(out)
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.VERSION, doc.xmlVersion)
            transformer.transform(domSource, result)
        } catch (e: TransformerException) {
            throw IOException("unable to transform dom to a stream")
        }

    }

    /**
     * Writes an xml representation to an input stream and return it.
     *
     * @param document
     * the document
     * @return the input stream containing the serialized xml representation
     * @throws IOException
     * if there is an error transforming the dom to a stream
     */
    @Throws(IOException::class)
    fun serializeDocument(document: Document): InputStream {
        var out: ByteArrayOutputStream? = null
        try {
            out = ByteArrayOutputStream()
            XmlUtil.toXml(document, out)
            return ByteArrayInputStream(out.toByteArray())
        } finally {
            IoSupport.closeQuietly(out)
        }
    }

    /**
     * Serializes the document to a XML string
     *
     * @param document
     * the document
     * @return the serialized XML string
     * @throws IOException
     * if there is an error transforming the dom to a stream
     */
    @Throws(IOException::class)
    fun toXmlString(document: Document): String {
        var inputStream: InputStream? = null
        try {
            inputStream = serializeDocument(document)
            return IOUtils.toString(inputStream, "UTF-8")
        } finally {
            IoSupport.closeQuietly(inputStream)
        }
    }

    /** Create an [org.xml.sax.InputSource] from an XML string.  */
    fun fromXmlString(xml: String): InputSource {
        return InputSource(IOUtils.toInputStream(xml))
    }

    /**
     * Creates an xml document root and returns it.
     *
     * @return the document
     */
    fun newDocument(): Document {
        try {
            return nsDbf.newDocumentBuilder().newDocument()
        } catch (e: ParserConfigurationException) {
            return chuck(e)
        }

    }

    /** Make a [org.w3c.dom.NodeList] iterable.  */
    fun <A : Node> iterable(nl: NodeList): Iterable<A> {
        return object : Iterable<A> {
            override fun iterator(): Iterator<A> {
                return object : ImmutableIteratorBase<A>() {
                    private var index = 0

                    override fun hasNext(): Boolean {
                        return index < nl.length
                    }

                    override fun next(): A {
                        if (hasNext()) {
                            val next = nl.item(index)
                            index = index + 1
                            return next as A
                        } else {
                            throw NoSuchElementException()
                        }
                    }
                }
            }
        }
    }
}
