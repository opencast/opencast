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
package org.opencastproject.adminui.impl;

import static org.opencastproject.mediapackage.MediaPackageElementFlavor.parseFlavor;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

public class AdminUIConfiguration implements ManagedService {

  /** This helper class provides all information relevant for the automatic distribution of
      thumbnails to a specific type of publication channels */
  public final class ThumbnailDistributionSettings {

    private boolean enabled;
    private String channelId;
    private MediaPackageElementFlavor flavor;
    private String[] tags;
    private String[] profiles;

    public boolean getEnabled() {
      return enabled && !StringUtils.isEmpty(channelId);
    }

    public String getChannelId() {
      return channelId;
    }

    public MediaPackageElementFlavor getFlavor() {
      return flavor;
    }

    public String[] getTags() {
      return tags;
    }

    public String[] getProfiles() {
      return profiles;
    }

    ThumbnailDistributionSettings(boolean enabled, String channelId, String flavor, String tags, String profiles) {
      this.enabled = enabled;
      this.channelId = StringUtils.trimToEmpty(channelId);
      this.flavor = parseFlavor(StringUtils.trimToEmpty(flavor));
      this.tags = StringUtils.split(StringUtils.trimToEmpty(tags), ",");
      this.profiles = StringUtils.split(StringUtils.trimToEmpty(profiles), ",");
    }
  }

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AdminUIConfiguration.class);

  public static final String OPT_PREVIEW_SUBTYPE = "preview.subtype";
  public static final String OPT_WAVEFORM_SUBTYPE = "waveform.subtype";
  public static final String OPT_SMIL_CATALOG_FLAVOR = "smil.catalog.flavor";
  public static final String OPT_SMIL_CATALOG_TAGS = "smil.catalog.tags";
  public static final String OPT_SMIL_SILENCE_FLAVOR = "smil.silence.flavor";
  public static final String OPT_THUMBNAIL_UPLOADED_FLAVOR = "thumbnail.uploaded.flavor";
  public static final String OPT_THUMBNAIL_UPLOADED_TAGS = "thumbnail.uploaded.tags";
  public static final String OPT_THUMBNAIL_MASTER_PROFILE = "thumbnail.master.profile";
  public static final String OPT_THUMBNAIL_PREVIEW_FLAVOR = "thumbnail.preview.flavor";
  public static final String OPT_THUMBNAIL_PREVIEW_PROFILE = "thumbnail.preview.profile";
  public static final String OPT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE = "thumbnail.preview.profile.downscale";
  public static final String OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY = "thumbnail.source.flavor.type.primary";
  public static final String OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY = "thumbnail.source.flavor.type.secondary";
  public static final String OPT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE = "thumbnail.source.flavor.subtype";
  public static final String OPT_THUMBNAIL_DEFAULT_POSITION = "thumbnail.default.position";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_AUTO = "thumbnail.distribution.auto";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL =
    "thumbnail.distribution.configurable.channel";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR =
    "thumbnail.distribution.configurable.flavor";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS = "thumbnail.distribution.configurable.tags";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES =
      "thumbnail.distribution.configurable.profiles";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL = "thumbnail.distribution.oaipmh.channel";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR = "thumbnail.distribution.oaipmh.flavor";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS = "thumbnail.distribution.oaipmh.tags";
  public static final String OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES = "thumbnail.distribution.oaipmh.profiles";

  public static final String OPT_SOURCE_TRACK_LEFT_FLAVOR = "sourcetrack.left.flavor";
  public static final String OPT_SOURCE_TRACK_RIGHT_FLAVOR = "sourcetrack.right.flavor";
  public static final String OPT_PREVIEW_AUDIO_SUBTYPE = "preview.audio.subtype";
  public static final String OPT_PREVIEW_VIDEO_SUBTYPE = "preview.video.subtype";
  private static final String OPT_RETRACT_WORKFLOW_ID = "retract.workflow.id";

  private static final String DEFAULT_PREVIEW_SUBTYPE = "preview";
  private static final String DEFAULT_WAVEFORM_SUBTYPE = "waveform";
  private static final String DEFAULT_SMIL_CATALOG_FLAVOR = "smil/cutting";
  private static final String DEFAULT_SMIL_CATALOG_TAGS = "archive";
  private static final String DEFAULT_SMIL_SILENCE_FLAVOR = "*/silence";
  private static final String DEFAULT_THUMBNAIL_UPLOADED_FLAVOR = "thumbnail/source";
  private static final String DEFAULT_THUMBNAIL_UPLOADED_TAGS = "archive";
  private static final String DEFAULT_THUMBNAIL_MASTER_PROFILE = "editor.thumbnail.master";
  private static final String DEFAULT_THUMBNAIL_PREVIEW_FLAVOR = "thumbnail/preview";
  private static final String DEFAULT_THUMBNAIL_PREVIEW_PROFILE = "editor.thumbnail.preview";
  private static final String DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE = "editor.thumbnail.preview.downscale";
  private static final String DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY = "presenter";
  private static final String DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY = "presentation";
  private static final String DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE = "source";
  private static final Double DEFAULT_THUMBNAIL_DEFAULT_POSITION = 1.0;
  private static final Boolean DEFAULT_THUMBNAIL_DISTRIBUTION_AUTO = false;
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL = "api";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR = "*/search+preview";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS = "engage-download";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES = "search-cover.http.downscale";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL = "oaipmh-default";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR = "*/search+preview";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS = "engage-download";
  private static final String DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES = "search-cover.http.downscale";

  private static final String DEFAULT_PREVIEW_VIDEO_SUBTYPE = "video+preview";
  private static final String DEFAULT_PREVIEW_AUDIO_SUBTYPE = "audio+preview";
  private static final String DEFAULT_SOURCE_TRACK_LEFT_FLAVOR = "presenter/source";
  private static final String DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR = "presentation/source";

  private static final String DEFAULT_RETRACT_WORKFLOW_ID = "delete";

  private String previewSubtype = DEFAULT_PREVIEW_SUBTYPE;
  private String waveformSubtype = DEFAULT_WAVEFORM_SUBTYPE;
  private Set<String> smilCatalogTagSet = new HashSet<>();
  private MediaPackageElementFlavor smilCatalogFlavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_SMIL_CATALOG_FLAVOR);
  private MediaPackageElementFlavor smilSilenceFlavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_SMIL_SILENCE_FLAVOR);
  private String thumbnailUploadedFlavor = DEFAULT_THUMBNAIL_UPLOADED_FLAVOR;
  private String thumbnailUploadedTags = DEFAULT_THUMBNAIL_UPLOADED_TAGS;
  private String thumbnailMasterProfile = DEFAULT_THUMBNAIL_MASTER_PROFILE;
  private String thumbnailPreviewFlavor = DEFAULT_THUMBNAIL_PREVIEW_FLAVOR;
  private String thumbnailPreviewProfile = DEFAULT_THUMBNAIL_PREVIEW_PROFILE;
  private String thumbnailPreviewProfileDownscale = DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE;
  private String thumbnailSourceFlavorTypePrimary = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY;
  private String thumbnailSourceFlavorTypeSecondary = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY;
  private String thumbnailSourceFlavorSubtype = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE;
  private Double thumbnailDefaultPosition = DEFAULT_THUMBNAIL_DEFAULT_POSITION;
  private ThumbnailDistributionSettings thumbnailDistributionOaiPmh;
  private ThumbnailDistributionSettings thumbnailDistributionConfigurable;
  private String previewVideoSubtype = DEFAULT_PREVIEW_VIDEO_SUBTYPE;
  private String previewAudioSubtype = DEFAULT_PREVIEW_AUDIO_SUBTYPE;
  private MediaPackageElementFlavor sourceTrackLeftFlavor = MediaPackageElementFlavor.parseFlavor(
    DEFAULT_SOURCE_TRACK_LEFT_FLAVOR);
  private MediaPackageElementFlavor sourceTrackRightFlavor = MediaPackageElementFlavor.parseFlavor(
    DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR);
  private String retractWorkflowId = DEFAULT_RETRACT_WORKFLOW_ID;

  public String getPreviewSubtype() {
    return previewSubtype;
  }

  public String getWaveformSubtype() {
    return waveformSubtype;
  }

  public MediaPackageElementFlavor getSmilCatalogFlavor() {
    return smilCatalogFlavor;
  }

  public Set<String> getSmilCatalogTags() {
    return smilCatalogTagSet;
  }

  public MediaPackageElementFlavor getSmilSilenceFlavor() {
    return smilSilenceFlavor;
  }

  public String getThumbnailUploadedFlavor() {
    return thumbnailUploadedFlavor;
  }

  public String getThumbnailUploadedTags() {
    return thumbnailUploadedTags;
  }

  public String getThumbnailPreviewFlavor() {
    return thumbnailPreviewFlavor;
  }

  public String getThumbnailPreviewProfile() {
    return thumbnailPreviewProfile;
  }

  public String getThumbnailPreviewProfileDownscale() {
    return thumbnailPreviewProfileDownscale;
  }

  public String getThumbnailMasterProfile() {
    return thumbnailMasterProfile;
  }
  public String getThumbnailSourceFlavorSubtype() {
    return thumbnailSourceFlavorSubtype;
  }

  public String getThumbnailSourceFlavorTypePrimary() {
    return thumbnailSourceFlavorTypePrimary;
  }

  public String getThumbnailSourceFlavorTypeSecondary() {
    return thumbnailSourceFlavorTypeSecondary;
  }

  public Double getThumbnailDefaultPosition() {
    return thumbnailDefaultPosition;
  }

  public ThumbnailDistributionSettings getThumbnailDistributionOaiPmh() {
    return thumbnailDistributionOaiPmh;
  }

  public ThumbnailDistributionSettings getThumbnailDistributionConfigurable() {
    return thumbnailDistributionConfigurable;
  }

  public String getPreviewVideoSubtype() {
    return previewVideoSubtype;
  }

  public String getPreviewAudioSubtype() {
    return previewAudioSubtype;
  }

  public MediaPackageElementFlavor getSourceTrackLeftFlavor() {
    return sourceTrackLeftFlavor;
  }

  public MediaPackageElementFlavor getSourceTrackRightFlavor() {
    return sourceTrackRightFlavor;
  }

  public String getRetractWorkflowId() {
    return retractWorkflowId;
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null)
      return;

    // Preview subtype
    previewSubtype = StringUtils.defaultString((String) properties.get(OPT_PREVIEW_SUBTYPE), DEFAULT_PREVIEW_SUBTYPE);
    logger.debug("Preview subtype configuration set to '{}'", previewSubtype);

    // Waveform subtype
    waveformSubtype = StringUtils.defaultString((String) properties.get(OPT_WAVEFORM_SUBTYPE), DEFAULT_WAVEFORM_SUBTYPE);
    logger.debug("Waveform subtype configuration set to '{}'", waveformSubtype);

    // SMIL catalog flavor
    smilCatalogFlavor = MediaPackageElementFlavor.parseFlavor(
      StringUtils.defaultString((String) properties.get(OPT_SMIL_CATALOG_FLAVOR), DEFAULT_SMIL_CATALOG_FLAVOR));
    logger.debug("Smil catalog flavor configuration set to '{}'", smilCatalogFlavor);

    // SMIL catalog tags
    String tags = StringUtils.defaultString((String) properties.get(OPT_SMIL_CATALOG_TAGS), DEFAULT_SMIL_CATALOG_TAGS);
    String[] smilCatalogTags = StringUtils.split(tags, ",");
    smilCatalogTagSet.clear();
    if (smilCatalogTags != null) {
      smilCatalogTagSet.addAll(Arrays.asList(smilCatalogTags));
    }
    logger.debug("Smil catalog target tags configuration set to '{}'", smilCatalogTagSet);

    // SMIL silence flavor
    smilSilenceFlavor = MediaPackageElementFlavor.parseFlavor(
      StringUtils.defaultString((String) properties.get(OPT_SMIL_SILENCE_FLAVOR), DEFAULT_SMIL_SILENCE_FLAVOR));
    logger.debug("Smil silence flavor configuration set to '{}'", smilSilenceFlavor);

    // Flavor of the uploaded thumbnail
    thumbnailUploadedFlavor = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_UPLOADED_FLAVOR), DEFAULT_THUMBNAIL_UPLOADED_FLAVOR);
    logger.debug("Flavor for uploaded thumbnail set to '{}'", thumbnailUploadedFlavor);

    // Tags of the uploaded thumbnail
    thumbnailUploadedTags = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_UPLOADED_TAGS), DEFAULT_THUMBNAIL_UPLOADED_TAGS);
    logger.debug("Tags for uploaded thumbnail set to '{}'", thumbnailUploadedTags);

    // Thumbnail preview flavor
    thumbnailPreviewFlavor = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PREVIEW_FLAVOR), DEFAULT_THUMBNAIL_PREVIEW_FLAVOR);
    logger.debug("Thumbnail preview flavor set to '{}'", thumbnailPreviewFlavor);

    // Encoding profile used to extract the thumbnail preview image
    thumbnailPreviewProfile = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PREVIEW_PROFILE), DEFAULT_THUMBNAIL_PREVIEW_PROFILE);
    logger.debug("Thumbnail preview encoding profile set to '{}'", thumbnailPreviewProfile);

    // Encoding profile used to downscale the thumbnail preview image
    thumbnailPreviewProfileDownscale = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE), DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE);
    logger.debug("Thumbnail preview downscale encoding profile set to '{}'", thumbnailPreviewProfileDownscale);

    // Encoding profile used to extract the master thumbnail image
    thumbnailMasterProfile = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_MASTER_PROFILE), DEFAULT_THUMBNAIL_MASTER_PROFILE);
    logger.debug("Thumbnail master extraction encoding profile set to '{}'", thumbnailMasterProfile);

    // Thumbnail source flavor primary type
    thumbnailSourceFlavorTypePrimary = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY), DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY);
    logger.debug("Thumbnail source flavor primary type set to '{}'", thumbnailSourceFlavorTypePrimary);

    // Thumbnail default track secondary
    thumbnailSourceFlavorTypeSecondary = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY),
      DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY);
    logger.debug("Thumbnail source flavor secondary type set to '{}'", thumbnailSourceFlavorTypeSecondary);

    // Thumbnail source flavor subtype
    thumbnailSourceFlavorSubtype = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE), DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE);
    logger.debug("Thumbnail source flavor subtype set to '{}'", thumbnailSourceFlavorSubtype);

    // Thumbnail default position
    thumbnailDefaultPosition = Double.parseDouble(StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_DEFAULT_POSITION), DEFAULT_THUMBNAIL_DEFAULT_POSITION.toString()));
    logger.debug("Thumbnail default position set to '{}'", thumbnailDefaultPosition);

    boolean thumbnailAutoDistribution = BooleanUtils.toBoolean(StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_AUTO), DEFAULT_THUMBNAIL_DISTRIBUTION_AUTO.toString()));
    logger.debug("Thumbnail auto distribution: {}", thumbnailAutoDistribution);

    thumbnailDistributionOaiPmh = new ThumbnailDistributionSettings(thumbnailAutoDistribution,
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL),
        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_CHANNEL),
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR),
        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_FLAVOR),
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS),
        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_TAGS),
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES),
        DEFAULT_THUMBNAIL_DISTRIBUTION_OAIPMH_PROFILES)
    );

    thumbnailDistributionConfigurable = new ThumbnailDistributionSettings(thumbnailAutoDistribution,
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL),
        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_CHANNEL),
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR),
        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_FLAVOR),
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS),
        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_TAGS),
      StringUtils.defaultString((String) properties.get(OPT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES),
        DEFAULT_THUMBNAIL_DISTRIBUTION_CONFIGURABLE_PROFILES)
    );

    // Preview Video subtype
    previewVideoSubtype = StringUtils.defaultString((String) properties.get(OPT_PREVIEW_VIDEO_SUBTYPE),
      DEFAULT_PREVIEW_VIDEO_SUBTYPE);
    logger.debug("Preview video subtype set to '{}'", previewVideoSubtype);

    // Preview Audio subtype
    previewAudioSubtype = StringUtils.defaultString((String) properties.get(OPT_PREVIEW_AUDIO_SUBTYPE),
      DEFAULT_PREVIEW_AUDIO_SUBTYPE);
    logger.debug("Preview audio subtype set to '{}'", previewAudioSubtype);

    // Source track left flavor
    sourceTrackLeftFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.defaultString(
      (String) properties.get(OPT_SOURCE_TRACK_LEFT_FLAVOR), DEFAULT_SOURCE_TRACK_LEFT_FLAVOR));
    logger.debug("Source track left flavor set to '{}'", sourceTrackLeftFlavor);

    // Source track right flavor
    sourceTrackRightFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.defaultString(
      (String) properties.get(OPT_SOURCE_TRACK_RIGHT_FLAVOR), DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR));
    logger.debug("Source track right flavor set to '{}'", sourceTrackRightFlavor);

    // Retract workflow ID
    retractWorkflowId = StringUtils.defaultString((String) properties.get(OPT_RETRACT_WORKFLOW_ID),
      DEFAULT_RETRACT_WORKFLOW_ID);
    logger.debug("Retract workflow ID set to {}", retractWorkflowId);
  }
}
