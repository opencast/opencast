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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
          event.setCreator(getSecurityService().getUser().getName());

          if (dc != null)
            EventIndexUtils.updateEvent(event, dc);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}",
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Update series name if not already done
        try {
          EventIndexUtils.updateSeriesName(event, organization, user, getSearchIndex());
        } catch (SearchIndexException e) {
          logger.error("Error updating the series name of the event to index: {}", ExceptionUtils.getStackTrace(e));
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
          logger.error("Error retrieving the recording event from the search index: {}",
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Persist the scheduling event
        updateEvent(event);
        return;
      case UpdateProperties:
        logger.debug("Received Update Properties");
        return;
      case UpdateOptOut:
        logger.debug("Received Update opt out status");

        // Load the corresponding recording event
        try {
          event = EventIndexUtils.getOrCreateEvent(schedulerItem.getMediaPackageId(), organization, user,
                  getSearchIndex());
          event.setOptedOut(schedulerItem.getOptOut());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}",
                  ExceptionUtils.getStackTrace(e));
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
          logger.error("Error retrieving the recording event from the search index: {}",
                  ExceptionUtils.getStackTrace(e));
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
          logger.debug("Scheduled recording {} removed from adminui search index", schedulerItem.getMediaPackageId());
        } catch (NotFoundException e) {
          logger.warn("Scheduled recording {} not found for deletion", schedulerItem.getMediaPackageId());
        } catch (SearchIndexException e) {
          logger.error("Error deleting the recording event from the search index: {}", ExceptionUtils.getStackTrace(e));
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
      logger.debug("Scheduled recording {} updated in the adminui search index", event.getIdentifier());
    } catch (SearchIndexException e) {
      logger.error("Error retrieving the recording event from the search index: {}", ExceptionUtils.getStackTrace(e));
    }
  }
}
