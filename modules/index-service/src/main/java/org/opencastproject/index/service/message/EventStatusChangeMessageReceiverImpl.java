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

import static org.opencastproject.index.service.impl.index.event.EventIndexUtils.getEvent;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.eventstatuschange.EventStatusChangeItem;
import org.opencastproject.message.broker.api.eventstatuschange.EventStatusChangeItem.Type;
import org.opencastproject.security.api.User;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventStatusChangeMessageReceiverImpl extends BaseMessageReceiverImpl<EventStatusChangeItem> {

  private static final Logger logger = LoggerFactory.getLogger(EventStatusChangeMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the event status change queue.
   */
  public EventStatusChangeMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(EventStatusChangeItem eventStatusChangeItem) {

    logger.debug("Received EventStatusChangeItem with type: {} and message: {} for these IDs: {}",
            eventStatusChangeItem.getType(), eventStatusChangeItem.getMessage(), eventStatusChangeItem.getEventIds());

    final String organization = getSecurityService().getOrganization().getId();
    final User user = getSecurityService().getUser();

    for (String eventId : eventStatusChangeItem.getEventIds()) {
      try {
        // Load the corresponding recording event
        final Event event = getEvent(eventId, organization, user, getSearchIndex());
        if (event == null) continue; // Event is not yet created and status cannot be set
        if (eventStatusChangeItem.getType() != Type.Starting) {
          logger.info("While creating a task for media package {}, the following error occurred: {}", eventId,
                  eventStatusChangeItem.getMessage());
          event.setWorkflowState(WorkflowState.FAILED);
        } else {
          event.setWorkflowState(WorkflowState.INSTANTIATED);
        }
        getSearchIndex().addOrUpdate(event);
      } catch (SearchIndexException e) {
        logger.error("Error retrieving or updating the recording event from/in the search index: {}", e.getMessage());
      }
    }
  }
}
