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

package org.opencastproject.composer.impl;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.serviceregistry.api.Incidents.NO_DETAILS;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.api.VideoClip;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.composer.layout.Layout;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.fun.juc.Mutables;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/** FFMPEG based implementation of the composer service api. */
public class ComposerServiceImpl extends AbstractJobProducer implements ComposerService, ManagedService {
  /**
   * The indexes the composite job uses to create a Job
   */
  private static final int BACKGROUND_COLOR_INDEX = 6;
  private static final int COMPOSITE_TRACK_SIZE_INDEX = 5;
  private static final int LOWER_TRACK_INDEX = 1;
  private static final int LOWER_TRACK_LAYOUT_INDEX = 2;
  private static final int PROFILE_ID_INDEX = 0;
  private static final int UPPER_TRACK_INDEX = 3;
  private static final int UPPER_TRACK_LAYOUT_INDEX = 4;
  private static final int WATERMARK_INDEX = 7;
  private static final int WATERMARK_LAYOUT_INDEX = 8;
  /**
   * Error codes
   */
  private static final int WORKSPACE_GET_IO_EXCEPTION = 1;
  private static final int WORKSPACE_GET_NOT_FOUND = 2;
  private static final int WORKSPACE_PUT_COLLECTION_IO_EXCEPTION = 3;
  private static final int PROFILE_NOT_FOUND = 4;
  private static final int ENCODING_FAILED = 7;
  private static final int TRIMMING_FAILED = 8;
  private static final int COMPOSITE_FAILED = 9;
  private static final int CONCAT_FAILED = 10;
  private static final int CONCAT_LESS_TRACKS = 11;
  private static final int CONCAT_NO_DIMENSION = 12;
  private static final int IMAGE_TO_VIDEO_FAILED = 13;
  private static final int CONVERT_IMAGE_FAILED = 14;
  private static final int IMAGE_EXTRACTION_FAILED = 15;
  private static final int IMAGE_EXTRACTION_UNKNOWN_DURATION = 16;
  private static final int IMAGE_EXTRACTION_TIME_OUTSIDE_DURATION = 17;
  private static final int IMAGE_EXTRACTION_NO_VIDEO = 18;
  private static final int PROCESS_SMIL_FAILED = 19;
  private static final int MULTI_ENCODE_FAILED = 20;
  private static final int NO_STREAMS = 23;

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** Default location of the ffmepg binary (resembling the installer) */
  private static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Configuration for the FFmpeg binary */
  private static final String CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpeg.path";

  /** The collection name */
  private static final String COLLECTION = "composer";

  /** Used to mark a track unavailable to composite. */
  private static final String NOT_AVAILABLE = "n/a";

  /** The formatter for load values */
  private static final DecimalFormat df = new DecimalFormat("#.#");

  /** Configuration for process-smil transition duration */
  public static final String PROCESS_SMIL_CLIP_TRANSITION_DURATION = "org.composer.process_smil.edit.transition.duration";

  /** default transition duration for process_smil in seconds */
  public static final float DEFAULT_PROCESS_SMIL_CLIP_TRANSITION_DURATION = 2.0f;

  /** The maximum job load allowed for operations that use multiple profile (ProcessSmil, MultiEncode) */
  public static final float DEFAULT_JOB_LOAD_MAX_MULTIPLE_PROFILES = 0.8f;
  /** The default factor used to multiply the sum of encoding profiles load job for ProcessSmil */
  public static final float DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR = 0.5f;
  public static final float DEFAULT_MULTI_ENCODE_JOB_LOAD_FACTOR = 0.5f;

  public static final String JOB_LOAD_MAX_MULTIPLE_PROFILES = "job.load.max.multiple.profiles";
  public static final String JOB_LOAD_FACTOR_PROCESS_SMIL = "job.load.factor.process.smil";

  private float maxMultipleProfilesJobLoad = DEFAULT_JOB_LOAD_MAX_MULTIPLE_PROFILES;
  private float processSmilJobLoadFactor = DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR;
  private float multiEncodeJobLoadFactor = DEFAULT_MULTI_ENCODE_JOB_LOAD_FACTOR;

  /** default transition */
  private int transitionDuration = (int) (DEFAULT_PROCESS_SMIL_CLIP_TRANSITION_DURATION * 1000);

  /** List of available operations on jobs */
  enum Operation {
    Encode, Image, ImageConversion, Mux, Trim, Composite, Concat, ImageToVideo, ParallelEncode, Demux, ProcessSmil, MultiEncode
  }

  /** tracked encoder engines */
  private Set<EncoderEngine> activeEncoder = new HashSet<>();

  /** Encoding profile manager */
  private EncodingProfileScanner profileScanner = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService = null;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /** The security service */
  private SecurityService securityService = null;

  /** SmilService for process SMIL */
  private SmilService smilService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService = null;

  /** Path to the FFmpeg binary */
  private String ffmpegBinary = FFMPEG_BINARY_DEFAULT;

