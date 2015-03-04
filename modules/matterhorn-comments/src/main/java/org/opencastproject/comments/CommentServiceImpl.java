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
package org.opencastproject.comments;

import org.opencastproject.comments.persistence.CommentDatabase;
import org.opencastproject.comments.persistence.CommentDatabaseException;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link CommentService}. Uses {@link CommentDatabase} for permanent storage for searching.
 */
public class CommentServiceImpl implements CommentService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

  /** Persistent storage */
  protected CommentDatabase persistence;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** OSGi callback for setting persistance. */
  public void setPersistence(CommentDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * OSGi callback to set the user directory service.
   * 
   * @param userDirectoryService
   *          the user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Activates Comment Service.
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Comment Service");
  }

  @Override
  public Comment getComment(long commentId) throws CommentException, NotFoundException {
    try {
      return persistence.getComment(commentId).toComment(userDirectoryService);
    } catch (CommentDatabaseException e) {
      logger.error("Exception occured while retrieving comment {}: {}", commentId, ExceptionUtils.getStackTrace(e));
      throw new CommentException(e);
    }
  }

  @Override
  public Comment updateComment(Comment comment) throws CommentException {
    try {
      return persistence.storeComment(comment).toComment(userDirectoryService);
    } catch (Exception e) {
      logger.error("Unable to update the comment {}: {}", comment, ExceptionUtils.getStackTrace(e));
      throw new CommentException(e);
    }
  }

  @Override
  public void deleteComment(long commentId) throws CommentException, NotFoundException {
    try {
      persistence.deleteComment(commentId);
    } catch (CommentDatabaseException e) {
      logger.error("Could not delete comment with id {} from persistence storage", commentId);
      throw new CommentException(e);
    }
  }

}
