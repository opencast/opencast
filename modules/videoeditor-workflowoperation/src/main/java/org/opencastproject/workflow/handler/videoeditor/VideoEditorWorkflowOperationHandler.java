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

import static java.lang.String.format;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.videoeditor.api.VideoEditorService;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

public class VideoEditorWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory.getLogger(VideoEditorWorkflowOperationHandler.class);

  /** Path to the hold ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/editor/index.html";

  /** Name of the configuration option that provides the source flavors we use for processing. */
  private static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";

  /** Name of the configuration option that provides the preview flavors we use as preview. */
  private static final String PREVIEW_FLAVORS_PROPERTY = "preview-flavors";

  /** Bypasses Videoeditor's encoding operation but keep the raw smil for later processing */
  private static final String SKIP_PROCESSING_PROPERTY = "skip-processing";

  /** Name of the configuration option that provides the source flavors on skipped videoeditor operation. */
  private static final String SKIPPED_FLAVORS_PROPERTY = "skipped-flavors";

  /** Name of the configuration option that provides the SMIL flavor as input. */
  private static final String SMIL_FLAVORS_PROPERTY = "smil-flavors";

  /** Name of the configuration option that provides the SMIL flavor as input. */
  private static final String TARGET_SMIL_FLAVOR_PROPERTY = "target-smil-flavor";

  /** Name of the configuration that provides the target flavor subtype for encoded media tracks. */
  private static final String TARGET_FLAVOR_SUBTYPE_PROPERTY = "target-flavor-subtype";

  /** Name of the configuration that provides the interactive flag */
  private static final String INTERACTIVE_PROPERTY = "interactive";

  /** Name of the configuration that provides the SMIL file name */
  private static final String SMIL_FILE_NAME = "smil.smil";

  /**
   * Name of the configuration that controls whether or not to process the input video(s) even when there are no
   * trimming points
   */
  private static final String SKIP_NOT_TRIMMED_PROPERTY = "skip-if-not-trimmed";

  /**
   * The SMIL service to modify SMIL files.
   */
  private SmilService smilService;
  /**
   * The VideoEditor service to edit files.
   */
  private VideoEditorService videoEditorService;
  /**
   * The workspace.
   */
  private Workspace workspace;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    setHoldActionTitle("Review / VideoEdit");
    registerHoldStateUserInterface(HOLD_UI_PATH);
    logger.info("Registering videoEditor hold state ui from classpath {}", HOLD_UI_PATH);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.info("Start editor workflow for mediapackage {}", mp.getIdentifier().compact());

    // Get configuration
    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String smilFlavorsProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(SMIL_FLAVORS_PROPERTY));
    if (smilFlavorsProperty == null) {
      throw new WorkflowOperationException(format("Required configuration property %s not set", SMIL_FLAVORS_PROPERTY));
    }
    String targetSmilFlavorProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(TARGET_SMIL_FLAVOR_PROPERTY));
    if (targetSmilFlavorProperty == null) {
      throw new WorkflowOperationException(
              format("Required configuration property %s not set", TARGET_SMIL_FLAVOR_PROPERTY));
    }
    String previewTrackFlavorsProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(PREVIEW_FLAVORS_PROPERTY));
    if (previewTrackFlavorsProperty == null) {
      logger.info("Configuration property '{}' not set, use preview tracks from SMIL catalog",
              PREVIEW_FLAVORS_PROPERTY);
    }

    /* false if it is missing */
    final boolean skipProcessing = BooleanUtils
            .toBoolean(worflowOperationInstance.getConfiguration(SKIP_PROCESSING_PROPERTY));
    /* skip smil processing (done in another operation) so target_flavors do not matter */
    if (!skipProcessing && StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY)) == null) {
      throw new WorkflowOperationException(
              String.format("Required configuration property %s not set", TARGET_FLAVOR_SUBTYPE_PROPERTY));
    }

    final boolean interactive = BooleanUtils.toBoolean(worflowOperationInstance.getConfiguration(INTERACTIVE_PROPERTY));

    // Check at least one SMIL catalog exists
    SimpleElementSelector elementSelector = new SimpleElementSelector();
    for (String flavor : asList(smilFlavorsProperty)) {
      elementSelector.addFlavor(flavor);
    }
    Collection<MediaPackageElement> smilCatalogs = elementSelector.select(mp, false);
    MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();

    if (smilCatalogs.isEmpty()) {

      // There is nothing to do, skip the operation
      // however, we still need the smil file to be produced for the entire video as one clip if
      // skipProcessing is TRUE
      if (!interactive && !skipProcessing) {
        logger.info("Skipping cutting operation since no edit decision list is available");
        return skip(workflowInstance, context);
      }

      // Without SMIL catalogs and without preview tracks, there is nothing we can do
      if (previewTrackFlavorsProperty == null) {
        throw new WorkflowOperationException(
                format("No SMIL catalogs with flavor %s nor preview files with flavor %s found in mediapackage %s",
                        smilFlavorsProperty, previewTrackFlavorsProperty, mp.getIdentifier().compact()));
      }

      // Based on the preview tracks, create new and empty SMIL catalog
      TrackSelector trackSelector = new TrackSelector();
      for (String flavor : asList(previewTrackFlavorsProperty)) {
        trackSelector.addFlavor(flavor);
      }
      Collection<Track> previewTracks = trackSelector.select(mp, false);
      if (previewTracks.isEmpty()) {
        throw new WorkflowOperationException(format("No preview tracks found in mediapackage %s with flavor %s",
                mp.getIdentifier().compact(), previewTrackFlavorsProperty));
      }
      Track[] previewTracksArr = previewTracks.toArray(new Track[previewTracks.size()]);
      MediaPackageElementFlavor smilFlavor = MediaPackageElementFlavor.parseFlavor(smilFlavorsProperty);

      for (Track previewTrack : previewTracks) {
        try {
          SmilResponse smilResponse = smilService.createNewSmil(mp);
          smilResponse = smilService.addParallel(smilResponse.getSmil());
          smilResponse = smilService.addClips(smilResponse.getSmil(), smilResponse.getEntity().getId(),
                  previewTracksArr, 0L, previewTracksArr[0].getDuration());
          Smil smil = smilResponse.getSmil();

          InputStream is = null;
          try {
            // Put new SMIL into workspace
            is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
            URI smilURI = workspace.put(mp.getIdentifier().compact(), smil.getId(), SMIL_FILE_NAME, is);
            MediaPackageElementFlavor trackSmilFlavor = previewTrack.getFlavor();
            if (!"*".equals(smilFlavor.getType())) {
              trackSmilFlavor = new MediaPackageElementFlavor(smilFlavor.getType(), trackSmilFlavor.getSubtype());
            }
            if (!"*".equals(smilFlavor.getSubtype())) {
              trackSmilFlavor = new MediaPackageElementFlavor(trackSmilFlavor.getType(), smilFlavor.getSubtype());
            }
            Catalog catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
                    trackSmilFlavor);
            catalog.setIdentifier(smil.getId());
            mp.add(catalog);
          } finally {
            IOUtils.closeQuietly(is);
          }
        } catch (Exception ex) {
          throw new WorkflowOperationException(
                  format("Failed to create SMIL catalog for mediapackage %s", mp.getIdentifier().compact()), ex);
        }
      }
    }

    // Check target SMIL catalog exists
    MediaPackageElementFlavor targetSmilFlavor = MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty);
    Catalog[] targetSmilCatalogs = mp.getCatalogs(targetSmilFlavor);
    if (targetSmilCatalogs == null || targetSmilCatalogs.length == 0) {

      if (!interactive && !skipProcessing) // create a smil even if not interactive
        return skip(workflowInstance, context);

      // Create new empty SMIL to fill it from editor UI
      try {
        SmilResponse smilResponse = smilService.createNewSmil(mp);
        Smil smil = smilResponse.getSmil();

        InputStream is = null;
        try {
          // Put new SMIL into workspace
          is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
          URI smilURI = workspace.put(mp.getIdentifier().compact(), smil.getId(), SMIL_FILE_NAME, is);
          Catalog catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
                  targetSmilFlavor);
          catalog.setIdentifier(smil.getId());
          mp.add(catalog);
        } finally {
          IOUtils.closeQuietly(is);
        }
      } catch (Exception ex) {
        throw new WorkflowOperationException(
                format("Failed to create an initial empty SMIL catalog for mediapackage %s",
                        mp.getIdentifier().compact()),
                ex);
      }

      if (!interactive) // deferred skip, keep empty smil
        return skip(workflowInstance, context);
      logger.info("Holding for video edit...");
      return createResult(mp, Action.PAUSE);
    } else {
      logger.debug("Move on, SMIL catalog ({}) already exists for media package '{}'", targetSmilFlavor, mp);
      return resume(workflowInstance, context, Collections.<String, String> emptyMap());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#skip(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    // If we do not hold for trim, we still need to put tracks in the mediapackage with the target flavor
    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.info("Skip video editor operation for mediapackage {}", mp.getIdentifier().compact());

    // Get configuration
    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String sourceTrackFlavorsProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(SKIPPED_FLAVORS_PROPERTY));
    if (sourceTrackFlavorsProperty == null || sourceTrackFlavorsProperty.isEmpty()) {
      logger.info("\"{}\" option not set, use value of \"{}\"", SKIPPED_FLAVORS_PROPERTY, SOURCE_FLAVORS_PROPERTY);
      sourceTrackFlavorsProperty = StringUtils
              .trimToNull(worflowOperationInstance.getConfiguration(SOURCE_FLAVORS_PROPERTY));
      if (sourceTrackFlavorsProperty == null) {
        throw new WorkflowOperationException(
                format("Required configuration property %s not set.", SOURCE_FLAVORS_PROPERTY));
      }
    }
    // processing will operate directly on source tracks as named in smil file
    final boolean skipProcessing = BooleanUtils
            .toBoolean(worflowOperationInstance.getConfiguration(SKIP_PROCESSING_PROPERTY));
    if (skipProcessing)
      return createResult(mp, Action.SKIP);
    // If not skipProcessing (set it up for process-smil), then clone and tag to target
    String targetFlavorSubTypeProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY));
    if (targetFlavorSubTypeProperty == null) {
      throw new WorkflowOperationException(
              format("Required configuration property %s not set.", TARGET_FLAVOR_SUBTYPE_PROPERTY));
    }

    // Get source tracks
    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(sourceTrackFlavorsProperty)) {
      trackSelector.addFlavor(flavor);
    }
    Collection<Track> sourceTracks = trackSelector.select(mp, false);

    for (Track sourceTrack : sourceTracks) {
      // Set target track flavor
      Track clonedTrack = (Track) sourceTrack.clone();
      clonedTrack.setIdentifier(null);
      // Use the same URI as the original
      clonedTrack.setURI(sourceTrack.getURI());
      clonedTrack
              .setFlavor(new MediaPackageElementFlavor(sourceTrack.getFlavor().getType(), targetFlavorSubTypeProperty));
      mp.addDerived(clonedTrack, sourceTrack);
    }

    return createResult(mp, Action.SKIP);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
          Map<String, String> properties) throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.info("Resume video editor operation for mediapackage {}", mp.getIdentifier().compact());

    // Get configuration
    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String sourceTrackFlavorsProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(SOURCE_FLAVORS_PROPERTY));
    if (sourceTrackFlavorsProperty == null) {
      throw new WorkflowOperationException(
              format("Required configuration property %s not set.", SOURCE_FLAVORS_PROPERTY));
    }
    String targetSmilFlavorProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(TARGET_SMIL_FLAVOR_PROPERTY));
    if (targetSmilFlavorProperty == null) {
      throw new WorkflowOperationException(
              format("Required configuration property %s not set.", TARGET_SMIL_FLAVOR_PROPERTY));
    }
    String targetFlavorSybTypeProperty = StringUtils
            .trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY));

    // if set, smil processing is done by another operation
    final boolean skipProcessing = BooleanUtils
            .toBoolean(worflowOperationInstance.getConfiguration(SKIP_PROCESSING_PROPERTY));
    if (!skipProcessing) {
      if (targetFlavorSybTypeProperty == null) {
        throw new WorkflowOperationException(
                format("Required configuration property %s not set.", TARGET_FLAVOR_SUBTYPE_PROPERTY));
      }
    }

    boolean skipIfNoTrim = BooleanUtils.toBoolean(worflowOperationInstance.getConfiguration(SKIP_NOT_TRIMMED_PROPERTY));

    // Get source tracks
    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(sourceTrackFlavorsProperty)) {
      trackSelector.addFlavor(flavor);
    }
    Collection<Track> sourceTracks = trackSelector.select(mp, false);
    if (sourceTracks.isEmpty()) {
      throw new WorkflowOperationException(format("No source tracks found in mediapacksge %s with flavors %s.",
              mp.getIdentifier().compact(), sourceTrackFlavorsProperty));
    }

    // Get SMIL file
    MediaPackageElementFlavor smilTargetFlavor = MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty);
    Catalog[] smilCatalogs = mp.getCatalogs(smilTargetFlavor);
    if (smilCatalogs == null || smilCatalogs.length == 0) {
      throw new WorkflowOperationException(format("No SMIL catalog found in mediapackage %s with flavor %s.",
              mp.getIdentifier().compact(), targetSmilFlavorProperty));
    }

    File smilFile = null;
    Smil smil = null;
    try {
      smilFile = workspace.get(smilCatalogs[0].getURI());
      smil = smilService.fromXml(smilFile).getSmil();
      smil = replaceAllTracksWith(smil, sourceTracks.toArray(new Track[sourceTracks.size()]));

      InputStream is = null;
      try {
        is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
        // Remove old SMIL
        workspace.delete(mp.getIdentifier().compact(), smilCatalogs[0].getIdentifier());
        mp.remove(smilCatalogs[0]);
        // put modified SMIL into workspace
        URI newSmilUri = workspace.put(mp.getIdentifier().compact(), smil.getId(), SMIL_FILE_NAME, is);
        Catalog catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(newSmilUri, MediaPackageElement.Type.Catalog, smilCatalogs[0].getFlavor());
        catalog.setIdentifier(smil.getId());
        mp.add(catalog);
      } catch (Exception ex) {
        throw new WorkflowOperationException(ex);
      } finally {
        IOUtils.closeQuietly(is);
      }

    } catch (NotFoundException ex) {
      throw new WorkflowOperationException(format("Failed to get SMIL catalog %s from mediapackage %s.",
              smilCatalogs[0].getIdentifier(), mp.getIdentifier().compact()), ex);
    } catch (IOException ex) {
      throw new WorkflowOperationException(format("Can't open SMIL catalog %s from mediapackage %s.",
              smilCatalogs[0].getIdentifier(), mp.getIdentifier().compact()), ex);
    } catch (SmilException ex) {
      throw new WorkflowOperationException(ex);
    }
    // If skipProcessing, The track is processed by a separate operation which takes the SMIL file and encode directly
    // to delivery format
    if (skipProcessing) {
      logger.info("VideoEdit workflow {} finished - smil file is {}", workflowInstance.getId(), smil.getId());
      return createResult(mp, Action.CONTINUE);
    }
    // create video edit jobs and run them
    if (skipIfNoTrim) {
      // We need to check whether or not there are trimming points defined
      // TODO The SmilService implementation does not do any filtering or optimizations for us. We need to
      // process the SMIL file ourselves. The SmilService should be something more than a bunch of classes encapsulating
      // data types which provide no extra functionality (e.g. we shouldn't have to check the SMIL structure ourselves)

      // We should not modify the SMIL file as we traverse through its elements, so we make a copy and modify it instead
      try {
        Smil filteredSmil = smilService.fromXml(smil.toXML()).getSmil();
        for (SmilMediaObject element : smil.getBody().getMediaElements()) {
          // body should contain par elements
          if (element.isContainer()) {
            SmilMediaContainer container = (SmilMediaContainer) element;
            if (SmilMediaContainer.ContainerType.PAR == container.getContainerType()) {
              continue;
            }
          }
          filteredSmil = smilService.removeSmilElement(filteredSmil, element.getId()).getSmil();
        }

        // Return an empty job list if not PAR components (i.e. trimming points) are defined, or if there is just
        // one that takes the whole video size
        switch (filteredSmil.getBody().getMediaElements().size()) {
          case 0:
            logger.info("Skipping SMIL job generation for mediapackage '{}', "
                    + "because the SMIL does not define any trimming points", mp.getIdentifier());
            return skip(workflowInstance, context);

          case 1:
            // If the whole duration was not defined in the mediapackage, we cannot tell whether or not this PAR
            // component represents the whole duration or not, therefore we don't bother to try
            if (mp.getDuration() < 0)
              break;

            SmilMediaContainer parElement = (SmilMediaContainer) filteredSmil.getBody().getMediaElements().get(0);
            boolean skip = true;
            for (SmilMediaObject elementChild : parElement.getElements()) {
              if (!elementChild.isContainer()) {
                SmilMediaElement media = (SmilMediaElement) elementChild;
                // Compare begin and endpoints
                // If they don't represent the whole length, then we break --we have a trimming point
                if ((media.getClipBeginMS() != 0) || (media.getClipEndMS() != mp.getDuration())) {
                  skip = false;
                  break;
                }
              }
            }

            if (skip) {
              logger.info("Skipping SMIL job generation for mediapackage '{}', "
                      + "because the trimming points in the SMIL correspond "
                      + "to the beginning and the end of the video", mp.getIdentifier());
              return skip(workflowInstance, context);
            }

            break;

          default:
            break;
        }
      } catch (MalformedURLException | SmilException | JAXBException | SAXException e) {
        logger.warn("Error parsing input SMIL to determine if it has trimpoints. "
                + "We will assume it does and go on creating jobs.");
      }
    }

    // Create video edit jobs and run them
    List<Job> jobs = null;

    try {
      logger.info("Create processing jobs for SMIL file: {}", smilCatalogs[0].getIdentifier());
      jobs = videoEditorService.processSmil(smil);
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException(
                format("Processing SMIL file failed: %s", smilCatalogs[0].getIdentifier()));
      }
      logger.info("Finished processing of SMIL file: {}", smilCatalogs[0].getIdentifier());
    } catch (ProcessFailedException ex) {
      throw new WorkflowOperationException(
              format("Finished processing of SMIL file: %s", smilCatalogs[0].getIdentifier()), ex);
    }

    // Move edited tracks to work location and set target flavor
    Track editedTrack = null;
    boolean mpAdded = false;
    for (Job job : jobs) {
      try {
        editedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
        MediaPackageElementFlavor editedTrackFlavor = editedTrack.getFlavor();
        editedTrack.setFlavor(new MediaPackageElementFlavor(editedTrackFlavor.getType(), targetFlavorSybTypeProperty));
        URI editedTrackNewUri = workspace.moveTo(editedTrack.getURI(), mp.getIdentifier().compact(),
                editedTrack.getIdentifier(), FilenameUtils.getName(editedTrack.getURI().toString()));
        editedTrack.setURI(editedTrackNewUri);
        for (Track track : sourceTracks) {
          if (track.getFlavor().getType().equals(editedTrackFlavor.getType())) {
            mp.addDerived(editedTrack, track);
            mpAdded = true;
            break;
          }
        }

        if (!mpAdded) {
          mp.add(editedTrack);
        }

      } catch (MediaPackageException ex) {
        throw new WorkflowOperationException("Failed to get information about the edited track(s)", ex);
      } catch (NotFoundException | IOException | IllegalArgumentException ex) {
        throw new WorkflowOperationException("Moving edited track to work location failed.", ex);
      } catch (Exception ex) {
        throw new WorkflowOperationException(ex);
      }
    }

    logger.info("VideoEdit workflow {} finished", workflowInstance.getId());
    return createResult(mp, Action.CONTINUE);
  }

  protected Smil replaceAllTracksWith(Smil smil, Track[] otherTracks) throws SmilException {
    SmilResponse smilResponse;
    try {
      // copy SMIL to work with
      smilResponse = smilService.fromXml(smil.toXML());
    } catch (Exception ex) {
      throw new SmilException("Can not parse SMIL files.");
    }

    long start;
    long end;
    boolean hasElements = false; // Check for missing smil so the process will fail early if no tracks found
    // iterate over all elements inside SMIL body
    for (SmilMediaObject elem : smil.getBody().getMediaElements()) {
      start = -1L;
      end = -1L;
      // body should contain par elements (container)
      if (elem.isContainer()) {
        // iterate over all elements in container
        for (SmilMediaObject child : ((SmilMediaContainer) elem).getElements()) {
          // second depth should contain media elements like audio or video
          if (!child.isContainer() && child instanceof SmilMediaElement) {
            SmilMediaElement media = (SmilMediaElement) child;
            start = media.getClipBeginMS();
            end = media.getClipEndMS();
            // remove it
            smilResponse = smilService.removeSmilElement(smilResponse.getSmil(), media.getId());
            hasElements = true;
          }
        }
        if (start != -1L && end != -1L) {
          // add the new tracks inside
          smilResponse = smilService.addClips(smilResponse.getSmil(), elem.getId(), otherTracks, start, end - start);
        }
      } else if (elem instanceof SmilMediaElement) {
        throw new SmilException("Media elements inside SMIL body are not supported yet.");
      }
    }
    if (!hasElements) {
      throw new SmilException("Smil does not define any elements");
    }
    return smilResponse.getSmil();
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  public void setVideoEditorService(VideoEditorService editor) {
    videoEditorService = editor;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
