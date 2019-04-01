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
package org.opencastproject.liveschedule.impl

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.Version
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.ARecord
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.capture.admin.api.CaptureAgentStateService
import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.liveschedule.api.LiveScheduleException
import org.opencastproject.liveschedule.api.LiveScheduleService
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.metadata.dublincore.DCMIPeriod
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchService
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.series.api.SeriesService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.data.Opt
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.URIUtils
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.Enumeration
import java.util.HashSet
import java.util.Objects
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

class LiveScheduleServiceImpl : LiveScheduleService {

    private var liveStreamingUrl: String? = null
    private var streamName: String? = null
    private var streamMimeType: String? = null
    private var streamResolution: Array<String>? = null
    private var liveFlavors: Array<MediaPackageElementFlavor>? = null
    private var distributionServiceType = DEFAULT_LIVE_DISTRIBUTION_SERVICE
    private var serverUrl: String? = null
    internal val snapshotVersionCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build<String, Version>()

    /** Services  */
    private var downloadDistributionService: DownloadDistributionService? = null // to distribute episode and series catalogs
    private var searchService: SearchService? = null // to publish/retract live media package
    private var seriesService: SeriesService? = null // to get series metadata
    private var dublinCoreService: DublinCoreCatalogService? = null // to setialize dc catalogs
    private var captureAgentService: CaptureAgentStateService? = null // to get agent capabilities
    private var serviceRegistry: ServiceRegistry? = null // to create publish/retract jobs
    private var workspace: Workspace? = null // to save dc catalogs before distributing
    private var assetManager: AssetManager? = null // to get current media package
    private var authService: AuthorizationService? = null
    private var organizationService: OrganizationDirectoryService? = null

    private var jobPollingInterval = JobBarrier.DEFAULT_POLLING_INTERVAL

    /**
     * OSGi callback on component activation.
     *
     * @param context
     * the component context
     */
    fun activate(context: ComponentContext) {
        val bundleContext = context.bundleContext

        serverUrl = StringUtils.trimToNull(bundleContext.getProperty(SERVER_URL_PROPERTY))
        if (serverUrl == null)
            logger.warn("Server url was not set in '{}'", SERVER_URL_PROPERTY)
        else
            logger.info("Server url is {}", serverUrl)

        val properties = context.properties
        if (!StringUtils.isBlank(properties.get(LIVE_STREAMING_URL) as String)) {
            liveStreamingUrl = StringUtils.trimToEmpty(properties.get(LIVE_STREAMING_URL) as String)
            logger.info("Live streaming server url is {}", liveStreamingUrl)
        } else {
            logger.info("Live streaming url not set in '{}'. Streaming urls must be provided by capture agent properties.",
                    LIVE_STREAMING_URL)
        }

        if (!StringUtils.isBlank(properties.get(LIVE_STREAM_NAME) as String)) {
            streamName = StringUtils.trimToEmpty(properties.get(LIVE_STREAM_NAME) as String)
        } else {
            streamName = DEFAULT_STREAM_NAME
        }

        if (!StringUtils.isBlank(properties.get(LIVE_STREAM_MIME_TYPE) as String)) {
            streamMimeType = StringUtils.trimToEmpty(properties.get(LIVE_STREAM_MIME_TYPE) as String)
        } else {
            streamMimeType = DEFAULT_STREAM_MIME_TYPE
        }

        var resolution: String? = null
        if (!StringUtils.isBlank(properties.get(LIVE_STREAM_RESOLUTION) as String)) {
            resolution = StringUtils.trimToEmpty(properties.get(LIVE_STREAM_RESOLUTION) as String)
        } else {
            resolution = DEFAULT_STREAM_RESOLUTION
        }
        streamResolution = resolution!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        var flavors: String? = null
        if (!StringUtils.isBlank(properties.get(LIVE_TARGET_FLAVORS) as String)) {
            flavors = StringUtils.trimToEmpty(properties.get(LIVE_TARGET_FLAVORS) as String)
        } else {
            flavors = DEFAULT_LIVE_TARGET_FLAVORS
        }
        val flavorArray = StringUtils.split(flavors, ",")
        liveFlavors = arrayOfNulls(flavorArray.size)
        var i = 0
        for (f in flavorArray)
            liveFlavors[i++] = MediaPackageElementFlavor.Companion.parseFlavor(f)

        if (!StringUtils.isBlank(properties.get(LIVE_DISTRIBUTION_SERVICE) as String)) {
            distributionServiceType = StringUtils.trimToEmpty(properties.get(LIVE_DISTRIBUTION_SERVICE) as String)
        }

        logger.info(
                "Configured live stream name: {}, mime type: {}, resolution: {}, target flavors: {}, distribution service: {}",
                streamName, streamMimeType, resolution, flavors, distributionServiceType)
    }

