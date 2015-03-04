/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler.comments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentException;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.User;
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
  public void testPossibleActions() throws WorkflowOperationException, CommentException, NotFoundException {
    // Testing that a duplicate comment won't be created but a different one will still be created.
    Long workflowId = 10L;
    Long deleteCommentId = 21L;
    String mediaPackageId = "abc-def";
    String reason = "Waiting for Trim";
    String description = "The comment description";

    // Setup WorkflowOperation Instance
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    // Create
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(CommentWorkflowOperationHandler.Operation.create.toString());
    // Resolve
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(CommentWorkflowOperationHandler.Operation.resolve.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(CommentWorkflowOperationHandler.Operation.resolve.toString());
    // Deletes
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(CommentWorkflowOperationHandler.Operation.delete.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(CommentWorkflowOperationHandler.Operation.delete.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(CommentWorkflowOperationHandler.Operation.delete.toString());
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON)).andReturn(reason).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION)).andReturn(description).anyTimes();

    // Setup mediaPackage
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(new IdImpl(mediaPackageId)).anyTimes();

    // Setup user
    User creator = EasyMock.createMock(User.class);

    // Setup WorkflowInstance
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowId).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(workflowOperationInstance).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCreator()).andReturn(creator).anyTimes();

    EasyMock.replay(creator, mediaPackage, workflowInstance, workflowOperationInstance);

    // Test create
    EventCommentService eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>()).anyTimes();
    Capture<String> eventId = EasyMock.newCapture();
    Capture<Comment> comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(15L), description, creator));
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(17L), description, creator));
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(19L), description, creator));
    EasyMock.replay(eventCommentService);
    CommentWorkflowOperationHandler commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
    assertEquals(false, comment.getValue().isResolvedStatus());

    // Test resolve
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    List<Comment> comments = new ArrayList<Comment>();
    comments.add(Comment.create(Option.option(deleteCommentId), description, creator, reason, false));
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes();
    eventId = EasyMock.newCapture();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(17L), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
    assertEquals(true, comment.getValue().isResolvedStatus());

    // Test resolve with no comment
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>()).anyTimes();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);

    // Test delete with no result, no delete
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>()).anyTimes();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);

    // Test delete with result
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    comments = new ArrayList<Comment>();
    comments.add(Comment.create(Option.option(deleteCommentId), description, creator, reason, false));
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes();
    eventCommentService.deleteComment(mediaPackageId, deleteCommentId);
    EasyMock.expectLastCall();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);

    // Test delete with unrelated comments
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    comments = new ArrayList<Comment>();
    comments.add(Comment.create(Option.option(35L), description, creator, "", false));
    comments.add(Comment.create(Option.option(37L), "Different Description", creator, reason, false));
    comments.add(Comment.create(Option.option(39L), description, creator, "Different Reason", false));
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments).anyTimes();
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
  }

  @Test
  public void testDifferentCaseAction() throws WorkflowOperationException, CommentException {
    // Testing that a duplicate comment won't be created but a different one will still be created.
    Long workflowId = 10L;
    String mediaPackageId = "abc-def";
    String reason = "Waiting for Trim";
    String description = "The comment description";

    // Setup WorkflowOperation Instance
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    // Standard
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn("create");
    // Mixed case
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn("CrEaTe");
    // All Caps
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn("CREATE");
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON)).andReturn(reason).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION)).andReturn(description).anyTimes();

    // Setup mediaPackage
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(new IdImpl(mediaPackageId)).anyTimes();

    // Setup user
    User creator = EasyMock.createMock(User.class);

    // Setup WorkflowInstance
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowId).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(workflowOperationInstance).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCreator()).andReturn(creator).anyTimes();

    EasyMock.replay(creator, mediaPackage, workflowInstance, workflowOperationInstance);

    // Test no previous comments
    EventCommentService eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>()).anyTimes();
    Capture<String> eventId = EasyMock.newCapture();
    Capture<Comment> comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(15L), description, creator));
    EasyMock.replay(eventCommentService);
    CommentWorkflowOperationHandler commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);

    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>()).anyTimes();
    eventId = EasyMock.newCapture();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(17L), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);

    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>()).anyTimes();
    eventId = EasyMock.newCapture();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(19L), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);

    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
  }

  @Test
  public void testDuplicateComments() throws WorkflowOperationException, CommentException {
    // Testing that a duplicate comment won't be created but a different one will still be created.
    Long workflowId = 10L;
    String mediaPackageId = "abc-def";
    String action = "create";
    String reason = "Waiting for Trim";
    String description = "The comment description";

    // Setup WorkflowOperation Instance
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createMock(WorkflowOperationInstance.class);
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.ACTION)).andReturn(action).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.REASON)).andReturn(reason).anyTimes();
    EasyMock.expect(workflowOperationInstance.getConfiguration(CommentWorkflowOperationHandler.DESCRIPTION)).andReturn(description).anyTimes();

    // Setup mediaPackage
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(new IdImpl(mediaPackageId)).anyTimes();

    // Setup user
    User creator = EasyMock.createMock(User.class);

    // Setup WorkflowInstance
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowId).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(workflowOperationInstance).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCreator()).andReturn(creator).anyTimes();

    // Test no previous comments
    EventCommentService eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(new ArrayList<Comment>());
    Capture<String> eventId = EasyMock.newCapture();
    Capture<Comment> comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(15L), description, creator));
    EasyMock.replay(creator, eventCommentService, mediaPackage, workflowInstance, workflowOperationInstance);
    CommentWorkflowOperationHandler commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    // Test previous comment with same reason and description
    List<Comment> comments = new ArrayList<Comment>();
    comments.add(Comment.create(Option.option(13L), description, creator, reason, true));
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments);
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());

    // Test previous comment with different reasons and descriptions
    comments = new ArrayList<Comment>();
    comments.add(Comment.create(Option.option(15L), "Different description", creator, reason, true));
    comments.add(Comment.create(Option.option(15L), description, creator, "Different reason", true));
    eventCommentService = EasyMock.createMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(mediaPackageId)).andReturn(comments);
    eventId = EasyMock.newCapture();
    comment = EasyMock.newCapture();
    EasyMock.expect(eventCommentService.updateComment(EasyMock.capture(eventId), EasyMock.capture(comment))).andReturn(Comment.create(Option.option(15L), description, creator));
    EasyMock.replay(eventCommentService);
    commentWorkflowOperationHandler = new CommentWorkflowOperationHandler();
    commentWorkflowOperationHandler.setEventCommentService(eventCommentService);
    commentWorkflowOperationHandler.start(workflowInstance, null);
    assertTrue(eventId.hasCaptured());
    assertEquals(mediaPackageId, eventId.getValue());
    assertTrue(comment.hasCaptured());
    assertEquals(creator, comment.getValue().getAuthor());
    assertEquals(description, comment.getValue().getText());
    assertEquals(reason, comment.getValue().getReason());
  }
}
