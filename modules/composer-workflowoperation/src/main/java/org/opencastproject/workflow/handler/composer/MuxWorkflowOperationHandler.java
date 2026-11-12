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
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The workflow definition for handling "mux" operations
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Mux Workflow Operation Handler",
        "workflow.operation=mux"
    }
)
public class MuxWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MuxWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the local composer service
   */
  @Reference
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
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowOperationHandler#start(WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running mux workflow operation on workflow {}", workflowInstance.getId());

    try {
      return mux(workflowInstance);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Mux tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   *
   * @param workflowInstance
   *          the current workflow instance
   * @return the operation result containing the updated media package
   * @throws EncoderException
   *           if encoding fails
   * @throws WorkflowOperationException
   *           if errors occur during processing
   * @throws IOException
   *           if the workspace operations fail
   * @throws NotFoundException
   *           if the workspace doesn't contain the requested file
   */
  private WorkflowOperationResult mux(WorkflowInstance workflowInstance)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage src = workflowInstance.getMediaPackage();
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
        Configuration.many, Configuration.many, Configuration.many, Configuration.many);
    ConfiguredTagsAndFlavors.TargetTags targetTagsOption = tagsAndFlavors.getTargetTags();
    MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor();

    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    Map<String, List<Track>> inputTracks = new HashMap<>();
    for (MediaPackageElementFlavor srcFlavor : tagsAndFlavors.getSrcFlavors()) {
      elementSelector.addFlavor(srcFlavor);
    }
    for (String srcTag : tagsAndFlavors.getSrcTags()) {
      elementSelector.addTag(srcTag);
    }
    Collection<Track> srcTracks = elementSelector.select(mediaPackage, false);
    inputTracks.put("video", srcTracks.stream().toList());

    // handle mux woh specific input keys
    final String sourceFlavorPrefix = "source-flavor-";
    final String sourceFlavorsPrefix = "source-flavors-";
    final String sourceTagPrefix = "source-tag-";
    final String sourceTagsPrefix = "source-tags-";
    for (String flavorConfKey : operation.getConfigurationKeys().stream().filter(
        confKey -> Strings.CS.startsWith(confKey, sourceFlavorPrefix) || Strings.CS.startsWith(confKey,
            sourceFlavorsPrefix)).collect(Collectors.toSet())) {
      elementSelector = new TrackSelector();
      for (String srcFlavor : StringUtils.split(operation.getConfiguration(flavorConfKey), ",")) {
        elementSelector.addFlavor(srcFlavor);
      }
      String srcType = StringUtils.split(flavorConfKey, "-", 3)[2];
      for (String srcTag : operation.getConfigurationKeys().stream().filter(
          confKey -> Strings.CS.equals(confKey, sourceTagPrefix + srcType)
              || Strings.CS.equals(confKey, sourceTagsPrefix + srcType)).collect(Collectors.toSet())) {
        asList(StringUtils.trimToNull(srcTag)).stream().filter(Objects::nonNull).forEach(elementSelector::addTag);
      }
      srcTracks = elementSelector.select(mediaPackage, false);
      if (!srcTracks.isEmpty()) {
        if (inputTracks.containsKey(srcType)) {
          inputTracks.get(srcType).addAll(srcTracks);
        } else {
          inputTracks.put(srcType, new ArrayList<>(srcTracks));
        }
      }
    }
    // Handle tags-only inputs
    for (String tagConfKey : operation.getConfigurationKeys().stream().filter(
        confKey -> Strings.CS.startsWith(confKey, sourceTagPrefix) || Strings.CS.startsWith(confKey,
            sourceTagsPrefix)).collect(Collectors.toSet())) {
      String srcType = StringUtils.split(tagConfKey, "-", 3)[2];
      if (inputTracks.containsKey(srcType)) {
        continue;
      }
      elementSelector = new TrackSelector();
      asList(StringUtils.trimToNull(operation.getConfiguration(tagConfKey)))
          .stream().filter(Objects::nonNull).forEach(elementSelector::addTag);
      srcTracks = elementSelector.select(mediaPackage, false);
      inputTracks.put(srcType, new ArrayList<>(srcTracks));
    }

    if (inputTracks.isEmpty()) {
      logger.warn("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.SKIP);
    }
    String profileOption = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));
    if (profileOption == null) {
      throw new WorkflowOperationException("No encoding profile was specified");
    }
    String profileId = StringUtils.trim(profileOption);
    EncodingProfile profile = composerService.getProfile(profileId);
    if (profile == null) {
      throw new WorkflowOperationException("Encoding profile '" + profileId + "' was not found");
    }

    Map<String, Track> muxSourceTracksMap = new HashMap<>();
    for (String srcType : inputTracks.keySet()) {
      List<Track> srcTypeTracks = inputTracks.get(srcType);
      for (int i = 0; i < srcTypeTracks.size(); i++) {
        muxSourceTracksMap.put(String.format("%s.%d", srcType, i + 1), srcTypeTracks.get(i));
      }
    }
    Job muxJob = composerService.mux(muxSourceTracksMap, profileId);

    // Wait for the jobs to return
    if (!waitForStatus(muxJob).isSuccess()) {
      throw new WorkflowOperationException("Mux job did not complete successfully");
    }

    Track encodedTrack = (Track) MediaPackageElementParser.getFromXml(muxJob.getPayload());
    encodedTrack.setFlavor(targetFlavor);
    applyTargetTagsToElement(targetTagsOption, encodedTrack);
    // store new track to mediaPackage
    String fileName = getFileNameFromElements(encodedTrack, encodedTrack);
    encodedTrack.setURI(workspace.moveTo(encodedTrack.getURI(), mediaPackage.getIdentifier().toString(),
        encodedTrack.getIdentifier(), fileName));
    if (muxSourceTracksMap.size() == 1) {
      mediaPackage.addDerived(encodedTrack, muxSourceTracksMap.get(0));
    } else {
      mediaPackage.add(encodedTrack);
    }

    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE);
    logger.debug("Mux operation completed");
    return result;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }
}