    @Throws(LiveScheduleException::class)
    override fun createOrUpdateLiveEvent(mpId: String, episodeDC: DublinCoreCatalog): Boolean {
        val mp = getMediaPackageFromSearch(mpId)
        if (mp == null) {
            // Check if capture not over. We have to check because we may get a notification for past events if
            // the admin ui index is rebuilt
            val period = EncodingSchemeUtils.decodeMandatoryPeriod(episodeDC.getFirst(DublinCore.PROPERTY_TEMPORAL)!!)
            if (period.end!!.time <= System.currentTimeMillis()) {
                logger.info("Live media package {} not created in search index because event is already past (end date: {})",
                        mpId, period.end)
                return false
            }
            return createLiveEvent(mpId, episodeDC)
        } else {
            // Check if the media package found in the search index is live. We have to check because we may get a
            // notification for past events if the admin ui index is rebuilt
            if (!isLive(mp)) {
                logger.info("Media package {} is in search index but not live so not updating it.", mpId)
                return false
            }
            return updateLiveEvent(mp, episodeDC)
        }
    }

    @Throws(LiveScheduleException::class)
    override fun deleteLiveEvent(mpId: String): Boolean {
        val mp = getMediaPackageFromSearch(mpId)
        if (mp == null) {
            logger.debug("Live media package {} not found in search index", mpId)
            return false
        } else {
            if (!isLive(mp)) {
                logger.info("Media package {} is not live. Not retracting.", mpId)
                return false
            }
            return retractLiveEvent(mp)
        }
    }

    @Throws(LiveScheduleException::class)
    override fun updateLiveEventAcl(mpId: String, acl: AccessControlList): Boolean {
        val previousMp = getMediaPackageFromSearch(mpId)
        if (previousMp != null) {
            if (!isLive(previousMp)) {
                logger.info("Media package {} is not live. Not updating acl.", mpId)
                return false
            }
            // Replace and distribute acl, this creates new mp
            val newMp = replaceAndDistributeAcl(previousMp, acl)
            // Publish mp to engage search index
            publish(newMp)
            // Don't leave garbage there!
            retractPreviousElements(previousMp, newMp)
            logger.info("Updated live acl for media package {}", newMp)
            return true
        }
        return false
    }

    @Throws(LiveScheduleException::class)
    internal fun createLiveEvent(mpId: String, episodeDC: DublinCoreCatalog): Boolean {
        try {
            logger.info("Creating live media package {}", mpId)
            // Get latest mp from the asset manager
            val snapshot = getSnapshot(mpId)
            // Temporary mp
            val tempMp = snapshot.mediaPackage.clone() as MediaPackage
            // Set duration (used by live tracks)
            setDuration(tempMp, episodeDC)
            // Add live tracks to media package
            addLiveTracks(tempMp, episodeDC.getFirst(DublinCore.PROPERTY_SPATIAL))
            // Add and distribute catalogs/acl, this creates a new mp object
            val mp = addAndDistributeElements(snapshot)
            // Add tracks from tempMp
            for (t in tempMp.tracks)
                mp.add(t)
            // Publish mp to engage search index
            publish(mp)
            // Add engage-live publication channel to archived mp
            var currentOrg: Organization? = null
            try {
                currentOrg = organizationService!!.getOrganization(snapshot.organizationId)
            } catch (e: NotFoundException) {
                logger.warn("Organization in snapshot not found: {}", snapshot.organizationId)
            }

            val archivedMp = snapshot.mediaPackage
            addLivePublicationChannel(currentOrg, archivedMp)
            // Take a snapshot with the publication added and put its version in our local cache
            // so that we ignore notifications for this snapshot version.
            snapshotVersionCache.put(mpId, assetManager!!.takeSnapshot(archivedMp).version)
            return true
        } catch (e: Exception) {
            throw LiveScheduleException(e)
        }

    }

