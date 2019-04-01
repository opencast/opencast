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

import org.opencastproject.mediapackage.identifier.Id

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * This class provides factory methods for the creation of media packages from manifest files, directories or from
 * scratch. This class is not thread safe, so create a new builder in each method invocation.
 */
class MediaPackageBuilderImpl : MediaPackageBuilder {

    /** The media package serializer  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageBuilder.getSerializer
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageBuilder.setSerializer
     */
    override var serializer: MediaPackageSerializer? = null

    /**
     * Creates a new media package builder.
     *
     * @throws IllegalStateException
     * if the temporary directory cannot be created or is not accessible
     */
    constructor() {}

    /**
     * Creates a new media package builder that uses the given serializer to resolve urls while reading manifests and
     * adding new elements.
     *
     * @param serializer
     * the media package serializer
     * @throws IllegalStateException
     * if the temporary directory cannot be created or is not accessible
     */
    constructor(serializer: MediaPackageSerializer?) {
        if (serializer == null)
            throw IllegalArgumentException("Serializer may not be null")
        this.serializer = serializer
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageBuilder.createNew
     */
    @Throws(MediaPackageException::class)
    override fun createNew(): MediaPackage {
        return MediaPackageImpl()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageBuilder.createNew
     */
    @Throws(MediaPackageException::class)
    override fun createNew(identifier: Id): MediaPackage {
        return MediaPackageImpl(identifier)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageBuilder.loadFromXml
     */
    @Throws(MediaPackageException::class)
    override fun loadFromXml(`is`: InputStream): MediaPackage {
        return if (serializer != null) {
            // FIXME This code runs if *any* serializer is present, regardless of the serializer implementation
            try {
                val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(`is`)
                rewriteUrls(xml, serializer)
                MediaPackageImpl.valueOf(xml)
            } catch (e: Exception) {
                throw MediaPackageException("Error deserializing paths in media package", e)
            }

        } else {
            MediaPackageImpl.valueOf(`is`)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageBuilder.loadFromXml
     */
    @Throws(MediaPackageException::class)
    override fun loadFromXml(xml: String): MediaPackage {
        var `in`: InputStream? = null
        try {
            `in` = IOUtils.toInputStream(xml, "UTF-8")
            return loadFromXml(`in`)
        } catch (e: IOException) {
            throw MediaPackageException(e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @Throws(MediaPackageException::class)
    override fun loadFromXml(xml: Node): MediaPackage {
        return if (serializer != null) {
            // FIXME This code runs if *any* serializer is present, regardless of the serializer implementation
            try {
                rewriteUrls(xml, serializer)
                MediaPackageImpl.valueOf(xml)
            } catch (e: Exception) {
                throw MediaPackageException("Error deserializing paths in media package", e)
            }

        } else {
            MediaPackageImpl.valueOf(xml)
        }
    }

    companion object {

        /** The logging instance  */
        private val logger = LoggerFactory.getLogger(MediaPackageBuilderImpl::class.java!!)

        /**
         * Rewrite the url elements using the serializer. Attention: This method modifies the given DOM!
         */
        @Throws(XPathExpressionException::class, URISyntaxException::class)
        private fun rewriteUrls(xml: Node, serializer: MediaPackageSerializer) {
            val xPath = XPathFactory.newInstance().newXPath()
            val nodes = xPath.evaluate("//*[local-name() = 'url']", xml, XPathConstants.NODESET) as NodeList
            for (i in 0 until nodes.length) {
                val uri = nodes.item(i).firstChild
                if (uri != null) {
                    val uriStr = uri.nodeValue
                    val trimmedUriStr = uriStr.trim { it <= ' ' }
                    /*
         * Warn the user if trimming is necessary as this means that the URI was technically invalid.
         */
                    if (trimmedUriStr != uriStr) {
                        logger.warn("Detected invalid URI. Trying to fix it by " + "removing spaces from beginning/end.")
                    }
                    uri.nodeValue = serializer.decodeURI(URI(trimmedUriStr)).toString()
                }
            }
        }
    }

}
