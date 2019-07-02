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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.index.service.impl.index.event.EventIndexUtils.getOrCreateEvent;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.scheduler.SchedulerItem;
import org.opencastproject.message.broker.api.scheduler.SchedulerItemList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SchedulerMessageReceiverImpl extends BaseMessageReceiverImpl<SchedulerItemList> {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the scheduler queue.
   */
  public SchedulerMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(SchedulerItemList messageContent) {
    for (SchedulerItem item : messageContent.getItems()) {
      executeSingle(messageContent.getId(), item);
    }
  }

  private void executeSingle(final String mediaPackageId, final SchedulerItem schedulerItem) {
    DublinCoreCatalog dc = schedulerItem.getEvent();

    Event event;
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    logger.debug("Received message of type {} for event {}", schedulerItem.getType(), mediaPackageId);

    try {
      switch (schedulerItem.getType()) {
        case UpdateCatalog:
          // Load or create the corresponding recording event
          event = getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          if (isBlank(event.getCreator()))
            event.setCreator(getSecurityService().getUser().getName());
          if (dc != null)
            EventIndexUtils.updateEvent(event, dc);

          // Update series name if not already done
          EventIndexUtils.updateSeriesName(event, organization, user, getSearchIndex());

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdateAcl:
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setAccessPolicy(AccessControlParser.toJsonSilent(schedulerItem.getAcl()));

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdateAgentId:
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setAgentId(schedulerItem.getAgentId());

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdateProperties:
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setAgentConfiguration(schedulerItem.getProperties());

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdateRecordingStatus:
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setRecordingStatus(schedulerItem.getRecordingState());

          // Persist the scheduling event
          updateEvent(event);
          return;
        case DeleteRecordingStatus:
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setRecordingStatus(null);

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdateEnd:
          String endTime = schedulerItem.getEnd() == null ? null : DateTimeSupport.toUTC(schedulerItem.getEnd().getTime());
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setTechnicalEndTime(endTime);

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdateStart:
          String startTime = schedulerItem.getStart() == null ? null : DateTimeSupport.toUTC(schedulerItem.getStart().getTime());
          // Load the corresponding recording event
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setTechnicalStartTime(startTime);

          // Persist the scheduling event
          updateEvent(event);
          return;
        case UpdatePresenters:
          event = EventIndexUtils.getOrCreateEvent(mediaPackageId, organization, user, getSearchIndex());
          event.setTechnicalPresenters(new ArrayList<>(schedulerItem.getPresenters()));

          // Persist the scheduling event
          updateEvent(event);
          return;
        case Delete:
          // Remove the scheduling from the search index
          try {
            getSearchIndex().deleteScheduling(organization, user, mediaPackageId);
            logger.debug("Scheduled recording {} removed from the {} search index",
              mediaPackageId, getSearchIndex().getIndexName());
          } catch (NotFoundException e) {
            logger.warn("Scheduled recording {} not found for deletion", mediaPackageId);
          }
          return;
        default:
          throw new IllegalArgumentException("Unhandled type of SchedulerItem");
      }
    } catch (SearchIndexException e) {
      logger.error("Got {} message for event {} but failed to retrieve the event from the search index",
              schedulerItem.getType(), mediaPackageId, e);
    }
  }

  private void updateEvent(Event event) {
    try {
      getSearchIndex().addOrUpdate(event);
      logger.debug("Scheduled recording {} updated in the {} search index",
        event.getIdentifier(), getSearchIndex().getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
    }
  }
}
