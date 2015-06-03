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

import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentException;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A workflow operation handler for creating, resolving and deleting comments automatically during the workflow process.
 */
public class CommentWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  protected static final String ACTION = "action";
  protected static final String DESCRIPTION = "description";
  protected static final String REASON = "reason";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CommentWorkflowOperationHandler.class);

  /** The event comment service instance */
  private EventCommentService eventCommentService;

  /** The workflow service instance */
  protected WorkflowService workflowService = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  public enum Operation {
    create, resolve, delete
  };

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(REASON,
                    "The optional comment reason's i18n id. You can find the id in etc/listproviders/event.comment.reasons.properties");
    CONFIG_OPTIONS.put(DESCRIPTION, "The description text to add to the comment.");
    CONFIG_OPTIONS.put(ACTION,
                    "Options are "
                            + StringUtils.join(Operation.values(), ",")
                            + ". Creates a new comment, marks a comment as resolved or deletes a comment that matches the same description and reason. By default creates.");
  }

  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running comment workflow operation on workflow {}", workflowInstance.getId());
    try {
      return handleCommentOperation(workflowInstance);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Determine the type of operation to do on a comment and execute it.
   *
   * @param workflowInstance
   *          The {@link WorkflowInstance} to be handled.
   * @return The result of handling the {@link WorkflowInstance}
   * @throws CommentException
   *           Thrown if there is an issue creating, resolving or deleting a comment
   * @throws NotFoundException
   *           Thrown if the comment cannot be found to delete.
   */
  private WorkflowOperationResult handleCommentOperation(WorkflowInstance workflowInstance) throws CommentException,
          NotFoundException {
    Date date = new Date();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    Operation action;
    String inputAction = StringUtils.trimToNull(operation.getConfiguration(ACTION));
    if (inputAction == null) {
      action = Operation.create;
    } else {
      action = Operation.valueOf(inputAction.toLowerCase());
    }
    String reason = StringUtils.trimToNull(operation.getConfiguration(REASON));
    String description = StringUtils.trimToNull(operation.getConfiguration(DESCRIPTION));
    switch (action) {
      case create:
        createComment(workflowInstance, reason, description);
        break;
      case resolve:
        resolveComment(workflowInstance, reason, description);
        break;
      case delete:
        deleteComment(workflowInstance, reason, description);
        break;
      default:
        logger.warn(
                "Unknown action '{}' for comment with description '{}' and reason '{}'. It should be one of the following: ",
                new Object[] { inputAction, description, reason, StringUtils.join(Operation.values(), ",") });
    }
    WorkflowOperationResult result = createResult(workflowInstance.getMediaPackage(), Action.CONTINUE,
            (new Date().getTime()) - date.getTime());
    return result;
  }

  /**
   * Create a new comment if one doesn't already exist with the reason and description.
   *
   * @param workflowInstance
   *          The {@link WorkflowInstance} to handle.
   * @param reason
   *          The reason for the comment.
   * @param description
   *          The descriptive text of the comment.
   * @throws CommentException
   *           Thrown if unable to create the comment.
   */
  private void createComment(WorkflowInstance workflowInstance, String reason, String description)
          throws CommentException {
    Opt<Comment> optComment = findComment(workflowInstance.getMediaPackage().getIdentifier().toString(), reason,
            description);
    if (optComment.isNone()) {
      Comment comment = Comment.create(Option.<Long> none(), description, workflowInstance.getCreator(), reason, false);
      eventCommentService.updateComment(workflowInstance.getMediaPackage().getIdentifier().toString(), comment);
    } else {
      logger.warn("Not creating comment with '{}' text and '{}' reason as it already exists for this event.",
              description, reason);
    }
  }

  /**
   * Resolve a comment with matching reason and description.
   *
   * @param workflowInstance
   *          The {@link WorkflowInstance} to handle.
   * @param reason
   *          The reason for the comment.
   * @param description
   *          The comment's descriptive text.
   * @throws CommentException
   *           Thrown if unable to update the comment.
   */
  private void resolveComment(WorkflowInstance workflowInstance, String reason, String description)
          throws CommentException {
    Opt<Comment> optComment = findComment(workflowInstance.getMediaPackage().getIdentifier().toString(), reason,
            description);
    if (optComment.isSome()) {
      Comment comment = Comment.create(optComment.get().getId(), optComment.get().getText(), optComment.get()
              .getAuthor(), optComment.get().getReason(), true, optComment.get().getCreationDate(), optComment.get()
              .getModificationDate(), optComment.get().getReplies());
      eventCommentService.updateComment(workflowInstance.getMediaPackage().getIdentifier().toString(), comment);
    } else {
      logger.warn("Not resolving comment with '{}' text and '{}' reason as it doesn't exist.", description, reason);
    }
  }

  /**
   * Delete a comment with matching reason and description.
   *
   * @param workflowInstance
   *          The {@link WorkflowInstance} to handle.
   * @param reason
   *          The reason for the comment.
   * @param description
   *          The comment's descriptive text.
   * @throws CommentException
   *           Thrown if unable to delete the comment.
   * @throws NotFoundException
   *           Thrown if unable to find the comment.
   */
  private void deleteComment(WorkflowInstance workflowInstance, String reason, String description)
          throws CommentException, NotFoundException {
    Opt<Comment> optComment = findComment(workflowInstance.getMediaPackage().getIdentifier().toString(), reason,
            description);
    if (optComment.isSome()) {
      try {
        eventCommentService.deleteComment(workflowInstance.getMediaPackage().getIdentifier().toString(), optComment
                .get().getId().get());
      } catch (NotFoundException e) {
        logger.warn("Not deleting comment with '{}' text and '{}' reason and id '{}' as it doesn't exist.",
                new Object[] { description, reason, optComment.get().getId() });
      }
    } else {
      logger.warn("Not deleting comment with '{}' text and '{}' reason as it doesn't exist.", description, reason);
    }
  }

  /**
   * Find a comment by its reason and description.
   *
   * @param eventId
   *          The event id to search the comments for.
   * @param reason
   *          The reason for the comment.
   * @param description
   *          The description for the comment.
   * @return The comment if one is found matching the reason and description.
   * @throws CommentException
   *           Thrown if there was a problem finding the comment.
   */
  private Opt<Comment> findComment(String eventId, String reason, String description) throws CommentException {
    Opt<Comment> comment = Opt.none();
    List<Comment> eventComments = eventCommentService.getComments(eventId);
    for (Comment existingComment : eventComments) {
      if (isSameComment(existingComment, reason, description)) {
        comment = Opt.some(existingComment);
        break;
      }
    }
    return comment;
  }

  /**
   * Determines if a comment has a given reason and description.
   *
   * @param comment
   *          The comment to compare.
   * @param reason
   *          The reason for the comment.
   * @param description
   *          The description for the comment.
   * @return True if the two properties match.
   */
  private boolean isSameComment(Comment comment, String reason, String description) {
    return description == null ? comment.getText() == null : description.equals(comment.getText())
            && (reason == null ? comment.getReason() == null : reason.equals(comment.getReason()));
  }

  /**
   * Callback from the OSGi environment that will pass a reference to the workflow service upon component activation.
   *
   * @param service
   *          the workflow service
   */
  public void setEventCommentService(EventCommentService eventCommentService) {
    this.eventCommentService = eventCommentService;
  }

  /**
   * Callback from the OSGi environment that will pass a reference to the workflow service upon component activation.
   *
   * @param service
   *          the workflow service
   */
  void setWorkflowService(WorkflowService service) {
    this.workflowService = service;
  }

}
