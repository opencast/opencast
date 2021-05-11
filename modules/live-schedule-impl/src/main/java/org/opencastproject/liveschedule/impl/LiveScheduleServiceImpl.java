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
package org.opencastproject.liveschedule.impl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.liveschedule.api.LiveScheduleException;
import org.opencastproject.liveschedule.api.LiveScheduleService;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveScheduleServiceImpl implements LiveScheduleService {
  /** The server url property **/
  static final String SERVER_URL_PROPERTY = "org.opencastproject.server.url";
  /** The engage base url property **/
  static final String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";
  /** The default path to the player **/
  static final String PLAYER_PATH = "/play/";

  /** Default values for configuration options */
  private static final String DEFAULT_STREAM_MIME_TYPE = "video/mp4";
  private static final String DEFAULT_STREAM_RESOLUTION = "1920x1080";
  private static final String DEFAULT_STREAM_NAME = "live-stream";
  private static final String DEFAULT_LIVE_TARGET_FLAVORS = "presenter/delivery";
  static final String DEFAULT_LIVE_DISTRIBUTION_SERVICE = "download";

  // If the capture agent registered this property, we expect to get a resolution and
  // a url in the following format:
  // capture.device.live.resolution.WIDTHxHEIGHT=COMPLETE_STREAMING_URL e.g.
  // capture.device.live.resolution.960x270=rtmp://cp398121.live.edgefcs.net/live/dev-epiphan005-2-presenter-delivery.stream-960x270_1_200@355694
  public static final String CA_PROPERTY_RESOLUTION_URL_PREFIX = "capture.device.live.resolution.";

  /** Variables that can be replaced in stream name */
  public static final String REPLACE_ID = "id";
  public static final String REPLACE_FLAVOR = "flavor";
  public static final String REPLACE_CA_NAME = "caName";
  public static final String REPLACE_RESOLUTION = "resolution";

  public static final String LIVE_STREAMING_URL = "live.streamingUrl";
  public static final String LIVE_STREAM_NAME = "live.streamName";
  public static final String LIVE_STREAM_MIME_TYPE = "live.mimeType";
  public static final String LIVE_STREAM_RESOLUTION = "live.resolution";
  public static final String LIVE_TARGET_FLAVORS = "live.targetFlavors";
  public static final String LIVE_DISTRIBUTION_SERVICE = "live.distributionService";
  public static final String LIVE_PUBLISH_STREAMING = "live.publishStreaming";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LiveScheduleServiceImpl.class);

  private String liveStreamingUrl;
  private String streamName;
  private String streamMimeType;
  private String[] streamResolution;
  private MediaPackageElementFlavor[] liveFlavors;
  private String distributionServiceType = DEFAULT_LIVE_DISTRIBUTION_SERVICE;
  private String serverUrl;
  private Cache<String, Version> snapshotVersionCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
  /** Which streaming formats should be published automatically */
  private List<String> publishedStreamingFormats = null;
  private String systemUserName;

  /** Services */
  private DownloadDistributionService downloadDistributionService; // to distribute episode and series catalogs
  private SearchService searchService; // to publish/retract live media package
  private SeriesService seriesService; // to get series metadata
  private DublinCoreCatalogService dublinCoreService; // to setialize dc catalogs
  private CaptureAgentStateService captureAgentService; // to get agent capabilities
  private ServiceRegistry serviceRegistry; // to create publish/retract jobs
  private Workspace workspace; // to save dc catalogs before distributing
  private AssetManager assetManager; // to get current media package
  private AuthorizationService authService;
  private OrganizationDirectoryService organizationService;
  private SecurityService securityService;

  private long jobPollingInterval = JobBarrier.DEFAULT_POLLING_INTERVAL;

  /**
   * OSGi callback on component activation.
   *
   * @param context
   *          the component context
   */
  protected void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();

    serverUrl = StringUtils.trimToNull(bundleContext.getProperty(SERVER_URL_PROPERTY));
    if (serverUrl == null)
      logger.warn("Server url was not set in '{}'", SERVER_URL_PROPERTY);
    else
      logger.info("Server url is {}", serverUrl);
    systemUserName = bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);

    @SuppressWarnings("rawtypes")
    Dictionary properties = context.getProperties();
    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAMING_URL))) {
      liveStreamingUrl = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAMING_URL));
      logger.info("Live streaming server url is {}", liveStreamingUrl);
    } else {
      logger.info("Live streaming url not set in '{}'. Streaming urls must be provided by capture agent properties.",
              LIVE_STREAMING_URL);
    }

    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAM_NAME))) {
      streamName = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAM_NAME));
    } else {
      streamName = DEFAULT_STREAM_NAME;
    }

    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAM_MIME_TYPE))) {
      streamMimeType = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAM_MIME_TYPE));
    } else {
      streamMimeType = DEFAULT_STREAM_MIME_TYPE;
    }

    String resolution = null;
    if (!StringUtils.isBlank((String) properties.get(LIVE_STREAM_RESOLUTION))) {
      resolution = StringUtils.trimToEmpty((String) properties.get(LIVE_STREAM_RESOLUTION));
    } else {
      resolution = DEFAULT_STREAM_RESOLUTION;
    }
    streamResolution = resolution.split(",");

    String flavors = null;
    if (!StringUtils.isBlank((String) properties.get(LIVE_TARGET_FLAVORS))) {
      flavors = StringUtils.trimToEmpty((String) properties.get(LIVE_TARGET_FLAVORS));
    } else {
      flavors = DEFAULT_LIVE_TARGET_FLAVORS;
    }
    String[] flavorArray = StringUtils.split(flavors, ",");
    liveFlavors = new MediaPackageElementFlavor[flavorArray.length];
    int i = 0;
    for (String f : flavorArray)
      liveFlavors[i++] = MediaPackageElementFlavor.parseFlavor(f);

    if (!StringUtils.isBlank((String) properties.get(LIVE_DISTRIBUTION_SERVICE))) {
      distributionServiceType = StringUtils.trimToEmpty((String) properties.get(LIVE_DISTRIBUTION_SERVICE));
    }
    publishedStreamingFormats = Arrays.asList(Optional.ofNullable(StringUtils.split(
            (String)properties.get(LIVE_PUBLISH_STREAMING), ",")).orElse(new String[0]));

    logger.info(
            "Configured live stream name: {}, mime type: {}, resolution: {}, target flavors: {}, distribution service: {}",
            streamName, streamMimeType, resolution, flavors, distributionServiceType);
  }

  @Override
  public boolean createOrUpdateLiveEvent(String mpId, DublinCoreCatalog episodeDC) throws LiveScheduleException {
    MediaPackage mp = getMediaPackageFromSearch(mpId);
    if (mp == null) {
      // Check if capture not over. We have to check because we may get a notification for past events if
      // the admin ui index is rebuilt
      DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(episodeDC.getFirst(DublinCore.PROPERTY_TEMPORAL));
      if (period.getEnd().getTime() <= System.currentTimeMillis()) {
        logger.info("Live media package {} not created in search index because event is already past (end date: {})",
                mpId, period.getEnd());
        return false;
      }
      return createLiveEvent(mpId, episodeDC);
    } else {
      // Check if the media package found in the search index is live. We have to check because we may get a
      // notification for past events if the admin ui index is rebuilt
      if (!isLive(mp)) {
        logger.info("Media package {} is in search index but not live so not updating it.", mpId);
        return false;
      }
      return updateLiveEvent(mp, episodeDC);
    }
  }

  @Override
  public boolean deleteLiveEvent(String mpId) throws LiveScheduleException {
    MediaPackage mp = getMediaPackageFromSearch(mpId);
    if (mp == null) {
      logger.debug("Live media package {} not found in search index", mpId);
      return false;
    } else {
      if (!isLive(mp)) {
        logger.info("Media package {} is not live. Not retracting.", mpId);
        return false;
      }
      return retractLiveEvent(mp);
    }
  }

  @Override
  public boolean updateLiveEventAcl(String mpId, AccessControlList acl) throws LiveScheduleException {
    MediaPackage previousMp = getMediaPackageFromSearch(mpId);
    if (previousMp != null) {
      if (!isLive(previousMp)) {
        logger.info("Media package {} is not live. Not updating acl.", mpId);
        return false;
      }
      // Replace and distribute acl, this creates new mp
      MediaPackage newMp = replaceAndDistributeAcl(previousMp, acl);
      // Publish mp to engage search index
      publish(newMp);
      // Don't leave garbage there!
      retractPreviousElements(previousMp, newMp);
      logger.info("Updated live acl for media package {}", newMp);
      return true;
    }
    return false;
  }

  boolean createLiveEvent(String mpId, DublinCoreCatalog episodeDC) throws LiveScheduleException {
    try {
      logger.info("Creating live media package {}", mpId);
      // Get latest mp from the asset manager
      Snapshot snapshot = getSnapshot(mpId);
      // Temporary mp
      MediaPackage tempMp = (MediaPackage) snapshot.getMediaPackage().clone();
      // Set duration (used by live tracks)
      setDuration(tempMp, episodeDC);
      // Add live tracks to media package
      Map<String, Track> generatedTracks = addLiveTracks(tempMp, episodeDC.getFirst(DublinCore.PROPERTY_SPATIAL));
      // Add and distribute catalogs/acl, this creates a new mp object
      MediaPackage mp = addAndDistributeElements(snapshot);
      // Add tracks from tempMp
      for (Track t : tempMp.getTracks())
        mp.add(t);
      // Publish mp to engage search index
      publish(mp);
      // Add engage-live publication channel to archived mp
      Organization currentOrg = null;
      try {
        currentOrg = organizationService.getOrganization(snapshot.getOrganizationId());
      } catch (NotFoundException e) {
        logger.warn("Organization in snapshot not found: {}", snapshot.getOrganizationId());
      }
      MediaPackage archivedMp = snapshot.getMediaPackage();
      addLivePublicationChannel(currentOrg, archivedMp, generatedTracks);
      // Take a snapshot with the publication added and put its version in our local cache
      // so that we ignore notifications for this snapshot version.
      snapshotVersionCache.put(mpId, assetManager.takeSnapshot(archivedMp).getVersion());
      return true;
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  boolean updateLiveEvent(MediaPackage previousMp, DublinCoreCatalog episodeDC) throws LiveScheduleException {
    // Get latest mp from the asset manager
    Snapshot snapshot = getSnapshot(previousMp.getIdentifier().toString());
    // If the snapshot version is in our local cache, it means that this snapshot was created by us so
    // nothing to do. Note that this is just to save time; if the entry has already been deleted, the mp
    // will be compared below.
    if (snapshot.getVersion().equals(snapshotVersionCache.getIfPresent(previousMp.getIdentifier().toString()))) {
      logger.debug("Snapshot version {} was created by us so this change is ignored.", snapshot.getVersion());
      return false;
    }
    // Temporary mp
    MediaPackage tempMp = (MediaPackage) snapshot.getMediaPackage().clone();
    // Set duration (used by live tracks)
    setDuration(tempMp, episodeDC);
    // Add live tracks to media package
    Map<String, Track> generatedTracks = addLiveTracks(tempMp, episodeDC.getFirst(DublinCore.PROPERTY_SPATIAL));
    // Update tracks in the publication
    createOrUpdatePublicationTracks(tempMp, generatedTracks);
    // If same mp, no need to do anything
    if (isSameMediaPackage(previousMp, tempMp)) {
      logger.debug("Live media package {} seems to be the same. Not updating.", previousMp);
      return false;
    }
    logger.info("Updating live media package {}", previousMp);
    // Add and distribute catalogs/acl, this creates a new mp
    MediaPackage mp = addAndDistributeElements(snapshot);
    // Add tracks from tempMp
    for (Track t : tempMp.getTracks())
      mp.add(t);
    // Remove publication element that came with the snapshot mp
    removeLivePublicationChannel(mp);
    // Publish mp to engage search index
    publish(mp);
    // Publication channel already there so no need to add
    // Don't leave garbage there!
    retractPreviousElements(previousMp, mp);
    return true;
  }

  private void createOrUpdatePublicationTracks(Publication publication, Map<String, Track> generatedTracks) {
    if (publication.getTracks().length > 0) {
      publication.clearTracks();
    }

    for (String publishedStreamingFormat : publishedStreamingFormats) {
      Track track = generatedTracks.get(publishedStreamingFormat);
      if (track != null) {
        publication.addTrack(track);
      }
    }
  }

  private void createOrUpdatePublicationTracks(MediaPackage mediaPackage, Map<String, Track> generatedTracks) {
    Publication[] publications = mediaPackage.getPublications();
    for (Publication publication : publications) {
      if (publication.getChannel().equals(CHANNEL_ID)) {
          createOrUpdatePublicationTracks(publication, generatedTracks);
      }
    }
  }

  boolean retractLiveEvent(MediaPackage mp) throws LiveScheduleException {
    retract(mp);

    // Get latest mp from the asset manager if there to remove the publication
    try {
      String mpId = mp.getIdentifier().toString();
      Snapshot snapshot = getSnapshot(mpId);
      MediaPackage archivedMp = snapshot.getMediaPackage();
      removeLivePublicationChannel(archivedMp);
      logger.debug("Removed live pub channel from archived media package {}", mp);
      // Take a snapshot with the publication removed and put its version in our local cache
      // so that we ignore notifications for this snapshot version.
      snapshotVersionCache.put(mpId, assetManager.takeSnapshot(archivedMp).getVersion());
    } catch (LiveScheduleException e) {
      // It was not found in asset manager. This is ok.
    }
    return true;
  }

  void publish(MediaPackage mp) throws LiveScheduleException {
    try {
      // Add media package to the search index
      logger.info("Publishing LIVE media package {} to search index", mp);
      Job publishJob = searchService.add(mp);
      if (!waitForStatus(publishJob).isSuccess())
        throw new LiveScheduleException("Live media package " + mp.getIdentifier() + " could not be published");
    } catch (LiveScheduleException e) {
      throw e;
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  void retract(MediaPackage mp) throws LiveScheduleException {
    try {
      List<Job> jobs = new ArrayList<Job>();
      Set<String> elementIds = new HashSet<String>();
      // Remove media package from the search index
      String mpId = mp.getIdentifier().compact();
      logger.info("Removing LIVE media package {} from the search index", mpId);

      jobs.add(searchService.delete(mpId));
      // Retract elements
      for (MediaPackageElement mpe : mp.getElements()) {
        if (!MediaPackageElement.Type.Publication.equals(mpe.getElementType()))
          elementIds.add(mpe.getIdentifier());
      }
      jobs.add(downloadDistributionService.retract(CHANNEL_ID, mp, elementIds));

      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess())
        throw new LiveScheduleException("Removing live media package from search did not complete successfully");
    } catch (LiveScheduleException e) {
      throw e;
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  /**
   * Retrieves the media package from the search index.
   *
   * @param mediaPackageId
   *          the media package id
   * @return the media package in the search index or null if not there
   * @throws LiveScheduleException
   *           if found many media packages with the same id
   */
  MediaPackage getMediaPackageFromSearch(String mediaPackageId) throws LiveScheduleException {
    // Issue #2504: make sure the search index is read by admin so that the media package is always found.
    Organization org = securityService.getOrganization();
    User prevUser = org != null ? securityService.getUser() : null;
    securityService.setUser(SecurityUtil.createSystemUser(systemUserName, org));
    try {
      // Look for the media package in the search index
      SearchQuery query = new SearchQuery().withId(mediaPackageId);
      SearchResult result = searchService.getForAdministrativeRead(query);
      if (result.size() == 0) {
        logger.debug("The search service doesn't know live mediapackage {}", mediaPackageId);
        return null;
      } else if (result.size() > 1) {
        logger.warn("More than one live mediapackage with id {} returned from search service", mediaPackageId);
        throw new LiveScheduleException("More than one live mediapackage with id " + mediaPackageId + " found");
      }
      return result.getItems()[0].getMediaPackage();
    } catch (UnauthorizedException e) {
      logger.warn("Unexpected unauthorized exception when querying the search index for mp {}", mediaPackageId, e);
      return null;
    } finally {
      securityService.setUser(prevUser);
    }
  }

  void setDuration(MediaPackage mp, DublinCoreCatalog dc) {
    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(dc.getFirst(DublinCore.PROPERTY_TEMPORAL));
    long duration = period.getEnd().getTime() - period.getStart().getTime();
    mp.setDuration(duration);
    logger.debug("Live media package {} has start {} and duration {}", mp.getIdentifier(), mp.getDate(),
            mp.getDuration());
  }

  Map<String, Track> addLiveTracks(MediaPackage mp, String caName) throws LiveScheduleException {
    HashMap<String, Track> generatedTracks = new HashMap<String, Track>();
    String mpId = mp.getIdentifier().compact();
    try {
      // If capture agent registered the properties:
      // capture.device.live.resolution.WIDTHxHEIGHT=COMPLETE_STREAMING_URL, use them!
      try {
        Properties caProps = captureAgentService.getAgentCapabilities(caName);
        if (caProps != null) {
          Enumeration<Object> en = caProps.keys();
          while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if (key.startsWith(CA_PROPERTY_RESOLUTION_URL_PREFIX)) {
              String resolution = key.substring(CA_PROPERTY_RESOLUTION_URL_PREFIX.length());
              String url = caProps.getProperty(key);
              // Note: only one flavor is supported in this format (the default: presenter/delivery)
              MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_LIVE_TARGET_FLAVORS);
              String replacedUrl = replaceVariables(mpId, caName, url, flavor, resolution);
              mp.add(buildStreamingTrack(replacedUrl, flavor, streamMimeType, resolution, mp.getDuration()));
            }
          }
        }
      } catch (NotFoundException e) {
        // Capture agent not found so we can't get its properties. Assume the service configuration should
        // be used instead. Note that we can't schedule anything on a CA that has not registered so this is
        // unlikely to happen.
      }

      // Capture agent did not pass any CA_PROPERTY_RESOLUTION_URL_PREFIX property when registering
      // so use the service configuration
      if (mp.getTracks().length == 0) {
        if (liveStreamingUrl == null)
          throw new LiveScheduleException(
                  "Cannot build live tracks because '" + LIVE_STREAMING_URL + "' configuration was not set.");

        for (MediaPackageElementFlavor flavor : liveFlavors) {
          for (int i = 0; i < streamResolution.length; i++) {
            String uri = replaceVariables(mpId, caName, UrlSupport.concat(liveStreamingUrl.toString(), streamName),
                    flavor, streamResolution[i]);
            Track track = buildStreamingTrack(uri, flavor, streamMimeType, streamResolution[i], mp.getDuration());
            mp.add(track);
            generatedTracks.put(flavor + ":" + streamResolution[i], track);
          }
        }
      }
    } catch (URISyntaxException e) {
      throw new LiveScheduleException(e);
    }
    return generatedTracks;
  }

  Track buildStreamingTrack(String uriString, MediaPackageElementFlavor flavor, String mimeType, String resolution,
          long duration) throws URISyntaxException {

    URI uri = new URI(uriString);

    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElement element = elementBuilder.elementFromURI(uri, MediaPackageElement.Type.Track, flavor);
    TrackImpl track = (TrackImpl) element;

    // Set duration and mime type
    track.setDuration(duration);
    track.setLive(true);
    track.setMimeType(MimeTypes.parseMimeType(mimeType));

    VideoStreamImpl video = new VideoStreamImpl("video-" + flavor.getType() + "-" + flavor.getSubtype());
    // Set video resolution
    String[] dimensions = resolution.split("x");
    video.setFrameWidth(Integer.parseInt(dimensions[0]));
    video.setFrameHeight(Integer.parseInt(dimensions[1]));

    track.addStream(video);

    logger.debug("Creating live track element of flavor {}, resolution {}, and url {}",
            new Object[] { flavor, resolution, uriString });

    return track;
  }

  /**
   * Replaces variables in the live stream name. Currently, this is only prepared to handle the following: #{id} = media
   * package id, #{flavor} = type-subtype of flavor, #{caName} = capture agent name, #{resolution} = stream resolution
   */
  String replaceVariables(String mpId, String caName, String toBeReplaced, MediaPackageElementFlavor flavor,
          String resolution) {

    // Substitution pattern: any string in the form #{name}, where 'name' has only word characters: [a-zA-Z_0-9].
    final Pattern pat = Pattern.compile("#\\{(\\w+)\\}");

    Matcher matcher = pat.matcher(toBeReplaced);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      if (matcher.group(1).equals(REPLACE_ID)) {
        matcher.appendReplacement(sb, mpId);
      } else if (matcher.group(1).equals(REPLACE_FLAVOR)) {
        matcher.appendReplacement(sb, flavor.getType() + "-" + flavor.getSubtype());
      } else if (matcher.group(1).equals(REPLACE_CA_NAME)) {
        // Taking the easy route to find the capture agent name...
        matcher.appendReplacement(sb, caName);
      } else if (matcher.group(1).equals(REPLACE_RESOLUTION)) {
        // Taking the easy route to find the capture agent name...
        matcher.appendReplacement(sb, resolution);
      } // else will not replace
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private boolean isLive(MediaPackage mp) {
    Track[] tracks = mp.getTracks();
    if (tracks != null)
      for (Track track : tracks)
        if (track.isLive())
          return true;

    return false;
  }

  /*
   * public void setDublinCoreService(DublinCoreCatalogService service) { this.dublinCoreService = service; }
   *
   * public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
   */

  private JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
    if (serviceRegistry == null)
      throw new IllegalStateException("Can't wait for job status without providing a service registry first");
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, jobPollingInterval, jobs);
    return barrier.waitForJobs();
  }

  Snapshot getSnapshot(String mpId) throws LiveScheduleException {
    AQueryBuilder query = assetManager.createQuery();
    AResult result = query.select(query.snapshot()).where(query.mediaPackageId(mpId).and(query.version().isLatest()))
            .run();
    if (result.getSize() == 0) {
      // Media package not archived?.
      throw new LiveScheduleException(String.format("Unexpected error: media package %s has not been archived.", mpId));
    }
    Opt<ARecord> record = result.getRecords().head();
    if (record.isNone()) {
      // No snapshot?
      throw new LiveScheduleException(String.format("Unexpected error: media package %s has not been archived.", mpId));
    }
    return record.get().getSnapshot().get();
  }

  MediaPackage addAndDistributeElements(Snapshot snapshot) throws LiveScheduleException {
    try {
      MediaPackage mp = (MediaPackage) snapshot.getMediaPackage().clone();

      Set<String> elementIds = new HashSet<String>();
      // Then, add series catalog if needed
      if (StringUtils.isNotEmpty(mp.getSeries())) {
        DublinCoreCatalog catalog = seriesService.getSeries(mp.getSeries());
        // Create temporary catalog and save to workspace
        mp.add(catalog);
        URI uri = workspace.put(mp.getIdentifier().toString(), catalog.getIdentifier(), "series.xml",
                dublinCoreService.serialize(catalog));
        catalog.setURI(uri);
        catalog.setChecksum(null);
        catalog.setFlavor(MediaPackageElements.SERIES);
        elementIds.add(catalog.getIdentifier());
      }

      if (mp.getCatalogs(MediaPackageElements.EPISODE).length > 0)
        elementIds.add(mp.getCatalogs(MediaPackageElements.EPISODE)[0].getIdentifier());
      if (mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE).length > 0)
        elementIds.add(mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE)[0].getIdentifier());

      // Distribute element(s)
      Job distributionJob = downloadDistributionService.distribute(CHANNEL_ID, mp, elementIds, false);
      if (!waitForStatus(distributionJob).isSuccess())
        throw new LiveScheduleException(
                "Element(s) for live media package " + mp.getIdentifier() + " could not be distributed");

      for (String id : elementIds) {
        MediaPackageElement e = mp.getElementById(id);
        // Cleanup workspace/wfr
        mp.remove(e);
        workspace.delete(e.getURI());
      }

      // Add distributed element(s) to mp
      List<MediaPackageElement> distributedElements = (List<MediaPackageElement>) MediaPackageElementParser
              .getArrayFromXml(distributionJob.getPayload());
      for (MediaPackageElement mpe : distributedElements)
        mp.add(mpe);

      return mp;
    } catch (LiveScheduleException e) {
      throw e;
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  MediaPackage replaceAndDistributeAcl(MediaPackage previousMp, AccessControlList acl) throws LiveScheduleException {
    try {
      // This is the mp from the search index
      MediaPackage mp = (MediaPackage) previousMp.clone();

      // Remove previous Acl from the mp
      Attachment[] atts = mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE);
      if (atts.length > 0)
        mp.remove(atts[0]);

      // Attach current ACL to mp, acl will be created in the ws/wfr
      authService.setAcl(mp, AclScope.Episode, acl);
      atts = mp.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE);
      if (atts.length > 0) {
        String aclId = atts[0].getIdentifier();
        // Distribute new acl
        Job distributionJob = downloadDistributionService.distribute(CHANNEL_ID, mp, aclId, false);
        if (!waitForStatus(distributionJob).isSuccess())
          throw new LiveScheduleException(
                  "Acl for live media package " + mp.getIdentifier() + " could not be distributed");

        MediaPackageElement e = mp.getElementById(aclId);
        // Cleanup workspace/wfr
        mp.remove(e);
        workspace.delete(e.getURI());

        // Add distributed acl to mp
        mp.add(MediaPackageElementParser.getFromXml(distributionJob.getPayload()));
      }
      return mp;
    } catch (LiveScheduleException e) {
      throw e;
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  void addLivePublicationChannel(Organization currentOrg, MediaPackage mp, Map<String, Track> generatedTracks) throws LiveScheduleException {
    logger.debug("Adding live channel publication element to media package {}", mp);
    String engageUrlString = null;
    if (currentOrg != null) {
      engageUrlString = StringUtils.trimToNull(currentOrg.getProperties().get(ENGAGE_URL_PROPERTY));
    }
    if (engageUrlString == null) {
      engageUrlString = serverUrl;
      logger.info(
              "Using 'server.url' as a fallback for the non-existing organization level key '{}' for the publication url",
              ENGAGE_URL_PROPERTY);
    }

    try {
      // Create new distribution element
      URI engageUri = URIUtils.resolve(new URI(engageUrlString), PLAYER_PATH + mp.getIdentifier().compact());
      Publication publicationElement = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_ID, engageUri,
              MimeTypes.parseMimeType("text/html"));
      mp.add(publicationElement);
      createOrUpdatePublicationTracks(publicationElement, generatedTracks);
    } catch (URISyntaxException e) {
      throw new LiveScheduleException(e);
    }
  }

  void removeLivePublicationChannel(MediaPackage mp) {
    // Remove publication element
    Publication[] publications = mp.getPublications();
    if (publications != null) {
      for (Publication publication : publications) {
        if (CHANNEL_ID.equals(publication.getChannel()))
          mp.remove(publication);
      }
    }
  }

  private boolean isSameArray(String[] previous, String[] current) {
    Set<String> previousSet = new HashSet<String>(Arrays.asList(previous));
    Set<String> currentSet = new HashSet<String>(Arrays.asList(current));
    return previousSet.equals(currentSet);
  }

  private boolean isSameTrackArray(Track[] previous, Track[] current) {
    Set<Track> previousTracks = new HashSet<Track>(Arrays.asList(previous));
    Set<Track> currentTracks = new HashSet<Track>(Arrays.asList(current));
    if (previousTracks.size() != currentTracks.size())
      return false;
    for (Track tp : previousTracks) {
      Iterator<Track> it = currentTracks.iterator();
      while (it.hasNext()) {
        Track tc = it.next();
        if (tp.getURI().equals(tc.getURI()) && tp.getDuration().equals(tc.getDuration())) {
          currentTracks.remove(tc);
          break;
        }
      }
    }
    if (currentTracks.size() > 0)
      return false;

    return true;
  }

  boolean isSameMediaPackage(MediaPackage previous, MediaPackage current) throws LiveScheduleException {
    return Objects.equals(previous.getTitle(), current.getTitle())
            && Objects.equals(previous.getLanguage(), current.getLanguage())
            && Objects.equals(previous.getSeries(), current.getSeries())
            && Objects.equals(previous.getSeriesTitle(), current.getSeriesTitle())
            && Objects.equals(previous.getDuration(), current.getDuration())
            && Objects.equals(previous.getDate(), current.getDate())
            && isSameArray(previous.getCreators(), current.getCreators())
            && isSameArray(previous.getContributors(), current.getContributors())
            && isSameArray(previous.getSubjects(), current.getSubjects())
            && isSameTrackArray(previous.getTracks(), current.getTracks());
  }

  void retractPreviousElements(MediaPackage previousMp, MediaPackage newMp) throws LiveScheduleException {
    try {
      // Now can retract elements from previous publish. Before creating a retraction
      // job, check if the element url is still used by the new media package.
      Set<String> elementIds = new HashSet<String>();
      for (MediaPackageElement element : previousMp.getElements()) {
        // We don't retract tracks because they are just live links
        if (!Track.TYPE.equals(element.getElementType())) {
          boolean canBeDeleted = true;
          for (MediaPackageElement newElement : newMp.getElements()) {
            if (element.getURI().equals(newElement.getURI())) {
              logger.debug(
                      "Not retracting element {} with URI {} from download distribution because it is still used by updated live media package",
                      element.getIdentifier(), element.getURI());
              canBeDeleted = false;
              break;
            }
          }
          if (canBeDeleted)
            elementIds.add(element.getIdentifier());
        }
      }
      if (elementIds.size() > 0) {
        Job job = downloadDistributionService.retract(CHANNEL_ID, previousMp, elementIds);
        // Wait for retraction to finish
        if (!waitForStatus(job).isSuccess())
          logger.warn("One of the download retract jobs did not complete successfully");
        else
          logger.debug("Retraction of previously published elements complete");
      }
    } catch (DistributionException e) {
      throw new LiveScheduleException(e);
    }
  }

  // === Set by OSGI - begin
  public void setDublinCoreService(DublinCoreCatalogService service) {
    this.dublinCoreService = service;
  }

  public void setSearchService(SearchService service) {
    this.searchService = service;
  }

  public void setSeriesService(SeriesService service) {
    this.seriesService = service;
  }

  public void setServiceRegistry(ServiceRegistry service) {
    this.serviceRegistry = service;
  }

  public void setCaptureAgentService(CaptureAgentStateService service) {
    this.captureAgentService = service;
  }

  public void setDownloadDistributionService(DownloadDistributionService service) {
    if (distributionServiceType.equalsIgnoreCase(service.getDistributionType()))
      this.downloadDistributionService = service;
  }

  public void setWorkspace(Workspace ws) {
    this.workspace = ws;
  }

  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  public void setAuthorizationService(AuthorizationService service) {
    this.authService = service;
  }

  public void setOrganizationService(OrganizationDirectoryService service) {
    this.organizationService = service;
  }

  public void setSecurityService(SecurityService service) {
    this.securityService = service;
  }
  // === Set by OSGI - end

  // === Used by unit tests - begin
  void setJobPollingInterval(long jobPollingInterval) {
    this.jobPollingInterval = jobPollingInterval;
  }

  Cache<String, Version> getSnapshotVersionCache() {
    return this.snapshotVersionCache;
  }
  // === Used by unit tests - end
}
