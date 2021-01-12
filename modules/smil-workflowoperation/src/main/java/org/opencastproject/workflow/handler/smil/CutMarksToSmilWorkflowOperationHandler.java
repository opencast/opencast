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
package org.opencastproject.workflow.handler.smil;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.JAXBException;

/**
 * The workflow definition for converting a smil containing cut marks into a legal smil for cutting
 */
public class CutMarksToSmilWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Workflow configuration keys */
  private static final String SOURCE_MEDIA_FLAVORS = "source-media-flavors";
  private static final String SOURCE_JSON_FLAVOR = "source-json-flavor";

  private static final String TARGET_SMIL_FLAVOR = "target-smil-flavor";
  private static final String TARGET_TAGS = "target-tags";

  private static final String CUTTING_SMIL_NAME = "prepared_cutting_smil";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PartialImportWorkflowOperationHandler.class);

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
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * The SMIL service to modify SMIL files.
   */
  private SmilService smilService;
  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  /** JSON Parser */
  private static final Gson gson = new Gson();

  /** Stores information read from JSON */
  class Times {
    private Long begin;
    private Long duration;

    public Long getStartTime() {
      return begin;
    }
    public void setStartTime(Long startTime) {
      this.begin = startTime;
    }
    public Long getDuration() {
      return duration;
    }
    public void setDuration(Long duration) {
      this.duration = duration;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.info("Running cut marks to smil workflow operation on workflow {}", workflowInstance.getId());

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final MediaPackage mediaPackage = (MediaPackage) workflowInstance.getMediaPackage().clone();

    // Read config options
    final MediaPackageElementFlavor jsonFlavor = MediaPackageElementFlavor.parseFlavor(
            getConfig(operation, SOURCE_JSON_FLAVOR));
    final MediaPackageElementFlavor targetSmilFlavor = MediaPackageElementFlavor.parseFlavor(
            getConfig(operation, TARGET_SMIL_FLAVOR));

    String flavorNames = operation.getConfiguration(SOURCE_MEDIA_FLAVORS);
    final List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();
    for (String flavorName : asList(flavorNames))
      flavors.add(MediaPackageElementFlavor.parseFlavor(flavorName));

    // Target tags
    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS));
    List<String> targetTags = asList(targetTagsOption);

    // Is there a catalog?
    Catalog[] catalogs = mediaPackage.getCatalogs(jsonFlavor);
    if (catalogs.length < 1) {
      logger.warn("No catalogs in the source flavor. Skipping...");
      final WorkflowOperationResult result = createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
      return result;
    } else if (catalogs.length > 1) {
      throw new WorkflowOperationException("More than one catalog in the source flavor! Make sure there is only catalog.");
    }

    // Parse JSON
    Times[] cutmarks;
    Catalog jsonWithTimes = catalogs[0];
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(getMediaPackageElementPath(jsonWithTimes)))) {
      cutmarks = gson.fromJson(bufferedReader, Times[].class);
    } catch (Exception e) {
      throw new WorkflowOperationException("Could not read JSON", e);
    }
    LinkedList<Times> cutmarksList = new LinkedList<Times>(Arrays.asList(cutmarks));

    // If the catalog was empty, give up
    if (cutmarksList.size() < 1) {
      logger.warn("Source JSON did not contain any timestamps! Skipping...");
      final WorkflowOperationResult result = createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
      return result;
    }

    // Check parsing results
    for (Times entry : cutmarksList) {
      logger.info("Entry begin {}, Entry duration {}", entry.begin, entry.duration);
      if (entry.begin < 0 || entry.duration < 0) {
        throw new WorkflowOperationException("Times may not be negative.");
      }
    }

    // Get video tracks
    logger.info("Get tracks from media package");
    ArrayList<Track> tracksFromFlavors = new ArrayList<>();
    for (MediaPackageElementFlavor flavor : flavors) {
      logger.debug("Trying to get Tracks from Flavor {}", flavor);
      List<Track> tracks = getTracksFromFlavor(flavor, mediaPackage);
      logger.debug("Found {} tracks in flavor {}", tracks.size(), flavor);
      if (tracks.size() > 0) {
        tracksFromFlavors.addAll(tracks);
      }
    }

    // Are there actually any tracks?
    if (tracksFromFlavors.isEmpty()) {
      logger.warn("None of the given flavors contained a track. Skipping...");
      final WorkflowOperationResult result = createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
      return result;
    }

    // Check for cut marks that would lead to errors with the given tracks and remove them
    // Possible TODO: Instead of removing, only apply cut marks to tracks with a long enough duration?
    // Get the shortest duration of all tracks
    long shortestDuration = Long.MAX_VALUE;
    for (Track track : tracksFromFlavors) {
      if (track.getDuration() < shortestDuration) {
        shortestDuration = track.getDuration();
      }
    }
    // Remove all timestamps that begin after the shortest duration
    ListIterator<Times> iter = cutmarksList.listIterator();
    while (iter.hasNext()) {
      long begin = iter.next().begin;
      if (begin > shortestDuration) {
        logger.info("Skipped mark with begin: {}, ", begin);
        iter.remove();
      }
    }
    // If the timestamp list is now empty, give up
    if (cutmarksList.size() < 1) {
      logger.warn("No timestamps are valid for the given tracks! Skipping...");
      final WorkflowOperationResult result = createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
      return result;
    }

    // Create the new SMIL document
    Smil smil;
    try {
      SmilResponse smilResponse = smilService.createNewSmil(mediaPackage);

      logger.info("Start adding tracks");
      for (Times mark : cutmarksList) {
        smilResponse = smilService.addParallel(smilResponse.getSmil());
        SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
        logger.debug("Segment begin: {}; Segment duration: {}", mark.begin, mark.duration);
        // Add tracks (as array) to par
        smilResponse = smilService
                .addClips(smilResponse.getSmil(),
                        par.getId(),
                        tracksFromFlavors.toArray(new Track[tracksFromFlavors.size()]), //new Track[] { presenterTrack, presentationTrack },
                        mark.begin,
                        mark.duration);
      }

      smil = smilResponse.getSmil();
      logger.info("Done adding tracks");
    } catch (SmilException e) {
      throw new WorkflowOperationException("Failed to create SMIL Catalog", e);
    }

    // Put new SMIL into workspace and add it to mediapackage
    try (InputStream is = IOUtils.toInputStream(smil.toXML(), "UTF-8")) {
      URI smilURI = workspace.put(mediaPackage.getIdentifier().toString(), smil.getId(), CUTTING_SMIL_NAME, is);
      MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      Catalog catalog = (Catalog) mpeBuilder
              .elementFromURI(smilURI, MediaPackageElement.Type.Catalog, targetSmilFlavor);
      catalog.setIdentifier(smil.getId());
      for (String tag : targetTags) {
        catalog.addTag(tag);
      }
      mediaPackage.add(catalog);
    } catch (JAXBException | SAXException | IOException e) {
      throw new WorkflowOperationException("Failed to parse crated SMIL Catalog", e);
    }

    final WorkflowOperationResult result = createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
    logger.debug("Cut marks to smil operation completed");
    return result;
  }

  private List<Track> getTracksFromFlavor(MediaPackageElementFlavor flavor, MediaPackage mediaPackage) {
    // Get tracks from flavor
    TrackSelector trackSelector = new TrackSelector();
    trackSelector.addFlavor(flavor);
    Collection<Track> tracks = trackSelector.select(mediaPackage, false);

    // Get only videos
    ArrayList<Track> videos = new ArrayList<Track>();
    for (Track video : tracks) {
      if (video.hasVideo()) {
        videos.add((video));
      }
    }

    return videos;
  }

  /**
   * Returns the absolute path for a given MediaPackageElement
   * @param mpe
   *          The MediaPackageElement we want to know the absolute path for
   * @return
   *          The absolute path
   * @throws WorkflowOperationException
   */
  private String getMediaPackageElementPath(MediaPackageElement mpe) throws WorkflowOperationException {
    File mediaFile;
    try {
      mediaFile = workspace.get(mpe.getURI());
    } catch (NotFoundException | IOException e) {
      throw new WorkflowOperationException(
              "Error finding the media file in the workspace", e);
    }

    return mediaFile.getAbsolutePath();
  }
}
