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
package org.opencastproject.index.service.impl.util;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.update.AssetManagerUpdateHandler;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.MimeType;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component(
        immediate = true,
        service = {
                AssetManagerUpdateHandler.class
        },
        property = {
                "service.description=Update event publication metadata and ACLs",
                "opencast.service.type=org.opencastproject.index.service.impl.util.PublicationEventUpdateHandler"
        }
)
public class PublicationEventUpdateHandler implements AssetManagerUpdateHandler {

  private static final Logger logger = LoggerFactory.getLogger(PublicationEventUpdateHandler.class);

  private static final String CONFIGURATION_PUBLICATION_CHANNEL_IDS = "publication.channel.ids";

  private Set<String> configurationPublicationChannelIds = new HashSet<>();

  private DownloadDistributionService downloadDistributionService = null;
  private StreamingDistributionService streamingDistributionService = null;
  private SearchService searchService = null;

  @Reference(target = "(distribution.channel=streaming)")
  protected void setStreamingDistributionService(StreamingDistributionService streamingDistributionService) {
    this.streamingDistributionService = streamingDistributionService;
  }

  @Reference(target = "(distribution.channel=download)")
  protected void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    this.downloadDistributionService = downloadDistributionService;
  }

  @Reference
  protected void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating {}", PublicationEventUpdateHandler.class.getName());
    Object pubChannelIdsValue = cc.getProperties().get(CONFIGURATION_PUBLICATION_CHANNEL_IDS);
    configurationPublicationChannelIds.clear();
    if (pubChannelIdsValue != null) {
      for (String publicationChannel : StringUtils.split(pubChannelIdsValue.toString(), ',')) {
        if (StringUtils.trimToNull(publicationChannel) != null) {
          configurationPublicationChannelIds.add(StringUtils.trimToNull(publicationChannel));
        }
      }
    }
    logger.debug("Excluded user providers: {}", configurationPublicationChannelIds);
  }

  @Deactivate
  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", PublicationEventUpdateHandler.class.getName());
  }

  @Override
  public void execute(AssetManagerItem messageItem) {
    if (! (messageItem instanceof AssetManagerItem.TakeSnapshot)) {
      // We don't want to handle anything but TakeSnapshot messages.
      return;
    }
    AssetManagerItem.TakeSnapshot snapshotItem = (AssetManagerItem.TakeSnapshot) messageItem;
    updatePublications(snapshotItem.getMediapackage());
  }

  private void updatePublications(MediaPackage mediaPackage) {
    for (Publication publication : new ArrayList<>(Arrays.asList(mediaPackage.getPublications()))) {
      if (!configurationPublicationChannelIds.contains(publication.getChannel())) {
        continue;
      }
      String channelId = publication.getChannel();
      String publicationId = publication.getIdentifier();
      URI publicationURI = publication.getURI();
      MimeType publicationMimeType = publication.getMimeType();

      if (channelId == null || publicationURI == null || publicationMimeType == null || publicationId == null) {
        // No publication, do nothing
        continue;
      }

      // Get metadata and ACLs
      SimpleElementSelector elementSelector = new SimpleElementSelector();
      // Relies on ACLs having this particular flavor
      elementSelector.addFlavor("security/*");
      Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, false);

      Set<String> elementIds = new HashSet<>();
      for (MediaPackageElement elem : elements) {
        elementIds.add(elem.getIdentifier());
      }

      // To make sure we hit all extended metadata catalogs, let's just get all catalogs
      for (Catalog catalog : mediaPackage.getCatalogs()) {
        elementIds.add(catalog.getIdentifier());
      }

      if (elementIds.size() < 1) {
        // Nothing to republish, do nothing
        continue;
      }

      // Remove publication from mediapackage
      mediaPackage.remove(publication);

      List<MediaPackageElement> downloadElements;
      List<MediaPackageElement> streamingElements = new ArrayList<>();
      try {
        downloadElements = downloadDistributionService.distributeSync(channelId, mediaPackage, elementIds, false);
        if (streamingDistributionService != null && streamingDistributionService.publishToStreaming()) {
          streamingElements = streamingDistributionService.distributeSync(channelId, mediaPackage, elementIds);
        }
      } catch (DistributionException e) {
        throw new RuntimeException(e);
      }
      logger.debug("Distribute of mediapackage {} to channel {} completed", mediaPackage, channelId);

      // Re-add publication
      Publication newPublication = PublicationImpl.publication(publicationId, channelId, publicationURI, publicationMimeType);

      // Add published elements
      for (MediaPackageElement element : downloadElements) {
        element.setIdentifier(null);
        PublicationImpl.addElementToPublication(newPublication, element);
      }
      for (MediaPackageElement element : streamingElements) {
        element.setIdentifier(null);
        PublicationImpl.addElementToPublication(newPublication, element);
      }

      mediaPackage.add(newPublication);
    }

    // Also search service
    try {
      searchService.addSynchronously(mediaPackage);
    } catch (UnauthorizedException e) {
      throw new RuntimeException(e);
    }
  }

}
