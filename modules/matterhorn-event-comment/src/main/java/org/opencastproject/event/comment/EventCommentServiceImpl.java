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
package org.opencastproject.event.comment;

import org.opencastproject.event.comment.persistence.EventCommentDatabaseService;
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
   * @param eventCommentDatabaseService
   *          the event comment database service
   */
  public void setEventCommentDatabaseService(EventCommentDatabaseService eventCommentDatabaseService) {
    this.eventCommentDatabaseService = eventCommentDatabaseService;
  }

  @Override
  public List<String> getReasons() throws EventCommentException {
    try {
      return eventCommentDatabaseService.getReasons();
    } catch (Exception e) {
      throw new EventCommentException(e);
    }
  }

  @Override
  public EventComment getComment(long commentId) throws NotFoundException, EventCommentException {
    try {
      return eventCommentDatabaseService.getComment(commentId);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new EventCommentException(e);
    }
  }

  @Override
  public void deleteComment(long commentId) throws NotFoundException, EventCommentException {
    try {
      eventCommentDatabaseService.deleteComment(commentId);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new EventCommentException(e);
    }
  }

  @Override
  public void deleteComments(String eventId) throws NotFoundException, EventCommentException {
    try {
      eventCommentDatabaseService.deleteComments(eventId);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new EventCommentException(e);
    }
  }

  @Override
  public EventComment updateComment(EventComment comment) throws EventCommentException {
    try {
      return eventCommentDatabaseService.updateComment(comment);
    } catch (Exception e) {
      throw new EventCommentException(e);
    }
  }

  @Override
  public List<EventComment> getComments(String eventId) throws EventCommentException {
    try {
      return eventCommentDatabaseService.getComments(eventId);
    } catch (Exception e) {
      throw new EventCommentException(e);
    }
  }

}
