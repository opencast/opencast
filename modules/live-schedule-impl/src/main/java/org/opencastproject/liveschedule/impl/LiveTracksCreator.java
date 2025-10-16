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
package org.opencastproject.liveschedule.impl;

import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.liveschedule.api.LiveScheduleException;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveTracksCreator {
  private static final Logger logger = LoggerFactory.getLogger(LiveTracksCreator.class);

  private final CaptureAgentStateService captureAgentService;

  /**
   * Variables that can be replaced in stream name
   */
  public static final String REPLACE_ID = "id";
  static final String REPLACE_FLAVOR = "flavor";
  public static final String REPLACE_CA_NAME = "caName";
  public static final String REPLACE_RESOLUTION = "resolution";

  // If the capture agent registered this property, we expect to get a resolution and
  // a url in the following format:
  // capture.device.live.resolution.WIDTHxHEIGHT=COMPLETE_STREAMING_URL e.g.
  // capture.device.live.resolution.960x270=
  // rtmp://cp398121.live.edgefcs.net/live/dev-epiphan005-2-presenter-delivery.stream-960x270_1_200@355694
  public static final String CA_PROPERTY_RESOLUTION_URL_PREFIX = "capture.device.live.resolution.";

  private final String liveStreamingUrl;
  private final String streamName;
  private final String[] streamResolution;
  private final List<MediaPackageElementFlavor> liveFlavors;
  private final MediaPackageElementFlavor defaultLiveFlavor;
  private final String streamMimeType;

  public LiveTracksCreator(CaptureAgentStateService captureAgentService, String streamMimeType, String liveStreamingUrl,
      String streamName, String[] streamResolution, List<MediaPackageElementFlavor> liveFlavors,
      MediaPackageElementFlavor defaultLiveFlavor) {
    this.captureAgentService = captureAgentService;
    this.streamMimeType = streamMimeType;
    this.liveStreamingUrl = liveStreamingUrl;
    this.streamName = streamName;
    this.streamResolution = streamResolution;
    this.liveFlavors = liveFlavors;
    this.defaultLiveFlavor = defaultLiveFlavor;
  }

  public List<Track> createLiveTracks(String mpId, Date startDate, Date endDate, String agentId)
          throws LiveScheduleException {

    long duration = endDate.getTime() - startDate.getTime();
    List<Track> generatedTracks = new ArrayList<>();

    try {
      // If capture agent registered the properties:
      // capture.device.live.resolution.WIDTHxHEIGHT=COMPLETE_STREAMING_URL, use them!
      try {
        Properties caProps = captureAgentService.getAgentCapabilities(agentId);
        if (caProps != null) {
          Enumeration<Object> en = caProps.keys();
          while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if (key.startsWith(CA_PROPERTY_RESOLUTION_URL_PREFIX)) {
              String resolution = key.substring(CA_PROPERTY_RESOLUTION_URL_PREFIX.length());
              String url = caProps.getProperty(key);
              // Note: only one flavor is supported in this format (the default: presenter/delivery)
              String replacedUrl = replaceVariables(mpId, agentId, url, defaultLiveFlavor, resolution);
              Track track = buildStreamingTrack(replacedUrl, defaultLiveFlavor, streamMimeType, resolution, duration);
              generatedTracks.add(track);
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
      if (generatedTracks.isEmpty()) {
        if (liveStreamingUrl == null) {
          throw new LiveScheduleException(
              "Cannot build live tracks because live stream URL configuration was not set.");
        }

        for (MediaPackageElementFlavor flavor : liveFlavors) {
          for (String resolution : streamResolution) {
            String uri = replaceVariables(mpId, agentId, UrlSupport.concat(liveStreamingUrl, streamName), flavor,
                resolution);
            Track track = buildStreamingTrack(uri, flavor, streamMimeType, resolution, duration);
            generatedTracks.add(track);
          }
        }
      }
    } catch (URISyntaxException e) {
      throw new LiveScheduleException(e);
    }
    return generatedTracks;
  }

  private Track buildStreamingTrack(String uriString, MediaPackageElementFlavor flavor, String mimeType,
      String resolution, long duration) throws URISyntaxException {
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

    logger.debug("Creating live track element of flavor {}, resolution {}, and url {}", flavor, resolution, uriString);

    return track;
  }

  /**
   * Replaces variables in the live stream name. Currently, this is only prepared to handle the following: #{id} = media
   * package id, #{flavor} = type-subtype of flavor, #{caName} = capture agent name, #{resolution} = stream resolution
   */
  private String replaceVariables(String mpId, String caName, String toBeReplaced, MediaPackageElementFlavor flavor,
      String resolution) throws LiveScheduleException {

    // Substitution pattern: any string in the form #{name}, where 'name' has only word characters: [a-zA-Z_0-9].
    final Pattern pat = Pattern.compile("#\\{(\\w+)}");

    Matcher matcher = pat.matcher(toBeReplaced);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      switch (matcher.group(1)) {
        case REPLACE_ID -> matcher.appendReplacement(sb, mpId);
        case REPLACE_FLAVOR -> matcher.appendReplacement(sb, flavor.getType() + "-" + flavor.getSubtype());
        case REPLACE_CA_NAME ->
          // Taking the easy route to find the capture agent name...
            matcher.appendReplacement(sb, caName);
        case REPLACE_RESOLUTION ->
          // Taking the easy route to find the capture agent name...
            matcher.appendReplacement(sb, resolution);
        default -> throw new LiveScheduleException("Unexpected value: " + matcher.group(1));
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
