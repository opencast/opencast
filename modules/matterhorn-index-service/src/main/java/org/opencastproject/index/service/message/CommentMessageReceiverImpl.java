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

package org.opencastproject.index.service.message;

import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.comments.CommentItem;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommentMessageReceiverImpl extends BaseMessageReceiverImpl<CommentItem> {

  private static final Logger logger = LoggerFactory.getLogger(CommentMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the comment queue.
   */
  public CommentMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(CommentItem commentItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (commentItem.getType()) {
      case Update:
        logger.debug("Received Comment update");

        try {
          EventIndexUtils.updateComments(commentItem.getEventId(), commentItem.hasComments(),
                  commentItem.hasOpenComments(), organization, user, getSearchIndex());
          logger.debug("Event {} comment status updated from adminui search index", commentItem.getEventId());
        } catch (SearchIndexException e) {
          logger.error("Error updating comment status of event {} from the search index: {}", commentItem.getEventId(),
                  ExceptionUtils.getStackTrace(e));
        } catch (NotFoundException e) {
          logger.warn("Event {} not found for comment status updating", commentItem.getEventId());
        }
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of CommentItem");
    }
  }
}
