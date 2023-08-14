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

package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.subtitleparser.SubtitleParsingException;
import org.opencastproject.subtitleparser.webvttparser.WebVTTParser;
import org.opencastproject.subtitleparser.webvttparser.WebVTTSubtitle;
import org.opencastproject.subtitleparser.webvttparser.WebVTTSubtitleCue;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This workflow operation processes a Webvtt into CutMarks
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Processes a WebVTT subtitle document into CutMarks for the editor",
        "workflow.operation=webvtt-to-cutmarks"
    }
)
public class WebvttToCutMarksWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(WebvttToCutMarksWorkflowOperationHandler.class);

  // Workflow Configuration Keys
  /** Configuration option, which describes the min time between two subtitles for them to be considered
   *  separate for cutting, otherwise they will be merge into one large section
   */
  private static final String CFGK_MIN_TIME_SILENCE_IN_MS = "min-time-silence-in-ms";
  private static final String CFGK_MIN_TIME_SILENCE_IN_MS_DEFAULT = "0";

  /** Configuration option, every subtitle cut/section is extended by this amount */
  private static final String CFGK_BUFFER_AROUND_SUBTITLE_IN_MS = "buffer-time-around-subtitle";
  private static final String CFGK_BUFFER_AROUND_SUBTITLE_IN_MS_DEFAULT = "0";

  /** Configuration option: video track of the webvtt file, for end of video detection */
  private static final String CFGK_TRACK_FLAVOR = "track-flavor";

  /** Configuration option which describes how the start of the recording should be treated for creating cuts */
  private static final String CFGK_MIN_TIME_SILENCE_TREATMENT_START = "start-treatment";
  private static final String CFGK_MIN_TIME_SILENCE_TREATMENT_START_DEFAULT = "IGNORE";
  /** Configuration option which describes how the end of the recording should be treated for creating cuts */
  private static final String CFGK_MIN_TIME_SILENCE_TREATMENT_END = "end-treatment";
  private static final String CFGK_MIN_TIME_SILENCE_TREATMENT_END_DEFAULT = "IGNORE";

  /** The filename of the output cut marks */
  private static final String TARGET_FILENAME = "cut-marks.json";

  private static final Gson gson = new Gson();

  private static class WFConfiguration {
    protected long minTimeSilenceInMS;
    protected long bufferTime;
    protected MediaPackageElementFlavor sourceFlavor;
    protected MediaPackageElementFlavor targetFlavor;
    protected Opt<String> trackFlavor;
    protected Treatment treatmentStart;
    protected Treatment treatmentEnd;
  }

  private static class Times {
    private Long begin;
    private Long duration;
  }

  /** Possible treatment options for end and start timestamp */
  private enum Treatment {
    IGNORE,
    USE_FOR_MIN_TIME,
    ALWAYS_INCLUDE
  }

  /** The workspace. */
  private Workspace workspace;

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.debug("Start WebVTT to CutMarks operation for mediapackage {}", mp.getIdentifier().toString());

    // Get configuration
    WFConfiguration config = readConfiguration(workflowInstance);

    // Identify read and parse webvtt
    WebVTTSubtitle webvtt = readAndParseWebVTT(mp, config.sourceFlavor);

    // Get track length
    Opt<Long> trackDuration = getTrackDuration(mp, config.trackFlavor);

    // Process WebVTT Subtitle Information into CutPoints
    List<Times> cutMarks = processWebVTTIntoCutPoints(
            webvtt,
            config.minTimeSilenceInMS,
            config.bufferTime,
            trackDuration,
            config.treatmentStart,
            config.treatmentEnd
    );

    saveCutMarks(mp, cutMarks, config.targetFlavor);

    return createResult(mp, Action.CONTINUE);
  }

  private WFConfiguration readConfiguration(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
            Configuration.none, Configuration.one, Configuration.none, Configuration.one);
    MediaPackageElementFlavor sourceFlavor = tagsAndFlavors.getSingleSrcFlavor();
    MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor();

    long minTimeSilenceInMS;
    long bufferTime;
    try {
      minTimeSilenceInMS = Long.parseLong(
              getConfig(workflowInstance, CFGK_MIN_TIME_SILENCE_IN_MS, CFGK_MIN_TIME_SILENCE_IN_MS_DEFAULT)
      );
      bufferTime = Long.parseLong(
              getConfig(workflowInstance, CFGK_BUFFER_AROUND_SUBTITLE_IN_MS, CFGK_BUFFER_AROUND_SUBTITLE_IN_MS_DEFAULT)
      );

      if (minTimeSilenceInMS < 0 || bufferTime < 0) {
        throw new NumberFormatException("Negative Integer, must be positive");
      }
    } catch (NumberFormatException error) {
      throw new WorkflowOperationException(
              CFGK_MIN_TIME_SILENCE_IN_MS + " and " + CFGK_BUFFER_AROUND_SUBTITLE_IN_MS + "must be a postive integer",
              error
      );
    }
    if (minTimeSilenceInMS < 2 * bufferTime) {
      throw new WorkflowOperationException(
              CFGK_MIN_TIME_SILENCE_IN_MS + " must be at least double the value of "
                      + CFGK_BUFFER_AROUND_SUBTITLE_IN_MS
      );
    }

    Opt<String> trackFlavor = getOptConfig(workflowInstance, CFGK_TRACK_FLAVOR);

    String treatmentStrStart = getConfig(
            workflowInstance,
            CFGK_MIN_TIME_SILENCE_TREATMENT_START,
            CFGK_MIN_TIME_SILENCE_TREATMENT_START_DEFAULT
    );
    String treatmentStrEnd = getConfig(
            workflowInstance,
            CFGK_MIN_TIME_SILENCE_TREATMENT_END,
            CFGK_MIN_TIME_SILENCE_TREATMENT_END_DEFAULT
    );
    Treatment treatmentStart;
    Treatment treatmentEnd;
    try {
      treatmentStart = Treatment.valueOf(treatmentStrStart);
      treatmentEnd = Treatment.valueOf(treatmentStrEnd);
    } catch (IllegalArgumentException error) {
      throw new WorkflowOperationException(
              CFGK_MIN_TIME_SILENCE_TREATMENT_START + " and "
                      + CFGK_MIN_TIME_SILENCE_TREATMENT_END
                      + " must be one of the values IGNORE, USE_FOR_MIN_TIME, ALWAYS_INCLUDE",
              error
      );
    }
    if (treatmentEnd != Treatment.IGNORE && trackFlavor.isEmpty()) {
      throw new WorkflowOperationException(
              CFGK_TRACK_FLAVOR + " is not defined, but "
                      + CFGK_MIN_TIME_SILENCE_TREATMENT_END + " is not set to IGNORE, therefore a "
                      + CFGK_TRACK_FLAVOR + " is needed"
      );
    }

    WFConfiguration config = new WFConfiguration();

    config.minTimeSilenceInMS = minTimeSilenceInMS;
    config.bufferTime = bufferTime;
    config.sourceFlavor = sourceFlavor;
    config.targetFlavor = targetFlavor;
    config.trackFlavor = trackFlavor;
    config.treatmentStart = treatmentStart;
    config.treatmentEnd = treatmentEnd;

    return config;
  }

  private List<Times> processWebVTTIntoCutPoints(
          WebVTTSubtitle webvtt,
          long minTimeSilenceInMS,
          long bufferTime,
          Opt<Long> trackDuration,
          Treatment treatmentStart,
          Treatment treatmentEnd
  ) {
    List<Times> cutMarks = new ArrayList<Times>();
    List<WebVTTSubtitleCue> cues = webvtt.getCues();
    if (cues.size() > 0) {
      WebVTTSubtitleCue firstCue = cues.remove(0);
      // in milliseconds
      long oldMarkStart = firstCue.getStartTime();
      long oldMarkEnd = firstCue.getEndTime();

      for (WebVTTSubtitleCue cue : webvtt.getCues()) {
        long newMarkStart = cue.getStartTime();
        long newMarkEnd = cue.getEndTime();

        // Save oldMark if enough silence is between old and new mark, otherwise combine them
        if (newMarkStart - oldMarkEnd > minTimeSilenceInMS) {
          // Save oldMark
          Times oldMark = new Times();
          // Expand Mark by bufferTime
          oldMark.begin = oldMarkStart - bufferTime;
          oldMark.duration = oldMarkEnd - oldMark.begin + bufferTime;
          cutMarks.add(oldMark);

          // newMark is the next oldMark
          oldMarkStart = newMarkStart;
          oldMarkEnd = newMarkEnd;
        } else if (newMarkEnd > oldMarkEnd) {
          // else if: old and new mark are close by, combine them
          oldMarkEnd = newMarkEnd;
        }
      }

      // Save last mark
      Times lastMark = new Times();
      // Expand Mark by bufferTime
      lastMark.begin = oldMarkStart - bufferTime;
      lastMark.duration = oldMarkEnd - lastMark.begin + bufferTime;
      cutMarks.add(lastMark);


      // handle start and end
      // crop start and end
      // (assumes that cropping is only necessary due to the bufferTime, does not include cases like the webvtt having timestamps outside of the videos runtime)
      // (also assumes that the video starts at 0)
      Times firstCutMark = cutMarks.get(0);
      if (treatmentStart == Treatment.ALWAYS_INCLUDE) {
        updateTimesBegin(firstCutMark, 0L);
      } else if (treatmentStart == Treatment.USE_FOR_MIN_TIME) {
        if ((firstCutMark.begin + bufferTime) - 0L <= minTimeSilenceInMS) {
          updateTimesBegin(firstCutMark, 0L);
        }
      } else if (treatmentStart == Treatment.IGNORE) {
        if (firstCutMark.begin < 0) {
          updateTimesBegin(firstCutMark, 0L);
        }
      }
      if (trackDuration.isDefined()) {
        long trackDur = trackDuration.get();
        Times lastCutMark = cutMarks.get(cutMarks.size() - 1);
        if (treatmentEnd == Treatment.ALWAYS_INCLUDE) {
          updateTimesEnd(lastCutMark, trackDur);
        } else if (treatmentEnd == Treatment.USE_FOR_MIN_TIME) {
          if (trackDur - (lastCutMark.begin + lastCutMark.duration - bufferTime) <= minTimeSilenceInMS) {
            updateTimesEnd(lastCutMark, trackDur);
          }
        } else if (treatmentEnd  == Treatment.IGNORE) {
          // cropping
          if (lastCutMark.begin + lastCutMark.duration > trackDur) {
            updateTimesEnd(lastCutMark, trackDur);
          }
        }
      }
    }

    return cutMarks;
  }

  /** No security checks, newBegin may not be after previous end */
  private void updateTimesBegin(Times toUpdate, long newBegin) {
    Long end = toUpdate.begin + toUpdate.duration;
    toUpdate.begin = newBegin;
    toUpdate.duration = end - newBegin;
  }
  /** No security checks, newEnd may not be before previous begin */
  private void updateTimesEnd(Times toUpdate, long newEnd) {
    toUpdate.duration = newEnd - toUpdate.begin;
  }

  private void saveCutMarks(MediaPackage mp, List<Times> cutMarks, MediaPackageElementFlavor targetFlavor)
          throws WorkflowOperationException {
    String jsonCutMarks = gson.toJson(cutMarks);

    try {
      InputStream cutMarksOut = IOUtils.toInputStream(jsonCutMarks, StandardCharsets.UTF_8);

      MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      MediaPackageElement mpe = mpeBuilder.newElement(MediaPackageElement.Type.Attachment, targetFlavor);
      mpe.setIdentifier(UUID.randomUUID().toString());

      URI cutMarksURI = workspace.put(mp.getIdentifier().toString(), mpe.getIdentifier(), TARGET_FILENAME, cutMarksOut);

      mpe.setURI(cutMarksURI);

      mp.add(mpe);
    } catch (IOException e) {
      throw new WorkflowOperationException("Couldn't write resulting cutMarks");
    }
  }

  private WebVTTSubtitle readAndParseWebVTT(MediaPackage mp, MediaPackageElementFlavor sourceFlavor)
          throws WorkflowOperationException {
    // Identify WebVTT Element to process
    MediaPackageElement[] webvttElements = mp.getElementsByFlavor(sourceFlavor);
    if (webvttElements.length != 1) {
      throw new WorkflowOperationException("Couldn't uniqly identify WebVTT Element");
    }
    URI webvttURI = webvttElements[0].getURI();

    // read and parse WebVTT Element
    InputStream webvttIS = null;
    WebVTTSubtitle webvtt;
    try {
      webvttIS = workspace.read(webvttURI);
      WebVTTParser wvparser = new WebVTTParser();

      webvtt = wvparser.parse(webvttIS);
    } catch (NullPointerException | IOException | NotFoundException e) {
      throw new WorkflowOperationException("Couldn't open WebVTT file for parsing", e);
    } catch (SubtitleParsingException e) {
      throw new WorkflowOperationException("Failed to parse WebVTT File", e);
    } finally {
      try {
        if (webvttIS != null) {
          webvttIS.close();
        } else {
          logger.debug("WebVTT InputStream is null (mediapackage {})", mp.getIdentifier().toString());
        }
      } catch (IOException e) {
        logger.warn("Couldn't close '{}' properly (mediapackage {})", webvttURI.toString(), mp.getIdentifier().toString());
      }
    }

    return webvtt;
  }

  private Opt<Long> getTrackDuration(MediaPackage mp, Opt<String> trackFlavor)
          throws WorkflowOperationException {
    if (trackFlavor.isDefined()) {
      String flavor = trackFlavor.get();
      Track[] tracks;
      try {
        tracks = mp.getTracks(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Couldn't parse " + CFGK_TRACK_FLAVOR, e);
      }
      if (tracks.length != 1) {
        throw new WorkflowOperationException(
                "Multiple tracks or no track found with flavor '"
                + flavor + "' in mediapackage '"
                + mp.getIdentifier().toString() + "', exactly one needed"
        );
      }
      return Opt.nul(tracks[0].getDuration());
    }

    return Opt.none();
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering webvtt-to-cutmarks workflow operation handler");
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}

