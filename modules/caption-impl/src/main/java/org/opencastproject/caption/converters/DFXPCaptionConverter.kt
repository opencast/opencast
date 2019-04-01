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

package org.opencastproject.caption.converters

import org.opencastproject.caption.api.Caption
import org.opencastproject.caption.api.CaptionConverter
import org.opencastproject.caption.api.CaptionConverterException
import org.opencastproject.caption.api.IllegalTimeFormatException
import org.opencastproject.caption.api.Time
import org.opencastproject.caption.impl.CaptionImpl
import org.opencastproject.caption.impl.TimeImpl
import org.opencastproject.caption.util.TimeUtil
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.ArrayList
import java.util.LinkedList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * This is converter for DFXP, XML based caption format. DOM parser is used for both caption importing and exporting,
 * while SAX parser is used for determining which languages are present (DFXP can contain multiple languages).
 */
class DFXPCaptionConverter : CaptionConverter {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.CaptionConverter.getExtension
     */
    override val extension: String
        get() = EXTENSION

    override val elementType: Type
        get() = MediaPackageElement.Type.Attachment

    /**
     * {@inheritDoc} Parser used for parsing XML document is DOM parser. Language parameter will determine which language
     * is searched for and parsed. If there is no matching language, empty collection is returned. If language parameter
     * is `null` first language found is parsed.
     *
     * @see org.opencastproject.caption.api.CaptionConverter.importCaption
     */
    @Throws(CaptionConverterException::class)
    override fun importCaption(`in`: InputStream, language: String): List<Caption> {

        // create new collection
        val collection = ArrayList<Caption>()

        val doc: Document
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            doc = builder.parse(`in`)
            doc.documentElement.normalize()
        } catch (e: ParserConfigurationException) {
            throw CaptionConverterException("Could not parse captions", e)
        } catch (e: SAXException) {
            throw CaptionConverterException("Could not parse captions", e)
        } catch (e: IOException) {
            throw CaptionConverterException("Could not parse captions", e)
        }

        // get all <div> elements since they contain information about language
        val divElements = doc.getElementsByTagName("div")

        var targetDiv: Element? = null
        if (language != null) {
            // find first <div> element with matching language
            for (i in 0 until divElements.length) {
                val n = divElements.item(i) as Element
                if (n.getAttribute("xml:lang") == language) {
                    targetDiv = n
                    break
                }
            }
        } else {
            if (divElements.length > 1) {
                // more than one existing <div> element, no language specified
                logger.warn("More than one <div> element available. Parsing first one...")
            }
            if (divElements.length != 0) {
                targetDiv = divElements.item(0) as Element
            }
        }

        // check if we found node
        if (targetDiv == null) {
            logger.warn("No suitable <div> element found for language {}", language)
        } else {
            val pElements = targetDiv.getElementsByTagName("p")

            // initialize start time
            var time: Time? = null
            try {
                time = TimeImpl(0, 0, 0, 0)
            } catch (e1: IllegalTimeFormatException) {
            }

            for (i in 0 until pElements.length) {
                try {
                    val caption = parsePElement(pElements.item(i) as Element)
                    // check time
                    if (caption.startTime.compareTo(time!!) < 0 || caption.stopTime.compareTo(caption.startTime) <= 0) {
                        logger.warn("Caption with invalid time encountered. Skipping...")
                        continue
                    }
                    collection.add(caption)
                } catch (e: IllegalTimeFormatException) {
                    logger.warn("Caption with invalid time format encountered. Skipping...")
                }

            }
        }

