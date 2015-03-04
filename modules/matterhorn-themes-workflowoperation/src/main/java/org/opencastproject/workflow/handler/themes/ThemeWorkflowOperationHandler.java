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
package org.opencastproject.workflow.handler.themes;

import static java.lang.String.format;
import static org.opencastproject.composer.layout.Offset.offset;

import org.opencastproject.composer.layout.AbsolutePositionLayoutSpec;
import org.opencastproject.composer.layout.AnchorOffset;
import org.opencastproject.composer.layout.Anchors;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.staticfiles.api.StaticFileService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The workflow definition for handling "theme" operations
 */
public class ThemeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String BUMPER_FLAVOR = "bumper-flavor";
  private static final String BUMPER_TAGS = "bumper-tags";

  private static final String TRAILER_FLAVOR = "trailer-flavor";
  private static final String TRAILER_TAGS = "trailer-tags";

  private static final String TITLE_SLIDE_FLAVOR = "title-slide-flavor";
  private static final String TITLE_SLIDE_TAGS = "title-slide-tags";

  private static final String LICENSE_SLIDE_FLAVOR = "license-slide-flavor";
  private static final String LICENSE_SLIDE_TAGS = "license-slide-tags";

  private static final String WATERMARK_FLAVOR = "watermark-flavor";
  private static final String WATERMARK_TAGS = "watermark-tags";
  private static final String WATERMARK_LAYOUT = "watermark-layout";
  private static final String WATERMARK_LAYOUT_VARIABLE = "watermark-layout-variable";

  /** The series theme property name */
  private static final String THEME_PROPERTY_NAME = "theme";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ThemeWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  private static final MediaPackageElementBuilderFactory elementBuilderFactory = MediaPackageElementBuilderFactory
          .newInstance();

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(BUMPER_FLAVOR, "The flavor to apply to the added bumper element");
    CONFIG_OPTIONS.put(BUMPER_TAGS, "The tags to apply to the added bumper element");
    CONFIG_OPTIONS.put(TRAILER_FLAVOR, "The flavor to apply to the added trailer element");
    CONFIG_OPTIONS.put(TRAILER_TAGS, "The tags to apply to the added trailer element");
    CONFIG_OPTIONS.put(TITLE_SLIDE_FLAVOR, "The flavor to apply to the added title slide element");
    CONFIG_OPTIONS.put(TITLE_SLIDE_TAGS, "The tags to apply to the added title slide element");
    CONFIG_OPTIONS.put(LICENSE_SLIDE_FLAVOR, "The flavor to apply to the added license slide element");
    CONFIG_OPTIONS.put(LICENSE_SLIDE_TAGS, "The tags to apply to the added license slide element");
    CONFIG_OPTIONS.put(WATERMARK_FLAVOR, "The flavor to apply to the added watermark element");
    CONFIG_OPTIONS.put(WATERMARK_TAGS, "The tags to apply to the added watermark element");
    CONFIG_OPTIONS.put(WATERMARK_LAYOUT, "The layout to adjust by the watermark position");
    CONFIG_OPTIONS.put(WATERMARK_LAYOUT_VARIABLE, "The workflow variable where the adjusted layout is stored");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /** The series service */
  private SeriesService seriesService;

  /** The themes database service */
  private ThemesServiceDatabase themesServiceDatabase;

  /** The static file service */
  private StaticFileService staticFileService;

  /** The workspace */
  private Workspace workspace;

  /** OSGi callback for the series service. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi callback for the themes database service. */
  public void setThemesServiceDatabase(ThemesServiceDatabase themesServiceDatabase) {
    this.themesServiceDatabase = themesServiceDatabase;
  }

  /** OSGi callback for the static file service. */
  public void setStaticFileService(StaticFileService staticFileService) {
    this.staticFileService = staticFileService;
  }

  /** OSGi callback for the workspace. */
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
    logger.debug("Running theme workflow operation on workflow {}", workflowInstance.getId());

    final MediaPackageElementFlavor bumperFlavor = getOptConfig(workflowInstance, BUMPER_FLAVOR).map(
            toMediaPackageElementFlavor).or(new MediaPackageElementFlavor("branding", "bumper"));
    final MediaPackageElementFlavor trailerFlavor = getOptConfig(workflowInstance, TRAILER_FLAVOR).map(
            toMediaPackageElementFlavor).or(new MediaPackageElementFlavor("branding", "trailer"));
    final MediaPackageElementFlavor titleSlideFlavor = getOptConfig(workflowInstance, TITLE_SLIDE_FLAVOR).map(
            toMediaPackageElementFlavor).or(new MediaPackageElementFlavor("branding", "title-slide"));
    final MediaPackageElementFlavor licenseSlideFlavor = getOptConfig(workflowInstance, LICENSE_SLIDE_FLAVOR).map(
            toMediaPackageElementFlavor).or(new MediaPackageElementFlavor("branding", "license-slide"));
    final MediaPackageElementFlavor watermarkFlavor = getOptConfig(workflowInstance, WATERMARK_FLAVOR).map(
            toMediaPackageElementFlavor).or(new MediaPackageElementFlavor("branding", "watermark"));
    final List<String> bumperTags = asList(workflowInstance.getConfiguration(BUMPER_TAGS));
    final List<String> trailerTags = asList(workflowInstance.getConfiguration(TRAILER_TAGS));
    final List<String> titleSlideTags = asList(workflowInstance.getConfiguration(TITLE_SLIDE_TAGS));
    final List<String> licenseSlideTags = asList(workflowInstance.getConfiguration(LICENSE_SLIDE_TAGS));
    final List<String> watermarkTags = asList(workflowInstance.getConfiguration(WATERMARK_TAGS));

    Opt<String> layoutStringOpt = getOptConfig(workflowInstance, WATERMARK_LAYOUT);
    Opt<String> watermarkLayoutVariable = getOptConfig(workflowInstance, WATERMARK_LAYOUT_VARIABLE);

    List<String> layoutList = new ArrayList<>(Stream.$(layoutStringOpt).bind(Strings.split(";")).toList());

    try {
      MediaPackage mediaPackage = workflowInstance.getMediaPackage();
      String series = mediaPackage.getSeries();
      if (series == null) {
        logger.info("Skipping theme workflow operation, no series assigned to mediapackage {}",
                mediaPackage.getIdentifier());
        return createResult(Action.SKIP);
      }

      Long themeId;
      try {
        themeId = Long.parseLong(seriesService.getSeriesProperty(series, THEME_PROPERTY_NAME));
      } catch (NotFoundException e) {
        logger.info("Skipping theme workflow operation, no theme assigned to series {} on mediapackage {}.", series,
                mediaPackage.getIdentifier());
        return createResult(Action.SKIP);
      } catch (UnauthorizedException e) {
        logger.warn("Skipping theme workflow operation, user not authorized to perform operation: {}",
                ExceptionUtils.getStackTrace(e));
        return createResult(Action.SKIP);
      }

      Theme theme;
      try {
        theme = themesServiceDatabase.getTheme(themeId);
      } catch (NotFoundException e) {
        logger.warn("Skipping theme workflow operation, no theme with id {} found.", themeId);
        return createResult(Action.SKIP);
      }

      logger.info("Applying theme {} to mediapackage {}", themeId, mediaPackage.getIdentifier());

      if (theme.isBumperActive() && StringUtils.isNotBlank(theme.getBumperFile())) {
        try (InputStream bumper = staticFileService.getFile(theme.getBumperFile())) {
          addElement(mediaPackage, bumperFlavor, bumperTags, bumper,
                  staticFileService.getFileName(theme.getBumperFile()), Type.Track);
        } catch (NotFoundException e) {
          logger.warn("Bumper file {} not found in static file service, skip applying it", theme.getBumperFile());
        }
      }

      if (theme.isTrailerActive() && StringUtils.isNotBlank(theme.getTrailerFile())) {
        try (InputStream trailer = staticFileService.getFile(theme.getTrailerFile())) {
          addElement(mediaPackage, trailerFlavor, trailerTags, trailer,
                  staticFileService.getFileName(theme.getTrailerFile()), Type.Track);
        } catch (NotFoundException e) {
          logger.warn("Trailer file {} not found in static file service, skip applying it", theme.getTrailerFile());
        }
      }

      if (theme.isTitleSlideActive()) {
        if (StringUtils.isNotBlank(theme.getTitleSlideBackground())) {
          try (InputStream titleSlideBackground = staticFileService.getFile(theme.getTitleSlideBackground())) {
            addElement(mediaPackage, titleSlideFlavor, titleSlideTags, titleSlideBackground,
                    staticFileService.getFileName(theme.getTitleSlideBackground()), Type.Attachment);
          } catch (NotFoundException e) {
            logger.warn("Title slide file {} not found in static file service, skip applying it",
                    theme.getTitleSlideBackground());
          }
        } else {
          // TODO define what to do here (maybe extract image as background)
        }

        // TODO add the title slide metadata to the workflow properties to be used by the cover-image WOH
        // String titleSlideMetadata = theme.getTitleSlideMetadata();
      }

      if (theme.isLicenseSlideActive()) {
        if (StringUtils.isNotBlank(theme.getLicenseSlideBackground())) {
          try (InputStream licenseSlideBackground = staticFileService.getFile(theme.getLicenseSlideBackground())) {
            addElement(mediaPackage, licenseSlideFlavor, licenseSlideTags, licenseSlideBackground,
                    staticFileService.getFileName(theme.getLicenseSlideBackground()), Type.Attachment);
          } catch (NotFoundException e) {
            logger.warn("License slide file {} not found in static file service, skip applying it",
                    theme.getLicenseSlideBackground());
          }
        } else {
          // TODO define what to do here (maybe extract image as background)
        }

        // TODO add the license slide description to the workflow properties to be used by the cover-image WOH
        // String licenseSlideDescription = theme.getLicenseSlideDescription();
      }

      if (theme.isWatermarkActive() && StringUtils.isNotBlank(theme.getWatermarkFile())) {
        try (InputStream watermark = staticFileService.getFile(theme.getWatermarkFile())) {
          addElement(mediaPackage, watermarkFlavor, watermarkTags, watermark,
                  staticFileService.getFileName(theme.getWatermarkFile()), Type.Attachment);
        } catch (NotFoundException e) {
          logger.warn("Watermark file {} not found in static file service, skip applying it", theme.getWatermarkFile());
        }

        if (layoutStringOpt.isNone() || watermarkLayoutVariable.isNone())
          throw new WorkflowOperationException(format("Configuration key '%s' or '%s' is either missing or empty",
                  WATERMARK_LAYOUT, WATERMARK_LAYOUT_VARIABLE));

        AbsolutePositionLayoutSpec watermarkLayout = parseLayout(theme.getWatermarkPosition());
        layoutList.set(layoutList.size() - 1, Serializer.json(watermarkLayout).toJson());
        layoutStringOpt = Opt.some(Stream.$(layoutList).mkString(";"));
      }

      if (watermarkLayoutVariable.isSome() && layoutStringOpt.isSome())
        workflowInstance.setConfiguration(watermarkLayoutVariable.get(), layoutStringOpt.get());

      return createResult(mediaPackage, Action.CONTINUE);
    } catch (SeriesException | ThemesServiceDatabaseException | IllegalStateException | IllegalArgumentException
            | IOException e) {
      throw new WorkflowOperationException(e);
    }
  }

  private AbsolutePositionLayoutSpec parseLayout(String watermarkPosition) {
    switch (watermarkPosition) {
      case "topLeft":
        return new AbsolutePositionLayoutSpec(new AnchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, offset(20, 20)));
      case "topRight":
        return new AbsolutePositionLayoutSpec(new AnchorOffset(Anchors.TOP_RIGHT, Anchors.TOP_RIGHT, offset(-20, 20)));
      case "bottomLeft":
        return new AbsolutePositionLayoutSpec(new AnchorOffset(Anchors.BOTTOM_LEFT, Anchors.BOTTOM_LEFT,
                offset(20, -20)));
      case "bottomRight":
        return new AbsolutePositionLayoutSpec(new AnchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, offset(-20,
                -20)));
      default:
        throw new IllegalStateException("Unknown watermark position: " + watermarkPosition);
    }
  }

  private void addElement(MediaPackage mediaPackage, final MediaPackageElementFlavor flavor, final List<String> tags,
          InputStream file, String filename, Type type) throws IOException {
    MediaPackageElement element = elementBuilderFactory.newElementBuilder().newElement(type, flavor);
    element.setIdentifier(UUID.randomUUID().toString());
    for (String tag : tags) {
      element.addTag(tag);
    }
    URI uri = workspace.put(mediaPackage.getIdentifier().compact(), element.getIdentifier(), filename, file);
    element.setURI(uri);
    try {
      MimeType mimeType = MimeTypes.fromString(filename);
      element.setMimeType(mimeType);
    } catch (UnknownFileTypeException e) {
      logger.warn("Unable to detect the mime type of file {}", filename);
    }
    mediaPackage.add(element);
  }

  private static Fn<String, MediaPackageElementFlavor> toMediaPackageElementFlavor = new Fn<String, MediaPackageElementFlavor>() {
    @Override
    public MediaPackageElementFlavor ap(String flavorString) {
      return MediaPackageElementFlavor.parseFlavor(flavorString);
    }
  };

}