    @Throws(LiveScheduleException::class)
    internal fun updateLiveEvent(previousMp: MediaPackage, episodeDC: DublinCoreCatalog): Boolean {
        // Get latest mp from the asset manager
        val snapshot = getSnapshot(previousMp.identifier.toString())
        // If the snapshot version is in our local cache, it means that this snapshot was created by us so
        // nothing to do. Note that this is just to save time; if the entry has already been deleted, the mp
        // will be compared below.
        if (snapshot.version == snapshotVersionCache.getIfPresent(previousMp.identifier.toString())) {
            logger.debug("Snapshot version {} was created by us so this change is ignored.", snapshot.version)
            return false
        }
        // Temporary mp
        val tempMp = snapshot.mediaPackage.clone() as MediaPackage
        // Set duration (used by live tracks)
        setDuration(tempMp, episodeDC)
        // Add live tracks to media package
        addLiveTracks(tempMp, episodeDC.getFirst(DublinCore.PROPERTY_SPATIAL))
        // If same mp, no need to do anything
        if (isSameMediaPackage(previousMp, tempMp)) {
            logger.debug("Live media package {} seems to be the same. Not updating.", previousMp)
            return false
        }
        logger.info("Updating live media package {}", previousMp)
        // Add and distribute catalogs/acl, this creates a new mp
        val mp = addAndDistributeElements(snapshot)
        // Add tracks from tempMp
        for (t in tempMp.tracks)
            mp.add(t)
        // Remove publication element that came with the snapshot mp
        removeLivePublicationChannel(mp)
        // Publish mp to engage search index
        publish(mp)
        // Publication channel already there so no need to add
        // Don't leave garbage there!
        retractPreviousElements(previousMp, mp)
        return true
    }

    @Throws(LiveScheduleException::class)
    internal fun retractLiveEvent(mp: MediaPackage): Boolean {
        retract(mp)

        // Get latest mp from the asset manager if there to remove the publication
        try {
            val mpId = mp.identifier.toString()
            val snapshot = getSnapshot(mpId)
            val archivedMp = snapshot.mediaPackage
            removeLivePublicationChannel(archivedMp)
            logger.debug("Removed live pub channel from archived media package {}", mp)
            // Take a snapshot with the publication removed and put its version in our local cache
            // so that we ignore notifications for this snapshot version.
            snapshotVersionCache.put(mpId, assetManager!!.takeSnapshot(archivedMp).version)
        } catch (e: LiveScheduleException) {
            // It was not found in asset manager. This is ok.
        }

        return true
    }

    @Throws(LiveScheduleException::class)
    internal fun publish(mp: MediaPackage) {
        try {
            // Add media package to the search index
            logger.info("Publishing LIVE media package {} to search index", mp)
            val publishJob = searchService!!.add(mp)
            if (!waitForStatus(publishJob)!!.isSuccess)
                throw LiveScheduleException("Live media package " + mp.identifier + " could not be published")
        } catch (e: LiveScheduleException) {
            throw e
        } catch (e: Exception) {
            throw LiveScheduleException(e)
        }

    }

    @Throws(LiveScheduleException::class)
    internal fun retract(mp: MediaPackage) {
        try {
            val jobs = ArrayList<Job>()
            val elementIds = HashSet<String>()
            // Remove media package from the search index
            val mpId = mp.identifier.compact()
            logger.info("Removing LIVE media package {} from the search index", mpId)

            jobs.add(searchService!!.delete(mpId))
            // Retract elements
            for (mpe in mp.elements) {
                if (MediaPackageElement.Type.Publication != mpe.elementType)
                    elementIds.add(mpe.identifier)
            }
            jobs.add(downloadDistributionService!!.retract(CHANNEL_ID, mp, elementIds))

            if (!waitForStatus(*jobs.toTypedArray())!!.isSuccess)
                throw LiveScheduleException("Removing live media package from search did not complete successfully")
        } catch (e: LiveScheduleException) {
            throw e
        } catch (e: Exception) {
            throw LiveScheduleException(e)
        }

    }

    /**
     * Retrieves the media package from the search index.
     *
     * @param mediaPackageId
     * the media package id
     * @return the media package in the search index or null if not there
     * @throws LiveException
     * if found many media packages with the same id
     */
    @Throws(LiveScheduleException::class)
    internal fun getMediaPackageFromSearch(mediaPackageId: String): MediaPackage? {
        // Look for the media package in the search index
        val query = SearchQuery().withId(mediaPackageId)
        val result = searchService!!.getByQuery(query)
        if (result.size() == 0L) {
            logger.debug("The search service doesn't know live mediapackage {}", mediaPackageId)
            return null
        } else if (result.size() > 1) {
            logger.warn("More than one live mediapackage with id {} returned from search service", mediaPackageId)
            throw LiveScheduleException("More than one live mediapackage with id $mediaPackageId found")
        }
        return result.items[0].mediaPackage
    }

