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
package org.opencastproject.composer.impl;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

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
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
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
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** FFMPEG based implementation of the composer service api. */
public class ComposerServiceImpl extends AbstractJobProducer implements ComposerService {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** The collection name */
  public static final String COLLECTION = "composer";

  /** List of available operations on jobs */
  private enum Operation {
    Caption, Encode, Image, ImageConversion, Mux, Trim, Watermark, Composite, Concat, ImageToVideo
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
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
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
  protected Option<Track> encode(Job job, Track videoTrack, Track audioTrack, String profileId,
          Map<String, String> properties) throws EncoderException, MediaPackageException {
    if (job == null) {
      throw new EncoderException("The Job parameter must not be null");
    }
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
          throw new EncoderException("Requested audio track " + audioTrack + " is not found");
        } catch (IOException e) {
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
          throw new EncoderException("Requested video track " + videoTrack + " is not found");
        } catch (IOException e) {
          throw new EncoderException("Unable to access audio track " + audioTrack);
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

      if (audioTrack != null && videoTrack != null)
        logger.info("Muxing audio track {} and video track {} into {}", new String[] { audioTrack.getIdentifier(),
                videoTrack.getIdentifier(), targetTrackId });
      else if (audioTrack == null)
        logger.info("Encoding video track {} to {} using profile '{}'", new String[] { videoTrack.getIdentifier(),
                targetTrackId, profileId });
      else if (videoTrack == null)
        logger.info("Encoding audio track {} to {} using profile '{}'", new String[] { audioTrack.getIdentifier(),
                targetTrackId, profileId });

      // Do the work
      for (File encodingOutput : encoderEngine.mux(audioFile, videoFile, profile, properties)) {
        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.putInCollection(COLLECTION,
                  job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
          logger.info("Copied the encoded file to the workspace at {}", returnURL);
        } catch (Exception e) {
          cleanup(encodingOutput);
          cleanupWorkspace(returnURL);
          throw new EncoderException("Unable to put the encoded file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }

        cleanup(encodingOutput);

        // Have the encoded track inspected and return the result
        Job inspectionJob = null;
        try {
          inspectionJob = inspect(returnURL);
        } catch (MediaInspectionException e) {
          throw new EncoderException("Media inspection of " + returnURL + " failed", e);
        }

        Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
        inspectedTrack.setIdentifier(targetTrackId);

        if (profile.getMimeType() != null)
          inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

        return some(inspectedTrack);
      }
      // mux did not return a file
      return none();
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
        throw new EncoderException("Requested track " + sourceTrack + " is not found");
      } catch (IOException e) {
        throw new EncoderException("Unable to access track " + sourceTrack);
      }

      // Get the encoding profile
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException("Profile '" + profileId + " is unknown");
      }

      // Create the engine
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException(encoderEngine, "No encoder engine available for profile '" + profileId + "'");
      }

      // Do the work
      for (File encodingOutput : encoderEngine.trim(trackFile, profile, start, duration, null)) {
        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.putInCollection(COLLECTION,
                  job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
          logger.info("Copied the trimmed file to the workspace at {}", returnURL);
        } catch (FileNotFoundException e) {
          throw new EncoderException("Encoded file " + encodingOutput + " not found", e);
        } catch (IOException e) {
          cleanup(encodingOutput);
          cleanupWorkspace(returnURL);
          throw new EncoderException("Error putting " + encodingOutput + " into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }

        cleanup(encodingOutput);

        // Have the encoded track inspected and return the result
        Job inspectionJob = null;
        try {
          inspectionJob = inspect(returnURL);
        } catch (MediaInspectionException e) {
          throw new EncoderException("Media inspection of " + returnURL + " failed", e);
        }

        Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
        inspectedTrack.setIdentifier(targetTrackId);

        return some(inspectedTrack);
      }
      // trim did not return a file
      return none();
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#composite(Dimension, LaidOutElement, LaidOutElement, Option,
   *      String, Option)
   */
  @Override
  public Job composite(Dimension compositeTrackSize, LaidOutElement<Track> upperTrack,
          LaidOutElement<Track> lowerTrack, Option<LaidOutElement<Attachment>> watermark, String profileId,
          String background) throws EncoderException, MediaPackageException {
    List<String> arguments = new ArrayList<String>();
    arguments.add(0, MediaPackageElementParser.getAsXml(lowerTrack.getElement()));
    arguments.add(1, Serializer.json(lowerTrack.getLayout()).toJson());
    arguments.add(2, MediaPackageElementParser.getAsXml(upperTrack.getElement()));
    arguments.add(3, Serializer.json(upperTrack.getLayout()).toJson());
    arguments.add(4, Serializer.json(compositeTrackSize).toJson());
    arguments.add(5, profileId);
    arguments.add(6, background);
    if (watermark.isSome()) {
      LaidOutElement<Attachment> watermarkLaidOutElement = watermark.get();
      arguments.add(7, MediaPackageElementParser.getAsXml(watermarkLaidOutElement.getElement()));
      arguments.add(8, Serializer.json(watermarkLaidOutElement.getLayout()).toJson());
    }
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Composite.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create composite job", e);
    }
  }

