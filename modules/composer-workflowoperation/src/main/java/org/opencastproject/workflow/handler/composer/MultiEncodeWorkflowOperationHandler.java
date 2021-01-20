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
import org.opencastproject.mediapackage.AdaptivePlaylist;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The workflow definition for handling multiple concurrent outputs in one ffmpeg operation. This allows encoding and
 * tagging to be done in one operation
 */
public class MultiEncodeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MultiEncodeWorkflowOperationHandler.class);

  /** seperator for independent clauses */
  static final String SEPARATOR = ";";

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

  private Predicate<EncodingProfile> isManifestEP = p ->  p.getOutputType() == EncodingProfile.MediaType.Manifest;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running Multiencode workflow operation on workflow {}", workflowInstance.getId());

    try {
      return multiencode(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  protected class ElementProfileTagFlavor {
    private AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();
    private String targetFlavor = null;
    private String targetTags = null;
    private List<String> encodingProfiles = new ArrayList<>(); // redundant storage
    private List<EncodingProfile> encodingProfileList = new ArrayList<>();

    ElementProfileTagFlavor(String profiles) {
      List<String> profilelist = asList(profiles);
      for (String profile : profilelist) {
        EncodingProfile encodingprofile = composerService.getProfile(profile);
        if (encodingprofile != null)
          encodingProfiles.add(encodingprofile.getIdentifier());
        encodingProfileList.add(encodingprofile);
      }
    }

    public AbstractMediaPackageElementSelector<Track> getSelector() {
      return this.elementSelector;
    }

    public List<String> getProfiles() {
      return this.encodingProfiles;
    }

    public List<EncodingProfile> getEncodingProfiles() {
      return this.encodingProfileList;
    }

    void addSourceFlavor(String flavor) {
      this.elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }

    void addSourceTag(String tag) {
      this.elementSelector.addTag(tag);
    }

    void setTargetTags(String tags) {
      this.targetTags = tags;
    }

    void setTargetFlavor(String flavor) {
      this.targetFlavor = flavor;
    }

    String getTargetFlavor() {
      return this.targetFlavor;
    }

    String getTargetTags() {
      return this.targetTags;
    }
  }

  /*
   * Figures out the logic of all the source tags, flavors and profiles and sorts out the source tracks and
   * corresponding encoding profiles.
   *
   * Source Tracks are selected by (Flavor AND Tag) if they are both provided
   *
   * There can be multiple sources and flavors to create more than one source tracks. In the workflow, A semi-colon ";"
   * is used to separate the independent operations.
   *
   * The independent operations can be either all share the same set of properties or all have different sets of
   * properties. For example, There are two sets of source flavors: * "presenter/* ; presentation/*", one source tag,
   * eg: "preview", and two sets of encoding profiles, eg: "mp4,flv ; mp4,hdtv" then there are two concurrent
   * operations: the first one is all "presenter" tracks tagged "preview" will be encoded with "mp4" and "flv". The
   * second one is all "presentation" tracks tagged "preview" encoded with "mp4" and "hdtv"
   *
   */
  private List<ElementProfileTagFlavor> getSrcSelector(String[] sourceFlavors, String[] sourceTags,
          String[] targetFlavors, String[] targetTags, String[] profiles) throws WorkflowOperationException {
    int n = 0;
    List<ElementProfileTagFlavor> elementSelectors = new ArrayList<>();
    if (sourceTags == null && sourceFlavors == null)
      throw new WorkflowOperationException("No source tags or Flavor");
    if (profiles == null)
      throw new WorkflowOperationException("Missing profiles");
    if (sourceTags != null) { // If source tags are used to select tracks
      // If use source and target tags, there should be the same number of them or all map into one target
      if (targetTags != null && (targetTags.length != 1 && sourceTags.length != targetTags.length))
        throw new WorkflowOperationException("number of source tags " + sourceTags.length
                + " does not match number of target tags " + targetTags.length + " (must be the same or one target)");
      // There should be the same number of source tags or profile groups or all use same group of profiles
      if (profiles.length != 1 && sourceTags.length != profiles.length) {
        throw new WorkflowOperationException(
                "number of source tags segments " + sourceTags.length + " does not match number of profiles segments "
                        + profiles.length + " (must be the same or one profile)");
      }
      // If use source tags and source flavors, there should be the same number of them or one
      if (sourceFlavors != null && (sourceTags.length != 1 && sourceFlavors.length != 1)
              && sourceFlavors.length != sourceTags.length) {
        throw new WorkflowOperationException("number of source tags segments " + sourceTags.length
                + " does not match number of source Flavor segments " + sourceFlavors.length
                + " (must be the same or one)");
      }
      n = sourceTags.length; // at least this many tracks
    }
    if (sourceFlavors != null) { // If flavors are used to select tracks
      // If use source and target flavors, there should be the same number of them or all map into one target
      if (targetFlavors != null && (targetFlavors.length != 1 && sourceFlavors.length != targetFlavors.length)) {
        throw new WorkflowOperationException(
                "number of source flavors " + sourceFlavors.length + " segment does not match number of target flavors"
                        + targetFlavors.length + " (must be the same or one target flavor)");
      }
      // If use target tags, there should be the same number of source flavors and target tags or all map into one
      // target tag
      if (targetTags != null && targetTags.length != 1 && sourceFlavors.length != targetTags.length) {
        throw new WorkflowOperationException(
                "number of source flavors " + sourceFlavors.length + " segment does not match number of target Tags"
                        + targetTags.length + " (must be the same or one target)");
      }
      // Number of profile groups should match number of source flavors
      if ((profiles.length != 1 && sourceFlavors.length != profiles.length)) {
        throw new WorkflowOperationException("number of source flavors segments " + sourceFlavors.length
                + " does not match number of profiles segments " + profiles.length
                + " (must be the same or one profile)");
      }
      if (sourceFlavors.length > n)
        n = sourceFlavors.length; // at least this many tracks
    }
    int numProfiles = 0;
    // One for each source flavor
    for (int i = 0; i < n; i++) {
      elementSelectors.add(new ElementProfileTagFlavor(profiles[numProfiles]));
      if (profiles.length > 1)
        numProfiles++; // All source use the same set of profiles or its own
    }
    // If uses tags to select, but sets target flavor, they must match
    if (sourceTags != null && sourceFlavors != null) {
      if (sourceTags.length != sourceFlavors.length && sourceFlavors.length != 1 && sourceTags.length != 1) {
        throw new WorkflowOperationException(
                "number of source flavors " + sourceTags.length + " does not match number of source tags "
                        + sourceFlavors.length + " (must be the same or one set of tags or flavors)");
      }
    }
    populateFlavorsAndTags(elementSelectors, sourceFlavors, targetFlavors, sourceTags, targetTags);
    return elementSelectors;
  }

  private List<ElementProfileTagFlavor> populateFlavorsAndTags(List<ElementProfileTagFlavor> elementSelectors,
          String[] sourceFlavors, String[] targetFlavors, String[] sourceTags, String[] targetTags)
          throws WorkflowOperationException {
    int sf = 0;
    int tf = 0;
    int st = 0;
    int tt = 0;
    for (ElementProfileTagFlavor ep : elementSelectors) {
      try {
        if (sourceTags != null) {
          for (String tag : asList(sourceTags[st])) {
            ep.addSourceTag(tag);
          }
          if (sourceTags.length != 1)
            st++;
        }
        if (targetTags != null) {
          ep.setTargetTags(targetTags[tt]);
          if (targetTags.length != 1)
            tt++;
        }
        if (sourceFlavors != null) {
          for (String flavor : asList(sourceFlavors[sf])) {
            ep.addSourceFlavor(flavor);
          }
          if (sourceFlavors.length != 1)
            sf++;
        }
        if (targetFlavors != null) {
          for (String flavor : asList(targetFlavors[tf])) {
            ep.setTargetFlavor(flavor);
          }
          if (targetFlavors.length != 1)
            tf++;
        }
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Set Tags or Flavor " + e.getMessage());
      }
    }
    return elementSelectors;
  }

  private String[] getConfigAsArray(WorkflowOperationInstance operation, String name) {
    String sourceOption = StringUtils.trimToNull(operation.getConfiguration(name));
    return StringUtils.split(sourceOption, SEPARATOR);
  }

  private List<Track> getManifest(Collection<Track> tracks) {
    return tracks.stream().filter(AdaptivePlaylist.isHLSTrackPred).collect(Collectors.toList());
  }

  /*
   * Encode multiple tracks in a mediaPackage concurrently with different encoding profiles for each track. The encoding
   * profiles are specified by names in a list and are the names used to tag each corresponding output. Each source
   * track will start one operation on one worker. concurrency is achieved by running on different workers
   *
   * @param src The source media package
   *
   * @param operation the current workflow operation
   *
   * @return the operation result containing the updated media package
   *
   * @throws EncoderException if encoding fails
   *
   * @throws WorkflowOperationException if errors occur during processing
   *
   * @throws IOException if the workspace operations fail
   *
   * @throws NotFoundException if the workspace doesn't contain the requested file
   */
  private WorkflowOperationResult multiencode(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    // Check which tags have been configured
    String[] sourceTags = getConfigAsArray(operation, "source-tags");
    String[] sourceFlavors = getConfigAsArray(operation, "source-flavors");
    String[] targetTags = getConfigAsArray(operation, "target-tags");
    String[] targetFlavors = getConfigAsArray(operation, "target-flavors");
    String tagWithProfileConfig = StringUtils.trimToNull(operation.getConfiguration("tag-with-profile"));
    boolean tagWithProfile = BooleanUtils.toBoolean(tagWithProfileConfig);

    // Make sure either one of tags or flavors are provided
    if (sourceFlavors == null && sourceTags == null) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE);
    }
    String[] profiles = getConfigAsArray(operation, "encoding-profiles");
    if (profiles == null)
      throw new WorkflowOperationException("Missing encoding profiles");

    // Sort out the combinatorics of all the tags and flavors
    List<ElementProfileTagFlavor> selectors = getSrcSelector(sourceFlavors, sourceTags, targetFlavors, targetTags,
            profiles);

    long totalTimeInQueue = 0;
    Map<Job, JobInformation> encodingJobs = new HashMap<>();
    // Find the encoding profiles - should only be one per flavor or tag
    for (ElementProfileTagFlavor eptf : selectors) {
      // Look for elements matching the tag and flavor
      Collection<Track> elements = eptf.elementSelector.select(mediaPackage, true);
      for (Track sourceTrack : elements) {
        logger.info("Encoding track {} using encoding profile '{}'", sourceTrack, eptf.getProfiles().get(0).toString());
        // Start encoding and wait for the result
        encodingJobs.put(composerService.multiEncode(sourceTrack, eptf.getProfiles()),
                new JobInformation(sourceTrack, eptf, tagWithProfile));
      }
    }

    if (encodingJobs.isEmpty()) {
      logger.info("No matching tracks found");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Wait for the jobs to return
    if (!waitForStatus(encodingJobs.keySet().toArray(new Job[encodingJobs.size()])).isSuccess()) {
      throw new WorkflowOperationException("One of the encoding jobs did not complete successfully");
    }

    // Process the result
    for (Map.Entry<Job, JobInformation> entry : encodingJobs.entrySet()) {
      Job job = entry.getKey();
      Track sourceTrack = entry.getValue().getTrack(); // source
      ElementProfileTagFlavor info = entry.getValue().getInfo(); // tags and flavors
      List<EncodingProfile> eplist = entry.getValue().getProfileList();
      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();
      // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
      if (job.getPayload().length() > 0) {
        @SuppressWarnings("unchecked")
        List<Track> composedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
        // HLS Manifest profile has precedence and overrides individual encoding profiles
        boolean isHLS = eplist.stream().anyMatch(isManifestEP);
        if (isHLS) { // check that manifests and segments counts are correct
          decipherHLSPlaylistResults(sourceTrack, entry.getValue(), mediaPackage, composedTracks);
        } else if (composedTracks.size() != info.getProfiles().size()) {
          logger.info("Encoded {} tracks, with {} profiles", composedTracks.size(), info.getProfiles().size());
          throw new WorkflowOperationException("Number of output tracks does not match number of encoding profiles");
        }
        for (Track composedTrack : composedTracks) {
          if (info.getTargetFlavor() != null) { // Has Flavors
            // set it to the matching flavor in the order listed
            composedTrack.setFlavor(newFlavor(sourceTrack, info.getTargetFlavor()));
            logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());
          }
          if (info.getTargetTags() != null) { // Has Tags
            for (String tag : asList(info.getTargetTags())) {
              logger.trace("Tagging composed track with '{}'", tag);
              composedTrack.addTag(tag);
            }
          }
          // Tag each output with encoding profile name if configured
          if (entry.getValue().getTagWithProfile()) {
            tagByProfile(composedTrack, eplist);
          }
          String fileName;
          if (!isHLS || composedTrack.isMaster()) {
            // name after source track if user facing
            fileName = getFileNameFromElements(sourceTrack, composedTrack);
          } else { // HLS-VOD
            // Should all the files be renamed to the same as source
            // which defeats the purpose of the suffix in encoding profiles
            fileName = FilenameUtils.getName(composedTrack.getURI().getPath());
          }
          // store new tracks to mediaPackage
          composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
                  composedTrack.getIdentifier(), fileName));
          mediaPackage.addDerived(composedTrack, sourceTrack);
        }
      } else {
        logger.warn("No output from MultiEncode operation");
      }
    }
    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("MultiEncode operation completed");
    return result;
  }

  /**
   * Find the matching encoding profile for this track and tag by name
   *
   * @param track
   * @param profiles
   *          - profiles used to encode a track to multiple formats
   * @return
   */
  private void tagByProfile(Track track, List<EncodingProfile> profiles) {
    String rawfileName = track.getURI().getRawPath();
    for (EncodingProfile ep : profiles) {
      String suffix = ep.getSuffix();
      // !! workspace.putInCollection renames the file - need to do the same with suffix
      suffix = PathSupport.toSafeName(suffix);
      if (suffix.length() > 0 && rawfileName.endsWith(suffix)) {
        track.addTag(ep.getIdentifier());
        return;
      }
    }
  }

  private void decipherHLSPlaylistResults(Track track, JobInformation jobInfo, MediaPackage mediaPackage,
          List<Track> composedTracks)
          throws WorkflowOperationException, IllegalArgumentException, NotFoundException, IOException {
    int nprofiles = jobInfo.getInfo().getProfiles().size();
    List<Track> manifests = getManifest(composedTracks);

    if (manifests.size() != nprofiles) {
      throw new WorkflowOperationException("Number of output playlists does not match number of encoding profiles");
    }
    if (composedTracks.size() != manifests.size() * 2 - 1) {
      throw new WorkflowOperationException("Number of output media does not match number of encoding profiles");
    }
  }

  private MediaPackageElementFlavor newFlavor(Track track, String flavor) throws WorkflowOperationException {
    if (StringUtils.isNotBlank(flavor)) {
      try {
        MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(flavor);
        String flavorType = targetFlavor.getType();
        String flavorSubtype = targetFlavor.getSubtype();
        // Adjust the target flavor. Make sure to account for partial updates
        if ("*".equals(flavorType))
          flavorType = track.getFlavor().getType();
        if ("*".equals(flavorSubtype))
          flavorSubtype = track.getFlavor().getSubtype();
        return (new MediaPackageElementFlavor(flavorType, flavorSubtype));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Target flavor '" + flavor + "' is malformed");
      }
    }
    return null;
  }

  /**
   * This class is used to store context information for the jobs.
   */
  private static final class JobInformation {

    private Track track = null;
    private ElementProfileTagFlavor info = null;
    private boolean tagWithProfile;

    JobInformation(Track track, ElementProfileTagFlavor info, boolean tagWithProfile) {
      this.track = track;
      this.info = info;
      this.tagWithProfile = tagWithProfile;
    }

    public List<EncodingProfile> getProfileList() {
      return info.encodingProfileList;
    }

    /**
     * Returns the track.
     *
     * @return the track
     */
    public Track getTrack() {
      return track;
    }

    public boolean getTagWithProfile() {
      return this.tagWithProfile;
    }

    public ElementProfileTagFlavor getInfo() {
      return info;
    }
  }

}
