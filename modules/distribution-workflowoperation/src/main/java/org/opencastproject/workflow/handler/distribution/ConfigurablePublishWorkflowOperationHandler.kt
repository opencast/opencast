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

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.RequireUtil
import org.opencastproject.util.doc.DocUtil
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.UUID

/**
 * WOH that distributes selected elements to an internal distribution channel and adds reflective publication elements
 * to the media package.
 */

open class ConfigurablePublishWorkflowOperationHandler : ConfigurableWorkflowOperationHandlerBase() {
    // service references
    private var distributionService: DownloadDistributionService? = null

    private var securityService: SecurityService? = null

    /** OSGi DI  */
    internal fun setDownloadDistributionService(distributionService: DownloadDistributionService) {
        this.distributionService = distributionService
    }

    /** OSGi DI  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    override fun getDistributionService(): DownloadDistributionService {
        assert(distributionService != null)
        return distributionService
    }

    /**
     * Replace possible variables in the url-pattern configuration for this workflow operation handler.
     *
     * @param urlPattern
     * The operation's template for replacing the variables.
     * @param mp
     * The [MediaPackage] used to get the event / mediapackage id.
     * @param pubUUID
     * The UUID for the published element.
     * @return The URI of the published element with the variables replaced.
     * @throws WorkflowOperationException
     * Thrown if the URI is malformed after replacing the variables.
     */
    @Throws(WorkflowOperationException::class)
    fun populateUrlWithVariables(urlPattern: String, mp: MediaPackage, pubUUID: String): URI {
        val values = HashMap<String, Any>()
        values[EVENT_ID_TEMPLATE_KEY] = mp.identifier.compact()
        values[PUBLICATION_ID_TEMPLATE_KEY] = pubUUID
        val playerPath = securityService!!.organization.properties[PLAYER_PROPERTY]
        values[PLAYER_PATH_TEMPLATE_KEY] = playerPath
        values[SERIES_ID_TEMPLATE_KEY] = StringUtils.trimToEmpty(mp.series)
        val uriWithVariables = DocUtil.processTextTemplate("Replacing Variables in Publish URL", urlPattern, values)
        val publicationURI: URI
        try {
            publicationURI = URI(uriWithVariables)
        } catch (e: URISyntaxException) {
            throw WorkflowOperationException(String.format(
                    "Unable to create URI from template '%s', replacement was: '%s'", urlPattern, uriWithVariables), e)
        }

        return publicationURI
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        RequireUtil.notNull(workflowInstance, "workflowInstance")

        val mp = workflowInstance.mediaPackage
        val op = workflowInstance.currentOperation

        val channelId = StringUtils.trimToEmpty(op.getConfiguration(CHANNEL_ID_KEY))
        if ("" == channelId) {
            throw WorkflowOperationException("Unable to publish this mediapackage as the configuration key "
                    + CHANNEL_ID_KEY + " is missing. Unable to determine where to publish these elements.")
        }

        val urlPattern = StringUtils.trimToEmpty(op.getConfiguration(URL_PATTERN))

        var mimetype: MimeType? = null
        val mimetypeString = StringUtils.trimToEmpty(op.getConfiguration(MIME_TYPE))
        if ("" != mimetypeString) {
            try {
                mimetype = MimeTypes.parseMimeType(mimetypeString)
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Unable to parse the provided configuration for $MIME_TYPE", e)
            }

        }

        val withPublishedElements = BooleanUtils
                .toBoolean(StringUtils.trimToEmpty(op.getConfiguration(WITH_PUBLISHED_ELEMENTS)))

        val checkAvailability = BooleanUtils
                .toBoolean(StringUtils.trimToEmpty(op.getConfiguration(CHECK_AVAILABILITY)))

        if (getPublications(mp, channelId).size > 0) {
            val rePublishStrategy = StringUtils.trimToEmpty(op.getConfiguration(STRATEGY))

            when (rePublishStrategy) {

                "fail" ->
                    // fail is a dummy function for further distribution strategies
                    fail(mp)
                "merge" -> {
                }
                else -> retract(mp, channelId)
            }// nothing to do here. other publication strategies can be added to this list later on
        }

        var mode = StringUtils.trimToEmpty(op.getConfiguration(MODE))
        if ("" == mode) {
            mode = DEFAULT_MODE
        } else if (!ArrayUtils.contains(KNOWN_MODES, mode)) {
            logger.error("Unknown value for configuration key mode: '{}'", mode)
            throw IllegalArgumentException("Unknown value for configuration key mode")
        }

        val sourceFlavors = StringUtils.split(StringUtils.trimToEmpty(op.getConfiguration(SOURCE_FLAVORS)), ",")
        val sourceTags = StringUtils.split(StringUtils.trimToEmpty(op.getConfiguration(SOURCE_TAGS)), ",")

        val publicationUUID = UUID.randomUUID().toString()
        val publication = PublicationImpl.publication(publicationUUID, channelId, null, null)

        // Configure the element selector
        val selector = SimpleElementSelector()
        for (flavor in sourceFlavors) {
            selector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
        for (tag in sourceTags) {
            selector.addTag(tag)
        }

        if (sourceFlavors.size > 0 || sourceTags.size > 0) {
            if (!withPublishedElements) {
                val elements = distribute(selector.select(mp, false), mp, channelId, mode,
                        checkAvailability)
                if (elements.size > 0) {
                    for (element in elements) {
                        // Make sure the mediapackage is prompted to create a new identifier for this element
                        element.identifier = null
                        PublicationImpl.addElementToPublication(publication, element)
                    }
                } else {
                    logger.info("No element found for distribution in media package '{}'", mp)
                    return createResult(mp, Action.CONTINUE)
                }
            } else {
                val publishedElements = ArrayList<MediaPackageElement>()
                for (alreadyPublished in mp.publications) {
                    publishedElements.addAll(Arrays.asList<Attachment>(*alreadyPublished.attachments))
                    publishedElements.addAll(Arrays.asList<Catalog>(*alreadyPublished.catalogs))
                    publishedElements.addAll(Arrays.asList<Track>(*alreadyPublished.tracks))
                }
                for (element in selector.select(publishedElements, false)) {
                    PublicationImpl.addElementToPublication(publication, element)
                }
            }
        }
        if ("" != urlPattern) {
            publication.setURI(populateUrlWithVariables(urlPattern, mp, publicationUUID))
        }
        if (mimetype != null) {
            publication.mimeType = mimetype
        }
        mp.add(publication)
        return createResult(mp, Action.CONTINUE)
    }

    @Throws(WorkflowOperationException::class)
    private fun distribute(elements: Collection<MediaPackageElement>, mediapackage: MediaPackage,
                           channelId: String, mode: String, checkAvailability: Boolean): Set<MediaPackageElement> {

        val result = HashSet<MediaPackageElement>()

        val bulkElementIds = HashSet<String>()
        val singleElementIds = HashSet<String>()

        for (element in elements) {
            if (MODE_BULK == mode || MODE_MIXED == mode && element.elementType !== MediaPackageElement.Type.Track) {
                bulkElementIds.add(element.identifier)
            } else {
                singleElementIds.add(element.identifier)
            }
        }

        val jobs = HashSet<Job>()
        if (bulkElementIds.size > 0) {
            logger.info("Start bulk publishing of {} elements of media package '{}' to publication channel '{}'",
                    bulkElementIds.size, mediapackage, channelId)
            try {
                val job = distributionService!!.distribute(channelId, mediapackage, bulkElementIds, checkAvailability)
                jobs.add(job)
            } catch (e: DistributionException) {
                logger.error("Creating the distribution job for {} elements of media package '{}' failed",
                        bulkElementIds.size, mediapackage, e)
                throw WorkflowOperationException(e)
            } catch (e: MediaPackageException) {
                logger.error("Creating the distribution job for {} elements of media package '{}' failed", bulkElementIds.size, mediapackage, e)
                throw WorkflowOperationException(e)
            }

        }
        if (singleElementIds.size > 0) {
            logger.info("Start single publishing of {} elements of media package '{}' to publication channel '{}'",
                    singleElementIds.size, mediapackage, channelId)
            for (elementId in singleElementIds) {
                try {
                    val job = distributionService!!.distribute(channelId, mediapackage, elementId, checkAvailability)
                    jobs.add(job)
                } catch (e: DistributionException) {
                    logger.error("Creating the distribution job for element '{}' of media package '{}' failed", elementId,
                            mediapackage, e)
                    throw WorkflowOperationException(e)
                } catch (e: MediaPackageException) {
                    logger.error("Creating the distribution job for element '{}' of media package '{}' failed", elementId, mediapackage, e)
                    throw WorkflowOperationException(e)
                }

            }
        }

        if (jobs.size > 0) {
            if (!waitForStatus(*jobs.toTypedArray()).isSuccess) {
                throw WorkflowOperationException("At least one of the distribution jobs did not complete successfully")
            }
            for (job in jobs) {
                try {
                    val elems = MediaPackageElementParser.getArrayFromXml(job.payload)
                    result.addAll(elems)
                } catch (e: MediaPackageException) {
                    logger.error("Job '{}' returned payload ({}) that could not be parsed to media package elements", job,
                            job.payload, e)
                    throw WorkflowOperationException(e)
                }

            }
            logger.info("Published {} elements of media package {} to publication channel {}",
                    bulkElementIds.size + singleElementIds.size, mediapackage, channelId)
        }
        return result
    }

    /**
     * Dummy function for further publication strategies
     *
     * @param mp
     * @throws WorkflowOperationException
     */
    @Throws(WorkflowOperationException::class)
    private fun fail(mp: MediaPackage) {
        logger.error("There is already a Published Media, fail Stragy for Mediapackage {}", mp.identifier)
        throw WorkflowOperationException("There is already a Published Media, fail Stragy for Mediapackage ")
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ConfigurablePublishWorkflowOperationHandler::class.java)

        /** The template key for adding the mediapackage / event id to the publication path.  */
        protected val EVENT_ID_TEMPLATE_KEY = "event_id"
        /** The template key for adding the player location path to the publication path.  */
        protected val PLAYER_PATH_TEMPLATE_KEY = "player_path"
        /** The template key for adding the publication id to the publication path.  */
        protected val PUBLICATION_ID_TEMPLATE_KEY = "publication_id"
        /** The template key for adding the series id to the publication path.  */
        protected val SERIES_ID_TEMPLATE_KEY = "series_id"
        /** The configuration property value for the player location.  */
        val PLAYER_PROPERTY = "player"

        /** Workflow configuration options  */
        internal val CHANNEL_ID_KEY = "channel-id"
        internal val MIME_TYPE = "mimetype"
        internal val SOURCE_TAGS = "source-tags"
        internal val SOURCE_FLAVORS = "source-flavors"
        internal val WITH_PUBLISHED_ELEMENTS = "with-published-elements"
        internal val CHECK_AVAILABILITY = "check-availability"
        internal val STRATEGY = "strategy"
        internal val MODE = "mode"

        /** Known values for mode  */
        internal val MODE_SINGLE = "single"
        internal val MODE_MIXED = "mixed"
        internal val MODE_BULK = "bulk"

        internal val KNOWN_MODES = arrayOf(MODE_SINGLE, MODE_MIXED, MODE_BULK)

        internal val DEFAULT_MODE = MODE_BULK

        /** The workflow configuration key for defining the url pattern.  */
        internal val URL_PATTERN = "url-pattern"
    }
}