  protected Option<Track> composite(Job job, Dimension compositeTrackSize, LaidOutElement<Track> lowerLaidOutElement,
          LaidOutElement<Track> upperLaiedOutElement, Option<LaidOutElement<Attachment>> watermarkOption,
          String profileId, String backgroundColor) throws EncoderException, MediaPackageException {
    if (job == null)
      throw new EncoderException("The Job parameter must not be null");
    if (compositeTrackSize == null)
      throw new EncoderException("The composite track size parameter must not be null");
    if (lowerLaidOutElement == null)
      throw new EncoderException("The lower laid out element parameter must not be null");
    if (upperLaiedOutElement == null)
      throw new EncoderException("The upper laid out element parameter must not be null");
    if (watermarkOption == null)
      throw new EncoderException("The optional watermark laid out element parameter must not be null");
    if (profileId == null)
      throw new EncoderException("The profileId parameter must not be null");
    if (backgroundColor == null)
      throw new EncoderException("The background color parameter must not be null");

    // Create the engine
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null)
      throw new EncoderException(null, "Profile '" + profileId + " is unknown");

    final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
    if (encoderEngine == null)
      throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");

    final String targetTrackId = idBuilder.createNew().toString();
    try {
      // Get the tracks and make sure they exist
      final File lowerVideoFile;
      try {
        lowerVideoFile = workspace.get(lowerLaidOutElement.getElement().getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested lower video track " + lowerLaidOutElement.getElement() + " is not found");
      } catch (IOException e) {
        throw new EncoderException("Unable to access lower video track " + lowerLaidOutElement.getElement());
      }

      final File upperVideoFile;
      try {
        upperVideoFile = workspace.get(upperLaiedOutElement.getElement().getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested upper video track " + upperLaiedOutElement.getElement() + " is not found");
      } catch (IOException e) {
        throw new EncoderException("Unable to access upper video track " + upperLaiedOutElement.getElement());
      }

      File watermarkFile = null;
      if (watermarkOption.isSome()) {
        try {
          watermarkFile = workspace.get(watermarkOption.get().getElement().getURI());
        } catch (NotFoundException e) {
          throw new EncoderException("Requested watermark image " + watermarkOption.get().getElement()
                  + " is not found");
        } catch (IOException e) {
          throw new EncoderException("Unable to access right watermark image " + watermarkOption.get().getElement());
        }
        logger.info("Compositing lower video track {} and upper video track {} including watermark {} into {}",
                new String[] { lowerLaidOutElement.getElement().getIdentifier(),
                        upperLaiedOutElement.getElement().getIdentifier(),
                        watermarkOption.get().getElement().getIdentifier(), targetTrackId });
      } else {
        logger.info("Compositing lower video track {} and upper video track {} into {}", new String[] {
                lowerLaidOutElement.getElement().getIdentifier(), upperLaiedOutElement.getElement().getIdentifier(),
                targetTrackId });
      }

      // Creating video filter command
      String compositeCommand = buildCompositeCommand(compositeTrackSize, lowerLaidOutElement, upperLaiedOutElement,
              watermarkOption, upperVideoFile, watermarkFile, backgroundColor);

      Map<String, String> properties = new HashMap<String, String>();
      properties.put("compositeCommand", compositeCommand);

      for (File compoundFile : encoderEngine.mux(upperVideoFile, lowerVideoFile, profile, properties)) {
        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(compoundFile);
          returnURL = workspace.putInCollection(COLLECTION,
                  job.getId() + "." + FilenameUtils.getExtension(compoundFile.getAbsolutePath()), in);
          logger.info("Copied the compound file to the workspace at {}", returnURL);
        } catch (Exception e) {
          cleanup(compoundFile);
          cleanupWorkspace(returnURL);
          throw new EncoderException("Unable to put the compound file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }

        cleanup(compoundFile);

        // Have the compound track inspected and return the result
        Job inspectionJob = null;
        try {
          inspectionJob = inspect(returnURL);
        } catch (MediaInspectionException e) {
          throw new EncoderException("Media inspection of " + returnURL + " failed", e);
        }

        Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
        inspectedTrack.setIdentifier(targetTrackId);

        if (profile.getMimeType() != null)
          inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

        return some(inspectedTrack);
      }
      // composite did not return a file
      return none();
    } catch (Exception e) {
      logger.warn(
              "Error compositing " + lowerLaidOutElement.getElement() + " and " + upperLaiedOutElement.getElement(), e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  private String buildCompositeCommand(Dimension compositeTrackSize, LaidOutElement<Track> lowerLaidOutElement,
          LaidOutElement<Track> upperLaiedOutElement, Option<LaidOutElement<Attachment>> watermarkOption,
          File upperVideoFile, File watermarkFile, String backgroundColor) {

    Layout lowerLayout = lowerLaidOutElement.getLayout();
    Layout upperLayout = upperLaiedOutElement.getLayout();

    String upperPosition = upperLayout.getOffset().getX() + ":" + upperLayout.getOffset().getY();
    String lowerPosition = lowerLayout.getOffset().getX() + ":" + lowerLayout.getOffset().getY();

    String scaleUpper = upperLayout.getDimension().getWidth() + ":" + upperLayout.getDimension().getHeight();
    String scaleLower = lowerLayout.getDimension().getWidth() + ":" + lowerLayout.getDimension().getHeight();

    StringBuilder sb = new StringBuilder().append("[in]scale=").append(scaleLower).append(",pad=")
            .append(compositeTrackSize.getWidth()).append(":").append(compositeTrackSize.getHeight()).append(":")
            .append(lowerPosition).append(":").append(backgroundColor).append("[lower];movie=")
            .append(upperVideoFile.getAbsolutePath()).append(",scale=").append(scaleUpper).append("[upper];");

    if (watermarkOption.isSome()) {
      Layout watermarkLayout = watermarkOption.get().getLayout();
      String watermarkPosition = watermarkLayout.getOffset().getX() + ":" + watermarkLayout.getOffset().getY();
      // TODO scaleWatermark
      String scaleWaterMark = watermarkLayout.getDimension().getWidth() + ":"
              + watermarkLayout.getDimension().getHeight();
      sb.append("movie=").append(watermarkFile.getAbsolutePath()).append("[watermark];[lower][upper]overlay=")
              .append(upperPosition).append("[video];[video][watermark]overlay=main_w-overlay_w-")
              .append(watermarkPosition).append("[out]");
    } else {
      sb.append("[lower][upper]overlay=").append(upperPosition).append("[out]");
    }
    return sb.toString();
  }

  private String buildConcatCommand(boolean onlyAudio, Dimension dimension, List<File> files, List<Track> tracks) {
    StringBuilder sb = new StringBuilder();

    // Add input file paths
    for (File f : files) {
      sb.append("-i ").append(f.getAbsolutePath()).append(" ");
    }
    sb.append("-filter_complex ");

    boolean hasAudio = false;
    if (!onlyAudio) {
      // Add video scaling and check for audio
      for (int i = 0; i < files.size(); i++) {
        sb.append("[").append(i).append(":v]scale=iw*min(").append(dimension.getWidth()).append("/iw\\,")
                .append(dimension.getHeight()).append("/ih):ih*min(").append(dimension.getWidth()).append("/iw\\,")
                .append(dimension.getHeight()).append("/ih),pad=").append(dimension.getWidth()).append(":")
                .append(dimension.getHeight()).append(":(ow-iw)/2:(oh-ih)/2").append(",setdar=")
                .append((float) dimension.getWidth() / (float) dimension.getHeight()).append("[")
                .append((char) ('a' + i + 1)).append("];");
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
    for (int i = 0; i < files.size(); i++) {
      if (!onlyAudio)
        sb.append("[").append((char) ('a' + i + 1)).append("]");

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

  @Override
  public Job concat(String profileId, Dimension outputDimension, Track... tracks) throws EncoderException,
          MediaPackageException {
    ArrayList<String> arguments = new ArrayList<String>();
    arguments.add(0, profileId);
    if (outputDimension != null) {
      arguments.add(1, Serializer.json(outputDimension).toJson());
    } else {
      arguments.add(1, "");
    }
    for (int i = 0; i < tracks.length; i++) {
      arguments.add(i + 2, MediaPackageElementParser.getAsXml(tracks[i]));
    }
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Concat.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create concat job", e);
    }
  }

  protected Option<Track> concat(Job job, List<Track> tracks, String profileId, Dimension outputDimension)
          throws EncoderException, MediaPackageException {
    if (job == null)
      throw new EncoderException("The job parameter must not be null");
    if (tracks == null || tracks.size() < 2)
      throw new EncoderException("The track parameter must not be null and at least two tracks must be present");
    if (profileId == null)
      throw new EncoderException("The profile id parameter must not be null");

    boolean onlyAudio = true;
    for (Track t : tracks) {
      if (t.hasVideo()) {
        onlyAudio = false;
        break;
      }
    }

    if (!onlyAudio && outputDimension == null)
      throw new EncoderException("The output dimension id parameter must not be null when concating video");

    // Create the engine
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null)
      throw new EncoderException(null, "Profile '" + profileId + " is unknown");

    InputStream in = null;
    final String targetTrackId = idBuilder.createNew().toString();
    try {
      // Get the tracks and make sure they exist
      List<File> trackFiles = new ArrayList<File>();
      int i = 0;
      for (Track track : tracks) {
        try {
          trackFiles.add(i++, workspace.get(track.getURI()));
        } catch (NotFoundException e) {
          throw new EncoderException("Requested track " + track + " is not found");
        } catch (IOException e) {
          throw new EncoderException("Unable to access track " + track);
        }
      }

      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null)
        throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");

      if (onlyAudio) {
        logger.info("Concating audio tracks {} into {}", new String[] { trackFiles.toString(), targetTrackId });
      } else {
        logger.info("Concating video tracks {} into {}", new String[] { trackFiles.toString(), targetTrackId });
      }

      // Creating video filter command for concat
      String concatCommand = buildConcatCommand(onlyAudio, outputDimension, trackFiles, tracks);

      Map<String, String> properties = new HashMap<String, String>();
      properties.put("concatCommand", concatCommand);

      for (File concatFile : encoderEngine.encode(trackFiles.get(0), profile, properties)) {
        // Put the file in the workspace
        URI returnURL = null;
        try {
          in = new FileInputStream(concatFile);
          returnURL = workspace.putInCollection(COLLECTION,
                  job.getId() + "." + FilenameUtils.getExtension(concatFile.getAbsolutePath()), in);
          logger.info("Copied the concat file to the workspace at {}", returnURL);
        } catch (Exception e) {
          cleanup(concatFile);
          cleanupWorkspace(returnURL);
          throw new EncoderException("Unable to put the concat file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }

        cleanup(concatFile);

        // Have the concat track inspected and return the result
        Job inspectionJob = null;
        try {
          inspectionJob = inspect(returnURL);
        } catch (MediaInspectionException e) {
          throw new EncoderException("Media inspection of " + returnURL + " failed", e);
        }

        Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
        inspectedTrack.setIdentifier(targetTrackId);

        if (profile.getMimeType() != null)
          inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

        return some(inspectedTrack);
      }
      // concat did not return a file
      return none();
    } catch (Exception e) {
      logger.warn("Error concating tracks {}: {}", tracks, e);
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

    // Create the engine
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null) {
      final String msg = "Profile " + profileId + " is unknown";
      logger.error(msg);
      throw new EncoderException(msg);
    }

    final String targetTrackId = idBuilder.createNew().toString();
    try {
      // Get the attachment and make sure it exist
      File imageFile = null;
      try {
        imageFile = workspace.get(sourceImage.getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested source image " + sourceImage + " is not found");
      } catch (IOException e) {
        throw new EncoderException("Unable to access source image " + sourceImage);
      }

      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null)
        throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");

      logger.info("Converting image attachment {} into video {}", new String[] { sourceImage.getIdentifier(),
              targetTrackId });

      Map<String, String> properties = new HashMap<String, String>();
      if (time == null || time == -1)
        time = 0D;

      DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
      ffmpegFormat.setDecimalSeparator('.');
      DecimalFormat df = new DecimalFormat("0.000", ffmpegFormat);
      properties.put("time", df.format(time));
      // Do the work
      for (File conversionOutput : encoderEngine.encode(imageFile, profile, properties)) {
        // check for validity of output
        if (conversionOutput == null || !conversionOutput.isFile()) {
          throw new EncoderException("Image conversion failed: no video was produced");
        }

        // Put the file in the workspace
        URI workspaceURI = null;
        InputStream in = null;
        try {
          in = new FileInputStream(conversionOutput);
          workspaceURI = workspace.putInCollection(COLLECTION,
                  job.getId() + "." + FilenameUtils.getExtension(conversionOutput.getAbsolutePath()), in);
          logger.debug("Copied video file to the workspace at {}", workspaceURI);
        } catch (Exception e) {
          cleanup(conversionOutput);
          cleanupWorkspace(workspaceURI);
          throw new EncoderException("Unable to put video file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }

        // cleanup
        cleanup(conversionOutput);

        // Have the compound track inspected and return the result
        Job inspectionJob = null;
        try {
          inspectionJob = inspect(workspaceURI);
        } catch (MediaInspectionException e) {
          throw new EncoderException("Media inspection of " + workspaceURI + " failed", e);
        }

        Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
        inspectedTrack.setIdentifier(targetTrackId);

        if (profile.getMimeType() != null)
          inspectedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

        return some(inspectedTrack);
      }
      // encoding did not return a file
      return none();
    } catch (Exception e) {
      logger.warn("Error converting " + sourceImage, e);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  protected Job inspect(URI workspaceURI) throws MediaInspectionException, EncoderException {
    Job inspectionJob = inspectionService.inspect(workspaceURI);
    JobBarrier barrier = new JobBarrier(serviceRegistry, inspectionJob);
    if (!barrier.waitForJobs().isSuccess()) {
      throw new EncoderException("Media inspection of " + workspaceURI + " failed");
    }
    return inspectionJob;
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

    String[] parameters = new String[times.length + 2];
    parameters[0] = MediaPackageElementParser.getAsXml(sourceTrack);
    parameters[1] = profileId;
    for (int i = 0; i < times.length; i++) {
      parameters[i + 2] = Double.toString(times[i]);
    }

    // TODO: This is unfortunate, since ffmpeg is slow on single images and it would be nice to be able to start a
    // separate job per image, so extraction can be spread over multiple machines in a cluster.
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Image.toString(), Arrays.asList(parameters));
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
   *          the identifer of the encoding profile to use
   * @param times
   *          (one or more) times in miliseconds
   * @return the image as an attachment element
   * @throws EncoderException
   *           if extracting the image fails
   */
  protected List<Attachment> image(Job job, Track sourceTrack, String profileId, double... times)
          throws EncoderException, MediaPackageException {

    if (sourceTrack == null)
      throw new EncoderException("SourceTrack cannot be null");

    try {
      logger.info("creating an image using video track {}", sourceTrack.getIdentifier());

      // Get the encoding profile
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException("Profile '" + profileId + "' is unknown");
      }

      // Create the encoding engine
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException("No encoder engine available for profile '" + profileId + "'");
      }

      // make sure there is a video stream in the track
      if (sourceTrack != null && !sourceTrack.hasVideo()) {
        throw new EncoderException("Cannot extract an image without a video stream");
      }

      // The time should not be outside of the track's duration
      for (double time : times) {
        if (sourceTrack.getDuration() == null)
          throw new EncoderException("Unable to extract an image from a track with unknown duration");
        if (time < 0 || time > sourceTrack.getDuration()) {
          throw new EncoderException("Can not extract an image at time " + Double.valueOf(time)
                  + " from a track with duration " + Long.valueOf(sourceTrack.getDuration()));
        }
      }

      // Finally get the file that needs to be encoded
      File videoFile;
      try {
        videoFile = workspace.get(sourceTrack.getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested video track " + sourceTrack + " was not found", e);
      } catch (IOException e) {
        throw new EncoderException("Error accessing video track " + sourceTrack, e);
      }

      // Do the work
      List<File> encodingOutput = encoderEngine.extract(videoFile, profile, null, times);

      // check for validity of output
      if (encodingOutput == null || encodingOutput.isEmpty()) {
        throw new EncoderException("Image extraction failed: no images were produced");
      }
      for (File output : encodingOutput) {
        if (output == null || !output.isFile()) {
          cleanup(encodingOutput.toArray(new File[encodingOutput.size()]));
          throw new EncoderException("Image extraction failed: encoding output doesn't exist at " + output);
        }
      }

      // Put the file in the workspace
      List<URI> workspaceURIs = new LinkedList<URI>();
      for (int i = 0; i < encodingOutput.size(); i++) {
        File output = encodingOutput.get(i);
        InputStream in = null;
        try {
          in = new FileInputStream(output);
          URI returnURL = workspace.putInCollection(COLLECTION,
                  job.getId() + "_" + i + "." + FilenameUtils.getExtension(output.getAbsolutePath()), in);
          logger.debug("Copied image file to the workspace at {}", returnURL);
          workspaceURIs.add(returnURL);
        } catch (Exception e) {
          cleanup(encodingOutput.toArray(new File[encodingOutput.size()]));
          cleanupWorkspace(workspaceURIs.toArray(new URI[workspaceURIs.size()]));
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
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException("Profile '" + profileId + "' is unknown");
      }

      // Create the encoding engine
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException("No encoder engine available for profile '" + profileId + "'");
      }

      // Finally get the file that needs to be encoded
      File imageFile;
      try {
        imageFile = workspace.get(sourceImage.getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested video track " + sourceImage + " was not found", e);
      } catch (IOException e) {
        throw new EncoderException("Error accessing video track " + sourceImage, e);
      }

      // Do the work
      for (File conversionOutput : encoderEngine.encode(imageFile, profile, null)) {
        // check for validity of output
        if (conversionOutput == null || !conversionOutput.isFile()) {
          throw new EncoderException("Image extraction failed: no images were produced");
        }

        // Put the file in the workspace
        URI workspaceURI = null;
        InputStream in = null;
        try {
          in = new FileInputStream(conversionOutput);
          workspaceURI = workspace.putInCollection(COLLECTION,
                  job.getId() + "." + FilenameUtils.getExtension(conversionOutput.getAbsolutePath()), in);
          logger.debug("Copied image file to the workspace at {}", workspaceURI);
        } catch (Exception e) {
          cleanup(conversionOutput);
          cleanupWorkspace(workspaceURI);
          throw new EncoderException("Unable to put image file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }

        // cleanup
        cleanup(conversionOutput);

        MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
        Attachment attachment = (Attachment) builder.elementFromURI(workspaceURI, Attachment.TYPE, null);

        return some(attachment);
      }
      // encoding did not return a file
      return none();
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
      return serviceRegistry.createJob(JOB_TYPE, Operation.Caption.toString(), args);
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
  protected Track captions(Job job, Track mediaTrack, Catalog[] captions) throws EmbedderException {
    try {
      logger.info("Atempting to create and embed subtitles to video track");

      final String targetTrackId = idBuilder.createNew().toString();

      // get embedder engine
      final EmbedderEngine engine = embedderEngineFactory.newEmbedderEngine();
      if (engine == null) {
        throw new EmbedderException("Embedder engine not available");
      }

      // check if media file has video track
      if (mediaTrack == null || !mediaTrack.hasVideo()) {
        throw new EmbedderException("Media track must contain video stream");
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
        throw new EmbedderException("Could not find track: " + mediaTrack);
      } catch (IOException e) {
        throw new EmbedderException("Error accessing track: " + mediaTrack);
      }

      final File[] captionFiles = new File[captions.length];
      final String[] captionLanguages = new String[captions.length];
      for (int i = 0; i < captions.length; i++) {
        // get file
        try {
          captionFiles[i] = workspace.get(captions[i].getURI());
        } catch (NotFoundException e) {
          throw new EmbedderException("Could not found captions at: " + captions[i]);
        } catch (IOException e) {
          throw new EmbedderException("Error accessing captions at: " + captions[i]);
        }
        // get language
        captionLanguages[i] = getLanguageFromTags(captions[i].getTags());
        if (captionLanguages[i] == null) {
          throw new EmbedderException("Missing caption language information for captions at: " + captions[i]);
        }
      }

      // set properties
      Map<String, String> properties = new HashMap<String, String>();
      properties.put("param.trackh", String.valueOf(subHeight));
      properties.put("param.offset", String.valueOf(subHeight / 2));

      File output = engine.embed(mediaFile, captionFiles, captionLanguages, properties);

      URI returnURL = null;
      InputStream in = null;
      try {
        in = new FileInputStream(output);
        returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "." + FilenameUtils.getExtension(output.getAbsolutePath()), in);
        logger.info("Copied the encoded file to the workspace at {}", returnURL);
      } catch (Exception e) {
        cleanup(output);
        cleanupWorkspace(returnURL);
        throw new EmbedderException("Unable to put the encoded file into the workspace", e);
      } finally {
        IOUtils.closeQuietly(in);
      }

      cleanup(output);

      // Have the encoded track inspected and return the result
      Job inspectionJob;
      try {
        inspectionJob = inspect(returnURL);
      } catch (MediaInspectionException e) {
        throw new EmbedderException("Media inspection of " + returnURL + " failed", e);
      }

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
        case Image:
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          double[] times = new double[arguments.size() - 2];
          for (int i = 2; i < arguments.size(); i++) {
            times[i - 2] = Double.parseDouble(arguments.get(i));
          }
          List<Attachment> resultingElements = image(job, firstTrack, encodingProfile, times);
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
          firstTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          Layout lowerLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(1)));
          LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(firstTrack, lowerLayout);

          secondTrack = (Track) MediaPackageElementParser.getFromXml(arguments.get(2));
          Layout upperLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(3)));
          LaidOutElement<Track> upperLaiedOutElement = new LaidOutElement<Track>(secondTrack, upperLayout);

          Dimension compositeTrackSize = Serializer.dimension(JsonObj.jsonObj(arguments.get(4)));
          encodingProfile = arguments.get(5);
          String backgroundColor = arguments.get(6);

          Option<LaidOutElement<Attachment>> watermarkOption = Option.<LaidOutElement<Attachment>> none();
          if (arguments.size() == 9) {
            watermarkAttachment = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(7));
            Layout watermarkLayout = Serializer.layout(JsonObj.jsonObj(arguments.get(8)));
            watermarkOption = Option.some(new LaidOutElement<Attachment>(watermarkAttachment, watermarkLayout));
          }
          serialized = composite(job, compositeTrackSize, lowerLaidOutElement, upperLaiedOutElement, watermarkOption,
                  encodingProfile, backgroundColor).map(MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case Concat:
          encodingProfile = arguments.get(0);
          String dimensionString = arguments.get(1);
          Dimension outputDimension = null;
          if (StringUtils.isNotBlank(dimensionString))
            outputDimension = Serializer.dimension(JsonObj.jsonObj(dimensionString));
          List<Track> tracks = new ArrayList<Track>();
          for (int i = 2; i < arguments.size(); i++) {
            tracks.add(i - 2, (Track) MediaPackageElementParser.getFromXml(arguments.get(i)));
          }
          serialized = concat(job, tracks, encodingProfile, outputDimension).map(
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
      throw new EncoderException("Watermark image not found");
    }

    Map<String, String> watermarkProperties = new HashMap<String, String>();
    watermarkProperties.put("watermark", watermarkFile.getAbsolutePath());

    return encode(job, mediaTrack, null, encodingProfile, watermarkProperties);
  }

}
