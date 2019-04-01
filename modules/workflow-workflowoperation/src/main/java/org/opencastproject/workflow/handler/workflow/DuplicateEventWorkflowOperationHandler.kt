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

package org.opencastproject.workflow.handler.workflow

import org.apache.commons.lang3.StringUtils.split
import org.apache.commons.lang3.StringUtils.trimToEmpty

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.PropertyId
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.distribution.api.DistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.util.JobUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.Optional
import java.util.UUID


/**
 * This WOH duplicates an input event.
 */
class DuplicateEventWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** AssetManager to use for creating new media packages.  */
    private var assetManager: AssetManager? = null

    /** The workspace to use for retrieving and storing files.  */
    protected var workspace: Workspace

    /** The distribution service  */
    protected var distributionService: DistributionService

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param assetManager
     * the asset manager
     */
    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param distributionService
     * the distributionService to set
     */
    fun setDistributionService(distributionService: DistributionService) {
        this.distributionService = distributionService
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val mediaPackage = workflowInstance.mediaPackage
        val operation = workflowInstance.currentOperation
        val configuredSourceFlavors = trimToEmpty(operation.getConfiguration(SOURCE_FLAVORS_PROPERTY))
        val configuredSourceTags = trimToEmpty(operation.getConfiguration(SOURCE_TAGS_PROPERTY))
        val configuredTargetTags = trimToEmpty(operation.getConfiguration(TARGET_TAGS_PROPERTY))
        val numberOfEvents = Integer.parseInt(operation.getConfiguration(NUMBER_PROPERTY))
        val configuredPropertyNamespaces = trimToEmpty(operation.getConfiguration(PROPERTY_NAMESPACES_PROPERTY))
        var maxNumberOfEvents = MAX_NUMBER_DEFAULT

        if (operation.getConfiguration(MAX_NUMBER_PROPERTY) != null) {
            maxNumberOfEvents = Integer.parseInt(operation.getConfiguration(MAX_NUMBER_PROPERTY))
        }

        if (numberOfEvents > maxNumberOfEvents) {
            throw WorkflowOperationException("Number of events to create exceeds the maximum of "
                    + maxNumberOfEvents + ". Aborting.")
        }

        logger.info("Creating {} new media packages from media package with id {}.", numberOfEvents,
                mediaPackage.identifier)

        val sourceTags = split(configuredSourceTags, ",")
        val targetTags = split(configuredTargetTags, ",")
        val sourceFlavors = split(configuredSourceFlavors, ",")
        val propertyNamespaces = split(configuredPropertyNamespaces, ",")
        val copyNumberPrefix = trimToEmpty(operation.getConfiguration(COPY_NUMBER_PREFIX_PROPERTY))

        val elementSelector = SimpleElementSelector()
        for (flavor in sourceFlavors) {
            elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }

        val removeTags = ArrayList<String>()
        val addTags = ArrayList<String>()
        val overrideTags = ArrayList<String>()

        for (tag in targetTags) {
            if (tag.startsWith(MINUS)) {
                removeTags.add(tag)
            } else if (tag.startsWith(PLUS)) {
                addTags.add(tag)
            } else {
                overrideTags.add(tag)
            }
        }

        for (tag in sourceTags) {
            elementSelector.addTag(tag)
        }

        // Filter elements to copy based on input tags and input flavors
        val elements = elementSelector.select(mediaPackage, false)
        val internalPublications = HashSet<Publication>()

        for (e in mediaPackage.elements) {
            if (e is Publication && InternalPublicationChannel.CHANNEL_ID.equals(e.channel)) {
                internalPublications.add(e)
            }
            if (MediaPackageElements.EPISODE.equals(e.flavor)) {
                // Remove episode DC since we will add a new one (with changed title)
                elements.remove(e)
            }
        }

        val originalEpisodeDc = mediaPackage.getElementsByFlavor(MediaPackageElements.EPISODE)
        if (originalEpisodeDc.size != 1) {
            throw WorkflowOperationException("Media package " + mediaPackage.identifier + " has "
                    + originalEpisodeDc.size + " episode dublin cores while it is expected to have exactly 1. Aborting.")
        }

        val properties = HashMap<String, String>()

        for (i in 0 until numberOfEvents) {
            val temporaryFiles = ArrayList<URI>()
            var newMp: MediaPackage? = null

            try {
                // Clone the media package (without its elements)
                newMp = copyMediaPackage(mediaPackage, (i + 1).toLong(), copyNumberPrefix)

                // Create and add new episode dublin core with changed title
                newMp = copyDublinCore(mediaPackage, originalEpisodeDc[0], newMp, removeTags, addTags, overrideTags,
                        temporaryFiles)

                // Clone regular elements
                for (e in elements) {
                    val element = e.clone() as MediaPackageElement
                    updateTags(element, removeTags, addTags, overrideTags)
                    newMp.add(element)
                }

                // Clone internal publications
                for (originalPub in internalPublications) {
                    copyPublication(originalPub, mediaPackage, newMp, removeTags, addTags, overrideTags, temporaryFiles)
                }

                assetManager!!.takeSnapshot(AssetManager.DEFAULT_OWNER, newMp)

                // Clone properties of media package
                for (namespace in propertyNamespaces) {
                    copyProperties(namespace, mediaPackage, newMp)
                }

                // Store media package ID as workflow property
                properties["duplicate_media_package_" + (i + 1) + "_id"] = newMp.identifier.toString()
            } finally {
                cleanup(temporaryFiles, Optional.ofNullable(newMp))
            }
        }
        return createResult(mediaPackage, properties, Action.CONTINUE, 0)
    }

    private fun cleanup(temporaryFiles: List<URI>, newMp: Optional<MediaPackage>) {
        // Remove temporary files of new media package
        for (temporaryFile in temporaryFiles) {
            try {
                workspace.delete(temporaryFile)
            } catch (e: NotFoundException) {
                logger.debug("{} could not be found in the workspace and hence, cannot be deleted.", temporaryFile)
            } catch (e: IOException) {
                logger.warn("Failed to delete {} from workspace.", temporaryFile)
            }

        }
        newMp.ifPresent { mp ->
            try {
                workspace.cleanup(mp.identifier)
            } catch (e: IOException) {
                logger.warn("Failed to cleanup the workspace for media package {}", mp.identifier)
            }
        }
    }

    private fun updateTags(
            element: MediaPackageElement,
            removeTags: List<String>,
            addTags: List<String>,
            overrideTags: List<String>) {
        element.identifier = null

        if (overrideTags.size > 0) {
            element.clearTags()
            for (overrideTag in overrideTags) {
                element.addTag(overrideTag)
            }
        } else {
            for (removeTag in removeTags) {
                element.removeTag(removeTag.substring(MINUS.length))
            }
            for (tag in addTags) {
                element.addTag(tag.substring(PLUS.length))
            }
        }
    }

    @Throws(WorkflowOperationException::class)
    private fun copyMediaPackage(
            source: MediaPackage,
            copyNumber: Long,
            copyNumberPrefix: String): MediaPackage {
        // We are not using MediaPackage.clone() here, since it does "too much" for us (e.g. copies all the attachments)
        val destination: MediaPackage
        try {
            destination = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        } catch (e: MediaPackageException) {
            logger.error("Failed to create media package " + e.localizedMessage)
            throw WorkflowOperationException(e)
        }

        logger.info("Created mediapackage {}", destination)
        destination.date = source.date
        destination.series = source.series
        destination.seriesTitle = source.seriesTitle
        destination.duration = source.duration
        destination.language = source.language
        destination.license = source.license
        destination.title = source.title + " (" + copyNumberPrefix + " " + copyNumber + ")"
        return destination
    }

    @Throws(WorkflowOperationException::class)
    private fun copyPublication(
            sourcePublication: Publication,
            source: MediaPackage,
            destination: MediaPackage,
            removeTags: List<String>,
            addTags: List<String>,
            overrideTags: List<String>,
            temporaryFiles: MutableList<URI>) {
        val newPublicationId = UUID.randomUUID().toString()
        val newPublication = PublicationImpl.publication(newPublicationId,
                InternalPublicationChannel.CHANNEL_ID, null, null)

        // re-distribute elements of publication to internal publication channel
        val sourcePubElements = HashSet<MediaPackageElement>()
        sourcePubElements.addAll(Arrays.asList<Attachment>(*sourcePublication.attachments))
        sourcePubElements.addAll(Arrays.asList<Catalog>(*sourcePublication.catalogs))
        sourcePubElements.addAll(Arrays.asList<Track>(*sourcePublication.tracks))
        for (e in sourcePubElements) {
            try {
                // We first have to copy the media package element into the workspace
                val element = e.clone() as MediaPackageElement
                workspace.read(element.getURI()).use { inputStream ->
                    val tmpUri = workspace.put(destination.identifier.toString(), element.identifier,
                            FilenameUtils.getName(element.getURI().toString()), inputStream)
                    temporaryFiles.add(tmpUri)
                    element.identifier = null
                    element.setURI(tmpUri)
                }

                // Now we can distribute it to the new media package
                destination.add(element) // Element has to be added before it can be distributed
                val job = distributionService.distribute(InternalPublicationChannel.CHANNEL_ID, destination,
                        element.identifier)
                val distributedElement = JobUtil.payloadAsMediaPackageElement(serviceRegistry).apply(job)
                destination.remove(element)

                updateTags(distributedElement, removeTags, addTags, overrideTags)

                PublicationImpl.addElementToPublication(newPublication, distributedElement)
            } catch (exception: Exception) {
                throw WorkflowOperationException(exception)
            }

        }

        // Using an altered copy of the source publication's URI is a bit hacky,
        // but it works without knowing the URI pattern...
        var publicationUri = sourcePublication.getURI().toString()
        publicationUri = publicationUri.replace(source.identifier.toString(), destination.identifier.toString())
        publicationUri = publicationUri.replace(sourcePublication.identifier, newPublicationId)
        newPublication.setURI(URI.create(publicationUri))
        destination.add(newPublication)
    }

    @Throws(WorkflowOperationException::class)
    private fun copyDublinCore(
            source: MediaPackage,
            sourceDublinCore: MediaPackageElement,
            destination: MediaPackage,
            removeTags: List<String>,
            addTags: List<String>,
            overrideTags: List<String>,
            temporaryFiles: MutableList<URI>): MediaPackage {
        val destinationDublinCore = DublinCoreUtil.loadEpisodeDublinCore(workspace, source).get()
        destinationDublinCore.identifier = null
        destinationDublinCore.setURI(sourceDublinCore.getURI())
        destinationDublinCore[DublinCore.PROPERTY_TITLE] = destination.title
        try {
            IOUtils.toInputStream(destinationDublinCore.toXmlString(), "UTF-8").use { inputStream ->
                val elementId = UUID.randomUUID().toString()
                val newUrl = workspace.put(destination.identifier.compact(), elementId, "dublincore.xml",
                        inputStream)
                temporaryFiles.add(newUrl)
                val mpe = destination.add(newUrl, MediaPackageElement.Type.Catalog,
                        MediaPackageElements.EPISODE)
                mpe.identifier = elementId
                for (tag in sourceDublinCore.tags) {
                    mpe.addTag(tag)
                }
                updateTags(mpe, removeTags, addTags, overrideTags)
            }
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

        return destination
    }

    private fun copyProperties(namespace: String, source: MediaPackage, destination: MediaPackage) {
        val q = assetManager!!.createQuery()
        val properties = q.select(q.propertiesOf(namespace))
                .where(q.mediaPackageId(source.identifier.toString())).run()
        if (properties.records.head().isNone) {
            logger.info("No properties to copy for media package {}.", source.identifier, namespace)
            return
        }
        for (p in properties.records.head().get().properties) {
            val newPropId = PropertyId.mk(destination.identifier.toString(), namespace, p.id
                    .name)
            assetManager!!.setProperty(Property.mk(newPropId, p.value))
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DuplicateEventWorkflowOperationHandler::class.java)
        private val PLUS = "+"
        private val MINUS = "-"

        /** Name of the configuration option that provides the source flavors we are looking for  */
        val SOURCE_FLAVORS_PROPERTY = "source-flavors"

        /** Name of the configuration option that provides the source tags we are looking for  */
        val SOURCE_TAGS_PROPERTY = "source-tags"

        /** Name of the configuration option that provides the target tags we should apply  */
        val TARGET_TAGS_PROPERTY = "target-tags"

        /** Name of the configuration option that provides the number of events to create  */
        val NUMBER_PROPERTY = "number-of-events"

        /** Name of the configuration option that provides the maximum number of events to create  */
        val MAX_NUMBER_PROPERTY = "max-number-of-events"

        /** The default maximum number of events to create. Can be overridden.  */
        val MAX_NUMBER_DEFAULT = 25

        /** The namespaces of the asset manager properties to copy.  */
        val PROPERTY_NAMESPACES_PROPERTY = "property-namespaces"

        /** The prefix to use for the number which is appended to the original title of the event.  */
        val COPY_NUMBER_PREFIX_PROPERTY = "copy-number-prefix"
    }
}
