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

package org.opencastproject.workflow.handler.composer;

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
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The workflow definition creating a video from a still image.
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Image to Video Workflow Operation Handler",
        "workflow.operation=image-to-video"
    }
)
public class ImageToVideoWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final String OPT_DURATION = "duration";
  private static final String OPT_PROFILE = "profile";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageToVideoWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the local composer service
   */
  @Reference
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
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
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(wi,
        Configuration.many, Configuration.many, Configuration.many, Configuration.many);
    final List<String> sourceTags = tagsAndFlavors.getSrcTags();
    List<MediaPackageElementFlavor> sourceFlavors = tagsAndFlavors.getSrcFlavors();

    if (sourceFlavors.isEmpty() && sourceTags.isEmpty()) {
      logger.warn("No source tags or flavor are given to determine the image to use");
      return createResult(mp, Action.SKIP);
    }
    final Optional<MediaPackageElementFlavor> sourceFlavor = Optional.ofNullable(sourceFlavors.get(0));

    final ConfiguredTagsAndFlavors.TargetTags targetTags = tagsAndFlavors.getTargetTags();
    List<MediaPackageElementFlavor> targetFlavors = tagsAndFlavors.getTargetFlavors();
    final Optional<MediaPackageElementFlavor> targetFlavor = Optional.ofNullable(targetFlavors.get(0));
    final double duration = getCfg(wi, OPT_DURATION)
        .flatMap(Strings::toDouble)
        .orElseGet(() -> this.<Double>cfgKeyMissing(OPT_DURATION).apply());
    final String profile = getCfg(wi, OPT_PROFILE).orElseGet(() -> this.<String>cfgKeyMissing(OPT_PROFILE).apply());
    // run image to video jobs
    final Function<MediaPackageElement, Boolean> flavorFilter;
    if (sourceFlavor.isPresent()) {
      flavorFilter = Filters.matchesFlavor(sourceFlavor.get());
    } else {
      flavorFilter = alwaysTrue;
    }
    final List<Job> jobs = Arrays.stream(mp.getAttachments())
        .filter(mpe -> flavorFilter.apply(mpe))
        .filter(mpe -> Filters.hasTagAny(sourceTags).apply(mpe))
        .map(mpe -> (Attachment) mpe)
        .map(imageToVideo(profile, duration)::apply)
        .collect(Collectors.toList());
    if (JobUtil.waitForJobs(serviceRegistry, jobs).isSuccess()) {
      for (final Job job : jobs) {
        if (job.getPayload().length() > 0) {
          Track track = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
          track.setURI(workspace.moveTo(track.getURI(), mp.getIdentifier().toString(), track.getIdentifier(),
                  FilenameUtils.getName(track.getURI().toString())));
          // Adjust the target tags
          applyTargetTagsToElement(targetTags, track);
          // Adjust the target flavor.
          if (targetFlavor.isPresent()) {
            track.setFlavor(targetFlavor.get());
          }
          // store new tracks to mediaPackage
          mp.add(track);
          logger.debug("Image to video operation completed");
        } else {
          logger.info("Image to video operation unsuccessful, no payload returned: {}", job);
          return createResult(mp, Action.SKIP);
        }
      }
      return createResult(mp, Action.CONTINUE,
          jobs.stream()
              .mapToLong(Job::getQueueTime)
              .max()
              .orElse(0L)
      );
    } else {
      throw new WorkflowOperationException("The image to video encoding jobs did not return successfully");
    }
  }

  /** Returned function may throw exceptions. */
  private Function<Attachment, Job> imageToVideo(final String profile, final double duration) {
    return new Function.X<Attachment, Job>() {
      @Override
      protected Job xapply(Attachment attachment) throws MediaPackageException, EncoderException {
        logger.info("Converting image {} to a video of {} sec", attachment.getURI().toString(), duration);
        return composerService.imageToVideo(attachment, profile, duration);
      }
    };
  }

  // Fallback function if sourceFlavor is empty
  private final Function<MediaPackageElement, Boolean> alwaysTrue = new Function<MediaPackageElement, Boolean>() {
    @Override
    public Boolean apply(MediaPackageElement mpe) {
      return true;
    }
  };

  @Reference
  @Override  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
