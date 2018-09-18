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

import static com.entwinemedia.fn.Equality.hash;
import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Prelude.unexhaustiveMatchError;
import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.parser.Parsers.character;
import static com.entwinemedia.fn.parser.Parsers.many;
import static com.entwinemedia.fn.parser.Parsers.opt;
import static com.entwinemedia.fn.parser.Parsers.space;
import static com.entwinemedia.fn.parser.Parsers.symbol;
import static com.entwinemedia.fn.parser.Parsers.token;
import static com.entwinemedia.fn.parser.Parsers.yield;
import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.MediaPackageSupport.Filters;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.P2;
import com.entwinemedia.fn.Prelude;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamFold;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;
import com.entwinemedia.fn.parser.Parser;
import com.entwinemedia.fn.parser.Parsers;
import com.entwinemedia.fn.parser.Result;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.SortedMap;
import java.util.UUID;

/**
 * The workflow definition for handling "image" operations
 */
public class ImageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageWorkflowOperationHandler.class);

  // legacy option
  public static final String OPT_SOURCE_FLAVOR = "source-flavor";
  public static final String OPT_SOURCE_FLAVORS = "source-flavors";
  public static final String OPT_SOURCE_TAGS = "source-tags";
  public static final String OPT_PROFILES = "encoding-profile";
  public static final String OPT_POSITIONS = "time";
  public static final String OPT_TARGET_FLAVOR = "target-flavor";
  public static final String OPT_TARGET_TAGS = "target-tags";
  public static final String OPT_TARGET_BASE_NAME_FORMAT_SECOND = "target-base-name-format-second";
  public static final String OPT_TARGET_BASE_NAME_FORMAT_PERCENT = "target-base-name-format-percent";
  public static final String OPT_END_MARGIN = "end-margin";

  private static final long END_MARGIN_DEFAULT = 100;

  /** The configuration options for this handler */
  @SuppressWarnings("unchecked")
  private static final SortedMap<String, String> CONFIG_OPTIONS = Collections.smap(
          tuple(OPT_SOURCE_FLAVOR, "The \"flavor\" of the track to use as a video source input"),
          tuple(OPT_SOURCE_FLAVORS, "The \"flavors\" of the track to use as a video source input"),
          tuple(OPT_SOURCE_TAGS,
                "The required tags that must exist on the track for the track to be used as a video source"),
          tuple(OPT_PROFILES, "The encoding profile to use"),
          tuple(OPT_POSITIONS, "The number of seconds into the video file to extract the image"),
          tuple(OPT_TARGET_FLAVOR, "The flavor to apply to the extracted image"),
          tuple(OPT_TARGET_TAGS, "The tags to apply to the extracted image"),
          tuple(OPT_TARGET_BASE_NAME_FORMAT_SECOND, "TODO"), // todo description
          tuple(OPT_TARGET_BASE_NAME_FORMAT_PERCENT, "The target base name pattern for seconds 'thumbnail' or 'extracted'."), //todo description
          tuple(OPT_END_MARGIN, "A margin in milliseconds at the end of the video. Each position is "
                  + "limited to not exceed this bound. Defaults to " + END_MARGIN_DEFAULT + "ms."));

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

  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance wi, JobContext ctx)
          throws WorkflowOperationException {
    logger.debug("Running image workflow operation on {}", wi);
    try {
      final Extractor e = new Extractor(this, configure(wi.getMediaPackage(), wi.getCurrentOperation()));
      return e.main(MediaPackageSupport.copy(wi.getMediaPackage()));
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
      final List<Extraction> extractions = $(cfg.sourceTracks).bind(new Fn<Track, Stream<Extraction>>() {
        @Override public Stream<Extraction> apply(final Track t) {
          final List<MediaPosition> p = limit(t, cfg.positions);
          if (p.size() != cfg.positions.size()) {
            logger.warn("Could not apply all configured positions to track " + t);
          } else {
            logger.info(format("Extracting images from %s at position %s", t, $(p).mkString(", ")));
          }
          // create one extraction per encoding profile
          return $(cfg.profiles).map(new Fn<EncodingProfile, Extraction>() {
            @Override public Extraction apply(EncodingProfile profile) {
              return new Extraction(extractImages(t, profile, p), t, profile, p);
            }
          });
        }
      }).toList();
      final List<Job> extractionJobs = concatJobs(extractions);
      final JobBarrier.Result extractionResult = JobUtil.waitForJobs(handler.serviceRegistry, extractionJobs);
      if (extractionResult.isSuccess()) {
        // all extractions were successful; iterate them
        for (final Extraction extraction : extractions) {
          final List<Attachment> images = getImages(extraction.job);
          final int expectedNrOfImages = extraction.positions.size();
          if (images.size() == expectedNrOfImages) {
            // post process images
            for (final P2<Attachment, MediaPosition> image : $(images).zip(extraction.positions)) {
              adjustMetadata(extraction, image.get1());
              if (image.get1().getIdentifier() == null) image.get1().setIdentifier(UUID.randomUUID().toString());
              mp.addDerived(image.get1(), extraction.track);
              final String fileName = createFileName(
                      extraction.profile.getSuffix(), extraction.track.getURI(), image.get2());
              moveToWorkspace(mp, image.get1(), fileName);
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
          format = cfg.targetBaseNameFormatSecond.getOr(trackBaseName + "_%.3fs%s");
          break;
        case Percentage:
          format = cfg.targetBaseNameFormatPercent.getOr(trackBaseName + "_%.1fp%s");
          break;
        default:
          throw unexhaustiveMatchError();
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
        chuck(new WorkflowOperationException(e));
      }
    }

    /** Start a composer job to extract images from a track at the given positions. */
    private Job extractImages(final Track track, final EncodingProfile profile, final List<MediaPosition> positions) {
      final List<Double> p = $(positions).map(new Fn<MediaPosition, Double>() {
        @Override public Double apply(MediaPosition mediaPosition) {
          return toSeconds(track, mediaPosition, cfg.endMargin);
        }
      }).toList();
      try {
        return handler.composerService.image(track, profile.getIdentifier(), Collections.toDoubleArray(p));
      } catch (Exception e) {
        return chuck(new WorkflowOperationException("Error starting image extraction job", e));
      }
    }
  }

  // ** ** **

  /**
   * Format a filename and make it "safe".
   *
   * @see org.opencastproject.util.PathSupport#toSafeName(String)
   */
  static String formatFileName(String format, double position, String suffix) {
    // #toSafeName will be applied to the file name anyway when moving to the working file repository
    // but doing it here make the tests more readable and useful for documentation
    return PathSupport.toSafeName(format(format, position, suffix));
  }


  /** Concat the jobs of a list of extraction objects. */
  private static List<Job> concatJobs(List<Extraction> extractions) {
    return $(extractions).map(new Fn<Extraction, Job>() {
      @Override public Job apply(Extraction extraction) {
        return extraction.job;
      }
    }).toList();
  }

  /** Get the images (payload) from a job. */
  @SuppressWarnings("unchecked")
  private static List<Attachment> getImages(Job job) {
    final List<Attachment> images;
    try {
      images = (List<Attachment>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
    } catch (MediaPackageException e) {
      return chuck(e);
    }
    if (!images.isEmpty()) {
      return images;
    } else {
      return chuck(new WorkflowOperationException("Job did not extract any images"));
    }
  }

  /** Limit the list of media positions to those that fit into the length of the track. */
  static List<MediaPosition> limit(Track track, List<MediaPosition> positions) {
    final long duration = track.getDuration();
    return $(positions).filter(new Fn<MediaPosition, Boolean>() {
      @Override public Boolean apply(MediaPosition p) {
        return !(
                (eq(p.type, PositionType.Seconds) && (p.position >= duration || p.position < 0.0))
                        ||
                        (eq(p.type, PositionType.Percentage) && (p.position < 0.0 || p.position > 100.0)));
      }
    }).toList();
  }

  /**
   * Convert a <code>position</code> into seconds in relation to the given track.
   * <em>Attention:</em> The function does not check if the calculated absolute position is within
   * the bounds of the tracks length.
   */
  static double toSeconds(Track track, MediaPosition position, double endMarginMs) {
    final long durationMs = track.getDuration();
    final double posMs;
    switch (position.type) {
      case Percentage:
        posMs = durationMs * position.position / 100.0;
        break;
      case Seconds:
        posMs = position.position * 1000.0;
        break;
      default:
        throw unexhaustiveMatchError();
    }
    // limit maximum position to Xms before the end of the video
    return Math.abs(durationMs - posMs) >= endMarginMs
            ? posMs / 1000.0
            : Math.max(0, ((double) durationMs - endMarginMs)) / 1000.0;
  }

  // ** ** **

  /** Create a fold that folds flavors into a media package element selector. */
  public static <E extends MediaPackageElement, S extends AbstractMediaPackageElementSelector<E>>
  StreamFold<MediaPackageElementFlavor, S> flavorFold(S selector) {
    return StreamFold.foldl(selector, new Fn2<S, MediaPackageElementFlavor, S>() {
      @Override public S apply(S sum, MediaPackageElementFlavor flavor) {
        sum.addFlavor(flavor);
        return sum;
      }
    });
  }

  /** Create a fold that folds tags into a media package element selector. */
  public static <E extends MediaPackageElement, S extends AbstractMediaPackageElementSelector<E>>
  StreamFold<String, S> tagFold(S selector) {
    return StreamFold.foldl(selector, new Fn2<S, String, S>() {
      @Override public S apply(S sum, String tag) {
        sum.addTag(tag);
        return sum;
      }
    });
  }

  /**
   * Fetch a profile from the composer service. Throw a WorkflowOperationException in case the profile
   * does not exist.
   */
  public static Fn<String, EncodingProfile> fetchProfile(final ComposerService composerService) {
    return new Fn<String, EncodingProfile>() {
      @Override public EncodingProfile apply(String profileName) {
        final EncodingProfile profile = composerService.getProfile(profileName);
        return profile != null
                ? profile
                : Prelude.<EncodingProfile>chuck(new WorkflowOperationException("Encoding profile '" + profileName + "' was not found"));
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
    private final Opt<MediaPackageElementFlavor> targetImageFlavor;
    private final List<String> targetImageTags;
    private final Opt<String> targetBaseNameFormatSecond;
    private final Opt<String> targetBaseNameFormatPercent;
    private final long endMargin;

    Cfg(List<Track> sourceTracks,
        List<MediaPosition> positions,
        List<EncodingProfile> profiles,
        Opt<MediaPackageElementFlavor> targetImageFlavor,
        List<String> targetImageTags,
        Opt<String> targetBaseNameFormatSecond,
        Opt<String> targetBaseNameFormatPercent,
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
  private Cfg configure(MediaPackage mp, WorkflowOperationInstance woi) throws WorkflowOperationException {
    final List<EncodingProfile> profiles = getOptConfig(woi, OPT_PROFILES).toStream().bind(asList.toFn())
            .map(fetchProfile(composerService)).toList();
    final List<String> targetImageTags = getOptConfig(woi, OPT_TARGET_TAGS).toStream().bind(asList.toFn()).toList();
    final Opt<MediaPackageElementFlavor> targetImageFlavor =
            getOptConfig(woi, OPT_TARGET_FLAVOR).map(MediaPackageElementFlavor.parseFlavor.toFn());
    final List<Track> sourceTracks;
    {
      // get the source flavors
      final Stream<MediaPackageElementFlavor> sourceFlavors = getOptConfig(woi, OPT_SOURCE_FLAVORS).toStream()
              .bind(Strings.splitCsv)
              .append(getOptConfig(woi, OPT_SOURCE_FLAVOR))
              .map(MediaPackageElementFlavor.parseFlavor.toFn());
      // get the source tags
      final Stream<String> sourceTags = getOptConfig(woi, OPT_SOURCE_TAGS).toStream().bind(Strings.splitCsv);
      // fold both into a selector
      final TrackSelector trackSelector = sourceTags.apply(tagFold(sourceFlavors.apply(flavorFold(new TrackSelector()))));
      // select the tracks based on source flavors and tags and skip those that don't have video
      sourceTracks = $(trackSelector.select(mp, true))
              .filter(Filters.hasVideo.toFn())
              .each(new Fx<Track>() {
                @Override public void apply(Track track) {
                  if (track.getDuration() == null) {
                    chuck(new WorkflowOperationException(format("Track %s cannot tell its duration", track)));
                  }
                }
              }).toList();
    }
    final List<MediaPosition> positions = parsePositions(getConfig(woi, OPT_POSITIONS));
    final long endMargin = getOptConfig(woi, OPT_END_MARGIN).bind(Strings.toLong).getOr(END_MARGIN_DEFAULT);
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
  private Opt<String> getTargetBaseNameFormat(WorkflowOperationInstance woi, final String formatName) {
    return getOptConfig(woi, formatName).each(validateTargetBaseNameFormat(formatName));
  }

  static Fx<String> validateTargetBaseNameFormat(final String formatName) {
    return new Fx<String>() {
      @Override public void apply(String format) {
        boolean valid;
        try {
          final String name = formatFileName(format, 15.11, ".png");
          valid = name.contains(".") && name.contains(".png");
        } catch (IllegalFormatException e) {
          valid = false;
        }
        if (!valid) {
          chuck(new WorkflowOperationException(format(
                  "%s is not a valid format string for config option %s",
                  format, formatName)));
        }
      }
    };
  }

  // ** ** **

  /**
   * Parse media position parameter strings.
   */
  static final class MediaPositionParser {
    private MediaPositionParser() {
    }

    static final Parser<Double> number = token(Parsers.dbl);
    static final Parser<MediaPosition> seconds = number.bind(new Fn<Double, Parser<MediaPosition>>() {
      @Override public Parser<MediaPosition> apply(Double p) {
        return yield(new MediaPosition(PositionType.Seconds, p));
      }
    });
    static final Parser<MediaPosition> percentage =
            number.bind(Parsers.<Double, String>ignore(symbol("%"))).bind(new Fn<Double, Parser<MediaPosition>>() {
              @Override public Parser<MediaPosition> apply(Double p) {
                return yield(new MediaPosition(PositionType.Percentage, p));
              }
            });
    static final Parser<Character> comma = token(character(','));
    static final Parser<Character> ws = token(space);
    static final Parser<MediaPosition> position = percentage.or(seconds);

    /** Main parser. */
    static final Parser<List<MediaPosition>> positions =
            position.bind(new Fn<MediaPosition, Parser<List<MediaPosition>>>() {
              // first position
              @Override public Parser<List<MediaPosition>> apply(final MediaPosition first) {
                // following
                return many(opt(comma).bind(Parsers.ignorePrevious(position)))
                        .bind(new Fn<List<MediaPosition>, Parser<List<MediaPosition>>>() {
                          @Override public Parser<List<MediaPosition>> apply(List<MediaPosition> rest) {
                            return yield($(first).append(rest).toList());
                          }
                        });
              }
            });
  }

  private List<MediaPosition> parsePositions(String time) throws WorkflowOperationException {
    final Result<List<MediaPosition>> r = MediaPositionParser.positions.parse(time);
    if (r.isDefined() && r.getRest().isEmpty()) {
      return r.getResult();
    } else {
      throw new WorkflowOperationException(format("Cannot parse time string %s. Rest is %s", time, r.getRest()));
    }
  }

  enum PositionType {
    Percentage, Seconds
  }

  /**
   * A position in time in a media file.
   */
  static final class MediaPosition {
    private final double position;
    private final PositionType type;

    MediaPosition(PositionType type, double position) {
      this.position = position;
      this.type = type;
    }

    @Override public int hashCode() {
      return hash(position, type);
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
}

