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
package org.opencastproject.smil.api.util

import org.opencastproject.util.IoSupport.withResource

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.CatalogSelector
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.XmlUtil
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.functions.Misc
import org.opencastproject.workspace.api.Workspace

import com.android.mms.dom.smil.parser.SmilXmlParser
import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.FnX

import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.smil.SMILDocument
import org.xml.sax.InputSource
import org.xml.sax.SAXException

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * General purpose utility functions for dealing with SMIL.
 */
object SmilUtil {

    private val logger = LoggerFactory.getLogger(SmilUtil::class.java)

    val SMIL_NODE_NAME = "smil"
    val SMIL_NS_URI = "http://www.w3.org/ns/SMIL"

    /** Parse a SMIL document from an input stream.  */
    val parseSmilFn: Fn<InputStream, SMILDocument> = object : FnX<InputStream, SMILDocument>() {
        @Throws(SAXException::class, IOException::class)
        override fun applyX(`in`: InputStream): SMILDocument {
            return SmilXmlParser().parse(`in`)
        }
    }

    enum class TrackType {
        PRESENTER, PRESENTATION
    }

    /**
     * Read a SMIL document from a string.
     *
     * @throws java.io.IOException
     * in case of any IO error
     * @throws org.xml.sax.SAXException
     * in case of a SAX related error
     */
    @Throws(IOException::class, SAXException::class)
    fun readSmil(smil: String): SMILDocument {
        return withResource(IOUtils.toInputStream(smil, "UTF-8"), parseSmilFn)
    }

    /**
     * Load the SMIL document identified by `mpe`. Throws an exception if it does not exist or cannot be loaded
     * by any reason.
     *
     * @return the document
     */
    fun loadSmilDocument(`in`: InputStream, mpe: MediaPackageElement): Document {
        try {
            val eitherDocument = XmlUtil.parseNs(InputSource(`in`))
            if (eitherDocument.isRight)
                return eitherDocument.right().value()

            throw eitherDocument.left().value()
        } catch (e: Exception) {
            logger.warn("Unable to load smil document from catalog '{}'", mpe, e)
            return Misc.chuck(e)
        }

    }

    /**
     * Creates a skeleton SMIL document
     *
     * @return the SMIL document
     */
    fun createSmil(): Document {
        val smilDocument = XmlUtil.newDocument()
        smilDocument.xmlVersion = "1.1"
        val smil = smilDocument.createElementNS(SMIL_NS_URI, SMIL_NODE_NAME)
        smil.setAttribute("version", "3.0")
        smilDocument.appendChild(smil)
        val head = smilDocument.createElement("head")
        smil.appendChild(head)
        val body = smilDocument.createElement("body")
        smil.appendChild(body)
        val parallel = smilDocument.createElement("par")
        parallel.setAttribute("dur", "0ms")
        body.appendChild(parallel)
        return smilDocument
    }

    /**
     * Adds a track to the SMIL document.
     *
     * @param smilDocument
     * the SMIL document
     * @param trackType
     * the track type
     * @param hasVideo
     * whether the track has a video stream
     * @param startTime
     * the start time
     * @param duration
     * the duration
     * @param uri
     * the track URI
     * @param trackId
     * the Id of the track
     * @return the augmented SMIL document
     */
    @JvmOverloads
    fun addTrack(smilDocument: Document, trackType: TrackType, hasVideo: Boolean, startTime: Long,
                 duration: Long, uri: URI, trackId: String? = null): Document {
        val parallel = smilDocument.getElementsByTagName("par").item(0) as Element
        if (parallel.childNodes.length == 0) {
            val presenterSeq = smilDocument.createElement("seq")
            parallel.appendChild(presenterSeq)
            val presentationSeq = smilDocument.createElement("seq")
            parallel.appendChild(presentationSeq)
        }

        val trackDurationString = parallel.getAttribute("dur")
        val oldTrackDuration = java.lang.Long.parseLong(trackDurationString.substring(0, trackDurationString.indexOf("ms")))
        val newTrackDuration = startTime + duration
        if (newTrackDuration > oldTrackDuration) {
            parallel.setAttribute("dur", newTrackDuration.toString() + "ms")
        }

        val sequence: Node
        when (trackType) {
            SmilUtil.TrackType.PRESENTER -> sequence = parallel.childNodes.item(0)
            SmilUtil.TrackType.PRESENTATION -> sequence = parallel.childNodes.item(1)
            else -> throw IllegalStateException("Unknown track type $trackType")
        }

        val element = smilDocument.createElement(if (hasVideo) "video" else "audio")
        element.setAttribute("begin", java.lang.Long.toString(startTime) + "ms")
        element.setAttribute("dur", java.lang.Long.toString(duration) + "ms")
        element.setAttribute("src", URIUtil.getPath(uri.toString()))
        if (trackId != null) {
            element.setAttribute("xml:id", trackId)
        }
        sequence.appendChild(element)
        return smilDocument
    }


    @Throws(IOException::class, SAXException::class, NotFoundException::class)
    fun getSmilDocumentFromMediaPackage(mp: MediaPackage, smilFlavor: MediaPackageElementFlavor,
                                        workspace: Workspace): SMILDocument {
        val smilSelector = CatalogSelector()
        smilSelector.addFlavor(smilFlavor)
        val smilCatalog = smilSelector.select(mp, false)
        if (smilCatalog.size == 1) {
            return getSmilDocument(smilCatalog.iterator().next(), workspace)
        } else {
            logger.error("More or less than one smil catalog found: {}", smilCatalog)
            throw IllegalStateException("More or less than one smil catalog found!")
        }
    }

    /** Get the SMIL document from a catalog.  */
    @Throws(NotFoundException::class, IOException::class, SAXException::class)
    private fun getSmilDocument(smilCatalog: Catalog, workspace: Workspace): SMILDocument {
        var `in`: FileInputStream? = null
        try {
            val smilXmlFile = workspace.get(smilCatalog.getURI())
            val smilParser = SmilXmlParser()
            `in` = FileInputStream(smilXmlFile)
            return smilParser.parse(`in`)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

}
/**
 * Adds a track to the SMIL document.
 *
 * @param smilDocument
 * the SMIL document
 * @param trackType
 * the track type
 * @param hasVideo
 * whether the track has a video stream
 * @param startTime
 * the start time
 * @param duration
 * the duration
 * @param uri
 * the track URI
 * @return the augmented SMIL document
 */
