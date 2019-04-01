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

import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import org.opencastproject.event.comment.EventComment
import org.opencastproject.event.comment.EventCommentException
import org.opencastproject.event.comment.EventCommentService
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance

import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Test

import java.util.ArrayList

class CommentWorkflowOperationHandlerTest {

    @Test
    @Throws(WorkflowOperationException::class, EventCommentException::class, NotFoundException::class)
    fun testPossibleActions() {
        // Testing that a duplicate comment won't be created but a different one will still be created.
        val workflowId = 10L
        val deleteCommentId = 21L
        val mediaPackageId = "abc-def"
        val reason = "Waiting for Trim"
        val description = "The comment description"

        val org = createNiceMock<Organization>(Organization::class.java)
        expect(org.id).andStubReturn("demo")
        replay(org)

        val secSrv = createNiceMock<SecurityService>(SecurityService::class.java)
        expect(secSrv.organization).andStubReturn(org)
        replay(secSrv)

        // Setup WorkflowOperation Instance
        val workflowOperationInstance = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        // Create
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(CommentWorkflowOperationHandler.Operation.create.toString())
        // Resolve
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(CommentWorkflowOperationHandler.Operation.resolve.toString())
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(CommentWorkflowOperationHandler.Operation.resolve.toString())
        // Deletes
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(CommentWorkflowOperationHandler.Operation.delete.toString())
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(CommentWorkflowOperationHandler.Operation.delete.toString())
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(CommentWorkflowOperationHandler.Operation.delete.toString())
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON))
                .andReturn(reason).anyTimes()
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION))
                .andReturn(description).anyTimes()

        // Setup mediaPackage
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect<Id>(mediaPackage.identifier).andReturn(IdImpl(mediaPackageId)).anyTimes()

        // Setup user
        val creator = EasyMock.createMock<User>(User::class.java)

        // Setup WorkflowInstance
        val workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.id).andReturn(workflowId).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(workflowOperationInstance).anyTimes()
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflowInstance.creator).andReturn(creator).anyTimes()

        EasyMock.replay(creator, mediaPackage, workflowInstance, workflowOperationInstance)

        // Test create
        var eventCommentService = EasyMock.createMock<EventCommentService>(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList()).anyTimes()
        var comment = EasyMock.newCapture<EventComment>()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.id, description, creator))
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(17L), mediaPackageId, org.id, description, creator))
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(19L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(eventCommentService)
        var commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)
        assertEquals(false, comment.value.isResolvedStatus)

        // Test resolve
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        var comments: MutableList<EventComment> = ArrayList()
        comments.add(EventComment.create(Option.option(deleteCommentId), mediaPackageId, org.id, description, creator,
                reason, false))
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes()
        comment = EasyMock.newCapture()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(17L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)
        assertEquals(true, comment.value.isResolvedStatus)

        // Test resolve with no comment
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList()).anyTimes()
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)

        // Test delete with no result, no delete
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList()).anyTimes()
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)

        // Test delete with result
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        comments = ArrayList()
        comments.add(EventComment.create(Option.option(deleteCommentId), mediaPackageId, org.id, description, creator,
                reason, false))
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes()
        eventCommentService.deleteComment(deleteCommentId)
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)

        // Test delete with unrelated comments
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        comments = ArrayList()
        comments.add(EventComment.create(Option.option(35L), mediaPackageId, org.id, description, creator, "", false))
        comments.add(EventComment.create(Option.option(37L), mediaPackageId, org.id, "Different Description", creator,
                reason, false))
        comments.add(EventComment.create(Option.option(39L), mediaPackageId, org.id, description, creator,
                "Different Reason", false))
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes()
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
    }

    @Test
    @Throws(WorkflowOperationException::class, EventCommentException::class)
    fun testDifferentCaseAction() {
        // Testing that a duplicate comment won't be created but a different one will still be created.
        val workflowId = 10L
        val mediaPackageId = "abc-def"
        val reason = "Waiting for Trim"
        val description = "The comment description"

        val org = createNiceMock<Organization>(Organization::class.java)
        expect(org.id).andStubReturn("demo")
        replay(org)

        val secSrv = createNiceMock<SecurityService>(SecurityService::class.java)
        expect(secSrv.organization).andStubReturn(org)
        replay(secSrv)

        // Setup WorkflowOperation Instance
        val workflowOperationInstance = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        // Standard
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn("create")
        // Mixed case
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn("CrEaTe")
        // All Caps
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn("CREATE")
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON))
                .andReturn(reason).anyTimes()
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION))
                .andReturn(description).anyTimes()

        // Setup mediaPackage
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect<Id>(mediaPackage.identifier).andReturn(IdImpl(mediaPackageId)).anyTimes()

        // Setup user
        val creator = EasyMock.createMock<User>(User::class.java)

        // Setup WorkflowInstance
        val workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.id).andReturn(workflowId).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(workflowOperationInstance).anyTimes()
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflowInstance.creator).andReturn(creator).anyTimes()

        EasyMock.replay(creator, mediaPackage, workflowInstance, workflowOperationInstance)

        // Test no previous comments
        var eventCommentService = EasyMock.createMock<EventCommentService>(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList()).anyTimes()
        var comment = EasyMock.newCapture<EventComment>()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(eventCommentService)
        var commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)

        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList()).anyTimes()
        comment = EasyMock.newCapture()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(17L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)

        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList()).anyTimes()
        comment = EasyMock.newCapture()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(19L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.setSecurityService(secSrv)

        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)
    }

    @Test
    @Throws(WorkflowOperationException::class, EventCommentException::class)
    fun testDuplicateComments() {
        // Testing that a duplicate comment won't be created but a different one will still be created.
        val workflowId = 10L
        val mediaPackageId = "abc-def"
        val action = "create"
        val reason = "Waiting for Trim"
        val description = "The comment description"

        val org = createNiceMock<Organization>(Organization::class.java)
        expect(org.id).andStubReturn("demo")
        replay(org)

        val secSrv = createNiceMock<SecurityService>(SecurityService::class.java)
        expect(secSrv.organization).andStubReturn(org)
        replay(secSrv)

        // Setup WorkflowOperation Instance
        val workflowOperationInstance = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
                .andReturn(action).anyTimes()
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON))
                .andReturn(reason).anyTimes()
        EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION))
                .andReturn(description).anyTimes()

        // Setup mediaPackage
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect<Id>(mediaPackage.identifier).andReturn(IdImpl(mediaPackageId)).anyTimes()

        // Setup user
        val creator = EasyMock.createMock<User>(User::class.java)

        // Setup WorkflowInstance
        val workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.id).andReturn(workflowId).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(workflowOperationInstance).anyTimes()
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflowInstance.creator).andReturn(creator).anyTimes()

        // Test no previous comments
        var eventCommentService = EasyMock.createMock<EventCommentService>(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(ArrayList())
        var comment = EasyMock.newCapture<EventComment>()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(creator, eventCommentService, mediaPackage, workflowInstance, workflowOperationInstance)
        var commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)

        // Test previous comment with same reason and description
        var comments: MutableList<EventComment> = ArrayList()
        comments.add(EventComment.create(Option.option(13L), mediaPackageId, org.id, description, creator, reason, true))
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments)
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)

        // Test previous comment with different reasons and descriptions
        comments = ArrayList()
        comments.add(EventComment.create(Option.option(15L), mediaPackageId, org.id, "Different description", creator,
                reason, true))
        comments.add(EventComment.create(Option.option(15L), mediaPackageId, org.id, description, creator,
                "Different reason", true))
        eventCommentService = EasyMock.createMock(EventCommentService::class.java)
        EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments)
        comment = EasyMock.newCapture()
        EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
                .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.id, description, creator))
        EasyMock.replay(eventCommentService)
        commentWorkflowOperationHandler = CommentWorkflowOperationHandler()
        commentWorkflowOperationHandler.setEventCommentService(eventCommentService)
        commentWorkflowOperationHandler.setSecurityService(secSrv)
        commentWorkflowOperationHandler.start(workflowInstance, null)
        assertTrue(comment.hasCaptured())
        assertEquals(creator, comment.value.author)
        assertEquals(description, comment.value.text)
        assertEquals(reason, comment.value.reason)
    }
}
