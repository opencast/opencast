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

package org.opencastproject.distribution.streaming

import java.lang.String.format
import org.opencastproject.util.OsgiUtil.getOptContextProperty
import org.opencastproject.util.PathSupport.path
import org.opencastproject.util.RequireUtil.notNull
import org.opencastproject.util.UrlSupport.concat
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some

import org.opencastproject.distribution.api.AbstractDistributionService
import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.StreamingDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.FileSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.OsgiUtil
import org.opencastproject.util.RequireUtil
import org.opencastproject.util.data.Option

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.DirectoryStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.HashSet

/**
 * Distributes media to the local media delivery directory.
 */
/** Creates a new instance of the streaming distribution service.  */
class StreamingDistributionServiceImpl : AbstractDistributionService(JOB_TYPE), ManagedService, StreamingDistributionService {

    /** The load on the system introduced by creating a distribute job  */
    private var distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD

    /** The load on the system introduced by creating a retract job  */
    private var retractJobLoad = DEFAULT_RETRACT_JOB_LOAD

    private var locations = none()

    override val distributionType: String
        get() = this.distributionChannel

    /** List of available operations on jobs  */
    private enum class Operation {
        Distribute, Retract
    }

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        // Get the configured streaming and server URLs
        if (cc != null) {
            for (streamingUrl in getOptContextProperty(cc, "org.opencastproject.streaming.url")) {
                for (distributionDirectoryPath in getOptContextProperty(cc,
                        "org.opencastproject.streaming.directory")) {
                    val distributionDirectory = File(distributionDirectoryPath)
                    if (!distributionDirectory.isDirectory) {
                        try {
                            FileUtils.forceMkdir(distributionDirectory)
                        } catch (e: IOException) {
                            throw IllegalStateException("Distribution directory does not exist and can't be created", e)
                        }

                    }
                    val compatibility = StringUtils
                            .trimToNull(cc.bundleContext.getProperty("org.opencastproject.streaming.flvcompatibility"))
                    var flvCompatibilityMode = false
                    if (compatibility != null) {
                        flvCompatibilityMode = java.lang.Boolean.parseBoolean(compatibility)
                        logger.info("Streaming distribution is using FLV compatibility mode")
                    }
                    locations = some(Locations(URI.create(streamingUrl), distributionDirectory, flvCompatibilityMode))
                    logger.info("Streaming url is {}", streamingUrl)
                    logger.info("Streaming distribution directory is {}", distributionDirectory)
                    return
                }
                logger.info("No streaming distribution directory configured (org.opencastproject.streaming.directory)")
            }
            logger.info("No streaming url configured (org.opencastproject.streaming.url)")
        }
        this.distributionChannel = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE)
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

    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>): Job {
        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")
        try {
            return serviceRegistry!!.createJob(
                    JOB_TYPE,
                    Operation.Distribute.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds)), distributeJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

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
            if (MediaPackageElement.Type.Track == element.elementType) {
                // Streaming servers only deal with tracks
                val distributedElement = distributeElement(channelId, mediapackage, element.identifier)
                distributedElements.add(distributedElement)
            } else {
                logger.warn("Skipping {} {} for distribution to the streaming server (only media tracks supported)",
                        element.elementType.toString().toLowerCase(), element.identifier)
            }
        }
        return distributedElements.toTypedArray()
    }


    /**
     * Distribute a Mediapackage element to the download distribution service.
     *
     * @param mp
     * The media package that contains the element to distribute.
     * @param mpeId
     * The id of the element that should be distributed contained within the media package.
     * @return A reference to the MediaPackageElement that has been distributed.
     * @throws DistributionException
     * Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
     * cannot be copied or another unexpected exception occurs.
     */

    @Throws(DistributionException::class)
    private fun distributeElement(channelId: String, mp: MediaPackage, mpeId: String): MediaPackageElement {
        RequireUtil.notNull(channelId, "channelId")
        RequireUtil.notNull(mp, "mp")
        RequireUtil.notNull(mpeId, "mpeId")
        //
        val element = mp.getElementById(mpeId)
                ?: throw IllegalStateException("No element $mpeId found in media package")
        // Make sure the element exists
        try {
            var source: File
            try {
                source = workspace!!.get(element.getURI())
            } catch (e: NotFoundException) {
                throw DistributionException("Unable to find " + element.getURI() + " in the workspace", e)
            } catch (e: IOException) {
                throw DistributionException("Error loading " + element.getURI() + " from the workspace", e)
            }

            // Try to find a duplicated element source
            try {
                source = findDuplicatedElementSource(source, mp.identifier.compact())
            } catch (e: IOException) {
                logger.warn("Unable to find duplicated source {}: {}", source, ExceptionUtils.getMessage(e))
            }

            val destination = locations.get().createDistributionFile(securityService!!.organization.id,
                    channelId, mp.identifier.compact(), element.identifier, element.getURI())

            if (destination != source) {
                // Put the file in place if sourcesfile differs destinationfile
                try {
                    FileUtils.forceMkdir(destination.getParentFile())
                } catch (e: IOException) {
                    throw DistributionException("Unable to create " + destination.getParentFile(), e)
                }

                logger.info("Distributing {} to {}", mpeId, destination)

                try {
                    FileSupport.link(source, destination, true)
                } catch (e: IOException) {
                    throw DistributionException("Unable to copy $source to $destination", e)
                }

            }
            // Create a representation of the distributed file in the mediapackage
            val distributedElement = element.clone() as MediaPackageElement
            distributedElement.setURI(locations.get().createDistributionUri(securityService!!.organization.id,
                    channelId, mp.identifier.compact(), element.identifier, element.getURI()))
            distributedElement.identifier = null
            (distributedElement as TrackImpl).setTransport(TrackImpl.StreamingProtocol.RTMP)
            logger.info("Finished distribution of {}", element)
            return distributedElement
        } catch (e: Exception) {
            logger.warn("Error distributing $element", e)
            if (e is DistributionException) {
                throw e
            } else {
                throw DistributionException(e)
            }
        }

    }

    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediapackage: MediaPackage, elementId: String): Job {
        val elementIds = HashSet()
        elementIds.add(elementId)
        return retract(channelId, mediapackage, elementIds)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.DistributionService.retract
     */
    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): Job {
        if (locations.isNone)
            return null

        RequireUtil.notNull(mediaPackage, "mediaPackage")
        RequireUtil.notNull(elementIds, "elementId")
        RequireUtil.notNull(channelId, "channelId")
        //
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Retract.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), gson.toJson(elementIds)),
                    retractJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    @Throws(DistributionException::class)
    override fun distributeSync(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement> {
        var job: Job? = null
        try {
            job = serviceRegistry!!
                    .createJob(
                            JOB_TYPE, Operation.Distribute.toString(), null!!, null!!, false, distributeJobLoad)
            job.status = Job.Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val mediaPackageElements = this.distributeElements(channelId, mediapackage, elementIds)
            job.status = Job.Status.FINISHED
            return Arrays.asList(*mediaPackageElements)
        } catch (e: ServiceRegistryException) {
            throw DistributionException(e)
        } catch (e: NotFoundException) {
            throw DistributionException("Unable to update distribution job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    @Throws(DistributionException::class)
    override fun retractSync(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement> {
        var job: Job? = null
        try {
            job = serviceRegistry!!
                    .createJob(
                            JOB_TYPE, Operation.Retract.toString(), null!!, null!!, false, retractJobLoad)
            job.status = Job.Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val mediaPackageElements = this.retractElements(channelId, mediaPackage, elementIds)
            job.status = Job.Status.FINISHED
            return Arrays.asList(*mediaPackageElements)
        } catch (e: ServiceRegistryException) {
            throw DistributionException(e)
        } catch (e: NotFoundException) {
            throw DistributionException("Unable to update retraction job", e)
        } finally {
            finallyUpdateJob(job)
        }
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
            val retractedElement = retractElement(channelId, mediapackage, element.identifier)
            retractedElements.add(retractedElement)
        }
        return retractedElements.toTypedArray()
    }


    /**
     * Retracts the mediapackage with the given identifier from the distribution channel.
     *
     * @param channelId
     * the channel id
     * @param mp
     * the mediapackage
     * @param mpeId
     * the element identifier
     * @return the retracted element or `null` if the element was not retracted
     */
    @Throws(DistributionException::class)
    private fun retractElement(channelId: String, mp: MediaPackage, mpeId: String): MediaPackageElement {
        RequireUtil.notNull(channelId, "channelId")
        RequireUtil.notNull(mp, "mp")
        RequireUtil.notNull(mpeId, "elementId")
        // Make sure the element exists
        val mpe = mp.getElementById(mpeId) ?: throw IllegalStateException("No element $mpeId found in media package")
        try {
            for (mpeFile in locations.get().getDistributionFileFrom(mpe.getURI())) {
                logger.info("Retracting element {} from {}", mpe, mpeFile)
                // Does the file exist? If not, the current element has not been distributed to this channel
                // or has been removed otherwise
                if (mpeFile.exists()) {
                    // Try to remove the file and - if possible - the parent folder
                    val parentDir = mpeFile.getParentFile()
                    FileUtils.forceDelete(mpeFile)
                    FileSupport.deleteHierarchyIfEmpty(File(locations.get().baseDir), parentDir)
                    logger.info("Finished retracting element {} of media package {}", mpeId, mp)
                    return mpe
                } else {
                    logger.info(format("Element %s@%s has already been removed from publication channel %s", mpeId,
                            mp.identifier, channelId))
                    return mpe
                }
            }
            // could not extract a file from the element's URI
            logger.info(format("Element %s has not been published to publication channel %s", mpe.getURI(), channelId))
            return mpe
        } catch (e: Exception) {
            logger.warn(format("Error retracting element %s of media package %s", mpeId, mp), e)
            if (e is DistributionException) {
                throw e
            } else {
                throw DistributionException(e)
            }
        }

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
                StreamingDistributionServiceImpl.Operation.Distribute -> {
                    val distributedElements = distributeElements(channelId, mediapackage, elementIds)
                    return if (distributedElements != null)
                        MediaPackageElementParser.getArrayAsXml(Arrays.asList(*distributedElements))
                    else
                        null
                }
                StreamingDistributionServiceImpl.Operation.Retract -> {
                    val retractedElements = retractElements(channelId, mediapackage, elementIds)
                    return if (retractedElements != null)
                        MediaPackageElementParser.getArrayAsXml(Arrays.asList(*retractedElements))
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

    /**
     * Try to find the same file being already distributed in one of the other channels
     *
     * @param source
     * the source file
     * @param mpId
     * the element's mediapackage id
     * @return the found duplicated file or the given source if nothing has been found
     * @throws IOException
     * if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun findDuplicatedElementSource(source: File, mpId: String): File {
        val orgId = securityService!!.organization.id
        val rootPath = File(path(locations.get().baseDir, orgId)).toPath()

        // Check if root path exists, if not you're file system has not been migrated to the new distribution service yet
        // and does not support this function
        if (!Files.exists(rootPath))
            return source

        // Find matching mediapackage directories
        val mediaPackageDirectories = ArrayList<Path>()
        Files.newDirectoryStream(rootPath).use { directoryStream ->
            for (path in directoryStream) {
                val mpDir = File(path.toFile(), mpId).toPath()
                if (Files.exists(mpDir)) {
                    mediaPackageDirectories.add(mpDir)
                }
            }
        }

        if (mediaPackageDirectories.isEmpty())
            return source

        val size = Files.size(source.toPath())

        val result = arrayOfNulls<File>(1)
        for (p in mediaPackageDirectories) {
            // Walk through found mediapackage directories to find duplicated element
            Files.walkFileTree(p, object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    // Walk through files only
                    if (Files.isDirectory(file))
                        return FileVisitResult.CONTINUE

                    // Check for same file size
                    if (size != attrs.size())
                        return FileVisitResult.CONTINUE

                    // If size less than 4096 bytes use readAllBytes method which performs better
                    if (size < 4096) {
                        if (!Arrays.equals(Files.readAllBytes(source.toPath()), Files.readAllBytes(file)))
                            return FileVisitResult.CONTINUE

                    } else {
                        // Otherwise compare file input stream
                        Files.newInputStream(source.toPath()).use { is1 ->
                            Files.newInputStream(file).use { is2 ->
                                if (!IOUtils.contentEquals(is1, is2))
                                    return FileVisitResult.CONTINUE
                            }
                        }
                    }

                    // File is equal, store file and terminate file walking
                    result[0] = file.toFile()
                    return FileVisitResult.TERMINATE
                }
            })

            // A duplicate has already been found, no further file walking is needed
            if (result[0] != null)
                break
        }

        // Return found duplicate otherwise source
        return if (result[0] != null) result[0] else source

    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        distributeJobLoad = LoadUtil.getConfiguredLoadValue(properties, DISTRIBUTE_JOB_LOAD_KEY,
                DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry!!)
        retractJobLoad = LoadUtil.getConfiguredLoadValue(properties, RETRACT_JOB_LOAD_KEY, DEFAULT_RETRACT_JOB_LOAD,
                serviceRegistry!!)
    }

    class Locations
    /**
     * @param baseUri
     * the base URL of the distributed streaming artifacts
     * @param baseDir
     * the file system base directory below which streaming distribution artifacts are stored
     */
    (baseUri: URI, baseDir: File, flvCompatibilityMode: Boolean) {
        private val baseUri: URI
        val baseDir: String

        /** Compatibility mode for nginx and maybe other streaming servers  */
        private val flvCompatibilityMode = false

        init {
            this.flvCompatibilityMode = flvCompatibilityMode
            try {
                val ensureSlash = if (baseUri.schemeSpecificPart.endsWith("/"))
                    baseUri.schemeSpecificPart
                else
                    baseUri.schemeSpecificPart + "/"
                this.baseUri = URI(baseUri.scheme, ensureSlash, null)
                this.baseDir = baseDir.absolutePath
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }

        }

        fun getBaseUri(): String {
            return baseUri.toString()
        }

        fun isDistributionUrl(mpeUrl: URI): Boolean {
            return mpeUrl.toString().startsWith(getBaseUri())
        }

        fun dropBase(mpeUrl: URI): Option<URI> {
            return if (isDistributionUrl(mpeUrl)) {
                some(baseUri.relativize(mpeUrl))
            } else {
                none()
            }
        }

        /**
         * Try to retrieve the distribution file from a distribution URI. This is the the inverse function of
         * [.createDistributionUri].
         *
         * @param mpeDistUri
         * the URI of a distributed media package element
         * @see .createDistributionUri
         * @see .createDistributionFile
         */
        fun getDistributionFileFrom(mpeDistUri: URI): Option<File> {
            // if the given URI is not a distribution URI there cannot be a corresponding file
            for (distPath in dropBase(mpeDistUri)) {
                // 0: orgId | [extension ":" ] orgId ;
                // extension = "mp4" | ...
                // 1: channelId
                // 2: mediaPackageId
                // 3: mediaPackageElementId
                // 4: fileName
                val splitUrl = distPath.toString().split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitUrl.size == 5) {
                    val split = splitUrl[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val ext: String
                    val orgId: String
                    if (split.size == 2) {
                        ext = split[0]
                        orgId = split[1]
                    } else {
                        ext = "flv"
                        orgId = split[0]
                    }
                    return some(File(path(baseDir, orgId, splitUrl[1], splitUrl[2], splitUrl[3], splitUrl[4] + "." + ext)))
                } else {
                    return none()
                }
            }
            return none()
        }

        /**
         * Create a file to distribute a media package element to.
         *
         * @param orgId
         * the id of the organization
         * @param channelId
         * the id of the distribution channel
         * @param mpId
         * the media package id
         * @param mpeId
         * the media package element id
         * @param mpeUri
         * the URI of the media package element to distribute
         * @see .createDistributionUri
         * @see .getDistributionFileFrom
         */
        fun createDistributionFile(orgId: String, channelId: String, mpId: String,
                                   mpeId: String, mpeUri: URI): File {
            for (f in getDistributionFileFrom(mpeUri)) {
                return f
            }
            return File(path(baseDir, orgId, channelId, mpId, mpeId, FilenameUtils.getName(mpeUri.toString())))
        }

        /**
         * Create a distribution URI for a media package element. This is the inverse function of
         * [.getDistributionFileFrom].
         *
         *
         * Distribution URIs look like this:
         *
         * <pre>
         * Flash video (flv)
         * rtmp://localhost/matterhorn-engage/mh_default_org/engage-player/9f411edb-edf5-4308-8df5-f9b111d9d346/bed1cdba-2d42-49b1-b78f-6c6745fb064a/Hans_Arp_1m10s
         * H.264 (mp4)
         * rtmp://localhost/matterhorn-engage/mp4:mh_default_org/engage-player/9f411edb-edf5-4308-8df5-f9b111d9d346/bd4d5a48-41a8-4362-93dc-be41aaae77f8/Hans_Arp_1m10s
        </pre> *
         *
         * @param orgId
         * the id of the organization
         * @param channelId
         * the id of the distribution channel
         * @param mpId
         * the media package id
         * @param mpeId
         * the media package element id
         * @param mpeUri
         * the URI of the media package element to distribute
         * @see .createDistributionFile
         * @see .getDistributionFileFrom
         */
        fun createDistributionUri(orgId: String, channelId: String, mpId: String, mpeId: String, mpeUri: URI): URI {
            // if the given media package element URI is already a distribution URI just return it
            if (!isDistributionUrl(mpeUri)) {
                val ext = FilenameUtils.getExtension(mpeUri.toString())
                val fileName = FilenameUtils.getBaseName(mpeUri.toString())
                var tag = "$ext:"

                // removes the tag for flv files, but keeps it for all others (mp4 needs it)
                if (flvCompatibilityMode && "flv:" == tag)
                    tag = ""

                return URI.create(concat(getBaseUri(), tag + orgId, channelId, mpId, mpeId, fileName))
            } else {
                return mpeUri
            }
        }
    }

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(StreamingDistributionServiceImpl::class.java)

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

        private val gson = Gson()
    }
}
