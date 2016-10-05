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

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.fun.juc.Immutables.list;
import static org.opencastproject.serviceregistry.api.Incidents.NO_DETAILS;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EmbedderEngine;
import org.opencastproject.composer.api.EmbedderEngineFactory;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderEngineFactory;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.LaidOutElement;
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
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/** FFMPEG based implementation of the composer service api. */
public class ComposerServiceImpl extends AbstractJobProducer implements ComposerService, ManagedService {
  /**
   * The indexes the composite job uses to create a Job
   */
  private static final int BACKGROUND_COLOR_INDEX = 6;
  private static final int COMPOSITE_TRACK_SIZE_INDEX = 4;
  private static final int LOWER_TRACK_INDEX = 0;
  private static final int LOWER_TRACK_LAYOUT_INDEX = 1;
  private static final int PROFILE_ID_INDEX = 5;
  private static final int UPPER_TRACK_INDEX = 2;
  private static final int UPPER_TRACK_LAYOUT_INDEX = 3;
  private static final int WATERMARK_INDEX = 7;
  private static final int WATERMARK_LAYOUT_INDEX = 8;
  /**
   * Error codes
   */
  private static final int WORKSPACE_GET_IO_EXCEPTION = 1;
  private static final int WORKSPACE_GET_NOT_FOUND = 2;
  private static final int WORKSPACE_PUT_COLLECTION_IO_EXCEPTION = 3;
  private static final int PROFILE_NOT_FOUND = 4;
  private static final int ENCODER_ENGINE_NOT_FOUND = 5;
  private static final int EMBEDDER_ENGINE_NOT_FOUND = 6;
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
  private static final int CAPTION_EMBEDD_FAILED = 19;
  private static final int CAPTION_NO_VIDEO = 20;
  private static final int CAPTION_NO_LANGUAGE = 21;
  private static final int WATERMARK_NOT_FOUND = 22;
  private static final int NO_STREAMS = 23;

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** The collection name */
  public static final String COLLECTION = "composer";

  /** Used to mark a track unavailable to composite. */
  private static final String NOT_AVAILABLE = "n/a";

