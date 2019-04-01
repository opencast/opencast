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

package org.opencastproject.workflow.handler.ingest

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.TrustedHttpClientException
import org.opencastproject.serviceregistry.api.ServiceRegistration
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.UrlSupport
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workingfilerepository.api.WorkingFileRepository
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpDelete
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.util.ArrayList

/**
 * Downloads all external URI's to the working file repository and optionally deletes external working file repository
 * resources
 */
/** The default no-arg constructor builds the configuration options set  */
class IngestDownloadWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /**
     * The workspace to use in retrieving and storing files.
     */
    protected var workspace: Workspace

    /** The http client to use when connecting to remote servers  */
    protected var client: TrustedHttpClient? = null

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
     * Sets the trusted http client
     *
     * @param client
     * the trusted http client
     */
    fun setTrustedHttpClient(client: TrustedHttpClient) {
        this.client = client
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage
        val currentOperation = workflowInstance.currentOperation

        val deleteExternal = BooleanUtils.toBoolean(currentOperation.getConfiguration(DELETE_EXTERNAL))
        val tagsAndFlavor = BooleanUtils.toBoolean(currentOperation.getConfiguration(TAGS_AND_FLAVORS))
        val sourceFlavors = getConfig(workflowInstance, SOURCE_FLAVORS, "*/*")
        val sourceTags = getConfig(workflowInstance, SOURCE_TAGS, "")

        // building elementSelector with tags and flavors
        val elementSelector = SimpleElementSelector()
        for (tag in asList(sourceTags)) {
            elementSelector.addTag(tag)
        }
        for (flavor in asList(sourceFlavors)) {
            elementSelector.addFlavor(flavor)
        }

        val baseUrl = workspace.baseUri.toString()

        val externalUris = ArrayList<URI>()
        for (element in elementSelector.select(mediaPackage, tagsAndFlavor)) {
            if (element.getURI() == null)
                continue

            if (element.elementType === MediaPackageElement.Type.Publication) {
                logger.debug("Skipping downloading media package element {} from media package {} " + "because it is a publication: {}",
                        element.identifier, mediaPackage.identifier.compact(), element.getURI())
                continue
            }

            val originalElementUri = element.getURI()
            if (originalElementUri.toString().startsWith(baseUrl)) {
                logger.info("Skipping downloading already existing element {}", originalElementUri)
                continue
            }

            // Download the external URI
            val file: File
            try {
                file = workspace.get(element.getURI())
            } catch (e: Exception) {
                logger.warn("Unable to download the external element {}", element.getURI())
                throw WorkflowOperationException("Unable to download the external element " + element.getURI(), e)
            }

            // Put to working file repository and rewrite URI on element
            var `in`: InputStream? = null
            try {
                `in` = FileInputStream(file)
                val uri = workspace.put(mediaPackage.identifier.compact(), element.identifier,
                        FilenameUtils.getName(element.getURI().getPath()), `in`)
                element.setURI(uri)
            } catch (e: Exception) {
                logger.warn("Unable to store downloaded element '{}': {}", element.getURI(), e.message)
                throw WorkflowOperationException("Unable to store downloaded element " + element.getURI(), e)
            } finally {
                IOUtils.closeQuietly(`in`)
                try {
                    workspace.delete(originalElementUri)
                } catch (e: Exception) {
                    logger.warn("Unable to delete ingest-downloaded element {}: {}", element.getURI(), e)
                }

            }

            logger.info("Downloaded the external element {}", originalElementUri)

            // Store origianl URI for deletion
            externalUris.add(originalElementUri)
        }

        if (!deleteExternal || externalUris.size == 0)
            return createResult(mediaPackage, Action.CONTINUE)

        // Find all external working file repository base Urls
        logger.debug("Assembling list of external working file repositories")
        val externalWfrBaseUrls = ArrayList<String>()
        try {
            for (reg in serviceRegistry.getServiceRegistrationsByType(WorkingFileRepository.SERVICE_TYPE)) {
                if (baseUrl.startsWith(reg.host)) {
                    logger.trace("Skpping local working file repository")
                    continue
                }
                externalWfrBaseUrls.add(UrlSupport.concat(reg.host, reg.path))
            }
            logger.debug("{} external working file repositories found", externalWfrBaseUrls.size)
        } catch (e: ServiceRegistryException) {
            logger.error("Unable to load WFR services from service registry: {}", e.message)
            throw WorkflowOperationException(e)
        }

        for (uri in externalUris) {

            val elementUri = uri.toString()

            // Delete external working file repository URI's
            var wfrBaseUrl: String? = null
            for (url in externalWfrBaseUrls) {
                if (elementUri.startsWith(url)) {
                    wfrBaseUrl = url
                    break
                }
            }

            if (wfrBaseUrl == null) {
                logger.info("Unable to delete external URI {}, no working file repository found", elementUri)
                continue
            }

            val delete: HttpDelete
            if (elementUri.startsWith(UrlSupport.concat(wfrBaseUrl, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX))) {
                val wfrDeleteUrl = elementUri.substring(0, elementUri.lastIndexOf("/"))
                delete = HttpDelete(wfrDeleteUrl)
            } else if (elementUri.startsWith(UrlSupport.concat(wfrBaseUrl, WorkingFileRepository.COLLECTION_PATH_PREFIX))) {
                delete = HttpDelete(elementUri)
            } else {
                logger.info("Unable to handle working file repository URI {}", elementUri)
                continue
            }

            var response: HttpResponse? = null
            try {
                response = client!!.execute(delete)
                val statusCode = response.statusLine.statusCode
                if (statusCode == HttpStatus.SC_NO_CONTENT || statusCode == HttpStatus.SC_OK) {
                    logger.info("Sucessfully deleted external URI {}", delete.uri)
                } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    logger.info("External URI {} has already been deleted", delete.uri)
                } else {
                    logger.info("Unable to delete external URI {}, status code '{}' returned", delete.uri, statusCode)
                }
            } catch (e: TrustedHttpClientException) {
                logger.warn("Unable to execute DELETE request on external URI {}", delete.uri)
                throw WorkflowOperationException(e)
            } finally {
                client!!.close(response!!)
            }
        }

        return createResult(mediaPackage, Action.CONTINUE)
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(IngestDownloadWorkflowOperationHandler::class.java)

        /** Deleting external working file repository URI's after download config key  */
        val DELETE_EXTERNAL = "delete-external"

        /** config key used to specify a list of flavors (seperated by comma), elements matching a flavor will be downloaded  */
        val SOURCE_FLAVORS = "source-flavors"

        /** config key used to specify a list of tags (seperated by comma), elements matching a tag will be downloaded  */
        val SOURCE_TAGS = "source-tags"

        /** config key used to specify, whether both, a tag and a flavor, must match or if one is sufficient  */
        val TAGS_AND_FLAVORS = "tags-and-flavors"
    }
}
