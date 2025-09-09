/*
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

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.eq;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The workflow definition for handling "image" operations
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Image Workflow Operation Handler",
        "workflow.operation=image"
    }
)
public class ImageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageWorkflowOperationHandler.class);

  // legacy option
  public static final String OPT_PROFILES = "encoding-profile";
  public static final String OPT_POSITIONS = "time";
  public static final String OPT_TARGET_BASE_NAME_FORMAT_SECOND = "target-base-name-format-second";
  public static final String OPT_TARGET_BASE_NAME_FORMAT_PERCENT = "target-base-name-format-percent";
  public static final String OPT_END_MARGIN = "end-margin";

  private static final long END_MARGIN_DEFAULT = 100;
  public static final double SINGLE_FRAME_POS = 0.0;

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the composer service
   */
  @Reference
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
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance wi, JobContext ctx)
          throws WorkflowOperationException {
    logger.debug("Running image workflow operation on {}", wi);
    try {
      MediaPackage mp = wi.getMediaPackage();
      final Extractor e = new Extractor(this, configure(mp, wi));
      return e.main(MediaPackageSupport.copy(mp));
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Computation within the context of a {@link Cfg}.
   */
  static final class Extractor {
    private final ImageWorkflowOperationHandler handler;
    private final Cfg cfg;

    Extractor(ImageWorkflowOperationHandler handler, Cfg cfg) {
      this.handler = handler;
      this.cfg = cfg;
    }

    /** Run the extraction. */
    WorkflowOperationResult main(final MediaPackage mp) throws WorkflowOperationException {
      if (cfg.sourceTracks.size() == 0) {
        logger.info("No source tracks found in media package {}, skipping operation", mp.getIdentifier());
        return handler.createResult(mp, Action.SKIP);
      }
      // start image extraction jobs
      final List<Extraction> extractions = cfg.sourceTracks.stream().flatMap(track -> {
          final List<MediaPosition> positions = limit(track, cfg.positions);
          if (positions.size() != cfg.positions.size()) {
            logger.warn("Could not apply all configured positions to track {}", track);
          }
          logger.info("Extracting images from {} at position {}", track, positions);
          // create one extraction per encoding profile
          return cfg.profiles.stream()
                  .map(profile -> new Extraction(extractImages(track, profile, positions), track, profile, positions));
        }).collect(Collectors.toList());
      final List<Job> extractionJobs = concatJobs(extractions);
      final JobBarrier.Result extractionResult = JobUtil.waitForJobs(handler.serviceRegistry, extractionJobs);
      if (extractionResult.isSuccess()) {
        // all extractions were successful; iterate them
        for (final Extraction extraction : extractions) {
          final List<Attachment> images = getImages(extraction.job);
          final int expectedNrOfImages = extraction.positions.size();
          if (images.size() == expectedNrOfImages) {
            // post process images
            for (int i = 0; i < images.size(); i++) {
              Attachment image = images.get(i);
              MediaPosition position = extraction.positions.get(i);

              adjustMetadata(extraction, image);
              if (image.getIdentifier() == null) {
                image.setIdentifier(UUID.randomUUID().toString());
              }
              mp.addDerived(image, extraction.track);
              String fileName = createFileName(
                  extraction.profile.getSuffix(),
                  extraction.track.getURI(),
                  position
              );
              moveToWorkspace(mp, image, fileName);
            }
          } else {
            // less images than expected have been extracted
            throw new WorkflowOperationException(
                    format("Only %s of %s images have been extracted from track %s",
                           images.size(), expectedNrOfImages, extraction.track));
          }
        }
        return handler.createResult(mp, Action.CONTINUE, JobUtil.sumQueueTime(extractionJobs));
      } else {
        throw new WorkflowOperationException("Image extraction failed");
      }
    }

    /**
     * Adjust flavor, tags, mime type of <code>image</code> according to the
     * configuration and the extraction.
     */
    void adjustMetadata(Extraction extraction, Attachment image) {
      // Adjust the target flavor. Make sure to account for partial updates
      for (final MediaPackageElementFlavor flavor : cfg.targetImageFlavor) {
        final String flavorType = eq("*", flavor.getType())
                ? extraction.track.getFlavor().getType()
                : flavor.getType();
        final String flavorSubtype = eq("*", flavor.getSubtype())
                ? extraction.track.getFlavor().getSubtype()
                : flavor.getSubtype();
        image.setFlavor(new MediaPackageElementFlavor(flavorType, flavorSubtype));
        logger.debug("Resulting image has flavor '{}'", image.getFlavor());
      }
      // Set the mime type
      try {
        image.setMimeType(MimeTypes.fromURI(image.getURI()));
      } catch (UnknownFileTypeException e) {
        logger.warn("Mime type unknown for file {}. Setting none.", image.getURI(), e);
      }
      // Add tags
      for (final String tag : cfg.targetImageTags) {
        logger.trace("Tagging image with '{}'", tag);
        image.addTag(tag);
      }
    }

    /** Create a file name for the extracted image. */
    String createFileName(final String suffix, final URI trackUri, final MediaPosition pos) {
      final String trackBaseName = FilenameUtils.getBaseName(trackUri.getPath());
      final String format;
      switch (pos.type) {
        case Seconds:
          format = cfg.targetBaseNameFormatSecond.orElse(trackBaseName + "_%.3fs%s");
          break;
        case Percentage:
          format = cfg.targetBaseNameFormatPercent.orElse(trackBaseName + "_%.1fp%s");
          break;
        default:
          throw new IllegalArgumentException("Unhandled MediaPosition type: " + pos.type);
      }
      return formatFileName(format, pos.position, suffix);
    }

    /** Move the extracted <code>image</code> to its final location in the workspace and rename it to <code>fileName</code>. */
    private void moveToWorkspace(final MediaPackage mp, final Attachment image, final String fileName) {
      try {
        image.setURI(handler.workspace.moveTo(
                image.getURI(),
                mp.getIdentifier().toString(),
                image.getIdentifier(),
                fileName));
      } catch (Exception e) {
        throw new RuntimeException(new WorkflowOperationException(e));
      }
    }

    /** Start a composer job to extract images from a track at the given positions. */
    private Job extractImages(final Track track, final EncodingProfile profile, final List<MediaPosition> positions) {
      List<Double> seconds = positions.stream()
          .map(position -> toSeconds(track, position, cfg.endMargin))
          .collect(Collectors.toList());

      try {
        return handler.composerService.image(track, profile.getIdentifier(), Collections.toDoubleArray(seconds));
      } catch (Exception e) {
        throw new RuntimeException(new WorkflowOperationException("Error starting image extraction job", e));
      }
    }
  }

  // ** ** **

  /**
   * Format a filename and make it "safe".
   */
  static String formatFileName(String format, double position, String suffix) {
    return format(Locale.ROOT, format, position, suffix);
  }


  /** Concat the jobs of a list of extraction objects. */
  private static List<Job> concatJobs(List<Extraction> extractions) {
    return extractions.stream()
        .map(extraction -> extraction.job)
        .collect(Collectors.toList());
  }

  /** Get the images (payload) from a job. */
  @SuppressWarnings("unchecked")
  private static List<Attachment> getImages(Job job) {
    final List<Attachment> images;
    try {
      images = (List<Attachment>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    } catch (MediaPackageException e) {
      throw new RuntimeException(e);
    }
    if (!images.isEmpty()) {
      return images;
    } else {
      throw new RuntimeException(new WorkflowOperationException("Job did not extract any images"));
    }
  }

  /** Limit the list of media positions to those that fit into the length of the track. */
  static List<MediaPosition> limit(Track track, List<MediaPosition> positions) {
    final Long duration = track.getDuration();
    // if the video has just one frame (e.g.: MP3-Podcasts) it makes no sense to go to a certain position
    // as the video has only one image at position 0
    if (duration == null || (track.getStreams() != null && Arrays.stream(track.getStreams())
            .filter(stream -> stream instanceof VideoStream)
            .map(org.opencastproject.mediapackage.Stream::getFrameCount)
            .allMatch(frameCount -> frameCount == null || frameCount == 1))) {
      return java.util.Collections.singletonList(new MediaPosition(PositionType.Seconds, 0));
    }

    return positions.stream()
        .filter(p -> (PositionType.Seconds.equals(p.type) && p.position >= 0 && p.position < duration)
                || (PositionType.Percentage.equals(p.type) && p.position >= 0 && p.position <= 100))
        .collect(Collectors.toList());
  }

  /**
   * Convert a <code>position</code> into seconds in relation to the given track.
   * <em>Attention:</em> The function does not check if the calculated absolute position is within
   * the bounds of the tracks length.
   */
  static double toSeconds(Track track, MediaPosition position, double endMarginMs) {
    final long durationMs = track.getDuration() == null ? 0 : track.getDuration();
    final double posMs;
    switch (position.type) {
      case Percentage:
        posMs = durationMs * position.position / 100.0;
        break;
      case Seconds:
        posMs = position.position * 1000.0;
        break;
      default:
        throw new IllegalArgumentException("Unhandled MediaPosition type: " + position.type);
    }
    // limit maximum position to Xms before the end of the video
    return Math.abs(durationMs - posMs) >= endMarginMs
            ? posMs / 1000.0
            : Math.max(0, ((double) durationMs - endMarginMs)) / 1000.0;
  }

  // ** ** **
  /**
   * Fetch a profile from the composer service. Throw a WorkflowOperationException in case the profile
   * does not exist.
   */
  public static Function<String, EncodingProfile> fetchProfile(final ComposerService composerService) {
    return new Function<String, EncodingProfile>() {
      @Override
      public EncodingProfile apply(String profileName) {
        final EncodingProfile profile = composerService.getProfile(profileName);
        if (profile != null) {
          return profile;
        } else {
          throw new RuntimeException(new WorkflowOperationException("Encoding profile '" + profileName + "' was not found"));
        }
      }
    };
  }

  /**
   * Describes the extraction of a list of images from a track, extracted after a certain encoding profile.
   * Track -> (profile, positions)
   */
  static final class Extraction {
    /** The extraction job. */
    private final Job job;
    /** The track to extract from. */
    private final Track track;
    /** The encoding profile to use for extraction. */
    private final EncodingProfile profile;
    /** Media positions. */
    private final List<MediaPosition> positions;

    private Extraction(Job job, Track track, EncodingProfile profile, List<MediaPosition> positions) {
      this.job = job;
      this.track = track;
      this.profile = profile;
      this.positions = positions;
    }
  }

  // ** ** **

  /**
   * The WOH's configuration options.
   */
  static final class Cfg {
    /** List of source tracks, with duration. */
    private final List<Track> sourceTracks;
    private final List<MediaPosition> positions;
    private final List<EncodingProfile> profiles;
    private final List<MediaPackageElementFlavor> targetImageFlavor;
    private final List<String> targetImageTags;
    private final Optional<String> targetBaseNameFormatSecond;
    private final Optional<String> targetBaseNameFormatPercent;
    private final long endMargin;

    Cfg(List<Track> sourceTracks,
        List<MediaPosition> positions,
        List<EncodingProfile> profiles,
        List<MediaPackageElementFlavor> targetImageFlavor,
        List<String> targetImageTags,
        Optional<String> targetBaseNameFormatSecond,
        Optional<String> targetBaseNameFormatPercent,
        long endMargin) {
      this.sourceTracks = sourceTracks;
      this.positions = positions;
      this.profiles = profiles;
      this.targetImageFlavor = targetImageFlavor;
      this.targetImageTags = targetImageTags;
      this.endMargin = endMargin;
      this.targetBaseNameFormatSecond = targetBaseNameFormatSecond;
      this.targetBaseNameFormatPercent = targetBaseNameFormatPercent;
    }
  }

  /** Get and parse the configuration options. */
  private Cfg configure(MediaPackage mp, WorkflowInstance wi) throws WorkflowOperationException {
    WorkflowOperationInstance woi = wi.getCurrentOperation();
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(wi,
        Configuration.many, Configuration.many, Configuration.many, Configuration.one);
    final List<EncodingProfile> profiles = getOptConfig(woi, OPT_PROFILES)
        .map(configString -> asList(configString))
        .orElseGet(java.util.Collections::emptyList)
        .stream()
        .map(fetchProfile(composerService))
        .collect(Collectors.toList());
    final List<String> targetImageTags = tagsAndFlavors.getTargetTags();
    final List<MediaPackageElementFlavor> targetImageFlavor = tagsAndFlavors.getTargetFlavors();
    final List<Track> sourceTracks;
    {
      // get the source tags
      final List<String> sourceTags = tagsAndFlavors.getSrcTags();
      final List<MediaPackageElementFlavor> sourceFlavors = tagsAndFlavors.getSrcFlavors();
      TrackSelector trackSelector = new TrackSelector();

      //add tags and flavors to TrackSelector
      for (String tag : sourceTags) {
        trackSelector.addTag(tag);
      }
      for (MediaPackageElementFlavor flavor : sourceFlavors) {
        trackSelector.addFlavor(flavor);
      }

      // select the tracks based on source flavors and tags and skip those that don't have video
      sourceTracks = trackSelector.select(mp, true).stream()
          .filter(Track::hasVideo)
          .collect(Collectors.toList());
    }
    final List<MediaPosition> positions = MediaPositionParser.parsePositions(getConfig(woi, OPT_POSITIONS));
    final long endMargin = getOptConfig(woi, OPT_END_MARGIN)
        .flatMap(s -> {
          try {
            return Optional.of(Long.parseLong(s));
          } catch (NumberFormatException e) {
            return Optional.empty();
          }
        })
        .orElse(END_MARGIN_DEFAULT);
    //
    return new Cfg(sourceTracks,
                   positions,
                   profiles,
                   targetImageFlavor,
                   targetImageTags,
                   getTargetBaseNameFormat(woi, OPT_TARGET_BASE_NAME_FORMAT_SECOND),
                   getTargetBaseNameFormat(woi, OPT_TARGET_BASE_NAME_FORMAT_PERCENT),
                   endMargin);
  }

  /** Validate a target base name format. */
  private Optional<String> getTargetBaseNameFormat(WorkflowOperationInstance woi, final String formatName) {
    Optional<String> opt = getOptConfig(woi, formatName);
    opt.ifPresent(validateTargetBaseNameFormat(formatName));
    return opt;
  }

  static Consumer<String> validateTargetBaseNameFormat(final String formatName) {
    return format -> {
      boolean valid;
      try {
        final String name = formatFileName(format, 15.11, ".png");
        valid = name.contains(".") && name.contains(".png");
      } catch (IllegalFormatException e) {
        valid = false;
      }
      if (!valid) {
        throw new RuntimeException(new WorkflowOperationException(String.format(
            "%s is not a valid format string for config option %s",
            format, formatName)));
      }
    };
  }

  // ** ** **

  /**
   * Parse media position parameter strings.
   */
  public final class MediaPositionParser {
    private MediaPositionParser() { }

    public static List<MediaPosition> parsePositions(String input) throws WorkflowOperationException {
      if (input == null || input.trim().isEmpty()) {
        throw new WorkflowOperationException("Cannot parse empty or blank time string");
      }

      List<MediaPosition> positions = new ArrayList<>();
      String[] tokens = input.trim().split("[,\\s]+");

      for (String token : tokens) {
        MediaPosition position = parseToken(token);
        if (position == null) {
          throw new WorkflowOperationException("Cannot parse time string: " + input);
        }
        positions.add(position);
      }
      return positions;
    }

    private static MediaPosition parseToken(String token) {
      token = token.trim();
      if (token.endsWith("%")) {
        try {
          double value = Double.parseDouble(token.substring(0, token.length() - 1));
          return new MediaPosition(PositionType.Percentage, value);
        } catch (NumberFormatException e) {
          return null;
        }
      } else {
        try {
          double value = Double.parseDouble(token);
          return new MediaPosition(PositionType.Seconds, value);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
  }

  enum PositionType {
    Percentage, Seconds
  }

  /**
   * A position in time in a media file.
   */
  static final class MediaPosition {
    private double position;
    private final PositionType type;

    MediaPosition(PositionType type, double position) {
      this.position = position;
      this.type = type;
    }

    public void setPosition(double position) {
      this.position = position;
    }

    @Override public int hashCode() {
      return Objects.hash(position, type);
    }

    @Override public boolean equals(Object that) {
      return (this == that) || (that instanceof MediaPosition && eqFields((MediaPosition) that));
    }

    private boolean eqFields(MediaPosition that) {
      return position == that.position && eq(type, that.type);
    }

    @Override public String toString() {
      return format("MediaPosition(%s, %s)", type, position);
    }
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}

