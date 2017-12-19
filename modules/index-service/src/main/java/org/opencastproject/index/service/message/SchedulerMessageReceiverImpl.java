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
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SchedulerMessageReceiverImpl extends BaseMessageReceiverImpl<SchedulerItem> {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the scheduler queue.
   */
  public SchedulerMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(SchedulerItem schedulerItem) {
    DublinCoreCatalog dc = schedulerItem.getEvent();

    Event event = null;
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (schedulerItem.getType()) {
      case UpdateCatalog:
        logger.debug("Received Update Catalog");

        // Load or create the corresponding recording event
        try {
          event = getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user, getSearchIndex());
          if (isBlank(event.getCreator()))
            event.setCreator(getSecurityService().getUser().getName());
          if (event.getBlacklisted() == null)
            event.setBlacklisted(false);
          if (event.getOptedOut() == null)
            event.setOptedOut(false);

          if (dc != null)
            EventIndexUtils.updateEvent(event, dc);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }

        // Update series name if not already done
        try {
          EventIndexUtils.updateSeriesName(event, organization, user, getSearchIndex());
        } catch (SearchIndexException e) {
          logger.error("Error updating the series name of the event to index: {}", getStackTrace(e));
        }

        // Persist the scheduling event
        updateEvent(event);
        break;
      case UpdateAcl:
        logger.debug("Received Update ACL");

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setAccessPolicy(AccessControlParser.toJsonSilent(schedulerItem.getAcl()));
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateAgentId:
        logger.debug("Received update event '{}' with agent id to '{}'", schedulerItem.getId(),
                schedulerItem.getAgentId());
        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setAgentId(schedulerItem.getAgentId());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }
        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateProperties:
        logger.debug("Received update event '{}' CA Properties '{}'", schedulerItem.getId(),
                schedulerItem.getProperties());
        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setAgentConfiguration(schedulerItem.getProperties());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }
        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateOptOut:
        logger.debug("Received Update opt out status");

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setOptedOut(schedulerItem.getOptOut());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateBlacklist:
        logger.debug("Received Update blacklist status");

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setBlacklisted(schedulerItem.getBlacklisted());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", e.getMessage());
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateReviewStatus:
        logger.debug("Received Update review status");

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setReviewStatus(schedulerItem.getReviewStatus());
          if (schedulerItem.getReviewDate() != null)
            event.setReviewDate(DateTimeSupport.toUTC(schedulerItem.getReviewDate().getTime()));
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateRecordingStatus:
        logger.debug("Received Update Recording {}", schedulerItem.getMediaPackageId());

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setRecordingStatus(schedulerItem.getRecordingState());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case DeleteRecordingStatus:
        logger.debug("Received Delete recording status {}", schedulerItem.getMediaPackageId());

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setRecordingStatus(null);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateEnd:
        String endTime = schedulerItem.getEnd() == null ? null : DateTimeSupport.toUTC(schedulerItem.getEnd().getTime());
        logger.debug("Received update event '{}' end time '{}'", schedulerItem.getId(), endTime);
        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setTechnicalEndTime(endTime);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }
        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateStart:
        String startTime = schedulerItem.getStart() == null ? null : DateTimeSupport.toUTC(schedulerItem.getStart().getTime());
        logger.debug("Received update event '{}' start time '{}'", schedulerItem.getId(), startTime);
        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setTechnicalStartTime(startTime);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }
        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdatePresenters:
        logger.debug("Received update event '{}' with presenters '{}'", schedulerItem.getId(),
                schedulerItem.getPresenters());
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setTechnicalPresenters(new ArrayList<>(schedulerItem.getPresenters()));
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", getStackTrace(e));
          return;
        }
        // Persist the scheduling event
        updateEvent(event);
        return;
      case Delete:
        logger.debug("Received Delete Event {}", schedulerItem.getMediaPackageId());

        // Remove the scheduling from the search index
        try {
          getSearchIndex().deleteScheduling(organization, user, schedulerItem.getMediaPackageId());
          logger.debug("Scheduled recording {} removed from the {} search index",
            schedulerItem.getMediaPackageId(), getSearchIndex().getIndexName());
        } catch (NotFoundException e) {
          logger.warn("Scheduled recording {} not found for deletion", schedulerItem.getMediaPackageId());
        } catch (SearchIndexException e) {
          logger.error("Error deleting the recording event from the search index: {}", getStackTrace(e));
          return;
        }
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of SchedulerItem");
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
