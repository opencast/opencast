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

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.AdaptivePlaylist;
import org.opencastproject.mediapackage.AdaptivePlaylist.HLSMediaPackageCheck;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function2;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * The <tt></tt> operation will make sure that media where hls playlists and video track come in separate files
 * will have appropriately references prior to further processing such as inspection.
 */
public class SanitizeAdaptiveWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SanitizeAdaptiveWorkflowOperationHandler.class);
  private static final String PLUS = "+";
  private static final String MINUS = "-";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the encoded file");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the encoded file");
  }

  /** The local workspace */
  private Workspace workspace = null;

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

  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
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
    logger.debug("Running HLS Check workflow operation on workflow {}", workflowInstance.getId());
    try {
      return sanitizeHLS(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Checks the references in the playists and make sure that the playlists can pass though an ffmpeg inspection. If the
   * file references are off, they will be rewritten. The problem is mainly the media package elementID.
   *
   * @param src
   *          The source media package
   * @param operation
   *          the sanitizeHLS workflow operation
   * @return the operation result containing the updated mediapackage
   * @throws EncoderException
   *           if encoding fails
   * @throws IOException
   *           if read/write operations from and to the workspace fail
   * @throws NotFoundException
   *           if the workspace does not contain the requested element
   * @throws URISyntaxException
   */
  private WorkflowOperationResult sanitizeHLS(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException,
          WorkflowOperationException, NotFoundException, MediaPackageException, IOException, URISyntaxException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    // Read the configuration properties
    String sourceFlavorName = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavorName = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));

    String[] targetTags = StringUtils.split(targetTrackTags, ",");

    List<String> removeTags = new ArrayList<String>();
    List<String> addTags = new ArrayList<String>();
    List<String> overrideTags = new ArrayList<String>();

    if (targetTags != null) {
      for (String tag : targetTags) {
        if (tag.startsWith(MINUS)) {
          removeTags.add(tag);
        } else if (tag.startsWith(PLUS)) {
          addTags.add(tag);
        } else {
          overrideTags.add(tag);
        }
      }
    }

    // Make sure the source flavor is properly set
    if (sourceFlavorName == null)
      throw new IllegalStateException("Source flavor must be specified");
    MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorName);

    // Make sure the target flavor is properly set
    if (targetTrackFlavorName == null)
      throw new IllegalStateException("Target flavor must be specified");
    MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetTrackFlavorName);

    // Select those tracks that have matching flavors
    Track[] tracks = mediaPackage.getTracks(sourceFlavor);
    List<Track> tracklist = Arrays.asList(tracks);

    // Nothing to sanitize, do not set target tags or flavor on tracks, just return
    if (!tracklist.stream().filter(AdaptivePlaylist.isHLSTrackPred).findAny().isPresent()) {
      return createResult(mediaPackage, Action.CONTINUE, 0);
    }
    HLSMediaPackageCheck hlstree;
    try {
      hlstree = new HLSMediaPackageCheck(tracklist, new Function<URI, File>() {
        @Override
        public File apply(URI uri) {
          try {
            return workspace.get(uri);
          } catch (NotFoundException | IOException e1) { // from workspace.get
            logger.error("Cannot get {} from workspace {}", uri, e1);
          }
          return null;
        }
      });
    } catch (URISyntaxException e1) {
      throw new MediaPackageException("Cannot process tracks from workspace");
    }
    /**
     * Adds new file to Mediapackage to replace old Track, while retaining all properties. Also sets the target flavor
     * and target tags
     */
    Function2<File, Track, Track> replaceHLSPlaylistInWS = new Function2<File, Track, Track>() {
      @Override
      public Track apply(File file, Track track) {
        try {
          InputStream inputStream = new FileInputStream(file);
          // put file into workspace for mp
          URI uri = workspace.put(mediaPackage.getIdentifier().toString(), track.getIdentifier(), file.getName(),
                  inputStream);
          track.setURI(uri); // point track to new URI
          handleTags(track, targetFlavor, overrideTags, removeTags, addTags); // add tags and flavor
          return track;
        } catch (Exception e) {
          logger.error("Cannot add track file to mediapackage in workspace: {} {} ",
                  mediaPackage.getIdentifier().toString(),
                  file);
          return null;
        }
      }
    };
    // remove old tracks if the entire operation succeeds, or remove new tracks if any of them fails
    Function<Track, Void> removeFromWS = new Function<Track, Void>() {
      @Override
      public Void apply(Track track) {
        try {
          workspace.delete(track.getURI());
        } catch (NotFoundException e) {
          logger.error("Cannot delete from workspace: File not found {} ", track);
        } catch (IOException e) {
          logger.error("Cannot delete from workspace: IO Error {} ", track);
        }
        return null;
      }
    };
    if (hlstree.needsRewriting()) {
      // rewrites the playlists and replaced the old ones in the mp
      try {
        hlstree.rewriteHLS(mediaPackage, replaceHLSPlaylistInWS, removeFromWS);
      } catch (Exception e) {
        logger.error("Error: cannot rewrite HLS renditions {}", e);
        throw new WorkflowOperationException(e);
      }
      for (Track track : tracks) { // Update the flavor and tags for all non HLS segments
        if (!AdaptivePlaylist.isPlaylist(track.getURI().getPath())) {
          handleTags(track, targetFlavor, overrideTags, removeTags, addTags);
          logger.info("Set flavor {} and tags to {} ", track, targetFlavor);
        }
      }
    } else { // change flavor to mark as sanitized
      for (Track track : tracks) {
        handleTags(track, targetFlavor, overrideTags, removeTags, addTags);
        logger.info("Set flavor {} and tags to {} ", track, targetFlavor);
      }
    }
    return createResult(mediaPackage, Action.CONTINUE, 0);
  }

  // Add the target tags and flavor
  private void handleTags(Track track, MediaPackageElementFlavor targetFlavor, List<String> overrideTags,
          List<String> removeTags, List<String> addTags) {
    if (targetFlavor != null) {
      String flavorType = targetFlavor.getType();
      String flavorSubtype = targetFlavor.getSubtype();
      if ("*".equals(flavorType))
        flavorType = track.getFlavor().getType();
      if ("*".equals(flavorSubtype))
        flavorSubtype = track.getFlavor().getSubtype();
      track.setFlavor(new MediaPackageElementFlavor(flavorType, flavorSubtype));
      logger.debug("Composed track has flavor '{}'", track.getFlavor());
    }
    if (overrideTags.size() > 0) {
      track.clearTags();
      for (String tag : overrideTags) {
        logger.trace("Tagging composed track with '{}'", tag);
        track.addTag(tag);
      }
    } else {
      for (String tag : removeTags) {
        logger.trace("Remove tagging '{}' from composed track", tag);
        track.removeTag(tag.substring(MINUS.length()));
      }
      for (String tag : addTags) {
        logger.trace("Add tagging '{}' to composed track", tag);
        track.addTag(tag.substring(PLUS.length()));
      }
    }
  }
}
