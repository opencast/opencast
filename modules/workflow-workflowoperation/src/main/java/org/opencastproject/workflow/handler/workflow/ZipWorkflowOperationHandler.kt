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

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.util.Checksum
import org.opencastproject.util.ChecksumType
import org.opencastproject.util.FileSupport
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.ZipUtil
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList

/**
 * Produces a zipped archive of a mediapackage, places it in the archive collection, and removes the rest of the
 * mediapackage elements from both the mediapackage xml and if possible, from storage altogether.
 */
class ZipWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The temporary storage location  */
    protected var tempStorageDir: File? = null

    /**
     * The workspace to use in retrieving and storing files.
     */
    protected var workspace: Workspace

    /**
     * Sets the workspace to use.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Activate the component, generating the temporary storage directory for
     * building zip archives if necessary.
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.activate
     */
    override fun activate(cc: ComponentContext) {
        tempStorageDir = if (StringUtils.isNotBlank(cc.bundleContext.getProperty(ZIP_ARCHIVE_TEMP_DIR_CFG_KEY)))
            File(cc.bundleContext.getProperty(ZIP_ARCHIVE_TEMP_DIR_CFG_KEY))
        else
            File(cc.bundleContext.getProperty("org.opencastproject.storage.dir"), DEFAULT_ZIP_ARCHIVE_TEMP_DIR)

        // create directory
        try {
            FileUtils.forceMkdir(tempStorageDir!!)
        } catch (e: IOException) {
            logger.error("Could not create temporary directory for ZIP archives: `{}`", tempStorageDir!!.absolutePath)
            throw IllegalStateException(e)
        }

        // Clean up tmp dir on start-up
        try {
            FileUtils.cleanDirectory(tempStorageDir!!)
        } catch (e: IOException) {
            logger.error("Could not clean temporary directory for ZIP archives: `{}`", tempStorageDir!!.absolutePath)
            throw IllegalStateException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance?, context: JobContext): WorkflowOperationResult {

        if (workflowInstance == null) {
            throw WorkflowOperationException("Invalid workflow instance")
        }

        val mediaPackage = workflowInstance.mediaPackage
        val currentOperation = workflowInstance.currentOperation
                ?: throw WorkflowOperationException("Cannot get current workflow operation")

        val flavors = currentOperation.getConfiguration(INCLUDE_FLAVORS_PROPERTY)
        val flavorsToZip = ArrayList<MediaPackageElementFlavor>()
        var targetFlavor = DEFAULT_ARCHIVE_FLAVOR

        // Read the target flavor
        val targetFlavorOption = currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY)
        try {
            targetFlavor = if (targetFlavorOption == null) DEFAULT_ARCHIVE_FLAVOR else MediaPackageElementFlavor.parseFlavor(targetFlavorOption)
            logger.trace("Using '{}' as the target flavor for the zip archive of recording {}", targetFlavor, mediaPackage)
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException("Flavor '$targetFlavorOption' is not valid", e)
        }

        // Read the target tags
        val targetTagsOption = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TAGS_PROPERTY))
        val targetTags = StringUtils.split(targetTagsOption, ",")

        // If the configuration does not specify flavors, just zip them all
        if (flavors == null) {
            flavorsToZip.add(MediaPackageElementFlavor.parseFlavor("*/*"))
        } else {
            for (flavor in asList(flavors)) {
                flavorsToZip.add(MediaPackageElementFlavor.parseFlavor(flavor))
            }
        }

        logger.info("Archiving mediapackage {} in workflow {}", mediaPackage, workflowInstance)

        val compressProperty = currentOperation.getConfiguration(COMPRESS_PROPERTY)
        val compress = if (compressProperty == null) false else java.lang.Boolean.valueOf(compressProperty)

        // Zip the contents of the mediapackage
        var zip: File? = null
        try {
            logger.info("Creating zipped archive of recording {}", mediaPackage)
            zip = zip(mediaPackage, flavorsToZip, compress)
        } catch (e: Exception) {
            throw WorkflowOperationException("Unable to create a zip archive from mediapackage $mediaPackage", e)
        }

        // Get the collection for storing the archived mediapackage
        val configuredCollectionId = currentOperation.getConfiguration(ZIP_COLLECTION_PROPERTY)
        val collectionId = configuredCollectionId ?: DEFAULT_ZIP_COLLECTION

        // Add the zip as an attachment to the mediapackage
        logger.info("Moving zipped archive of recording {} to the working file repository collection '{}'", mediaPackage,
                collectionId)

        var `in`: InputStream? = null
        var uri: URI? = null
        try {
            `in` = FileInputStream(zip)
            uri = workspace.putInCollection(collectionId, mediaPackage.identifier.compact() + ".zip", `in`)
            logger.info("Zipped archive of recording {} is available from {}", mediaPackage, uri)
        } catch (e: FileNotFoundException) {
            throw WorkflowOperationException("zip file $zip not found", e)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        val attachment = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(uri, Type.Attachment, targetFlavor) as Attachment
        try {
            attachment.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, zip)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

        attachment.mimeType = MimeTypes.ZIP

        // Apply the target tags
        for (tag in targetTags) {
            attachment.addTag(tag)
            logger.trace("Tagging the archive of recording '{}' with '{}'", mediaPackage, tag)
        }
        attachment.mimeType = MimeTypes.ZIP

        // The zip file is safely in the archive, so it's now safe to attempt to remove the original zip
        try {
            FileUtils.forceDelete(zip)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

        mediaPackage.add(attachment)

        return createResult(mediaPackage, Action.CONTINUE)
    }

    /**
     * Creates a zip archive of all elements in a mediapackage.
     *
     * @param mediaPackage
     * the mediapackage to zip
     *
     * @return the zip file
     *
     * @throws IOException
     * If an IO exception occurs
     * @throws NotFoundException
     * If a file referenced in the mediapackage can not be found
     * @throws MediaPackageException
     * If the mediapackage can not be serialized to xml
     * @throws WorkflowOperationException
     * If the mediapackage is invalid
     */
    @Throws(IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    protected fun zip(mediaPackage: MediaPackage?, flavorsToZip: List<MediaPackageElementFlavor>, compress: Boolean): File {

        if (mediaPackage == null) {
            throw WorkflowOperationException("Invalid mediapackage")
        }

        // Create the temp directory
        val mediaPackageDir = File(tempStorageDir, mediaPackage.identifier.compact())
        FileUtils.forceMkdir(mediaPackageDir)

        // Link or copy each matching element's file from the workspace to the temp directory
        val serializer = DefaultMediaPackageSerializerImpl(mediaPackageDir)
        val clone = mediaPackage.clone() as MediaPackage
        for (element in clone.elements) {
            // remove the element if it doesn't match the flavors to zip
            var remove = true
            for (flavor in flavorsToZip) {
                if (flavor.matches(element.flavor)) {
                    remove = false
                    break
                }
            }
            if (remove) {
                clone.remove(element)
                continue
            }
            val elementDir = File(mediaPackageDir, element.identifier)
            FileUtils.forceMkdir(elementDir)
            val workspaceFile = workspace.get(element.getURI())
            val linkedFile = FileSupport.link(workspaceFile, File(elementDir, workspaceFile.name), true)
            try {
                element.setURI(serializer.encodeURI(linkedFile.toURI()))
            } catch (e: URISyntaxException) {
                throw MediaPackageException("unable to serialize a mediapackage element", e)
            }

        }

        // Add the manifest
        FileUtils.writeStringToFile(File(mediaPackageDir, "manifest.xml"), MediaPackageParser.getAsXml(clone), "UTF-8")

        // Zip the directory
        val zip = File(tempStorageDir, clone.identifier.compact() + ".zip")
        val compressValue = if (compress) ZipUtil.DEFAULT_COMPRESSION else ZipUtil.NO_COMPRESSION

        val startTime = System.currentTimeMillis()
        ZipUtil.zip(arrayOf(mediaPackageDir), zip, true, compressValue)
        val stopTime = System.currentTimeMillis()

        logger.debug("Zip file creation took {} seconds", (stopTime - startTime) / 1000)

        // Remove the directory
        FileUtils.forceDelete(mediaPackageDir)

        // Return the zip
        return zip
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(ZipWorkflowOperationHandler::class.java)

        /** The workflow operation's property to consult to determine the collection to use to store an archive  */
        val ZIP_COLLECTION_PROPERTY = "zip-collection"

        /** The element flavors to include in the zip file  */
        val INCLUDE_FLAVORS_PROPERTY = "include-flavors"

        /** The zip archive's target element flavor  */
        val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** The zip archive's target tags  */
        val TARGET_TAGS_PROPERTY = "target-tags"

        /** The property indicating whether to apply compression to the archive  */
        val COMPRESS_PROPERTY = "compression"

        /** The default collection in the working file repository to store archives  */
        val DEFAULT_ZIP_COLLECTION = "zip"

        /** The default location to use when building an zip archive relative to the
         * storage directory  */
        val DEFAULT_ZIP_ARCHIVE_TEMP_DIR = "tmp/zip"

        /** Key for configuring the location of the archive-temp folder  */
        val ZIP_ARCHIVE_TEMP_DIR_CFG_KEY = "org.opencastproject.workflow.handler.workflow.ZipWorkflowOperationHandler.tmpdir"

        /** The default flavor to use for a mediapackage archive  */
        val DEFAULT_ARCHIVE_FLAVOR = MediaPackageElementFlavor
                .parseFlavor("archive/zip")
    }

}
