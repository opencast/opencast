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
package org.opencastproject.distribution.streaming.wowza

import java.lang.String.format
import org.opencastproject.util.RequireUtil.notNull

import org.opencastproject.distribution.api.AbstractDistributionService
import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.StreamingDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.TrackImpl.StreamingProtocol
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.FileSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.MimeType
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.OsgiUtil
import org.opencastproject.util.RequireUtil
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.DOMException
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Dictionary
import java.util.HashMap
import java.util.HashSet
import java.util.Objects
import java.util.TreeSet
import java.util.stream.Collectors

import javax.ws.rs.core.UriBuilder
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Distributes media to the local media delivery directory.
 */
/**
 * Creates a new instance of the streaming distribution service.
 */
class WowzaAdaptiveStreamingDistributionService : AbstractDistributionService(JOB_TYPE), StreamingDistributionService, ManagedService {

    /** The load on the system introduced by creating a distribute job  */
    private var distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD

    /** The load on the system introduced by creating a retract job  */
    private var retractJobLoad = DEFAULT_RETRACT_JOB_LOAD

    /** The distribution directory  */
    protected var distributionDirectory: File? = null

    /** The base URI for streaming  */
    var streamingUri: URI? = null

    /** The base URI for adaptive streaming  */
    var adaptiveStreamingUri: URI? = null

    /** The set of supported streaming formats to distribute.  */
    private var supportedAdaptiveFormats: MutableSet<StreamingProtocol>? = null

    /** Whether or not RTMP is supported  */
    private var isRTMPSupported = false

    /** Whether or not the video order in the SMIL files is descending  */
    private var isSmilOrderDescending = false

    /** List of available operations on jobs  */
    private enum class Operation {
        Distribute, Retract
    }

    override fun activate(cc: ComponentContext) {
        // Get the configured streaming and server URLs
        if (cc != null) {

            val readStreamingUrl = StringUtils.trimToNull(cc.bundleContext.getProperty(STREAMING_URL_KEY))
            val readStreamingPort = StringUtils.trimToNull(cc.bundleContext.getProperty(STREAMING_PORT_KEY))
            val readAdaptiveStreamingUrl = StringUtils
                    .trimToNull(cc.bundleContext.getProperty(ADAPTIVE_STREAMING_URL_KEY))
            val readAdaptiveStreamingPort = StringUtils
                    .trimToNull(cc.bundleContext.getProperty(ADAPTIVE_STREAMING_PORT_KEY))

            try {
                streamingUri = getStreamingUrl(readStreamingUrl, readStreamingPort, validStreamingSchemes,
                        DEFAULT_STREAMING_SCHEME, DEFAULT_STREAMING_URL)
                logger.info("Streaming URL set to \"{}\"", streamingUri)
            } catch (e: URISyntaxException) {
                logger.warn("Streaming URL {} could not be parsed", readStreamingUrl, e)
            }

            try {
                adaptiveStreamingUri = getStreamingUrl(readAdaptiveStreamingUrl, readAdaptiveStreamingPort,
                        validAdaptiveStreamingSchemes, DEFAULT_ADAPTIVE_STREAMING_SCHEME, null)
                logger.info("Adaptive streaming URL set to \"{}\"", adaptiveStreamingUri)
            } catch (e: URISyntaxException) {
                logger.warn("Adaptive Streaming URL {} could not be parsed: {}", readAdaptiveStreamingUrl,
                        ExceptionUtils.getStackTrace(e))
            } catch (e: IllegalArgumentException) {
                logger.info("Adaptive streaming URL was not defined in the configuration file")
            }

            if (adaptiveStreamingUri == null && streamingUri == null) {
                throw IllegalArgumentException("Streaming URL and adaptive streaming URL are undefined.")
            }

            var distributionDirectoryPath: String? = StringUtils
                    .trimToNull(cc.bundleContext.getProperty("org.opencastproject.streaming.directory"))
            if (distributionDirectoryPath == null) {
                // set default streaming directory to ${org.opencastproject.storage.dir}/streams
                distributionDirectoryPath = StringUtils
                        .trimToNull(cc.bundleContext.getProperty("org.opencastproject.storage.dir"))
                if (distributionDirectoryPath != null) {
                    distributionDirectoryPath += "/streams"
                }
            }
            if (distributionDirectoryPath == null)
                logger.warn("Streaming distribution directory must be set (org.opencastproject.streaming.directory)")
            else {
                distributionDirectory = File(distributionDirectoryPath)
                if (!distributionDirectory!!.isDirectory()) {
                    try {
                        Files.createDirectories(distributionDirectory!!.toPath())
                    } catch (e: IOException) {
                        throw IllegalStateException("Distribution directory does not exist and can't be created", e)
                    }

                }
            }

            logger.info("Streaming distribution directory is {}", distributionDirectory)
        }
    }