  /** The load introduced on the system by creating a caption job */
  public static final float DEFAULT_CAPTION_JOB_LOAD = 1.0f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_CAPTION_JOB_LOAD} */
  public static final String CAPTION_JOB_LOAD_KEY = "job.load.caption.embed";

  /** The load introduced on the system by creating a caption job */
  private float captionJobLoad = DEFAULT_CAPTION_JOB_LOAD;

  /** List of available operations on jobs */
  private enum Operation {
    Caption, Encode, Image, ImageConversion, Mux, Trim, Watermark, Composite, Concat, ImageToVideo, ParallelEncode
  }

  /** Encoding profile manager */
  private EncodingProfileScanner profileScanner = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** Reference to the encoder engine factory */
  private EncoderEngineFactory encoderEngineFactory;

  /** Reference to the embedder engine factory */
  private EmbedderEngineFactory embedderEngineFactory;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

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
    logger.info("Activating composer service");
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
      return serviceRegistry.createJob(JOB_TYPE, Operation.Encode.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), profileId));
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   *
   * @param videoTrack
   *          the video track
   * @param audioTrack
   *          the audio track
   * @param profileId
   *          the encoding profile
   * @param properties
   *          encoding properties
   * @return the encoded track or none if the operation does not return a track. This may happen for example when doing
   *         two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if encoding fails
   */
  protected Option<Track> encode(final Job job, Track videoTrack, Track audioTrack, String profileId,
          Map<String, String> properties) throws EncoderException, MediaPackageException {
    if (job == null)
      throw new IllegalArgumentException("The Job parameter must not be null");

    final String targetTrackId = idBuilder.createNew().toString();
    try {
      // Get the tracks and make sure they exist
      final File audioFile;
      if (audioTrack == null) {
        audioFile = null;
      } else {
        try {
          audioFile = workspace.get(audioTrack.getURI());
        } catch (NotFoundException e) {
          incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                  getWorkspaceMediapackageParams("audio", Type.Track, audioTrack.getURI()), NO_DETAILS);
          throw new EncoderException("Requested audio track " + audioTrack + " is not found");
        } catch (IOException e) {
          incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                  getWorkspaceMediapackageParams("audio", Type.Track, audioTrack.getURI()), NO_DETAILS);
          throw new EncoderException("Unable to access audio track " + audioTrack);
        }
      }

      final File videoFile;
      if (videoTrack == null) {
        videoFile = null;
      } else {
        try {
          videoFile = workspace.get(videoTrack.getURI());
        } catch (NotFoundException e) {
          incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                  getWorkspaceMediapackageParams("video", Type.Track, videoTrack.getURI()), NO_DETAILS);
          throw new EncoderException("Requested video track " + videoTrack + " is not found");
        } catch (IOException e) {
          incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                  getWorkspaceMediapackageParams("video", Type.Track, videoTrack.getURI()), NO_DETAILS);
          throw new EncoderException("Unable to access video track " + videoTrack);
        }
      }

      // Get the encoding profile
      final EncodingProfile profile = getProfile(job, profileId);

      // Create the engine
      final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

      if (audioTrack != null && videoTrack != null)
        logger.info("Muxing audio track {} and video track {} into {}",
                new Object[] { audioTrack.getIdentifier(), videoTrack.getIdentifier(), targetTrackId });
      else if (audioTrack == null)
        logger.info("Encoding video track {} to {} using profile '{}'",
                new Object[] { videoTrack.getIdentifier(), targetTrackId, profileId });
      else if (videoTrack == null)
        logger.info("Encoding audio track {} to {} using profile '{}'",
                new Object[] { audioTrack.getIdentifier(), targetTrackId, profileId });

      // Do the work
      Option<File> output;
      try {
        output = encoderEngine.mux(audioFile, videoFile, profile, properties);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<String, String>();
        if (audioFile != null) {
          params.put("audio", audioTrack.getURI().toString());
        } else {
          params.put("audio", "EMPTY");
        }
        if (videoFile != null) {
          params.put("video", videoTrack.getURI().toString());
        } else {
          params.put("video", "EMPTY");
        }
        params.put("profile", profile.getIdentifier());
        if (properties != null) {
          params.put("properties", properties.toString());
        } else {
          params.put("properties", "EMPTY");
        }
        incident().recordFailure(job, ENCODING_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      }

      // mux did not return a file
      if (output.isNone() || !output.get().exists() || output.get().length() == 0)
        return none();

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(), "encoded file");

      // Have the encoded track inspected and return the result
      Job inspectionJob = inspect(job, workspaceURI);

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      if (profile.getMimeType() != null)
        inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

      return some(inspectedTrack);
    } catch (Exception e) {
      logger.warn("Error encoding " + videoTrack + " and " + audioTrack, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   *
   * @param job
   * @param mediaTrack
   * @param profileId
   *          the encoding profile
   * @param properties
   *          encoding properties
   * @return the encoded track or none if the operation does not return a track. This may happen for example when doing
   *         two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if encoding fails
   */
  protected List <Track> parralelEncode(Job job, Track mediaTrack, String profileId,
          Map<String, String> properties) throws EncoderException, MediaPackageException {
    if (job == null) {
      throw new EncoderException("The Job parameter must not be null");
    }
    try {
      // Get the tracks and make sure they exist
      final File mediaFile;
      if (mediaTrack == null) {
        mediaFile = null;
      } else {
        try {
          mediaFile = workspace.get(mediaTrack.getURI());
        } catch (NotFoundException e) {
          throw new EncoderException("Requested media track " + mediaTrack + " is not found");
        } catch (IOException e) {
          throw new EncoderException("Unable to access media track " + mediaTrack);
        }
      }

      // Create the engine
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException(null, "Profile '" + profileId + " is unknown");
      }
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");
      }

      // List of encoded tracks
      LinkedList<Track> encodedTracks = new LinkedList<Track>();
      // Do the work
      int i = 0;
      for (File encodingOutput : encoderEngine.parallelEncode(mediaFile, profile, properties)) {
        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        final String targetTrackId = idBuilder.createNew().toString();

        try {
          in = new FileInputStream(encodingOutput);
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
        } finally {
          IOUtils.closeQuietly(in);
        }

        // Have the encoded track inspected and return the result
        Job inspectionJob = null;
        try {
          inspectionJob = inspectionService.inspect(returnURL);
          JobBarrier barrier = new JobBarrier(job, serviceRegistry, inspectionJob);
          if (!barrier.waitForJobs().isSuccess()) {
            throw new EncoderException("Media inspection of " + returnURL + " failed");
          }
        } catch (MediaInspectionException e) {
          throw new EncoderException("Media inspection of " + returnURL + " failed", e);
        }

        Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
        inspectedTrack.setIdentifier(targetTrackId);

        List<String> tags = profile.getTags();
        if (tags.size() > 0) {
          for (int j = 0; j < tags.size(); j++) {
            if (encodingOutput.getName().endsWith(profile.getSuffix(tags.get(j))))
              inspectedTrack.addTag(tags.get(j));
          }
        }

        // Will the mimetype be provided by the inspect service?
        // if (profile.getMimeType() != null)
        // inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

        encodedTracks.add(inspectedTrack);
        i++;
      }

      return encodedTracks;
    } catch (Exception e) {
      logger.warn("Error encoding " + mediaTrack, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
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
      logger.info("Starting parallel encode with profile {} ", profileId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.ParallelEncode.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), profileId));
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
      return serviceRegistry.createJob(
              JOB_TYPE,
              Operation.Trim.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), profileId, Long.toString(start),
                      Long.toString(duration)));
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
  protected Option<Track> trim(Job job, Track sourceTrack, String profileId, long start, long duration)
          throws EncoderException {
    try {
      String targetTrackId = idBuilder.createNew().toString();

      // Get the track and make sure it exists
      final File trackFile;
      try {
        trackFile = workspace.get(sourceTrack.getURI());
      } catch (NotFoundException e) {
        incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                getWorkspaceMediapackageParams("source", Type.Track, sourceTrack.getURI()), NO_DETAILS);
        throw new EncoderException("Requested track " + sourceTrack + " is not found");
      } catch (IOException e) {
        incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                getWorkspaceMediapackageParams("source", Type.Track, sourceTrack.getURI()), NO_DETAILS);
        throw new EncoderException("Unable to access track " + sourceTrack);
      }

      // Get the encoding profile
      final EncodingProfile profile = getProfile(job, profileId);

      // Create the engine
      final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

      Option<File> output;
      try {
        output = encoderEngine.trim(trackFile, profile, start, duration, null);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("track", sourceTrack.getURI().toString());
        params.put("profile", profile.getIdentifier());
        params.put("start", Long.toString(start));
        params.put("duration", Long.toString(duration));
        incident().recordFailure(job, TRIMMING_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      }

      // trim did not return a file
      if (output.isNone() || !output.get().exists() || output.get().length() == 0)
        return none();

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(), "trimmed file");

      // Have the encoded track inspected and return the result
      Job inspectionJob = inspect(job, workspaceURI);

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      return some(inspectedTrack);
    } catch (Exception e) {
      logger.warn("Error trimming " + sourceTrack, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
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
      return serviceRegistry.createJob(
              JOB_TYPE,
              Operation.Mux.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(videoTrack),
                      MediaPackageElementParser.getAsXml(audioTrack), profileId));
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
  protected Option<Track> mux(Job job, Track videoTrack, Track audioTrack, String profileId) throws EncoderException,
          MediaPackageException {
    return encode(job, videoTrack, audioTrack, profileId, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Job composite(Dimension compositeTrackSize, Option<LaidOutElement<Track>> upperTrack,
          LaidOutElement<Track> lowerTrack, Option<LaidOutElement<Attachment>> watermark, String profileId,
          String background) throws EncoderException, MediaPackageException {
    List<String> arguments = new ArrayList<String>();
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
    arguments.add(PROFILE_ID_INDEX, profileId);
    arguments.add(BACKGROUND_COLOR_INDEX, background);
    if (watermark.isSome()) {
      LaidOutElement<Attachment> watermarkLaidOutElement = watermark.get();
      arguments.add(WATERMARK_INDEX, MediaPackageElementParser.getAsXml(watermarkLaidOutElement.getElement()));
      arguments.add(WATERMARK_LAYOUT_INDEX, Serializer.json(watermarkLaidOutElement.getLayout()).toJson());
    }
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Composite.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create composite job", e);
    }
  }

  protected Option<Track> composite(Job job, Dimension compositeTrackSize, LaidOutElement<Track> lowerLaidOutElement,
          Option<LaidOutElement<Track>> upperLaidOutElement, Option<LaidOutElement<Attachment>> watermarkOption,
          String profileId, String backgroundColor) throws EncoderException, MediaPackageException {
    if (job == null)
      throw new EncoderException("The Job parameter must not be null");
    if (compositeTrackSize == null)
      throw new EncoderException("The composite track size parameter must not be null");
    if (lowerLaidOutElement == null)
      throw new EncoderException("The lower laid out element parameter must not be null");
    if (upperLaidOutElement == null)
      throw new EncoderException("The upper laid out element parameter must not be null");
    if (watermarkOption == null)
      throw new EncoderException("The optional watermark laid out element parameter must not be null");
    if (profileId == null)
      throw new EncoderException("The profileId parameter must not be null");
    if (backgroundColor == null)
      throw new EncoderException("The background color parameter must not be null");

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    // Create the engine
    final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

    final String targetTrackId = idBuilder.createNew().toString();
    Option<File> upperVideoFile = Option.<File> none();
    try {
      // Get the tracks and make sure they exist
      final File lowerVideoFile;
      try {
        lowerVideoFile = workspace.get(lowerLaidOutElement.getElement().getURI());
      } catch (NotFoundException e) {
        incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                getWorkspaceMediapackageParams("lower video", Type.Track, lowerLaidOutElement.getElement().getURI()),
                NO_DETAILS);
        throw new EncoderException("Requested lower video track " + lowerLaidOutElement.getElement() + " is not found");
      } catch (IOException e) {
        incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                getWorkspaceMediapackageParams("lower video", Type.Track, lowerLaidOutElement.getElement().getURI()),
                NO_DETAILS);
        throw new EncoderException("Unable to access lower video track " + lowerLaidOutElement.getElement());
      }

      if (upperLaidOutElement.isSome()) {
        try {
          upperVideoFile = Option.option(workspace.get(upperLaidOutElement.get().getElement().getURI()));
        } catch (NotFoundException e) {
          incident().recordFailure(
                  job,
                  WORKSPACE_GET_NOT_FOUND,
                  e,
                  getWorkspaceMediapackageParams("upper video", Type.Track, upperLaidOutElement.get().getElement()
                          .getURI()), NO_DETAILS);
          throw new EncoderException("Requested upper video track " + upperLaidOutElement.get().getElement()
                  + " is not found");
        } catch (IOException e) {
          incident().recordFailure(
                  job,
                  WORKSPACE_GET_IO_EXCEPTION,
                  e,
                  getWorkspaceMediapackageParams("upper video", Type.Track, upperLaidOutElement.get().getElement()
                          .getURI()), NO_DETAILS);
          throw new EncoderException("Unable to access upper video track " + upperLaidOutElement.get().getElement());
        }
      }
      File watermarkFile = null;
      if (watermarkOption.isSome()) {
        try {
          watermarkFile = workspace.get(watermarkOption.get().getElement().getURI());
        } catch (NotFoundException e) {
          incident().recordFailure(
                  job,
                  WORKSPACE_GET_NOT_FOUND,
                  e,
                  getWorkspaceMediapackageParams("watermark image", Type.Attachment, watermarkOption.get().getElement()
                          .getURI()), NO_DETAILS);
          throw new EncoderException("Requested watermark image " + watermarkOption.get().getElement()
                  + " is not found");
        } catch (IOException e) {
          incident().recordFailure(
                  job,
                  WORKSPACE_GET_IO_EXCEPTION,
                  e,
                  getWorkspaceMediapackageParams("watermark image", Type.Attachment, watermarkOption.get().getElement()
                          .getURI()), NO_DETAILS);
          throw new EncoderException("Unable to access right watermark image " + watermarkOption.get().getElement());
        }
        if (upperLaidOutElement.isSome()) {
          logger.info("Composing lower video track {} {} and upper video track {} {} including watermark {} {} into {}",
                  new Object[] { lowerLaidOutElement.getElement().getIdentifier(),
                          lowerLaidOutElement.getElement().getURI(),
                          upperLaidOutElement.get().getElement().getIdentifier(),
                          upperLaidOutElement.get().getElement().getURI(),
                          watermarkOption.get().getElement().getIdentifier(),
                          watermarkOption.get().getElement().getURI(), targetTrackId });
        } else {
          logger.info("Composing video track {} {} including watermark {} {} into {}",
                  new Object[] { lowerLaidOutElement.getElement().getIdentifier(),
                          lowerLaidOutElement.getElement().getURI(), watermarkOption.get().getElement().getIdentifier(),
                          watermarkOption.get().getElement().getURI(), targetTrackId });
        }
      } else {
        if (upperLaidOutElement.isSome()) {
          logger.info("Composing lower video track {} {} and upper video track {} {} into {}",
                  new Object[] { lowerLaidOutElement.getElement().getIdentifier(),
                          lowerLaidOutElement.getElement().getURI(),
                          upperLaidOutElement.get().getElement().getIdentifier(),
                          upperLaidOutElement.get().getElement().getURI(), targetTrackId });
        } else {
          logger.info("Composing video track {} {} into {}",
                  new Object[] { lowerLaidOutElement.getElement().getIdentifier(),
                          lowerLaidOutElement.getElement().getURI(), targetTrackId });
        }
      }

      // Creating video filter command
      final String compositeCommand = buildCompositeCommand(compositeTrackSize, lowerLaidOutElement,
              upperLaidOutElement, upperVideoFile, watermarkOption, watermarkFile, backgroundColor);

      Map<String, String> properties = new HashMap<String, String>();
      properties.put("compositeCommand", compositeCommand);
      Option<File> output;
      try {
        if (upperVideoFile.isSome()) {
          output = encoderEngine.mux(upperVideoFile.get(), lowerVideoFile, profile, properties);
        } else {
          output = encoderEngine.mux(null, lowerVideoFile, profile, properties);
        }
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<String, String>();
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
      }

      // composite did not return a file
      if (output.isNone() || !output.get().exists() || output.get().length() == 0)
        return none();

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(), "compound file");

      // Have the compound track inspected and return the result
      Job inspectionJob = inspect(job, workspaceURI);

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      if (profile.getMimeType() != null)
        inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

      return some(inspectedTrack);
    } catch (Exception e) {
      if (upperLaidOutElement.isSome()) {
        logger.warn("Error composing {}  and {}: {}", new Object[] { lowerLaidOutElement.getElement(),
                upperLaidOutElement.get().getElement(), getStackTrace(e) });
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
  public Job concat(String profileId, Dimension outputDimension, Track... tracks) throws EncoderException,
          MediaPackageException {
    return concat(profileId, outputDimension, -1.0f, tracks);
  }

  @Override
  public Job concat(String profileId, Dimension outputDimension, float outputFrameRate, Track... tracks) throws EncoderException,
          MediaPackageException {
    ArrayList<String> arguments = new ArrayList<String>();
    arguments.add(0, profileId);
    if (outputDimension != null) {
      arguments.add(1, Serializer.json(outputDimension).toJson());
    } else {
      arguments.add(1, "");
    }
    arguments.add(2, String.format(Locale.US, "%f", outputFrameRate));
    for (int i = 0; i < tracks.length; i++) {
      arguments.add(i + 3, MediaPackageElementParser.getAsXml(tracks[i]));
    }
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Concat.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create concat job", e);
    }
  }

  protected Option<Track> concat(Job job, List<Track> tracks, String profileId, Dimension outputDimension, float outputFrameRate)
          throws EncoderException, MediaPackageException {
    if (job == null)
      throw new EncoderException("The job parameter must not be null");
    if (tracks == null)
      throw new EncoderException("The track parameter must not be null");
    if (profileId == null)
      throw new EncoderException("The profile id parameter must not be null");

    if (tracks.size() < 2) {
      Map<String, String> params = new HashMap<String, String>();
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

    if (!onlyAudio && outputDimension == null) {
      Map<String, String> params = new HashMap<String, String>();
      params.put("tracks", StringUtils.join(tracks, ","));
      incident().recordFailure(job, CONCAT_NO_DIMENSION, params);
      throw new EncoderException("The output dimension id parameter must not be null when concatenating video");
    }

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    InputStream in = null;
    final String targetTrackId = idBuilder.createNew().toString();
    try {
      // Get the tracks and make sure they exist
      List<File> trackFiles = new ArrayList<File>();
      int i = 0;
      for (Track track : tracks) {
        if (!track.hasAudio() && !track.hasVideo()) {
          Map<String, String> params = new HashMap<String, String>();
          params.put("track-id", track.getIdentifier());
          params.put("track-url", track.getURI().toString());
          incident().recordFailure(job, NO_STREAMS, params);
          throw new EncoderException("Track has no audio or video stream available: " + track);
        }
        try {
          trackFiles.add(i++, IoSupport.waitForFile(workspace.get(track.getURI())));
        } catch (NotFoundException e) {
          incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                  getWorkspaceMediapackageParams("concat", Type.Track, track.getURI()), NO_DETAILS);
          throw new EncoderException("Requested track " + track + " is not found");
        } catch (IOException e) {
          incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                  getWorkspaceMediapackageParams("concat", Type.Track, track.getURI()), NO_DETAILS);
          throw new EncoderException("Unable to access track " + track);
        }
      }

      // Create the engine
      final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

      if (onlyAudio) {
        logger.info("Concatenating audio tracks {} into {}", trackFiles, targetTrackId);
      } else {
        logger.info("Concatenating video tracks {} into {}", trackFiles, targetTrackId);
      }

      // Creating video filter command for concat
      String concatCommand = buildConcatCommand(onlyAudio, outputDimension, outputFrameRate, trackFiles, tracks);

      Map<String, String> properties = new HashMap<String, String>();
      properties.put("concatCommand", concatCommand);

      Option<File> output;
      try {
        output = encoderEngine.encode(trackFiles.get(0), profile, properties);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<String, String>();
        List<String> trackList = new ArrayList<String>();
        for (Track t : tracks) {
          trackList.add(t.getURI().toString());
        }
        params.put("tracks", StringUtils.join(trackList, ","));
        params.put("profile", profile.getIdentifier());
        params.put("properties", properties.toString());
        incident().recordFailure(job, CONCAT_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      }

      // concat did not return a file
      if (output.isNone() || !output.get().exists() || output.get().length() == 0)
        return none();

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(), "concatenated file");

      // Have the concat track inspected and return the result
      Job inspectionJob = inspect(job, workspaceURI);

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      if (profile.getMimeType() != null)
        inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

      return some(inspectedTrack);
    } catch (Exception e) {
      logger.warn("Error concatenating tracks {}: {}", tracks, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  @Override
  public Job imageToVideo(Attachment sourceImageAttachment, String profileId, double time) throws EncoderException,
          MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.ImageToVideo.toString(), Arrays.asList(
              MediaPackageElementParser.getAsXml(sourceImageAttachment), profileId, Double.toString(time)));
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create image to video job", e);
    }
  }

  protected Option<Track> imageToVideo(Job job, Attachment sourceImage, String profileId, Double time)
          throws EncoderException, MediaPackageException {
    if (job == null)
      throw new EncoderException("The Job parameter must not be null");
    if (sourceImage == null)
      throw new EncoderException("The sourceImage attachment parameter must not be null");
    if (profileId == null)
      throw new EncoderException("The profileId parameter must not be null");
    if (time == null)
      throw new EncoderException("The time parameter must not be null");

    // Get the encoding profile
    final EncodingProfile profile = getProfile(job, profileId);

    final String targetTrackId = idBuilder.createNew().toString();
    try {
      // Get the attachment and make sure it exist
      File imageFile = null;
      try {
        imageFile = workspace.get(sourceImage.getURI());
      } catch (NotFoundException e) {
        incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                getWorkspaceMediapackageParams("source image", Type.Attachment, sourceImage.getURI()), NO_DETAILS);
        throw new EncoderException("Requested source image " + sourceImage + " is not found");
      } catch (IOException e) {
        incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                getWorkspaceMediapackageParams("source image", Type.Attachment, sourceImage.getURI()), NO_DETAILS);
        throw new EncoderException("Unable to access source image " + sourceImage);
      }

      // Create the engine
      final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

      logger.info("Converting image attachment {} into video {}", sourceImage.getIdentifier(), targetTrackId);

      Map<String, String> properties = new HashMap<String, String>();
      if (time == null || time == -1)
        time = 0D;

      DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
      ffmpegFormat.setDecimalSeparator('.');
      DecimalFormat df = new DecimalFormat("0.000", ffmpegFormat);
      properties.put("time", df.format(time));

      Option<File> output;
      try {
        output = encoderEngine.encode(imageFile, profile, properties);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("image", sourceImage.getURI().toString());
        params.put("profile", profile.getIdentifier());
        params.put("properties", properties.toString());
        incident().recordFailure(job, IMAGE_TO_VIDEO_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      }

      // encoding did not return a file
      if (output.isNone() || !output.get().exists() || output.get().length() == 0)
        return none();

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(), "converted image file");

      // Have the compound track inspected and return the result
      Job inspectionJob = inspect(job, workspaceURI);

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      if (profile.getMimeType() != null)
        inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

      return some(inspectedTrack);
    } catch (Exception e) {
      logger.warn("Error converting " + sourceImage, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
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
    parameters.add(MediaPackageElementParser.getAsXml(sourceTrack));
    parameters.add(profileId);
    parameters.add(Boolean.TRUE.toString());
    for (int i = 0; i < times.length; i++) {
      parameters.add(Double.toString(times[i]));
    }

    // TODO: This is unfortunate, since ffmpeg is slow on single images and it would be nice to be able to start a
    // separate job per image, so extraction can be spread over multiple machines in a cluster.
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Image.toString(), parameters);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  @Override
  public Job image(Track sourceTrack, String profileId, Map<String, String> properties) throws EncoderException,
          MediaPackageException {
    if (sourceTrack == null)
      throw new IllegalArgumentException("SourceTrack cannot be null");

    List<String> arguments = new ArrayList<String>();
    arguments.add(MediaPackageElementParser.getAsXml(sourceTrack));
    arguments.add(profileId);
    arguments.add(Boolean.FALSE.toString());
    arguments.add(getPropertiesAsString(properties));

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Image.toString(), arguments);
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
        Map<String, String> params = new HashMap<String, String>();
        params.put("track-id", sourceTrack.getIdentifier());
        params.put("track-url", sourceTrack.getURI().toString());
        incident().recordFailure(job, IMAGE_EXTRACTION_UNKNOWN_DURATION, params);
        throw new EncoderException("Unable to extract an image from a track with unknown duration");
      }
      if (time < 0 || time * 1000 > sourceTrack.getDuration()) {
        Map<String, String> params = new HashMap<String, String>();
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
    try {
      logger.info("creating an image using video track {}", sourceTrack.getIdentifier());

      // Get the encoding profile
      final EncodingProfile profile = getProfile(job, profileId);

      // Create the encoding engine
      final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

      // Finally get the file that needs to be encoded
      File videoFile;
      try {
        videoFile = workspace.get(sourceTrack.getURI());
      } catch (NotFoundException e) {
        incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                getWorkspaceMediapackageParams("video", Type.Track, sourceTrack.getURI()), NO_DETAILS);
        throw new EncoderException("Requested video track " + sourceTrack + " was not found", e);
      } catch (IOException e) {
        incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                getWorkspaceMediapackageParams("video", Type.Track, sourceTrack.getURI()), NO_DETAILS);
        throw new EncoderException("Error accessing video track " + sourceTrack, e);
      }

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
        Map<String, String> params = new HashMap<String, String>();
        params.put("video", sourceTrack.getURI().toString());
        params.put("profile", profile.getIdentifier());
        params.put("positions", Arrays.toString(times));
        incident().recordFailure(job, IMAGE_EXTRACTION_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      }

      int i = 0;
      List<URI> workspaceURIs = new LinkedList<URI>();
      for (File output : encodingOutput) {

        if (!output.exists() || output.length() == 0) {
          logger.warn("Extracted image {} is empty!", output);
          throw new NotFoundException("Extracted image " + output.toString() + " is empty!");
        }

        // Put the file in the workspace
        InputStream in = null;
        try {
          in = new FileInputStream(output);
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
        } finally {
          IOUtils.closeQuietly(in);
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
    } catch (Exception e) {
      logger.warn("Error extracting image from " + sourceTrack, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  private void validateVideoStream(Job job, Track sourceTrack) throws EncoderException {
    // make sure there is a video stream in the track
    if (sourceTrack != null && !sourceTrack.hasVideo()) {
      Map<String, String> params = new HashMap<String, String>();
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
    parameters[0] = MediaPackageElementParser.getAsXml(image);
    parameters[1] = profileId;

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.ImageConversion.toString(), Arrays.asList(parameters));
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
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
  protected Option<Attachment> convertImage(Job job, Attachment sourceImage, String profileId) throws EncoderException,
          MediaPackageException {
    if (sourceImage == null)
      throw new EncoderException("SourceImage cannot be null");

    try {
      logger.info("Converting {}", sourceImage);

      // Get the encoding profile
      final EncodingProfile profile = getProfile(job, profileId);

      // Create the encoding engine
      final EncoderEngine encoderEngine = getEncoderEngine(job, profile);

      // Finally get the file that needs to be encoded
      File imageFile;
      try {
        imageFile = workspace.get(sourceImage.getURI());
      } catch (NotFoundException e) {
        incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                getWorkspaceMediapackageParams("source image", Type.Attachment, sourceImage.getURI()), NO_DETAILS);
        throw new EncoderException("Requested video track " + sourceImage + " was not found", e);
      } catch (IOException e) {
        incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                getWorkspaceMediapackageParams("source image", Type.Attachment, sourceImage.getURI()), NO_DETAILS);
        throw new EncoderException("Error accessing video track " + sourceImage, e);
      }

      // Do the work
      Option<File> output;
      try {
        output = encoderEngine.encode(imageFile, profile, null);
      } catch (EncoderException e) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("image", sourceImage.getURI().toString());
        params.put("profile", profile.getIdentifier());
        incident().recordFailure(job, CONVERT_IMAGE_FAILED, e, params, detailsFor(e, encoderEngine));
        throw e;
      }

      // encoding did not return a file
      if (output.isNone() || !output.get().exists() || output.get().length() == 0)
        return none();

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output.get(), "converted image file");

      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      Attachment attachment = (Attachment) builder.elementFromURI(workspaceURI, Attachment.TYPE, null);

      return some(attachment);
    } catch (Exception e) {
      logger.warn("Error converting image " + sourceImage, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }

  }

  /**
   * {@inheritDoc}
   *
   * Supports inserting captions in QuickTime files.
   *
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Catalog[])
   */
  @Override
  public Job captions(final Track mediaTrack, final Catalog[] captions) throws EmbedderException, MediaPackageException {
    List<String> args = new ArrayList<String>();
    args.set(0, MediaPackageElementParser.getAsXml(mediaTrack));
    for (int i = 0; i < captions.length; i++) {
      args.set(i + 1, MediaPackageElementParser.getAsXml(captions[i]));
    }

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Caption.toString(), args, captionJobLoad);
    } catch (ServiceRegistryException e) {
      throw new EmbedderException("Unable to create a job", e);
    }
  }

  /**
   * Adds the closed captions contained in the <code>captions</code> catalog collection to <code>mediaTrack</code>.
   *
   * @param job
   *          the associated job
   * @param mediaTrack
   *          the source track
   * @param captions
   *          the caption catalogs
   * @return the captioned track
   * @throws EmbedderException
   *           if embedding captions into the track fails
   */
  @SuppressWarnings("unchecked")
  protected Track captions(Job job, Track mediaTrack, Catalog[] captions) throws EmbedderException {
    try {
      logger.info("Attempting to create and embed subtitles to video track");

      final String targetTrackId = idBuilder.createNew().toString();

      // check if media file has video track
      if (mediaTrack == null || !mediaTrack.hasVideo()) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("track-id", mediaTrack.getIdentifier());
        params.put("track-url", mediaTrack.getURI().toString());
        incident().recordFailure(job, CAPTION_NO_VIDEO, params);
        throw new EmbedderException("Media track must contain video stream");
      }

      // get embedder engine
      final EmbedderEngine engine = embedderEngineFactory.newEmbedderEngine();
      if (engine == null) {
        final String msg = "Embedder engine not available";
        logger.error(msg);
        incident().recordFailure(job, EMBEDDER_ENGINE_NOT_FOUND,
                list(tuple("embedder-engine-class", embedderEngineFactory.getClass().getName())));
        throw new EmbedderException(msg);
      }

      // get video height
      Integer videoHeigth = null;
      for (Stream s : mediaTrack.getStreams()) {
        if (s instanceof VideoStream) {
          videoHeigth = ((VideoStream) s).getFrameHeight();
          break;
        }
      }
      final int subHeight;
      if (videoHeigth != null) {
        // get 1/8 of track height
        // smallest size is 60 pixels
        subHeight = videoHeigth > 8 * 60 ? videoHeigth / 8 : 60;
      } else {
        // no information about video height retrieved, use 60 pixels
        subHeight = 60;
      }

      // retrieve media file
      final File mediaFile;
      try {
        mediaFile = workspace.get(mediaTrack.getURI());
      } catch (NotFoundException e) {
        incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                getWorkspaceMediapackageParams("source", Type.Track, mediaTrack.getURI()), NO_DETAILS);
        throw new EmbedderException("Could not find track: " + mediaTrack);
      } catch (IOException e) {
        incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                getWorkspaceMediapackageParams("source", Type.Track, mediaTrack.getURI()), NO_DETAILS);
        throw new EmbedderException("Error accessing track: " + mediaTrack);
      }

      final File[] captionFiles = new File[captions.length];
      final String[] captionLanguages = new String[captions.length];
      for (int i = 0; i < captions.length; i++) {
        // get file
        try {
          captionFiles[i] = workspace.get(captions[i].getURI());
        } catch (NotFoundException e) {
          incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                  getWorkspaceMediapackageParams("caption", Type.Catalog, captions[i].getURI()), NO_DETAILS);
          throw new EmbedderException("Could not found captions at: " + captions[i]);
        } catch (IOException e) {
          incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                  getWorkspaceMediapackageParams("caption", Type.Catalog, captions[i].getURI()), NO_DETAILS);
          throw new EmbedderException("Error accessing captions at: " + captions[i]);
        }
        // get language
        captionLanguages[i] = getLanguageFromTags(captions[i].getTags());
        if (captionLanguages[i] == null) {
          Map<String, String> params = new HashMap<String, String>();
          params.put("caption-id", captions[i].getIdentifier());
          params.put("caption-url", captions[i].getURI().toString());
          params.put("caption-tags", StringUtils.join(captions[i].getTags()));
          incident().recordFailure(job, CAPTION_NO_LANGUAGE, params);
          throw new EmbedderException("Missing caption language information for captions at: " + captions[i]);
        }
      }

      // set properties
      Map<String, String> properties = new HashMap<String, String>();
      properties.put("param.trackh", String.valueOf(subHeight));
      properties.put("param.offset", String.valueOf(subHeight / 2));

      properties.put("param.input.stream.count", String.valueOf(mediaTrack.getStreams().length));

      File output;
      try {
        output = engine.embed(mediaFile, captionFiles, captionLanguages, properties);
      } catch (EmbedderException e) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("media", mediaTrack.getURI().toString());
        params.put("captions", StringUtils.join(captionFiles));
        params.put("languages", StringUtils.join(captionLanguages));
        params.put("properties", properties.toString());
        incident().recordFailure(job, CAPTION_EMBEDD_FAILED, e, params,
                list(tuple("embedder-engine-class", engine.getClass().getName())));
        throw e;
      }

      if (!output.exists() || output.length() == 0) {
        logger.warn("Embedded captions output file {} is empty!", output);
        throw new NotFoundException("Embedded captions output file " + output.toString() + " is empty!");
      }

      // Put the file in the workspace
      URI workspaceURI = putToCollection(job, output, "caption catalog file");

      // Have the encoded track inspected and return the result
      Job inspectionJob = inspect(job, workspaceURI);

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      return inspectedTrack;
    } catch (Exception e) {
      logger.warn("Error embedding captions into " + mediaTrack, e);
      if (e instanceof EncoderException) {
        throw (EmbedderException) e;
      } else {
        throw new EmbedderException(e);
      }
    }
  }

  @Override
  public Job watermark(Track mediaTrack, String watermark, String profileId) throws EncoderException,
          MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Watermark.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(mediaTrack), watermark, profileId));
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Encodes a video track with a watermark.
   *
   * @param mediaTrack
   *          the video track
   * @param watermark
   *          the watermark image
   * @param encodingProfile
   *          the encoding profile
   * @return the watermarked track or none if the operation does not return a track. This may happen for example when
   *         doing two pass encodings where the first pass only creates metadata for the second one
   * @throws EncoderException
   *           if encoding fails
   */
  protected Option<Track> watermark(Job job, Track mediaTrack, String watermark, String encodingProfile)
          throws EncoderException, MediaPackageException {
    logger.info("watermarking track {}.", mediaTrack.getIdentifier());
    File watermarkFile = new File(watermark);
    if (!watermarkFile.exists()) {
      logger.error("Watermark image {} not found.", watermark);
      Map<String, String> params = new HashMap<String, String>();
      params.put("watermark", watermarkFile.getAbsolutePath());
      incident().recordFailure(job, WATERMARK_NOT_FOUND, params);
      throw new EncoderException("Watermark image not found");
    }

    Map<String, String> watermarkProperties = new HashMap<String, String>();
    watermarkProperties.put("watermark", watermarkFile.getAbsolutePath());

    return encode(job, mediaTrack, null, encodingProfile, watermarkProperties);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      Track firstTrack = null;
      Track secondTrack = null;
      String encodingProfile = null;

      final String serialized;
      switch (op) {
        case Caption:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          Catalog[] catalogs = new Catalog[arguments.size() - 1];
          for (int i = 1; i < arguments.size(); i++) {
            catalogs[i] = (Catalog) MediaPackageElementParser.getFromXml(arguments.get(i));
          }
          serialized = MediaPackageElementParser.getAsXml(captions(job, firstTrack, catalogs));
          break;
        case Encode:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          serialized = encode(job, firstTrack, null, encodingProfile, null).map(
                  MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case ParallelEncode:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          serialized = MediaPackageElementParser.getArrayAsXml(parralelEncode(job, firstTrack, encodingProfile, null));
          break;
        case Image:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
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
          Attachment sourceImage = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          serialized = convertImage(job, sourceImage, encodingProfile).map(
                  MediaPackageElementParser.<Attachment> getAsXml()).getOrElse("");
          break;
        case Mux:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          secondTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
          encodingProfile = arguments.get(2);
          serialized = mux(job, firstTrack, secondTrack, encodingProfile).map(
                  MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case Trim:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          long start = Long.parseLong(arguments.get(2));
          long duration = Long.parseLong(arguments.get(3));
          serialized = trim(job, firstTrack, encodingProfile, start, duration).map(
                  MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case Watermark:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          String watermark = arguments.get(1);
          encodingProfile = arguments.get(2);
          serialized = watermark(job, firstTrack, watermark, encodingProfile).map(
                  MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case Composite:
          Attachment watermarkAttachment = null;
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(LOWER_TRACK_INDEX));
          Layout lowerLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(LOWER_TRACK_LAYOUT_INDEX)));
          LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(firstTrack, lowerLayout);
          Option<LaidOutElement<Track>> upperLaidOutElement = Option.<LaidOutElement<Track>> none();
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
          encodingProfile = arguments.get(PROFILE_ID_INDEX);
          String backgroundColor = arguments.get(BACKGROUND_COLOR_INDEX);

          Option<LaidOutElement<Attachment>> watermarkOption = Option.none();
          if (arguments.size() == 9) {
            watermarkAttachment = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(WATERMARK_INDEX));
            Layout watermarkLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(WATERMARK_LAYOUT_INDEX)));
            watermarkOption = Option.some(new LaidOutElement<Attachment>(watermarkAttachment, watermarkLayout));
          }
          serialized = composite(job, compositeTrackSize, lowerLaidOutElement, upperLaidOutElement, watermarkOption,
                  encodingProfile, backgroundColor).map(MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case Concat:
          encodingProfile = arguments.get(0);
          String dimensionString = arguments.get(1);
          String frameRateString = arguments.get(2);
          Dimension outputDimension = null;
          if (StringUtils.isNotBlank(dimensionString))
            outputDimension = Serializer.dimension(JsonObj.jsonObj(dimensionString));
          float outputFrameRate = NumberUtils.toFloat(frameRateString, -1.0f);
          List<Track> tracks = new ArrayList<Track>();
          for (int i = 3; i < arguments.size(); i++) {
            tracks.add(i - 3, (Track) MediaPackageElementParser.getFromXml(arguments.get(i)));
          }
          serialized = concat(job, tracks, encodingProfile, outputDimension, outputFrameRate).map(
                  MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case ImageToVideo:
          Attachment image = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          double time = Double.parseDouble(arguments.get(2));
          serialized = imageToVideo(job, image, encodingProfile, time)
                  .map(MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }

      return serialized;
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
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

  protected Job inspect(Job job, URI workspaceURI) throws EncoderException {
    Job inspectionJob;
    try {
      inspectionJob = inspectionService.inspect(workspaceURI);
    } catch (MediaInspectionException e) {
      incident().recordJobCreationIncident(job, e);
      throw new EncoderException("Media inspection of " + workspaceURI + " failed", e);
    }

    JobBarrier barrier = new JobBarrier(job, serviceRegistry, inspectionJob);
    if (!barrier.waitForJobs().isSuccess()) {
      throw new EncoderException("Media inspection of " + workspaceURI + " failed");
    }
    return inspectionJob;
  }

  /**
   * Helper function that iterates tags and returns language from tag in form lang:&lt;lang&gt;
   *
   * @param tags
   *          catalog tags
   * @return language or null if no corresponding tag was found
   */
  protected String getLanguageFromTags(String[] tags) {
    for (String tag : tags) {
      if (tag.startsWith("lang:") && tag.length() > 5) {
        return tag.substring(5);
      }
    }
    return null;
  }

  /**
   * Deletes any valid file in the list.
   *
   * @param encodingOutput
   *          list of files to be deleted
   */
  protected void cleanup(File... encodingOutput) {
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

  protected void cleanupWorkspace(URI... workspaceURIs) {
    for (URI url : workspaceURIs) {
      try {
        workspace.delete(url);
      } catch (Exception e) {
        logger.warn("Could not delete {} from workspace: {}", url, e.getMessage());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private EncoderEngine getEncoderEngine(Job job, final EncodingProfile profile) throws EncoderException {
    final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
    if (encoderEngine == null) {
      final String msg = "No encoder engine available for profile '" + profile.getIdentifier() + "'";
      logger.error(msg);
      incident().recordFailure(job, ENCODER_ENGINE_NOT_FOUND,
              Collections.map(tuple("profile", profile.getIdentifier()), tuple("profile-name", profile.getName())));
      throw new EncoderException(msg);
    }
    return encoderEngine;
  }

  @SuppressWarnings("unchecked")
  private EncodingProfile getProfile(Job job, String profileId) throws EncoderException {
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null) {
      final String msg = "Profile " + profileId + " is unknown";
      logger.error(msg);
      incident().recordFailure(job, PROFILE_NOT_FOUND, Collections.map(tuple("profile", profileId)));
      throw new EncoderException(msg);
    }
    return profile;
  }

  private Map<String, String> getWorkspaceMediapackageParams(String description, MediaPackageElement.Type type, URI url) {
    Map<String, String> params = new HashMap<String, String>();
    params.put("description", description);
    params.put("type", type.toString());
    params.put("url", url.toString());
    return params;
  }

  private Map<String, String> getWorkspaceCollectionParams(String description, String collectionId, URI url) {
    Map<String, String> params = new HashMap<String, String>();
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
  protected static String buildCompositeCommand(Dimension compositeTrackSize,
          LaidOutElement<Track> lowerLaidOutElement, Option<LaidOutElement<Track>> upperLaidOutElement,
          Option<File> upperFile, Option<LaidOutElement<Attachment>> watermarkOption, File watermarkFile,
          String backgroundColor) {
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
      cmd.append("-i " + upperFile.get().getAbsolutePath() + " ");
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
        fpsFilter = String.format(Locale.US, "fps=fps=%f,", outputFrameRate);
      }
      // Add video scaling and check for audio
      int characterCount = 0;
      for (int i = 0; i < files.size(); i++) {
        if ((i % 25) == 0)
          characterCount++;
        sb.append("[").append(i).append(":v]").append(fpsFilter)
                .append("scale=iw*min(").append(dimension.getWidth()).append("/iw\\,")
                .append(dimension.getHeight()).append("/ih):ih*min(").append(dimension.getWidth()).append("/iw\\,")
                .append(dimension.getHeight()).append("/ih),pad=").append(dimension.getWidth()).append(":")
                .append(dimension.getHeight()).append(":(ow-iw)/2:(oh-ih)/2").append(",setdar=")
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
            sb.append("aevalsrc=0::d=1[silent").append(i + 1).append("];");
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

  private URI putToCollection(Job job, File output, String description) throws EncoderException {
    URI returnURL = null;
    InputStream in = null;
    try {
      in = new FileInputStream(output);
      returnURL = workspace.putInCollection(COLLECTION,
              job.getId() + "." + FilenameUtils.getExtension(output.getAbsolutePath()), in);
      logger.info("Copied the {} to the workspace at {}", description, returnURL);
      return returnURL;
    } catch (Exception e) {
      incident().recordFailure(job, WORKSPACE_PUT_COLLECTION_IO_EXCEPTION, e,
              getWorkspaceCollectionParams(description, COLLECTION, output.toURI()), NO_DETAILS);
      cleanupWorkspace(returnURL);
      throw new EncoderException("Unable to put the " + description + " into the workspace", e);
    } finally {
      cleanup(output);
      IOUtils.closeQuietly(in);
    }
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
    InputStream in = null;
    try {
      in = IOUtils.toInputStream(serializedProperties, "UTF-8");
      properties.load(in);
      Map<String, String> map = new HashMap<String, String>();
      for (Entry<Object, Object> e : properties.entrySet()) {
        map.put((String) e.getKey(), (String) e.getValue());
      }
      return map;
    } finally {
      IOUtils.closeQuietly(in);
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
   * Sets the encoder engine factory
   *
   * @param encoderEngineFactory
   *          The encoder engine factory
   */
  protected void setEncoderEngineFactory(EncoderEngineFactory encoderEngineFactory) {
    this.encoderEngineFactory = encoderEngineFactory;
  }

  /**
   * Sets the embedder engine factoy
   *
   * @param embedderEngineFactory
   *          The embedder engine factory
   */
  protected void setEmbedderEngineFactory(EmbedderEngineFactory embedderEngineFactory) {
    this.embedderEngineFactory = embedderEngineFactory;
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
  public void updated(Dictionary properties) throws ConfigurationException {
    captionJobLoad = LoadUtil.getConfiguredLoadValue(properties, CAPTION_JOB_LOAD_KEY, DEFAULT_CAPTION_JOB_LOAD, serviceRegistry);
  }

}
