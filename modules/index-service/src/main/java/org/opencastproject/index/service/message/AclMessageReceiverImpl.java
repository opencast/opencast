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
import org.opencastproject.elasticsearch.index.event.EventIndexUtils;
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

  private void updateManagedAcl(String managedAcl, Optional<String> newManagedAclOpt, String organization, User user) {
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
        Series series = seriesOpt.orElse(new Series(seriesId, organization));

        if (series.getManagedAcl().equals(managedAcl)) {  // check in case it changed between the queries
          series.setManagedAcl(newManagedAclOpt.orElse(null));
          return Optional.of(series);
        }
        return Optional.empty();
      };

      try {
        getSearchIndex().addOrUpdate(seriesId, updateFunction, organization, user);
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
        EventIndexUtils.updateManagedAclName(aclItem.getCurrentAclName(), aclItem.getNewAclName(), organization, user,
                getSearchIndex());

        logger.debug("Update the series to change their managed acl name from '{}' to '{}'",
                aclItem.getCurrentAclName(), aclItem.getNewAclName());
        updateManagedAcl(managedAcl, Optional.of(aclItem.getNewAclName()), organization, user);
       return;
      case Delete:
        logger.debug("Received Delete Managed Entry {}", aclItem.getCurrentAclName());

        logger.debug("Update the events to delete their managed acl name '{}'", aclItem.getCurrentAclName());
        EventIndexUtils.deleteManagedAcl(aclItem.getCurrentAclName(), organization, user, getSearchIndex());

        logger.debug("Update the series to delete their managed acl name '{}'", aclItem.getCurrentAclName());
        updateManagedAcl(managedAcl, Optional.empty(), organization, user);
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of AclItem");
    }
  }
}
