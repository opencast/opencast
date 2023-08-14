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
package org.opencastproject.event.comment.persistence;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.util.NotFoundException;

import java.util.List;

public interface EventCommentDatabaseService {
  /**
   * Get the available reasons for commenting on an event.
   *
   * @return A list of reasons of why a comment was made.
   * @throws EventCommentDatabaseException
   *           Thrown if there was an issue getting the comment reasons.
   */
  List<String> getReasons() throws EventCommentDatabaseException;

  /**
   * Get a comment for a particular event.
   * @param commentId
   *          The id for the comment.
   *
   * @return The comment
   * @throws NotFoundException
   *           Thrown if the comment cannot be found.
   * @throws EventCommentDatabaseException
   *           Thrown if there was an issue getting the comment from the database.
   */
  EventComment getComment(long commentId) throws NotFoundException, EventCommentDatabaseException;

  /**
   * Get all of the comments for an event.
   *
   * @param eventId
   *          The id of the event to get the comments for (mediapackage id).
   * @return The {@link List} of comments.
   * @throws EventCommentDatabaseException
   *           Thrown if there was a problem getting the comments from the database.
   */
  List<EventComment> getComments(String eventId) throws EventCommentDatabaseException;

  /**
   * Delete a comment from an event.
   * @param commentId
   *          The id of the comment.
   *
   * @throws NotFoundException
   *           Thrown if cannot find the event / comment.
   * @throws EventCommentDatabaseException
   *           Thrown if there is a problem deleting the comment.
   */
  void deleteComment(long commentId) throws NotFoundException, EventCommentDatabaseException;

  /**
   * Delete all comments from an event.
   * @param eventId
   *          The id of the event.
   *
   * @throws NotFoundException
   *           Thrown if cannot find the event.
   * @throws EventCommentDatabaseException
   *           Thrown if there is a problem deleting the comments.
   */
  void deleteComments(String eventId) throws NotFoundException, EventCommentDatabaseException;

  /**
   * Update a comment.
   * @param comment
   *          The new comment status to update to.
   *
   * @return The comment updated.
   * @throws EventCommentDatabaseException
   *           Thrown if there is a problem updating the comment.
   */
  EventComment updateComment(EventComment comment) throws EventCommentDatabaseException;
}
