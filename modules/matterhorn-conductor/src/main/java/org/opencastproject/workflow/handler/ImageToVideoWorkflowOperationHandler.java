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

import org.apache.commons.io.FilenameUtils;
import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport.Filters;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Booleans;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedMap;

import static java.lang.String.format;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Collections.smap;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple.tuple;

/**
 * The workflow definition creating a video from a still image.
 */
public class ImageToVideoWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final String OPT_SOURCE_TAGS = "source-tags";
  private static final String OPT_SOURCE_FLAVOR = "source-flavor";
  private static final String OPT_TARGET_TAGS = "target-tags";
  private static final String OPT_TARGET_FLAVOR = "target-flavor";
  private static final String OPT_DURATION = "duration";
  private static final String OPT_PROFILE = "profile";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageToVideoWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS = smap(
          tuple(OPT_SOURCE_TAGS, "The image attachment must be tagged with one of the given tags"),
          tuple(OPT_SOURCE_FLAVOR, "The image attachment must be of the given flavor"),
          tuple(OPT_DURATION, "The duration of the resulting video in seconds"));

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *         the local composer service
   */
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *         an instance of the workspace
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

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running image to video workflow operation on workflow {}", workflowInstance.getId());
    try {
      return imageToVideo(workflowInstance.getMediaPackage(), workflowInstance);
    } catch (WorkflowOperationException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult imageToVideo(MediaPackage mp, WorkflowInstance wi) throws Exception {
    // read cfg
    final List<String> sourceTags = getCfg(wi, OPT_SOURCE_TAGS).map(asList).getOrElse(nil(String.class));
    final Option<MediaPackageElementFlavor> sourceFlavor = getCfg(wi, OPT_SOURCE_FLAVOR).map(MediaPackageElementFlavor.parseFlavor);
    if (sourceFlavor.isNone() && sourceTags.isEmpty()) {
      logger.warn("No source tags or flavor are given to determine the image to use");
      return createResult(mp, Action.SKIP);
    }
    final List<String> targetTags = getCfg(wi, OPT_TARGET_TAGS).map(asList).getOrElse(nil(String.class));
    final Option<MediaPackageElementFlavor> targetFlavor = getCfg(wi, OPT_TARGET_FLAVOR).map(MediaPackageElementFlavor.parseFlavor);
    final long duration = getCfg(wi, OPT_DURATION).bind(Strings.toLong).getOrElse(this.<Long>cfgKeyMissing(OPT_DURATION));
    final String profile = getCfg(wi, OPT_PROFILE).getOrElse(this.<String>cfgKeyMissing(OPT_PROFILE));
    // run image to video jobs
    final List<Job> jobs =
            Monadics.<MediaPackageElement>mlist(mp.getAttachments())
                    .filter(sourceFlavor.map(Filters.matchesFlavor).getOrElse(Booleans.<MediaPackageElement>yes()))
                    .filter(Filters.hasTagAny(sourceTags))
                    .map(Misc.<MediaPackageElement, Attachment>cast())
                    .map(imageToVideo(profile, duration)).value();
    if (JobUtil.waitForJobs(serviceRegistry, jobs).isSuccess()) {
      for (final Job job : jobs) {
        if (job.getPayload().length() > 0) {
          Track track = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
          track.setURI(workspace.moveTo(track.getURI(),
                                        mp.getIdentifier().toString(),
                                        track.getIdentifier(),
                                        FilenameUtils.getName(track.getURI().toString())));
          // Adjust the target tags
          for (String tag : targetTags) {
            track.addTag(tag);
          }
          // Adjust the target flavor.
          for (MediaPackageElementFlavor flavor : targetFlavor) {
            track.setFlavor(flavor);
          }
          // store new tracks to mediaPackage
          mp.add(track);
          logger.debug("Image to video operation completed");
        } else {
          logger.info("Image to video operation unsuccessful, no payload returned: {}", job);
          return createResult(mp, Action.SKIP);
        }
      }
      return createResult(mp, Action.CONTINUE, mlist(jobs).foldl(0L, new Function2<Long, Job, Long>() {
        @Override public Long apply(Long max, Job job) {
          return Math.max(max, job.getQueueTime());
        }
      }));
    } else {
      throw new WorkflowOperationException("The image to video encoding jobs did not return successfully");
    }
  }

  /** Returned function may throw exceptions. */
  private Function<Attachment, Job> imageToVideo(final String profile, final long duration) {
    return new Function.X<Attachment, Job>() {
      @Override protected Job xapply(Attachment attachment) throws MediaPackageException, EncoderException {
        logger.info(format("Converting image %s to a video of %d sec", attachment.getURI(), duration));
        return composerService.imageToVideo(attachment, profile, duration);
      }
    };
  }
}
