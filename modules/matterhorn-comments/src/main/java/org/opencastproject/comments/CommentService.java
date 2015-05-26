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

package org.opencastproject.comments;

import org.opencastproject.util.NotFoundException;

/**
 * Comment service API for creating, removing and searching over comments.
 */
public interface CommentService {

  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.system.comment";

  /**
   * Returns the comment by its ID.
   * 
   * @param commentId
   *          comment to be retrieved
   * @return the comment
   * @throws CommentException
   *           if retrieving fails
   * @throws NotFoundException
   *           if comment with specified ID does not exist
   */
  Comment getComment(long commentId) throws CommentException, NotFoundException;

  /**
   * Adds or updates a comment.
   * 
   * @param comment
   *          the comment to update
   * @return the updated comment
   * @throws CommentException
   *           if adding or updating fails
   */
  Comment updateComment(Comment comment) throws CommentException;

  /**
   * Removes comment
   * 
   * @param commentId
   *          ID of the comment to be removed
   * @throws CommentException
   *           if deleting fails
   * @throws NotFoundException
   *           if comment with specified ID does not exist
   */
  void deleteComment(long commentId) throws CommentException, NotFoundException;

}
