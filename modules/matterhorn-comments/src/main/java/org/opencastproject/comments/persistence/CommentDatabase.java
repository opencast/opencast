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
package org.opencastproject.comments.persistence;

import org.opencastproject.comments.Comment;
import org.opencastproject.util.NotFoundException;

/**
 * API that defines persistent storage of comments.
 */
public interface CommentDatabase {

  /**
   * Store (or update) a comment.
   * 
   * @param comment
   *          the comment
   * @return the stored or updated comment
   * @throws CommentDatabaseException
   *           if exception occurs
   */
  CommentDto storeComment(Comment comment) throws CommentDatabaseException;

  /**
   * Removes comments from persistent storage.
   * 
   * @param commentId
   *          ID of the comment to be removed
   * @throws CommentDatabaseException
   *           if exception occurs
   * @throws NotFoundException
   *           if comment with specified ID is not found
   */
  void deleteComment(long commentId) throws CommentDatabaseException, NotFoundException;

  /**
   * Gets a single comment by its identifier.
   * 
   * @param commentId
   *          the comment identifier
   * @return the comment
   * @throws NotFoundException
   *           if there is no comment with this identifier
   * @throws CommentDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  CommentDto getComment(long commentId) throws NotFoundException, CommentDatabaseException;

}
