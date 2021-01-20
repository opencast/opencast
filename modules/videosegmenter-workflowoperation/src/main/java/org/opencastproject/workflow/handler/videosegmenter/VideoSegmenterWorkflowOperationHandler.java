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

package org.opencastproject.workflow.handler.videosegmenter;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The workflow definition will run suitable recordings by the video segmentation.
 */
public class VideoSegmenterWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenterWorkflowOperationHandler.class);

  /** Name of the configuration key that specifies the flavor of the track to analyze */
  private static final String PROP_ANALYSIS_TRACK_FLAVOR = "source-flavor";

  /** Name of the configuration key that specifies the flavor of the track to analyze */
  private static final String PROP_TARGET_TAGS = "target-tags";

  /** Name of the configuration key that specifies the tag of the track to analyze */
  private static final String PROP_ANALYSIS_TRACK_TAG = "source-tags";

  /** Minimum video length in seconds for video segmentation to run */
  private static final int MIN_VIDEO_LENGTH = 30000;

  /** The composer service */
  private VideoSegmenterService videosegmenter = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running video segmentation on workflow {}", workflowInstance.getId());

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Find movie track to analyze
    String trackTag = StringUtils.trimToNull(operation.getConfiguration(PROP_ANALYSIS_TRACK_TAG));
    String trackFlavor = StringUtils.trimToNull(operation.getConfiguration(PROP_ANALYSIS_TRACK_FLAVOR));
    List<String> targetTags = asList(operation.getConfiguration(PROP_TARGET_TAGS));
    List<Track> candidates = new ArrayList<Track>();
    // Allow the combination of flavor and tag to narrow down choice of source

    if (StringUtils.isBlank(trackTag) && StringUtils.isBlank(trackFlavor)) {
      // Default
      candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElements.PRESENTATION_SOURCE)));
    } else {
      AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();
      if (StringUtils.isNotBlank(trackTag)) {
        elementSelector.addTag(trackTag);
      }
      if (StringUtils.isNotBlank(trackFlavor)) {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(trackFlavor));
      }
      candidates.addAll(elementSelector.select(mediaPackage, true));
    }
    // Select the source flavors

    // Remove unsupported tracks (only those containing video can be segmented)
    candidates.removeIf(t -> !t.hasVideo());

    // Found one?
    if (candidates.size() == 0) {
      logger.info("No matching tracks available for video segmentation in workflow {}", workflowInstance);
      return createResult(Action.CONTINUE);
    }

    // More than one left? Let's be pragmatic...
    if (candidates.size() > 1) {
      logger.info("Found more than one track to segment, choosing the first one ({})", candidates.get(0));
    }
    Track track = candidates.get(0);

    // Skip operation if media is shorter than the minimum defined video length (30s) since we won't generate a
    // sensible segmentation on such a short video anyway.
    if (track.getDuration() != null && track.getDuration() < MIN_VIDEO_LENGTH) {
      return createResult(mediaPackage, Action.SKIP);
    }

    // Segment the media package
    Catalog mpeg7Catalog = null;
    Job job = null;
    try {
      job = videosegmenter.segment(track);
      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException("Video segmentation of " + track + " failed");
      }
      mpeg7Catalog = (Catalog) MediaPackageElementParser.getFromXml(job.getPayload());
      mediaPackage.add(mpeg7Catalog);
      mpeg7Catalog.setURI(workspace.moveTo(mpeg7Catalog.getURI(), mediaPackage.getIdentifier().toString(),
              mpeg7Catalog.getIdentifier(), "segments.xml"));
      mpeg7Catalog.setReference(new MediaPackageReferenceImpl(track));
      // Add target tags
      for (String tag : targetTags) {
        mpeg7Catalog.addTag(tag);
      }
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Video segmentation completed");
    return createResult(mediaPackage, Action.CONTINUE, job.getQueueTime());
  }

  /**
   * Callback for declarative services configuration that will introduce us to the videosegmenter service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param videosegmenter
   *          the video segmenter
   */
  protected void setVideoSegmenter(VideoSegmenterService videosegmenter) {
    this.videosegmenter = videosegmenter;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
