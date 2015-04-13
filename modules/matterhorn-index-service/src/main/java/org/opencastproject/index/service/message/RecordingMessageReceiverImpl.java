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
package org.opencastproject.index.service.message;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.agent.RecordingItem;
import org.opencastproject.security.api.User;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingMessageReceiverImpl extends BaseMessageReceiverImpl<RecordingItem> {

  private static final Logger logger = LoggerFactory.getLogger(RecordingMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the destination of the recording queue.
   */
  public RecordingMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(RecordingItem recordingItem) {
    Event event = null;
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (recordingItem.getType()) {
      case Update:
        logger.debug("Received Update Recording {}", recordingItem.getEventId());

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(recordingItem.getEventId(), organization, user, getSearchIndex());
          event.setRecordingStatus(recordingItem.getState());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}",
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case Delete:
        logger.debug("Received Delete Recording {}", recordingItem.getEventId());

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(recordingItem.getEventId(), organization, user, getSearchIndex());
          event.setRecordingStatus(null);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}",
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of RecordingItem");
    }
  }

  private void updateEvent(Event event) {
    try {
      getSearchIndex().addOrUpdate(event);
      logger.debug("Scheduled recording {} updated in the {} search index", event.getIdentifier(), getSearchIndex()
              .getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error retrieving the recording event from the search index: {}", ExceptionUtils.getStackTrace(e));
    }
  }

}
