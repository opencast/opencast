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
package org.opencastproject.workflow.handler.videogrid;

import static java.lang.String.format;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.smil.api.util.SmilUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.videogrid.api.VideoGridService;
import org.opencastproject.videogrid.api.VideoGridServiceException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The workflow definition for handling multiple videos that have overlapping playtime, e.g. webcam videos from
 * a video conference call.
 * Checks which videos are currently playing and dynamically scales them to fit in a single video.
 *
 * Relies on a smil with videoBegin and duration times, as is created by ingest through addPartialTrack.
 * Will pad sections where no video is playing with a background color. This includes beginning and end.
 *
 * Returns the final video to the target flavor
 */
public class VideoGridWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Workflow configuration keys */
  private static final String SOURCE_FLAVORS = "source-flavors";
  private static final String SOURCE_SMIL_FLAVOR = "source-smil-flavor";
  private static final String CONCAT_ENCODING_PROFILE = "concat-encoding-profile";

  private static final String OPT_RESOLUTION = "resolution";
  private static final String OPT_BACKGROUND_COLOR = "background-color";

  private static final String TARGET_FLAVOR = "target-flavor";
  private static final String OPT_TARGET_TAGS = "target-tags";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoGridWorkflowOperationHandler.class);

  /** Constants */
  private static final String NODE_TYPE_VIDEO = "video";

  // TODO: Make ffmpeg commands more "opencasty"
  private static final String[] FFMPEG = {"ffmpeg", "-y", "-v", "warning", "-nostats", "-max_error_rate", "1.0"};
  private static final String FFMPEG_WF_CODEC = "h264"; //"mpeg2video";
  private static final int FFMPEG_WF_FRAMERATE = 24;
  private static final String[] FFMPEG_WF_ARGS = {"-an", "-codec", FFMPEG_WF_CODEC, "-q:v", "2", "-g", Integer.toString(FFMPEG_WF_FRAMERATE * 10), "-pix_fmt", "yuv420p", "-r", Integer.toString(FFMPEG_WF_FRAMERATE)};

  /** Services */
  private Workspace workspace = null;
  private VideoGridService videoGridService = null;
  private MediaInspectionService inspectionService = null;
  private ComposerService composerService = null;

  /** Service Callbacks **/
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
  public void setVideoGridService(VideoGridService videoGridService) {
    this.videoGridService = videoGridService;
  }
  protected void setMediaInspectionService(MediaInspectionService inspectionService) {
    this.inspectionService = inspectionService;
  }
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /** Structs to store data and make code more readable **/
  /**
   * Holds basic information on the final video, which is for example used to appropriately place and scale
   * individual videos.
   */
  class LayoutArea
  {
    private int x = 0;
    private int y = 0;
    private int width = 1920;
    private int height = 1080;
    private String name = "webcam";
    private String bgColor = "0xFFFFFF";

    public int getX() {
      return x;
    }
    public void setX(int x) {
      this.x = x;
    }
    public int getY() {
      return y;
    }
    public void setY(int y) {
      this.y = y;
    }
    public int getWidth() {
      return width;
    }
    public void setWidth(int width) {
      this.width = width;
    }
    public int getHeight() {
      return height;
    }
    public void setHeight(int height) {
      this.height = height;
    }
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public String getBgColor() {
      return bgColor;
    }
    public void setBgColor(String bgColor) {
      this.bgColor = bgColor;
    }

    LayoutArea(int width, int height) {
      this.width = width;
      this.height = height;
    }

    LayoutArea(String name, int x, int y, int width, int height, String bgColor) {
      this(width, height);
      this.name = name;
      this.x = x;
      this.y = y;
      this.bgColor = bgColor;
    }
  }

  /**
   * Holds information on a single video beyond what is usually stored in a Track
   */
  class VideoInfo
  {
    private int aspectRatioWidth = 16;
    private int aspectRatioHeight = 9;

    private long startTime = 0;
    private long duration = 0;
    private Track video;

    public int getAspectRatioWidth() {
      return aspectRatioWidth;
    }
    public void setAspectRatioWidth(int aspectRatioWidth) {
      this.aspectRatioWidth = aspectRatioWidth;
    }
    public int getAspectRatioHeight() {
      return aspectRatioHeight;
    }
    public void setAspectRatioHeight(int aspectRatioHeight) {
      this.aspectRatioHeight = aspectRatioHeight;
    }
    public long getStartTime() {
      return startTime;
    }
    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }
    public long getDuration() {
      return duration;
    }
    public void setDuration(long duration) {
      this.duration = duration;
    }
    public Track getVideo() {
      return video;
    }
    public void setVideo(Track video) {
      this.video = video;
    }


    VideoInfo() {

    }

    VideoInfo(int height, int width) {
      aspectRatioWidth = width;
      aspectRatioHeight = height;
    }

    VideoInfo(Track video, long timeStamp, int aspectRatioHeight, int aspectRatioWidth, long startTime)
    {
      this(aspectRatioHeight, aspectRatioWidth);
      this.video = video;
      this.startTime = startTime;
    }
  }

  /**
   * Pair class for readability
   */
  class Offset
  {
    private int x = 16;
    private int y = 9;

    public int getX() {
      return x;
    }
    public void setX(int x) {
      this.x = x;
    }
    public int getY() {
      return y;
    }
    public void setY(int y) {
      this.y = y;
    }

    Offset(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  /**
   * A section of the complete edit decision list.
   * A new section is defined whenever a video becomes active or inactive.
   * Therefore it contains information on the timing as well as all currently active videos in the section.
   */
  class EditDecisionListSection
  {
    private long timeStamp = 0;
    private long nextTimeStamp = 0;
    private List<VideoInfo> areas;

    public long getTimeStamp() {
      return timeStamp;
    }
    public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
    }
    public long getNextTimeStamp() {
      return nextTimeStamp;
    }
    public void setNextTimeStamp(long nextTimeStamp) {
      this.nextTimeStamp = nextTimeStamp;
    }
    public List<VideoInfo> getAreas() {
      return areas;
    }
    public void setAreas(List<VideoInfo> areas) {
      this.areas = areas;
    }

    EditDecisionListSection()
    {
      areas = new ArrayList<VideoInfo>();
    }
  }

  /**
   * Stores relevant information from the source SMIL
   */
  class StartStopEvent implements Comparable<StartStopEvent>
  {
    private boolean start;
    private long timeStamp;
    private Track video;
    private VideoInfo videoInfo;

    public boolean isStart() {
      return start;
    }
    public void setStart(boolean start) {
      this.start = start;
    }
    public long getTimeStamp() {
      return timeStamp;
    }
    public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
    }
    public VideoInfo getVideoInfo() {
      return videoInfo;
    }
    public void setVideoInfo(VideoInfo videoInfo) {
      this.videoInfo = videoInfo;
    }

    StartStopEvent(boolean start, Track video, long timeStamp, VideoInfo videoInfo)
    {
      this.start = start;
      this.timeStamp = timeStamp;
      this.video = video;
      this.videoInfo = videoInfo;
    }

    @Override
    public int compareTo(StartStopEvent o) {
      return Long.compare(this.timeStamp, o.timeStamp);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running videogrid workflow operation on workflow {}", workflowInstance.getId());

    final MediaPackage mediaPackage = (MediaPackage) workflowInstance.getMediaPackage().clone();

    // Read config options
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final MediaPackageElementFlavor smilFlavor = MediaPackageElementFlavor.parseFlavor(
            getConfig(operation, SOURCE_SMIL_FLAVOR));
    final MediaPackageElementFlavor targetPresenterFlavor = MediaPackageElementFlavor.parseFlavor(
            getConfig(operation, TARGET_FLAVOR));
    String concatEncodingProfile = StringUtils.trimToNull(operation.getConfiguration(CONCAT_ENCODING_PROFILE));

    // Get source flavors
    String sourceFlavorNames = operation.getConfiguration(SOURCE_FLAVORS);
    final List<MediaPackageElementFlavor> sourceFlavors = new ArrayList<>();
    for (String flavorName : asList(sourceFlavorNames)) {
      sourceFlavors.add(MediaPackageElementFlavor.parseFlavor(flavorName));
    }

    // Get tracks from flavor
    final List<Track> sourceTracks = new ArrayList<>();
    for (MediaPackageElementFlavor sourceFlavor: sourceFlavors) {
      TrackSelector trackSelector = new TrackSelector();
      trackSelector.addFlavor(sourceFlavor);
      sourceTracks.addAll(trackSelector.select(mediaPackage, false));
    }

    // No tracks? Skip
    if (sourceTracks.isEmpty()) {
      logger.warn("No tracks in source flavors, skipping ...");
      return createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    // No concat encoding profile? Fail
    if (concatEncodingProfile == null)
      throw new WorkflowOperationException("Encoding profile must be set!");
    EncodingProfile profile = composerService.getProfile(concatEncodingProfile);
    if (profile == null)
      throw new WorkflowOperationException("Encoding profile '" + concatEncodingProfile + "' was not found");


    // Define a general Layout for the final video
    ImmutablePair<Integer, Integer> resolution;
    try {
      resolution = getResolution(getConfig(workflowInstance, OPT_RESOLUTION, "1280x720"));
    } catch (IllegalArgumentException e) {
      logger.warn("Given resolution was not well formatted!");
      throw new WorkflowOperationException(e);
    }
    logger.info("The resolution of the final video: {}/{}", resolution.getLeft(), resolution.getRight());

    // Define a bg color for the final video
    String bgColor = getConfig(workflowInstance, OPT_BACKGROUND_COLOR, "0xFFFFFF");
    final Pattern pattern = Pattern.compile("0x[A-Fa-f0-9]{6}");
    if (!pattern.matcher(bgColor).matches()) {
      logger.warn("Given color {} was not well formatted!", bgColor);
      throw new WorkflowOperationException("Given color was not well formatted!");
    }
    logger.info("The background color of the final video: {}", bgColor);

    // Target tags
    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(OPT_TARGET_TAGS));
    List<String> targetTags = asList(targetTagsOption);

    // Define general layout for the final video
    LayoutArea layoutArea = new LayoutArea("webcam", 0, 0, resolution.getLeft(), resolution.getRight(),
                                            bgColor);

    // Get SMIL catalog
    final SMILDocument smilDocument;
    try {
      smilDocument = SmilUtil.getSmilDocumentFromMediaPackage(mediaPackage, smilFlavor, workspace);
    } catch (SAXException e) {
      throw new WorkflowOperationException("SMIL is not well formatted", e);
    } catch (IOException | NotFoundException e) {
      throw new WorkflowOperationException("SMIL could not be found", e);
    }

    final SMILParElement parallel = (SMILParElement) smilDocument.getBody().getChildNodes().item(0);
    final NodeList sequences = parallel.getTimeChildren();
    final float trackDurationInSeconds = parallel.getDur();
    final long trackDurationInMs = Math.round(trackDurationInSeconds * 1000f);

    // Get Start- and endtime of the final video from SMIL
    long finalStartTime = 0;
    long finalEndTime = trackDurationInMs;

    // Create a list of start and stop events, i.e. every time a new video begins or an old one ends
    // Create list from SMIL from partial ingests
    List<StartStopEvent> events = new ArrayList<>();
    List<Track> videoSourceTracks = new ArrayList<>();

    for (int i = 0; i < sequences.getLength(); i++) {
      final SMILElement item = (SMILElement) sequences.item(i);
      NodeList children = item.getChildNodes();

      for (int j = 0; j < children.getLength(); j++) {
        Node node = children.item(j);
        SMILMediaElement e = (SMILMediaElement) node;

        // Avoid any element that is not a video or of the source type
        if (NODE_TYPE_VIDEO.equals(e.getNodeName())) {
          Track track;
          try {
            track = getTrackByID(e.getId(), sourceTracks);
          } catch (IllegalStateException ex) {
            logger.info("No track corresponding to SMIL ID found, skipping SMIL ID {}", e.getId());
            continue;
          }
          videoSourceTracks.add(track);

          double beginInSeconds = e.getBegin().item(0).getResolvedOffset();
          long beginInMs = Math.round(beginInSeconds * 1000d);
          double durationInSeconds = e.getDur();
          long durationInMs = Math.round(durationInSeconds * 1000d);

          // Gather video information
          VideoInfo videoInfo = new VideoInfo();
          // Aspect Ratio, e.g. 16:9
          List<Track> tmpList = new ArrayList<Track>();
          tmpList.add(track);
          LayoutArea trackDimension = determineDimension(tmpList, true);
          if (trackDimension == null) {
            throw new WorkflowOperationException("One of the source video tracks did not contain a valid video stream or dimension");
          }
          videoInfo.aspectRatioHeight = trackDimension.getHeight();
          videoInfo.aspectRatioWidth = trackDimension.getWidth();
          // "StartTime" is calculated later. It describes how far into the video the next section starts.
          // (E.g. If webcam2 is started 10 seconds after webcam1, the startTime for webcam1 in the next section is 10)
          videoInfo.startTime = 0;

          logger.info("Video information: Width: {}, Height {}, StartTime: {}", videoInfo.aspectRatioWidth,
                  videoInfo.aspectRatioHeight, videoInfo.startTime);

          events.add(new StartStopEvent(true, track, beginInMs, videoInfo));
          events.add(new StartStopEvent(false, track, beginInMs + durationInMs, videoInfo));

        }
      }
    }

    // No events? Skip
    if (events.isEmpty()) {
      logger.warn("Could not generate sections from given SMIL catalogue for tracks in given flavor, skipping ...");
      return createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    // Sort by timestamps ascending
    Collections.sort(events);

    // Create an edit decision list
    List<EditDecisionListSection> videoEdl = new ArrayList<EditDecisionListSection>();
    HashMap<Track, StartStopEvent> activeVideos = new HashMap<>();   // Currently running videos

    // Define starting point
    EditDecisionListSection start = new EditDecisionListSection();
    start.timeStamp = finalStartTime;
    videoEdl.add(start);

    // Define mid-points
    for (StartStopEvent event : events) {
      if (event.start) {
        logger.info("Add start event at {}", event.timeStamp);
        activeVideos.put(event.video, event);
      } else {
        logger.info("Add stop event at {}", event);
        activeVideos.remove(event.video);
      }
      videoEdl.add(createEditDecisionList(event, activeVideos));
    }

    // Define ending point
    EditDecisionListSection endVideo = new EditDecisionListSection();
    endVideo.timeStamp = finalEndTime;
    endVideo.nextTimeStamp = finalEndTime;
    videoEdl.add(endVideo);

    // Pre processing EDL
    for (int i = 0; i < videoEdl.size() - 1; i++) {
      // For calculating cut lengths
      videoEdl.get(i).nextTimeStamp = videoEdl.get(i + 1).timeStamp;
    }

    // Create ffmpeg command for each section
    List<List<String>> commands = new ArrayList<>();          // FFmpeg command
    List<List<Track>> tracksForCommands = new ArrayList<>();  // Tracks used in the FFmpeg command
    for (EditDecisionListSection edl : videoEdl) {
      // A too small duration will result in ffmpeg producing a faulty video, so avoid any section smaller than 50ms
      if (edl.nextTimeStamp - edl.timeStamp < 50) {
        logger.info("Skipping {}-length edl entry", edl.nextTimeStamp - edl.timeStamp);
        continue;
      }
      // Create command for section
      commands.add(compositeSection(layoutArea, edl));
      tracksForCommands.add(edl.getAreas().stream().map(m -> m.getVideo()).collect(Collectors.toList()));
    }

    // Create video tracks for each section
    List<URI> uris = new ArrayList<>();
    for (int i = 0; i < commands.size(); i++) {
      logger.info("Sending command {} of {} to service. Command: {}", i + 1, commands.size(), commands.get(i));

      Job job;
      try {
        job = videoGridService.createPartialTrack(commands.get(i), tracksForCommands.get(i).toArray(new Track[tracksForCommands.get(i).size()]));
      } catch (VideoGridServiceException | org.apache.commons.codec.EncoderException | MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }

      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException(String.format("VideoGrid job for media package '%s' failed", mediaPackage));
      }

      Gson gson = new Gson();
      uris.add(gson.fromJson(job.getPayload(), new TypeToken<URI>() { }.getType()));
    }

    // Parse uris into tracks and enrich them with metadata
    List<Track> tracks = new ArrayList<>();
    for (URI uri : uris) {
      TrackImpl track = new TrackImpl();
      track.setFlavor(targetPresenterFlavor);
      track.setURI(uri);

      Job inspection = null;
      try {
        inspection = inspectionService.enrich(track, true);
      } catch (MediaInspectionException | MediaPackageException e) {
        throw new WorkflowOperationException("Inspection service could not enrich track", e);
      }
      if (!waitForStatus(inspection).isSuccess()) {
        throw new WorkflowOperationException(String.format("Failed to add metadata to track."));
      }

      try {
        tracks.add((TrackImpl) MediaPackageElementParser.getFromXml(inspection.getPayload()));
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException("Could not parse track returned by inspection service", e);
      }
    }

    // Concatenate sections
    Job concatJob = null;
    try {
      concatJob = composerService.concat(composerService.getProfile(concatEncodingProfile).getIdentifier(),
              new Dimension(layoutArea.width,layoutArea.height) , true, tracks.toArray(new Track[tracks.size()]));
    } catch (EncoderException | MediaPackageException e) {
      throw new WorkflowOperationException("The concat job failed", e);
    }
    if (!waitForStatus(concatJob).isSuccess()) {
      throw new WorkflowOperationException("The concat job did not complete successfully.");
    }

    // Add to mediapackage
    if (concatJob.getPayload().length() > 0) {
      Track concatTrack;
      try {
        concatTrack = (Track) MediaPackageElementParser.getFromXml(concatJob.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException("Could not parse track returned by concat service", e);
      }
      concatTrack.setFlavor(targetPresenterFlavor);
      concatTrack.setURI(concatTrack.getURI());
      for (String tag : targetTags) {
        concatTrack.addTag(tag);
      }

      mediaPackage.add(concatTrack);
    } else {
      throw new WorkflowOperationException("Concat operation unsuccessful, no payload returned.");
    }

    try {
      workspace.cleanup(mediaPackage.getIdentifier());
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }

    final WorkflowOperationResult result = createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
    logger.debug("Video Grid operation completed");
    return result;
  }

  /**
   * Create a ffmpeg command that generates a video for the given section
   *
   * The videos passed as part of <code>videoEdl</code> are arranged in a grid layout.
   * The grid layout is calculated in a way  that maximizes area usage (i.e. minimizes the areas where the background
   * color has to be shown) by checking the area usage for each combination of vertical and horizontal rows, based
   * on the resolution of the layout area. The number of tiles per row/column is then used to genrate a complex
   * ffmpeg filter.
   *
   *
   * @param layoutArea
   *          General layout information for the video
   * @param videoEdl
   *          The edit decision list for the current cut
   * @return A command line ready ffmpeg command
   */
  private List<String> compositeSection(LayoutArea layoutArea, EditDecisionListSection videoEdl)
  {
    // Duration for this cut
    long duration = videoEdl.nextTimeStamp - videoEdl.timeStamp;
    logger.info("Cut timeStamp {}, duration {}", videoEdl.timeStamp, duration);

    // Declare ffmpeg command
    String ffmpegFilter = String.format("color=c=%s:s=%dx%d:r=24", layoutArea.bgColor, layoutArea.width, layoutArea.height);

    List<VideoInfo> videos = videoEdl.areas;
    int videoCount = videoEdl.areas.size();

    logger.info("Laying out {} videos in {}", videoCount, layoutArea.name);


    if (videoCount > 0) {
      int tilesH = 0;
      int tilesV = 0;
      int tileWidth = 0;
      int tileHeight = 0;
      int totalArea = 0;

      // Do and exhaustive search to maximize video areas
      for (int tmpTilesV = 1; tmpTilesV < videoCount + 1; tmpTilesV++) {
        int tmpTilesH = (int) Math.ceil((videoCount / (float)tmpTilesV));
        int tmpTileWidth = (int) (2 * Math.floor((float)layoutArea.width / tmpTilesH / 2));
        int tmpTileHeight = (int) (2 * Math.floor((float)layoutArea.height / tmpTilesV / 2));

        if (tmpTileWidth <= 0 || tmpTileHeight <= 0) {
          continue;
        }

        int tmpTotalArea = 0;
        for (VideoInfo video: videos) {
          int videoWidth = video.aspectRatioWidth;
          int videoHeight = video.aspectRatioHeight;
          VideoInfo videoScaled = aspectScale(videoWidth, videoHeight, tmpTileWidth, tmpTileHeight);
          tmpTotalArea += videoScaled.aspectRatioWidth * videoScaled.aspectRatioHeight;
        }

        if (tmpTotalArea > totalArea) {
          tilesH = tmpTilesH;
          tilesV = tmpTilesV;
          tileWidth = tmpTileWidth;
          tileHeight = tmpTileHeight;
          totalArea = tmpTotalArea;
        }
      }


      int tileX = 0;
      int tileY = 0;

      logger.info("Tiling in a {}x{} grid", tilesH, tilesV);

      ffmpegFilter += String.format("[%s_in];", layoutArea.name);

      for (VideoInfo video : videos) {
        //Get videoinfo
        logger.info("tile location ({}, {})", tileX, tileY);
        int videoWidth = video.aspectRatioWidth;
        int videoHeight = video.aspectRatioHeight;
        logger.info("original aspect: {}x{}", videoWidth, videoHeight);

        VideoInfo videoScaled = aspectScale(videoWidth, videoHeight, tileWidth, tileHeight);
        logger.info("scaled size: {}x{}", videoScaled.aspectRatioWidth, videoScaled.aspectRatioHeight);

        Offset offset = padOffset(videoScaled.aspectRatioWidth, videoScaled.aspectRatioHeight, tileWidth, tileHeight);
        logger.info("offset: left: {}, top: {}", offset.x, offset.y);

        // TODO: Get a proper value instead of the badly hardcoded 0
        // Offset in case the pts is greater than 0
        long seekOffset = 0;
        logger.info("seek offset: {}", seekOffset);

        // Webcam videos are variable, low fps; it might be that there's
        // no frame until some time after the seek point. Start decoding
        // 10s before the desired point to avoid this issue.
        long seek = video.startTime - 10000;
        if (seek < 0) {
          seek = 0;
        }

        String padName = String.format("%s_x%d_y%d", layoutArea.name, tileX, tileY);

        // Apply the video start time offset to seek to the correct point.
        // Only actually apply the offset if we're already seeking so we
        // don't start seeking in a file where we've overridden the seek
        // behaviour.
        if (seek > 0) {
          seek = seek + seekOffset;
        }
        // Instead of adding the filepath here, we put a placeholder.
        // This is so that the videogrid service can later replace it, after it put the files in it's workspace
        ffmpegFilter += String.format("movie=%s:sp=%s", "#{" + video.getVideo().getIdentifier() + "}", msToS(seek));
        // Subtract away the offset from the timestamps, so the trimming
        // in the fps filter is accurate
        ffmpegFilter += String.format(",setpts=PTS-%s/TB", msToS(seekOffset));
        // fps filter fills in frames up to the desired start point, and
        // cuts the video there
        ffmpegFilter += String.format(",fps=%d:start_time=%s", FFMPEG_WF_FRAMERATE, msToS(video.startTime));
        // Reset the timestamps to start at 0 so that everything is synced
        // for the video tiling, and scale to the desired size.
        ffmpegFilter += String.format(",setpts=PTS-STARTPTS,scale=%d:%d,setsar=1", videoScaled.aspectRatioWidth, videoScaled.aspectRatioHeight);
        // And finally, pad the video to the desired aspect ratio
        ffmpegFilter += String.format(",pad=w=%d:h=%d:x=%d:y=%d:color=%s", tileWidth, tileHeight, offset.x, offset.y, layoutArea.bgColor);
        ffmpegFilter += String.format("[%s_movie];", padName);

        // In case the video was shorter than expected, we might have to pad
        // it to length. do that by concatenating a video generated by the
        // color filter. (It would be nice to repeat the last frame instead,
        // but there's no easy way to do that.)
        ffmpegFilter += String.format("color=c=%s:s=%dx%d:r=%d", layoutArea.bgColor, tileWidth, tileHeight, FFMPEG_WF_FRAMERATE);
        ffmpegFilter += String.format("[%s_pad];", padName);
        ffmpegFilter += String.format("[%s_movie][%s_pad]concat=n=2:v=1:a=0[%s];", padName, padName, padName);

        tileX += 1;
        if (tileX >= tilesH) {
          tileX = 0;
          tileY += 1;
        }
      }

      // Create the video rows
      int remaining = videoCount;
      for (tileY = 0; tileY < tilesV; tileY++) {
        int thisTilesH = Math.min(tilesH, remaining);
        remaining -= thisTilesH;

        for (tileX = 0; tileX < thisTilesH; tileX++) {
          ffmpegFilter += String.format("[%s_x%d_y%d]", layoutArea.name, tileX, tileY);
        }
        if (thisTilesH > 1) {
          ffmpegFilter += String.format("hstack=inputs=%d,", thisTilesH);
        }
        ffmpegFilter += String.format("pad=w=%d:h=%d:color=%s", layoutArea.width, tileHeight, layoutArea.bgColor);
        ffmpegFilter += String.format("[%s_y%d];", layoutArea.name, tileY);
      }

      // Stack the video rows
      for (tileY = 0; tileY < tilesV; tileY++) {
        ffmpegFilter += String.format("[%s_y%d]", layoutArea.name, tileY);
      }
      if (tilesV > 1) {
        ffmpegFilter += String.format("vstack=inputs=%d,", tilesV);
      }
      ffmpegFilter += String.format("pad=w=%d:h=%d:color=%s", layoutArea.width, layoutArea.height, layoutArea.bgColor);
      ffmpegFilter += String.format("[%s];", layoutArea.name);
      ffmpegFilter += String.format("[%s_in][%s]overlay=x=%d:y=%d", layoutArea.name, layoutArea.name, layoutArea.x, layoutArea.y);

      // Here would be the end of the layoutArea Loop
    }

    ffmpegFilter += String.format(",trim=end=%s", msToS(duration));

    List<String> ffmpegCmd = new ArrayList<String>(Arrays.asList(FFMPEG));
    ffmpegCmd.add("-filter_complex");
    ffmpegCmd.add(ffmpegFilter);
    ffmpegCmd.addAll(Arrays.asList(FFMPEG_WF_ARGS));

    logger.info("Final command:");
    logger.info(String.join(" ", ffmpegCmd));

    return ffmpegCmd;
  }

  /**
   * Scale the video resolution to fit the new resolution while maintaining aspect ratio
   * @param oldWidth
   *          Width of the video
   * @param oldHeight
   *          Height of the video
   * @param newWidth
   *          Intended new width of the video
   * @param newHeight
   *          Intended new height of the video
   * @return
   *          Actual new width and height of the video, guaranteed to be the same or smaller as the intended values
   */
  private VideoInfo aspectScale(int oldWidth, int oldHeight, int newWidth, int newHeight) {
    if ((float)oldWidth / oldHeight > (float)newWidth / newHeight) {
      newHeight = (int) (2 * Math.round((float)oldHeight * newWidth / oldWidth / 2));
    } else {
      newWidth = (int) (2 * Math.round((float)oldWidth * newHeight / oldHeight / 2));
    }
    return new VideoInfo(newHeight, newWidth);
  }

  /**
   * Calculate video offset from borders for ffmpeg pad operation
   * @param videoWidth
   *          Width of the video
   * @param videoHeight
   *          Height of the video
   * @param areaWidth
   *          Width of the area
   * @param areaHeight
   *          Width of the area
   * @return
   *          The position of the video within the padded area
   */
  private Offset padOffset(int videoWidth, int videoHeight, int areaWidth, int areaHeight) {
    int padX = (int) (2 * Math.round((float)(areaWidth - videoWidth) / 4));
    int padY = (int) (2 * Math.round((float)(areaHeight - videoHeight) / 4));
    return new Offset(padX, padY);
  }

  /**
   * Converts milliseconds to seconds and to string
   * @param timestamp
   *          Time in milliseconds, e.g. 12567
   * @return
   *          Time in seconds, e.g. "12.567"
   */
  private String msToS(long timestamp)
  {
    double s = (double)timestamp / 1000;
    return String.format(Locale.US, "%.3f", s);   // Locale.US to get a . instead of a ,
  }

  /**
   * Finds and returns the first track matching the given id in a list of tracks
   * @param trackId
   *          The id of the track we're looking for
   * @param tracks
   *          The collection of tracks we're looking in
   * @return
   *          The first track with the given trackId
   */
  private Track getTrackByID(String trackId, List<Track> tracks) {
    for (Track t : tracks) {
      if (t.getIdentifier().contains(trackId)) {
        logger.debug("Track-Id from smil found in Mediapackage ID: " + t.getIdentifier());
        return t;
      }
    }
    throw new IllegalStateException("No track matching smil Track-id: " + trackId);
  }

  /**
   * Determine the largest dimension of the given list of tracks
   *
   * @param tracks
   *          the list of tracks
   * @param forceDivisible
   *          Whether to enforce the track's dimension to be divisible by two
   * @return the largest dimension from the list of track
   */
  private LayoutArea determineDimension(List<Track> tracks, boolean forceDivisible) {
    Tuple<Track, LayoutArea> trackDimension = getLargestTrack(tracks);
    if (trackDimension == null)
      return null;

    if (forceDivisible && (trackDimension.getB().getHeight() % 2 != 0 || trackDimension.getB().getWidth() % 2 != 0)) {
      LayoutArea scaledDimension = new LayoutArea((trackDimension.getB().getWidth() / 2) * 2, (trackDimension
              .getB().getHeight() / 2) * 2);
      logger.info("Determined output dimension {} scaled down from {} for track {}", scaledDimension,
              trackDimension.getB(), trackDimension.getA());
      return scaledDimension;
    } else {
      logger.info("Determined output dimension {} for track {}", trackDimension.getB(), trackDimension.getA());
      return trackDimension.getB();
    }
  }

  /**
   * Returns the track with the largest resolution from the list of tracks
   *
   * @param tracks
   *          the list of tracks
   * @return a {@link Tuple} with the largest track and it's dimension
   */
  private Tuple<Track, LayoutArea> getLargestTrack(List<Track> tracks) {
    Track track = null;
    LayoutArea dimension = null;
    for (Track t : tracks) {
      if (!t.hasVideo())
        continue;

      VideoStream[] videoStreams = TrackSupport.byType(t.getStreams(), VideoStream.class);
      int frameWidth = videoStreams[0].getFrameWidth();
      int frameHeight = videoStreams[0].getFrameHeight();
      if (dimension == null || (frameWidth * frameHeight) > (dimension.getWidth() * dimension.getHeight())) {
        dimension = new LayoutArea(frameWidth, frameHeight);
        track = t;
      }
    }
    if (track == null || dimension == null)
      return null;

    return Tuple.tuple(track, dimension);
  }

  /**
   * Returns the absolute path of the track
   *
   * @param track
   *          Track whose path you want
   * @return {@String} containing the absolute path of the given track
   * @throws WorkflowOperationException
   */
  private String getTrackPath(Track track) throws WorkflowOperationException {
    File mediaFile;
    try {
      mediaFile = workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new WorkflowOperationException(
              "Error finding the media file in the workspace", e);
    } catch (IOException e) {
      throw new WorkflowOperationException(
              "Error reading the media file in the workspace", e);
    }
    return mediaFile.getAbsolutePath();
  }

  /**
   * Collects the info for the next section of the final video into an object
   * @param event
   *          Event detailing the time a video has become active/inactive
   * @param activeVideos
   *          Currently active videos
   * @return
   */
  private EditDecisionListSection createEditDecisionList(StartStopEvent event, HashMap<Track, StartStopEvent> activeVideos) {
    EditDecisionListSection nextEdl = new EditDecisionListSection();
    nextEdl.timeStamp = event.timeStamp;

    for (Map.Entry<Track, StartStopEvent> activeVideo : activeVideos.entrySet()) {
      nextEdl.areas.add(new VideoInfo(activeVideo.getKey(), event.timeStamp, activeVideo.getValue().videoInfo.aspectRatioHeight,
              activeVideo.getValue().videoInfo.aspectRatioWidth, event.timeStamp - activeVideo.getValue().timeStamp));
    }

    return nextEdl;
  }

  /**
   * Parses a string detailing a resolution into two integers
   * @param s
   *          String of the form "AxB"
   * @return
   *          The width and height
   * @throws IllegalArgumentException
   */
  private ImmutablePair<Integer, Integer> getResolution(String s) throws IllegalArgumentException {
      String[] parts = s.split("x");
      if (parts.length != 2)
        throw new IllegalArgumentException(format("Unable to create resolution from \"%s\"", s));

      return new ImmutablePair<Integer, Integer>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
  }
}