    override fun getDistributionType(): String {
        return DISTRIBUTION_TYPE
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>?) {
        val formats: Option<String>
        val smilOrder: Option<String>

        if (properties == null) {
            formats = Option.none()
            smilOrder = Option.none()
        } else {
            formats = OsgiUtil.getOptCfg(properties, STREAMING_FORMATS_KEY)
            smilOrder = OsgiUtil.getOptCfg(properties, SMIL_ORDER_KEY)
        }

        if (formats.isSome) {
            setSupportedFormats(formats.get())
        } else {
            setDefaultSupportedFormats()
        }
        logger.info("The supported streaming formats are: {}", StringUtils.join(supportedAdaptiveFormats, ","))

        if (smilOrder.isNone || SMIL_ASCENDING_VALUE == smilOrder.get()) {
            logger.info("The videos in the SMIL files will be sorted in ascending bitrate order")
            isSmilOrderDescending = false
        } else if (SMIL_DESCENDING_VALUE == smilOrder.get()) {
            isSmilOrderDescending = true
            logger.info("The videos in the SMIL files will be sorted in descending bitrate order")
        } else {
            throw ConfigurationException(SMIL_ORDER_KEY, format("Illegal value '%s'. Valid options are '%s' and '%s'",
                    smilOrder.get(), SMIL_ASCENDING_VALUE, SMIL_DESCENDING_VALUE))
        }

        distributeJobLoad = LoadUtil.getConfiguredLoadValue(properties!!, DISTRIBUTE_JOB_LOAD_KEY,
                DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry)
        retractJobLoad = LoadUtil.getConfiguredLoadValue(properties, RETRACT_JOB_LOAD_KEY, DEFAULT_RETRACT_JOB_LOAD,
                serviceRegistry)
    }

