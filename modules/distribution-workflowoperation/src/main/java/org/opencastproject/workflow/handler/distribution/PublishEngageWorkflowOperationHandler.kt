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

package org.opencastproject.workflow.handler.distribution

import org.apache.commons.lang3.StringUtils.isBlank
import org.opencastproject.systems.OpencastConstants.SERVER_URL_PROPERTY
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.functions.Strings.toBool
import org.opencastproject.util.data.functions.Strings.trimToNone
import org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DistributionService
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageReference
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.search.api.SearchException
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchService
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.URIUtils
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import java.util.stream.Collectors

/**
 * The workflow definition for handling "engage publication" operations
 */
class PublishEngageWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The streaming distribution service  */
    private var streamingDistributionService: DistributionService? = null

    /** The download distribution service  */
    private var downloadDistributionService: DownloadDistributionService? = null

    /** The search service  */
    private var searchService: SearchService? = null

    /** The server url  */
    private var serverUrl: URL? = null

    /** To get the tenant path to the player URL  */
    private var securityService: SecurityService? = null

    private var organizationDirectoryService: OrganizationDirectoryService? = null

    /** Whether to distribute to streaming server  */
    private var distributeStreaming = false

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param streamingDistributionService
     * the streaming distribution service
     */
    protected fun setStreamingDistributionService(streamingDistributionService: DistributionService) {
        this.streamingDistributionService = streamingDistributionService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param downloadDistributionService
     * the download distribution service
     */
    protected fun setDownloadDistributionService(downloadDistributionService: DownloadDistributionService) {
        this.downloadDistributionService = downloadDistributionService
    }

    /**
     * Callback for declarative services configuration that will introduce us to the search service. Implementation
     * assumes that the reference is configured as being static.
     *
     * @param searchService
     * an instance of the search service
     */
    protected fun setSearchService(searchService: SearchService) {
        this.searchService = searchService
    }

    fun setOrganizationDirectoryService(organizationDirectoryService: OrganizationDirectoryService) {
        this.organizationDirectoryService = organizationDirectoryService
    }

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        val bundleContext = cc.bundleContext

        // Get configuration
        serverUrl = UrlSupport.url(bundleContext.getProperty(SERVER_URL_PROPERTY))
        distributeStreaming = StringUtils.isNotBlank(bundleContext.getProperty(STREAMING_URL_PROPERTY))
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running engage publication workflow operation")

        val mediaPackage = workflowInstance.mediaPackage
        val op = workflowInstance.currentOperation

        // Check which tags have been configured
        val downloadSourceTags = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_SOURCE_TAGS))
        val downloadTargetTags = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_TARGET_TAGS))
        val downloadSourceFlavors = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_SOURCE_FLAVORS))
        val downloadTargetSubflavor = StringUtils.trimToNull(op.getConfiguration(DOWNLOAD_TARGET_SUBFLAVOR))
        val streamingSourceTags = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_SOURCE_TAGS))
        val streamingTargetTags = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_TARGET_TAGS))
        val streamingSourceFlavors = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_SOURCE_FLAVORS))
        val streamingTargetSubflavor = StringUtils.trimToNull(op.getConfiguration(STREAMING_TARGET_SUBFLAVOR))
        val republishStrategy = StringUtils.trimToEmpty(op.getConfiguration(STRATEGY))
        val mergeForceFlavorsStr = StringUtils.trimToEmpty(
                StringUtils.defaultString(op.getConfiguration(MERGE_FORCE_FLAVORS), MERGE_FORCE_FLAVORS_DEFAULT))

        val checkAvailability = option(op.getConfiguration(CHECK_AVAILABILITY)).bind(trimToNone).map(toBool)
                .getOrElse(true)

        val sourceDownloadTags = StringUtils.split(downloadSourceTags, ",")
        val targetDownloadTags = StringUtils.split(downloadTargetTags, ",")
        val sourceDownloadFlavors = StringUtils.split(downloadSourceFlavors, ",")
        val sourceStreamingTags = StringUtils.split(streamingSourceTags, ",")
        val targetStreamingTags = StringUtils.split(streamingTargetTags, ",")
        val sourceStreamingFlavors = StringUtils.split(streamingSourceFlavors, ",")

        if (sourceDownloadTags.size == 0 && sourceDownloadFlavors.size == 0 && sourceStreamingTags.size == 0
                && sourceStreamingFlavors.size == 0) {
            logger.warn("No tags or flavors have been specified, so nothing will be published to the engage publication channel")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Parse forced flavors
        val mergeForceFlavors = Arrays.stream(StringUtils.split(mergeForceFlavorsStr, ", "))
                .map(Function<String, Any> { parseFlavor() }).collect(Collectors.toList<Any>())

        // Parse the download target flavor
        var downloadSubflavor: MediaPackageElementFlavor? = null
        if (downloadTargetSubflavor != null) {
            try {
                downloadSubflavor = MediaPackageElementFlavor.parseFlavor(downloadTargetSubflavor)
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException(e)
            }

        }

        // Parse the streaming target flavor
        var streamingSubflavor: MediaPackageElementFlavor? = null
        if (streamingTargetSubflavor != null) {
            try {
                streamingSubflavor = MediaPackageElementFlavor.parseFlavor(streamingTargetSubflavor)
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException(e)
            }

        }

        // Configure the download element selector
        val downloadElementSelector = SimpleElementSelector()
        for (flavor in sourceDownloadFlavors) {
            downloadElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
        for (tag in sourceDownloadTags) {
            downloadElementSelector.addTag(tag)
        }

        // Configure the streaming element selector
        val streamingElementSelector = SimpleElementSelector()
        for (flavor in sourceStreamingFlavors) {
            streamingElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
        for (tag in sourceStreamingTags) {
            streamingElementSelector.addTag(tag)
        }

        // Select the appropriate elements for download and streaming
        val downloadElements = downloadElementSelector.select(mediaPackage, false)
        val streamingElements = streamingElementSelector.select(mediaPackage, false)

        try {
            val downloadElementIds = HashSet<String>()
            val streamingElementIds = HashSet<String>()

            // Look for elements matching the tag
            for (elem in downloadElements) {
                downloadElementIds.add(elem.identifier)
            }
            for (elem in streamingElements) {
                streamingElementIds.add(elem.identifier)
            }

            removePublicationElement(mediaPackage)
            when (republishStrategy) {
                "merge" -> {
                }
                else -> retractFromEngage(mediaPackage)
            }// nothing to do here. other publication strategies can be added to this list later on

            val jobs = ArrayList<Job>()
            //distribute Elements
            try {
                if (downloadElementIds.size > 0) {
                    val job = downloadDistributionService!!.distribute(CHANNEL_ID, mediaPackage, downloadElementIds, checkAvailability)
                    if (job != null) {
                        jobs.add(job)
                    }
                }

                if (distributeStreaming) {
                    for (elementId in streamingElementIds) {
                        val job = streamingDistributionService!!.distribute(CHANNEL_ID, mediaPackage, elementId)
                        if (job != null) {
                            jobs.add(job)
                        }
                    }
                }
            } catch (e: DistributionException) {
                throw WorkflowOperationException(e)
            }

            if (jobs.size < 1) {
                logger.info("No mediapackage element was found for distribution to engage")
                return createResult(mediaPackage, Action.CONTINUE)
            }

            // Wait until all distribution jobs have returned
            if (!waitForStatus(*jobs.toTypedArray()).isSuccess)
                throw WorkflowOperationException("One of the distribution jobs did not complete successfully")

            logger.debug("Distribute of mediapackage {} completed", mediaPackage)

            var engageUrlString: String? = null
            try {
                var mediaPackageForSearch: MediaPackage? = getMediaPackageForSearchIndex(mediaPackage, jobs, downloadSubflavor,
                        targetDownloadTags, downloadElementIds, streamingSubflavor, streamingElementIds, targetStreamingTags)

                // MH-10216, check if only merging into existing mediapackage
                removePublicationElement(mediaPackage)
                when (republishStrategy) {
                    "merge" -> {
                        // merge() returns merged mediapackage or null mediaPackage is not published
                        mediaPackageForSearch = merge(mediaPackageForSearch, mergeForceFlavors)
                        if (mediaPackageForSearch == null) {
                            logger.info("Skipping republish for {} since it is not currently published", mediaPackage.identifier.toString())
                            return createResult(mediaPackage, Action.SKIP)
                        }
                    }
                }// nothing to do here

                if (!isPublishable(mediaPackageForSearch!!))
                    throw WorkflowOperationException("Media package does not meet criteria for publication")

                logger.info("Publishing media package {} to search index", mediaPackageForSearch)

                val engageBaseUrl: URL?
                val organization = organizationDirectoryService!!.getOrganization(workflowInstance.organizationId)
                engageUrlString = StringUtils.trimToNull(organization.properties[ENGAGE_URL_PROPERTY])
                if (engageUrlString != null) {
                    engageBaseUrl = URL(engageUrlString)
                } else {
                    engageBaseUrl = serverUrl
                    logger.info(
                            "Using 'server.url' as a fallback for the non-existing organization level key '{}' for the publication url",
                            ENGAGE_URL_PROPERTY)
                }

                // create the publication URI (used by Admin UI for event details link)
                val engageUri = this.createEngageUri(engageBaseUrl!!.toURI(), mediaPackage)

                // Create new distribution element
                val publicationElement = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_ID,
                        engageUri, MimeTypes.parseMimeType("text/html"))
                mediaPackage.add(publicationElement)

                // Adding media package to the search index
                var publishJob: Job? = null
                try {
                    publishJob = searchService!!.add(mediaPackageForSearch)
                    if (!waitForStatus(publishJob).isSuccess) {
                        throw WorkflowOperationException("Mediapackage " + mediaPackageForSearch.identifier
                                + " could not be published")
                    }
                } catch (e: SearchException) {
                    throw WorkflowOperationException("Error publishing media package", e)
                } catch (e: MediaPackageException) {
                    throw WorkflowOperationException("Error parsing media package", e)
                }

                logger.debug("Publishing of mediapackage {} completed", mediaPackage)
                return createResult(mediaPackage, Action.CONTINUE)
            } catch (e: MalformedURLException) {
                logger.error("{} is malformed: {}", ENGAGE_URL_PROPERTY, engageUrlString)
                throw WorkflowOperationException(e)
            } catch (t: Throwable) {
                if (t is WorkflowOperationException)
                    throw t
                else
                    throw WorkflowOperationException(t)
            }

        } catch (e: Exception) {
            if (e is WorkflowOperationException) {
                throw e
            } else {
                throw WorkflowOperationException(e)
            }
        }

    }

    /**
     * Local utility to assemble player path for this class
     *
     * @param engageUri
     * @param mp
     * @return the assembled player URI for this mediapackage
     */
    internal fun createEngageUri(engageUri: URI, mp: MediaPackage): URI {
        return URIUtils.resolve(engageUri, PLAYER_PATH + mp.identifier.compact())
    }

    /**
     * Returns a mediapackage that only contains elements that are marked for distribution.
     *
     * @param current
     * the current mediapackage
     * @param jobs
     * the distribution jobs
     * @param downloadSubflavor
     * flavor to be applied to elements distributed to download
     * @param downloadTargetTags
     * tags to be applied to elements distributed to downloads
     * @param downloadElementIds
     * identifiers for elements that have been distributed to downloads
     * @param streamingSubflavor
     * flavor to be applied to elements distributed to streaming
     * @param streamingElementIds
     * identifiers for elements that have been distributed to streaming
     * @param streamingTargetTags
     * tags to be applied to elements distributed to streaming
     * @return the new mediapackage
     */
    @Throws(MediaPackageException::class, NotFoundException::class, ServiceRegistryException::class, WorkflowOperationException::class)
    protected fun getMediaPackageForSearchIndex(current: MediaPackage, jobs: List<Job>,
                                                downloadSubflavor: MediaPackageElementFlavor?, downloadTargetTags: Array<String>, downloadElementIds: Set<String>,
                                                streamingSubflavor: MediaPackageElementFlavor?, streamingElementIds: Set<String>, streamingTargetTags: Array<String>): MediaPackage {
        val mp = current.clone() as MediaPackage

        // All the jobs have passed, let's update the mediapackage with references to the distributed elements
        val elementsToPublish = ArrayList<String>()
        val distributedElementIds = HashMap<String, String>()

        for (entry in jobs) {
            val job = serviceRegistry.getJob(entry.id)

            // If there is no payload, then the item has not been distributed.
            if (job.payload == null)
                continue

            var distributedElements: List<MediaPackageElement>? = null
            try {
                distributedElements = MediaPackageElementParser.getArrayFromXml(job.payload)
            } catch (e: MediaPackageException) {
                throw WorkflowOperationException(e)
            }

            // If the job finished successfully, but returned no new element, the channel simply doesn't support this
            // kind of element. So we just keep on looping.
            if (distributedElements == null || distributedElements.size < 1)
                continue

            for (distributedElement in distributedElements) {

                val sourceElementId = distributedElement.identifier
                if (sourceElementId != null) {
                    val sourceElement = mp.getElementById(sourceElementId)

                    // Make sure the mediapackage is prompted to create a new identifier for this element
                    distributedElement.identifier = null
                    if (sourceElement != null) {
                        // Adjust the flavor and tags for downloadable elements
                        if (downloadElementIds.contains(sourceElementId)) {
                            if (downloadSubflavor != null) {
                                val flavor = sourceElement.flavor
                                if (flavor != null) {
                                    val newFlavor = MediaPackageElementFlavor(flavor.type!!,
                                            downloadSubflavor.subtype!!)
                                    distributedElement.flavor = newFlavor
                                }
                            }
                        } else if (streamingElementIds.contains(sourceElementId)) {
                            if (streamingSubflavor != null && streamingElementIds.contains(sourceElementId)) {
                                val flavor = sourceElement.flavor
                                if (flavor != null) {
                                    val newFlavor = MediaPackageElementFlavor(flavor.type!!,
                                            streamingSubflavor.subtype!!)
                                    distributedElement.flavor = newFlavor
                                }
                            }
                        }// Adjust the flavor and tags for streaming elements
                        // Copy references from the source elements to the distributed elements
                        val ref = sourceElement.reference
                        if (ref != null && mp.getElementByReference(ref) != null) {
                            val newReference = ref.clone() as MediaPackageReference
                            distributedElement.reference = newReference
                        }
                    }
                }

                if (isStreamingFormat(distributedElement))
                    applyTags(distributedElement, streamingTargetTags)
                else
                    applyTags(distributedElement, downloadTargetTags)

                // Add the new element to the mediapackage
                mp.add(distributedElement)
                elementsToPublish.add(distributedElement.identifier)
                distributedElementIds[sourceElementId] = distributedElement.identifier
            }
        }

        // Mark everything that is set for removal
        val removals = ArrayList<MediaPackageElement>()
        for (element in mp.elements) {
            if (!elementsToPublish.contains(element.identifier)) {
                removals.add(element)
            }
        }

        // Translate references to the distributed artifacts
        for (element in mp.elements) {

            if (removals.contains(element))
                continue

            // Is the element referencing anything?
            val reference = element.reference ?: continue

            // See if the element has been distributed
            val distributedElementId = distributedElementIds[reference.identifier] ?: continue

            val translatedReference = MediaPackageReferenceImpl(mp.getElementById(distributedElementId))
            if (reference.properties != null) {
                translatedReference.properties.putAll(reference.properties)
            }

            // Set the new reference
            element.reference = translatedReference

        }

        // Remove everything we don't want to add to publish
        for (element in removals) {
            mp.remove(element)
        }
        return mp
    }

    /**
     * Checks if the MediaPackage track transport protocol is a streaming format protocol
     * @param element The MediaPackageElement to analyze
     * @return true if it is a TrackImpl and has a streaming protocol as transport
     */
    private fun isStreamingFormat(element: MediaPackageElement): Boolean {
        return element is TrackImpl && STREAMING_FORMATS.contains(element.getTransport())
    }

    /**
     * Adds Tags to a MediaPackageElement
     * @param element the element that needs the tags
     * @param tags the list of tags to apply
     */
    private fun applyTags(element: MediaPackageElement, tags: Array<String>) {
        for (tag in tags) {
            element.addTag(tag)
        }
    }

    /** Media package must meet these criteria in order to be published.  */
    private fun isPublishable(mp: MediaPackage): Boolean {
        val hasTitle = !isBlank(mp.title)
        if (!hasTitle)
            logger.warn("Media package does not meet criteria for publication: There is no title")

        val hasTracks = mp.hasTracks()
        if (!hasTracks)
            logger.warn("Media package does not meet criteria for publication: There are no tracks")

        return hasTitle && hasTracks
    }

    @Throws(WorkflowOperationException::class)
    protected fun getDistributedMediapackage(mediaPackageID: String): MediaPackage? {
        var mediaPackage: MediaPackage? = null
        val query = SearchQuery().withId(mediaPackageID)
        query.includeEpisodes(true)
        query.includeSeries(false)
        val result = searchService!!.getByQuery(query)
        if (result.size() == 0L) {
            logger.info("The search service doesn't know mediapackage {}.", mediaPackageID)
            return mediaPackage // i.e. null
        } else if (result.size() > 1) {
            logger.warn("More than one mediapackage with id {} returned from search service", mediaPackageID)
            throw WorkflowOperationException("More than one mediapackage with id $mediaPackageID found")
        } else {
            // else, merge the new with the existing (new elements will overwrite existing elements)
            mediaPackage = result.items[0].mediaPackage
        }
        return mediaPackage
    }


    /**
     * MH-10216, method copied from the original RepublishWorkflowOperationHandler
     * Merges mediapackage with published mediapackage.
     *
     * @param mediaPackageForSearch
     * @return merged mediapackage or null if a published medipackage was not found
     * @throws WorkflowOperationException
     */
    @Throws(WorkflowOperationException::class)
    protected fun merge(mediaPackageForSearch: MediaPackage?, forceFlavors: List<MediaPackageElementFlavor>): MediaPackage? {
        return mergePackages(mediaPackageForSearch,
                getDistributedMediapackage(mediaPackageForSearch!!.toString()),
                forceFlavors)
    }

    /**
     * MH-10216, Copied from the original RepublishWorkflowOperationHandler
     *
     * Merges the updated mediapackage with the one that is currently published in a way where the updated elements
     * replace existing ones in the published mediapackage based on their flavor.
     *
     *
     * If `publishedMp` is `null`, this method returns the updated mediapackage without any
     * modifications.
     *
     * @param updatedMp
     * the updated media package
     * @param publishedMp
     * the mediapackage that is currently published
     * @return the merged mediapackage
     */
    protected fun mergePackages(updatedMp: MediaPackage?, publishedMp: MediaPackage?,
                                forceFlavors: List<MediaPackageElementFlavor>): MediaPackage? {
        if (publishedMp == null)
            return updatedMp

        val mergedMediaPackage = updatedMp!!.clone() as MediaPackage
        for (element in publishedMp.elements()) {
            val type = element.elementType.toString().toLowerCase()
            if (updatedMp.getElementsByFlavor(element.flavor).size == 0) {
                if (forceFlavors.stream().anyMatch { f -> element.flavor.matches(f) }) {
                    logger.info("Forcing removal of {} {} due to the absence of a new element with flavor {}",
                            type, element.identifier, element.flavor.toString())
                    continue
                }
                logger.info("Merging {} '{}' into the updated mediapackage", type, element.identifier)
                mergedMediaPackage.add(element.clone() as MediaPackageElement)
            } else {
                logger.info(String.format("Overwriting existing %s '%s' with '%s' in the updated mediapackage",
                        type, element.identifier, updatedMp.getElementsByFlavor(element.flavor)[0].identifier))

            }
        }

        return mergedMediaPackage
    }

    private fun removePublicationElement(mediaPackage: MediaPackage) {
        for (publicationElement in mediaPackage.publications) {
            if (CHANNEL_ID == publicationElement.channel) {
                mediaPackage.remove(publicationElement)
            }
        }
    }

    /**
     * Removes every Publication for Searchindex from Mediapackage
     * Removes Mediapackage from Searchindex
     * @param mediaPackage Mediapackage
     * @param mediaPackageForSearch Mediapackage prepared for searchIndex
     * @throws WorkflowOperationException
     */
    @Throws(WorkflowOperationException::class)
    private fun retractFromEngage(mediaPackage: MediaPackage) {
        val jobs = ArrayList<Job>()
        val elementIds = HashSet<String>()
        try {
            val distributedMediaPackage = getDistributedMediapackage(mediaPackage.toString())
            if (distributedMediaPackage != null) {

                for (element in distributedMediaPackage.elements) {
                    elementIds.add(element.identifier)
                }
                //bulk retraction
                if (elementIds.size > 0) {
                    val retractDownloadDistributionJob = downloadDistributionService!!.retract(CHANNEL_ID, distributedMediaPackage, elementIds)
                    if (retractDownloadDistributionJob != null) {
                        jobs.add(retractDownloadDistributionJob)
                    }
                }

                if (distributeStreaming) {
                    for (element in distributedMediaPackage.elements) {
                        val retractStreamingJob = streamingDistributionService!!.retract(CHANNEL_ID, distributedMediaPackage, element.identifier)
                        if (retractStreamingJob != null) {
                            jobs.add(retractStreamingJob)
                        }
                    }
                }

                var deleteSearchJob: Job? = null
                logger.info("Retracting already published Elements for Mediapackage: {}", mediaPackage.identifier.toString())
                deleteSearchJob = searchService!!.delete(mediaPackage.identifier.toString())
                if (deleteSearchJob != null) {
                    jobs.add(deleteSearchJob)
                }
            }
            // Wait until all retraction jobs have returned
            if (!waitForStatus(*jobs.toTypedArray()).isSuccess) {
                throw WorkflowOperationException("One of the retraction jobs did not complete successfully")
            }
        } catch (e: DistributionException) {
            throw WorkflowOperationException(e)
        } catch (e: SearchException) {
            throw WorkflowOperationException("Error retracting media package", e)
        } catch (ex: UnauthorizedException) {
            logger.error("Retraction failed of Mediapackage: { }", mediaPackage.identifier.toString(), ex)
        } catch (ex: NotFoundException) {
            logger.error("Retraction failed of Mediapackage: { }", mediaPackage.identifier.toString(), ex)
        }

    }

    /** OSGi DI  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(PublishEngageWorkflowOperationHandler::class.java)

        /** Configuration properties id  */
        private val ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url"
        private val STREAMING_URL_PROPERTY = "org.opencastproject.streaming.url"

        /** Workflow configuration option keys  */
        private val DOWNLOAD_SOURCE_FLAVORS = "download-source-flavors"
        private val DOWNLOAD_TARGET_SUBFLAVOR = "download-target-subflavor"
        private val DOWNLOAD_SOURCE_TAGS = "download-source-tags"
        private val DOWNLOAD_TARGET_TAGS = "download-target-tags"
        private val STREAMING_SOURCE_TAGS = "streaming-source-tags"
        private val STREAMING_TARGET_TAGS = "streaming-target-tags"
        private val STREAMING_SOURCE_FLAVORS = "streaming-source-flavors"
        private val STREAMING_TARGET_SUBFLAVOR = "streaming-target-subflavor"
        private val CHECK_AVAILABILITY = "check-availability"
        private val STRATEGY = "strategy"
        private val MERGE_FORCE_FLAVORS = "merge-force-flavors"

        private val MERGE_FORCE_FLAVORS_DEFAULT = "dublincore/*,security/*"

        /** Path the REST endpoint which will re-direct users to the currently configured video player  */
        internal val PLAYER_PATH = "/play/"

        /** Supported streaming formats  */
        private val STREAMING_FORMATS = HashSet<TrackImpl.StreamingProtocol>(Arrays.asList<StreamingProtocol>(
                TrackImpl.StreamingProtocol.RTMP,
                TrackImpl.StreamingProtocol.RTMPE,
                TrackImpl.StreamingProtocol.HLS,
                TrackImpl.StreamingProtocol.DASH,
                TrackImpl.StreamingProtocol.HDS,
                TrackImpl.StreamingProtocol.SMOOTH))
    }

}