    internal fun setDuration(mp: MediaPackage, dc: DublinCoreCatalog) {
        val period = EncodingSchemeUtils.decodeMandatoryPeriod(dc.getFirst(DublinCore.PROPERTY_TEMPORAL)!!)
        val duration = period.end!!.time - period.start!!.time
        mp.duration = duration
        logger.debug("Live media package {} has start {} and duration {}", mp.identifier, mp.date,
                mp.duration)
    }

    @Throws(LiveScheduleException::class)
    internal fun addLiveTracks(mp: MediaPackage, caName: String?) {
        val mpId = mp.identifier.compact()
        try {
            // If capture agent registered the properties:
            // capture.device.live.resolution.WIDTHxHEIGHT=COMPLETE_STREAMING_URL, use them!
            try {
                val caProps = captureAgentService!!.getAgentCapabilities(caName)
                if (caProps != null) {
                    val en = caProps.keys()
                    while (en.hasMoreElements()) {
                        val key = en.nextElement() as String
                        if (key.startsWith(CA_PROPERTY_RESOLUTION_URL_PREFIX)) {
                            val resolution = key.substring(CA_PROPERTY_RESOLUTION_URL_PREFIX.length)
                            val url = caProps.getProperty(key)
                            // Note: only one flavor is supported in this format (the default: presenter/delivery)
                            val flavor = MediaPackageElementFlavor.Companion.parseFlavor(DEFAULT_LIVE_TARGET_FLAVORS)
                            val replacedUrl = replaceVariables(mpId, caName, url, flavor, resolution)
                            mp.add(buildStreamingTrack(replacedUrl, flavor, streamMimeType, resolution, mp.duration!!))
                        }
                    }
                }
            } catch (e: NotFoundException) {
                // Capture agent not found so we can't get its properties. Assume the service configuration should
                // be used instead. Note that we can't schedule anything on a CA that has not registered so this is
                // unlikely to happen.
            }

            // Capture agent did not pass any CA_PROPERTY_RESOLUTION_URL_PREFIX property when registering
            // so use the service configuration
            if (mp.tracks.size == 0) {
                if (liveStreamingUrl == null)
                    throw LiveScheduleException(
                            "Cannot build live tracks because '$LIVE_STREAMING_URL' configuration was not set.")

                for (flavor in liveFlavors!!) {
                    for (i in streamResolution!!.indices) {
                        val uri = replaceVariables(mpId, caName, UrlSupport.concat(liveStreamingUrl!!.toString(), streamName),
                                flavor, streamResolution!![i])
                        mp.add(buildStreamingTrack(uri, flavor, streamMimeType, streamResolution!![i], mp.duration!!))
                    }
                }
            }
        } catch (e: URISyntaxException) {
            throw LiveScheduleException(e)
        }

    }

    @Throws(URISyntaxException::class)
    internal fun buildStreamingTrack(uriString: String, flavor: MediaPackageElementFlavor, mimeType: String?, resolution: String,
                                     duration: Long): Track {

        val uri = URI(uriString)

        val elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        val element = elementBuilder!!.elementFromURI(uri, MediaPackageElement.Type.Track, flavor)
        val track = element as TrackImpl

        // Set duration and mime type
        track.duration = duration
        track.isLive = true
        track.mimeType = MimeTypes.parseMimeType(mimeType!!)

        val video = VideoStreamImpl("video-" + flavor.type + "-" + flavor.subtype)
        // Set video resolution
        val dimensions = resolution.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        video.setFrameWidth(Integer.parseInt(dimensions[0]))
        video.setFrameHeight(Integer.parseInt(dimensions[1]))

        track.addStream(video)

        logger.debug("Creating live track element of flavor {}, resolution {}, and url {}",
                *arrayOf(flavor, resolution, uriString))

        return track
    }

