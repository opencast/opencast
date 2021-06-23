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

package org.opencastproject.workflow.handler.videoeditor;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException;
import org.opencastproject.silencedetection.api.SilenceDetectionService;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * workflowoperationhandler for silencedetection executes the silencedetection and adds a SMIL document to the
 * mediapackage containing the cutting points
 */
public class SilenceDetectionWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectionWorkflowOperationHandler.class);

  /** Name of the configuration option that provides the source flavors we are looking for. */
  private static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";

  /** Name of the configuration option that provides the source flavor we are looking for. */
  private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";

  /** Name of the configuration option that provides the smil flavor subtype we will produce. */
  private static final String SMIL_FLAVOR_SUBTYPE_PROPERTY = "smil-flavor-subtype";

  /** Name of the configuration option that provides the smil target flavor we will produce. */
  private static final String SMIL_TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Name of the configuration option for track flavors to reference in generated smil. */
  private static final String REFERENCE_TRACKS_FLAVOR_PROPERTY = "reference-tracks-flavor";

  /** Name of the configuration option whether to set workflow properties with sum of
   * segments duration in seconds and relation to the whole track length for each track.*/
  private static final String EXPORT_SEGMENTS_DURATION = "export-segments-duration";

  /** Name of the configuration option that provides the smil file name */
  private static final String TARGET_FILE_NAME = "smil.smil";

  /** The silence detection service. */
  private SilenceDetectionService detetionService;

  /** The smil service for smil parsing. */
  private SmilService smilService;

  /** The workspace. */
  private Workspace workspace;

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.debug("Start silence detection workflow operation for mediapackage {}", mp.getIdentifier().toString());

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
        Configuration.none, Configuration.many, Configuration.none, Configuration.none);
    List<MediaPackageElementFlavor> sourceFlavors = tagsAndFlavors.getSrcFlavors();
    String smilFlavorSubType = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(
            SMIL_FLAVOR_SUBTYPE_PROPERTY));
    String smilTargetFlavorString = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(
            SMIL_TARGET_FLAVOR_PROPERTY));
    String exportSegmentsDurationString = StringUtils.trimToNull(
        workflowInstance.getCurrentOperation().getConfiguration(EXPORT_SEGMENTS_DURATION));
    boolean exportSegmentsDuration = false;

    if (StringUtils.isNotBlank(exportSegmentsDurationString)) {
      try {
        exportSegmentsDuration = BooleanUtils.toBoolean(exportSegmentsDurationString);
      } catch (IllegalArgumentException e) {
        exportSegmentsDuration = false;
        logger.warn("Unable to parse %s option value %s. Deactivating export of workflow properties.",
                EXPORT_SEGMENTS_DURATION, exportSegmentsDurationString);
      }
    }

    MediaPackageElementFlavor smilTargetFlavor = null;
    if (smilTargetFlavorString != null) {
      smilTargetFlavor = MediaPackageElementFlavor.parseFlavor(smilTargetFlavorString);
    }

    if (sourceFlavors.isEmpty()) {
      throw new WorkflowOperationException(String.format("No %s or %s have been specified", SOURCE_FLAVOR_PROPERTY,
              SOURCE_FLAVORS_PROPERTY));
    }
    if (smilFlavorSubType == null && smilTargetFlavor == null) {
      throw new WorkflowOperationException(String.format("No %s or %s have been specified",
              SMIL_FLAVOR_SUBTYPE_PROPERTY, SMIL_TARGET_FLAVOR_PROPERTY));
    }
    if (sourceFlavors != null && smilTargetFlavor != null) {
      throw new WorkflowOperationException(String.format("Can't use %s and %s together", SOURCE_FLAVORS_PROPERTY,
              SMIL_TARGET_FLAVOR_PROPERTY));
    }

    final String finalSourceFlavors;
    if (smilTargetFlavor != null) {
      finalSourceFlavors = sourceFlavors.get(sourceFlavors.size()).toString();
    } else {
      finalSourceFlavors = sourceFlavors.stream().map(MediaPackageElementFlavor::toString)
          .collect(Collectors.joining(","));
    }

    String referenceTracksFlavor = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(
            REFERENCE_TRACKS_FLAVOR_PROPERTY));
    if (referenceTracksFlavor == null) {
      referenceTracksFlavor = finalSourceFlavors;
    }

    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(finalSourceFlavors)) {
      trackSelector.addFlavor(flavor);
    }
    Collection<Track> sourceTracks = trackSelector.select(mp, false);
    if (sourceTracks.isEmpty()) {
      logger.info("No source tracks found, skip silence detection");
      return createResult(mp, Action.SKIP);
    }

    trackSelector = new TrackSelector();
    for (String flavor : asList(referenceTracksFlavor)) {
      trackSelector.addFlavor(flavor);
    }
    Collection<Track> referenceTracks = trackSelector.select(mp, false);
    if (referenceTracks.isEmpty()) {
      // REFERENCE_TRACKS_FLAVOR_PROPERTY was set to wrong value
      throw new WorkflowOperationException(String.format("No tracks found filtered by flavor(s) '%s'",
              referenceTracksFlavor));
    }
    MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    Map<String, String> exportWorkflowProperties = new HashMap<>();
    for (Track sourceTrack : sourceTracks) {
      // Skip over track with no audio stream
      if (!sourceTrack.hasAudio()) {
        logger.info("Skipping silence detection of track {} since it has no audio", sourceTrack);
        continue;
      }
      logger.info("Executing silence detection on track {}", sourceTrack.getIdentifier());
      try {
        Job detectionJob = detetionService.detect(sourceTrack,
                referenceTracks.toArray(new Track[referenceTracks.size()]));
        if (!waitForStatus(detectionJob).isSuccess()) {
          throw new WorkflowOperationException("Silence Detection failed");
        }
        Smil smil = smilService.fromXml(detectionJob.getPayload()).getSmil();
        InputStream is = null;
        try {
          is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
          URI smilURI = workspace.put(mp.getIdentifier().toString(), smil.getId(), TARGET_FILE_NAME, is);
          MediaPackageElementFlavor smilFlavor = smilTargetFlavor;
          if (smilFlavor == null) {
            smilFlavor = new MediaPackageElementFlavor(sourceTrack.getFlavor().getType(), smilFlavorSubType);
          }
          Catalog catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog, smilFlavor);
          catalog.setIdentifier(smil.getId());
          mp.add(catalog);
        } catch (Exception ex) {
          throw new WorkflowOperationException(String.format(
                  "Failed to put smil into workspace. Silence detection for track %s failed",
                  sourceTrack.getIdentifier()), ex);
        } finally {
          IOUtils.closeQuietly(is);
        }
        if (exportSegmentsDuration) {
          long durationMS = 0;
          for (SmilMediaObject smilElement : smil.getBody().getMediaElements()) {
            durationMS += getSegmentDurationMS(smilElement);
          }
          String durationWfPropertyName = String.format("%s_%s_active_audio_duration",
                  sourceTrack.getFlavor().getType(),
                  sourceTrack.getFlavor().getSubtype());
          exportWorkflowProperties.put(durationWfPropertyName,
                  Long.toString(TimeUnit.MILLISECONDS.toSeconds(durationMS)));
          String relationWfPropertyName = String.format("%s_%s_active_audio_duration_percent",
                  sourceTrack.getFlavor().getType(),
                  sourceTrack.getFlavor().getSubtype());
          double durationTrackLengthRelation = 0;
          if (sourceTrack.getDuration() > 0) {
            durationTrackLengthRelation = (double)durationMS / (double)sourceTrack.getDuration();
            durationTrackLengthRelation *= 100;
          }
          durationTrackLengthRelation = Math.floor(durationTrackLengthRelation);
          durationTrackLengthRelation = Math.min(100, durationTrackLengthRelation);
          durationTrackLengthRelation = Math.max(0, durationTrackLengthRelation);
          exportWorkflowProperties.put(relationWfPropertyName, String.format("%.0f", durationTrackLengthRelation));
        }
        logger.info("Finished silence detection on track {}", sourceTrack.getIdentifier());
      } catch (SilenceDetectionFailedException ex) {
        throw new WorkflowOperationException(String.format("Failed to create silence detection job for track %s",
                sourceTrack.getIdentifier()));
      } catch (SmilException ex) {
        throw new WorkflowOperationException(String.format(
                "Failed to get smil from silence detection job for track %s", sourceTrack.getIdentifier()));
      }
    }
    logger.debug("Finished silence detection workflow operation for mediapackage {}", mp.getIdentifier().toString());
    return createResult(mp, exportWorkflowProperties, Action.CONTINUE, 0);
  }

  /**
   * Return first media segment length in milliseconds. If smilElement is a container, look for sub elements and
   * return duration from the first matching element.
   * @param smilElement smil media or container element to query duration
   * @return media duration in milliseconds
   * @throws SmilException on smil parsing error
   */
  protected long getSegmentDurationMS(SmilMediaObject smilElement) throws SmilException {
    if (smilElement.isContainer()) {
      for (SmilMediaObject element : ((SmilMediaContainer) smilElement).getElements()) {
        return getSegmentDurationMS(element);
      }
    }
    SmilMediaElement smilMediaElement = (SmilMediaElement) smilElement;
    return smilMediaElement.getClipEndMS() - smilMediaElement.getClipBeginMS();
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering silence detection workflow operation handler");
  }

  public void setDetectionService(SilenceDetectionService detectionService) {
    this.detetionService = detectionService;
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
