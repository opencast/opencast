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

import com.entwinemedia.fn.fns.Strings.trimToNone
import java.lang.String.format
import org.opencastproject.util.JobUtil.waitForJobs
import org.opencastproject.util.data.Collections.set

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.util.data.Collections
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashSet

/** Workflow operation for handling "republish" operations to OAI-PMH repositories.  */
class RepublishOaiPmhWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    private var oaiPmhPublicationService: OaiPmhPublicationService? = null

    @Throws(WorkflowOperationException::class)
    override fun start(wi: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mp = wi.mediaPackage
        // The flavors of the elements that are to be published
        val flavors = HashSet<String>()
        // Check which flavors have been configured
        val configuredFlavors = getOptConfig(wi, OPT_SOURCE_FLAVORS).bind(trimToNone).map(asList.toFn())
                .getOr(Collections.nil())
        for (flavor in configuredFlavors) {
            flavors.add(flavor)
        }
        // Get the configured tags
        val tags = set(getOptConfig(wi, OPT_SOURCE_TAGS).getOr(""))
        // repository
        val repository = getConfig(wi, OPT_REPOSITORY)

        logger.debug("Start updating metadata of the media package {} in OAI-PMH repository {}",
                mp.identifier.compact(), repository)
        try {
            val updateMetadataJob = oaiPmhPublicationService!!.updateMetadata(mp, repository, flavors, tags, true)
            if (updateMetadataJob == null) {
                logger.info("Unable to create an OAI-PMH update metadata job for the media package {} in repository {}",
                        mp.identifier.compact(), repository)
                return createResult(mp, Action.CONTINUE)
            }

            if (!waitForJobs(serviceRegistry, updateMetadataJob).isSuccess()) {
                throw WorkflowOperationException(format(
                        "OAI-PMH update metadata job for the media package %s did not end successfully",
                        mp.identifier.compact()))
            }
        } catch (e: MediaPackageException) {
            throw WorkflowOperationException(format(
                    "Unable to create an OAI-PMH update metadata job for the media package %s in repository %s",
                    mp.identifier.compact(), repository), e)
        } catch (e: PublicationException) {
            throw WorkflowOperationException(format("Unable to create an OAI-PMH update metadata job for the media package %s in repository %s", mp.identifier.compact(), repository), e)
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException(format("Unable to create an OAI-PMH update metadata job for the media package %s in repository %s", mp.identifier.compact(), repository), e)
        } catch (e: IllegalStateException) {
            throw WorkflowOperationException(format("Unable to create an OAI-PMH update metadata job for the media package %s in repository %s", mp.identifier.compact(), repository), e)
        }

        logger.debug("Updating metadata of the media package {} in OAI-PMH repository {} done",
                mp.identifier.compact(), repository)
        return createResult(mp, Action.CONTINUE)
    }

    /** OSGI DI  */
    fun setOaiPmhPublicationService(oaiPmhPublicationService: OaiPmhPublicationService) {
        this.oaiPmhPublicationService = oaiPmhPublicationService
    }

    companion object {
        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(RepublishOaiPmhWorkflowOperationHandler::class.java)

        /** The configuration options  */
        private val OPT_SOURCE_FLAVORS = "source-flavors"
        private val OPT_SOURCE_TAGS = "source-tags"
        private val OPT_REPOSITORY = "repository"
    }

}