    /**
     * Replaces variables in the live stream name. Currently, this is only prepared to handle the following: #{id} = media
     * package id, #{flavor} = type-subtype of flavor, #{caName} = capture agent name, #{resolution} = stream resolution
     */
    internal fun replaceVariables(mpId: String, caName: String?, toBeReplaced: String, flavor: MediaPackageElementFlavor,
                                  resolution: String): String {

        // Substitution pattern: any string in the form #{name}, where 'name' has only word characters: [a-zA-Z_0-9].
        val pat = Pattern.compile("#\\{(\\w+)\\}")

        val matcher = pat.matcher(toBeReplaced)
        val sb = StringBuffer()
        while (matcher.find()) {
            if (matcher.group(1) == REPLACE_ID) {
                matcher.appendReplacement(sb, mpId)
            } else if (matcher.group(1) == REPLACE_FLAVOR) {
                matcher.appendReplacement(sb, flavor.type + "-" + flavor.subtype)
            } else if (matcher.group(1) == REPLACE_CA_NAME) {
                // Taking the easy route to find the capture agent name...
                matcher.appendReplacement(sb, caName!!)
            } else if (matcher.group(1) == REPLACE_RESOLUTION) {
                // Taking the easy route to find the capture agent name...
                matcher.appendReplacement(sb, resolution)
            } // else will not replace
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private fun isLive(mp: MediaPackage): Boolean {
        val tracks = mp.tracks
        if (tracks != null)
            for (track in tracks)
                if (track.isLive)
                    return true

        return false
    }

    /*
   * public void setDublinCoreService(DublinCoreCatalogService service) { this.dublinCoreService = service; }
   *
   * public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
   */

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    private fun waitForStatus(vararg jobs: Job): JobBarrier.Result? {
        if (serviceRegistry == null)
            throw IllegalStateException("Can't wait for job status without providing a service registry first")
        val barrier = JobBarrier(null, serviceRegistry, jobPollingInterval, *jobs)
        return barrier.waitForJobs()
    }

    @Throws(LiveScheduleException::class)
    internal fun getSnapshot(mpId: String): Snapshot {
        val query = assetManager!!.createQuery()
        val result = query.select(query.snapshot()).where(query.mediaPackageId(mpId).and(query.version().isLatest))
                .run()
        if (result.size == 0L) {
            // Media package not archived?.
            throw LiveScheduleException(String.format("Unexpected error: media package %s has not been archived.", mpId))
        }
        val record = result.records.head()
        if (record.isNone) {
            // No snapshot?
            throw LiveScheduleException(String.format("Unexpected error: media package %s has not been archived.", mpId))
        }
        return record.get().snapshot.get()
    }

    @Throws(LiveScheduleException::class)
    internal fun addAndDistributeElements(snapshot: Snapshot): MediaPackage {
        try {
            val mp = snapshot.mediaPackage.clone() as MediaPackage

            val elementIds = HashSet<String>()
            // Then, add series catalog if needed
            if (StringUtils.isNotEmpty(mp.series)) {
                val catalog = seriesService!!.getSeries(mp.series)
                // Create temporary catalog and save to workspace
                mp.add(catalog)
                val uri = workspace!!.put(mp.identifier.toString(), catalog.identifier, "series.xml",
                        dublinCoreService!!.serialize(catalog))
                catalog.setURI(uri)
                catalog.checksum = null
                catalog.flavor = MediaPackageElements.SERIES
                elementIds.add(catalog.identifier)
            }

            if (mp.getCatalogs(MediaPackageElements.EPISODE).size > 0)
                elementIds.add(mp.getCatalogs(MediaPackageElements.EPISODE)[0].identifier)
            if (mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE).size > 0)
                elementIds.add(mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE)[0].identifier)

            // Distribute element(s)
            val distributionJob = downloadDistributionService!!.distribute(CHANNEL_ID, mp, elementIds, false)
            if (!waitForStatus(distributionJob)!!.isSuccess)
                throw LiveScheduleException(
                        "Element(s) for live media package " + mp.identifier + " could not be distributed")

            for (id in elementIds) {
                val e = mp.getElementById(id)
                // Cleanup workspace/wfr
                mp.remove(e)
                workspace!!.delete(e.getURI())
            }

            // Add distributed element(s) to mp
            for (mpe in MediaPackageElementParser
                    .getArrayFromXml(distributionJob.payload))
                mp.add(mpe)

            return mp
        } catch (e: LiveScheduleException) {
            throw e
        } catch (e: Exception) {
            throw LiveScheduleException(e)
        }

    }