    /**
     * Transform the configuration value into the supported formats to distribute to the Wowza server.
     *
     * @param formatString
     * The string to parse with the supported formats.
     */
    protected fun setSupportedFormats(formatString: String) {
        supportedAdaptiveFormats = TreeSet()

        for (format in formatString.toUpperCase().split("[\\s,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!format.isEmpty()) {
                try {
                    val protocol = StreamingProtocol.valueOf(format)
                    if (protocol == StreamingProtocol.RTMP)
                        isRTMPSupported = true
                    else
                        supportedAdaptiveFormats!!.add(protocol)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Found incorrect format \"{}\". Ignoring...", format)
                }

            }
        }
    }

    /**
     * Get the default set of supported formats to distribute to Wowza.
     */
    protected fun setDefaultSupportedFormats() {
        isRTMPSupported = true
        supportedAdaptiveFormats = TreeSet()
        supportedAdaptiveFormats!!.add(TrackImpl.StreamingProtocol.HLS)
        supportedAdaptiveFormats!!.add(TrackImpl.StreamingProtocol.HDS)
        supportedAdaptiveFormats!!.add(TrackImpl.StreamingProtocol.SMOOTH)
        supportedAdaptiveFormats!!.add(TrackImpl.StreamingProtocol.DASH)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.StreamingDistributionService.distribute
     */
    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>): Job {

        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")

        if (streamingUri == null && adaptiveStreamingUri == null)
            throw IllegalStateException(
                    "A least one streaming url must be set (org.opencastproject.streaming.url,org.opencastproject.adaptive-streaming.url)")
        if (distributionDirectory == null)
            throw IllegalStateException(
                    "Streaming distribution directory must be set (org.opencastproject.streaming.directory)")

        try {
            return serviceRegistry.createJob(
                    JOB_TYPE,
                    Operation.Distribute.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds)), distributeJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.DistributionService.distribute
     */
    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediapackage: MediaPackage, elementId: String): Job {
        val elmentIds = HashSet()
        elmentIds.add(elementId)
        return distribute(channelId, mediapackage, elmentIds)
    }

    /**
     * Distribute Mediapackage elements to the download distribution service.
     *
     * @param channelId The id of the publication channel to be distributed to.
     * @param mediapackage The media package that contains the elements to be distributed.
     * @param elementIds The ids of the elements that should be distributed
     * contained within the media package.
     * @return A reference to the MediaPackageElements that have been distributed.
     * @throws DistributionException Thrown if the parent directory of the
     * MediaPackageElement cannot be created, if the MediaPackageElement cannot be
     * copied or another unexpected exception occurs.
     */
    @Throws(DistributionException::class)
    fun distributeElements(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>?): Array<MediaPackageElement> {
        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")

        val elements = getElements(mediapackage, elementIds!!)
        val distributedElements = ArrayList<MediaPackageElement>()

        for (element in elements) {
            val distributed = distributeElement(channelId, mediapackage, element.identifier)
            if (distributed != null) {
                for (e in distributed) {
                    if (e != null) distributedElements.add(e)
                }
            }
        }
        return distributedElements.toTypedArray()
    }

    /**
     * Distribute a Mediapackage element to the download distribution service.
     *
     * @param mediapackage
     * The media package that contains the element to distribute.
     * @param elementId
     * The id of the element that should be distributed contained within the media package.
     * @return A reference to the MediaPackageElement that has been distributed.
     * @throws DistributionException
     * Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
     * cannot be copied or another unexpected exception occurs.
     */
    @Synchronized
    @Throws(DistributionException::class)
    fun distributeElement(channelId: String, mediapackage: MediaPackage, elementId: String): Array<MediaPackageElement>? {
        notNull(mediapackage, "mediapackage")
        notNull(elementId, "elementId")
        notNull(channelId, "channelId")

        val element = mediapackage.getElementById(elementId) ?: throw IllegalStateException(
                "No element " + elementId + " found in mediapackage" + mediapackage.identifier)

        // Make sure the element exists

        // Streaming servers only deal with tracks
        if (MediaPackageElement.Type.Track != element.elementType) {
            logger.debug("Skipping {} {} for distribution to the streaming server",
                    element.elementType.toString().toLowerCase(), element.identifier)
            return null
        }

        try {
            val source: File
            try {
                source = workspace.get(element.getURI())
            } catch (e: NotFoundException) {
                throw DistributionException("Unable to find " + element.getURI() + " in the workspace", e)
            } catch (e: IOException) {
                throw DistributionException("Error loading " + element.getURI() + " from the workspace", e)
            }

            val distribution = ArrayList<MediaPackageElement>()

            if (!isRTMPSupported && supportedAdaptiveFormats!!.isEmpty()) {
                logger.warn("Skipping distribution of element \"{}\" because no streaming format was specified", element)
                return distribution.toTypedArray()
            }

            // Put the file in place

            val destination = getDistributionFile(channelId, mediapackage, element)
            try {
                Files.createDirectories(destination.toPath().parent)
            } catch (e: IOException) {
                throw DistributionException("Unable to create " + destination.parentFile, e)
            }

            logger.info("Distributing {} to {}", elementId, destination)

            try {
                FileSupport.link(source, destination, true)
            } catch (e: IOException) {
                throw DistributionException("Unable to copy $source to $destination", e)
            }

            if (isRTMPSupported) {
                // Create a representation of the distributed file in the mediapackage
                val distributedElement = element.clone() as MediaPackageElement

                try {
                    distributedElement.setURI(getDistributionUri(channelId, mediapackage, element))
                } catch (e: URISyntaxException) {
                    throw DistributionException("Distributed element produces an invalid URI", e)
                }

                distributedElement.identifier = null
                setTransport(distributedElement, TrackImpl.StreamingProtocol.RTMP)
                distributedElement.referTo(element)

                distribution.add(distributedElement)
            }

            if (!supportedAdaptiveFormats!!.isEmpty() && isAdaptiveStreamingFormat(element)) {
                // Only if the Smil file does not exist we need to distribute adaptive streams
                // Otherwise the adaptive streams only were extended with new qualities
                val smilFile = getSmilFile(element, mediapackage, channelId)
                val createAdaptiveStreamingEntries = !smilFile.isFile
                val smilXml = getSmilDocument(smilFile)
                addElementToSmil(smilXml, channelId, mediapackage, element)
                val smilUri = getSmilUri(smilFile)

                if (createAdaptiveStreamingEntries) {
                    for (protocol in supportedAdaptiveFormats!!) {
                        distribution.add(createTrackforStreamingProtocol(element, smilUri, protocol))
                        logger.info("Distributed element {} in {} format to the Wowza Server", element, protocol)
                    }
                } else {
                    logger.debug("Skipped adding adaptive streaming manifest {} to search index, as it already exists.", element)
                }

                saveSmilFile(smilFile, smilXml)
            }

            logger.info("Distributed file {} to Wowza Server", element)
            return distribution.toTypedArray()

        } catch (e: Exception) {
            logger.warn("Error distributing $element", e)
            if (e is DistributionException) {
                throw e
            } else {
                throw DistributionException(e)
            }
        }

    }

    private fun setTransport(element: MediaPackageElement, protocol: TrackImpl.StreamingProtocol) {
        if (element is TrackImpl) {
            element.setTransport(protocol)
        }
    }

    private fun getSmilFile(element: MediaPackageElement, mediapackage: MediaPackage, channelId: String): File {
        val orgId = securityService.organization.id
        val smilFileName = (channelId + "_" + mediapackage.identifier + "_" + element.flavor.type
                + ".smil")
        return distributionDirectory!!.toPath().resolve(Paths.get(orgId, smilFileName)).toFile()
    }

    @Throws(URISyntaxException::class)
    private fun getSmilUri(smilFile: File): URI {
        return UriBuilder.fromUri(adaptiveStreamingUri).path("smil:" + smilFile.name).build()
    }

    @Throws(URISyntaxException::class)
    private fun getAdaptiveStreamingUri(smilUri: URI, protocol: StreamingProtocol): URI {
        val fileName: String
        when (protocol) {
            TrackImpl.StreamingProtocol.HLS -> fileName = "playlist.m3u8"
            TrackImpl.StreamingProtocol.HDS -> fileName = "manifest.f4m"
            TrackImpl.StreamingProtocol.SMOOTH -> fileName = "Manifest"
            TrackImpl.StreamingProtocol.DASH -> fileName = "manifest_mpm4sav_mvlist.mpd"
            else -> fileName = ""
        }
        return URI(UrlSupport.concat(smilUri.toString(), fileName))
    }

    private fun isAdaptiveStreamingFormat(element: MediaPackageElement): Boolean {
        val uriPath = element.getURI().getPath()
        return uriPath.endsWith(".mp4") || uriPath.contains("mp4:")
    }

    @Throws(DistributionException::class)
    private fun getSmilDocument(smilFile: File): Document {
        if (!smilFile.isFile) {
            try {
                val docBuilderFactory = DocumentBuilderFactory.newInstance()
                val docBuilder = docBuilderFactory.newDocumentBuilder()
                val doc = docBuilder.newDocument()
                val smil = doc.createElement("smil")
                doc.appendChild(smil)

                val head = doc.createElement("head")
                smil.appendChild(head)

                val body = doc.createElement("body")
                smil.appendChild(body)

                val switchElement = doc.createElement("switch")
                body.appendChild(switchElement)

                return doc
            } catch (ex: ParserConfigurationException) {
                logger.error("Could not create XML file for {}.", smilFile)
                throw DistributionException("Could not create XML file for $smilFile")
            }

        }

        try {
            val docBuilderFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = docBuilderFactory.newDocumentBuilder()
            val doc = docBuilder.parse(smilFile)

            if (!"smil".equals(doc.documentElement.nodeName, ignoreCase = true)) {
                logger.error("XML-File {} is not a SMIL file.", smilFile)
                throw DistributionException(format("XML-File %s is not an SMIL file.", smilFile.name))
            }

            return doc
        } catch (e: IOException) {
            logger.error("Could not open SMIL file {}", smilFile)
            throw DistributionException(format("Could not open SMIL file %s", smilFile))
        } catch (e: ParserConfigurationException) {
            logger.error("Could not parse SMIL file {}", smilFile)
            throw DistributionException(format("Could not parse SMIL file %s", smilFile))
        } catch (e: SAXException) {
            logger.error("Could not parse XML file {}", smilFile)
            throw DistributionException(format("Could not parse XML file %s", smilFile))
        }

    }

    @Throws(DistributionException::class)
    private fun saveSmilFile(smilFile: File, doc: Document) {
        try {
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            val source = DOMSource(doc)
            val stream = StreamResult(smilFile)
            transformer.transform(source, stream)
            logger.info("SMIL file for Wowza server saved at {}", smilFile)
        } catch (ex: TransformerConfigurationException) {
            logger.error("Could not write SMIL file {} for distribution", smilFile)
            throw DistributionException(format("Could not write SMIL file %s for distribution", smilFile))
        } catch (ex: TransformerException) {
            logger.error("Could not write SMIL file {} for distribution", smilFile)
            throw DistributionException(format("Could not write SMIL file %s for distribution", smilFile))
        }

    }

    @Throws(DOMException::class, URISyntaxException::class)
    private fun addElementToSmil(doc: Document, channelId: String, mediapackage: MediaPackage, element: MediaPackageElement) {
        if (element !is TrackImpl)
            return
        val switchElementsList = doc.getElementsByTagName("switch")
        var switchElement: Node? = null

        // There should only be one switch element in the file. If there are more we will igore this.
        // If there is no switch element we need to create the xml first.
        if (switchElementsList.length > 0) {
            switchElement = switchElementsList.item(0)
        } else {
            if (doc.getElementsByTagName("head").length < 1)
                doc.appendChild(doc.createElement("head"))
            if (doc.getElementsByTagName("body").length < 1)
                doc.appendChild(doc.createElement("body"))
            switchElement = doc.createElement("switch")
            doc.getElementsByTagName("body").item(0).appendChild(switchElement)
        }

        val video = doc.createElement("video")
        video.setAttribute("src", getAdaptiveDistributionName(channelId, mediapackage, element))

        var bitrate = 0f

        // Add bitrate corresponding to the audio streams
        for (stream in element.getAudio()!!) {
            bitrate += stream.bitRate!!
        }

        // Add bitrate corresponding to the video streams
        // Also, set the video width and height values:
        // In the rare case where there is more than one video stream, the values of the first stream
        // have priority, but always prefer the first stream with both "frameWidth" and "frameHeight"
        // parameters defined
        var width: Int? = null
        var height: Int? = null
        for (stream in element.getVideo()!!) {
            bitrate += stream.bitRate!!
            // Update if both width and height are defined for a stream or if we have no values at all
            if (stream.frameWidth != null && stream.frameHeight != null || width == null && height == null) {
                width = stream.frameWidth
                height = stream.frameHeight
            }
        }

        video.setAttribute(SMIL_ATTR_VIDEO_BITRATE, Integer.toString(bitrate.toInt()))

        if (width != null) {
            video.setAttribute(SMIL_ATTR_VIDEO_WIDTH, Integer.toString(width))
        } else {
            logger.debug("Could not set video width in the SMIL file for element {} of mediapackage {}. The value was null",
                    element.identifier, mediapackage.identifier)
        }
        if (height != null) {
            video.setAttribute(SMIL_ATTR_VIDEO_HEIGHT, Integer.toString(height))
        } else {
            logger.debug("Could not set video height in the SMIL file for element {} of mediapackage {}. The value was null",
                    element.identifier, mediapackage.identifier)
        }

        val currentVideos = switchElement!!.childNodes
        for (i in 0 until currentVideos.length) {
            val current = currentVideos.item(i)
            if ("video" == current.nodeName) {
                val currentBitrate = java.lang.Float
                        .parseFloat(current.attributes.getNamedItem(SMIL_ATTR_VIDEO_BITRATE).textContent)
                if (isSmilOrderDescending && currentBitrate < bitrate || !isSmilOrderDescending && currentBitrate > bitrate) {
                    switchElement.insertBefore(video, current)
                    return
                }
            }
        }

        // If we get here, we could not insert the video before
        switchElement.appendChild(video)
    }

    @Throws(URISyntaxException::class)
    private fun createTrackforStreamingProtocol(element: MediaPackageElement, smilUri: URI,
                                                protocol: StreamingProtocol): TrackImpl {
        val track = element.clone() as TrackImpl

        when (protocol) {
            TrackImpl.StreamingProtocol.HLS -> track.mimeType = MimeType.mimeType("application", "x-mpegURL")
            TrackImpl.StreamingProtocol.HDS -> track.mimeType = MimeType.mimeType("application", "f4m+xml")
            TrackImpl.StreamingProtocol.SMOOTH -> track.mimeType = MimeType.mimeType("application", "vnd.ms-sstr+xml")
            TrackImpl.StreamingProtocol.DASH -> track.mimeType = MimeType.mimeType("application", "dash+xml")
            else -> throw IllegalArgumentException(format("Received invalid, non-adaptive streaming protocol: '%s'", protocol))
        }

        setTransport(track, protocol)
        track.setURI(getAdaptiveStreamingUri(smilUri, protocol))
        track.referTo(element)
        track.identifier = null
        track.setAudio(null!!)
        track.setVideo(null!!)
        track.checksum = null

        return track
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.DistributionService.retract
     */
    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediapackage: MediaPackage, elementId: String): Job {
        val elementIds = HashSet()
        elementIds.add(elementId)
        return retract(channelId, mediapackage, elementIds)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.StreamingDistributionService.retract
     */
    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): Job {
        RequireUtil.notNull(mediaPackage, "mediaPackage")
        RequireUtil.notNull(elementIds, "elementIds")
        RequireUtil.notNull(channelId, "channelId")
        //
        try {
            return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), gson.toJson(elementIds)),
                    retractJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    @Throws(DistributionException::class)
    override fun distributeSync(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement>? {
        val result = distributeElements(channelId, mediapackage, elementIds) ?: return null
        return Arrays.stream(result)
                .filter(Predicate<MediaPackageElement> { Objects.nonNull(it) })
                .collect<List<MediaPackageElement>, Any>(Collectors.toList())
    }

    @Throws(DistributionException::class)
    override fun retractSync(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement>? {
        if (distributionDirectory == null || streamingUri == null && adaptiveStreamingUri == null) {
            return null
        }
        val result = retractElements(channelId, mediaPackage, elementIds) ?: return null
        return Arrays.stream(result).filter(Predicate<MediaPackageElement> { Objects.nonNull(it) }).collect<List<MediaPackageElement>, Any>(Collectors.toList())
    }

    /**
     * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
     * one given as parameter `elementId`. Instead, the element's distribution URI will be calculated. This way
     * you are able to retract elements by providing the "original" element here.
     *
     * @param channelId
     * the channel id
     * @param mediapackage
     * the mediapackage
     * @param elementIds
     * the element identifiers
     * @return the retracted element or `null` if the element was not retracted
     * @throws org.opencastproject.distribution.api.DistributionException
     * in case of an error
     */
    @Throws(DistributionException::class)
    protected fun retractElements(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>?): Array<MediaPackageElement> {
        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")

        val elements = getElements(mediapackage, elementIds!!)
        val retractedElements = ArrayList<MediaPackageElement>()

        for (element in elements) {
            val retracted = retractElement(channelId, mediapackage, element.identifier)
            if (retracted != null) {
                for (e in retracted) {
                    if (e != null) retractedElements.add(e)
                }
            }
        }
        return retractedElements.toTypedArray()
    }

    /**
     * Retracts the mediapackage with the given identifier from the distribution channel.
     *
     * @param channelId
     * the channel id
     * @param mediapackage
     * the mediapackage
     * @param elementId
     * the element identifier
     * @return the retracted element or `null` if the element was not retracted
     */
    @Throws(DistributionException::class)
    protected fun retractElement(channelId: String, mediapackage: MediaPackage, elementId: String): Array<MediaPackageElement>? {

        notNull(mediapackage, "mediapackage")
        notNull(elementId, "elementId")
        notNull(channelId, "channelId")

        // Make sure the element exists
        val element = mediapackage.getElementById(elementId) ?: throw IllegalStateException(
                "No element " + elementId + " found in mediapackage" + mediapackage.identifier)

        logger.debug("Start element retraction for element \"{}\" with URI {}", elementId, element.getURI())

        val retractedElements = ArrayList<MediaPackageElement>()

        // Has this element been distributed?
        if (element == null || element !is TrackImpl)
            return null

        try {
            // Get the distribution path on the disk for this mediapackage element
            var elementFile: File? = getDistributionFile(channelId, mediapackage, element)
            val smilFile = getSmilFile(element, mediapackage, channelId)
            logger.debug("delete elementFile {}", elementFile)

            // Does the file exist? If not, the current element has not been distributed to this channel
            // or has been removed otherwise
            if (elementFile == null || !elementFile.exists()) {
                logger.warn("Deleting element file: File does not exist. Perhaps was it already deleted?: {}", elementFile)
                retractedElements.add(element)
                return retractedElements.toTypedArray()
            } else {
                // If a SMIL file is referenced by this element, delete first all the elements within
                if (elementFile == smilFile) {
                    val smilXml = getSmilDocument(smilFile)
                    val videoList = smilXml.getElementsByTagName("video")
                    for (i in 0 until videoList.length) {
                        if (videoList.item(i) is Element) {
                            var smilPathStr = (videoList.item(i) as Element).getAttribute("src")
                            // Patch the streaming tags
                            if (smilPathStr.contains("mp4:"))
                                smilPathStr = smilPathStr.replace("mp4:", "")
                            if (!smilPathStr.endsWith(".mp4"))
                                smilPathStr += ".mp4"

                            elementFile = smilFile.toPath().resolveSibling(smilPathStr).toFile()
                            deleteElementFile(elementFile!!)
                        }
                    }

                    if (smilFile.isFile && !smilFile.delete()) {
                        logger.warn("The SMIL file {} could not be succesfully deleted. Forcing quite deletion...")
                    }
                } else {
                    deleteElementFile(elementFile)
                }

            }

            logger.info("Finished rectracting element {} of media package {}", elementId, mediapackage)
            retractedElements.add(element)
            return retractedElements.toTypedArray()
        } catch (e: Exception) {
            logger.warn("Error retracting element $elementId of mediapackage $mediapackage", e)
            if (e is DistributionException) {
                throw e
            } else {
                throw DistributionException(e)
            }
        }

    }

    /**
     * Delete an element file and the parent folders, if necessary
     *
     * @param elementFile
     */
    private fun deleteElementFile(elementFile: File) {

        // Try to remove the element file
        if (elementFile.exists()) {
            if (!elementFile.delete())
                logger.warn("Could not properly delete element file: {}", elementFile)
        } else {
            logger.warn("Tried to delete non-existent element file. Perhaps was already deleted?: {}", elementFile)
        }

        // Try to remove the parent folders, if possible
        val elementDir = elementFile.parentFile
        if (elementDir != null && elementDir.exists()) {
            if (elementDir.list()!!.size == 0) {
                if (!elementDir.delete())
                    logger.warn("Could not properly delete element directory: {}", elementDir)
            } else {
                logger.warn("Element directory was not empty after deleting element. Skipping deletion: {}", elementDir)
            }
        } else {
            logger.warn("Element directory did not exist when trying to delete it: {}", elementDir)
        }

        val mediapackageDir = elementDir!!.parentFile
        if (mediapackageDir != null && mediapackageDir.exists()) {
            if (mediapackageDir.list()!!.size == 0) {
                if (!mediapackageDir.delete())
                    logger.warn("Could not properly delete mediapackage directory: {}", mediapackageDir)
            } else {
                logger.debug("Mediapackage directory was not empty after deleting element. Skipping deletion: {}",
                        mediapackageDir)
            }
        } else {
            logger.warn("Mediapackage directory did not exist when trying to delete it: {}", mediapackageDir)
        }
    }

    /**
     * Gets the destination file to copy the contents of a mediapackage element.
     *
     * @return The file to copy the content to
     */
    @Throws(DistributionException::class)
    protected fun getDistributionFile(channelId: String, mediapackage: MediaPackage, element: MediaPackageElement): File {

        val orgId = securityService.organization.id
        val distributionPath = distributionDirectory!!.toPath().resolve(orgId)
        val elementUri = element.getURI()
        var relativeUri: URI

        if (adaptiveStreamingUri != null) {
            relativeUri = adaptiveStreamingUri!!.relativize(elementUri)
            if (relativeUri !== elementUri) {
                // SMIL file

                // Get the relativized URL path
                var uriPath = relativeUri.path
                // Remove the last part (corresponds to the part of the "virtual" manifests)
                uriPath = uriPath.substring(0, uriPath.lastIndexOf('/'))
                // Remove the "smil:" tags, if any, and set the right extension if needed
                uriPath = uriPath.replace("smil:", "")
                if (!uriPath.endsWith(".smil"))
                    uriPath += ".smil"

                val uriPathParts = uriPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (uriPathParts.size > 1) {
                    logger.warn(
                            "Malformed URI path \"{}\". The SMIL files must be at the streaming application's root. Trying anyway...",
                            uriPath)
                }
                return distributionPath.resolve(uriPath).toFile()
            }
        }

        if (streamingUri != null) {
            relativeUri = streamingUri!!.relativize(elementUri)
            if (relativeUri !== elementUri) {
                // RTMP file

                // Get the relativized URL path
                var urlPath = relativeUri.path
                // Remove the "mp4:" tags, if any, and set the right extension if needed
                urlPath = urlPath.replace("mp4:", "")
                if (!urlPath.endsWith(".mp4"))
                    urlPath += ".mp4"

                val urlPathParts = urlPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (urlPathParts.size < 5) {
                    logger.warn(
                            format("Malformed URI %s. Must be of format .../{orgId}/{channelId}/{mediapackageId}/{elementId}/{fileName}." + " Trying URI with current orgId", elementUri))
                }
                return distributionPath.resolve(urlPath).toFile()
            }
        }
        // We have an ordinary file (not yet distributed)
        return File(getElementDirectory(channelId, mediapackage, element.identifier),
                FilenameUtils.getName(elementUri.getPath()))
    }

    /**
     * Gets the directory containing the distributed files for this mediapackage.
     *
     * @return the filesystem directory
     */
    protected fun getMediaPackageDirectory(channelId: String, mediaPackage: MediaPackage): File {
        val orgId = securityService.organization.id
        return distributionDirectory!!.toPath().resolve(Paths.get(orgId, channelId, mediaPackage.identifier.compact()))
                .toFile()
    }

    /**
     * Gets the directory containing the distributed file for this elementId.
     *
     * @return the filesystem directory
     */
    protected fun getElementDirectory(channelId: String, mediaPackage: MediaPackage, elementId: String): File {
        return File(getMediaPackageDirectory(channelId, mediaPackage), elementId)
    }

    /**
     * Gets the URI for the element to be distributed.
     *
     * @return The resulting URI after distribution
     * @throws URISyntaxException
     * if the concrete implementation tries to create a malformed uri
     */
    @Throws(URISyntaxException::class)
    protected fun getDistributionUri(channelId: String, mp: MediaPackage, element: MediaPackageElement): URI {
        val elementId = element.identifier
        val fileName = FilenameUtils.getBaseName(element.getURI().toString())
        var tag = FilenameUtils.getExtension(element.getURI().toString()) + ":"

        // removes the tag for flv files, but keeps it for all others (mp4 needs it)
        if ("flv:" == tag)
            tag = ""

        return UriBuilder.fromUri(streamingUri).path(tag + channelId).path(mp.identifier.compact()).path(elementId)
                .path(fileName).build()
    }

    /**
     * Gets the URI for the element to be distributed.
     *
     * @return The resulting URI after distributionthFromSmil
     * @throws URISyntaxException
     * if the concrete implementation tries to create a malformed uri
     */
    @Throws(URISyntaxException::class)
    protected fun getAdaptiveDistributionName(channelId: String, mp: MediaPackage, element: MediaPackageElement): String {
        val elementId = element.identifier
        val fileName = FilenameUtils.getBaseName(element.getURI().toString())
        var tag = FilenameUtils.getExtension(element.getURI().toString()) + ":"

        // removes the tag for flv files, but keeps it for all others (mp4 needs it)
        if ("flv:" == tag)
            tag = ""
        return tag + channelId + "/" + mp.identifier.compact() + "/" + elementId + "/" + fileName
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    override fun process(job: Job): String {
        var op: Operation? = null
        val operation = job.operation
        val arguments = job.arguments
        try {
            op = Operation.valueOf(operation)
            val channelId = arguments[0]
            val mediapackage = MediaPackageParser.getFromXml(arguments[1])
            val elementIds = gson.fromJson<Set<String>>(arguments[2], object : TypeToken<Set<String>>() {

            }.type)
            when (op) {
                WowzaAdaptiveStreamingDistributionService.Operation.Distribute -> {
                    val distributedElements = distributeElements(channelId, mediapackage, elementIds)
                    if (logger.isDebugEnabled && distributedElements != null) {
                        for (element in distributedElements)
                            if (element != null)
                                logger.debug("Distributed element {} with URL {}", element.identifier, element.getURI())
                    }
                    val distributedElementsList = ArrayList<MediaPackageElement>()
                    if (distributedElements != null) {
                        for (i in distributedElements.indices) {
                            if (distributedElements[i] != null) distributedElementsList.add(distributedElements[i])
                        }
                    }
                    return if (!distributedElementsList.isEmpty())
                        MediaPackageElementParser.getArrayAsXml(distributedElementsList)
                    else
                        null
                }
                WowzaAdaptiveStreamingDistributionService.Operation.Retract -> {
                    var retractedElements: Array<MediaPackageElement>? = null
                    if (distributionDirectory != null) {
                        if (streamingUri != null || adaptiveStreamingUri != null) {
                            retractedElements = retractElements(channelId, mediapackage, elementIds)
                            if (logger.isDebugEnabled && retractedElements != null) {
                                for (element in retractedElements)
                                    if (element != null)
                                        logger.debug("Retracted element {} with URL {}", element.identifier, element.getURI())
                            }
                        }
                    }
                    val retractedElementsList = ArrayList<MediaPackageElement>()
                    if (retractedElements != null) {
                        for (element in retractedElements) {
                            if (element != null) {
                                retractedElementsList.add(element)
                            }
                        }
                    }
                    return if (!retractedElementsList.isEmpty())
                        MediaPackageElementParser.getArrayAsXml(retractedElementsList)
                    else
                        null
                }
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    @Throws(IllegalStateException::class)
    private fun getElements(mediapackage: MediaPackage, elementIds: Set<String>): Set<MediaPackageElement> {
        val elements = HashSet<MediaPackageElement>()
        for (elementId in elementIds) {
            val element = mediapackage.getElementById(elementId)
            if (element != null) {
                elements.add(element)
            } else {
                logger.debug("No element " + elementId + " found in mediapackage " + mediapackage.identifier)
            }
        }
        return elements
    }

    companion object {
        /** The key in the properties file that defines the streaming formats to distribute.  */
        protected val STREAMING_FORMATS_KEY = "org.opencastproject.streaming.formats"

        /** The key in the properties file that defines the streaming url.  */
        val STREAMING_URL_KEY = "org.opencastproject.streaming.url"

        /** The key in the properties file that defines the streaming port.  */
        protected val STREAMING_PORT_KEY = "org.opencastproject.streaming.port"

        /** The key in the properties file that defines the adaptive streaming url.  */
        val ADAPTIVE_STREAMING_URL_KEY = "org.opencastproject.adaptive-streaming.url"

        /** The key in the properties file that defines the adaptive streaming port.  */
        protected val ADAPTIVE_STREAMING_PORT_KEY = "org.opencastproject.adaptive-streaming.port"

        /** The key in the properties file that specifies in which order the videos in the SMIL file should be stored  */
        protected val SMIL_ORDER_KEY = "org.opencastproject.adaptive-streaming.smil.order"

        /** One of the possible values for the order of the videos in the SMIL file  */
        protected val SMIL_ASCENDING_VALUE = "ascending"

        /** One of the possible values for the order of the videos in the SMIL file  */
        protected val SMIL_DESCENDING_VALUE = "descending"

        /** The attribute "video-bitrate" in the SMIL files  */
        protected val SMIL_ATTR_VIDEO_BITRATE = "video-bitrate"

        /** The attribute "video-width" in the SMIL files  */
        protected val SMIL_ATTR_VIDEO_WIDTH = "width"

        /** The attribute "video-height" in the SMIL files  */
        protected val SMIL_ATTR_VIDEO_HEIGHT = "height"

        /** The attribute to return for Distribution Type  */
        protected val DISTRIBUTION_TYPE = "streaming"

        /** Acceptable values for the streaming schemes  */
        protected val validStreamingSchemes: Set<String>
        protected val validAdaptiveStreamingSchemes: Set<String>
        protected val defaultProtocolPorts: Map<String, Int>

        init {
            var temp = HashSet<String>()
            temp.add("rtmp")
            temp.add("rtmps")
            validStreamingSchemes = Collections.unmodifiableSet(temp)

            temp = HashSet()
            temp.add("http")
            temp.add("https")
            validAdaptiveStreamingSchemes = Collections.unmodifiableSet(temp)

            val tempMap = HashMap<String, Int>()
            tempMap["rtmp"] = 1935
            tempMap["rtmps"] = 443
            tempMap["http"] = 80
            tempMap["https"] = 443
            defaultProtocolPorts = Collections.unmodifiableMap(tempMap)
        }

        /** Default scheme for streaming  */
        protected val DEFAULT_STREAMING_SCHEME = "rtmp"

        /** Default scheme for adaptative streaming  */
        protected val DEFAULT_ADAPTIVE_STREAMING_SCHEME = "http"

        /** Default streaming URL  */
        protected val DEFAULT_STREAMING_URL = "$DEFAULT_STREAMING_SCHEME://localhost/matterhorn-engage"

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(WowzaAdaptiveStreamingDistributionService::class.java)

        /** Receipt type  */
        val JOB_TYPE = "org.opencastproject.distribution.streaming"

        /** The load on the system introduced by creating a distribute job  */
        val DEFAULT_DISTRIBUTE_JOB_LOAD = 0.1f

        /** The load on the system introduced by creating a retract job  */
        val DEFAULT_RETRACT_JOB_LOAD = 0.1f

        /** The key to look for in the service configuration file to override the [DEFAULT_DISTRIBUTE_JOB_LOAD]  */
        val DISTRIBUTE_JOB_LOAD_KEY = "job.load.streaming.distribute"

        /** The key to look for in the service configuration file to override the [DEFAULT_RETRACT_JOB_LOAD]  */
        val RETRACT_JOB_LOAD_KEY = "job.load.streaming.retract"

        /** Default distribution directory  */
        val DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator

        private val gson = Gson()

        /**
         * Calculate a streaming URL based on input parameters
         *
         * @throws URISyntaxException
         */
        @Throws(URISyntaxException::class)
        protected fun getStreamingUrl(inputUri: String?, inputPort: String?, validSchemes: Set<String>,
                                      defaultScheme: String, defaultUri: String?): URI {

            var port: Int?
            try {
                port = Integer.parseInt(StringUtils.trimToEmpty(inputPort))
            } catch (e: NumberFormatException) {
                port = null
            }

            var uri: URI? = null
            if (StringUtils.isNotBlank(inputUri)) {
                uri = URI(inputUri!!)
            } else if (StringUtils.isNotBlank(defaultUri)) {
                uri = URI(defaultUri!!)
            } else {
                throw IllegalArgumentException("Provided streaming URL is empty.")
            }
            val uriBuilder = UriBuilder.fromUri(uri)
            val scheme = uri.scheme
            val uriPath = uri.path
            // When a URI does not have a scheme, Java parses it as if all the URI was a (relative) path
            // However, we will assume that a host was always provided, so everything before the first "/" is the host,
            // not part of the path
            if (uri.host == null) {
                uriBuilder.host(uriPath.substring(0, uriPath.indexOf("/"))).replacePath(uriPath.substring(uriPath.indexOf("/")))
            }

            if (!validSchemes.contains(scheme)) {
                if (scheme == null)
                    uriBuilder.scheme(defaultScheme)
                else
                    throw URISyntaxException(inputUri!!, "Provided URI has an illegal scheme")
            }

            if (port != null && port !== defaultProtocolPorts[uriBuilder.build().scheme]) {
                uriBuilder.port(port)
            }

            return uriBuilder.build()
        }
    }

}
