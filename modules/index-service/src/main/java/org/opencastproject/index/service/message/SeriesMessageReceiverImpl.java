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
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

public class SeriesMessageReceiverImpl extends BaseMessageReceiverImpl<SeriesItem> {

  private static final Logger logger = LoggerFactory.getLogger(SeriesMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the series queue.
   */
  public SeriesMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(SeriesItem seriesItem) {
    Function<Optional<Series>, Optional<Series>> updateFunction = null;
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    String seriesId = seriesItem.getSeriesId();

    switch (seriesItem.getType()) {
      case UpdateCatalog:
        logger.debug("Received Update Series for index {}", getSearchIndex().getIndexName());
        DublinCoreCatalog dc = seriesItem.getMetadata();

        updateFunction = (Optional<Series> seriesOpt) -> {
          Series series = seriesOpt.orElse(new Series(seriesItem.getSeriesId(), organization));
          series.setCreator(getSecurityService().getUser().getName());

          // update from dublin core catalog
          series.setTitle(dc.getFirst(DublinCoreCatalog.PROPERTY_TITLE));
          series.setDescription(dc.getFirst(DublinCore.PROPERTY_DESCRIPTION));
          series.setSubject(dc.getFirst(DublinCore.PROPERTY_SUBJECT));
          series.setLanguage(dc.getFirst(DublinCoreCatalog.PROPERTY_LANGUAGE));
          series.setLicense(dc.getFirst(DublinCoreCatalog.PROPERTY_LICENSE));
          series.setRightsHolder(dc.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
          String createdDateStr = dc.getFirst(DublinCoreCatalog.PROPERTY_CREATED);
          if (createdDateStr != null) {
            series.setCreatedDateTime(EncodingSchemeUtils.decodeDate(createdDateStr));
          }
          series.setPublishers(dc.get(DublinCore.PROPERTY_PUBLISHER, DublinCore.LANGUAGE_ANY));
          series.setContributors(dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_ANY));
          series.setOrganizers(dc.get(DublinCoreCatalog.PROPERTY_CREATOR, DublinCore.LANGUAGE_ANY));
          return Optional.of(series);
        };
        break;
      case UpdateElement:
        // nothing to do
        break;
      default:
        throw new IllegalArgumentException("Unhandled type of SeriesItem");
    }

    // do the actual update
    if (updateFunction != null) {
      try {
        Optional<Series> updatedSeriesOpt = getSearchIndex().addOrUpdateSeries(seriesId, updateFunction, organization,
                user);

        // update series title in events
        if (updatedSeriesOpt.isPresent() && updatedSeriesOpt.get().isSeriesTitleUpdated()) {
          Series updatedSeries = updatedSeriesOpt.get();
          SearchResult<Event> events = getSearchIndex().getByQuery(
                  new EventSearchQuery(organization, user).withoutActions().withSeriesId(updatedSeries.getIdentifier()));
          for (SearchResultItem<Event> searchResultItem : events.getItems()) {
            String eventId = searchResultItem.getSource().getIdentifier();

            Function<Optional<Event>, Optional<Event>> eventUpdateFunction = (Optional<Event> eventOpt) -> {
              if (eventOpt.isPresent() && eventOpt.get().getSeriesId().equals(updatedSeries.getIdentifier())) {
                Event event = eventOpt.get();
                event.setSeriesName(updatedSeries.getTitle());
                return Optional.of(event);
              }
              return Optional.empty();
            };

            getSearchIndex().addOrUpdateEvent(eventId, eventUpdateFunction, organization, user);
          }
        }
        logger.debug("Series {} updated in the search index", seriesId);
      } catch (SearchIndexException e) {
        logger.error("Error storing the series {} in the search index", seriesId, e);
      }
    }
  }
}
