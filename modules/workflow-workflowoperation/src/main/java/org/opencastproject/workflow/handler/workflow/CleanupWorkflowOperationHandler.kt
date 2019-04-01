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

import org.opencastproject.mediapackage.MediaPackageElement.Type.Publication

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.TrustedHttpClientException
import org.opencastproject.serviceregistry.api.ServiceRegistration
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workingfilerepository.api.WorkingFileRepository
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpDelete
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList

/**
 * Removes all files in the working file repository for mediapackage elements that don't match one of the
 * "preserve-flavors" configuration value.
 */
class CleanupWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /**
     * The workspace to use in retrieving and storing files.
     */
    protected var workspace: Workspace

    /** The http client to use when connecting to remote servers  */
    protected var client: TrustedHttpClient? = null

    private val allWorkingFileRepositoryUrls: MutableList<String>
        get() {
            val wfrBaseUrls = ArrayList<String>()
            try {
                for (reg in serviceRegistry.getServiceRegistrationsByType(WorkingFileRepository.SERVICE_TYPE))
                    wfrBaseUrls.add(UrlSupport.concat(reg.host, reg.path))
            } catch (e: ServiceRegistryException) {
                logger.warn("Unable to load services of type {} from service registry: {}",
                        WorkingFileRepository.SERVICE_TYPE, e.message)
            }

            return wfrBaseUrls
        }

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
     * Deletes JobArguments for every finished Job of the WorkfloInstance
     *
     * @param workflowInstance
     */
    fun cleanUpJobArgument(workflowInstance: WorkflowInstance) {
        val operationInstances = workflowInstance.operations
        for (operationInstance in operationInstances) {
            logger.debug("Delete JobArguments for Job id from Workflowinstance" + operationInstance.id!!)

            // delete job Arguments
            var operationInstanceId: Long? = null
            try {
                operationInstanceId = operationInstance.id
                // instanceId can be null if the operation never run
                if (operationInstanceId != null) {
                    val operationInstanceJob = serviceRegistry.getJob(operationInstanceId)
                    val list = ArrayList<String>()
                    operationInstanceJob.arguments = list
                    serviceRegistry.updateJob(operationInstanceJob)

                    val jobs = serviceRegistry.getChildJobs(operationInstanceId)
                    for (job in jobs) {
                        if (job.status === Job.Status.FINISHED) {
                            logger.debug("Deleting Arguments:  " + job.arguments)
                            job.arguments = list
                            serviceRegistry.updateJob(job)
                        }
                    }
                }
            } catch (ex: ServiceRegistryException) {
                logger.error("Deleting JobArguments failed for Job {}: {} ", operationInstanceId, ex)
            } catch (ex: NotFoundException) {
                logger.error("Deleting JobArguments failed for Job {}: {} ", operationInstanceId, ex)
            }

        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        cleanUpJobArgument(workflowInstance)

        val mediaPackage = workflowInstance.mediaPackage
        val currentOperation = workflowInstance.currentOperation

        val flavors = currentOperation.getConfiguration(PRESERVE_FLAVOR_PROPERTY)
        val flavorsToPreserve = ArrayList<MediaPackageElementFlavor>()

        val deleteExternal = BooleanUtils.toBoolean(currentOperation.getConfiguration(DELETE_EXTERNAL))

        val delayStr = currentOperation.getConfiguration(DELAY)
        var delay = 1

        if (delayStr != null) {
            try {
                delay = Integer.parseInt(delayStr)
            } catch (e: NumberFormatException) {
                logger.warn("Invalid value '{}' for delay in workflow operation configuration (should be integer)", delayStr)
            }

        }

        if (delay > 0) {
            try {
                logger.debug("Sleeping {}s before removing workflow files", delay)
                Thread.sleep((delay * 1000).toLong())
            } catch (e: InterruptedException) {
                // ignore
            }

        }

        // If the configuration does not specify flavors, remove them all
        for (flavor in asList(flavors))
            flavorsToPreserve.add(MediaPackageElementFlavor.parseFlavor(flavor))

        val elementsToRemove = ArrayList<MediaPackageElement>()
        for (element in mediaPackage.elements) {
            if (element.getURI() == null)
                continue


            if (!isPreserved(element, flavorsToPreserve))
                elementsToRemove.add(element)
        }

        var externalBaseUrls: MutableList<String>? = null
        if (deleteExternal) {
            externalBaseUrls = allWorkingFileRepositoryUrls
            externalBaseUrls.remove(workspace.baseUri.toString())
        }
        for (elementToRemove in elementsToRemove) {
            if (deleteExternal) {
                // cleanup external working file repositories
                for (repository in externalBaseUrls!!) {
                    logger.debug("Removing {} from repository {}", elementToRemove.getURI(), repository)
                    try {
                        removeElementFromRepository(elementToRemove, repository)
                    } catch (ex: TrustedHttpClientException) {
                        logger.debug("Removing media package element {} from repository {} failed: {}",
                                elementToRemove.getURI(), repository, ex.message)
                    }

                }
            }
            // cleanup workspace and also the internal working file repository
            logger.debug("Removing {} from the workspace", elementToRemove.getURI())
            try {
                mediaPackage.remove(elementToRemove)
                workspace.delete(elementToRemove.getURI())
            } catch (ex: NotFoundException) {
                logger.debug("Workspace doesn't contain element with Id '{}' from media package '{}': {}",
                        elementToRemove.identifier, mediaPackage.identifier.compact(), ex.message)
            } catch (ex: IOException) {
                logger.warn("Unable to remove element with Id '{}' from the media package '{}': {}",
                        elementToRemove.identifier, mediaPackage.identifier.compact(), ex.message)
            }

        }
        return createResult(mediaPackage, Action.CONTINUE)
    }

    /**
     * Returns if elements flavor matches one of the preserved flavors or the element is a publication.
     * Publications cannot be deleted but need to be retracted and will hence always be preserved. Note that publications
     * should also never directly correspond to files in the workspace or the working file repository.
     *
     * @param element Media package element to test
     * @param flavorsToPreserve Flavors to preserve
     * @return true, if elements flavor matches one of the preserved flavors, false otherwise
     */
    private fun isPreserved(element: MediaPackageElement, flavorsToPreserve: List<MediaPackageElementFlavor>): Boolean {
        if (Publication === element.elementType)
            return true

        for (flavor in flavorsToPreserve) {
            if (flavor.matches(element.flavor)) {
                return true
            }
        }
        return false
    }

    @Throws(TrustedHttpClientException::class)
    private fun removeElementFromRepository(elementToRemove: MediaPackageElement?, repositoryBaseUrl: String) {
        if (elementToRemove == null || elementToRemove.getURI() == null || StringUtils.isBlank(repositoryBaseUrl)) {
            return
        }

        val elementUri = elementToRemove.getURI().toString()
        val deleteUri: String
        if (StringUtils.containsIgnoreCase(elementUri, UrlSupport.concat(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                        elementToRemove.mediaPackage.identifier.compact(), elementToRemove.identifier))) {
            deleteUri = UrlSupport.concat(repositoryBaseUrl, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                    elementToRemove.mediaPackage.identifier.compact(), elementToRemove.identifier)
        } else if (StringUtils.containsIgnoreCase(elementUri, WorkingFileRepository.COLLECTION_PATH_PREFIX)) {
            deleteUri = UrlSupport.concat(repositoryBaseUrl, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                    StringUtils.substringAfter(elementToRemove.getURI().getPath(), WorkingFileRepository.COLLECTION_PATH_PREFIX))
        } else {
            // the element isn't from working file repository, skip
            logger.info("Unable to handle URI {} for deletion from repository {}", elementUri, repositoryBaseUrl)
            return
        }
        val delete = HttpDelete(deleteUri)
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
        } finally {
            client!!.close(response!!)
        }
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(CleanupWorkflowOperationHandler::class.java)

        /** The element flavors to maintain in the original mediapackage. All others will be removed  */
        val PRESERVE_FLAVOR_PROPERTY = "preserve-flavors"

        /** Deleting external URI's config key  */
        val DELETE_EXTERNAL = "delete-external"

        /** Time to wait in seconds before removing files  */
        val DELAY = "delay"
    }
}
