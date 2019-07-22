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
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The workflow definition for handling "compose" operations
 */
public class ProcessSmilWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  static final String SEPARATOR = ";";
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ProcessSmilWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;
  /** The smil service to parse the smil */
  private SmilService smilService;
  /** The local workspace */
  private Workspace workspace = null;

  /**
   * A convenience structure to hold info for each paramgroup in the Smil which will produce one trim/concat/encode job
   */
  private class TrackSection {
    private final String paramGroupId;
    private List<Track> sourceTracks;
    private List<String> smilTracks;
    private final String flavor;
    private String mediaType = ""; // Has both Audio and Video

    TrackSection(String id, String flavor) {
      this.flavor = flavor;
      this.paramGroupId = id;
    }

    public List<Track> getSourceTracks() {
      return sourceTracks;
    }

    /**
     * Set source Tracks for this group, if audio or video is missing in any of the source files, then do not try to
     * edit with the missing media type, because it will fail
     *
     * @param sourceTracks
     */
    public void setSourceTracks(List<Track> sourceTracks) {
      boolean hasVideo = true;
      boolean hasAudio = true;
      this.sourceTracks = sourceTracks;
      for (Track track : sourceTracks) {
        if (!track.hasVideo())
          hasVideo = false;
        if (!track.hasAudio())
          hasAudio = false;
      }
      if (!hasVideo) {
        mediaType = ComposerService.AUDIO_ONLY;
      }
      if (!hasAudio) {
        mediaType = ComposerService.VIDEO_ONLY;
      }
    }

    public String getFlavor() {
      return flavor;
    }

    @Override
    public String toString() {
      return paramGroupId + " " + flavor + " " + sourceTracks.toString();
    }

    public void setSmilTrackList(List<String> smilSourceTracks) {
      smilTracks = smilSourceTracks;
    }

    public List<String> getSmilTrackList() {
      return smilTracks;
    }
  };

  // To return both params from a function that checks all the jobs
  private class ResultTally {
    private final MediaPackage mediaPackage;
    private final long totalTimeInQueue;

    ResultTally(MediaPackage mediaPackage, long totalTimeInQueue) {
      super();
      this.mediaPackage = mediaPackage;
      this.totalTimeInQueue = totalTimeInQueue;
    }

    public MediaPackage getMediaPackage() {
      return mediaPackage;
    }

    public long getTotalTimeInQueue() {
      return totalTimeInQueue;
    }
  }

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
   * Callback for the OSGi declarative services configuration.
   *
   * @param smilService
   */
  protected void setSmilService(SmilService smilService) {
    this.smilService = smilService;
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
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    try {
      return processSmil(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      e.printStackTrace();
      throw new WorkflowOperationException(e);
    }
  }

  private String[] getConfigAsArray(WorkflowOperationInstance operation, String name) {
    String sourceOption = StringUtils.trimToNull(operation.getConfiguration(name));
    String[] options = (sourceOption != null) ? sourceOption.split(SEPARATOR) : null;
    return (options);
  }

  private String[] collapseConfig(WorkflowOperationInstance operation, String name) {
    String targetOption = StringUtils.trimToNull(operation.getConfiguration(name));
    return (targetOption != null) ? new String[] { targetOption.replaceAll(SEPARATOR, ",") } : null;
  }

  /**
   * Encode tracks from Smil using profiles stored in properties and updates current MediaPackage. This procedure parses
   * the workflow definitions and decides how many encoding jobs are needed
   *
   * @param src
   *          The source media package
   * @param operation
   *          the current workflow operation
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
  private WorkflowOperationResult processSmil(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    // Check which tags have been configured
    String smilFlavorOption = StringUtils.trimToEmpty(operation.getConfiguration("smil-flavor"));
    String[] srcFlavors = getConfigAsArray(operation, "source-flavors");
    String[] targetFlavors = getConfigAsArray(operation, "target-flavors");
    String[] targetTags = getConfigAsArray(operation, "target-tags");
    String[] profilesSections = getConfigAsArray(operation, "encoding-profiles");
    String tagWithProfileConfig = StringUtils.trimToNull(operation.getConfiguration("tag-with-profile"));
    boolean tagWithProfile = tagWithProfileConfig != null && Boolean.parseBoolean(tagWithProfileConfig);

    // Make sure there is a smil src
    if (StringUtils.isBlank(smilFlavorOption)) {
      logger.info("No smil flavor has been specified, no src to process"); // Must have Smil input
      return createResult(mediaPackage, Action.CONTINUE);
    }

    if (srcFlavors == null) {
      logger.info("No source flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE); // Should be OK
    }
    // Make sure at least one encoding profile is provided
    if (profilesSections == null) {
      throw new WorkflowOperationException("No encoding profile was specified");
    }

    /*
     * Must have smil file, and encoding profile(s) If source-flavors is used, then target-flavors must be used If
     * separators ";" are used in source-flavors, then there must be the equivalent number of matching target-flavors
     * and encoding profiles used, or one for all of them.
     */
    if (srcFlavors.length > 1) { // Different processing for each flavor
      if (targetFlavors != null && srcFlavors.length != targetFlavors.length && targetFlavors.length != 1) {
        String mesg = "Number of target flavor sections " + targetFlavors + " must either match that of src flavor "
                + srcFlavors + " or equal 1 ";
        throw new WorkflowOperationException(mesg);
      }
      if (srcFlavors.length != profilesSections.length) {
        if (profilesSections.length != 1) {
          String mesg = "Number of encoding profile sections " + profilesSections
                  + " must either match that of src flavor " + srcFlavors + " or equal 1 ";
          throw new WorkflowOperationException(mesg);
        } else { // we need to duplicate profileSections for each src selector
          String[] array = new String[srcFlavors.length];
          Arrays.fill(array, 0, srcFlavors.length, profilesSections[0]);
          profilesSections = array;
        }
      }
      if (targetTags != null && srcFlavors.length != targetTags.length && targetTags.length != 1) {
        String mesg = "Number of target Tags sections " + targetTags + " must either match that of src flavor "
                + srcFlavors + " or equal 1 ";
        throw new WorkflowOperationException(mesg);
      }
    } else { // Only one srcFlavor - collapse all sections into one
      targetFlavors = collapseConfig(operation, "target-flavors");
      targetTags = collapseConfig(operation, "target-tags");
      profilesSections = collapseConfig(operation, "encoding-profiles");
      if (profilesSections.length != 1)
        throw new WorkflowOperationException(
                "No matching src flavors " + srcFlavors + " for encoding profiles sections " + profilesSections);

      logger.debug("Single input flavor: output= " + Arrays.toString(targetFlavors) + " tag: "
              + Arrays.toString(targetTags) + " profile:" + Arrays.toString(profilesSections));
    }

    Map<Job, JobInformation> encodingJobs = new HashMap<Job, JobInformation>();
    for (int i = 0; i < profilesSections.length; i++) {
      // Each section is one multiconcatTrim job - set up the jobs
      processSection(encodingJobs, mediaPackage, (srcFlavors.length > 1) ? srcFlavors[i] : srcFlavors[0],
              (targetFlavors != null) ? ((targetFlavors.length > 1) ? targetFlavors[i] : targetFlavors[0]) : null,
              (targetTags != null) ? ((targetTags.length > 1) ? targetTags[i] : targetTags[0]) : null,
              (profilesSections.length > 0) ? profilesSections[i] : profilesSections[0], smilFlavorOption,
              tagWithProfile);
    }

    if (encodingJobs.isEmpty()) {
      logger.info("Failed to process any tracks");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Wait for the jobs to return
    if (!waitForStatus(encodingJobs.keySet().toArray(new Job[encodingJobs.size()])).isSuccess()) {
      throw new WorkflowOperationException("One of the encoding jobs did not complete successfully");
    }
    ResultTally allResults = parseResults(encodingJobs, mediaPackage);
    WorkflowOperationResult result = createResult(allResults.getMediaPackage(), Action.CONTINUE,
            allResults.getTotalTimeInQueue());
    logger.debug("ProcessSmil operation completed");
    return result;

  }

  /**
   * Process one group encode section with one source Flavor declaration(may be wildcard) , sharing one set of shared
   * optional target tags/flavors and one set of encoding profiles
   *
   * @param encodingJobs
   * @param mediaPackage
   * @param srcFlavors
   *          - used to select which param group/tracks to process
   * @param targetFlavors
   *          - the resultant track will be tagged with these flavors
   * @param targetTags
   *          - the resultant track will be tagged
   * @param media
   *          - if video or audio only
   * @param encodingProfiles
   *          - profiles to use, if ant of them does not fit the source tracks, they will be omitted
   * @param smilFlavor
   *          - the smil flavor for the input smil
   * @param tagWithProfile - tag target with profile name
   * @throws WorkflowOperationException
   *           if flavors/tags/etc are malformed or missing
   * @throws EncoderException
   *           if encoding command cannot be constructed
   * @throws MediaPackageException
   * @throws IllegalArgumentException
   * @throws NotFoundException
   * @throws IOException
   */
  private void processSection(Map<Job, JobInformation> encodingJobs, MediaPackage mediaPackage,
          String srcFlavors, String targetFlavors, String targetTags,
          String encodingProfiles, String smilFlavor, boolean tagWithProfile) throws WorkflowOperationException,
          EncoderException, MediaPackageException, IllegalArgumentException, NotFoundException, IOException {
    // Select the source flavors
    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();
    for (String flavor : asList(srcFlavors)) {
      try {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Source flavor '" + flavor + "' is malformed");
      }
    }
    Smil smil = getSmil(mediaPackage, smilFlavor);
    // Check that the matching source tracks exist in the SMIL
    List<TrackSection> smilgroups;
    try {
      smilgroups = selectTracksFromMP(mediaPackage, smil, srcFlavors);
    } catch (URISyntaxException e1) {
      logger.info("Smil contains bad URI {}", e1);
      throw new WorkflowOperationException("Smil contains bad URI - cannot process", e1);
    }
    if (smilgroups.size() == 0 || smilgroups.get(0).sourceTracks.size() == 0) {
      logger.info("Smil does not contain any tracks of {} source flavor", srcFlavors);
      return;
    }

    // Check Target flavor
    MediaPackageElementFlavor targetFlavor = null;
    if (StringUtils.isNotBlank(targetFlavors)) {
      try {
        targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavors);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Target flavor '" + targetFlavors + "' is malformed");
      }
    }

    Set<EncodingProfile> profiles = new HashSet<EncodingProfile>();
    Set<String> profileNames = new HashSet<String>();
    // Find all the encoding profiles
    // Check that the profiles support the media source types
    for (TrackSection ts : smilgroups)
      for (Track track : ts.getSourceTracks()) {
        // Check that the profile is supported
        for (String profileName : asList(encodingProfiles)) {
          EncodingProfile profile = composerService.getProfile(profileName);
          if (profile == null)
            throw new WorkflowOperationException("Encoding profile '" + profileName + "' was not found");
          MediaType outputType = profile.getOutputType();
          // Check if the track supports the output type of the profile MediaType outputType = profile.getOutputType();
          // Omit if needed
          if (outputType.equals(MediaType.Audio) && !track.hasAudio()) {
            logger.info("Skipping encoding of '{}' with " + profileName + ", since the track lacks an audio stream",
                    track);
            continue;
          } else if (outputType.equals(MediaType.Visual) && !track.hasVideo()) {
            logger.info("Skipping encoding of '{}' " + profileName + ", since the track lacks a video stream", track);
            continue;
          } else if (outputType.equals(MediaType.AudioVisual) && !track.hasAudio() && !track.hasVideo()) {
            logger.info("Skipping encoding of '{}' (audiovisual)" + profileName
                    + ", since it lacks a audio or video stream", track);
            continue;
          }
          profiles.add(profile); // Include this profiles for encoding
          profileNames.add(profileName);
        }
      }
    // Make sure there is at least one profile
    if (profiles.isEmpty())
      throw new WorkflowOperationException("No encoding profile was specified");

    List<String> tags = (targetTags != null) ? asList(targetTags) : null;
    // Encode all tracks found in each param group
    // Start encoding and wait for the result - usually one for presenter, one for presentation
    for (TrackSection trackGroup : smilgroups) {
      encodingJobs.put(
              composerService.processSmil(smil, trackGroup.paramGroupId, trackGroup.mediaType,
                      new ArrayList<String>(profileNames)),
              new JobInformation(trackGroup.paramGroupId, trackGroup.sourceTracks,
                      new ArrayList<EncodingProfile>(profiles), tags, targetFlavor, tagWithProfile));

      logger.info("Edit and encode {} target flavors: {} tags: {} profile {}", trackGroup, targetFlavor, tags,
              profileNames);
    }
  }

  /**
   * parse all the encoding jobs to collect all the composed tracks, if any of them fail, just fail the whole thing and
   * try to clean up
   *
   * @param encodingJobs
   *          - queued jobs to do the encodings, this is parsed for payload
   * @param mediaPackage
   *          - to hold the target tracks
   * @return a structure with time in queue plus a mediaPackage with all the new tracks added if all the encoding jobs
   *         passed, if any of them fail, just fail the whole thing and try to clean up
   * @throws IllegalArgumentException
   * @throws NotFoundException
   * @throws IOException
   * @throws MediaPackageException
   */
  @SuppressWarnings("unchecked")
  private ResultTally parseResults(Map<Job, JobInformation> encodingJobs, MediaPackage mediaPackage)
          throws IllegalArgumentException, NotFoundException, IOException, MediaPackageException {
    // Process the result
    long totalTimeInQueue = 0;
    for (Map.Entry<Job, JobInformation> entry : encodingJobs.entrySet()) {
      Job job = entry.getKey();
      List<Track> tracks = entry.getValue().getTracks();
      Track track = tracks.get(0); // Can only reference one track, pick one
      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();
      // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
      List<Track> composedTracks = null;
      if (job.getPayload().length() > 0) {
        composedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
        // Adjust the target tags
        for (Track composedTrack : composedTracks) {
          if (entry.getValue().getTags() != null) {
            for (String tag : entry.getValue().getTags()) {
              composedTrack.addTag(tag);
            }
          }
          // Adjust the target flavor. Make sure to account for partial updates
          MediaPackageElementFlavor targetFlavor = entry.getValue().getFlavor();
          if (targetFlavor != null) {
            String flavorType = targetFlavor.getType();
            String flavorSubtype = targetFlavor.getSubtype();
            if ("*".equals(flavorType))
              flavorType = track.getFlavor().getType();
            if ("*".equals(flavorSubtype))
              flavorSubtype = track.getFlavor().getSubtype();
            composedTrack.setFlavor(new MediaPackageElementFlavor(flavorType, flavorSubtype));
            logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());
          }
          String fileName = composedTrack.getURI().getRawPath();
          if (entry.getValue().getTagProfile()) {
            // Tag each output with encoding profile name, if configured
            List<EncodingProfile> eps = entry.getValue().getProfiles();
            for (EncodingProfile ep : eps) {
              String suffix = ep.getSuffix();
              // !! workspace.putInCollection renames the file - need to do the same with suffix
              suffix = PathSupport.toSafeName(suffix);
              if (suffix.length() > 0 && fileName.endsWith(suffix)) {
                composedTrack.addTag(ep.getIdentifier());
                logger.debug("Tagging composed track {} with '{}'", composedTrack.getURI(), ep.getIdentifier());
                break;
              }
            }
          }

          composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
                  composedTrack.getIdentifier(), fileName));
          synchronized (mediaPackage) {
            mediaPackage.addDerived(composedTrack, track);
          }
        }
      }
    }
    return new ResultTally(mediaPackage, totalTimeInQueue);
  }

  /**
   * @param trackFlavor
   * @param sourceFlavor
   * @return true if trackFlavor matches sourceFlavor
   */
  private boolean trackMatchesFlavor(MediaPackageElementFlavor trackFlavor, MediaPackageElementFlavor sourceFlavor) {
    return ((trackFlavor.getType().equals(sourceFlavor.getType()) && trackFlavor.getSubtype() // exact match
            .equals(sourceFlavor.getSubtype()))
            || ("*".equals(sourceFlavor.getType()) && trackFlavor.getSubtype().equals(sourceFlavor.getSubtype())) // same
                                                                                                                  // subflavor
            || (trackFlavor.getType().equals(sourceFlavor.getType()) && "*".equals(sourceFlavor.getSubtype()))); // same
                                                                                                                 // flavor
  }

  /**
   * @param mediaPackage
   *          - mp obj contains tracks
   * @param smil
   *          - smil obj contains description of clips
   * @param srcFlavors
   *          - source flavor string (may contain wild cards)
   * @return a structure of smil groups, each with a single flavor and mp tracks for that flavor only
   * @throws WorkflowOperationException
   * @throws URISyntaxException
   */
  private List<TrackSection> selectTracksFromMP(MediaPackage mediaPackage, Smil smil, String srcFlavors)
          throws WorkflowOperationException, URISyntaxException {
    List<TrackSection> sourceTrackList = new ArrayList<TrackSection>();
    Collection<TrackSection> smilFlavors = parseSmil(smil);
    Iterator<TrackSection> it = smilFlavors.iterator();
    while (it.hasNext()) {
      TrackSection ts = it.next();

      for (String f : StringUtils.split(srcFlavors, ",")) { // Look for all source Flavors
        String sourceFlavorStr = StringUtils.trimToNull(f);
        if (sourceFlavorStr == null)
          continue;
        MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorStr);
        MediaPackageElementFlavor trackFlavor = MediaPackageElementFlavor.parseFlavor(ts.getFlavor());

        if (trackMatchesFlavor(trackFlavor, sourceFlavor)) {
          sourceTrackList.add(ts); // This smil group matches src Flavor, add to list
          Track[] elements = null;
          List<Track> sourceTracks = new ArrayList<Track>();
          elements = mediaPackage.getTracks(sourceFlavor);
          for (String t : ts.getSmilTrackList()) { // Look thru all the tracks referenced by the smil
            URI turi = new URI(t);
            for (Track e : elements)
              if (e.getURI().equals(turi)) { // find it in the mp
                sourceTracks.add(e); // add the track from mp containing inspection info
              }
          }
          if (sourceTracks.isEmpty()) {
            logger.info("ProcessSmil - No tracks in mediapackage matching the URI in the smil- cannot process");
            throw new WorkflowOperationException("Smil has no matching tracks in the mediapackage");
          }
          ts.setSourceTracks(sourceTracks); // Will also if srcTracks are Video/Audio Only
        }
      }
    }
    return sourceTrackList;
  }

  /**
   * Get smil from media package
   *
   * @param mp
   * @param smilFlavorOption
   * @return smil
   * @throws WorkflowOperationException
   */
  private Smil getSmil(MediaPackage mp, String smilFlavorOption) throws WorkflowOperationException {
    MediaPackageElementFlavor smilFlavor = MediaPackageElementFlavor.parseFlavor(smilFlavorOption);
    Catalog[] catalogs = mp.getCatalogs(smilFlavor);
    if (catalogs.length == 0) {
      throw new WorkflowOperationException("MediaPackage does not contain a SMIL document.");
    }
    Smil smil = null;
    try {
      File smilFile = workspace.get(catalogs[0].getURI());
      // break up chained method for junit smil service mockup
      SmilResponse response = smilService.fromXml(FileUtils.readFileToString(smilFile, "UTF-8"));
      smil = response.getSmil();
      return smil;
    } catch (NotFoundException ex) {
      throw new WorkflowOperationException("MediaPackage does not contain a smil catalog.");
    } catch (IOException ex) {
      throw new WorkflowOperationException("Failed to read smil catalog.", ex);
    } catch (SmilException ex) {
      throw new WorkflowOperationException(ex);
    }
  }

  /**
   * Sort paramGroup by flavor, each one will be a separate job
   *
   * @param smil
   * @return TrackSection
   */
  private Collection<TrackSection> parseSmil(Smil smil) {
    // get all source tracks
    List<TrackSection> trackGroups = new ArrayList<TrackSection>();
    // Find the track flavors, and find track groups that matches the flavors
    for (SmilMediaParamGroup paramGroup : smil.getHead().getParamGroups()) { // For each group look at elements
      TrackSection ts = null;
      List<String> src = new ArrayList<String>();
      for (SmilMediaParam param : paramGroup.getParams()) {
        if (SmilMediaParam.PARAM_NAME_TRACK_FLAVOR.matches(param.getName())) { // Is a flavor
          ts = new TrackSection(paramGroup.getId(), param.getValue());
          trackGroups.add(ts);
        }
        if (SmilMediaParam.PARAM_NAME_TRACK_SRC.matches(param.getName())) { // Is a track
          src.add(param.getValue());
        }
      }
      if (ts != null)
        ts.setSmilTrackList(src);
    }
    return trackGroups;
  }

  /**
   * This class is used to store context information for the jobs.
   */
  private static final class JobInformation {

    private final List<EncodingProfile> profiles;
    private final List<Track> tracks;
    private String grp = null;
    private MediaPackageElementFlavor flavor = null;
    private List<String> tags = null;
    private boolean tagProfile;

    JobInformation(String paramgroup, List<Track> tracks, List<EncodingProfile> profiles, List<String> tags,
            MediaPackageElementFlavor flavor, boolean tagWithProfile) {
      this.tracks = tracks;
      this.grp = paramgroup;
      this.profiles = profiles;
      this.tags = tags;
      this.flavor = flavor;
      this.tagProfile = tagWithProfile;
    }

    public List<Track> getTracks() {
      return tracks;
    }

    public MediaPackageElementFlavor getFlavor() {
      return flavor;
    }

    public List<String> getTags() {
      return tags;
    }

    public boolean getTagProfile() {
      return this.tagProfile;
    }

    @SuppressWarnings("unused")
    public String getGroups() {
      return grp;
    }

    @SuppressWarnings("unused")
    public List<EncodingProfile> getProfiles() {
      return profiles;
    }

  }

}
