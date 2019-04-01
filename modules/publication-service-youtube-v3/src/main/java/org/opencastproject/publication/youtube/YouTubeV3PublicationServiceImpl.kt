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

package org.opencastproject.publication.youtube

import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.publication.api.YouTubePublicationService
import org.opencastproject.publication.youtube.auth.ClientCredentials
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.XProperties
import org.opencastproject.workspace.api.Workspace

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.Video

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.Dictionary
import java.util.UUID

/**
 * Publishes media to a Youtube play list.
 */
class YouTubeV3PublicationServiceImpl
/**
 * Creates a new instance of the youtube publication service.
 */
@Throws(Exception::class)
internal constructor(
        /** Youtube configuration instance  */
        private val youTubeService: YouTubeAPIVersion3Service) : AbstractJobProducer(JOB_TYPE), YouTubePublicationService, ManagedService {

    /** The load on the system introduced by creating a publish job  */
    private var youtubePublishJobLoad = DEFAULT_YOUTUBE_PUBLISH_JOB_LOAD

    /** The load on the system introduced by creating a retract job  */
    private var youtubeRetractJobLoad = DEFAULT_YOUTUBE_RETRACT_JOB_LOAD

    /** workspace instance  */
    protected var workspace: Workspace? = null

    /** The remote service registry  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Callback for the OSGi environment to set the service registry reference.
     *
     * @param serviceRegistry
     * the service registry
     */
    protected override var serviceRegistry: ServiceRegistry? = null
        set

    /** The organization directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getOrganizationDirectoryService
     */
    /**
     * Sets a reference to the organization directory service.
     *
     * @param organizationDirectory
     * the organization directory
     */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    /** The user directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getUserDirectoryService
     */
    /**
     * Callback for setting the user directory service.
     *
     * @param userDirectoryService
     * the userDirectoryService to set
     */
    override var userDirectoryService: UserDirectoryService? = null
        set

    /** The security service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getSecurityService
     */
    /**
     * Callback for setting the security service.
     *
     * @param securityService
     * the securityService to set
     */
    override var securityService: SecurityService? = null
        set

    private var enabled = false

    /**
     * The default playlist to publish to, in case there is not enough information in the mediapackage to find a playlist
     */
    private var defaultPlaylist: String? = null

    private var makeVideosPrivate: Boolean = false

    private var tags: Array<String>? = null

    private val properties = XProperties()

    /**
     * The maximum length of a Recording or Series title.
     * A value of zero will be treated as no limit
     */
    private var maxFieldLength: Int = 0

    internal val isMaxFieldLengthSet: Boolean
        get() = maxFieldLength != 0

    /** List of available operations on jobs  */
    private enum class Operation {
        Publish, Retract
    }

    /**
     * Creates a new instance of the youtube publication service.
     */
    @Throws(Exception::class)
    constructor() : this(YouTubeAPIVersion3ServiceImpl()) {
    }

    /**
     * Called when service activates. Defined in OSGi resource file.
     */
    @Synchronized
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        properties.bundleContext = cc.bundleContext
    }

    @Throws(ConfigurationException::class)
    override fun updated(props: Dictionary<*, *>) {
        properties.merge(props)

        enabled = java.lang.Boolean.valueOf(properties[YOUTUBE_ENABLED_KEY] as String)

        val dataStore = YouTubeUtils.get(properties, YouTubeKey.credentialDatastore)

        try {
            if (enabled) {
                val clientCredentials = ClientCredentials()
                clientCredentials.credentialDatastore = dataStore
                val path = YouTubeUtils.get(properties, YouTubeKey.clientSecretsV3)
                val secretsFile = File(path!!)
                if (secretsFile.exists() && !secretsFile.isDirectory) {
                    clientCredentials.clientSecrets = secretsFile
                    clientCredentials.dataStoreDirectory = YouTubeUtils.get(properties, YouTubeKey.dataStore)
                    //
                    youTubeService.initialize(clientCredentials)
                    //
                    tags = StringUtils.split(YouTubeUtils.get(properties, YouTubeKey.keywords), ',')
                    defaultPlaylist = YouTubeUtils.get(properties, YouTubeKey.defaultPlaylist)
                    makeVideosPrivate = StringUtils
                            .containsIgnoreCase(YouTubeUtils.get(properties, YouTubeKey.makeVideosPrivate), "true")
                    defaultMaxFieldLength(YouTubeUtils.get(properties, YouTubeKey.maxFieldLength, false))
                } else {
                    logger.warn("Client information file does not exist: $path")
                }
            } else {
                logger.info("YouTube v3 publication service is disabled")
            }
        } catch (e: Exception) {
            throw ConfigurationException("Failed to load YouTube v3 properties", dataStore, e)
        }

        youtubePublishJobLoad = LoadUtil.getConfiguredLoadValue(properties, YOUTUBE_PUBLISH_LOAD_KEY, DEFAULT_YOUTUBE_PUBLISH_JOB_LOAD, serviceRegistry!!)
        youtubeRetractJobLoad = LoadUtil.getConfiguredLoadValue(properties, YOUTUBE_RETRACT_LOAD_KEY, DEFAULT_YOUTUBE_RETRACT_JOB_LOAD, serviceRegistry!!)
    }

    @Throws(PublicationException::class)
    override fun publish(mediaPackage: MediaPackage, track: Track): Job {
        return if (mediaPackage.contains(track)) {
            try {
                val args = Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), track.identifier)
                serviceRegistry!!.createJob(JOB_TYPE, Operation.Publish.toString(), args, youtubePublishJobLoad)
            } catch (e: ServiceRegistryException) {
                throw PublicationException("Unable to create a job for track: $track", e)
            }

        } else {
            throw IllegalArgumentException("Mediapackage does not contain track " + track.identifier)
        }

    }

    /**
     * Publishes the element to the publication channel and returns a reference to the published version of the element.
     *
     * @param job
     * the associated job
     * @param mediaPackage
     * the mediapackage
     * @param elementId
     * the mediapackage element id to publish
     * @return the published element
     * @throws PublicationException
     * if publication fails
     */
    @Throws(PublicationException::class)
    private fun publish(job: Job, mediaPackage: MediaPackage?, elementId: String?): Publication? {
        if (mediaPackage == null) {
            throw IllegalArgumentException("Mediapackage must be specified")
        } else if (elementId == null) {
            throw IllegalArgumentException("Mediapackage ID must be specified")
        }
        val element = mediaPackage.getElementById(elementId)
                ?: throw IllegalArgumentException("Mediapackage element must be specified")
        if (element.identifier == null) {
            throw IllegalArgumentException("Mediapackage element must have an identifier")
        }
        if (element.mimeType.toString().matches("text/xml".toRegex())) {
            throw IllegalArgumentException("Mediapackage element cannot be XML")
        }
        try {
            // create context strategy for publication
            val c = YouTubePublicationAdapter(mediaPackage, workspace)
            val file = workspace!!.get(element.getURI())
            val episodeName = c.episodeName
            val operationProgressListener = UploadProgressListener(mediaPackage, file)
            val privacyStatus = if (makeVideosPrivate) "private" else "public"
            val videoUpload = VideoUpload(truncateTitleToMaxFieldLength(episodeName, false), c.episodeDescription, privacyStatus, file, operationProgressListener, *tags)
            val video = youTubeService.addVideoToMyChannel(videoUpload)
            val timeoutMinutes = 60
            val startUploadMilliseconds = Date().time
            while (!operationProgressListener.isComplete) {
                Thread.sleep(POLL_MILLISECONDS)
                val howLongWaitingMinutes = (Date().time - startUploadMilliseconds) / 60000
                if (howLongWaitingMinutes > timeoutMinutes) {
                    throw PublicationException("Upload to YouTube exceeded $timeoutMinutes minutes for episode $episodeName")
                }
            }
            var playlistName: String? = StringUtils.trimToNull(truncateTitleToMaxFieldLength(mediaPackage.seriesTitle, true))
            playlistName = playlistName ?: this.defaultPlaylist
            val playlist: Playlist
            val existingPlaylist = youTubeService.getMyPlaylistByTitle(playlistName)
            if (existingPlaylist == null) {
                playlist = youTubeService.createPlaylist(playlistName, c.contextDescription, mediaPackage.series)
            } else {
                playlist = existingPlaylist
            }
            youTubeService.addPlaylistItem(playlist.id, video.id)
            // Create new publication element
            val url = URL("http://www.youtube.com/watch?v=" + video.id)
            return PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_NAME, url.toURI(), MimeTypes.parseMimeType(MIME_TYPE))
        } catch (e: Exception) {
            logger.error("failed publishing to Youtube", e)
            logger.warn("Error publishing {}, {}", element, e.message)
            if (e is PublicationException) {
                throw e
            } else {
                throw PublicationException("YouTube publish failed on job: " + ToStringBuilder.reflectionToString(job, ToStringStyle.MULTI_LINE_STYLE), e)
            }
        }

    }

    @Throws(PublicationException::class)
    override fun retract(mediaPackage: MediaPackage): Job {
        if (mediaPackage == null) {
            throw IllegalArgumentException("Mediapackage must be specified")
        }
        try {
            val arguments = ArrayList<String>()
            arguments.add(MediaPackageParser.getAsXml(mediaPackage))
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Retract.toString(), arguments, youtubeRetractJobLoad)
        } catch (e: ServiceRegistryException) {
            throw PublicationException("Unable to create a job", e)
        }

    }

    /**
     * Retracts the mediapackage from YouTube.
     *
     * @param job
     * the associated job
     * @param mediaPackage
     * the mediapackage
     * @throws PublicationException
     * if retract did not work
     */
    @Throws(PublicationException::class)
    private fun retract(job: Job, mediaPackage: MediaPackage): Publication? {
        logger.info("Retract video from YouTube: {}", mediaPackage)
        var youtube: Publication? = null
        for (publication in mediaPackage.publications) {
            if (CHANNEL_NAME == publication.channel) {
                youtube = publication
                break
            }
        }
        if (youtube == null) {
            return null
        }
        val contextStrategy = YouTubePublicationAdapter(mediaPackage, workspace)
        val episodeName = contextStrategy.episodeName
        try {
            retract(mediaPackage.seriesTitle, episodeName)
        } catch (e: Exception) {
            logger.error("Failure retracting YouTube media {}", e.message)
            throw PublicationException("YouTube media retract failed on job: " + ToStringBuilder.reflectionToString(job, ToStringStyle.MULTI_LINE_STYLE), e)
        }

        return youtube
    }

    @Throws(Exception::class)
    private fun retract(seriesTitle: String?, episodeName: String?) {
        val items = youTubeService.searchMyVideos(truncateTitleToMaxFieldLength(episodeName, false), null, 1).items
        if (!items.isEmpty()) {
            val videoId = items[0].id.videoId
            if (seriesTitle != null) {
                val playlist = youTubeService.getMyPlaylistByTitle(truncateTitleToMaxFieldLength(seriesTitle, true))
                youTubeService.removeVideoFromPlaylist(playlist.id, videoId)
            }
            youTubeService.removeMyVideo(videoId)
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
        try {
            op = Operation.valueOf(job.operation)
            val arguments = job.arguments
            val mediapackage = MediaPackageParser.getFromXml(arguments[0])
            when (op) {
                YouTubeV3PublicationServiceImpl.Operation.Publish -> {
                    val publicationElement = publish(job, mediapackage, arguments[1])
                    return if (publicationElement == null) null else MediaPackageElementParser.getAsXml(publicationElement)
                }
                YouTubeV3PublicationServiceImpl.Operation.Retract -> {
                    val retractedElement = retract(job, mediapackage)
                    return if (retractedElement == null) null else MediaPackageElementParser.getAsXml(retractedElement)
                }
                else -> throw IllegalStateException("Don't know how to handle operation '" + job.operation + "'")
            }
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    /**
     * Callback for the OSGi environment to set the workspace reference.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    private fun truncateTitleToMaxFieldLength(title: String?, tolerateNull: Boolean): String? {
        if (StringUtils.isBlank(title) && !tolerateNull) {
            throw IllegalArgumentException("Title fields cannot be null, empty, or whitespace")
        }
        return if (isMaxFieldLengthSet && title != null) {
            StringUtils.left(title, maxFieldLength)
        } else {
            title
        }
    }

    private fun defaultMaxFieldLength(maxFieldLength: String?) {
        if (StringUtils.isBlank(maxFieldLength)) {
            this.maxFieldLength = 0
        } else {
            try {
                this.maxFieldLength = Integer.parseInt(maxFieldLength!!)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("maxFieldLength must be an integer")
            }

            if (this.maxFieldLength <= 0) {
                throw IllegalArgumentException("maxFieldLength must be greater than zero")
            }
        }
    }

    companion object {

        /** The load on the system introduced by creating a publish job  */
        val DEFAULT_YOUTUBE_PUBLISH_JOB_LOAD = 0.1f

        /** The load on the system introduced by creating a retract job  */
        val DEFAULT_YOUTUBE_RETRACT_JOB_LOAD = 0.1f

        /** The key to look for in the service configuration file to override the [DEFAULT_YOUTUBE_PUBLISH_JOB_LOAD]  */
        val YOUTUBE_PUBLISH_LOAD_KEY = "job.load.youtube.publish"

        /** The key to look for in the service configuration file to override the [DEFAULT_YOUTUBE_RETRACT_JOB_LOAD]  */
        val YOUTUBE_RETRACT_LOAD_KEY = "job.load.youtube.retract"

        val YOUTUBE_ENABLED_KEY = "org.opencastproject.publication.youtube.enabled"

        /** Time to wait between polling for status (milliseconds.)  */
        private val POLL_MILLISECONDS = 30L * 1000L

        /** The channel name  */
        private val CHANNEL_NAME = "youtube"

        /** logger instance  */
        private val logger = LoggerFactory.getLogger(YouTubeV3PublicationServiceImpl::class.java)

        /** The mime-type of the published element  */
        private val MIME_TYPE = "text/html"
    }

}
