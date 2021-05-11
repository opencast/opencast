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
import static org.opencastproject.message.broker.api.scheduler.SchedulerItem.Type.Delete;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventIndexUtils;
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
import java.util.Optional;
import java.util.function.Function;

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
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    logger.debug("Received message of type {} for event {}", schedulerItem.getType(), mediaPackageId);
    Function<Optional<Event>, Optional<Event>> updateFunction;

    if (schedulerItem.getType() == Delete) {

      // Remove the scheduling from the search index
      try {
        getSearchIndex().deleteScheduling(organization, user, mediaPackageId);
        logger.debug("Scheduled recording {} removed from the {} search index", mediaPackageId, getSearchIndex().getIndexName());
      } catch (NotFoundException e) {
        logger.warn("Scheduled recording {} not found for deletion", mediaPackageId);
      } catch (SearchIndexException e) {
        logger.error("Error deleting the event {} from the search index:", mediaPackageId, e);
      }
    } else {
      updateFunction = (Optional<Event> eventOpt) -> {
        Event event = eventOpt.orElse(new Event(mediaPackageId, organization));

        switch (schedulerItem.getType()) {
          case UpdateCatalog:
            if (isBlank(event.getCreator()))
              event.setCreator(getSecurityService().getUser().getName());
            if (dc != null)
              EventIndexUtils.updateEvent(event, dc);

            // Update series name if not already done
            try {
              EventIndexUtils.updateSeriesName(event, organization, user, getSearchIndex());
            } catch (SearchIndexException e) {
              logger.error("Error updating the series name of the event to index", e);
            }
            break;
          case UpdateAcl:
            event.setAccessPolicy(AccessControlParser.toJsonSilent(schedulerItem.getAcl()));
            break;
          case UpdateAgentId:
            event.setAgentId(schedulerItem.getAgentId());
            break;
          case UpdateProperties:
            event.setAgentConfiguration(schedulerItem.getProperties());
            break;
          case UpdateRecordingStatus:
            event.setRecordingStatus(schedulerItem.getRecordingState());
            break;
          case DeleteRecordingStatus:
            event.setRecordingStatus(null);
            break;
          case UpdateEnd:
            String endTime = schedulerItem.getEnd() == null ? null
                    : DateTimeSupport.toUTC(schedulerItem.getEnd().getTime());
            event.setTechnicalEndTime(endTime);
            break;
          case UpdateStart:
            String startTime = schedulerItem.getStart() == null ? null
                    : DateTimeSupport.toUTC(schedulerItem.getStart().getTime());
            event.setTechnicalStartTime(startTime);
            break;
          case UpdatePresenters:
            event.setTechnicalPresenters(new ArrayList<>(schedulerItem.getPresenters()));
            break;
          default:
            throw new IllegalArgumentException("Unhandled type of SchedulerItem");
        }
        return Optional.of(event);
      };

      try {
        getSearchIndex().addOrUpdateEvent(mediaPackageId, updateFunction, organization, user);
        logger.debug("Scheduled recording {} updated in the {} search index", mediaPackageId,
                getSearchIndex().getIndexName());
      } catch (SearchIndexException e) {
        logger.error("Error updating the event {} in the search index:", mediaPackageId, e);
      }
    }
  }
}
