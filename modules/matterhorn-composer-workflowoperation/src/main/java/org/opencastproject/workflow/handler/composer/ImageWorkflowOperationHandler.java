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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * The workflow definition for handling "image" operations
 */
public class ImageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("source-tags",
            "The required tags that must exist on the track for the track to be used as a video source");
    CONFIG_OPTIONS.put("encoding-profile", "The encoding profile to use");
    CONFIG_OPTIONS.put("time", "The number of seconds into the video file to extract the image");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the extracted image");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the extracted image");
  }

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
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
    logger.debug("Running image workflow operation on {}", workflowInstance);

    MediaPackage src = (MediaPackage) workflowInstance.getMediaPackage().clone();

    // Create the image
    try {
      return image(src, workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   *
   * @param mediaPackage
   * @param properties
   * @return the operation result
   * @throws EncoderException
   * @throws ExecutionException
   * @throws IOException
   * @throws NotFoundException
   * @throws WorkflowOperationException
   */
  private WorkflowOperationResult image(final MediaPackage mediaPackage, WorkflowOperationInstance operation)
          throws EncoderException, ExecutionException, NotFoundException, MediaPackageException, IOException,
          WorkflowOperationException {

    // Read the configuration properties
    String sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration("source-flavors"));
    String sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String targetImageTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetImageFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));
    String timeConfiguration = StringUtils.trimToNull(operation.getConfiguration("time"));

    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorOption)
            && StringUtils.isBlank(sourceFlavorsOption)) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Select the source flavors
    for (String flavor : asList(sourceFlavorsOption)) {
      try {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Source flavor '" + flavor + "' is malformed");
      }
    }

    // Support legacy "source-flavor" option
    if (StringUtils.isNotBlank(sourceFlavorOption)) {
      String flavor = StringUtils.trim(sourceFlavorOption);
      try {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Source flavor '" + flavor + "' is malformed");
      }
    }

    // Select the source tags
    for (String tag : asList(sourceTagsOption)) {
      elementSelector.addTag(tag);
    }

    // Find the encoding profile
    EncodingProfile profile = composerService.getProfile(encodingProfileName);
    if (profile == null)
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");

    // Select the tracks based on source flavors and tags and skip those that don't have video
    Set<Track> videoTracks = new HashSet<Track>();
    for (Track track : elementSelector.select(mediaPackage, true)) {
      if (sourceFlavorsOption == null
              || (track.getFlavor() != null && sourceFlavorsOption.equals(track.getFlavor().toString()))) {
        if (track.hasVideo()) {
          videoTracks.add(track);
        }
      }
    }

    if (videoTracks.size() == 0) {
      logger.debug("Mediapackage {} has no suitable tracks to extract images based on tags {} and flavor {}",
              new Object[] { mediaPackage, sourceTagsOption, sourceFlavorsOption });
      return createResult(mediaPackage, Action.CONTINUE);
    }

    long totalTimeInQueue = 0;

    // Start the segmentation jobs
    Map<Track, Job> jobs = new HashMap<Track, Job>();
    for (Track t : videoTracks) {
      if (t.getDuration() == null)
        throw new IllegalStateException("Track " + t + " does not have a duration");

      // take the minimum of the specified time and the video track duration
      long time = Math.min(Long.parseLong(timeConfiguration), t.getDuration() / 1000L);

      // Start encoding and wait for the result
      Job job = composerService.image(t, profile.getIdentifier(), time);
      jobs.put(t, job);
    }

    // Wait for the jobs to be finished
    waitForStatus(jobs.values().toArray(new Job[jobs.size()]));

    // Go through the job results and process them
    for (Map.Entry<Track, Job> entry : jobs.entrySet()) {
      Track t = entry.getKey();
      Job job = entry.getValue();

      if (!Job.Status.FINISHED.equals(job.getStatus()))
        throw new WorkflowOperationException("Image extraction from " + t + " at " + timeConfiguration + " failed");

      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();

      // assume only one image was extracted
      Attachment composedImage = (Attachment) MediaPackageElementParser.getArrayFromXml(job.getPayload()).get(0);
      if (composedImage == null)
        throw new IllegalStateException("Composer service did not return an image");

      // Adjust the target flavor. Make sure to account for partial updates
      if (targetImageFlavor != null) {
        MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetImageFlavor);
        String flavorType = targetFlavor.getType();
        String flavorSubtype = targetFlavor.getSubtype();
        if ("*".equals(flavorType))
          flavorType = t.getFlavor().getType();
        if ("*".equals(flavorSubtype))
          flavorSubtype = t.getFlavor().getSubtype();
        composedImage.setFlavor(new MediaPackageElementFlavor(flavorType, flavorSubtype));
        logger.debug("Resulting image has flavor '{}'", composedImage.getFlavor());
      }

      // Set the mimetype
      if (profile.getMimeType() != null)
        composedImage.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

      // Add tags
      if (targetImageTags != null) {
        for (String tag : asList(targetImageTags)) {
          logger.trace("Tagging image with '{}'", tag);
          if (StringUtils.trimToNull(tag) != null)
            composedImage.addTag(tag);
        }
      }

      // store new image in the mediaPackage
      mediaPackage.addDerived(composedImage, t);

      // Now determine the element's new filename
      String fileName = getFileNameFromElements(t, composedImage);
      composedImage.setURI(workspace.moveTo(composedImage.getURI(), mediaPackage.getIdentifier().toString(),
              composedImage.getIdentifier(), fileName));
    }

    return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
  }

}