    @Throws(LiveScheduleException::class)
    internal fun replaceAndDistributeAcl(previousMp: MediaPackage, acl: AccessControlList): MediaPackage {
        try {
            // This is the mp from the search index
            val mp = previousMp.clone() as MediaPackage

            // Remove previous Acl from the mp
            var atts = mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE)
            if (atts.size > 0)
                mp.remove(atts[0])

            // Attach current ACL to mp, acl will be created in the ws/wfr
            authService!!.setAcl(mp, AclScope.Episode, acl)
            atts = mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE)
            if (atts.size > 0) {
                val aclId = atts[0].identifier
                // Distribute new acl
                val distributionJob = downloadDistributionService!!.distribute(CHANNEL_ID, mp, aclId, false)
                if (!waitForStatus(distributionJob)!!.isSuccess)
                    throw LiveScheduleException(
                            "Acl for live media package " + mp.identifier + " could not be distributed")

                val e = mp.getElementById(aclId)
                // Cleanup workspace/wfr
                mp.remove(e)
                workspace!!.delete(e.getURI())

                // Add distributed acl to mp
                mp.add(MediaPackageElementParser.getFromXml(distributionJob.payload))
            }
            return mp
        } catch (e: LiveScheduleException) {
            throw e
        } catch (e: Exception) {
            throw LiveScheduleException(e)
        }

    }

    @Throws(LiveScheduleException::class)
    internal fun addLivePublicationChannel(currentOrg: Organization?, mp: MediaPackage) {
        logger.debug("Adding live channel publication element to media package {}", mp)
        var engageUrlString: String? = null
        if (currentOrg != null) {
            engageUrlString = StringUtils.trimToNull(currentOrg.properties[ENGAGE_URL_PROPERTY])
        }
        if (engageUrlString == null) {
            engageUrlString = serverUrl
            logger.info(
                    "Using 'server.url' as a fallback for the non-existing organization level key '{}' for the publication url",
                    ENGAGE_URL_PROPERTY)
        }

        try {
            // Create new distribution element
            val engageUri = URIUtils.resolve(URI(engageUrlString!!), PLAYER_PATH + mp.identifier.compact())
            val publicationElement = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_ID, engageUri,
                    MimeTypes.parseMimeType("text/html"))
            mp.add(publicationElement)
        } catch (e: URISyntaxException) {
            throw LiveScheduleException(e)
        }

    }

    internal fun removeLivePublicationChannel(mp: MediaPackage) {
        // Remove publication element
        val publications = mp.publications
        if (publications != null) {
            for (publication in publications) {
                if (CHANNEL_ID.equals(publication.channel))
                    mp.remove(publication)
            }
        }
    }

    private fun isSameArray(previous: Array<String>, current: Array<String>): Boolean {
        val previousSet = HashSet(Arrays.asList(*previous))
        val currentSet = HashSet(Arrays.asList(*current))
        return previousSet == currentSet
    }

    private fun isSameTrackArray(previous: Array<Track>, current: Array<Track>): Boolean {
        val previousTracks = HashSet(Arrays.asList(*previous))
        val currentTracks = HashSet(Arrays.asList(*current))
        if (previousTracks.size != currentTracks.size)
            return false
        for (tp in previousTracks) {
            val it = currentTracks.iterator()
            while (it.hasNext()) {
                val tc = it.next()
                if (tp.getURI().equals(tc.getURI()) && tp.duration == tc.duration) {
                    currentTracks.remove(tc)
                    break
                }
            }
        }
        return if (currentTracks.size > 0) false else true

    }

    @Throws(LiveScheduleException::class)
    internal fun isSameMediaPackage(previous: MediaPackage, current: MediaPackage): Boolean {
        return (previous.title == current.title
                && previous.language == current.language
                && previous.series == current.series
                && previous.seriesTitle == current.seriesTitle
                && previous.duration == current.duration
                && previous.date == current.date
                && isSameArray(previous.creators, current.creators)
                && isSameArray(previous.contributors, current.contributors)
                && isSameArray(previous.subjects, current.subjects)
                && isSameTrackArray(previous.tracks, current.tracks))
    }

    @Throws(LiveScheduleException::class)
    internal fun retractPreviousElements(previousMp: MediaPackage, newMp: MediaPackage) {
        try {
            // Now can retract elements from previous publish. Before creating a retraction
            // job, check if the element url is still used by the new media package.
            val elementIds = HashSet<String>()
            for (element in previousMp.elements) {
                // We don't retract tracks because they are just live links
                if (Track.TYPE != element.elementType) {
                    var canBeDeleted = true
                    for (newElement in newMp.elements) {
                        if (element.getURI().equals(newElement.getURI())) {
                            logger.debug(
                                    "Not retracting element {} with URI {} from download distribution because it is still used by updated live media package",
                                    element.identifier, element.getURI())
                            canBeDeleted = false
                            break
                        }
                    }
                    if (canBeDeleted)
                        elementIds.add(element.identifier)
                }
            }
            if (elementIds.size > 0) {
                val job = downloadDistributionService!!.retract(CHANNEL_ID, previousMp, elementIds)
                // Wait for retraction to finish
                if (!waitForStatus(job)!!.isSuccess)
                    logger.warn("One of the download retract jobs did not complete successfully")
                else
                    logger.debug("Retraction of previously published elements complete")
            }
        } catch (e: DistributionException) {
            throw LiveScheduleException(e)
        }

    }

    // === Set by OSGI - begin
    fun setDublinCoreService(service: DublinCoreCatalogService) {
        this.dublinCoreService = service
    }

    fun setSearchService(service: SearchService) {
        this.searchService = service
    }

    fun setSeriesService(service: SeriesService) {
        this.seriesService = service
    }

    fun setServiceRegistry(service: ServiceRegistry) {
        this.serviceRegistry = service
    }

    fun setCaptureAgentService(service: CaptureAgentStateService) {
        this.captureAgentService = service
    }

    fun setDownloadDistributionService(service: DownloadDistributionService) {
        if (distributionServiceType.equals(service.distributionType, ignoreCase = true))
            this.downloadDistributionService = service
    }

    fun setWorkspace(ws: Workspace) {
        this.workspace = ws
    }

    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    fun setAuthorizationService(service: AuthorizationService) {
        this.authService = service
    }

    fun setOrganizationService(service: OrganizationDirectoryService) {
        this.organizationService = service
    }
    // === Set by OSGI - end

    // === Used by unit tests - begin
    internal fun setJobPollingInterval(jobPollingInterval: Long) {
        this.jobPollingInterval = jobPollingInterval
    }

    companion object {

        // TODO Implement updated() so that change in configuration can be dynamically loaded.

        /** The server url property  */
        internal val SERVER_URL_PROPERTY = "org.opencastproject.server.url"
        /** The engage base url property  */
        internal val ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url"
        /** The default path to the player  */
        internal val PLAYER_PATH = "/play/"

        /** Default values for configuration options  */
        private val DEFAULT_STREAM_MIME_TYPE = "video/mp4"
        private val DEFAULT_STREAM_RESOLUTION = "1920x1080"
        private val DEFAULT_STREAM_NAME = "live-stream"
        private val DEFAULT_LIVE_TARGET_FLAVORS = "presenter/delivery"
        internal val DEFAULT_LIVE_DISTRIBUTION_SERVICE = "download"

        // If the capture agent registered this property, we expect to get a resolution and
        // a url in the following format:
        // capture.device.live.resolution.WIDTHxHEIGHT=COMPLETE_STREAMING_URL e.g.
        // capture.device.live.resolution.960x270=rtmp://cp398121.live.edgefcs.net/live/dev-epiphan005-2-presenter-delivery.stream-960x270_1_200@355694
        val CA_PROPERTY_RESOLUTION_URL_PREFIX = "capture.device.live.resolution."

        /** Variables that can be replaced in stream name  */
        val REPLACE_ID = "id"
        val REPLACE_FLAVOR = "flavor"
        val REPLACE_CA_NAME = "caName"
        val REPLACE_RESOLUTION = "resolution"

        val LIVE_STREAMING_URL = "live.streamingUrl"
        val LIVE_STREAM_NAME = "live.streamName"
        val LIVE_STREAM_MIME_TYPE = "live.mimeType"
        val LIVE_STREAM_RESOLUTION = "live.resolution"
        val LIVE_TARGET_FLAVORS = "live.targetFlavors"
        val LIVE_DISTRIBUTION_SERVICE = "live.distributionService"

        /** The logger  */
        private val logger = LoggerFactory.getLogger(LiveScheduleServiceImpl::class.java)
    }
    // === Used by unit tests - end
}
