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

import org.opencastproject.util.IoSupport

import org.apache.commons.io.IOUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.util.ArrayList
import java.util.LinkedList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Simple class that enables storage of [DublinCoreCatalog] list and serializing into xml or json string.
 *
 */
class DublinCoreCatalogList
/**
 * Initialize with the given catalog list.
 *
 * @param catalogs
 * the catalogs to initialize this list with.
 * @param totalCount
 * the total count of catalogs that match the creating query of this list must be &gt;= catalogs.size
 */
(catalogs: List<DublinCoreCatalog>, totalCount: Long) {
    /** Array storing Dublin cores  */
    private val catalogList = LinkedList<DublinCoreCatalog>()
    /**
     * Get the total number of catalogs matching the creating query. Is &gt;= [.size].
     *
     * @return int totalCatalogCount
     */

    val totalCount: Long = 0

    /**
     * Serializes list to XML.
     *
     * @return serialized array as XML string
     * @throws IOException
     * if serialization cannot be properly performed
     */
    val resultsAsXML: String
        @Throws(IOException::class)
        get() {
            try {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val impl = builder.domImplementation

                val doc = impl.createDocument(null, null, null)
                val root = doc.createElement("dublincorelist")
                root.setAttribute("totalCount", totalCount.toString())
                doc.appendChild(root)
                for (series in catalogList) {
                    val node = doc.importNode(series.toXml().documentElement, true)
                    root.appendChild(node)
                }

                val tf = TransformerFactory.newInstance().newTransformer()
                val xmlSource = DOMSource(doc)
                val out = StringWriter()
                tf.transform(xmlSource, StreamResult(out))
                return out.toString()
            } catch (e: Exception) {
                throw IOException(e)
            }

        }

    /**
     * Serializes list to JSON array string.
     *
     * @return serialized array as json array string
     */
    val resultsAsJson: String
        get() {
            val jsonObj = JSONObject()
            val jsonArray = JSONArray()
            for (catalog in catalogList) {
                jsonArray.add(DublinCoreJsonFormat.writeJsonObject(catalog))
            }
            jsonObj["totalCount"] = totalCount.toString()
            jsonObj["catalogs"] = jsonArray
            return jsonObj.toJSONString()
        }

    init {
        if (totalCount < catalogs.size)
            throw IllegalArgumentException("total count is less than the number of catalogs passed")
        catalogList.addAll(catalogs)
        this.totalCount = totalCount
    }

    /**
     * Returns list of Dublin Core currently stored
     *
     * @return List of [DublinCoreCatalog]s
     */
    fun getCatalogList(): List<DublinCoreCatalog> {
        return LinkedList(catalogList)
    }

    /**
     * Return the number of contained catalogs.
     */
    fun size(): Long {
        return catalogList.size.toLong()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DublinCoreCatalogList::class.java)

        /**
         * Parses an XML or JSON string to an dublin core catalog list.
         *
         * @param dcString
         * the XML or JSON string
         * @throws IOException
         * if there is a problem parsing the XML or JSON
         */
        @Throws(IOException::class)
        fun parse(dcString: String): DublinCoreCatalogList {
            val catalogs = ArrayList<DublinCoreCatalog>()
            if (dcString.startsWith("{")) {
                val json: JSONObject
                try {
                    json = JSONParser().parse(dcString) as JSONObject
                    val totalCount = java.lang.Long.parseLong(json["totalCount"] as String)
                    val catalogsArray = json["catalogs"] as JSONArray
                    for (catalog in catalogsArray) {
                        catalogs.add(DublinCoreJsonFormat.read(catalog as JSONObject))
                    }
                    return DublinCoreCatalogList(catalogs, totalCount)
                } catch (e: Exception) {
                    throw IllegalStateException("Unable to load dublin core catalog list, json parsing failed.", e)
                }

            } else {
                // XML
                var `is`: InputStream? = null
                try {
                    val docBuilderFactory = DocumentBuilderFactory.newInstance()
                    docBuilderFactory.isNamespaceAware = true
                    val docBuilder = docBuilderFactory.newDocumentBuilder()
                    `is` = IOUtils.toInputStream(dcString, "UTF-8")
                    val document = docBuilder.parse(`is`!!)
                    val xPath = XPathFactory.newInstance().newXPath()

                    val totalCount = xPath.evaluate("/*[local-name() = 'dublincorelist']/@totalCount", document,
                            XPathConstants.NUMBER) as Number

                    val nodes = xPath
                            .evaluate("//*[local-name() = 'dublincore']", document, XPathConstants.NODESET) as NodeList
                    for (i in 0 until nodes.length) {
                        var nodeIs: InputStream? = null
                        try {
                            nodeIs = nodeToString(nodes.item(i))
                            catalogs.add(DublinCoreXmlFormat.read(nodeIs!!))
                        } finally {
                            IoSupport.closeQuietly(nodeIs)
                        }
                    }
                    return DublinCoreCatalogList(catalogs, totalCount.toLong())
                } catch (e: Exception) {
                    throw IOException(e)
                } finally {
                    IoSupport.closeQuietly(`is`)
                }
            }
        }

        /**
         * Serialize a node to an input stream
         *
         * @param node
         * the node to serialize
         * @return the serialized input stream
         */
        private fun nodeToString(node: Node): InputStream? {
            val outputStream = ByteArrayOutputStream()
            try {
                val t = TransformerFactory.newInstance().newTransformer()
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                t.setOutputProperty(OutputKeys.INDENT, "yes")
                t.transform(DOMSource(node), StreamResult(outputStream))
                return ByteArrayInputStream(outputStream.toByteArray())
            } catch (te: TransformerException) {
                logger.warn("nodeToString Transformer Exception", te)
            }

            return null
        }
    }
}