        // return collection
        return collection
    }

    /**
     * Parse &lt;p&gt; element which contains one caption.
     *
     * @param p
     * &lt;p&gt; element to be parsed
     * @return new [Caption] object
     * @throws IllegalTimeFormatException
     * if time format does not match with expected format for DFXP
     */
    @Throws(IllegalTimeFormatException::class)
    private fun parsePElement(p: Element): Caption {
        val begin = TimeUtil.importDFXP(p.getAttribute("begin").trim { it <= ' ' })
        val end = TimeUtil.importDFXP(p.getAttribute("end").trim { it <= ' ' })
        // FIXME add logic for duration if end is absent

        // get text inside p
        val textArray = getTextCore(p).split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return CaptionImpl(begin, end, textArray)
    }

    /**
     * Returns caption text stripped of all tags.
     *
     * @param p
     * &lt;p&gt; element to be parsed
     * @return Caption text with \n as new line character
     */
    private fun getTextCore(p: Node): String {
        val captionText = StringBuffer()
        // get children
        val list = p.childNodes
        for (i in 0 until list.length) {
            if (list.item(i).nodeType == Node.TEXT_NODE) {
                captionText.append(list.item(i).textContent)
            } else if ("br" == list.item(i).nodeName) {
                captionText.append("\n")
            } else {
                captionText.append(getTextCore(list.item(i)))
            }
        }
        return captionText.toString().trim { it <= ' ' }
    }

    /**
     * {@inheritDoc} DOM parser is used to parse template from which whole document is then constructed.
     */
    @Throws(IOException::class)
    fun exportCaption(outputStream: OutputStream, captions: List<Caption>, language: String?) {
        // get document builder factory and parse template
        val factory = DocumentBuilderFactory.newInstance()
        var doc: Document? = null
        var `is`: InputStream? = null
        try {
            val builder = factory.newDocumentBuilder()
            // load dfxp template from file
            `is` = DFXPCaptionConverter::class.java.getResourceAsStream("/templates/template.dfxp.xml")
            doc = builder.parse(`is`!!)
        } catch (e: ParserConfigurationException) {
            // should not happen
            throw RuntimeException(e)
        } catch (e: SAXException) {
            // should not happen unless template is invalid
            throw RuntimeException(e)
        } catch (e: IOException) {
            // should not happen
            throw RuntimeException(e)
        } finally {
            IOUtils.closeQuietly(`is`)
        }

        // retrieve body element
        val bodyNode = doc!!.getElementsByTagName("body").item(0)

        // create new div element with specified language
        val divNode = doc.createElement("div")
        divNode.setAttribute("xml:lang", language ?: "und")
        bodyNode.appendChild(divNode)

        // update document
        for (caption in captions) {
            val newNode = doc.createElement("p")
            newNode.setAttribute("begin", TimeUtil.exportToDFXP(caption.startTime))
            newNode.setAttribute("end", TimeUtil.exportToDFXP(caption.stopTime))
            val captionText = caption.caption
            // text part
            newNode.appendChild(doc.createTextNode(captionText[0]))
            for (i in 1 until captionText.size) {
                newNode.appendChild(doc.createElement("br"))
                newNode.appendChild(doc.createTextNode(captionText[i]))
            }
            divNode.appendChild(newNode)
        }

        // initialize stream writer
        val osw = OutputStreamWriter(outputStream, "UTF-8")
        val result = StreamResult(osw)
        val source = DOMSource(doc)
        val tfactory = TransformerFactory.newInstance()
        val transformer: Transformer
        try {
            transformer = tfactory.newTransformer()
            transformer.transform(source, result)
            osw.flush()
        } catch (e: TransformerConfigurationException) {
            // should not happen
            throw RuntimeException(e)
        } catch (e: TransformerException) {
            // should not happen
            throw RuntimeException(e)
        } finally {
            IOUtils.closeQuietly(osw)
        }
    }

    /**
     * {@inheritDoc} Uses SAX parser to quickly read the document and retrieve available languages.
     *
     * @see org.opencastproject.caption.api.CaptionConverter.getLanguageList
     */
    @Throws(CaptionConverterException::class)
    override fun getLanguageList(input: InputStream): Array<String> {

        // create lang list
        val langList = LinkedList<String>()

        // get SAX parser
        val factory = SAXParserFactory.newInstance()
        try {
            val parser = factory.newSAXParser()
            // create handler
            val handler = object : DefaultHandler() {
                @Throws(SAXException::class)
                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                    if ("div" == qName) {
                        // we found div tag - let's make a lookup for language
                        val lang = attributes!!.getValue("xml:lang")
                        if (lang == null) {
                            // should never happen
                            logger.warn("Missing xml:lang attribute for div element.")
                        } else if (langList.contains(lang)) {
                            logger.warn("Multiple div elements with same language.")
                        } else {
                            langList.add(lang)
                        }
                    }
                }
            }

            // parse stream
            parser.parse(input, handler)
        } catch (e: ParserConfigurationException) {
            // should not happen
            throw RuntimeException(e)
        } catch (e: SAXException) {
            throw CaptionConverterException("Could not parse captions", e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return langList.toTypedArray()
    }

    companion object {

        /** logging utility  */
        private val logger = LoggerFactory.getLogger(DFXPCaptionConverter::class.java)

        private val EXTENSION = "dfxp.xml"
    }

}
