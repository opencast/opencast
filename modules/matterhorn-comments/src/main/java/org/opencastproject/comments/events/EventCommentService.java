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
package org.opencastproject.comments.events;

import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentException;
import org.opencastproject.util.NotFoundException;

import java.util.List;

public interface EventCommentService {
  /**
   * Get the available reasons for commenting on an event.
   *
   * @return A list of reasons of why a comment was made.
   * @throws CommentException
   *           Thrown if there was an issue getting the comment reasons.
   */
  List<String> getReasons() throws CommentException;

  /**
   * Get a comment for a particular event.
   *
   * @param eventId
   *          The id of the event (mediapackage id)
   * @param commentId
   *          The id for the comment.
   * @return The comment
   * @throws NotFoundException
   *           Thrown if the comment cannot be found.
   * @throws CommentException
   *           Thrown if there was an issue getting the comment.
   */
  Comment getComment(String eventId, long commentId) throws NotFoundException, CommentException;

  /**
   * Get all of the comments for an event.
   *
   * @param eventId
   *          The id of the event to get the comments for (mediapackage id).
   * @return The {@link List} of comments.
   * @throws CommentException
   *           Thrown if there was a problem getting the comments.
   */
  List<Comment> getComments(String eventId) throws CommentException;

  /**
   * Delete a comment from an event.
   *
   * @param eventId
   *          The id of the event (mediapackage id).
   * @param commentId
   *          The id of the comment.
   * @throws NotFoundException
   *           Thrown if cannot find the event / comment.
   * @throws CommentException
   *           Thrown if there is a problem deleting the comment.
   */
  void deleteComment(String eventId, long commentId) throws NotFoundException, CommentException;

  /**
   * Update a comment.
   *
   * @param eventId
   *          The id of the event this comment is attached to (mediapackge id).
   * @param comment
   *          The new comment status to update to.
   * @return The comment updated.
   * @throws CommentException
   *           Thrown if there is a problem updating the comment.
   */
  Comment updateComment(String eventId, Comment comment) throws CommentException;
}
