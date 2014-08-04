/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(PROP_ANALYSIS_TRACK_FLAVOR,
            "The flavor of the track to analyze. If multiple tracks match this flavor, the first will be used.");
    CONFIG_OPTIONS.put(PROP_TARGET_TAGS, "The tags to apply to the resulting mpeg-7 segments catalog");
  }

  /** The composer service */
  private VideoSegmenterService videosegmenter = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running video segmentation on workflow {}", workflowInstance.getId());

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Find movie track to analyze
    String trackFlavor = StringUtils.trimToNull(operation.getConfiguration(PROP_ANALYSIS_TRACK_FLAVOR));
    List<String> targetTags = asList(operation.getConfiguration(PROP_TARGET_TAGS));
    List<Track> candidates = new ArrayList<Track>();
    if (trackFlavor != null)
      candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(trackFlavor))));
    else
      candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElements.PRESENTATION_SOURCE)));

    // Remove unsupported tracks (only those containing video can be segmented)
    Iterator<Track> ti = candidates.iterator();
    while (ti.hasNext()) {
      Track t = ti.next();
      if (!t.hasVideo())
        ti.remove();
    }

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
