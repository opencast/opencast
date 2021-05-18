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

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.acl.AclItem;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

public class AclMessageReceiverImpl extends BaseMessageReceiverImpl<AclItem> {

  private static final Logger logger = LoggerFactory.getLogger(AclMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the acl queue.
   */
  public AclMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  private void updateManagedAclForSeries(String managedAcl, Optional<String> newManagedAclOpt, String organization,
          User user) {
    SearchResult<Series> result;
    try {
      result = getSearchIndex().getByQuery(new SeriesSearchQuery(organization, user).withoutActions()
              .withManagedAcl(managedAcl));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the series in org '{}' with current managed acl name '{}'", organization, managedAcl,
              e);
      return;
    }

    for (SearchResultItem<Series> seriesItem : result.getItems()) {
      String seriesId = seriesItem.getSource().getIdentifier();

      Function<Optional<Series>, Optional<Series>> updateFunction = (Optional<Series> seriesOpt) -> {
        if (seriesOpt.isPresent() && seriesOpt.get().getManagedAcl().equals(managedAcl)) {
          Series series = seriesOpt.get();
          series.setManagedAcl(newManagedAclOpt.orElse(null));
          return Optional.of(series);
        }
        return Optional.empty();
      };

      try {
        getSearchIndex().addOrUpdateSeries(seriesId, updateFunction, organization, user);
      } catch (SearchIndexException e) {
        if (newManagedAclOpt.isPresent()) {
          logger.warn("Unable to update series'{}' from current managed acl '{}' to new managed acl name '{}'",
                  seriesId, managedAcl, newManagedAclOpt.get(), e);
        } else {
          logger.warn("Unable to update series '{}' to remove managed acl '{}'", seriesId, managedAcl, e);
        }
      }
    }
  }

  private void updateManagedAclForEvents(String managedAcl, Optional<String> newManagedAclOpt, String organization,
          User user) {
    SearchResult<Event> result;
    try {
      result = getSearchIndex().getByQuery(new EventSearchQuery(organization, user).withoutActions()
              .withManagedAcl(managedAcl));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the events in org '{}' with current managed acl name '{}' for event",
              organization, managedAcl, e);
      return;
    }

    for (SearchResultItem<Event> eventItem : result.getItems()) {
      String eventId = eventItem.getSource().getIdentifier();

      Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
        if (eventOpt.isPresent() && eventOpt.get().getManagedAcl().equals(managedAcl)) {
          Event event = eventOpt.get();
          event.setManagedAcl(newManagedAclOpt.orElse(null));
          return Optional.of(event);
        }
        return Optional.empty();
      };

      try {
        getSearchIndex().addOrUpdateEvent(eventId, updateFunction, organization, user);
      } catch (SearchIndexException e) {
        if (newManagedAclOpt.isPresent()) {
          logger.warn(
                  "Unable to update event '{}' from current managed acl '{}' to new managed acl name '{}'",
                  eventId, managedAcl, newManagedAclOpt.get(), e);
        } else {
          logger.warn("Unable to update event '{}' to remove managed acl '{}'", eventId, managedAcl, e);
        }
      }
    }
  }

  @Override
  protected void execute(AclItem aclItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    String managedAcl = aclItem.getCurrentAclName();
    switch (aclItem.getType()) {
      case Create:
        logger.debug("Ignoring create as we don't need to update any indices with it.");
        return;
      case Update:
        logger.debug("Received Update Managed Acl Entry");

        logger.debug("Update the events to change their managed acl name from '{}' to '{}'",
                aclItem.getCurrentAclName(), aclItem.getNewAclName());
        updateManagedAclForEvents(managedAcl, Optional.of(aclItem.getNewAclName()), organization, user);

        logger.debug("Update the series to change their managed acl name from '{}' to '{}'",
                aclItem.getCurrentAclName(), aclItem.getNewAclName());
        updateManagedAclForSeries(managedAcl, Optional.of(aclItem.getNewAclName()), organization, user);
       return;
      case Delete:
        logger.debug("Received Delete Managed Entry {}", aclItem.getCurrentAclName());

        logger.debug("Update the events to delete their managed acl name '{}'", aclItem.getCurrentAclName());
        updateManagedAclForEvents(managedAcl, Optional.empty(), organization, user);

        logger.debug("Update the series to delete their managed acl name '{}'", aclItem.getCurrentAclName());
        updateManagedAclForSeries(managedAcl, Optional.empty(), organization, user);
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of AclItem");
    }
  }
}
