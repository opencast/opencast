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
import org.opencastproject.comments.events.persistence.EventCommentDatabaseService;
import org.opencastproject.util.NotFoundException;

import java.util.List;

/**
 * Implements permanent storage for event comments.
 */
public class EventCommentServiceImpl implements EventCommentService {

  private EventCommentDatabaseService eventCommentDatabaseService;

  /**
   * OSGi callback to set the event comment database service.
   *
   * @param messageSender
   *          the message sender
   */
  public void setEventCommentDatabaseService(EventCommentDatabaseService eventCommentDatabaseService) {
    this.eventCommentDatabaseService = eventCommentDatabaseService;
  }

  @Override
  public List<String> getReasons() throws CommentException {
    try {
      return eventCommentDatabaseService.getReasons();
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  @Override
  public Comment getComment(String eventId, long commentId) throws NotFoundException, CommentException {
    try {
      return eventCommentDatabaseService.getComment(eventId, commentId);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  @Override
  public void deleteComment(String eventId, long commentId) throws NotFoundException, CommentException {
    try {
      eventCommentDatabaseService.deleteComment(eventId, commentId);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  @Override
  public Comment updateComment(String eventId, Comment comment) throws CommentException {
    try {
      return eventCommentDatabaseService.updateComment(eventId, comment);
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  @Override
  public List<Comment> getComments(String eventId) throws CommentException {
    try {
      return eventCommentDatabaseService.getComments(eventId);
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

}
