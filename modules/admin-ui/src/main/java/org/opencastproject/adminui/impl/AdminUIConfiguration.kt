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
package org.opencastproject.adminui.impl

import org.opencastproject.mediapackage.MediaPackageElementFlavor.parseFlavor

import org.opencastproject.mediapackage.MediaPackageElementFlavor

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Arrays
import java.util.Dictionary
import java.util.HashSet

class AdminUIConfiguration : ManagedService {

    var previewSubtype = DEFAULT_PREVIEW_SUBTYPE
        private set
    var waveformSubtype = DEFAULT_WAVEFORM_SUBTYPE
        private set
    private val smilCatalogTagSet = HashSet<String>()
    var smilCatalogFlavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_SMIL_CATALOG_FLAVOR)
        private set
    var smilSilenceFlavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_SMIL_SILENCE_FLAVOR)
        private set
    var thumbnailUploadedFlavor = DEFAULT_THUMBNAIL_UPLOADED_FLAVOR
        private set
    var thumbnailUploadedTags = DEFAULT_THUMBNAIL_UPLOADED_TAGS
        private set
    var thumbnailMasterProfile = DEFAULT_THUMBNAIL_MASTER_PROFILE
        private set
    var thumbnailPreviewFlavor = DEFAULT_THUMBNAIL_PREVIEW_FLAVOR
        private set
    var thumbnailPreviewProfile = DEFAULT_THUMBNAIL_PREVIEW_PROFILE
        private set
    var thumbnailPreviewProfileDownscale = DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE
        private set
    var thumbnailSourceFlavorTypePrimary = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY
        private set
    var thumbnailSourceFlavorTypeSecondary = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY
        private set
    var thumbnailSourceFlavorSubtype = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE
        private set
    var thumbnailDefaultPosition: Double? = DEFAULT_THUMBNAIL_DEFAULT_POSITION
        private set
    var thumbnailDistributionOaiPmh: ThumbnailDistributionSettings? = null
        private set
    var thumbnailDistributionConfigurable: ThumbnailDistributionSettings? = null
        private set
    var previewVideoSubtype = DEFAULT_PREVIEW_VIDEO_SUBTYPE
        private set
    var previewAudioSubtype = DEFAULT_PREVIEW_AUDIO_SUBTYPE
        private set
    var sourceTrackLeftFlavor = MediaPackageElementFlavor.parseFlavor(
            DEFAULT_SOURCE_TRACK_LEFT_FLAVOR)
        private set
    var sourceTrackRightFlavor = MediaPackageElementFlavor.parseFlavor(
            DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR)
        private set

    val smilCatalogTags: Set<String>
        get() = smilCatalogTagSet

    /** This helper class provides all information relevant for the automatic distribution of
     * thumbnails to a specific type of publication channels  */
    inner class ThumbnailDistributionSettings internal constructor(private val enabled: Boolean, channelId: String, flavor: String, tags: String, profiles: String) {
        val channelId: String
        val flavor: MediaPackageElementFlavor
        val tags: Array<String>
        val profiles: Array<String>

        fun getEnabled(): Boolean {
            return enabled && !StringUtils.isEmpty(channelId)
        }

        init {
            this.channelId = StringUtils.trimToEmpty(channelId)
            this.flavor = parseFlavor(StringUtils.trimToEmpty(flavor))
            this.tags = StringUtils.split(StringUtils.trimToEmpty(tags), ",")
            this.profiles = StringUtils.split(StringUtils.trimToEmpty(profiles), ",")
        }
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null)
            return

        // Preview subtype
        previewSubtype = StringUtils.defaultString(properties.get(OPT_PREVIEW_SUBTYPE) as String, DEFAULT_PREVIEW_SUBTYPE)
        logger.debug("Preview subtype configuration set to '{}'", previewSubtype)

        // Waveform subtype
        waveformSubtype = StringUtils.defaultString(properties.get(OPT_WAVEFORM_SUBTYPE) as String, DEFAULT_WAVEFORM_SUBTYPE)
        logger.debug("Waveform subtype configuration set to '{}'", waveformSubtype)

        // SMIL catalog flavor
        smilCatalogFlavor = MediaPackageElementFlavor.parseFlavor(
                StringUtils.defaultString(properties.get(OPT_SMIL_CATALOG_FLAVOR) as String, DEFAULT_SMIL_CATALOG_FLAVOR))
        logger.debug("Smil catalog flavor configuration set to '{}'", smilCatalogFlavor)

        // SMIL catalog tags
        val tags = StringUtils.defaultString(properties.get(OPT_SMIL_CATALOG_TAGS) as String, DEFAULT_SMIL_CATALOG_TAGS)
        val smilCatalogTags = StringUtils.split(tags, ",")
        smilCatalogTagSet.clear()
        if (smilCatalogTags != null) {
            smilCatalogTagSet.addAll(Arrays.asList(*smilCatalogTags))
        }
        logger.debug("Smil catalog target tags configuration set to '{}'", smilCatalogTagSet)

        // SMIL silence flavor
        smilSilenceFlavor = MediaPackageElementFlavor.parseFlavor(
                StringUtils.defaultString(properties.get(OPT_SMIL_SILENCE_FLAVOR) as String, DEFAULT_SMIL_SILENCE_FLAVOR))
        logger.debug("Smil silence flavor configuration set to '{}'", smilSilenceFlavor)

        // Flavor of the uploaded thumbnail
        thumbnailUploadedFlavor = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_UPLOADED_FLAVOR) as String, DEFAULT_THUMBNAIL_UPLOADED_FLAVOR)
        logger.debug("Flavor for uploaded thumbnail set to '{}'", thumbnailUploadedFlavor)

        // Tags of the uploaded thumbnail
        thumbnailUploadedTags = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_UPLOADED_TAGS) as String, DEFAULT_THUMBNAIL_UPLOADED_TAGS)
        logger.debug("Tags for uploaded thumbnail set to '{}'", thumbnailUploadedTags)

        // Thumbnail preview flavor
        thumbnailPreviewFlavor = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_PREVIEW_FLAVOR) as String, DEFAULT_THUMBNAIL_PREVIEW_FLAVOR)
        logger.debug("Thumbnail preview flavor set to '{}'", thumbnailPreviewFlavor)

        // Encoding profile used to extract the thumbnail preview image
        thumbnailPreviewProfile = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_PREVIEW_PROFILE) as String, DEFAULT_THUMBNAIL_PREVIEW_PROFILE)
        logger.debug("Thumbnail preview encoding profile set to '{}'", thumbnailPreviewProfile)

        // Encoding profile used to downscale the thumbnail preview image
        thumbnailPreviewProfileDownscale = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE) as String, DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE)
        logger.debug("Thumbnail preview downscale encoding profile set to '{}'", thumbnailPreviewProfileDownscale)

        // Encoding profile used to extract the master thumbnail image
        thumbnailMasterProfile = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_MASTER_PROFILE) as String, DEFAULT_THUMBNAIL_MASTER_PROFILE)
        logger.debug("Thumbnail master extraction encoding profile set to '{}'", thumbnailMasterProfile)

        // Thumbnail source flavor primary type
        thumbnailSourceFlavorTypePrimary = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY) as String, DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY)
        logger.debug("Thumbnail source flavor primary type set to '{}'", thumbnailSourceFlavorTypePrimary)

        // Thumbnail default track secondary
        thumbnailSourceFlavorTypeSecondary = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY) as String,
                DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY)
        logger.debug("Thumbnail source flavor secondary type set to '{}'", thumbnailSourceFlavorTypeSecondary)

        // Thumbnail source flavor subtype
        thumbnailSourceFlavorSubtype = StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE) as String, DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE)
        logger.debug("Thumbnail source flavor subtype set to '{}'", thumbnailSourceFlavorSubtype)

        // Thumbnail default position
        thumbnailDefaultPosition = java.lang.Double.parseDouble(StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_DEFAULT_POSITION) as String, DEFAULT_THUMBNAIL_DEFAULT_POSITION.toString()))
        logger.debug("Thumbnail default position set to '{}'", thumbnailDefaultPosition)

        val thumbnailAutoDistribution = BooleanUtils.toBoolean(StringUtils.defaultString(
                properties.get(OPT_THUMBNAIL_DISTRIBUTION_AUTO) as String, DEFAULT_THUMBNAIL_DISTRIBUTION_AUTO.toString()))
        logger.debug("Thumbnail auto distribution: {}", thumbnailAutoDistribution)

        thumbnailDistributionOaiPmh = ThumbnailDistributionSettings(thumbnailAutoDistribution,
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL),
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR),
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS),
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES)
        )

        thumbnailDistributionConfigurable = ThumbnailDistributionSettings(thumbnailAutoDistribution,
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL),
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR),
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS),
                StringUtils.defaultString(properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES) as String,
                        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES)
        )

        // Preview Video subtype
        previewVideoSubtype = StringUtils.defaultString(properties.get(OPT_PREVIEW_VIDEO_SUBTYPE) as String,
                DEFAULT_PREVIEW_VIDEO_SUBTYPE)
        logger.debug("Preview video subtype set to '{}'", previewVideoSubtype)

        // Preview Audio subtype
        previewAudioSubtype = StringUtils.defaultString(properties.get(OPT_PREVIEW_AUDIO_SUBTYPE) as String,
                DEFAULT_PREVIEW_AUDIO_SUBTYPE)
        logger.debug("Preview audio subtype set to '{}'", previewAudioSubtype)

        // Source track left flavor
        sourceTrackLeftFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.defaultString(
                properties.get(OPT_SOURCE_TRACK_LEFT_FLAVOR) as String, DEFAULT_SOURCE_TRACK_LEFT_FLAVOR))
        logger.debug("Source track left flavor set to '{}'", sourceTrackLeftFlavor)

        // Source track right flavor
        sourceTrackRightFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.defaultString(
                properties.get(OPT_SOURCE_TRACK_RIGHT_FLAVOR) as String, DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR))
        logger.debug("Source track right flavor set to '{}'", sourceTrackRightFlavor)

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(AdminUIConfiguration::class.java)

        val OPT_PREVIEW_SUBTYPE = "preview.subtype"
        val OPT_WAVEFORM_SUBTYPE = "waveform.subtype"
        val OPT_SMIL_CATALOG_FLAVOR = "smil.catalog.flavor"
        val OPT_SMIL_CATALOG_TAGS = "smil.catalog.tags"
        val OPT_SMIL_SILENCE_FLAVOR = "smil.silence.flavor"
        val OPT_THUMBNAIL_UPLOADED_FLAVOR = "thumbnail.uploaded.flavor"
        val OPT_THUMBNAIL_UPLOADED_TAGS = "thumbnail.uploaded.tags"
        val OPT_THUMBNAIL_MASTER_PROFILE = "thumbnail.master.profile"
        val OPT_THUMBNAIL_PREVIEW_FLAVOR = "thumbnail.preview.flavor"
        val OPT_THUMBNAIL_PREVIEW_PROFILE = "thumbnail.preview.profile"
        val OPT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE = "thumbnail.preview.profile.downscale"
        val OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY = "thumbnail.source.flavor.type.primary"
        val OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY = "thumbnail.source.flavor.type.secondary"
        val OPT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE = "thumbnail.source.flavor.subtype"
        val OPT_THUMBNAIL_DEFAULT_POSITION = "thumbnail.default.position"
        val OPT_THUMBNAIL_DISTRIBUTION_AUTO = "thumbnail.distribution.auto"
        val OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL = "thumbnail.distribution.configurable.channel"
        val OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR = "thumbnail.distribution.configurable.flavor"
        val OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS = "thumbnail.distribution.configurable.tags"
        val OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES = "thumbnail.distribution.configurable.profiles"
        val OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL = "thumbnail.distribution.oaipmh.channel"
        val OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR = "thumbnail.distribution.oaipmh.flavor"
        val OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS = "thumbnail.distribution.oaipmh.tags"
        val OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES = "thumbnail.distribution.oaipmh.profiles"

        val OPT_SOURCE_TRACK_LEFT_FLAVOR = "sourcetrack.left.flavor"
        val OPT_SOURCE_TRACK_RIGHT_FLAVOR = "sourcetrack.right.flavor"
        val OPT_PREVIEW_AUDIO_SUBTYPE = "preview.audio.subtype"
        val OPT_PREVIEW_VIDEO_SUBTYPE = "preview.video.subtype"

        private val DEFAULT_PREVIEW_SUBTYPE = "preview"
        private val DEFAULT_WAVEFORM_SUBTYPE = "waveform"
        private val DEFAULT_SMIL_CATALOG_FLAVOR = "smil/cutting"
        private val DEFAULT_SMIL_CATALOG_TAGS = "archive"
        private val DEFAULT_SMIL_SILENCE_FLAVOR = "*/silence"
        private val DEFAULT_THUMBNAIL_UPLOADED_FLAVOR = "thumbnail/source"
        private val DEFAULT_THUMBNAIL_UPLOADED_TAGS = "archive"
        private val DEFAULT_THUMBNAIL_MASTER_PROFILE = "editor.thumbnail.master"
        private val DEFAULT_THUMBNAIL_PREVIEW_FLAVOR = "thumbnail/preview"
        private val DEFAULT_THUMBNAIL_PREVIEW_PROFILE = "editor.thumbnail.preview"
        private val DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE = "editor.thumbnail.preview.downscale"
        private val DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY = "presenter"
        private val DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY = "presentation"
        private val DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE = "source"
        private val DEFAULT_THUMBNAIL_DEFAULT_POSITION = 1.0
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_AUTO = false
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL = "api"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR = "*/search+preview"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS = "engage-download"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES = "search-cover.http.downscale"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL = "oaipmh-default"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR = "*/search+preview"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS = "engage-download"
        private val DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES = "search-cover.http.downscale"

        private val DEFAULT_PREVIEW_VIDEO_SUBTYPE = "video+preview"
        private val DEFAULT_PREVIEW_AUDIO_SUBTYPE = "audio+preview"
        private val DEFAULT_SOURCE_TRACK_LEFT_FLAVOR = "presenter/source"
        private val DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR = "presentation/source"
    }
}
