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

import org.opencastproject.util.MimeType.mimeType
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option

import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.functions.Options
import org.opencastproject.util.data.functions.Strings

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 * This class represents the mime type registry that is responsible for providing resolving mime types through all
 * system components.
 *
 *
 * The registry is initialized from the file `org.opencastproject.util.MimeTypes.xml`.
 */
object MimeTypes {

    val MIME_TYPE_PATTERN = Pattern.compile("([a-zA-Z0-9-]+)/([a-zA-Z0-9-+.]+)")

    /** Name of the mime type files  */
    val DEFINITION_FILE = "/org/opencastproject/util/MimeTypes.xml"

    /** Name of the mime type files  */
    val DEFAULT_TYPE = "application/octet-stream"

    /** The mime types  */
    private val mimeTypes = ArrayList<MimeType>()

    /** the logging facility provided by log4j  */
    private val logger = LoggerFactory.getLogger(MimeType::class.java!!)

    /** Common mime types  */
    val XML: MimeType
    val TEXT: MimeType
    val JSON: MimeType
    val JPG: MimeType
    val MJPEG: MimeType
    val MPEG4: MimeType
    val MATROSKA: MimeType
    val MPEG4_AAC: MimeType
    val DV: MimeType
    val MJPEG2000: MimeType
    val MP3: MimeType
    val AAC: MimeType
    val CALENDAR: MimeType
    val ZIP: MimeType
    val JAR: MimeType
    val SMIL: MimeType
    val PNG: MimeType

    val toMimeType: Fn<String, Opt<MimeType>> = object : Fn<String, Opt<MimeType>>() {
        override fun apply(name: String): Opt<MimeType> {
            try {
                return Opt.some(fromString(name))
            } catch (e: Exception) {
                return Opt.none()
            }

        }
    }

    // Initialize common mime types
    init {
        XML = MimeTypes.parseMimeType("text/xml")
        TEXT = MimeTypes.parseMimeType("text/plain")
        JSON = MimeTypes.parseMimeType("application/json")
        JPG = MimeTypes.parseMimeType("image/jpg")
        MJPEG = MimeTypes.parseMimeType("video/x-motion-jpeg")
        MPEG4 = MimeTypes.parseMimeType("video/mp4")
        MATROSKA = MimeTypes.parseMimeType("video/x-matroska")
        MPEG4_AAC = MimeTypes.parseMimeType("video/x-m4v")
        DV = MimeTypes.parseMimeType("video/x-dv")
        MJPEG2000 = MimeTypes.parseMimeType("video/mj2")
        MP3 = MimeTypes.parseMimeType("audio/mpeg")
        AAC = MimeTypes.parseMimeType("audio/x-m4a")
        CALENDAR = MimeTypes.parseMimeType("text/calendar")
        ZIP = MimeTypes.parseMimeType("application/zip")
        JAR = MimeTypes.parseMimeType("application/java-archive")
        SMIL = MimeTypes.parseMimeType("application/smil")
        PNG = MimeTypes.parseMimeType("image/png")

        // initialize from file
        try {
            val parserFactory = SAXParserFactory.newInstance()
            val parser = parserFactory.newSAXParser()
            val handler = MimeTypeParser(mimeTypes)

            MimeTypes::class.java!!.getResourceAsStream(DEFINITION_FILE).use({ inputStream -> parser.parse(inputStream, handler) })
        } catch (e: IOException) {
            logger.error("Error initializing mime type registry", e)
        } catch (e: ParserConfigurationException) {
            logger.error("Error parsing mime type registry", e)
        } catch (e: SAXException) {
            logger.error("Error parsing mime type registry", e)
        }

    }

    /**
     * Returns a mime type for the given type and subtype, e. g. `video/mj2`.
     *
     * @param mimeType
     * the mime type
     * @return the corresponding mime type
     */
    fun parseMimeType(mimeType: String): MimeType {
        val m = MIME_TYPE_PATTERN.matcher(mimeType)
        if (!m.matches())
            throw IllegalArgumentException("Malformed mime type '$mimeType'")
        val type = m.group(1)
        val subtype = m.group(2)
        for (t in mimeTypes) {
            if (t.type == type && t.subtype == subtype)
                return t
        }
        return mimeType(type, subtype)
    }

