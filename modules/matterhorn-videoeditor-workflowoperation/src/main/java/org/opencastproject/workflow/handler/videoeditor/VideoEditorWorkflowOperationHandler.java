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
package org.opencastproject.workflow.handler.videoeditor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.opencastproject.ingest.api.IngestService;
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
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase;

public class VideoEditorWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory
          .getLogger(VideoEditorWorkflowOperationHandler.class);
  /**
   * Path to the hold ui resources
   */
  private static final String HOLD_UI_PATH = "/ui/operation/editor/index.html";

  /**
   * Name of the configuration option that provides the source flavors we use for processing.
   */
  private static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";
  /**
   * Name of the configuration option that provides the preview flavors we use as preview.
   */
  private static final String PREVIEW_FLAVORS_PROPERTY = "preview-flavors";
  /**
   * Name of the configuration option that provides the source flavors on skipped videoeditor operation.
   */
  private static final String SKIPPED_FLAVORS_PROPERTY = "skipped-flavors";
  /**
   * Name of the configuration option that provides the smil flavor as input.
   */
  private static final String SMIL_FLAVORS_PROPERTY = "smil-flavors";
  /**
   * Name of the configuration option that provides the smil flavor as input.
   */
  private static final String TARGET_SMIL_FLAVOR_PROPERTY = "target-smil-flavor";
  /**
   * Name of the configuration that provides the target flavor subtype for encoded media tracks.
   */
  private static final String TARGET_FLAVOR_SUBTYPE_PROPERTY = "target-flavor-subtype";
  /**
   * Name of the configuration that provides the smil file name
   */
  private static final String SMIL_FILE_NAME = "smil.smil";

  /**
   * The configuration options for this handler
   */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_FLAVORS_PROPERTY,
            "The flavor for working files (tracks to edit).");
    CONFIG_OPTIONS.put(PREVIEW_FLAVORS_PROPERTY,
            "The flavor for preview files (tracks to show in edit UI).");
    CONFIG_OPTIONS.put(SKIPPED_FLAVORS_PROPERTY,
            "The flavor for working files if videoeditor operation is disabled."
            + " This is an optional option."
            + " Default value is given by \"" + SOURCE_FLAVORS_PROPERTY + "\".");
    CONFIG_OPTIONS.put(SMIL_FLAVORS_PROPERTY,
            "The flavor for input smil files.");
    CONFIG_OPTIONS.put(TARGET_SMIL_FLAVOR_PROPERTY,
            "The flavor for target smil file.");
    CONFIG_OPTIONS.put(TARGET_FLAVOR_SUBTYPE_PROPERTY,
            "The flavor subtype for target media files.");
  }

  /**
   * The Smil service to modify smil files.
   */
  private SmilService smilService;
  /**
   * The VideoEditor service to edit files.
   */
  private VideoEditorService videoEditorService;
  /**
   * The Ingest service to ingest produced files.
   */
  private IngestService ingestService;
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
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   * JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.info("Start editor workflow for mediapackage {}", mp.getIdentifier().compact());

    // get configuration
    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String smilFlavorsProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(SMIL_FLAVORS_PROPERTY));
    if (smilFlavorsProperty == null) {
      throw new WorkflowOperationException(String.format("Required configuration property %s not set", SMIL_FLAVORS_PROPERTY));
    }
    String targetSmilFlavorProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(TARGET_SMIL_FLAVOR_PROPERTY));
    if (targetSmilFlavorProperty == null) {
      throw new WorkflowOperationException(String.format(
        "Required configuration property %s not set", TARGET_SMIL_FLAVOR_PROPERTY));
    }
    String previewTrackFlavorsProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(PREVIEW_FLAVORS_PROPERTY));
    if (previewTrackFlavorsProperty == null) {
      logger.info("Configuration property {} not set, use preview tracks from smil catalog");
    }

    if (StringUtils.trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY)) == null) {
      throw new WorkflowOperationException(String.format(
        "Required configuration property %s not set", TARGET_FLAVOR_SUBTYPE_PROPERTY));
    }

    // check at least one smil catalog exists
    SimpleElementSelector elementSelector = new SimpleElementSelector();
    for (String flavor : asList(smilFlavorsProperty)) {
      elementSelector.addFlavor(flavor);
    }
    Collection<MediaPackageElement> smilCatalogs = elementSelector.select(mp, false);
    MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();

    if (smilCatalogs.isEmpty()) {
      if (previewTrackFlavorsProperty == null) {
        throw new WorkflowOperationException(String.format("No smil catalogs found in mediapackage %s with flavors %s",
                mp.getIdentifier().compact(), smilFlavorsProperty));
      }

      // no smil catalogs exists but preview flavors are set
      // create new smil catalog
      TrackSelector trackSelector = new TrackSelector();
      for (String flavor : asList(previewTrackFlavorsProperty)) {
        trackSelector.addFlavor(flavor);
      }
      Collection<Track> previewTracks = trackSelector.select(mp, false);
      if (previewTracks.isEmpty()) {
        throw new WorkflowOperationException(String.format(
                "No preview tracks found in mediapackage %s with flavor %s",
                mp.getIdentifier().compact(), previewTrackFlavorsProperty));
      }
      Track[] previewTracksArr = previewTracks.toArray(new Track[previewTracks.size()]);
      MediaPackageElementFlavor smilFlavor = MediaPackageElementFlavor.parseFlavor(smilFlavorsProperty);

      for (Track previewTrack : previewTracks) {
        try {
          SmilResponse smilResponse = smilService.createNewSmil(mp);
          smilResponse = smilService.addParallel(smilResponse.getSmil());
          smilResponse = smilService.addClips(smilResponse.getSmil(),
                  smilResponse.getEntity().getId(),
                  previewTracksArr, 0L, previewTracksArr[0].getDuration());
          Smil smil = smilResponse.getSmil();

          InputStream is = null;
          try {
            // put new smil into workspace
            is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
            URI smilURI = workspace.put(mp.getIdentifier().compact(), smil.getId(), SMIL_FILE_NAME, is);
            MediaPackageElementFlavor trackSmilFlavor = previewTrack.getFlavor();
            if (!"*".equals(smilFlavor.getType())) {
              trackSmilFlavor = new MediaPackageElementFlavor(smilFlavor.getType(), trackSmilFlavor.getSubtype());
            }
            if (!"*".equals(smilFlavor.getSubtype())) {
              trackSmilFlavor = new MediaPackageElementFlavor(trackSmilFlavor.getType(), smilFlavor.getSubtype());
            }
            Catalog catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog, trackSmilFlavor);
            catalog.setIdentifier(smil.getId());
            mp.add(catalog);
          } finally {
            IOUtils.closeQuietly(is);
          }
        } catch (Exception ex) {
          throw new WorkflowOperationException(String.format(
                  "Failed to create smil catalog for mediapackage %s", mp.getIdentifier().compact()), ex);
        }
      }
    }

    // check target smil catalog exists
    MediaPackageElementFlavor targetSmilFlavor = MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty);
    Catalog[] targetSmilCatalogs = mp.getCatalogs(targetSmilFlavor);
    if (targetSmilCatalogs == null || targetSmilCatalogs.length == 0) {
      // create new empty smil to fill it from editor UI
      try {
          SmilResponse smilResponse = smilService.createNewSmil(mp);
          Smil smil = smilResponse.getSmil();

          InputStream is = null;
          try {
            // put new smil into workspace
            is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
            URI smilURI = workspace.put(mp.getIdentifier().compact(), smil.getId(), SMIL_FILE_NAME, is);
            Catalog catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog, targetSmilFlavor);
            catalog.setIdentifier(smil.getId());
            mp.add(catalog);
          } finally {
            IOUtils.closeQuietly(is);
          }
        } catch (Exception ex) {
          throw new WorkflowOperationException(String.format(
                  "Failed to create an initial empty smil catalog for mediapackage %s", mp.getIdentifier().compact()), ex);
        }
    }

    logger.info("Holding for video edit...");
    return createResult(mp, Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#skip(org.opencastproject.workflow.api.WorkflowInstance,
   * JobContext)
   */
  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    // If we do not hold for trim, we still need to put tracks in the mediapackage with the target flavor
    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.info("Skip video editor operation for mediapackage {}", mp.getIdentifier().compact());

    // get configuration
    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String sourceTrackFlavorsProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(SKIPPED_FLAVORS_PROPERTY));
    if (sourceTrackFlavorsProperty == null || sourceTrackFlavorsProperty.isEmpty()) {
      logger.info("\"{}\" option not set, use value of \"{}\"", SKIPPED_FLAVORS_PROPERTY, SOURCE_FLAVORS_PROPERTY);
      sourceTrackFlavorsProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(SOURCE_FLAVORS_PROPERTY));
      if (sourceTrackFlavorsProperty == null) {
        throw new WorkflowOperationException(String.format("Required configuration property %s not set.", SOURCE_FLAVORS_PROPERTY));
      }
    }
    String targetFlavorSubTypeProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY));
    if (targetFlavorSubTypeProperty == null) {
      throw new WorkflowOperationException(String.format("Required configuration property %s not set.", TARGET_FLAVOR_SUBTYPE_PROPERTY));
    }

    // get source tracks
    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(sourceTrackFlavorsProperty)) {
      trackSelector.addFlavor(flavor);
    }
    Collection<Track> sourceTracks = trackSelector.select(mp, false);

    for (Track sourceTrack : sourceTracks) {
      // set target track flavor
      Track clonedTrack = (Track) sourceTrack.clone();
      clonedTrack.setIdentifier(null);
      clonedTrack.setURI(sourceTrack.getURI()); // use the same URI as the original
      clonedTrack.setFlavor(new MediaPackageElementFlavor(sourceTrack.getFlavor().getType(),
              targetFlavorSubTypeProperty));
      mp.addDerived(clonedTrack, sourceTrack);
    }

    return createResult(mp, Action.SKIP);
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   * JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
          Map<String, String> properties)
          throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    logger.info("Resume video editor operation for mediapackage {}", mp.getIdentifier().compact());

    // get configuration
    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String sourceTrackFlavorsProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(SOURCE_FLAVORS_PROPERTY));
    if (sourceTrackFlavorsProperty == null) {
      throw new WorkflowOperationException(String.format("Required configuration property %s not set.", SOURCE_FLAVORS_PROPERTY));
    }
    String targetSmilFlavorProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(TARGET_SMIL_FLAVOR_PROPERTY));
    if (targetSmilFlavorProperty == null) {
      throw new WorkflowOperationException(String.format("Required configuration property %s not set.", TARGET_SMIL_FLAVOR_PROPERTY));
    }
    String targetFlavorSybTypeProperty = StringUtils.trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY));
    if (targetFlavorSybTypeProperty == null) {
      throw new WorkflowOperationException(String.format("Required configuration property %s not set.", TARGET_FLAVOR_SUBTYPE_PROPERTY));
    }

    // get source tracks
    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(sourceTrackFlavorsProperty)) {
      trackSelector.addFlavor(flavor);
    }
    Collection<Track> sourceTracks = trackSelector.select(mp, false);
    if (sourceTracks.isEmpty()) {
      throw new WorkflowOperationException(String.format(
              "No source tracks found in mediapacksge %s with flavors %s.",
              mp.getIdentifier().compact(), sourceTrackFlavorsProperty));
    }

    // get smil file
    MediaPackageElementFlavor smilTargetFlavor = MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty);
    Catalog[] smilCatalogs = mp.getCatalogs(smilTargetFlavor);
    if (smilCatalogs == null || smilCatalogs.length == 0) {
      throw new WorkflowOperationException(String.format(
              "No smil catalog found in mediapackage %s with flavor %s.",
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
        is = IOUtils.toInputStream(smil.toXML());
        // remove old smil
        workspace.delete(mp.getIdentifier().compact(), smilCatalogs[0].getIdentifier());
        mp.remove(smilCatalogs[0]);
        // put modified smil into workspace
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
      throw new WorkflowOperationException(String.format(
              "Failed to get smil catalog %s from mediapackage %s.",
              smilCatalogs[0].getIdentifier(), mp.getIdentifier().compact()), ex);
    } catch (IOException ex) {
      throw new WorkflowOperationException(String.format(
              "Can't open smil catalog %s from mediapackage %s.",
              smilCatalogs[0].getIdentifier(), mp.getIdentifier().compact()), ex);
    } catch (SmilException ex) {
      throw new WorkflowOperationException(ex);
    }

    // create video edit jobs and run them
    List<Job> jobs = null;
    try {
      logger.info("Create processing jobs for smil {}.", smilCatalogs[0].getIdentifier());
      jobs = videoEditorService.processSmil(smil);
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("Smil processing jobs for smil "
                + smilCatalogs[0].getIdentifier() + " are ended unsuccessfull.");
      }
      logger.info("Smil " + smilCatalogs[0].getIdentifier() + " processing finished.");
    } catch (ProcessFailedException ex) {
      throw new WorkflowOperationException("Processing smil " + smilCatalogs[0].getIdentifier() + " failed", ex);
    }

    // move edited tracks to work location and set target flavor
    Track editedTrack = null;
    boolean mpAdded = false;
    for (Job job : jobs) {
      try {
        editedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
        MediaPackageElementFlavor editedTrackFlavor = editedTrack.getFlavor();
        editedTrack.setFlavor(new MediaPackageElementFlavor(editedTrackFlavor.getType(), targetFlavorSybTypeProperty));
        URI editedTrackNewUri = workspace.moveTo(editedTrack.getURI(), mp.getIdentifier().compact(), editedTrack.getIdentifier(),
                FilenameUtils.getName(editedTrack.getURI().toString()));
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
        throw new WorkflowOperationException("Failed to get edited track information.", ex);
      } catch (Exception ex) {
        if (ex instanceof NotFoundException || ex instanceof IOException || ex instanceof IllegalArgumentException) {
          throw new WorkflowOperationException("Moving edited track to work location failed.", ex);
        } else {
          throw new WorkflowOperationException(ex);
        }
      }
    }

    logger.info("VideoEdit workflow {} finished", workflowInstance.getId());

    return createResult(mp, Action.CONTINUE);

  }

  protected Smil replaceAllTracksWith(Smil smil, Track[] otherTracks) throws SmilException {
    SmilResponse smilResponse;
    try {
      // copy smil to work with
      smilResponse = smilService.fromXml(smil.toXML());
    } catch (Exception ex) {
      throw new SmilException("Can't parse smil.");
    }

    long start;
    long end;
    // iterate over all elements inside smil body
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
          }
        }
        if (start != -1L && end != -1L) {
          // add the new tracks inside
          smilResponse = smilService.addClips(smilResponse.getSmil(), elem.getId(), otherTracks, start, end - start);
        }
      } else if (elem instanceof SmilMediaElement) {
        throw new SmilException("Media elements inside smil body are not supported yet.");
      }
    }
    return smilResponse.getSmil();
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  public void setVideoEditorService(VideoEditorService editor) {
    this.videoEditorService = editor;
  }

  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
