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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.themes

import java.lang.String.format
import org.opencastproject.composer.layout.Offset.offset

import org.opencastproject.composer.layout.AbsolutePositionLayoutSpec
import org.opencastproject.composer.layout.AnchorOffset
import org.opencastproject.composer.layout.Anchors
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.staticfiles.api.StaticFileService
import org.opencastproject.themes.Theme
import org.opencastproject.themes.ThemesServiceDatabase
import org.opencastproject.themes.persistence.ThemesServiceDatabaseException
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Strings

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.UUID

/**
 * The workflow definition for handling "theme" operations
 */
class ThemeWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The series service  */
    private var seriesService: SeriesService? = null

    /** The themes database service  */
    private var themesServiceDatabase: ThemesServiceDatabase? = null

    /** The static file service  */
    private var staticFileService: StaticFileService? = null

    /** The workspace  */
    private var workspace: Workspace? = null

    /** OSGi callback for the series service.  */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /** OSGi callback for the themes database service.  */
    fun setThemesServiceDatabase(themesServiceDatabase: ThemesServiceDatabase) {
        this.themesServiceDatabase = themesServiceDatabase
    }

    /** OSGi callback for the static file service.  */
    fun setStaticFileService(staticFileService: StaticFileService) {
        this.staticFileService = staticFileService
    }

    /** OSGi callback for the workspace.  */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running theme workflow operation on workflow {}", workflowInstance.id)

        val bumperFlavor = getOptConfig(workflowInstance, BUMPER_FLAVOR).map(
                toMediaPackageElementFlavor).getOr(MediaPackageElementFlavor("branding", "bumper"))
        val trailerFlavor = getOptConfig(workflowInstance, TRAILER_FLAVOR).map(
                toMediaPackageElementFlavor).getOr(MediaPackageElementFlavor("branding", "trailer"))
        val titleSlideFlavor = getOptConfig(workflowInstance, TITLE_SLIDE_FLAVOR).map(
                toMediaPackageElementFlavor).getOr(MediaPackageElementFlavor("branding", "title-slide"))
        val licenseSlideFlavor = getOptConfig(workflowInstance, LICENSE_SLIDE_FLAVOR).map(
                toMediaPackageElementFlavor).getOr(MediaPackageElementFlavor("branding", "license-slide"))
        val watermarkFlavor = getOptConfig(workflowInstance, WATERMARK_FLAVOR).map(
                toMediaPackageElementFlavor).getOr(MediaPackageElementFlavor("branding", "watermark"))
        val bumperTags = asList(workflowInstance.getConfiguration(BUMPER_TAGS))
        val trailerTags = asList(workflowInstance.getConfiguration(TRAILER_TAGS))
        val titleSlideTags = asList(workflowInstance.getConfiguration(TITLE_SLIDE_TAGS))
        val licenseSlideTags = asList(workflowInstance.getConfiguration(LICENSE_SLIDE_TAGS))
        val watermarkTags = asList(workflowInstance.getConfiguration(WATERMARK_TAGS))

        var layoutStringOpt = getOptConfig(workflowInstance, WATERMARK_LAYOUT)
        val watermarkLayoutVariable = getOptConfig(workflowInstance, WATERMARK_LAYOUT_VARIABLE)

        val layoutList = ArrayList(Stream.`$`(layoutStringOpt).bind(Strings.split(";")).toList())

        try {
            val mediaPackage = workflowInstance.mediaPackage
            val series = mediaPackage.series
            if (series == null) {
                logger.info("Skipping theme workflow operation, no series assigned to mediapackage {}",
                        mediaPackage.identifier)
                return createResult(Action.SKIP)
            }

            val themeId: Long?
            try {
                themeId = java.lang.Long.parseLong(seriesService!!.getSeriesProperty(series, THEME_PROPERTY_NAME))
            } catch (e: NotFoundException) {
                logger.info("Skipping theme workflow operation, no theme assigned to series {} on mediapackage {}.", series,
                        mediaPackage.identifier)
                return createResult(Action.SKIP)
            } catch (e: UnauthorizedException) {
                logger.warn("Skipping theme workflow operation, user not authorized to perform operation: {}",
                        ExceptionUtils.getStackTrace(e))
                return createResult(Action.SKIP)
            }

            val theme: Theme
            try {
                theme = themesServiceDatabase!!.getTheme(themeId)
            } catch (e: NotFoundException) {
                logger.warn("Skipping theme workflow operation, no theme with id {} found.", themeId)
                return createResult(Action.SKIP)
            }

            logger.info("Applying theme {} to mediapackage {}", themeId, mediaPackage.identifier)

            /* Make theme settings available to workflow instance */
            workflowInstance.setConfiguration(THEME_ACTIVE, java.lang.Boolean.toString(
                    theme.isBumperActive
                            || theme.isTrailerActive
                            || theme.isTitleSlideActive
                            || theme.isWatermarkActive
            )
            )
            workflowInstance.setConfiguration(THEME_BUMPER_ACTIVE, java.lang.Boolean.toString(theme.isBumperActive))
            workflowInstance.setConfiguration(THEME_TRAILER_ACTIVE, java.lang.Boolean.toString(theme.isTrailerActive))
            workflowInstance.setConfiguration(THEME_TITLE_SLIDE_ACTIVE, java.lang.Boolean.toString(theme.isTitleSlideActive))
            workflowInstance.setConfiguration(THEME_TITLE_SLIDE_UPLOADED, java.lang.Boolean.toString(StringUtils.isNotBlank(theme.titleSlideBackground)))
            workflowInstance.setConfiguration(THEME_WATERMARK_ACTIVE, java.lang.Boolean.toString(theme.isWatermarkActive))

            if (theme.isBumperActive && StringUtils.isNotBlank(theme.bumperFile)) {
                try {
                    staticFileService!!.getFile(theme.bumperFile).use { bumper ->
                        addElement(mediaPackage, bumperFlavor, bumperTags, bumper,
                                staticFileService!!.getFileName(theme.bumperFile), Type.Track)
                    }
                } catch (e: NotFoundException) {
                    logger.warn("Bumper file {} not found in static file service, skip applying it", theme.bumperFile)
                }

            }

            if (theme.isTrailerActive && StringUtils.isNotBlank(theme.trailerFile)) {
                try {
                    staticFileService!!.getFile(theme.trailerFile).use { trailer ->
                        addElement(mediaPackage, trailerFlavor, trailerTags, trailer,
                                staticFileService!!.getFileName(theme.trailerFile), Type.Track)
                    }
                } catch (e: NotFoundException) {
                    logger.warn("Trailer file {} not found in static file service, skip applying it", theme.trailerFile)
                }

            }

            if (theme.isTitleSlideActive) {
                if (StringUtils.isNotBlank(theme.titleSlideBackground)) {
                    try {
                        staticFileService!!.getFile(theme.titleSlideBackground).use { titleSlideBackground ->
                            addElement(mediaPackage, titleSlideFlavor, titleSlideTags, titleSlideBackground,
                                    staticFileService!!.getFileName(theme.titleSlideBackground), Type.Attachment)
                        }
                    } catch (e: NotFoundException) {
                        logger.warn("Title slide file {} not found in static file service, skip applying it",
                                theme.titleSlideBackground)
                    }

                }

                // TODO add the title slide metadata to the workflow properties to be used by the cover-image WOH
                // String titleSlideMetadata = theme.getTitleSlideMetadata();
            }

            if (theme.isLicenseSlideActive) {
                if (StringUtils.isNotBlank(theme.licenseSlideBackground)) {
                    try {
                        staticFileService!!.getFile(theme.licenseSlideBackground).use { licenseSlideBackground ->
                            addElement(mediaPackage, licenseSlideFlavor, licenseSlideTags, licenseSlideBackground,
                                    staticFileService!!.getFileName(theme.licenseSlideBackground), Type.Attachment)
                        }
                    } catch (e: NotFoundException) {
                        logger.warn("License slide file {} not found in static file service, skip applying it",
                                theme.licenseSlideBackground)
                    }

                } else {
                    // TODO define what to do here (maybe extract image as background)
                }

                // TODO add the license slide description to the workflow properties to be used by the cover-image WOH
                // String licenseSlideDescription = theme.getLicenseSlideDescription();
            }

            if (theme.isWatermarkActive && StringUtils.isNotBlank(theme.watermarkFile)) {
                try {
                    staticFileService!!.getFile(theme.watermarkFile).use { watermark ->
                        addElement(mediaPackage, watermarkFlavor, watermarkTags, watermark,
                                staticFileService!!.getFileName(theme.watermarkFile), Type.Attachment)
                    }
                } catch (e: NotFoundException) {
                    logger.warn("Watermark file {} not found in static file service, skip applying it", theme.watermarkFile)
                }

                if (layoutStringOpt.isNone || watermarkLayoutVariable.isNone)
                    throw WorkflowOperationException(format("Configuration key '%s' or '%s' is either missing or empty",
                            WATERMARK_LAYOUT, WATERMARK_LAYOUT_VARIABLE))

                val watermarkLayout = parseLayout(theme.watermarkPosition)
                layoutList[layoutList.size - 1] = Serializer.json(watermarkLayout).toJson()
                layoutStringOpt = Opt.some(Stream.`$`(layoutList).mkString(";"))
            }

            if (watermarkLayoutVariable.isSome && layoutStringOpt.isSome)
                workflowInstance.setConfiguration(watermarkLayoutVariable.get(), layoutStringOpt.get())

            return createResult(mediaPackage, Action.CONTINUE)
        } catch (e: SeriesException) {
            throw WorkflowOperationException(e)
        } catch (e: ThemesServiceDatabaseException) {
            throw WorkflowOperationException(e)
        } catch (e: IllegalStateException) {
            throw WorkflowOperationException(e)
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException(e)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

    }

    private fun parseLayout(watermarkPosition: String): AbsolutePositionLayoutSpec {
        when (watermarkPosition) {
            "topLeft" -> return AbsolutePositionLayoutSpec(AnchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, offset(20, 20)))
            "topRight" -> return AbsolutePositionLayoutSpec(AnchorOffset(Anchors.TOP_RIGHT, Anchors.TOP_RIGHT, offset(-20, 20)))
            "bottomLeft" -> return AbsolutePositionLayoutSpec(AnchorOffset(Anchors.BOTTOM_LEFT, Anchors.BOTTOM_LEFT,
                    offset(20, -20)))
            "bottomRight" -> return AbsolutePositionLayoutSpec(AnchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, offset(-20,
                    -20)))
            else -> throw IllegalStateException("Unknown watermark position: $watermarkPosition")
        }
    }

    @Throws(IOException::class)
    private fun addElement(mediaPackage: MediaPackage, flavor: MediaPackageElementFlavor, tags: List<String>,
                           file: InputStream, filename: String, type: Type) {
        val element = elementBuilderFactory.newElementBuilder()!!.newElement(type, flavor)
        element.identifier = UUID.randomUUID().toString()
        for (tag in tags) {
            element.addTag(tag)
        }
        val uri = workspace!!.put(mediaPackage.identifier.compact(), element.identifier, filename, file)
        element.setURI(uri)
        try {
            val mimeType = MimeTypes.fromString(filename)
            element.mimeType = mimeType
        } catch (e: UnknownFileTypeException) {
            logger.warn("Unable to detect the mime type of file {}", filename)
        }

        mediaPackage.add(element)
    }

    companion object {

        private val BUMPER_FLAVOR = "bumper-flavor"
        private val BUMPER_TAGS = "bumper-tags"

        private val TRAILER_FLAVOR = "trailer-flavor"
        private val TRAILER_TAGS = "trailer-tags"

        private val TITLE_SLIDE_FLAVOR = "title-slide-flavor"
        private val TITLE_SLIDE_TAGS = "title-slide-tags"

        private val LICENSE_SLIDE_FLAVOR = "license-slide-flavor"
        private val LICENSE_SLIDE_TAGS = "license-slide-tags"

        private val WATERMARK_FLAVOR = "watermark-flavor"
        private val WATERMARK_TAGS = "watermark-tags"
        private val WATERMARK_LAYOUT = "watermark-layout"
        private val WATERMARK_LAYOUT_VARIABLE = "watermark-layout-variable"

        /** Workflow property names  */
        private val THEME_ACTIVE = "theme_active"
        private val THEME_BUMPER_ACTIVE = "theme_bumper_active"
        private val THEME_TRAILER_ACTIVE = "theme_trailer_active"
        private val THEME_TITLE_SLIDE_ACTIVE = "theme_title_slide_active"
        private val THEME_TITLE_SLIDE_UPLOADED = "theme_title_slide_uploaded"
        private val THEME_WATERMARK_ACTIVE = "theme_watermark_active"

        /** The series theme property name  */
        private val THEME_PROPERTY_NAME = "theme"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ThemeWorkflowOperationHandler::class.java)

        private val elementBuilderFactory = MediaPackageElementBuilderFactory
                .newInstance()

        private val toMediaPackageElementFlavor = object : Fn<String, MediaPackageElementFlavor>() {
            override fun apply(flavorString: String): MediaPackageElementFlavor {
                return MediaPackageElementFlavor.parseFlavor(flavorString)
            }
        }
    }

}
