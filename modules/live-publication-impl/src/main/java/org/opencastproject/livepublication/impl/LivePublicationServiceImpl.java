/*
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
package org.opencastproject.livepublication.impl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.livepublication.api.LivePublicationException;
import org.opencastproject.livepublication.api.LivePublicationService;
import org.opencastproject.livepublication.publication.ArchiveUpdater;
import org.opencastproject.livepublication.publication.DistributionUpdater;
import org.opencastproject.livepublication.publication.SearchUpdater;
import org.opencastproject.livepublication.util.LiveTrackEquator;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component(immediate = true, service = LivePublicationService.class, property = {
    "service.description=Live Schedule Service" })
public class LivePublicationServiceImpl implements LivePublicationService {

  /**
   * Default values for configuration options
   */
  private static final String DEFAULT_STREAM_MIME_TYPE = "video/mp4";
  private static final String DEFAULT_STREAM_RESOLUTION = "1920x1080";
  private static final String DEFAULT_STREAM_NAME = "live-stream";
  static final String DEFAULT_LIVE_TARGET_FLAVOR = "presenter/delivery";
  static final String DEFAULT_LIVE_DISTRIBUTION_SERVICE = "download";

  public static final String LIVE_STREAMING_URL = "live.streamingUrl";
  public static final String LIVE_STREAM_NAME = "live.streamName";
  public static final String LIVE_STREAM_MIME_TYPE = "live.mimeType";
  public static final String LIVE_STREAM_RESOLUTION = "live.resolution";
  public static final String LIVE_TARGET_FLAVORS = "live.targetFlavors";
  private static final String DELETE_ON_CAPTURE_ERROR = "live.deleteOnCaptureError";

  static final String SERVER_URL_PROPERTY = "org.opencastproject.server.url";

  private static final MediaPackageElementFlavor[] publishFlavors = { MediaPackageElements.EPISODE,
      MediaPackageElements.SERIES, MediaPackageElements.XACML_POLICY_EPISODE,
      MediaPackageElements.XACML_POLICY_SERIES }; // make configurable later

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(LivePublicationServiceImpl.class);

  private final Cache<String, String> snapshotVersionCache = CacheBuilder.newBuilder()
      .expireAfterWrite(5, TimeUnit.MINUTES).build();

  /**
   * Services
   */
  private DownloadDistributionService downloadDistributionService;
  private SearchService searchService;
  private CaptureAgentStateService captureAgentService;
  private Workspace workspace;
  private AssetManager assetManager;
  private SecurityService securityService;

  private boolean deleteOnCaptureError = true;

  private ArchiveUpdater archiveUpdater;
  private SearchUpdater searchUpdater;
  private LiveTracksCreator liveTracksCreator;
  private DistributionUpdater distributionHandler;

  /**
   * OSGi callback on component activation.
   *
   * @param context the component context
   */
  @Activate
  protected void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();
    String systemUserName = bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);

    String serverUrl = StringUtils.trimToNull(bundleContext.getProperty(SERVER_URL_PROPERTY));
    if (serverUrl == null) {
      logger.warn("Server url was not set in '{}'", SERVER_URL_PROPERTY);
    } else {
      logger.info("Server url is {}", serverUrl);
    }

    @SuppressWarnings("rawtypes") Dictionary properties = context.getProperties();
    String liveStreamingUrl = null;
    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAMING_URL))) {
      liveStreamingUrl = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAMING_URL));
      logger.info("Live streaming server url is {}", liveStreamingUrl);
    } else {
      logger.info("Live streaming url not set in '{}'. Streaming urls must be provided by capture agent properties.",
              LIVE_STREAMING_URL);
    }

    String streamName;
    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAM_NAME))) {
      streamName = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAM_NAME));
    } else {
      streamName = DEFAULT_STREAM_NAME;
    }
    String streamMimeType;
    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAM_MIME_TYPE))) {
      streamMimeType = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAM_MIME_TYPE));
    } else {
      streamMimeType = DEFAULT_STREAM_MIME_TYPE;
    }

    String resolution;
    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAM_RESOLUTION))) {
      resolution = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAM_RESOLUTION));
    } else {
      resolution = DEFAULT_STREAM_RESOLUTION;
    }
    String[] streamResolution = resolution.split(",");

    String flavors;
    if (!StringUtils.isBlank((String) properties.get(LIVE_TARGET_FLAVORS))) {
      flavors = StringUtils.trimToEmpty((String) properties.get(LIVE_TARGET_FLAVORS));
    } else {
      flavors = DEFAULT_LIVE_TARGET_FLAVOR;
    }
    String[] flavorArray = StringUtils.split(flavors, ",");
    List<MediaPackageElementFlavor> liveFlavors = Arrays.stream(flavorArray).map(MediaPackageElementFlavor::parseFlavor)
        .toList();

    SimpleElementSelector publishElementSelector = new SimpleElementSelector();
    for (MediaPackageElementFlavor flavor : publishFlavors) {
      publishElementSelector.addFlavor(flavor);
    }

    deleteOnCaptureError = BooleanUtils.toBoolean(Objects.toString(properties.get(DELETE_ON_CAPTURE_ERROR), "true"));

    logger.info("Configured live stream name: {}, mime type: {}, resolution: {}, target flavors: {}", streamName,
        streamMimeType, resolution, flavors);

    this.archiveUpdater = new ArchiveUpdater(assetManager, securityService, serverUrl);
    this.searchUpdater = new SearchUpdater(searchService, securityService, systemUserName);
    this.liveTracksCreator = new LiveTracksCreator(captureAgentService, streamMimeType, liveStreamingUrl, streamName,
        streamResolution, liveFlavors, MediaPackageElementFlavor.parseFlavor(DEFAULT_LIVE_TARGET_FLAVOR));
    this.distributionHandler = new DistributionUpdater(downloadDistributionService, workspace, publishElementSelector);
  }

  @Override
  public void createLiveEvent(MediaPackage archivedMediaPackage, Date startDate, Date endDate, String agentId)
          throws LivePublicationException {
    try {
      // generate live tracks, distribute elements
      List<Track> liveTracks = liveTracksCreator.createLiveTracks(archivedMediaPackage.getIdentifier().toString(),
          startDate, endDate, agentId);
      List<MediaPackageElement> distributedElements = distributionHandler.distributeElements(archivedMediaPackage);

      // publish to search
      MediaPackage mpForSearch = (MediaPackage) archivedMediaPackage.clone();
      mpForSearch.clearElements();
      liveTracks.forEach(mpForSearch::add);
      distributedElements.forEach(mpForSearch::add);
      searchUpdater.publishToSearch(mpForSearch);

      // add live publication to archive
      archiveUpdater.addLivePublication(archivedMediaPackage, Arrays.asList(mpForSearch.getElements()));
      snapshotVersionCache.put(archivedMediaPackage.getIdentifier().toString(),
          assetManager.takeSnapshot(archivedMediaPackage).getVersion().toString());
    } catch (Exception e) {
      throw new LivePublicationException(e);
    }
  }

  @Override
  public void updateLiveTracks(String mpId, Date startDate, Date endDate, String agentId)
          throws LivePublicationException, NotFoundException {
    MediaPackage mpFromSearch = searchUpdater.getMediaPackageFromSearch(mpId);
    if (mpFromSearch.isLive()) {
      // create new live tracks
      List<Track> newLiveTracks = liveTracksCreator.createLiveTracks(mpId, startDate, endDate, agentId);
      if (CollectionUtils.isEqualCollection(List.of(mpFromSearch.getTracks()), newLiveTracks, new LiveTrackEquator())) {
        logger.debug("No changes in live tracks of {}, not updating.", mpFromSearch);
        return;
      }

      // update search
      logger.info("Updating live media package {}", mpFromSearch);
      mpFromSearch.clearElements(MediaPackageElement.Type.Track);
      newLiveTracks.forEach(mpFromSearch::add);
      searchUpdater.publishToSearch(mpFromSearch);

      // update archive
      MediaPackage mpFromArchive = archiveUpdater.getMediapackageFromArchive(mpId);
      archiveUpdater.updateLivePublication(mpFromArchive, newLiveTracks);
      snapshotVersionCache.put(mpId, assetManager.takeSnapshot(mpFromArchive).getVersion().toString());
    } else {
      throw new LivePublicationException("Trying to update already processed event " + mpId);
    }
  }

  @Override
  public void updateLiveEvent(MediaPackage archivedMediaPackage, String version)
          throws NotFoundException, LivePublicationException {
    String mpId = archivedMediaPackage.getIdentifier().toString();

    if (version.equals(snapshotVersionCache.getIfPresent(mpId))) {
      logger.debug("Snapshot version {} was created by us so this change is ignored.", version);
      return;
    }

    MediaPackage mpFromSearch = searchUpdater.getMediaPackageFromSearch(mpId);
    if (mpFromSearch.isLive()) {
      // distribute and/or retract elements
      Map<String, Collection<MediaPackageElement>> results = distributionHandler.updateElements(mpFromSearch,
          archivedMediaPackage);
      Collection<MediaPackageElement> retractedElements = results.getOrDefault(distributionHandler.RETRACTED,
          new ArrayList<>());
      Collection<MediaPackageElement> distributedElements = results.getOrDefault(distributionHandler.DISTRIBUTED,
          new ArrayList<>());
      if (retractedElements.isEmpty() && distributedElements.isEmpty()) {
        logger.debug("Attachments and catalogs for live media package {} seem to be the same, not updating.",
            mpFromSearch);
        return;
      }

      // update in search
      logger.info("Updating live media package {}", mpFromSearch);
      retractedElements.forEach(mpFromSearch::remove);
      distributedElements.forEach(mpFromSearch::add);
      searchUpdater.publishToSearch(mpFromSearch);

    // update live publication in archive
      archiveUpdater.updateLivePublication(archivedMediaPackage, retractedElements, distributedElements);
      snapshotVersionCache.put(mpId, assetManager.takeSnapshot(archivedMediaPackage).getVersion().toString());
    } else {
      logger.warn("Leftover live publication for {}, but mediapackage in search is not live, retracting live event",
          mpId);
      try {
        distributionHandler.retractAllElements(archivedMediaPackage, true);
      } catch (DistributionException e) {
        logger.warn("Distributed elements for live event {} could not be retracted", mpId);
      }
      archiveUpdater.removeLivePublication(archivedMediaPackage);
      snapshotVersionCache.put(mpId, assetManager.takeSnapshot(archivedMediaPackage).getVersion().toString());
    }
  }

  @Override
  public void deleteLiveEvent(String mpId, boolean updateAssetManager) {
    boolean retractedFromSearch = false;
    try {
      MediaPackage mpFromSearch = searchUpdater.getMediaPackageFromSearch(mpId);
      if (mpFromSearch.isLive()) {
        searchUpdater.retractFromSearch(mpId);
        distributionHandler.retractAllElements(mpFromSearch, false);
        retractedFromSearch = true;
      }
    } catch (NotFoundException e) {
      logger.debug("Live event {} not found in search for retraction", mpId);
    } catch (DistributionException e) {
      logger.warn("Distributed elements for live event {} could not be retracted", mpId);
    }

    if (updateAssetManager) {
      try {
        MediaPackage archivedMp = archiveUpdater.getMediapackageFromArchive(mpId);
        if (ArchiveUpdater.getLivePublication(archivedMp).isPresent()) {
          if (!retractedFromSearch) {
            distributionHandler.retractAllElements(archivedMp, true);
          }
          archiveUpdater.removeLivePublication(archivedMp);
          snapshotVersionCache.put(mpId, assetManager.takeSnapshot(archivedMp).getVersion().toString());
          logger.debug("Removed live pub channel from archived media package {}", mpId);
        }
      } catch (NotFoundException e) {
        logger.debug("Live event {} not found in archive for removal", mpId);
      } catch (DistributionException e) {
        logger.warn("Distributed elements for live event {} could not be retracted", mpId);
      }
    }
  }

  @Override
  public void handleCaptureError(String mpId) {
    if (deleteOnCaptureError) {
      deleteLiveEvent(mpId, true);
    }
  }

  // OSGI setters
  @Reference
  public void setSearchService(SearchService service) {
    this.searchService = service;
  }

  @Reference
  public void setCaptureAgentService(CaptureAgentStateService service) {
    this.captureAgentService = service;
  }

  @Reference(name = "DownloadDistributionService", target = "(distribution.channel=download)")
  public void setDownloadDistributionService(DownloadDistributionService service) {
    this.downloadDistributionService = service;
    logger.info("Distribution service with type '{}' set.", downloadDistributionService.getDistributionType());
  }

  @Reference
  public void setWorkspace(Workspace ws) {
    this.workspace = ws;
  }

  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  @Reference
  public void setSecurityService(SecurityService service) {
    this.securityService = service;
  }

  //  // Used by unit tests
  //  void setJobPollingInterval(long jobPollingInterval) {
  //    this.jobPollingInterval = jobPollingInterval;
  //  }

  Cache<String, String> getSnapshotVersionCache() {
    return this.snapshotVersionCache;
  }
}
