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

package org.opencastproject.workflow.handler.composer

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.selector.AttachmentSelector
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.PathSupport
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.Hashtable

class ImageConvertWorkflowOperationHandler : AbstractWorkflowOperationHandler() {


    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the composer service
     */
    fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
    }

    /**
     * Callback for declarative services configuration that will introduce us to the local workspace service.
     *
     * @param workspace
     * an instance of the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val operation = workflowInstance.currentOperation
        val sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_SOURCE_FLAVOR))
        val sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_SOURCE_FLAVORS))
        val sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_SOURCE_TAGS))
        var targetFlavorOption: String? = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TARGET_FLAVOR))
        if (targetFlavorOption == null)
            targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TARGET_FLAVORS))
        val targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TARGET_TAGS))
        var encodingProfileOption: String? = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_ENCODING_PROFILE))
        if (encodingProfileOption == null)
            encodingProfileOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_ENCODING_PROFILES))
        val tagsAndFlavorsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TAGS_AND_FLAVORS))
        val tagsAndFlavors = BooleanUtils.toBoolean(tagsAndFlavorsOption)


        val mediaPackage = workflowInstance.mediaPackage

        // Make sure either one of tags or flavors are provided
        if (StringUtils.isBlank(sourceFlavorOption) && StringUtils.isBlank(sourceFlavorsOption)
                && StringUtils.isBlank(sourceTagsOption)) {
            logger.info("No source tags or flavors have been specified, not matching anything")
            return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE)
        }

        // Target flavor
        var targetFlavor: MediaPackageElementFlavor? = null
        if (StringUtils.isNotBlank(targetFlavorOption)) {
            try {
                targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption)
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Target flavor '$targetFlavorOption' is malformed")
            }

        }
        // check target-tags configuration
        val fixedTags = ArrayList<String>()
        val additionalTags = ArrayList<String>()
        val removingTags = ArrayList<String>()
        for (targetTag in asList(targetTagsOption)) {
            if (!StringUtils.startsWithAny(targetTag, "+", "-")) {
                if (additionalTags.size > 0 || removingTags.size > 0) {
                    logger.warn("You may not mix fixed tags and tag changes. "
                            + "Please review target-tags option on image-convert operation of your workflow definition. "
                            + "The tag {} is not prefixed with '+' or '-'.", targetTag)
                }
                fixedTags.add(targetTag)
            } else if (StringUtils.startsWith(targetTag, "+")) {
                additionalTags.add(StringUtils.substring(targetTag, 1))
            } else if (StringUtils.startsWith(targetTag, "-")) {
                removingTags.add(StringUtils.substring(targetTag, 1))
            }
        }

        val profiles = ArrayList<String>()
        for (encodingProfileId in asList(encodingProfileOption)) {
            val profile = composerService!!.getProfile(encodingProfileId)
                    ?: throw WorkflowOperationException("Encoding profile '$encodingProfileId' was not found")
// just test if the profile exists, we only need the profile id for further work
            profiles.add(encodingProfileId)
        }

        // Make sure there is at least one profile
        if (profiles.isEmpty())
            throw WorkflowOperationException("No encoding profile was specified")

        val attachmentSelector = AttachmentSelector()
        for (sourceFlavor in asList(sourceFlavorsOption)) {
            attachmentSelector.addFlavor(sourceFlavor)
        }
        for (sourceFlavor in asList(sourceFlavorOption)) {
            attachmentSelector.addFlavor(sourceFlavor)
        }
        for (sourceTag in asList(sourceTagsOption)) {
            attachmentSelector.addTag(sourceTag)
        }

        // Look for elements matching the tag
        val sourceElements = attachmentSelector.select(mediaPackage, tagsAndFlavors)

        val jobs = Hashtable<Job, Attachment>()
        try {
            for (sourceElement in sourceElements) {
                val job = composerService!!.convertImage(sourceElement, *profiles.toTypedArray())
                jobs[job] = sourceElement
            }
            if (!waitForStatus(*jobs.keys.toTypedArray()).isSuccess) {
                throw WorkflowOperationException("At least one image conversation job did not succeed.")
            }
            for ((job, sourceElement) in jobs) {
                val targetElements = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Attachment>
                for (targetElement in targetElements) {
                    val targetFileName = PathSupport.toSafeName(FilenameUtils.getName(targetElement.getURI().getPath()))
                    val newTargetElementUri = workspace!!.moveTo(targetElement.getURI(), mediaPackage.identifier.compact(),
                            targetElement.identifier, targetFileName)
                    targetElement.setURI(newTargetElementUri)
                    targetElement.checksum = null

                    // set flavor on target element
                    if (targetFlavor != null) {
                        targetElement.flavor = targetFlavor
                        if (StringUtils.equalsAny("*", targetFlavor.type)) {
                            targetElement.flavor = MediaPackageElementFlavor.flavor(
                                    sourceElement.flavor.type, targetElement.flavor.subtype)
                        }
                        if (StringUtils.equalsAny("*", targetFlavor.subtype)) {
                            targetElement.flavor = MediaPackageElementFlavor.flavor(
                                    targetElement.flavor.type, sourceElement.flavor.subtype)
                        }
                    }
                    // set tags on target element
                    targetElement.clearTags()
                    if (fixedTags.isEmpty() && (!additionalTags.isEmpty() || !removingTags.isEmpty())) {
                        for (tag in sourceElement.tags) {
                            targetElement.addTag(tag)
                        }
                    }
                    for (targetTag in fixedTags) {
                        targetElement.addTag(targetTag)
                    }
                    for (additionalTag in additionalTags) {
                        targetElement.addTag(additionalTag)
                    }
                    for (removingTag in removingTags) {
                        targetElement.removeTag(removingTag)
                    }
                    mediaPackage.addDerived(targetElement, sourceElement)
                }
            }
            return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE)
        } catch (ex: WorkflowOperationException) {
            throw ex
        } catch (t: Throwable) {
            throw WorkflowOperationException("Convert image operation failed", t)
        } finally {
            cleanupWorkspace(jobs.keys)
        }
    }

    private fun cleanupWorkspace(jobs: Collection<Job>) {
        for (job in jobs) {
            try {
                val targetElements = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Attachment>
                for (targetElement in targetElements) {
                    try {
                        workspace!!.delete(targetElement.getURI())
                    } catch (ex: NotFoundException) {
                        logger.trace("The image file {} not found", targetElement, ex)
                    } catch (ex: IOException) {
                        logger.warn("Unable to delete image file {} from workspace", targetElement, ex)
                    }

                }
            } catch (ex: MediaPackageException) {
                logger.debug("Unable to parse job payload from job {}", job.id, ex)
            }

        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImageConvertWorkflowOperationHandler::class.java)

        /** Configuration key for the source flavor  */
        private val CONFIG_KEY_SOURCE_FLAVOR = "source-flavor"
        /** Configuration key for source flavors (comma seperated values)  */
        private val CONFIG_KEY_SOURCE_FLAVORS = "source-flavors"
        /** Configuration key for source tags (comma separated values)  */
        private val CONFIG_KEY_SOURCE_TAGS = "source-tags"
        /** Configuration key for the target flavor  */
        private val CONFIG_KEY_TARGET_FLAVOR = "target-flavor"
        /** Configuration key for target flavor. The value of this configuration will be used, if target-flavor isn't set  */
        private val CONFIG_KEY_TARGET_FLAVORS = "target-flavors"
        /** Configuration key for target tags  */
        private val CONFIG_KEY_TARGET_TAGS = "target-tags"
        /** Configuration key for encoding profile  */
        private val CONFIG_KEY_ENCODING_PROFILE = "encoding-profile"
        /** Configuration key for encoding profile. The value of this configuration will be used,
         * if encoding-profile isn't set   */
        private val CONFIG_KEY_ENCODING_PROFILES = "encoding-profiles"
        /** Boolean configuration key for value, wether to use flavors and tags for selection of the source
         * attachments (set to true) or flavors or tags (set to false)  */
        private val CONFIG_KEY_TAGS_AND_FLAVORS = "tags-and-flavors"
    }
}