    /**
     * Returns a mime type for the provided file suffix.
     *
     *
     * For example, if the suffix is `mj2`, the mime type will be that of a ISO Motion JPEG 2000 document.
     *
     *
     * If no mime type is found for the suffix, a `UnknownFileTypeException` is thrown.
     *
     * @param suffix
     * the file suffix
     * @return the corresponding mime type
     * @throws UnknownFileTypeException
     * if the suffix does not map to a mime type
     */
    @Throws(UnknownFileTypeException::class)
    fun fromSuffix(suffix: String?): MimeType {
        if (suffix == null)
            throw IllegalArgumentException("Argument 'suffix' was null!")

        for (m in mimeTypes) {
            if (m.supportsSuffix(suffix))
                return m
        }
        throw UnknownFileTypeException("File suffix '$suffix' cannot be matched to any mime type")
    }

    /**
     * Returns a mime type for the provided file.
     *
     *
     * This method tries various ways to extract mime type information from the files name or its contents.
     *
     *
     * If no mime type can be derived from either the file name or its contents, a `UnknownFileTypeException`
     * is thrown.
     *
     * @param uri
     * the file
     * @return the corresponding mime type
     * @throws UnknownFileTypeException
     * if the mime type cannot be derived from the file
     */
    @Throws(UnknownFileTypeException::class)
    fun fromURI(uri: URI?): MimeType {
        if (uri == null)
            throw IllegalArgumentException("Argument 'uri' is null")
        return fromString(uri.path)
    }

    /**
     * Returns a mime type for the provided file name.
     *
     *
     * This method tries to find the mime type from the file name suffix (extension).
     *
     *
     * If no mime type can be derived from the file name, an `UnknownFileTypeException`
     * is thrown.
     *
     * @param name
     * the file
     * @return the corresponding mime type
     * @throws UnknownFileTypeException
     * if the mime type cannot be derived from the file
     */
    @Throws(UnknownFileTypeException::class)
    fun fromString(name: String?): MimeType {
        if (name == null)
            throw IllegalArgumentException("Argument 'name' is null")

        return fromSuffix(FilenameUtils.getExtension(name))
    }

    /**
     * Convenience method to get a mime type as String from a filename extension
     *
     * @param name
     * the filename
     * @return the corresponding mime type or DEFAULT_TYPE if no match
     */
    fun getMimeType(name: String): String {
        try {
            return MimeTypes.fromString(name).toString()
        } catch (e: UnknownFileTypeException) {
            return DEFAULT_TYPE
        }

    }

    /**
     * Reads the mime type definitions from the xml file comming with this distribution.
     */
    private class MimeTypeParser
    /**
     * Creates a new mime type reader.
     *
     * @param registry
     * the registry
     */
    internal constructor(registry: MutableList<MimeType>) : DefaultHandler() {

        /** The mime types  */
        private val registry: MutableList<MimeType>? = null

        /** Element content  */
        private var content = StringBuffer()

        /** Type  */
        private var type: String? = null

        /** Description  */
        private var description: String? = null

        /** Extensions, comma separated  */
        private var extensions: String? = null

        init {
            this.registry = registry
        }

        @Throws(SAXException::class)
        override fun characters(ch: CharArray?, start: Int, length: Int) {
            super.characters(ch, start, length)
            content.append(ch, start, length)
        }

        /**
         * Returns the element content.
         *
         * @return the element content
         */
        private fun getContent(): String {
            val str = content.toString()
            content = StringBuffer()
            return str
        }

        @Throws(SAXException::class)
        override fun endElement(uri: String?, localName: String?, name: String?) {
            super.endElement(uri, localName, name)

            if ("Type" == name) {
                type = getContent()
            } else if ("Description" == name) {
                description = getContent()
            } else if ("Extensions" == name) {
                extensions = getContent()
            } else if ("MimeType" == name) {
                val t = type!!.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                val mimeType = mimeType(t[0].trim({ it <= ' ' }), t[1].trim({ it <= ' ' }),
                        mlist<String>(*extensions!!.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()).bind(Options.asList<String>().o(Strings.trimToNone)).value(),
                        Collections.nil(), option(description), none(""), none(""))
                registry!!.add(mimeType)
            }
        }

        @Throws(SAXException::class)
        override fun warning(e: SAXParseException?) {
            super.warning(e)
        }

        @Throws(SAXException::class)
        override fun error(e: SAXParseException?) {
            super.error(e)
        }

        @Throws(SAXException::class)
        override fun fatalError(e: SAXParseException?) {
            super.fatalError(e)
        }

    }

}
/** Disallow construction of this utility class  */
