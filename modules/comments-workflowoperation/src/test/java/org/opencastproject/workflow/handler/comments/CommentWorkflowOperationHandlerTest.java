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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.comments;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentException;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CommentWorkflowOperationHandlerTest {

  @Test
  public void testPossibleActions() throws WorkflowOperationException, EventCommentException, NotFoundException {
    // Testing that a duplicate comment won't be created but a different one will still be created.
    Long workflowId = 10L;
    Long deleteCommentId = 21L;
    String mediaPackageId = "abc-def";
    String reason = "Waiting for Trim";
    String description = "The comment description";

    Organization org = createNiceMock(Organization.class);
    expect(org.getId()).andStubReturn("demo");
    replay(org);

    SecurityService secSrv = createNiceMock(SecurityService.class);
    expect(secSrv.getOrganization()).andStubReturn(org);
    replay(secSrv);

    // Setup WorkflowOperation Instance
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    // Create
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(CommentWorkflowOperationHandler.Operation.create.toString());
    // Resolve
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(CommentWorkflowOperationHandler.Operation.resolve.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(CommentWorkflowOperationHandler.Operation.resolve.toString());
    // Deletes
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(CommentWorkflowOperationHandler.Operation.delete.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(CommentWorkflowOperationHandler.Operation.delete.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(CommentWorkflowOperationHandler.Operation.delete.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON))
            .andReturn(reason).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION))
            .andReturn(description).anyTimes();

    // Setup mediaPackage
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(new IdImpl(mediaPackageId)).anyTimes();

    // Setup user
    User creator = EasyMock.createMock(User.class);

    // setup user directory
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyString())).andReturn(creator).anyTimes();

    // Setup WorkflowInstance
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowId).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(workflowOperationInstance).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCreatorName()).andReturn("user").anyTimes();

    EasyMock.replay(creator, userDirectoryService, mediaPackage, workflowInstance, workflowOperationInstance);

    // Test create
    EventCommentService eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>()).anyTimes();
    Capture<EventComment> comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.getId(), description, creator));
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(17L), mediaPackageId, org.getId(), description, creator));
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(19L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(eventCommentService);
    CommentWorkflowOperationHandler commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
    assertEquals(false, comment.getValue().isResolvedStatus());

    // Test resolve
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    List<EventComment> comments = new ArrayList<EventComment>();
    comments.add(EventComment.create(Option.option(deleteCommentId), mediaPackageId, org.getId(), description, creator,
            reason, false));
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(17L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
    assertEquals(true, comment.getValue().isResolvedStatus());

    // Test resolve with no comment
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>()).anyTimes();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);

    // Test delete with no result, no delete
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>()).anyTimes();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.start(workflowInstance, null);

    // Test delete with result
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    comments = new ArrayList<EventComment>();
    comments.add(EventComment.create(Option.option(deleteCommentId), mediaPackageId, org.getId(), description, creator,
            reason, false));
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes();
    eventCommentService.deleteComment(deleteCommentId);
    EasyMock.expectLastCall();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.start(workflowInstance, null);

    // Test delete with unrelated comments
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    comments = new ArrayList<EventComment>();
    comments.add(EventComment.create(Option.option(35L), mediaPackageId, org.getId(), description, creator, "", false));
    comments.add(EventComment.create(Option.option(37L), mediaPackageId, org.getId(), "Different Description", creator,
            reason, false));
    comments.add(EventComment.create(Option.option(39L), mediaPackageId, org.getId(), description, creator,
            "Different Reason", false));
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.start(workflowInstance, null);
  }

  @Test
  public void testDifferentCaseAction() throws WorkflowOperationException, EventCommentException {
    // Testing that a duplicate comment won't be created but a different one will still be created.
    Long workflowId = 10L;
    String mediaPackageId = "abc-def";
    String reason = "Waiting for Trim";
    String description = "The comment description";

    Organization org = createNiceMock(Organization.class);
    expect(org.getId()).andStubReturn("demo");
    replay(org);

    SecurityService secSrv = createNiceMock(SecurityService.class);
    expect(secSrv.getOrganization()).andStubReturn(org);
    replay(secSrv);

    // Setup WorkflowOperation Instance
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    // Standard
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn("create");
    // Mixed case
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn("CrEaTe");
    // All Caps
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn("CREATE");
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON))
            .andReturn(reason).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION))
            .andReturn(description).anyTimes();

    // Setup mediaPackage
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(new IdImpl(mediaPackageId)).anyTimes();

    // Setup user
    User creator = EasyMock.createMock(User.class);

    // setup user directory
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyString())).andReturn(creator).anyTimes();

    // Setup WorkflowInstance
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowId).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(workflowOperationInstance).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCreatorName()).andReturn("user").anyTimes();

    EasyMock.replay(creator, userDirectoryService, mediaPackage, workflowInstance, workflowOperationInstance);

    // Test no previous comments
    EventCommentService eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>()).anyTimes();
    Capture<EventComment> comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(eventCommentService);
    CommentWorkflowOperationHandler commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>()).anyTimes();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(17L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>()).anyTimes();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(19L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
  }

  @Test
  public void testDuplicateComments() throws WorkflowOperationException, EventCommentException {
    // Testing that a duplicate comment won't be created but a different one will still be created.
    Long workflowId = 10L;
    String mediaPackageId = "abc-def";
    String action = "create";
    String reason = "Waiting for Trim";
    String description = "The comment description";

    Organization org = createNiceMock(Organization.class);
    expect(org.getId()).andStubReturn("demo");
    replay(org);

    SecurityService secSrv = createNiceMock(SecurityService.class);
    expect(secSrv.getOrganization()).andStubReturn(org);
    replay(secSrv);

    // Setup WorkflowOperation Instance
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION))
            .andReturn(action).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON))
            .andReturn(reason).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION))
            .andReturn(description).anyTimes();

    // Setup mediaPackage
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(new IdImpl(mediaPackageId)).anyTimes();

    // Setup user
    User creator = EasyMock.createMock(User.class);

    // setup user directory
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyString())).andReturn(creator).anyTimes();

    // Setup WorkflowInstance
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowId).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(workflowOperationInstance).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCreatorName()).andReturn("user").anyTimes();

    // Test no previous comments
    EventCommentService eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<EventComment>());
    Capture<EventComment> comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(creator, userDirectoryService, eventCommentService, mediaPackage, workflowInstance,
            workflowOperationInstance);
    CommentWorkflowOperationHandler commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    // Test previous comment with same reason and description
    List<EventComment> comments = new ArrayList<EventComment>();
    comments.add(EventComment.create(Option.option(13L), mediaPackageId, org.getId(), description, creator, reason, true));
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments);
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    // Test previous comment with different reasons and descriptions
    comments = new ArrayList<EventComment>();
    comments.add(EventComment.create(Option.option(15L), mediaPackageId, org.getId(), "Different description", creator,
            reason, true));
    comments.add(EventComment.create(Option.option(15L), mediaPackageId, org.getId(), description, creator,
            "Different reason", true));
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments);
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(comment)))
            .andReturn(EventComment.create(Option.option(15L), mediaPackageId, org.getId(), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.setSecurityService(secSrv);
    commentWorkflowOperationHandler.setUserDirectoryService(userDirectoryService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
  }
}