  /** Creates a new composer service instance. */
  public ComposerServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * OSGi callback on component activation.
   *
   * @param cc
   *          the component context
   */
  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    ffmpegBinary = StringUtils.defaultString(cc.getBundleContext().getProperty(CONFIG_FFMPEG_PATH),
            FFMPEG_BINARY_DEFAULT);
    logger.debug("ffmpeg binary: {}", ffmpegBinary);
    logger.info("Activating composer service");
  }

  /**
   * OSGi callback on component deactivation.
   */
  public void deactivate() {
    logger.info("Deactivating composer service");
    for (EncoderEngine engine: activeEncoder) {
      engine.close();
    }
    logger.debug("Closed encoder engine factory");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  @Override
  public Job encode(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException {
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Encode.toString(),
              Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack)), profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Load track into workspace and return a file handler, filing an incident if something went wrong.
   *
   * @param job The job in which context this operation is executed
   * @param name Name of the track to load into the workspace
   * @param track Track to load into the workspace
   * @return File handler for track
   * @throws EncoderException Could not load file into workspace
   */
  private File loadTrackIntoWorkspace(final Job job, final String name, final Track track) throws EncoderException {
    try {
      return workspace.get(track.getURI());
    } catch (NotFoundException e) {
      incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
              getWorkspaceMediapackageParams(name, track), NO_DETAILS);
      throw new EncoderException(format("%s track %s not found", name, track));
    } catch (IOException e) {
      incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
              getWorkspaceMediapackageParams(name, track), NO_DETAILS);
      throw new EncoderException(format("Unable to access %s track %s", name, track));
    }
  }

  /**
   * Load URI into workspace by URI and return a file handler, filing an incident if something went wrong.
   *
   * @param job
   *          The job in which context this operation is executed
   * @param name
   *          Name of the track to load into the workspace
   * @param uri
   *          URI of Track to load into the workspace
   * @return File handler for track
   * @throws EncoderException
   *           Could not load file into workspace
   */
  private File loadURIIntoWorkspace(final Job job, final String name, final URI uri) throws EncoderException {
    try {
      return workspace.get(uri);
    } catch (NotFoundException e) {
      incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e, getWorkspaceCollectionParams(name, name, uri),
              NO_DETAILS);
      throw new EncoderException(String.format("%s uri %s not found", name, uri));
    } catch (IOException e) {
      incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e, getWorkspaceCollectionParams(name, name, uri),
              NO_DETAILS);
      throw new EncoderException(String.format("Unable to access %s uri %s", name, uri));
    }
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   *
   * @param tracks
   *          tracks to use for processing
   * @param profileId
   *          the encoding profile
   * @param properties
   *          encoding properties
   * @return the encoded track or none if the operation does not return a track. This may happen for example when doing
   *         two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if encoding fails
   */
  private Option<Track> encode(final Job job, Map<String, Track> tracks, String profileId)
          throws EncoderException, MediaPackageException {

    final String targetTrackId = idBuilder.createNew().toString();

    Map<String, File> files = new HashMap<>();
    // Get the tracks and make sure they exist
    for (Entry<String, Track> track: tracks.entrySet()) {
      files.put(track.getKey(), loadTrackIntoWorkspace(job, track.getKey(), track.getValue()));
    }

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    List <String> trackMsg = new LinkedList<>();
    for (Entry<String, Track> track: tracks.entrySet()) {
      trackMsg.add(format("%s: %s", track.getKey(), track.getValue().getIdentifier()));
    }
    logger.info("Encoding {} into {} using profile {}", StringUtils.join(trackMsg, ", "), targetTrackId, profileId);

    // Do the work
    final EncoderEngine encoder = getEncoderEngine();
    List<File> output;
    try {
      output = encoder.process(files, profile, null);
    } catch (EncoderException e) {
      Map<String, String> params = new HashMap<>();
      for (Entry<String, Track> track: tracks.entrySet()) {
        params.put(track.getKey(), track.getValue().getIdentifier());
      }
      params.put("profile", profile.getIdentifier());
      params.put("properties", "EMPTY");
      incident().recordFailure(job, ENCODING_FAILED, e, params, detailsFor(e, encoder));
      throw e;
    } finally {
      activeEncoder.remove(encoder);
    }

    // We expect zero or one file as output
    if (output.size() == 0) {
      return none();
    } else if (output.size() != 1) {
      // Ensure we do not leave behind old files in the workspace
      for (File file : output) {
        FileUtils.deleteQuietly(file);
      }
      throw new EncoderException("Composite does not support multiple files as output");
    }

    // Put the file in the workspace
    URI workspaceURI = putToCollection(job, output.get(0), "encoded file");

    // Have the encoded track inspected and return the result
    Track inspectedTrack = inspect(job, workspaceURI);
    inspectedTrack.setIdentifier(targetTrackId);

    return some(inspectedTrack);
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   *
   * @param job
   *          Job in which context the encoding is done
   * @param mediaTrack
   *          Source track
   * @param profileId
   *          the encoding profile
   * @return the encoded track or none if the operation does not return a track. This may happen for example when doing
   *         two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if encoding fails
   */
  private List <Track> parallelEncode(Job job, Track mediaTrack, String profileId)
          throws EncoderException, MediaPackageException {
    if (job == null) {
      throw new EncoderException("The Job parameter must not be null");
    }
    // Get the tracks and make sure they exist
    final File mediaFile = loadTrackIntoWorkspace(job, "source", mediaTrack);

    // Create the engine
    final EncodingProfile profile = getProfile(profileId);
    final EncoderEngine encoderEngine = getEncoderEngine();

    // List of encoded tracks
    LinkedList<Track> encodedTracks = new LinkedList<>();
    // Do the work
    int i = 0;
    Map<String, File> source = new HashMap<>();
    source.put("video", mediaFile);
    List<File> outputFiles = encoderEngine.process(source, profile, null);
    activeEncoder.remove(encoderEngine);
    for (File encodingOutput: outputFiles) {
      // Put the file in the workspace
      URI returnURL;
      final String targetTrackId = idBuilder.createNew().toString();

      try (InputStream in = new FileInputStream(encodingOutput)) {
        returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "-" + i + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
        logger.info("Copied the encoded file to the workspace at {}", returnURL);
        if (encodingOutput.delete()) {
          logger.info("Deleted the local copy of the encoded file at {}", encodingOutput.getAbsolutePath());
        } else {
          logger.warn("Unable to delete the encoding output at {}", encodingOutput);
        }
      } catch (Exception e) {
        throw new EncoderException("Unable to put the encoded file into the workspace", e);
      }

      // Have the encoded track inspected and return the result
      Track inspectedTrack = inspect(job, returnURL);
      inspectedTrack.setIdentifier(targetTrackId);

      List<String> tags = profile.getTags();
      for (String tag : tags) {
        if (encodingOutput.getName().endsWith(profile.getSuffix(tag)))
          inspectedTrack.addTag(tag);
      }

      encodedTracks.add(inspectedTrack);
      i++;
    }

    return encodedTracks;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  @Override
  public Job parallelEncode(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException {
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      logger.info("Starting parallel encode with profile {} with job load {}", profileId, df.format(profile.getJobLoad()));
      return serviceRegistry.createJob(JOB_TYPE, Operation.ParallelEncode.toString(),
              Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack)), profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#trim(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, long)
   */
  @Override
  public Job trim(final Track sourceTrack, final String profileId, final long start, final long duration)
          throws EncoderException, MediaPackageException {
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Trim.toString(),
              Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack), Long.toString(start),
                      Long.toString(duration)), profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Trims the given track using the encoding profile <code>profileId</code> and the given starting point and duration
   * in miliseconds.
   *
   * @param job
   *          the associated job
   * @param sourceTrack
   *          the source track
   * @param profileId
   *          the encoding profile identifier
   * @param start
   *          the trimming in-point in millis
   * @param duration
   *          the trimming duration in millis
   * @return the trimmed track or none if the operation does not return a track. This may happen for example when doing
   *         two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if trimming fails
   */
  private Option<Track> trim(Job job, Track sourceTrack, String profileId, long start, long duration)
          throws EncoderException {
    String targetTrackId = idBuilder.createNew().toString();

    // Get the track and make sure it exists
    final File trackFile = loadTrackIntoWorkspace(job, "source", sourceTrack);

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    // Create the engine
    final EncoderEngine encoderEngine = getEncoderEngine();

    File output;
    try {
      output = encoderEngine.trim(trackFile, profile, start, duration, null);
    } catch (EncoderException e) {
      Map<String, String> params = new HashMap<>();
      params.put("track", sourceTrack.getURI().toString());
      params.put("profile", profile.getIdentifier());
      params.put("start", Long.toString(start));
      params.put("duration", Long.toString(duration));
      incident().recordFailure(job, TRIMMING_FAILED, e, params, detailsFor(e, encoderEngine));
      throw e;
    } finally {
      activeEncoder.remove(encoderEngine);
    }

    // trim did not return a file
    if (!output.exists() || output.length() == 0)
      return none();

    // Put the file in the workspace
    URI workspaceURI = putToCollection(job, output, "trimmed file");

    // Have the encoded track inspected and return the result
    Track inspectedTrack = inspect(job, workspaceURI);
    inspectedTrack.setIdentifier(targetTrackId);
    return some(inspectedTrack);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String)
   */
  @Override
  public Job mux(Track videoTrack, Track audioTrack, String profileId) throws EncoderException, MediaPackageException {
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Mux.toString(),
              Arrays.asList(profileId, MediaPackageElementParser.getAsXml(videoTrack),
                      MediaPackageElementParser.getAsXml(audioTrack)), profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Muxes the audio and video track into one movie container.
   *
   * @param job
   *          the associated job
   * @param videoTrack
   *          the video track
   * @param audioTrack
   *          the audio track
   * @param profileId
   *          the profile identifier
   * @return the muxed track
   * @throws EncoderException
   *           if encoding fails
   * @throws MediaPackageException
   *           if serializing the mediapackage elements fails
   */
  private Option<Track> mux(Job job, Track videoTrack, Track audioTrack, String profileId) throws EncoderException,
          MediaPackageException {
    return encode(job, Collections.map(tuple("audio", audioTrack), tuple("video", videoTrack)), profileId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Job composite(Dimension compositeTrackSize, Option<LaidOutElement<Track>> upperTrack,
          LaidOutElement<Track> lowerTrack, Option<LaidOutElement<Attachment>> watermark, String profileId,
          String background) throws EncoderException, MediaPackageException {
    List<String> arguments = new ArrayList<>(9);
    arguments.add(PROFILE_ID_INDEX, profileId);
    arguments.add(LOWER_TRACK_INDEX, MediaPackageElementParser.getAsXml(lowerTrack.getElement()));
    arguments.add(LOWER_TRACK_LAYOUT_INDEX, Serializer.json(lowerTrack.getLayout()).toJson());
    if (upperTrack.isNone()) {
      arguments.add(UPPER_TRACK_INDEX, NOT_AVAILABLE);
      arguments.add(UPPER_TRACK_LAYOUT_INDEX, NOT_AVAILABLE);
    } else {
      arguments.add(UPPER_TRACK_INDEX, MediaPackageElementParser.getAsXml(upperTrack.get().getElement()));
      arguments.add(UPPER_TRACK_LAYOUT_INDEX, Serializer.json(upperTrack.get().getLayout()).toJson());
    }
    arguments.add(COMPOSITE_TRACK_SIZE_INDEX, Serializer.json(compositeTrackSize).toJson());
    arguments.add(BACKGROUND_COLOR_INDEX, background);
    if (watermark.isSome()) {
      LaidOutElement<Attachment> watermarkLaidOutElement = watermark.get();
      arguments.add(WATERMARK_INDEX, MediaPackageElementParser.getAsXml(watermarkLaidOutElement.getElement()));
      arguments.add(WATERMARK_LAYOUT_INDEX, Serializer.json(watermarkLaidOutElement.getLayout()).toJson());
    }
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Composite.toString(), arguments, profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create composite job", e);
    }
  }

  private Option<Track> composite(Job job, Dimension compositeTrackSize, LaidOutElement<Track> lowerLaidOutElement,
          Option<LaidOutElement<Track>> upperLaidOutElement, Option<LaidOutElement<Attachment>> watermarkOption,
          String profileId, String backgroundColor) throws EncoderException, MediaPackageException {

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    // Create the engine
    final EncoderEngine encoderEngine = getEncoderEngine();

    final String targetTrackId = idBuilder.createNew().toString();
    Option<File> upperVideoFile = Option.none();
    try {
      // Get the tracks and make sure they exist
      final File lowerVideoFile = loadTrackIntoWorkspace(job, "lower video", lowerLaidOutElement.getElement());

      if (upperLaidOutElement.isSome()) {
        upperVideoFile = Option.option(
                loadTrackIntoWorkspace(job, "upper video", upperLaidOutElement.get().getElement()));
      }
      File watermarkFile = null;
      if (watermarkOption.isSome()) {
        try {
          watermarkFile = workspace.get(watermarkOption.get().getElement().getURI());
        } catch (NotFoundException e) {
          incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                  getWorkspaceMediapackageParams("watermark image", watermarkOption.get().getElement()),
                  NO_DETAILS);
          throw new EncoderException("Requested watermark image " + watermarkOption.get().getElement()
                  + " is not found");
        } catch (IOException e) {
          incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                  getWorkspaceMediapackageParams("watermark image", watermarkOption.get().getElement()),
                  NO_DETAILS);
          throw new EncoderException("Unable to access right watermark image " + watermarkOption.get().getElement());
        }
        if (upperLaidOutElement.isSome()) {
          logger.info("Composing lower video track {} {} and upper video track {} {} including watermark {} {} into {}",
                  lowerLaidOutElement.getElement().getIdentifier(), lowerLaidOutElement.getElement().getURI(),
                  upperLaidOutElement.get().getElement().getIdentifier(), upperLaidOutElement.get().getElement().getURI(),
                  watermarkOption.get().getElement().getIdentifier(), watermarkOption.get().getElement().getURI(),
                  targetTrackId);
        } else {
          logger.info("Composing video track {} {} including watermark {} {} into {}",
                  lowerLaidOutElement.getElement().getIdentifier(), lowerLaidOutElement.getElement().getURI(),
                  watermarkOption.get().getElement().getIdentifier(), watermarkOption.get().getElement().getURI(),
                  targetTrackId);
        }
      } else {
        if (upperLaidOutElement.isSome()) {
          logger.info("Composing lower video track {} {} and upper video track {} {} into {}",
                  lowerLaidOutElement.getElement().getIdentifier(), lowerLaidOutElement.getElement().getURI(),
                  upperLaidOutElement.get().getElement().getIdentifier(), upperLaidOutElement.get().getElement().getURI(),
                  targetTrackId);
        } else {
          logger.info("Composing video track {} {} into {}", lowerLaidOutElement.getElement().getIdentifier(),
                  lowerLaidOutElement.getElement().getURI(), targetTrackId);
        }
      }

      // Creating video filter command
      final String compositeCommand = buildCompositeCommand(compositeTrackSize, lowerLaidOutElement,
              upperLaidOutElement, upperVideoFile, watermarkOption, watermarkFile, backgroundColor);

      Map<String, String> properties = new HashMap<>();
      properties.put(EncoderEngine.CMD_SUFFIX + ".compositeCommand", compositeCommand);
      List<File> output;
      try {
        Map<String, File> source = new HashMap<>();
        if (upperVideoFile.isSome()) {
          source.put("audio", upperVideoFile.get());
        }
        source.put("video", lowerVideoFile);
        output = encoderEngine.process(source, profile, properties);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<>();
        if (upperLaidOutElement.isSome()) {
          params.put("upper", upperLaidOutElement.get().getElement().getURI().toString());
        }
        params.put("lower", lowerLaidOutElement.getElement().getURI().toString());
        if (watermarkFile != null)
          params.put("watermark", watermarkOption.get().getElement().getURI().toString());
        params.put("profile", profile.getIdentifier());
        params.put("properties", properties.toString());
        incident().recordFailure(job, COMPOSITE_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      } finally {
        activeEncoder.remove(encoderEngine);
      }

      // We expect one file as output
      if (output.size() != 1) {
        // Ensure we do not leave behind old files in the workspace
        for (File file : output) {
          FileUtils.deleteQuietly(file);
        }
        throw new EncoderException("Composite does not support multiple files as output");
      }


      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(0), "compound file");

      // Have the compound track inspected and return the result

      Track inspectedTrack = inspect(job, workspaceURI);
      inspectedTrack.setIdentifier(targetTrackId);

      return some(inspectedTrack);
    } catch (Exception e) {
      if (upperLaidOutElement.isSome()) {
        logger.warn("Error composing {}  and {}: {}", lowerLaidOutElement.getElement(), upperLaidOutElement.get().getElement(),
                getStackTrace(e));
      } else {
        logger.warn("Error composing {}: {}", lowerLaidOutElement.getElement(), getStackTrace(e));
      }
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  @Override
  public Job concat(String profileId, Dimension outputDimension, boolean sameCodec, Track... tracks) throws EncoderException,
          MediaPackageException {
    return concat(profileId, outputDimension, -1.0f, sameCodec, tracks);
  }

  @Override
  public Job concat(String profileId, Dimension outputDimension, float outputFrameRate, boolean sameCodec, Track... tracks) throws EncoderException,
          MediaPackageException {
    ArrayList<String> arguments = new ArrayList<String>();
    arguments.add(0, profileId);
    if (outputDimension != null) {
      arguments.add(1, Serializer.json(outputDimension).toJson());
    } else {
      arguments.add(1, "");
    }
    arguments.add(2, String.format(Locale.US, "%f", outputFrameRate));
    arguments.add(3, Boolean.toString(sameCodec));
    for (int i = 0; i < tracks.length; i++) {
      arguments.add(i + 4, MediaPackageElementParser.getAsXml(tracks[i]));
    }
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Concat.toString(), arguments, profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create concat job", e);
    }
  }

  private Option<Track> concat(Job job, List<Track> tracks, String profileId, Dimension outputDimension,
          float outputFrameRate, boolean sameCodec)
          throws EncoderException, MediaPackageException {

    if (tracks.size() < 2) {
      Map<String, String> params = new HashMap<>();
      params.put("tracks-size", Integer.toString(tracks.size()));
      params.put("tracks", StringUtils.join(tracks, ","));
      incident().recordFailure(job, CONCAT_LESS_TRACKS, params);
      throw new EncoderException("The track parameter must at least have two tracks present");
    }

    boolean onlyAudio = true;
    for (Track t : tracks) {
      if (t.hasVideo()) {
        onlyAudio = false;
        break;
      }
    }

    if (!sameCodec && !onlyAudio && outputDimension == null) {
      Map<String, String> params = new HashMap<>();
      params.put("tracks", StringUtils.join(tracks, ","));
      incident().recordFailure(job, CONCAT_NO_DIMENSION, params);
      throw new EncoderException("The output dimension id parameter must not be null when concatenating video");
    }

    final String targetTrackId = idBuilder.createNew().toString();
    // Get the tracks and make sure they exist
    List<File> trackFiles = new ArrayList<>();
    int i = 0;
    for (Track track : tracks) {
      if (!track.hasAudio() && !track.hasVideo()) {
        Map<String, String> params = new HashMap<>();
        params.put("track-id", track.getIdentifier());
        params.put("track-url", track.getURI().toString());
        incident().recordFailure(job, NO_STREAMS, params);
        throw new EncoderException("Track has no audio or video stream available: " + track);
      }
      trackFiles.add(i++, loadTrackIntoWorkspace(job, "concat", track));
    }

    // Create the engine
    final EncoderEngine encoderEngine = getEncoderEngine();

    if (onlyAudio) {
      logger.info("Concatenating audio tracks {} into {}", trackFiles, targetTrackId);
    } else {
      logger.info("Concatenating video tracks {} into {}", trackFiles, targetTrackId);
    }

    // Get the encoding profile
    EncodingProfile profile = getProfile(job, profileId);
    String concatCommand;
    File fileList = null;
    // Creating video filter command for concat
    if (sameCodec) {
      // create file list to use Concat demuxer - lossless - pack contents into a single container
      fileList = new File(workspace.rootDirectory(), "concat_tracklist_" + job.getId() + ".txt");
      fileList.deleteOnExit();
      try (PrintWriter printer = new PrintWriter(new FileWriter(fileList, true))) {
        for (Track track : tracks) {
          printer.append("file '").append(workspace.get(track.getURI()).getAbsolutePath()).append("'\n");
        }
      } catch (IOException e) {
        throw new EncoderException("Cannot create file list for concat", e);
      } catch (NotFoundException e) {
        throw new EncoderException("Cannot find track filename in workspace for concat", e);
      }
      concatCommand = "-f concat -safe 0 -i " + fileList.getAbsolutePath();
    } else {
      concatCommand = buildConcatCommand(onlyAudio, outputDimension, outputFrameRate, trackFiles, tracks);
    }

    Map<String, String> properties = new HashMap<>();
    properties.put(EncoderEngine.CMD_SUFFIX + ".concatCommand", concatCommand);

    File output;
    try {
      output = encoderEngine.encode(trackFiles.get(0), profile, properties);
    } catch (EncoderException e) {
      Map<String, String> params = new HashMap<>();
      List<String> trackList = new ArrayList<>();
      for (Track t : tracks) {
        trackList.add(t.getURI().toString());
      }
      params.put("tracks", StringUtils.join(trackList, ","));
      params.put("profile", profile.getIdentifier());
      params.put("properties", properties.toString());
      incident().recordFailure(job, CONCAT_FAILED, e, params, detailsFor(e, encoderEngine));
      throw e;
    } finally {
      activeEncoder.remove(encoderEngine);
      if (fileList != null) {
        FileSupport.deleteQuietly(fileList);
      }
    }

    // concat did not return a file
    if (!output.exists() || output.length() == 0)
      return none();

    // Put the file in the workspace
    URI workspaceURI = putToCollection(job, output, "concatenated file");

    // Have the concat track inspected and return the result
    Track inspectedTrack = inspect(job, workspaceURI);
    inspectedTrack.setIdentifier(targetTrackId);

    return some(inspectedTrack);
  }

  @Override
  public Job imageToVideo(Attachment sourceImageAttachment, String profileId, double time) throws EncoderException,
          MediaPackageException {
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.ImageToVideo.toString(), Arrays.asList(
              profileId, MediaPackageElementParser.getAsXml(sourceImageAttachment), Double.toString(time)),
              profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create image to video job", e);
    }
  }

  private Option<Track> imageToVideo(Job job, Attachment sourceImage, String profileId, Double time)
          throws EncoderException, MediaPackageException {

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    final String targetTrackId = idBuilder.createNew().toString();
    // Get the attachment and make sure it exist
    File imageFile;
    try {
      imageFile = workspace.get(sourceImage.getURI());
    } catch (NotFoundException e) {
      incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
              getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS);
      throw new EncoderException("Requested source image " + sourceImage + " is not found");
    } catch (IOException e) {
      incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
              getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS);
      throw new EncoderException("Unable to access source image " + sourceImage);
    }

    // Create the engine
    final EncoderEngine encoderEngine = getEncoderEngine();

    logger.info("Converting image attachment {} into video {}", sourceImage.getIdentifier(), targetTrackId);

    Map<String, String> properties = new HashMap<>();
    if (time == -1)
      time = 0D;

    DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
    ffmpegFormat.setDecimalSeparator('.');
    DecimalFormat df = new DecimalFormat("0.000", ffmpegFormat);
    properties.put("time", df.format(time));

    File output;
    try {
      output = encoderEngine.encode(imageFile, profile, properties);
    } catch (EncoderException e) {
      Map<String, String> params = new HashMap<>();
      params.put("image", sourceImage.getURI().toString());
      params.put("profile", profile.getIdentifier());
      params.put("properties", properties.toString());
      incident().recordFailure(job, IMAGE_TO_VIDEO_FAILED, e, params, detailsFor(e, encoderEngine));
      throw e;
    } finally {
      activeEncoder.remove(encoderEngine);
    }

    // encoding did not return a file
    if (!output.exists() || output.length() == 0)
      return none();

    // Put the file in the workspace
    URI workspaceURI = putToCollection(job, output, "converted image file");

    // Have the compound track inspected and return the result
    Track inspectedTrack = inspect(job, workspaceURI);
    inspectedTrack.setIdentifier(targetTrackId);

    return some(inspectedTrack);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#image(Track, String, double...)
   */
  @Override
  public Job image(Track sourceTrack, String profileId, double... times) throws EncoderException, MediaPackageException {
    if (sourceTrack == null)
      throw new IllegalArgumentException("SourceTrack cannot be null");

    if (times.length == 0)
      throw new IllegalArgumentException("At least one time argument has to be specified");

    List<String> parameters = new ArrayList<>();
    parameters.add(profileId);
    parameters.add(MediaPackageElementParser.getAsXml(sourceTrack));
    parameters.add(Boolean.TRUE.toString());
    for (double time : times) {
      parameters.add(Double.toString(time));
    }

    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Image.toString(), parameters, profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  @Override
  public List<Attachment> imageSync(Track sourceTrack, String profileId, double... time) throws EncoderException,
      MediaPackageException {
    Job job = null;
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      job = serviceRegistry
          .createJob(
              JOB_TYPE, Operation.Image.toString(), null, null, false, profile.getJobLoad());
      job.setStatus(Job.Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      final List<Attachment> images = image(job, sourceTrack, profileId, time);
      job.setStatus(Job.Status.FINISHED);
      return images;
    } catch (ServiceRegistryException | NotFoundException e) {
      throw new EncoderException("Unable to create a job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  @Override
  public Job image(Track sourceTrack, String profileId, Map<String, String> properties) throws EncoderException,
          MediaPackageException {
    if (sourceTrack == null)
      throw new IllegalArgumentException("SourceTrack cannot be null");

    List<String> arguments = new ArrayList<String>();
    arguments.add(profileId);
    arguments.add(MediaPackageElementParser.getAsXml(sourceTrack));
    arguments.add(Boolean.FALSE.toString());
    arguments.add(getPropertiesAsString(properties));

    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Image.toString(), arguments, profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Extracts an image from <code>sourceTrack</code> at the given point in time.
   *
   * @param job
   *          the associated job
   * @param sourceTrack
   *          the source track
   * @param profileId
   *          the identifier of the encoding profile to use
   * @param times
   *          (one or more) times in seconds
   * @return the images as an attachment element list
   * @throws EncoderException
   *           if extracting the image fails
   */
  protected List<Attachment> image(Job job, Track sourceTrack, String profileId, double... times)
          throws EncoderException, MediaPackageException {
    if (sourceTrack == null)
      throw new EncoderException("SourceTrack cannot be null");

    validateVideoStream(job, sourceTrack);

    // The time should not be outside of the track's duration
    for (double time : times) {
      if (sourceTrack.getDuration() == null) {
        Map<String, String> params = new HashMap<>();
        params.put("track-id", sourceTrack.getIdentifier());
        params.put("track-url", sourceTrack.getURI().toString());
        incident().recordFailure(job, IMAGE_EXTRACTION_UNKNOWN_DURATION, params);
        throw new EncoderException("Unable to extract an image from a track with unknown duration");
      }
      if (time < 0 || time * 1000 > sourceTrack.getDuration()) {
        Map<String, String> params = new HashMap<>();
        params.put("track-id", sourceTrack.getIdentifier());
        params.put("track-url", sourceTrack.getURI().toString());
        params.put("track-duration", sourceTrack.getDuration().toString());
        params.put("time", Double.toString(time));
        incident().recordFailure(job, IMAGE_EXTRACTION_TIME_OUTSIDE_DURATION, params);
        throw new EncoderException("Can not extract an image at time " + time + " from a track with duration "
                + sourceTrack.getDuration());
      }
    }

    return extractImages(job, sourceTrack, profileId, null, times);
  }

  /**
   * Extracts an image from <code>sourceTrack</code> by the given properties and the corresponding encoding profile.
   *
   * @param job
   *          the associated job
   * @param sourceTrack
   *          the source track
   * @param profileId
   *          the identifier of the encoding profile to use
   * @param properties
   *          the properties applied to the encoding profile
   * @return the images as an attachment element list
   * @throws EncoderException
   *           if extracting the image fails
   */
  protected List<Attachment> image(Job job, Track sourceTrack, String profileId, Map<String, String> properties)
          throws EncoderException, MediaPackageException {
    if (sourceTrack == null)
      throw new EncoderException("SourceTrack cannot be null");

    validateVideoStream(job, sourceTrack);

    return extractImages(job, sourceTrack, profileId, properties);
  }

  private List<Attachment> extractImages(Job job, Track sourceTrack, String profileId, Map<String, String> properties,
          double... times) throws EncoderException {
    logger.info("creating an image using video track {}", sourceTrack.getIdentifier());

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    // Create the encoding engine
    final EncoderEngine encoderEngine = getEncoderEngine();

    // Finally get the file that needs to be encoded
    File videoFile = loadTrackIntoWorkspace(job, "video", sourceTrack);

    // Do the work
    List<File> encodingOutput;
    try {
      encodingOutput = encoderEngine.extract(videoFile, profile, properties, times);
      // check for validity of output
      if (encodingOutput == null || encodingOutput.isEmpty()) {
        logger.error("Image extraction from video {} with profile {} failed: no images were produced",
                sourceTrack.getURI(), profile.getIdentifier());
        throw new EncoderException("Image extraction failed: no images were produced");
      }
    } catch (EncoderException e) {
      Map<String, String> params = new HashMap<>();
      params.put("video", sourceTrack.getURI().toString());
      params.put("profile", profile.getIdentifier());
      params.put("positions", Arrays.toString(times));
      incident().recordFailure(job, IMAGE_EXTRACTION_FAILED, e, params, detailsFor(e, encoderEngine));
      throw e;
    } finally {
      activeEncoder.remove(encoderEngine);
    }

    int i = 0;
    List<URI> workspaceURIs = new LinkedList<>();
    for (File output : encodingOutput) {

      if (!output.exists() || output.length() == 0) {
        logger.warn("Extracted image {} is empty!", output);
        throw new EncoderException("Extracted image " + output.toString() + " is empty!");
      }

      // Put the file in the workspace

      try (InputStream in = new FileInputStream(output)) {
        URI returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "_" + i++ + "." + FilenameUtils.getExtension(output.getAbsolutePath()), in);
        logger.debug("Copied image file to the workspace at {}", returnURL);
        workspaceURIs.add(returnURL);
      } catch (Exception e) {
        cleanup(encodingOutput.toArray(new File[encodingOutput.size()]));
        cleanupWorkspace(workspaceURIs.toArray(new URI[workspaceURIs.size()]));
        incident().recordFailure(job, WORKSPACE_PUT_COLLECTION_IO_EXCEPTION, e,
                getWorkspaceCollectionParams("extracted image file", COLLECTION, output.toURI()), NO_DETAILS);
        throw new EncoderException("Unable to put image file into the workspace", e);
      }
    }

    // cleanup
    cleanup(encodingOutput.toArray(new File[encodingOutput.size()]));

    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    List<Attachment> imageAttachments = new LinkedList<Attachment>();
    for (URI url : workspaceURIs) {
      Attachment attachment = (Attachment) builder.elementFromURI(url, Attachment.TYPE, null);
      imageAttachments.add(attachment);
    }

    return imageAttachments;
  }

  private void validateVideoStream(Job job, Track sourceTrack) throws EncoderException {
    // make sure there is a video stream in the track
    if (sourceTrack != null && !sourceTrack.hasVideo()) {
      Map<String, String> params = new HashMap<>();
      params.put("track-id", sourceTrack.getIdentifier());
      params.put("track-url", sourceTrack.getURI().toString());
      incident().recordFailure(job, IMAGE_EXTRACTION_NO_VIDEO, params);
      throw new EncoderException("Cannot extract an image without a video stream");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#convertImage(org.opencastproject.mediapackage.Attachment,
   *      java.lang.String)
   */
  @Override
  public Job convertImage(Attachment image, String profileId) throws EncoderException, MediaPackageException {
    if (image == null)
      throw new IllegalArgumentException("Source image cannot be null");

    String[] parameters = new String[2];
    parameters[0] = profileId;
    parameters[1] = MediaPackageElementParser.getAsXml(image);

    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.ImageConversion.toString(), Arrays.asList(parameters),
              profile.getJobLoad());
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  @Override
  public Attachment convertImageSync(Attachment image, String profileId) throws EncoderException, MediaPackageException {
    Job job = null;
    try {
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      job = serviceRegistry
          .createJob(
              JOB_TYPE, Operation.Image.toString(), null, null, false, profile.getJobLoad());
      job.setStatus(Job.Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      Option<Attachment> result = convertImage(job, image, profileId);
      job.setStatus(Job.Status.FINISHED);
      return result.getOrElseNull();
    } catch (ServiceRegistryException | NotFoundException e) {
      throw new EncoderException("Unable to create a job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * Converts an image from <code>sourceImage</code> to a new format.
   *
   * @param job
   *          the associated job
   * @param sourceImage
   *          the source image
   * @param profileId
   *          the identifer of the encoding profile to use
   * @return the image as an attachment or none if the operation does not return an image. This may happen for example
   *         when doing two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if converting the image fails
   */
  private Option<Attachment> convertImage(Job job, Attachment sourceImage, String profileId) throws EncoderException,
          MediaPackageException {
    logger.info("Converting {}", sourceImage);

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    // Create the encoding engine
    final EncoderEngine encoderEngine = getEncoderEngine();

    // Finally get the file that needs to be encoded
    File imageFile;
    try {
      imageFile = workspace.get(sourceImage.getURI());
    } catch (NotFoundException e) {
      incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
              getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS);
      throw new EncoderException("Requested video track " + sourceImage + " was not found", e);
    } catch (IOException e) {
      incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
              getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS);
      throw new EncoderException("Error accessing video track " + sourceImage, e);
    }

    // Do the work
    File output;
    try {
      output = encoderEngine.encode(imageFile, profile, null);
    } catch (EncoderException e) {
      Map<String, String> params = new HashMap<>();
      params.put("image", sourceImage.getURI().toString());
      params.put("profile", profile.getIdentifier());
      incident().recordFailure(job, CONVERT_IMAGE_FAILED, e, params, detailsFor(e, encoderEngine));
      throw e;
    } finally {
      activeEncoder.remove(encoderEngine);
    }

    // encoding did not return a file
    if (!output.exists() || output.length() == 0)
      return none();

    // Put the file in the workspace
    URI workspaceURI = putToCollection(job, output, "converted image file");

    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    Attachment attachment = (Attachment) builder.elementFromURI(workspaceURI, Attachment.TYPE, null);

    return some(attachment);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws ServiceRegistryException {
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      Operation op = Operation.valueOf(operation);
      Track firstTrack;
      Track secondTrack;
      String encodingProfile = arguments.get(0);
      final String serialized;

      switch (op) {
        case Encode:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          serialized = encode(job, Collections.map(tuple("video", firstTrack)), encodingProfile).map(
                  MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case ParallelEncode:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          serialized = MediaPackageElementParser.getArrayAsXml(parallelEncode(job, firstTrack, encodingProfile));
          break;
        case Image:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          List<Attachment> resultingElements;
          if (Boolean.parseBoolean(arguments.get(2))) {
            double[] times = new double[arguments.size() - 3];
            for (int i = 3; i < arguments.size(); i++) {
              times[i - 3] = Double.parseDouble(arguments.get(i));
            }
            resultingElements = image(job, firstTrack, encodingProfile, times);
          } else {
            Map<String, String> properties = parseProperties(arguments.get(3));
            resultingElements = image(job, firstTrack, encodingProfile, properties);
          }
          serialized = MediaPackageElementParser.getArrayAsXml(resultingElements);
          break;
        case ImageConversion:
          Attachment sourceImage = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(1));
          serialized = convertImage(job, sourceImage, encodingProfile).map(
                  MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case Mux:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          secondTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(2));
          serialized = mux(job, firstTrack, secondTrack, encodingProfile).map(
                  MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case Trim:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          long start = Long.parseLong(arguments.get(2));
          long duration = Long.parseLong(arguments.get(3));
          serialized = trim(job, firstTrack, encodingProfile, start, duration).map(
                  MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case Composite:
          Attachment watermarkAttachment;
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(LOWER_TRACK_INDEX));
          Layout lowerLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(LOWER_TRACK_LAYOUT_INDEX)));
          LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<>(firstTrack, lowerLayout);
          Option<LaidOutElement<Track>> upperLaidOutElement = Option.none();
          if (NOT_AVAILABLE.equals(arguments.get(UPPER_TRACK_INDEX))
                  && NOT_AVAILABLE.equals(arguments.get(UPPER_TRACK_LAYOUT_INDEX))) {
            logger.trace("This composite action does not use a second track.");
          } else {
            secondTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(UPPER_TRACK_INDEX));
            Layout upperLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(UPPER_TRACK_LAYOUT_INDEX)));
            upperLaidOutElement = Option.option(new LaidOutElement<Track>(secondTrack, upperLayout));
          }
          Dimension compositeTrackSize = Serializer
                  .dimension(JsonObj.jsonObj(arguments.get(COMPOSITE_TRACK_SIZE_INDEX)));
          String backgroundColor = arguments.get(BACKGROUND_COLOR_INDEX);

          Option<LaidOutElement<Attachment>> watermarkOption = Option.none();
          if (arguments.size() == 9) {
            watermarkAttachment = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(WATERMARK_INDEX));
            Layout watermarkLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(WATERMARK_LAYOUT_INDEX)));
            watermarkOption = Option.some(new LaidOutElement<>(watermarkAttachment, watermarkLayout));
          }
          serialized = composite(job, compositeTrackSize, lowerLaidOutElement, upperLaidOutElement, watermarkOption,
                  encodingProfile, backgroundColor).map(MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case Concat:
          String dimensionString = arguments.get(1);
          String frameRateString = arguments.get(2);
          Dimension outputDimension = null;
          if (StringUtils.isNotBlank(dimensionString))
            outputDimension = Serializer.dimension(JsonObj.jsonObj(dimensionString));
          float outputFrameRate = NumberUtils.toFloat(frameRateString, -1.0f);
          boolean sameCodec = Boolean.parseBoolean(arguments.get(3));
          List<Track> tracks = new ArrayList<>();
          for (int i = 4; i < arguments.size(); i++) {
            tracks.add(i - 4, (Track) MediaPackageElementParser.getFromXml(arguments.get(i)));
          }
          serialized = concat(job, tracks, encodingProfile, outputDimension, outputFrameRate, sameCodec).map(
                  MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case ImageToVideo:
          Attachment image = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(1));
          double time = Double.parseDouble(arguments.get(2));
          serialized = imageToVideo(job, image, encodingProfile, time)
                  .map(MediaPackageElementParser.getAsXml()).getOrElse("");
          break;
        case Demux:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          List<Track> outTracks = demux(job, firstTrack, encodingProfile);
          serialized = StringUtils.trimToEmpty(MediaPackageElementParser.getArrayAsXml(outTracks));
          break;
        case ProcessSmil:
          Smil smil = this.smilService.fromXml(arguments.get(0)).getSmil(); // Pass the entire smil
          String trackParamGroupId = arguments.get(1); // Only process this track
          String mediaType = arguments.get(2); // v=video,a=audio,otherwise both
          List<String> encodingProfiles = arguments.subList(3, arguments.size());
          outTracks = processSmil(job, smil, trackParamGroupId, mediaType, encodingProfiles);
          serialized = StringUtils.trimToEmpty(MediaPackageElementParser.getArrayAsXml(outTracks));
          break;
        case MultiEncode:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          List<String> encodingProfiles2 = arguments.subList(1, arguments.size());
          outTracks = multiEncode(job, firstTrack, encodingProfiles2);
          serialized = StringUtils.trimToEmpty(MediaPackageElementParser.getArrayAsXml(outTracks));
          break;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }

      return serialized;
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException(format("Cannot handle operations of type '%s'", operation), e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException(format("Invalid arguments for operation '%s'", operation), e);
    } catch (Exception e) {
      throw new ServiceRegistryException(format("Error handling operation '%s'", operation), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  @Override
  public EncodingProfile[] listProfiles() {
    Collection<EncodingProfile> profiles = profileScanner.getProfiles().values();
    return profiles.toArray(new EncodingProfile[profiles.size()]);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    return profileScanner.getProfiles().get(profileId);
  }

  protected List<Track> inspect(Job job, List<URI> uris) throws EncoderException {
    // Start inspection jobs
    Job[] inspectionJobs = new Job[uris.size()];
    for (int i = 0; i < uris.size(); i++) {
      try {
        inspectionJobs[i] = inspectionService.inspect(uris.get(i));
      } catch (MediaInspectionException e) {
        incident().recordJobCreationIncident(job, e);
        throw new EncoderException(String.format("Media inspection of %s failed", uris.get(i)), e);
      }
    }

    // Wait for inspection jobs
    JobBarrier barrier = new JobBarrier(job, serviceRegistry, inspectionJobs);
    if (!barrier.waitForJobs().isSuccess()) {
      for (Map.Entry<Job, Job.Status> result : barrier.getStatus().getStatus().entrySet()) {
        if (result.getValue() != Job.Status.FINISHED) {
          logger.error("Media inspection failed in job {}: {}", result.getKey(), result.getValue());
        }
      }
      throw new EncoderException("Inspection of encoded file failed");
    }

    // De-serialize tracks
    List<Track> results = new ArrayList<>(uris.size());
    for (Job inspectionJob: inspectionJobs) {
      try {
        results.add((Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload()));
      } catch (MediaPackageException e) {
        throw new EncoderException(e);
      }
    }
    return results;
  }

  protected Track inspect(Job job, URI workspaceURI) throws EncoderException {
    return inspect(job, java.util.Collections.singletonList(workspaceURI)).get(0);
  }

  /**
   * Deletes any valid file in the list.
   *
   * @param encodingOutput
   *          list of files to be deleted
   */
  private void cleanup(File... encodingOutput) {
    for (File file : encodingOutput) {
      if (file != null && file.isFile()) {
        String path = file.getAbsolutePath();
        if (file.delete()) {
          logger.info("Deleted local copy of encoding file at {}", path);
        } else {
          logger.warn("Could not delete local copy of encoding file at {}", path);
        }
      }
    }
  }

  private void cleanupWorkspace(URI... workspaceURIs) {
    for (URI url : workspaceURIs) {
      try {
        workspace.delete(url);
      } catch (Exception e) {
        logger.warn("Could not delete {} from workspace: {}", url, e.getMessage());
      }
    }
  }

  private EncoderEngine getEncoderEngine() {
    EncoderEngine engine = new EncoderEngine(ffmpegBinary);
    activeEncoder.add(engine);
    return engine;
  }

  private EncodingProfile getProfile(Job job, String profileId) throws EncoderException {
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null) {
      final String msg = format("Profile %s is unknown", profileId);
      logger.error(msg);
      incident().recordFailure(job, PROFILE_NOT_FOUND, Collections.map(tuple("profile", profileId)));
      throw new EncoderException(msg);
    }
    return profile;
  }

  private Map<String, String> getWorkspaceMediapackageParams(String description, MediaPackageElement element) {
    return Collections.map(tuple("description", description),
            tuple("type", element.getElementType().toString()),
            tuple("url", element.getURI().toString()));
  }

  private Map<String, String> getWorkspaceCollectionParams(String description, String collectionId, URI url) {
    Map<String, String> params = new HashMap<>();
    params.put("description", description);
    params.put("collection", collectionId);
    params.put("url", url.toString());
    return params;
  }

  /**
   * Example composite command below. Use with `-filter_complex` option of ffmpeg if upper video is available otherwise
   * use -filver:v option for a single video.
   *
   * Dual video sample: The ffmpeg command needs two source files set with the `-i` option. The first media file is the
   * `lower`, the second the `upper` one. Example filter: -filter_complex
   * [0:v]scale=909:682,pad=1280:720:367:4:0x444345FF[lower];[1:v]scale=358:151[upper];[lower][upper]overlay=4:4[out]
   *
   * Single video sample: The ffmpeg command needs one source files set with the `-i` option. Example filter: filter:v
   * [in]scale=909:682,pad=1280:720:367:4:0x444345FF[out]
   *
   * @return commandline part with -filter_complex and -map options
   */
  private static String buildCompositeCommand(Dimension compositeTrackSize, LaidOutElement<Track> lowerLaidOutElement,
          Option<LaidOutElement<Track>> upperLaidOutElement, Option<File> upperFile,
          Option<LaidOutElement<Attachment>> watermarkOption, File watermarkFile, String backgroundColor) {
    final StringBuilder cmd = new StringBuilder();
    final String videoId = watermarkOption.isNone() ? "[out]" : "[video]";
    if (upperLaidOutElement.isNone()) {
      // There is only one video track and possibly one watermark.
      final Layout videoLayout = lowerLaidOutElement.getLayout();
      final String videoPosition = videoLayout.getOffset().getX() + ":" + videoLayout.getOffset().getY();
      final String scaleVideo = videoLayout.getDimension().getWidth() + ":" + videoLayout.getDimension().getHeight();
      final String padLower = compositeTrackSize.getWidth() + ":" + compositeTrackSize.getHeight() + ":"
              + videoPosition + ":" + backgroundColor;
      cmd.append("-filter:v [in]scale=").append(scaleVideo).append(",pad=").append(padLower).append(videoId);
    } else if (upperFile.isSome() && upperLaidOutElement.isSome()) {
      // There are two video tracks to handle.
      final Layout lowerLayout = lowerLaidOutElement.getLayout();
      final Layout upperLayout = upperLaidOutElement.get().getLayout();

      final String upperPosition = upperLayout.getOffset().getX() + ":" + upperLayout.getOffset().getY();
      final String lowerPosition = lowerLayout.getOffset().getX() + ":" + lowerLayout.getOffset().getY();

      final String scaleUpper = upperLayout.getDimension().getWidth() + ":" + upperLayout.getDimension().getHeight();
      final String scaleLower = lowerLayout.getDimension().getWidth() + ":" + lowerLayout.getDimension().getHeight();

      final String padLower = compositeTrackSize.getWidth() + ":" + compositeTrackSize.getHeight() + ":"
              + lowerPosition + ":" + backgroundColor;

      // Add input file for the upper track
      cmd.append("-i ").append(upperFile.get().getAbsolutePath()).append(" ");
      // Add filter complex mode
      cmd.append("-filter_complex").
      // lower video
              append(" [0:v]scale=").append(scaleLower).append(",pad=").append(padLower).append("[lower]")
              // upper video
              .append(";[1:v]scale=").append(scaleUpper).append("[upper]")
              // mix
              .append(";[lower][upper]overlay=").append(upperPosition).append(videoId);
    }

    for (final LaidOutElement<Attachment> watermarkLayout : watermarkOption) {
      String watermarkPosition = watermarkLayout.getLayout().getOffset().getX() + ":"
              + watermarkLayout.getLayout().getOffset().getY();
      cmd.append(";").append("movie=").append(watermarkFile.getAbsoluteFile()).append("[watermark];").append(videoId)
              .append("[watermark]overlay=").append(watermarkPosition).append("[out]");
    }

    if (upperLaidOutElement.isSome()) {
      // handle audio
      // if both videos contain audio mix it into a single audio stream
      final boolean lowerAudio = lowerLaidOutElement.getElement().hasAudio();
      final boolean upperAudio = upperLaidOutElement.get().getElement().hasAudio();
      if (lowerAudio && upperAudio) {
        cmd.append(";[0:a][1:a]amix=inputs=2[aout] -map [out] -map [aout]");
      } else if (lowerAudio) {
        cmd.append(" -map [out] -map 0:a");
      } else if (upperAudio) {
        cmd.append(" -map [out] -map 1:a");
      } else {
        cmd.append(" -map [out]");
      }
    }

    return cmd.toString();
  }

  private String buildConcatCommand(boolean onlyAudio, Dimension dimension, float outputFrameRate, List<File> files, List<Track> tracks) {
    StringBuilder sb = new StringBuilder();

    // Add input file paths
    for (File f : files) {
      sb.append("-i ").append(f.getAbsolutePath()).append(" ");
    }
    sb.append("-filter_complex ");

    boolean hasAudio = false;
    if (!onlyAudio) {
      // fps video filter if outputFrameRate is valid
      String fpsFilter = StringUtils.EMPTY;
      if (outputFrameRate > 0) {
        fpsFilter = format(Locale.US, "fps=fps=%f,", outputFrameRate);
      }
      // Add video scaling and check for audio
      int characterCount = 0;
      for (int i = 0; i < files.size(); i++) {
        if ((i % 25) == 0)
          characterCount++;
        sb.append("[").append(i).append(":v]").append(fpsFilter)
                .append("scale=iw*min(").append(dimension.getWidth()).append("/iw\\,").append(dimension.getHeight())
                .append("/ih):ih*min(").append(dimension.getWidth()).append("/iw\\,").append(dimension.getHeight())
                .append("/ih),pad=").append(dimension.getWidth()).append(":").append(dimension.getHeight())
                .append(":(ow-iw)/2:(oh-ih)/2").append(",setdar=")
                .append((float) dimension.getWidth() / (float) dimension.getHeight()).append("[");
        int character = ('a' + i + 1 - ((characterCount - 1) * 25));
        for (int y = 0; y < characterCount; y++) {
          sb.append((char) character);
        }
        sb.append("];");
        if (tracks.get(i).hasAudio())
          hasAudio = true;
      }

      // Add silent audio streams if at least one audio stream is available
      if (hasAudio) {
        for (int i = 0; i < files.size(); i++) {
          if (!tracks.get(i).hasAudio())
            sb.append("aevalsrc=0:d=1[silent").append(i + 1).append("];");
        }
      }
    }

    // Add concat segments
    int characterCount = 0;
    for (int i = 0; i < files.size(); i++) {
      if ((i % 25) == 0)
        characterCount++;

      int character = ('a' + i + 1 - ((characterCount - 1) * 25));
      if (!onlyAudio) {
        sb.append("[");
        for (int y = 0; y < characterCount; y++) {
          sb.append((char) character);
        }
        sb.append("]");
      }

      if (tracks.get(i).hasAudio()) {
        sb.append("[").append(i).append(":a]");
      } else if (hasAudio) {
        sb.append("[silent").append(i + 1).append("]");
      }
    }

    // Add concat command and output mapping
    sb.append("concat=n=").append(files.size()).append(":v=");
    if (onlyAudio) {
      sb.append("0");
    } else {
      sb.append("1");
    }
    sb.append(":a=");

    if (!onlyAudio) {
      if (hasAudio) {
        sb.append("1[v][a] -map [v] -map [a] ");
      } else {
        sb.append("0[v] -map [v] ");
      }
    } else {
      sb.append("1[a] -map [a]");
    }
    return sb.toString();
  }

  private List<URI> putToCollection(Job job, List<File> files, String description) throws EncoderException {
    List<URI> returnURLs = new ArrayList<>(files.size());
    for (File file: files) {
      try (InputStream in = new FileInputStream(file)) {
        String newFileName = format("%s.%s", job.getId(), FilenameUtils.getName(file.getAbsolutePath()));
        URI newFileURI = workspace.putInCollection(COLLECTION, newFileName, in);
        logger.info("Copied the {} to the workspace at {}", description, newFileURI);
        returnURLs.add(newFileURI);
      } catch (Exception e) {
        incident().recordFailure(job, WORKSPACE_PUT_COLLECTION_IO_EXCEPTION, e,
                getWorkspaceCollectionParams(description, COLLECTION, file.toURI()), NO_DETAILS);
        returnURLs.forEach(this::cleanupWorkspace);
        throw new EncoderException("Unable to put the " + description + " into the workspace", e);
      } finally {
        cleanup(file);
      }
    }
    return returnURLs;
  }

  private URI putToCollection(Job job, File output, String description) throws EncoderException {
    return putToCollection(job, java.util.Collections.singletonList(output), description).get(0);
  }

  private static List<Tuple<String, String>> detailsFor(EncoderException ex, EncoderEngine engine) {
    final List<Tuple<String, String>> d = Mutables.arrayList();
    d.add(tuple("encoder-engine-class", engine.getClass().getName()));
    if (ex instanceof CmdlineEncoderException) {
      d.add(tuple("encoder-commandline", ((CmdlineEncoderException) ex).getCommandLine()));
    }
    return d;
  }

  private Map<String, String> parseProperties(String serializedProperties) throws IOException {
    Properties properties = new Properties();
    try (InputStream in = IOUtils.toInputStream(serializedProperties, "UTF-8")) {
      properties.load(in);
      Map<String, String> map = new HashMap<>();
      for (Entry<Object, Object> e : properties.entrySet()) {
        map.put((String) e.getKey(), (String) e.getValue());
      }
      return map;
    }
  }

  private String getPropertiesAsString(Map<String, String> props) {
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : props.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Sets the media inspection service
   *
   * @param mediaInspectionService
   *          an instance of the media inspection service
   */
  protected void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.inspectionService = mediaInspectionService;
  }

  /**
   * Sets the workspace
   *
   * @param workspace
   *          an instance of the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the service registry
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Sets the profile scanner.
   *
   * @param scanner
   *          the profile scanner
   */
  protected void setProfileScanner(EncodingProfileScanner scanner) {
    this.profileScanner = scanner;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  @Override
  public Job demux(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Demux.toString(),
              Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack)));
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  private List<Track> demux(final Job job, Track videoTrack, String encodingProfile) throws EncoderException {
    if (job == null)
      throw new IllegalArgumentException("The Job parameter must not be null");

    try {
      // Get the track and make sure it exists
      final File videoFile = (videoTrack != null) ? loadTrackIntoWorkspace(job, "source", videoTrack) : null;

      // Get the encoding profile
      EncodingProfile profile = getProfile(job, encodingProfile);
      // Create the engine/get
      logger.info(format("Encoding video track %s using profile '%s'", videoTrack.getIdentifier(), profile));
      final EncoderEngine encoderEngine = getEncoderEngine();

      // Do the work
      List<File> outputs;
      try {
        Map<String, File> source = new HashMap<>();
        source.put("video", videoFile);
        outputs = encoderEngine.process(source, profile, null);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<>();
        params.put("video", (videoFile != null) ? videoTrack.getURI().toString() : "EMPTY");
        params.put("profile", profile.getIdentifier());
        params.put("properties", "EMPTY");
        incident().recordFailure(job, ENCODING_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      } finally {
        activeEncoder.remove(encoderEngine);
      }

      // demux did not return a file
      if (outputs.isEmpty() || !outputs.get(0).exists() || outputs.get(0).length() == 0)
        return null;

      List<URI> workspaceURIs = putToCollection(job, outputs, "demuxed file");
      List<Track> tracks = inspect(job, workspaceURIs);
      tracks.forEach(track -> track.setIdentifier(idBuilder.createNew().toString()));
      return tracks;
    } catch (Exception e) {
      logger.warn("Demux/MultiOutputEncode operation failed to encode " + videoTrack, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    maxMultipleProfilesJobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_MAX_MULTIPLE_PROFILES,
            DEFAULT_JOB_LOAD_MAX_MULTIPLE_PROFILES, serviceRegistry);
    processSmilJobLoadFactor = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_FACTOR_PROCESS_SMIL,
            DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR, serviceRegistry);
    if (processSmilJobLoadFactor == 0) {
      processSmilJobLoadFactor = DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR;
    }
    transitionDuration = 1000 * (int) LoadUtil.getConfiguredLoadValue(properties, PROCESS_SMIL_CLIP_TRANSITION_DURATION,
            DEFAULT_PROCESS_SMIL_CLIP_TRANSITION_DURATION, serviceRegistry);
  }

  /**
   * ProcessSmil processes editing of one source group (which may contain multiple source tracks) to one set of outputs
   * (to one or more encoding profiles). Note that the source tracks are expected to have the same dimensions.
   *
   * @param smil
   *          - smil containing with video names and clip sections from them
   * @param trackparamId
   *          - group id
   * @param mediaType
   *          - VIDEO_ONLY, AUDIO_ONLY, or "" if neither is true
   * @param profileIds
   *          - list of encoding profile Ids
   * @return Compose Job
   * @throws EncoderException
   *           - if encoding fails
   * @throws MediaPackageException
   *           - if missing files or bad mp
   */
  @Override
  public Job processSmil(Smil smil, String trackparamId, String mediaType, List<String> profileIds)
          throws EncoderException, MediaPackageException {
    try {
      ArrayList<String> al = new ArrayList<>();
      al.add(smil.toXML());
      al.add(trackparamId); // place holder for param ID
      al.add(mediaType); // audio, video or av
      for (String i : profileIds) {
        al.add(i);
      }
      float load = calculateJobLoadForMultipleProfiles(profileIds, processSmilJobLoadFactor);
      try {
        for (SmilMediaParamGroup paramGroup : smil.getHead().getParamGroups()) {
          for (SmilMediaParam param : paramGroup.getParams()) {
            if (SmilMediaParam.PARAM_NAME_TRACK_ID.equals(param.getName())) {
              if (trackparamId == null || trackparamId.equals(paramGroup.getId())) { // any track or specific groupid
                al.set(1, paramGroup.getId());
                return (serviceRegistry.createJob(JOB_TYPE, Operation.ProcessSmil.toString(), al, load));
              }
            }
          }
        }
      } catch (ServiceRegistryException e) {
        throw new EncoderException("Unable to create a job", e);
      } catch (Exception e) {
        throw new EncoderException("Unable to create a job - Exception in Parsing Smil", e);
      }
    } catch (Exception e) {
      throw new EncoderException("Unable to create a job - Exception processing XML in ProcessSmil", e);
    }
    throw new EncoderException("Unable to create a job - Cannot find paramGroup");
  }

  private List<EncodingProfile> findSuitableProfiles(List<String> encodingProfiles, String mediaType) {
    List<EncodingProfile> profiles = new ArrayList<>();
    for (String profileId1 : encodingProfiles) { // Check for mismatched profiles/media types
      EncodingProfile profile = profileScanner.getProfile(profileId1);
      // warn about bad encoding profiles, but encode anyway, the profile type is not enforced
      if (VIDEO_ONLY.equals(mediaType) && profile.getApplicableMediaType() == EncodingProfile.MediaType.Audio) {
        logger.warn("Profile '{}' supports {} but media is Video Only", profileId1, profile.getApplicableMediaType());
      } else if (AUDIO_ONLY.equals(mediaType) && profile.getApplicableMediaType() == EncodingProfile.MediaType.Visual) {
        logger.warn("Profile '{}' supports {} but media is Audio Only", profileId1, profile.getApplicableMediaType());
      }
      profiles.add(profile);
    }
    return (profiles);
  }

  /**
   * Fetch specified or first SmilMediaParamGroup from smil
   *
   * @param smil
   *          - smil object
   * @param trackParamGroupId
   *          - id for a particular param group or null
   * @return a named track group by id, if id is not specified, get first param group
   * @throws EncoderException
   */
  private SmilMediaParamGroup getSmilMediaParamGroup(Smil smil, String trackParamGroupId) throws EncoderException {
    try { // Find a track group id if not specified, get first param group
      if (trackParamGroupId == null)
        for (SmilMediaParamGroup paramGroup : smil.getHead().getParamGroups()) {
          for (SmilMediaParam param : paramGroup.getParams()) {
            if (SmilMediaParam.PARAM_NAME_TRACK_ID.equals(param.getName())) {
              trackParamGroupId = paramGroup.getId();
              break;
            }
          }
        }
      return ((SmilMediaParamGroup) smil.get(trackParamGroupId)); // If we want to concat multiple files
    } catch (SmilException ex) {
      throw new EncoderException("Smil does not contain a paramGroup element with Id " + trackParamGroupId, ex);
    }
  }

  /**
   * Splice segments given by smil document for the given track to the new one. This function reads the smil file and
   * reduce them to arguments to send to the encoder
   *
   * @param job
   *          processing job
   * @param smil
   *          smil document with media segments description
   * @param trackParamGroupId
   *          source track group
   * @param mediaType
   *          VIDEO_ONLY or AUDIO_ONLY or "" if it has both
   * @param encodingProfiles
   *          - profiles
   * @return serialized array of processed tracks
   * @throws EncoderException
   *           if an error occurred
   * @throws MediaPackageException
   *           - bad Mediapackage
   * @throws URISyntaxException
   */
  protected List<Track> processSmil(Job job, Smil smil, String trackParamGroupId, String mediaType,
          List<String> encodingProfiles) throws EncoderException, MediaPackageException, URISyntaxException {

    List<EncodingProfile> profiles = findSuitableProfiles(encodingProfiles, mediaType);
    // If there are no usable encoding profiles, throw exception
    if (profiles.size() == 0)
      throw new EncoderException(
              "ProcessSmil - Media is not supported by the assigned encoding Profiles '" + encodingProfiles + "'");

    SmilMediaParamGroup trackParamGroup;
    ArrayList<String> inputfile = new ArrayList<>();
    Map<String, String> props = new HashMap<>();

    ArrayList<VideoClip> videoclips = new ArrayList<>();
    trackParamGroup = getSmilMediaParamGroup(smil, trackParamGroupId);

    String sourceTrackId = null;
    MediaPackageElementFlavor sourceTrackFlavor = null;
    String sourceTrackUri = null;
    File sourceFile = null;

    // get any source track from track group to get metadata
    for (SmilMediaParam param : trackParamGroup.getParams()) {
      if (SmilMediaParam.PARAM_NAME_TRACK_ID.equals(param.getName())) {
        sourceTrackId = param.getValue();
      } else if (SmilMediaParam.PARAM_NAME_TRACK_SRC.equals(param.getName())) {
        sourceTrackUri = param.getValue();
      } else if (SmilMediaParam.PARAM_NAME_TRACK_FLAVOR.equals(param.getName())) {
        sourceTrackFlavor = MediaPackageElementFlavor.parseFlavor(param.getValue());
      }
    }

    logger.info("ProcessSmil: Start processing track {}", sourceTrackUri);
    sourceFile = loadURIIntoWorkspace(job, "source", new URI(sourceTrackUri));
    inputfile.add(sourceFile.getAbsolutePath()); // default source - add to source table as 0
    props.put("in.video.path", sourceFile.getAbsolutePath());
    int srcIndex = inputfile.indexOf(sourceFile.getAbsolutePath()); // index = 0
    try {
      List<File> outputs;
      // parse body elements
      for (SmilMediaObject element : smil.getBody().getMediaElements()) {
        // body should contain par elements
        if (element.isContainer()) {
          SmilMediaContainer container = (SmilMediaContainer) element;
          if (SmilMediaContainer.ContainerType.PAR == container.getContainerType()) {
            // par element should contain media elements
            for (SmilMediaObject elementChild : container.getElements()) {
              if (!elementChild.isContainer()) {
                SmilMediaElement media = (SmilMediaElement) elementChild;
                if (trackParamGroupId.equals(media.getParamGroup())) {
                  long begin = media.getClipBeginMS();
                  long end = media.getClipEndMS();
                  URI clipTrackURI = media.getSrc();
                  File clipSourceFile = null;
                  if (clipTrackURI != null) {
                    clipSourceFile = loadURIIntoWorkspace(job, "Source", clipTrackURI);
                  }
                  if (sourceFile == null) {
                    sourceFile = clipSourceFile; // need one source file
                  }
                  int index = -1;

                  if (clipSourceFile != null) { // clip has different source
                    index = inputfile.indexOf(clipSourceFile.getAbsolutePath()); // Look for known tracks
                    if (index < 0) { // if new unknown track
                      inputfile.add(clipSourceFile.getAbsolutePath()); // add track
                      props.put("in.video.path" + index, sourceFile.getAbsolutePath());
                      index = inputfile.indexOf(clipSourceFile.getAbsolutePath());
                    }
                  } else {
                    index = srcIndex; // default source track
                  }
                  logger.debug("Adding edit clip index " + index + " begin " + begin + " end " + end + " to "
                          + sourceTrackId);
                  videoclips.add(new VideoClip(index, begin, end));
                }
              } else {
                throw new EncoderException("Smil container '"
                        + ((SmilMediaContainer) elementChild).getContainerType().toString() + "'is not supported yet");
              }
            }
          } else {
            throw new EncoderException(
                    "Smil container '" + container.getContainerType().toString() + "'is not supported yet");
          }
        }
      }
      List<Long> edits = new ArrayList<>(); // collect edit points
      for (VideoClip clip : videoclips) {
        edits.add((long) clip.getSrc());
        edits.add(clip.getStartMS());
        edits.add(clip.getEndMS());
      }
      List<File> inputs = new ArrayList<>(); // collect input source tracks
      for (String f : inputfile) {
        inputs.add(new File(f));
      }
      EncoderEngine encoderEngine = getEncoderEngine();
      try {
        outputs = encoderEngine.multiTrimConcat(inputs, edits, profiles, transitionDuration,
                !AUDIO_ONLY.equals(mediaType), !VIDEO_ONLY.equals(mediaType));
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<>();
        List<String> profileList = new ArrayList<>();
        for (EncodingProfile p : profiles) {
          profileList.add(p.getIdentifier().toString());
        }
        params.put("videos", StringUtils.join(inputs, ","));
        params.put("profiles", StringUtils.join(profileList,","));
        incident().recordFailure(job, PROCESS_SMIL_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      } finally {
        activeEncoder.remove(encoderEngine);
      }
      logger.info("ProcessSmil/MultiTrimConcat returns {} media files {}", outputs.size(), outputs);
      List<URI> workspaceURIs = putToCollection(job, outputs, "processSmil files");
      List<Track> tracks = inspect(job, workspaceURIs);
      tracks.forEach(track -> track.setIdentifier(idBuilder.createNew().toString()));
      return tracks;
    } catch (Exception e) { // clean up all the stored files
      throw new EncoderException("ProcessSmil operation failed to run ", e);
    }
  }

  @Override
  public Job multiEncode(Track sourceTrack, List<String> profileIds) throws EncoderException, MediaPackageException {
    try {
      // Job Load is based on number of encoding profiles
      float load = calculateJobLoadForMultipleProfiles(profileIds, multiEncodeJobLoadFactor);
      ArrayList<String> args = new ArrayList<>();
      args.add(MediaPackageElementParser.getAsXml(sourceTrack));
      args.addAll(profileIds);
      return serviceRegistry.createJob(JOB_TYPE, Operation.MultiEncode.toString(), args, load);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * A single encoding process that produces multiple outputs from a single track(s) using a list of encoding profiles.
   * Each output can be tagged by the profile name.
   *
   * @param job
   *          - encoding job
   * @param track
   *          - source track
   * @param profileIds
   *          - list of encoding profile Ids
   * @return encoded files
   * @throws EncoderException
   *           - if can't encode
   * @throws IllegalArgumentException
   *           - if missing arguments
   *
   */
  protected List<Track> multiEncode(final Job job, Track track, List<String> profileIds)
          throws EncoderException, IllegalArgumentException {
    if (job == null)
      throw new IllegalArgumentException("The Job parameter must not be null");
    if (track == null)
      throw new IllegalArgumentException("Source track cannot be null");
    if (profileIds == null || profileIds.isEmpty())
      throw new IllegalArgumentException("Cannot encode without encoding profiles");
    List<File> outputs = null;
    try {
      final File videoFile = loadTrackIntoWorkspace(job, "source", track);
      // Get the encoding profiles
      List<EncodingProfile> profiles = new ArrayList<>();
      for (String profileId : profileIds) {
        EncodingProfile profile = getProfile(job, profileId);
        profiles.add(profile);
      }
      logger.info("Encoding source track {} using profiles '{}'", track.getIdentifier(), profileIds);
      // Do the work
      EncoderEngine encoderEngine = getEncoderEngine();
      try {
        outputs = encoderEngine.multiTrimConcat(Arrays.asList(videoFile), null, profiles, 0, track.hasVideo(),
                track.hasAudio());
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<>();
        params.put("videos", videoFile.getName());
        params.put("profiles", StringUtils.join(profileIds, ","));
        incident().recordFailure(job, MULTI_ENCODE_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      } finally {
        activeEncoder.remove(encoderEngine);
      }
      logger.info("MultiEncode returns {} media files {} ", outputs.size(), outputs);
      List<URI> workspaceURIs = putToCollection(job, outputs, "multiencode files");
      List<Track> tracks = inspect(job, workspaceURIs);
      tracks.forEach(eachtrack -> eachtrack.setIdentifier(idBuilder.createNew().toString()));
      return tracks;
    } catch (Exception e) {
      throw new EncoderException("MultiEncode operation failed to run ", e);
    }
  }

  private float calculateJobLoadForMultipleProfiles(List<String> profileIds, float adjustmentFactor)
          throws EncoderException {
    // Job load is calculated based on the encoding profiles. They are summed up and multiplied by a factor.
    // The factor represents the adjustment that should be made assuming each profile job load was specified
    // based on it running with 1 input -> 1 output so normally will be a number 0 < n < 1.
    float load = 0.0f;
    for (String profileId : profileIds) {
      EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException("Encoding profile not found: " + profileId);
      }
      load += profile.getJobLoad();
    }
    load *= adjustmentFactor;
    if (load > maxMultipleProfilesJobLoad) {
      load = maxMultipleProfilesJobLoad;
    }
    return load;
  }
}
