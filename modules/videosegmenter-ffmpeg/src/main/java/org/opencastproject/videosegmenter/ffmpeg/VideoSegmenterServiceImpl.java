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

package org.opencastproject.videosegmenter.ffmpeg;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.MediaLocator;
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl;
import org.opencastproject.metadata.mpeg7.MediaRelTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.MediaTimePointImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videosegmenter.api.VideoSegmenterException;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.io.LineReader;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Media analysis plugin that takes a video stream and extracts video segments
 * by trying to detect slide and/or scene changes.
 *
 * This plugin runs
 *
 * <pre>
 * ffmpeg -nostats -i in.mp4 -filter:v 'select=gt(scene\,0.04),showinfo' -f null - 2&gt;&amp;1 | grep Parsed_showinfo_1
 * </pre>
 */
public class VideoSegmenterServiceImpl extends AbstractJobProducer implements
VideoSegmenterService, ManagedService {

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "videosegments";

  /** List of available operations on jobs */
  private enum Operation {
    Segment
  };

  /** Path to the executable */
  protected String binary;

  public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Name of the constant used to retrieve the stability threshold */
  public static final String OPT_STABILITY_THRESHOLD = "stabilitythreshold";

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  public static final int DEFAULT_STABILITY_THRESHOLD = 60;

  /** Name of the constant used to retrieve the changes threshold */
  public static final String OPT_CHANGES_THRESHOLD = "changesthreshold";

  /** Default value for the number of pixels that may change between two frames without considering them different */
  public static final float DEFAULT_CHANGES_THRESHOLD = 0.025f; // 2.5% change

  /** Name of the constant used to retrieve the preferred number of segments */
  public static final String OPT_PREF_NUMBER = "prefNumber";

  /** Default value for the preferred number of segments */
  public static final int DEFAULT_PREF_NUMBER = 30;

  /** Name of the constant used to retrieve the maximum number of cycles */
  public static final String OPT_MAX_CYCLES = "maxCycles";

  /** Default value for the maximum number of cycles */
  public static final int DEFAULT_MAX_CYCLES = 3;

  /** Name of the constant used to retrieve the maximum tolerance for result */
  public static final String OPT_MAX_ERROR = "maxError";

  /** Default value for the maximum tolerance for result */
  public static final float DEFAULT_MAX_ERROR = 0.25f;

  /** Name of the constant used to retrieve the absolute maximum number of segments */
  public static final String OPT_ABSOLUTE_MAX = "absoluteMax";

  /** Default value for the absolute maximum number of segments */
  public static final int DEFAULT_ABSOLUTE_MAX = 150;

  /** Name of the constant used to retrieve the absolute minimum number of segments */
  public static final String OPT_ABSOLUTE_MIN = "absoluteMin";

  /** Default value for the absolute minimum number of segments */
  public static final int DEFAULT_ABSOLUTE_MIN = 3;

  /** Name of the constant used to retrieve the option whether segments numbers depend on track duration */
  public static final String OPT_DURATION_DEPENDENT = "durationDependent";

  /** Default value for the option whether segments numbers depend on track duration */
  public static final boolean DEFAULT_DURATION_DEPENDENT = false;

  /** The load introduced on the system by a segmentation job */
  public static final float DEFAULT_SEGMENTER_JOB_LOAD = 0.3f;

  /** The key to look for in the service configuration file to override the DEFAULT_CAPTION_JOB_LOAD */
  public static final String SEGMENTER_JOB_LOAD_KEY = "job.load.videosegmenter";

  /** The load introduced on the system by creating a caption job */
  private float segmenterJobLoad = DEFAULT_SEGMENTER_JOB_LOAD;

  /** The logging facility */
  protected static final Logger logger = LoggerFactory
    .getLogger(VideoSegmenterServiceImpl.class);

  /** Number of pixels that may change between two frames without considering them different */
  protected float changesThreshold = DEFAULT_CHANGES_THRESHOLD;

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  protected int stabilityThreshold = DEFAULT_STABILITY_THRESHOLD;

  /** The minimum segment length in seconds for creation of segments from ffmpeg output */
  protected int stabilityThresholdPrefilter = 1;

  /** The number of segments that should be generated */
  protected int prefNumber = DEFAULT_PREF_NUMBER;

  /** The number of cycles after which the optimization of the number of segments is forced to end */
  protected int maxCycles = DEFAULT_MAX_CYCLES;

  /** The tolerance with which the optimization of the number of segments is considered successful */
  protected float maxError = DEFAULT_MAX_ERROR;

  /** The absolute maximum for the number of segments whose compliance will be enforced after the optimization*/
  protected int absoluteMax = DEFAULT_ABSOLUTE_MAX;

  /** The absolute minimum for the number of segments whose compliance will be enforced after the optimization*/
  protected int absoluteMin = DEFAULT_ABSOLUTE_MIN;

  /** The boolean that defines whether segment numbers are interpreted as absolute or relative to track duration */
  protected boolean durationDependent = DEFAULT_DURATION_DEPENDENT;

  /** Reference to the receipt service */
  protected ServiceRegistry serviceRegistry = null;

  /** The mpeg-7 service */
  protected Mpeg7CatalogService mpeg7CatalogService = null;

  /** The workspace to use when retrieving remote media files */
  protected Workspace workspace = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * Creates a new instance of the video segmenter service.
   */
  public VideoSegmenterServiceImpl() {
    super(JOB_TYPE);
    this.binary = FFMPEG_BINARY_DEFAULT;
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    /* Configure segmenter */
    final String path = cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG);
    this.binary = path == null ? FFMPEG_BINARY_DEFAULT : path;
    logger.debug("Configuration {}: {}", FFMPEG_BINARY_CONFIG, FFMPEG_BINARY_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.debug("Configuring the videosegmenter");

    // Stability threshold
    if (properties.get(OPT_STABILITY_THRESHOLD) != null) {
      String threshold = (String) properties.get(OPT_STABILITY_THRESHOLD);
      try {
        stabilityThreshold = Integer.parseInt(threshold);
        logger.info("Stability threshold set to {} consecutive frames", stabilityThreshold);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's stability threshold", threshold);
      }
    }

    // Changes threshold
    if (properties.get(OPT_CHANGES_THRESHOLD) != null) {
      String threshold = (String) properties.get(OPT_CHANGES_THRESHOLD);
      try {
        changesThreshold = Float.parseFloat(threshold);
        logger.info("Changes threshold set to {}", changesThreshold);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's changes threshold", threshold);
      }
    }

    // Preferred Number of Segments
    if (properties.get(OPT_PREF_NUMBER) != null) {
      String number = (String) properties.get(OPT_PREF_NUMBER);
      try {
        prefNumber = Integer.parseInt(number);
        logger.info("Preferred number of segments set to {}", prefNumber);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's preferred number of segments", number);
      }
    }

    // Maximum number of cycles
    if (properties.get(OPT_MAX_CYCLES) != null) {
      String number = (String) properties.get(OPT_MAX_CYCLES);
      try {
        maxCycles = Integer.parseInt(number);
        logger.info("Maximum number of cycles set to {}", maxCycles);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's maximum number of cycles", number);
      }
    }

    // Absolute maximum number of segments
    if (properties.get(OPT_ABSOLUTE_MAX) != null) {
      String number = (String) properties.get(OPT_ABSOLUTE_MAX);
      try {
        absoluteMax = Integer.parseInt(number);
        logger.info("Absolute maximum number of segments set to {}", absoluteMax);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's absolute maximum number of segments", number);
      }
    }

    // Absolute minimum number of segments
    if (properties.get(OPT_ABSOLUTE_MIN) != null) {
      String number = (String) properties.get(OPT_ABSOLUTE_MIN);
      try {
        absoluteMin = Integer.parseInt(number);
        logger.info("Absolute minimum number of segments set to {}", absoluteMin);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's absolute minimum number of segments", number);
      }
    }

    // Dependency on video duration
    if (properties.get(OPT_DURATION_DEPENDENT) != null) {
      String value = (String) properties.get(OPT_DURATION_DEPENDENT);
      try {
        durationDependent = Boolean.parseBoolean(value);
        logger.info("Dependency on video duration is set to {}", durationDependent);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's dependency on video duration", value);
      }
    }

    segmenterJobLoad = LoadUtil.getConfiguredLoadValue(properties, SEGMENTER_JOB_LOAD_KEY, DEFAULT_SEGMENTER_JOB_LOAD, serviceRegistry);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.videosegmenter.api.VideoSegmenterService#segment(org.opencastproject.mediapackage.Track)
   */
  public Job segment(Track track) throws VideoSegmenterException,
         MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE,
          Operation.Segment.toString(),
          Arrays.asList(MediaPackageElementParser.getAsXml(track)), segmenterJobLoad);
    } catch (ServiceRegistryException e) {
      throw new VideoSegmenterException("Unable to create a job", e);
    }
  }

  /**
   * Starts segmentation on the video track identified by
   * <code>mediapackageId</code> and <code>elementId</code> and returns a
   * receipt containing the final result in the form of anMpeg7Catalog.
   *
   * @param track
   *            the element to analyze
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws VideoSegmenterException
   */
  protected Catalog segment(Job job, Track track)
    throws VideoSegmenterException, MediaPackageException {

    // Make sure the element can be analyzed using this analysis
    // implementation
    if (!track.hasVideo()) {
      logger.warn("Element {} is not a video track", track);
      throw new VideoSegmenterException("Element is not a video track");
    }

    try {
      Mpeg7Catalog mpeg7;

      File mediaFile = null;
      URL mediaUrl = null;
      try {
        mediaFile = workspace.get(track.getURI());
        mediaUrl = mediaFile.toURI().toURL();
      } catch (NotFoundException e) {
        throw new VideoSegmenterException(
            "Error finding the video file in the workspace", e);
      } catch (IOException e) {
        throw new VideoSegmenterException(
            "Error reading the video file in the workspace", e);
      }

      if (track.getDuration() == null)
        throw new MediaPackageException("Track " + track
            + " does not have a duration");
      logger.info("Track {} loaded, duration is {} s", mediaUrl,
          track.getDuration() / 1000);

      MediaTime contentTime = new MediaRelTimeImpl(0,
          track.getDuration());
      MediaLocator contentLocator = new MediaLocatorImpl(track.getURI());

      Video videoContent;

      logger.debug("changesThreshold: {}, stabilityThreshold: {}", changesThreshold, stabilityThreshold);
      logger.debug("prefNumber: {}, maxCycles: {}", prefNumber, maxCycles);

      boolean endOptimization = false;
      int cycleCount = 0;
      LinkedList<Segment> segments;
      LinkedList<OptimizationStep> optimizationList = new LinkedList<OptimizationStep>();
      LinkedList<OptimizationStep> unusedResultsList = new LinkedList<OptimizationStep>();
      OptimizationStep stepBest = new OptimizationStep();

      // local copy of changesThreshold, that can safely be changed over optimization iterations
      float changesThresholdLocal = changesThreshold;

      // local copies of prefNumber, absoluteMin and absoluteMax, to make a dependency on track length possible
      int prefNumberLocal = prefNumber;
      int absoluteMaxLocal = absoluteMax;
      int absoluteMinLocal = absoluteMin;

      // if the number of segments should depend on the duration of the track, calculate new values for prefNumber,
      // absoluteMax and absoluteMin with the duration of the track
      if (durationDependent) {
        double trackDurationInHours = track.getDuration() / 3600000.0;
        prefNumberLocal = (int) Math.round(trackDurationInHours * prefNumberLocal);
        absoluteMaxLocal = (int) Math.round(trackDurationInHours * absoluteMax);
        absoluteMinLocal = (int) Math.round(trackDurationInHours * absoluteMin);

        //make sure prefNumberLocal will never be 0 or negative
        if (prefNumberLocal <= 0) {
          prefNumberLocal = 1;
        }

        logger.info("Numbers of segments are set to be relative to track duration. Therefore for {} the preferred "
                + "number of segments is {}", mediaUrl, prefNumberLocal);
      }

      logger.info("Starting video segmentation of {}", mediaUrl);


      // optimization loop to get a segmentation with a number of segments close
      // to the desired number of segments
      while (!endOptimization) {

        mpeg7 = mpeg7CatalogService.newInstance();
        videoContent = mpeg7.addVideoContent("videosegment",
            contentTime, contentLocator);


        // run the segmentation with FFmpeg
        segments = runSegmentationFFmpeg(track, videoContent, mediaFile, changesThresholdLocal);


        // calculate errors for "normal" and filtered segmentation
        // and compare them to find better optimization.
        // "normal"
        OptimizationStep currentStep = new OptimizationStep(stabilityThreshold,
                changesThresholdLocal, segments.size(), prefNumberLocal, mpeg7, segments);
        // filtered
        LinkedList<Segment> segmentsNew = new LinkedList<Segment>();
        OptimizationStep currentStepFiltered = new OptimizationStep(
                stabilityThreshold, changesThresholdLocal, 0,
                prefNumberLocal, filterSegmentation(segments, track, segmentsNew, stabilityThreshold * 1000), segments);
        currentStepFiltered.setSegmentNumAndRecalcErrors(segmentsNew.size());

        logger.info("Segmentation yields {} segments after filtering", segmentsNew.size());

        OptimizationStep currentStepBest;

        // save better optimization in optimizationList
        //
        // the unfiltered segmentation is better if
        // - the error is smaller than the error of the filtered segmentation
        // OR - the filtered number of segments is smaller than the preferred number
        //    - and the unfiltered number of segments is bigger than a value that should roughly estimate how many
        //          segments with the length of the stability threshold could maximally be in a video
        //          (this is to make sure that if there are e.g. 1000 segments and the filtering would yield
        //           smaller and smaller results, the stability threshold won't be optimized in the wrong direction)
        //    - and the filtered segmentation is not already better than the maximum error
        if (currentStep.getErrorAbs() <= currentStepFiltered.getErrorAbs() || (segmentsNew.size() < prefNumberLocal
                && currentStep.getSegmentNum() > (track.getDuration() / 1000.0f) / (stabilityThreshold / 2)
                && !(currentStepFiltered.getErrorAbs() <= maxError))) {

          optimizationList.add(currentStep);
          Collections.sort(optimizationList);
          currentStepBest = currentStep;
          unusedResultsList.add(currentStepFiltered);
        } else {
          optimizationList.add(currentStepFiltered);
          Collections.sort(optimizationList);
          currentStepBest = currentStepFiltered;
        }

        cycleCount++;

        logger.debug("errorAbs = {}, error = {}", currentStep.getErrorAbs(), currentStep.getError());
        logger.debug("changesThreshold = {}", changesThresholdLocal);
        logger.debug("cycleCount = {}", cycleCount);

        // end optimization if maximum number of cycles is reached or if the segmentation is good enough
        if (cycleCount >= maxCycles || currentStepBest.getErrorAbs() <= maxError) {
          endOptimization = true;
          if (optimizationList.size() > 0) {
            if (optimizationList.getFirst().getErrorAbs() <= optimizationList.getLast().getErrorAbs()
                && optimizationList.getFirst().getError() >= 0) {
              stepBest = optimizationList.getFirst();
            } else {
              stepBest = optimizationList.getLast();
            }
          }

          // just to be sure, check if one of the unused results was better
          for (OptimizationStep currentUnusedStep : unusedResultsList) {
            if (currentUnusedStep.getErrorAbs() < stepBest.getErrorAbs()) {
              stepBest = unusedResultsList.getFirst();
            }
          }


        // continue optimization, calculate new changes threshold for next iteration of optimization
        } else {
          OptimizationStep first = optimizationList.getFirst();
          OptimizationStep last = optimizationList.getLast();
          // if this was the first iteration or there are only positive or negative errors,
          // estimate a new changesThreshold based on the one yielding the smallest error
          if (optimizationList.size() == 1 || first.getError() < 0 || last.getError() > 0) {
            if (currentStepBest.getError() >= 0) {
              // if the error is smaller or equal to 1, increase changes threshold weighted with the error
              if (currentStepBest.getError() <= 1) {
                changesThresholdLocal += changesThresholdLocal * currentStepBest.getError();
              } else {
                  // if there are more than 2000 segments in the first iteration, set changes threshold to 0.2
                  // to faster reach reasonable segment numbers
                if (cycleCount <= 1 && currentStep.getSegmentNum() > 2000) {
                  changesThresholdLocal = 0.2f;
                // if the error is bigger than one, double the changes threshold, because multiplying
                // with a large error can yield a much too high changes threshold
                } else {
                changesThresholdLocal *= 2;
                }
              }
            } else {
                changesThresholdLocal /= 2;
            }

            logger.debug("onesided optimization yields new changesThreshold = {}", changesThresholdLocal);
          // if there are already iterations with positive and negative errors, choose a changesThreshold between those
          } else {
            // for simplicity a linear relationship between the changesThreshold
            // and the number of generated segments is assumed and based on that
            // the expected correct changesThreshold is calculated

            // the new changesThreshold is calculated by averaging the the mean and the mean weighted with errors
            // because this seemed to yield better results in several cases

            float x = (first.getSegmentNum() - prefNumberLocal) / (float)(first.getSegmentNum() - last.getSegmentNum());
            float newX = ((x + 0.5f) * 0.5f);
            changesThresholdLocal = first.getChangesThreshold() * (1 - newX) + last.getChangesThreshold() * newX;
            logger.debug("doublesided optimization yields new changesThreshold = {}", changesThresholdLocal);
          }
        }
      }


      // after optimization of the changes threshold, the minimum duration for a segment
      // (stability threshold) is optimized if the result is still not good enough
      int threshLow = stabilityThreshold * 1000;
      int threshHigh = threshLow + (threshLow / 2);

      LinkedList<Segment> tmpSegments;
      float smallestError = Float.MAX_VALUE;
      int bestI = threshLow;
      segments = stepBest.getSegments();

      // if the error is negative (which means there are already too few segments) or if the error
      // is smaller than the maximum error, the stability threshold will not be optimized
      if (stepBest.getError() <= maxError) {
        threshHigh = stabilityThreshold * 1000;
      }
      for (int i = threshLow; i <= threshHigh; i = i + 1000) {
        tmpSegments = new LinkedList<Segment>();
        filterSegmentation(segments, track, tmpSegments, i);
        float newError = OptimizationStep.calculateErrorAbs(tmpSegments.size(), prefNumberLocal);
        if (newError < smallestError) {
          smallestError = newError;
          bestI = i;
        }
      }
      tmpSegments = new LinkedList<Segment>();
      mpeg7 = filterSegmentation(segments, track, tmpSegments, bestI);

      // for debugging: output of final segmentation after optimization
      logger.debug("result segments:");
      for (int i = 0; i < tmpSegments.size(); i++) {
        int[] tmpLog2 = new int[7];
        tmpLog2[0] = tmpSegments.get(i).getMediaTime().getMediaTimePoint().getHour();
        tmpLog2[1] = tmpSegments.get(i).getMediaTime().getMediaTimePoint().getMinutes();
        tmpLog2[2] = tmpSegments.get(i).getMediaTime().getMediaTimePoint().getSeconds();
        tmpLog2[3] = tmpSegments.get(i).getMediaTime().getMediaDuration().getHours();
        tmpLog2[4] = tmpSegments.get(i).getMediaTime().getMediaDuration().getMinutes();
        tmpLog2[5] = tmpSegments.get(i).getMediaTime().getMediaDuration().getSeconds();
        Object[] tmpLog1 = {tmpLog2[0], tmpLog2[1], tmpLog2[2], tmpLog2[3], tmpLog2[4], tmpLog2[5], tmpLog2[6]};
        tmpLog1[6] = tmpSegments.get(i).getIdentifier();
        logger.debug("s:{}:{}:{}, d:{}:{}:{}, {}", tmpLog1);
      }

      logger.info("Optimized Segmentation yields (after {} iteration" + (cycleCount == 1 ? "" : "s") + ") {} segments",
          cycleCount, tmpSegments.size());

      // if no reasonable segmentation could be found, instead return a uniform segmentation
      if (tmpSegments.size() < absoluteMinLocal || tmpSegments.size() > absoluteMaxLocal) {
        mpeg7 = uniformSegmentation(track, tmpSegments, prefNumberLocal);
        logger.info("Since no reasonable segmentation could be found, a uniform segmentation was created");
      }



      Catalog mpeg7Catalog = (Catalog) MediaPackageElementBuilderFactory
        .newInstance().newElementBuilder()
        .newElement(Catalog.TYPE, MediaPackageElements.SEGMENTS);
      URI uri;
      try {
        uri = workspace.putInCollection(COLLECTION_ID, job.getId()
            + ".xml", mpeg7CatalogService.serialize(mpeg7));
      } catch (IOException e) {
        throw new VideoSegmenterException(
            "Unable to put the mpeg7 catalog into the workspace", e);
      }
      mpeg7Catalog.setURI(uri);

      logger.info("Finished video segmentation of {}", mediaUrl);
      return mpeg7Catalog;
    } catch (Exception e) {
      logger.warn("Error segmenting " + track, e);
      if (e instanceof VideoSegmenterException) {
        throw (VideoSegmenterException) e;
      } else {
        throw new VideoSegmenterException(e);
      }
    }
  }

  /**
   * Does the actual segmentation with an FFmpeg call, adds the segments to the given videoContent of a catalog and
   * returns a list with the resulting segments
   *
   * @param track the element to analyze
   * @param videoContent the videoContent of the Mpeg7Catalog that the segments should be added to
   * @param mediaFile the file of the track to analyze
   * @param changesThreshold the changesThreshold that is used as option for the FFmpeg call
   * @return a list of the resulting segments
   * @throws IOException
   * @throws VideoSegmenterException
   */
  protected LinkedList<Segment> runSegmentationFFmpeg(Track track, Video videoContent, File mediaFile,
          float changesThreshold) throws IOException, VideoSegmenterException {

    String[] command = new String[] { binary, "-nostats", "-i", mediaFile.getAbsolutePath(),
      "-filter:v", "select=gt(scene\\," + changesThreshold + "),showinfo", "-f", "null", "-"};

    logger.info("Detecting video segments using command: {}", (Object) command);

    ProcessBuilder pbuilder = new ProcessBuilder(command);
    List<String> segmentsStrings = new LinkedList<String>();
    Process process = pbuilder.start();
    BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getErrorStream()));
    try {
      LineReader lr = new LineReader(reader);
      String line = lr.readLine();
      while (null != line) {
        if (line.startsWith("[Parsed_showinfo")) {
          segmentsStrings.add(line);
        }
        line = lr.readLine();
      }
    } catch (IOException e) {
      logger.error("Error executing ffmpeg: {}", e.getMessage());
    } finally {
      reader.close();
    }

    // [Parsed_showinfo_1 @ 0x157fb40] n:0 pts:12 pts_time:12 pos:227495
    // fmt:rgb24 sar:0/1 s:320x240 i:P iskey:1 type:I checksum:8DF39EA9
    // plane_checksum:[8DF39EA9]

    int segmentcount = 1;
    LinkedList<Segment> segments = new LinkedList<Segment>();

    if (segmentsStrings.size() == 0) {
      Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segment-" + segmentcount);
      s.setMediaTime(new MediaRelTimeImpl(0, track.getDuration()));
      segments.add(s);
    } else {
      long starttime = 0;
      long endtime = 0;
      Pattern pattern = Pattern.compile("pts_time\\:\\d+(\\.\\d+)?");
      for (String seginfo : segmentsStrings) {
        Matcher matcher = pattern.matcher(seginfo);
        String time = "";
        while (matcher.find()) {
          time = matcher.group().substring(9);
        }
        if ("".equals(time)) {
          // continue if the showinfo does not contain any time information. This may happen since the FFmpeg showinfo
          // filter is used for multiple purposes.
          continue;
        }
        try {
          endtime = Math.round(Float.parseFloat(time) * 1000);
        } catch (NumberFormatException e) {
          logger.error("Unable to parse FFmpeg output, likely FFmpeg version mismatch!", e);
          throw new VideoSegmenterException(e);
        }
        long segmentLength = endtime - starttime;
        if (1000 * stabilityThresholdPrefilter < segmentLength) {
          Segment segment = videoContent.getTemporalDecomposition()
              .createSegment("segment-" + segmentcount);
          segment.setMediaTime(new MediaRelTimeImpl(starttime,
              endtime - starttime));
          logger.debug("Created segment {} at start time {} with duration {}", segmentcount, starttime, endtime);
          segments.add(segment);
          segmentcount++;
          starttime = endtime;
        }
      }
      // Add last segment
      Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segment-" + segmentcount);
      s.setMediaTime(new MediaRelTimeImpl(starttime, track.getDuration() - starttime));
      logger.debug("Created segment {} at start time {} with duration {}", segmentcount, starttime,
              track.getDuration() - endtime);
      segments.add(s);
    }

   logger.info("Segmentation of {} yields {} segments",
           mediaFile.toURI().toURL(), segments.size());

    return segments;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      switch (op) {
        case Segment:
          Track track = (Track) MediaPackageElementParser
            .getFromXml(arguments.get(0));
          Catalog catalog = segment(job, track);
          return MediaPackageElementParser.getAsXml(catalog);
        default:
          throw new IllegalStateException(
              "Don't know how to handle operation '" + operation
              + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException(
          "This service can't handle operations of type '" + op + "'",
          e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException(
          "This argument list for operation '" + op
          + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '"
          + op + "'", e);
    }
  }

  /**
   * Merges small subsequent segments (with high difference) into a bigger one
   *
   * @param segments list of segments to be filtered
   * @param track the track that is segmented
   * @param segmentsNew will be set to list of new segments (pass null if not required)
   * @return Mpeg7Catalog that can later be saved in a Catalog as endresult
   */
  protected Mpeg7Catalog filterSegmentation(
          LinkedList<Segment> segments, Track track, LinkedList<Segment> segmentsNew) {
    int mergeThresh = stabilityThreshold * 1000;
    return filterSegmentation(segments, track, segmentsNew, mergeThresh);
  }


  /**
   * Merges small subsequent segments (with high difference) into a bigger one
   *
   * @param segments list of segments to be filtered
   * @param track the track that is segmented
   * @param segmentsNew will be set to list of new segments (pass null if not required)
   * @param mergeThresh minimum duration for a segment in milliseconds
   * @return Mpeg7Catalog that can later be saved in a Catalog as endresult
   */
  protected Mpeg7Catalog filterSegmentation(
          LinkedList<Segment> segments, Track track, LinkedList<Segment> segmentsNew, int mergeThresh) {
    if (segmentsNew == null) {
      segmentsNew = new LinkedList<Segment>();
    }
    boolean merging = false;
    MediaTime contentTime = new MediaRelTimeImpl(0, track.getDuration());
    MediaLocator contentLocator = new MediaLocatorImpl(track.getURI());
    Mpeg7Catalog mpeg7 = mpeg7CatalogService.newInstance();
    Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);

    int segmentcount = 1;

    MediaTimePoint currentSegStart = new MediaTimePointImpl();

    for (Segment o : segments) {

      // if the current segment is shorter than merge treshold start merging
      if (o.getMediaTime().getMediaDuration().getDurationInMilliseconds() <= mergeThresh) {
        // start merging and save beginning of new segment that will be generated
        if (!merging) {
          currentSegStart = o.getMediaTime().getMediaTimePoint();
          merging = true;
        }

      // current segment is longer than merge threshold
      } else {
        long currentSegDuration = o.getMediaTime().getMediaDuration().getDurationInMilliseconds();
        long currentSegEnd = o.getMediaTime().getMediaTimePoint().getTimeInMilliseconds()
                             + currentSegDuration;

        if (merging) {
          long newDuration = o.getMediaTime().getMediaTimePoint().getTimeInMilliseconds()
                             - currentSegStart.getTimeInMilliseconds();

          // if new segment would be long enough
          // save new segment that merges all previously skipped short segments
          if (newDuration >= mergeThresh) {
            Segment s = videoContent.getTemporalDecomposition()
                .createSegment("segment-" + segmentcount++);
            s.setMediaTime(new MediaRelTimeImpl(currentSegStart.getTimeInMilliseconds(), newDuration));
            segmentsNew.add(s);

            // copy the following long segment to new list
            Segment s2 = videoContent.getTemporalDecomposition()
                .createSegment("segment-" + segmentcount++);
            s2.setMediaTime(o.getMediaTime());
            segmentsNew.add(s2);

          // if too short split new segment in middle and merge halves to
          // previous and following segments
          } else {
            long followingStartOld = o.getMediaTime().getMediaTimePoint().getTimeInMilliseconds();
            long newSplit = (currentSegStart.getTimeInMilliseconds() + followingStartOld) / 2;
            long followingEnd = followingStartOld + o.getMediaTime().getMediaDuration().getDurationInMilliseconds();
            long followingDuration = followingEnd - newSplit;

            // if at beginning, don't split, just merge to first large segment
            if (segmentsNew.isEmpty()) {
              Segment s = videoContent.getTemporalDecomposition()
                  .createSegment("segment-" + segmentcount++);
              s.setMediaTime(new MediaRelTimeImpl(0, followingEnd));
              segmentsNew.add(s);
            } else {

              long previousStart = segmentsNew.getLast().getMediaTime().getMediaTimePoint().getTimeInMilliseconds();

              // adjust end time of previous segment to split time
              segmentsNew.getLast().setMediaTime(new MediaRelTimeImpl(previousStart, newSplit - previousStart));

              // create new segment starting at split time
              Segment s = videoContent.getTemporalDecomposition()
                  .createSegment("segment-" + segmentcount++);
              s.setMediaTime(new MediaRelTimeImpl(newSplit, followingDuration));
              segmentsNew.add(s);
            }
          }
          merging = false;

        // copy segments that are long enough to new list (with corrected number)
        } else {
          Segment s = videoContent.getTemporalDecomposition()
              .createSegment("segment-" + segmentcount++);
          s.setMediaTime(o.getMediaTime());
          segmentsNew.add(s);
        }
      }
    }

    // if there is an unfinished merging process after going through all segments
    if (merging && !segmentsNew.isEmpty()) {

      long newDuration = track.getDuration() - currentSegStart.getTimeInMilliseconds();
      // if merged segment is long enough, create new segment
      if (newDuration >= mergeThresh) {

        Segment s = videoContent.getTemporalDecomposition()
            .createSegment("segment-" + segmentcount);
        s.setMediaTime(new MediaRelTimeImpl(currentSegStart.getTimeInMilliseconds(), newDuration));
        segmentsNew.add(s);

      // if not long enough, merge with previous segment
      } else {
        newDuration = track.getDuration() - segmentsNew.getLast().getMediaTime().getMediaTimePoint()
            .getTimeInMilliseconds();
        segmentsNew.getLast().setMediaTime(new MediaRelTimeImpl(segmentsNew.getLast().getMediaTime()
            .getMediaTimePoint().getTimeInMilliseconds(), newDuration));

      }
    }

    // if there is no segment in the list (to merge with), create new
    // segment spanning the whole video
    if (segmentsNew.isEmpty()) {
      Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segment-" + segmentcount);
      s.setMediaTime(new MediaRelTimeImpl(0, track.getDuration()));
      segmentsNew.add(s);
    }

    return mpeg7;
  }

  /**
   * Creates a uniform segmentation for a given track, with prefNumber as the number of segments
   * which will all have the same length
   *
   * @param track the track that is segmented
   * @param segmentsNew will be set to list of new segments (pass null if not required)
   * @param prefNumber number of generated segments
   * @return Mpeg7Catalog that can later be saved in a Catalog as endresult
   */
  protected Mpeg7Catalog uniformSegmentation(Track track, LinkedList<Segment> segmentsNew, int prefNumber) {
    if (segmentsNew == null) {
      segmentsNew = new LinkedList<Segment>();
    }
    MediaTime contentTime = new MediaRelTimeImpl(0, track.getDuration());
    MediaLocator contentLocator = new MediaLocatorImpl(track.getURI());
    Mpeg7Catalog mpeg7 = mpeg7CatalogService.newInstance();
    Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);

    long segmentDuration = track.getDuration() / prefNumber;
    long currentSegStart = 0;

    // create "prefNumber"-many segments that all have the same length
    for (int i = 1; i < prefNumber; i++) {
      Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segment-" + i);
      s.setMediaTime(new MediaRelTimeImpl(currentSegStart, segmentDuration));
      segmentsNew.add(s);

      currentSegStart += segmentDuration;
    }

    // add last segment separately to make sure the last segment ends exactly at the end of the track
    Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segment-" + prefNumber);
      s.setMediaTime(new MediaRelTimeImpl(currentSegStart, track.getDuration() - currentSegStart));
      segmentsNew.add(s);

    return mpeg7;
  }

  /**
   * Sets the workspace
   *
   * @param workspace
   *            an instance of the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the mpeg7CatalogService
   *
   * @param mpeg7CatalogService
   *            an instance of the mpeg7 catalog service
   */
  protected void setMpeg7CatalogService(
      Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
      }

  /**
   * Sets the receipt service
   *
   * @param serviceRegistry
   *            the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *            the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *            the userDirectoryService to set
   */
  public void setUserDirectoryService(
      UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *            the organization directory
   */
  public void setOrganizationDirectoryService(
      OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
