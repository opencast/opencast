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
package org.opencastproject.workflow.handler.animate;

import org.opencastproject.animate.api.AnimateService;
import org.opencastproject.animate.api.AnimateServiceException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow operation for the animate service.
 */
public class AnimateWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(AnimateWorkflowOperationHandler.class);

  /** Animation source file configuration property name. */
  private static final String ANIMATION_FILE_PROPERTY = "animation-file";

  /** Command line arguments configuration property name. */
  private static final String COMMANDLINE_ARGUMENTS_PROPERTY = "cmd-args";

  /** Animation width file configuration property name. */
  private static final String WIDTH_PROPERTY = "width";

  /** Animation height configuration property name. */
  private static final String HEIGHT_PROPERTY = "height";

  /** Animation fps configuration property name. */
  private static final String FPS_PROPERTY = "fps";

  /** Target flavor configuration property name. */
  private static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Target tags configuration property name. */
  private static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** The animate service. */
  private AnimateService animateService = null;

  /** The workspace service. */
  private Workspace workspace = null;

  /** The inspection service */
  private MediaInspectionService mediaInspectionService;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering animate workflow operation handler");
  }

  private void addArgumentIfExists(final WorkflowOperationInstance operation, final List<String> arguments,
          final String property, final String option) {
    String value = StringUtils.trimToNull(operation.getConfiguration(property));
    if (value != null) {
      arguments.add(option);
      arguments.add(value);
    }
  }

  private Map<String, String> getMetadata(MediaPackage mediaPackage) {
    Map<String, String> metadata = new HashMap<>();
    // get episode metadata
    MediaPackageElementFlavor[] flavors = {MediaPackageElements.EPISODE, MediaPackageElements.SERIES};
    for (MediaPackageElementFlavor flavor: flavors) {

      // Get metadata catalogs
      for (Catalog catalog : mediaPackage.getCatalogs(flavor)) {
        DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);
        for (Map.Entry<EName, List<DublinCoreValue>> entry : dc.getValues().entrySet()) {
          String key = String.format("%s.%s", flavor.getSubtype(), entry.getKey().getLocalName());
          String value = entry.getValue().get(0).getValue();
          metadata.put(key, value);
          logger.debug("metadata: {} -> {}", key, value);
        }
      }
    }
    return metadata;
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   * org.opencastproject.job.api.JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    logger.info("Start animate workflow operation for media package {}", mediaPackage);

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    List<String> arguments;

    // Check required options
    final File animationFile = new File(StringUtils.trimToEmpty(operation.getConfiguration(ANIMATION_FILE_PROPERTY)));
    if (!animationFile.isFile()) {
      throw new WorkflowOperationException(String.format("Animation file `%s` does not exist", animationFile));
    }
    URI animation = animationFile.toURI();

    final MediaPackageElementFlavor targetFlavor;
    try {
      targetFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.trimToNull(
              operation.getConfiguration(TARGET_FLAVOR_PROPERTY)));
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException("Invalid target flavor", e);
    }

    // Get optional options
    String targetTagsProperty = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS_PROPERTY));

    // Check if we have custom command line options
    String cmd = operation.getConfiguration(COMMANDLINE_ARGUMENTS_PROPERTY);
    if (StringUtils.isNotEmpty(cmd)) {
      arguments = Arrays.asList(StringUtils.split(cmd));
    } else {
      // set default encoding
      arguments = new ArrayList<>();
      arguments.add("-t");
      arguments.add("ffmpeg");
      arguments.add("--video-codec");
      arguments.add("libx264-lossless");
      arguments.add("--video-bitrate");
      arguments.add("10000");
      addArgumentIfExists(operation, arguments, WIDTH_PROPERTY, "-w");
      addArgumentIfExists(operation, arguments, HEIGHT_PROPERTY, "-h");
      addArgumentIfExists(operation, arguments, FPS_PROPERTY, "--fps");
    }

    final Map<String, String> metadata = getMetadata(mediaPackage);

    Job job;
    try {
      job = animateService.animate(animation, metadata, arguments);
    } catch (AnimateServiceException e) {
      throw new WorkflowOperationException(String.format("Rendering animation from '%s' in media package '%s' failed",
              animation, mediaPackage), e);
    }

    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException(String.format("Animate job for media package '%s' failed", mediaPackage));
    }

    // put animated clip into media package
    try {
      URI output = new URI(job.getPayload());
      String id = UUID.randomUUID().toString();
      InputStream in = workspace.read(output);
      URI uri = workspace.put(mediaPackage.getIdentifier().toString(), id, FilenameUtils.getName(output.getPath()), in);
      TrackImpl track = new TrackImpl();
      track.setIdentifier(id);
      track.setFlavor(targetFlavor);
      track.setURI(uri);

      Job inspection = mediaInspectionService.enrich(track, true);
      if (!waitForStatus(inspection).isSuccess()) {
        throw new AnimateServiceException(String.format("Animating %s failed", animation));
      }

      track = (TrackImpl) MediaPackageElementParser.getFromXml(inspection.getPayload());

      // add track to media package
      for (String tag : asList(targetTagsProperty)) {
        track.addTag(tag);
      }
      mediaPackage.add(track);
      workspace.delete(output);
    } catch (Exception e) {
      throw new WorkflowOperationException("Error handling animation service output", e);
    }

    try {
      workspace.cleanup(mediaPackage.getIdentifier());
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }

    logger.info("Animate workflow operation for media package {} completed", mediaPackage);
    return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
  }

  public void setAnimateService(AnimateService animateService) {
    this.animateService = animateService;
  }

  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.mediaInspectionService = mediaInspectionService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
