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

package org.opencastproject.workflow.handler.comments

import org.opencastproject.event.comment.EventComment
import org.opencastproject.event.comment.EventCommentException
import org.opencastproject.event.comment.EventCommentService
import org.opencastproject.job.api.JobContext
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import com.entwinemedia.fn.data.Opt

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Date

/**
 * A workflow operation handler for creating, resolving and deleting comments automatically during the workflow process.
 */
class CommentWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /* service references */
    private var eventCommentService: EventCommentService? = null
    private var securityService: SecurityService? = null

    enum class Operation {
        create, resolve, delete
    }

    /**
     * {@inheritDoc}
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running comment workflow operation on workflow {}", workflowInstance.id)
        try {
            return handleCommentOperation(workflowInstance)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    /**
     * Determine the type of operation to do on a comment and execute it.
     *
     * @param workflowInstance
     * The [WorkflowInstance] to be handled.
     * @return The result of handling the [WorkflowInstance]
     * @throws EventCommentException
     * Thrown if there is an issue creating, resolving or deleting a comment
     * @throws NotFoundException
     * Thrown if the comment cannot be found to delete.
     */
    @Throws(EventCommentException::class, NotFoundException::class)
    private fun handleCommentOperation(workflowInstance: WorkflowInstance): WorkflowOperationResult {
        val date = Date()
        val operation = workflowInstance.currentOperation
        val action: Operation
        val inputAction = StringUtils.trimToNull(operation.getConfiguration(ACTION))
        if (inputAction == null) {
            action = Operation.create
        } else {
            action = Operation.valueOf(inputAction.toLowerCase())
        }
        val reason = StringUtils.trimToNull(operation.getConfiguration(REASON))
        val description = StringUtils.trimToNull(operation.getConfiguration(DESCRIPTION))
        when (action) {
            CommentWorkflowOperationHandler.Operation.create -> createComment(workflowInstance, reason, description)
            CommentWorkflowOperationHandler.Operation.resolve -> resolveComment(workflowInstance, reason, description)
            CommentWorkflowOperationHandler.Operation.delete -> deleteComment(workflowInstance, reason, description)
            else -> logger.warn(
                    "Unknown action '{}' for comment with description '{}' and reason '{}'. It should be one of the following: ",
                    inputAction, description, reason, StringUtils.join(Operation.values(), ","))
        }
        return createResult(workflowInstance.mediaPackage, Action.CONTINUE,
                Date().time - date.time)
    }

    /**
     * Create a new comment if one doesn't already exist with the reason and description.
     *
     * @param workflowInstance
     * The [WorkflowInstance] to handle.
     * @param reason
     * The reason for the comment.
     * @param description
     * The descriptive text of the comment.
     * @throws EventCommentException
     * Thrown if unable to create the comment.
     */
    @Throws(EventCommentException::class)
    private fun createComment(workflowInstance: WorkflowInstance, reason: String?, description: String?) {
        val optComment = findComment(workflowInstance.mediaPackage.identifier.toString(), reason,
                description)
        if (optComment.isNone) {
            val comment = EventComment.create(Option.none(), workflowInstance.mediaPackage.identifier.toString(),
                    securityService!!.organization.id, description, workflowInstance.creator, reason, false)
            eventCommentService!!.updateComment(comment)
        } else {
            logger.debug("Not creating comment with '{}' text and '{}' reason as it already exists for this event.",
                    description, reason)
        }
    }

    /**
     * Resolve a comment with matching reason and description.
     *
     * @param workflowInstance
     * The [WorkflowInstance] to handle.
     * @param reason
     * The reason for the comment.
     * @param description
     * The comment's descriptive text.
     * @throws EventCommentException
     * Thrown if unable to update the comment.
     */
    @Throws(EventCommentException::class)
    private fun resolveComment(workflowInstance: WorkflowInstance, reason: String?, description: String?) {
        val optComment = findComment(workflowInstance.mediaPackage.identifier.toString(), reason,
                description)
        if (optComment.isSome) {
            val comment = EventComment.create(optComment.get().id, workflowInstance.mediaPackage.identifier.toString(),
                    securityService!!.organization.id, optComment.get().text,
                    optComment.get().author, optComment.get().reason, true, optComment.get().creationDate,
                    optComment.get().modificationDate, optComment.get().replies)
            eventCommentService!!.updateComment(comment)
        } else {
            logger.debug("Not resolving comment with '{}' text and/or '{}' reason as it doesn't exist.", description, reason)
        }
    }

    /**
     * Delete a comment with matching reason and description.
     *
     * @param workflowInstance
     * The [WorkflowInstance] to handle.
     * @param reason
     * The reason for the comment.
     * @param description
     * The comment's descriptive text.
     * @throws EventCommentException
     * Thrown if unable to delete the comment.
     * @throws NotFoundException
     * Thrown if unable to find the comment.
     */
    @Throws(EventCommentException::class, NotFoundException::class)
    private fun deleteComment(workflowInstance: WorkflowInstance, reason: String?, description: String?) {
        val optComment = findComment(workflowInstance.mediaPackage.identifier.toString(), reason,
                description)
        if (optComment.isSome) {
            try {
                eventCommentService!!.deleteComment(optComment.get().id.get())
            } catch (e: NotFoundException) {
                logger.debug("Not deleting comment with '{}' text and '{}' reason and id '{}' as it doesn't exist.",
                        description, reason, optComment.get().id)
            }

        } else {
            logger.debug("Not deleting comment with '{}' text and/or '{}' reason as it doesn't exist.", description, reason)
        }
    }

    /**
     * Find a comment by its reason, description or both
     *
     * @param eventId
     * The event id to search the comments for.
     * @param reason
     * The reason for the comment (optional)
     * @param description
     * The description for the comment (optional)
     * @return The comment if one is found matching the reason and description.
     * @throws EventCommentException
     * Thrown if there was a problem finding the comment.
     */
    @Throws(EventCommentException::class)
    private fun findComment(eventId: String, reason: String?, description: String?): Opt<EventComment> {
        var comment = Opt.none<EventComment>()
        val eventComments = eventCommentService!!.getComments(eventId)

        for (existingComment in eventComments) {
            // Match on reason and description
            if (reason != null && description != null
                    && reason == existingComment.reason && description == existingComment.text) {
                comment = Opt.some(existingComment)
                break
            }
            // Match on reason only
            if (reason != null && description == null && reason == existingComment.reason) {
                comment = Opt.some(existingComment)
                break
            }
            // Match on description only
            if (reason == null && description != null && description == existingComment.text) {
                comment = Opt.some(existingComment)
                break
            }
        }
        return comment
    }

    /**
     * Callback from the OSGi environment that will pass a reference to the workflow service upon component activation.
     *
     * @param eventCommentService
     * the workflow service
     */
    fun setEventCommentService(eventCommentService: EventCommentService) {
        this.eventCommentService = eventCommentService
    }

    /** OSGi DI  */
    internal fun setSecurityService(service: SecurityService) {
        this.securityService = service
    }

    companion object {
        val ACTION = "action"
        val DESCRIPTION = "description"
        val REASON = "reason"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CommentWorkflowOperationHandler::class.java)
    }

}
