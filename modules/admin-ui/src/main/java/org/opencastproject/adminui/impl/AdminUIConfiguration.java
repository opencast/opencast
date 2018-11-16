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

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AdminUIConfiguration.class);

  public static final String OPT_PREVIEW_SUBTYPE = "preview.subtype";
  public static final String OPT_WAVEFORM_SUBTYPE = "waveform.subtype";
  public static final String OPT_SMIL_CATALOG_FLAVOR = "smil.catalog.flavor";
  public static final String OPT_SMIL_CATALOG_TAGS = "smil.catalog.tags";
  public static final String OPT_SMIL_SILENCE_FLAVOR = "smil.silence.flavor";
  public static final String OPT_THUMBNAIL_PUBLISH_FLAVOR = "thumbnail.publish.flavor";
  public static final String OPT_THUMBNAIL_PUBLISH_TAGS = "thumbnail.publish.tags";
  public static final String OPT_THUMBNAIL_UPLOADED_FLAVOR = "thumbnail.uploaded.flavor";
  public static final String OPT_THUMBNAIL_UPLOADED_TAGS = "thumbnail.uploaded.tags";
  public static final String OPT_THUMBNAIL_PREVIEW_FLAVOR = "thumbnail.preview.flavor";
  public static final String OPT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE = "thumbnail.preview.profile.downscale";
  public static final String OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY = "thumbnail.source.flavor.type.primary";
  public static final String OPT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY = "thumbnail.source.flavor.type.secondary";
  public static final String OPT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE = "thumbnail.source.flavor.subtype";
  public static final String OPT_THUMBNAIL_ENCODING_PROFILE = "thumbnail.encoding.profile";
  public static final String OPT_THUMBNAIL_DEFAULT_POSITION = "thumbnail.default.position";
  public static final String OPT_THUMBNAIL_AUTO_DISTRIBUTION = "thumbnail.auto.distribution";
  public static final String OPT_OAIPMH_CHANNEL = "oaipmh.channel";
  public static final String OPT_SOURCE_TRACK_LEFT_FLAVOR = "sourcetrack.left.flavor";
  public static final String OPT_SOURCE_TRACK_RIGHT_FLAVOR = "sourcetrack.right.flavor";
  public static final String OPT_PREVIEW_AUDIO_SUBTYPE = "preview.audio.subtype";
  public static final String OPT_PREVIEW_VIDEO_SUBTYPE = "preview.video.subtype";

  private static final String DEFAULT_PREVIEW_SUBTYPE = "preview";
  private static final String DEFAULT_WAVEFORM_SUBTYPE = "waveform";
  private static final String DEFAULT_SMIL_CATALOG_FLAVOR = "smil/cutting";
  private static final String DEFAULT_SMIL_CATALOG_TAGS = "archive";
  private static final String DEFAULT_SMIL_SILENCE_FLAVOR = "*/silence";
  private static final String DEFAULT_THUMBNAIL_PUBLISH_FLAVOR = "*/search+preview";
  private static final String DEFAULT_THUMBNAIL_PUBLISH_TAGS = "engage-download";
  private static final String DEFAULT_THUMBNAIL_UPLOADED_FLAVOR = "thumbnail/source";
  private static final String DEFAULT_THUMBNAIL_UPLOADED_TAGS = "archive";
  private static final String DEFAULT_THUMBNAIL_PREVIEW_FLAVOR = "thumbnail/preview";
  private static final String DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE = "editor.thumbnail.preview.downscale";
  private static final String DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY = "presenter";
  private static final String DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY = "presentation";
  private static final String DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE = "source";
  private static final String DEFAULT_THUMBNAIL_ENCODING_PROFILE = "search-cover.http";
  private static final Double DEFAULT_THUMBNAIL_DEFAULT_POSITION = 1.0;
  private static final Boolean DEFAULT_THUMBNAIL_AUTO_DISTRIBUTION = false;
  private static final String DEFAULT_OAIPMH_CHANNEL = "default";
  private static final String DEFAULT_PREVIEW_VIDEO_SUBTYPE = "video+preview";
  private static final String DEFAULT_PREVIEW_AUDIO_SUBTYPE = "audio+preview";
  private static final String DEFAULT_SOURCE_TRACK_LEFT_FLAVOR = "presenter/source";
  private static final String DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR = "presentation/source";

  private String previewSubtype = DEFAULT_PREVIEW_SUBTYPE;
  private String waveformSubtype = DEFAULT_WAVEFORM_SUBTYPE;
  private Set<String> smilCatalogTagSet = new HashSet<>();
  private MediaPackageElementFlavor smilCatalogFlavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_SMIL_CATALOG_FLAVOR);
  private MediaPackageElementFlavor smilSilenceFlavor = MediaPackageElementFlavor.parseFlavor(DEFAULT_SMIL_SILENCE_FLAVOR);
  private String thumbnailPublishFlavor = DEFAULT_THUMBNAIL_PUBLISH_FLAVOR;
  private String thumbnailPublishTags = DEFAULT_THUMBNAIL_PUBLISH_TAGS;
  private String thumbnailUploadedFlavor = DEFAULT_THUMBNAIL_UPLOADED_FLAVOR;
  private String thumbnailUploadedTags = DEFAULT_THUMBNAIL_UPLOADED_TAGS;
  private String thumbnailPreviewFlavor = DEFAULT_THUMBNAIL_PREVIEW_FLAVOR;
  private String thumbnailPreviewProfileDownscale = DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE;
  private String thumbnailSourceFlavorTypePrimary = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_PRIMARY;
  private String thumbnailSourceFlavorTypeSecondary = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_TYPE_SECONDARY;
  private String thumbnailSourceFlavorSubtype = DEFAULT_THUMBNAIL_SOURCE_FLAVOR_SUBTYPE;
  private String thumbnailEncodingProfile = DEFAULT_THUMBNAIL_ENCODING_PROFILE;
  private Double thumbnailDefaultPosition = DEFAULT_THUMBNAIL_DEFAULT_POSITION;
  private boolean thumbnailAutoDistribution = DEFAULT_THUMBNAIL_AUTO_DISTRIBUTION;
  private String oaipmhChannel = DEFAULT_OAIPMH_CHANNEL;
  private String previewVideoSubtype = DEFAULT_PREVIEW_VIDEO_SUBTYPE;
  private String previewAudioSubtype = DEFAULT_PREVIEW_AUDIO_SUBTYPE;
  private MediaPackageElementFlavor sourceTrackLeftFlavor = MediaPackageElementFlavor.parseFlavor(
    DEFAULT_SOURCE_TRACK_LEFT_FLAVOR);
  private MediaPackageElementFlavor sourceTrackRightFlavor = MediaPackageElementFlavor.parseFlavor(
    DEFAULT_SOURCE_TRACK_RIGHT_FLAVOR);

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

  public String getThumbnailPublishFlavor() {
    return thumbnailPublishFlavor;
  }

  public String getThumbnailPublishTags() {
    return thumbnailPublishTags;
  }

  public String getThumbnailPreviewFlavor() {
    return thumbnailPreviewFlavor;
  }

  public String getThumbnailPreviewProfileDownscale() {
    return thumbnailPreviewProfileDownscale;
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

  public String getThumbnailEncodingProfile() {
    return thumbnailEncodingProfile;
  }

  public Double getThumbnailDefaultPosition() {
    return thumbnailDefaultPosition;
  }

  public boolean getThumbnailAutoDistribution() {
    return thumbnailAutoDistribution;
  }

  public String getOaipmhChannel() {
    return oaipmhChannel;
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

    // Thumbnail publish flavor
    thumbnailPublishFlavor = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PUBLISH_FLAVOR), DEFAULT_THUMBNAIL_PUBLISH_FLAVOR);
    logger.debug("Thumbnail publish flavor set to '{}'", thumbnailPublishFlavor);

    // Thumbnail publish tags
    thumbnailPublishTags = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PUBLISH_TAGS), DEFAULT_THUMBNAIL_PUBLISH_TAGS);
    logger.debug("Thumbnail publish tags set to '{}'", thumbnailPublishTags);

    // Thumbnail preview flavor
    thumbnailPreviewFlavor = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PREVIEW_FLAVOR), DEFAULT_THUMBNAIL_PREVIEW_FLAVOR);
    logger.debug("Thumbnail preview flavor set to '{}'", thumbnailPreviewFlavor);

    // Encoding profile used to downscale the thumbnail preview image
    thumbnailPreviewProfileDownscale = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE), DEFAULT_THUMBNAIL_PREVIEW_PROFILE_DOWNSCALE);
    logger.debug("Thumbnail preview downscale encoding profile set to '{}'", thumbnailPreviewProfileDownscale);

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

    // Thumbnail encoding profile
    thumbnailEncodingProfile = StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_ENCODING_PROFILE), DEFAULT_THUMBNAIL_ENCODING_PROFILE);
    logger.debug("Thumbnail encoding profile set to '{}'", thumbnailEncodingProfile);

    // Thumbnail default position
    thumbnailDefaultPosition = Double.parseDouble(StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_DEFAULT_POSITION), DEFAULT_THUMBNAIL_DEFAULT_POSITION.toString()));
    logger.debug("Thumbnail default position set to '{}'", thumbnailDefaultPosition);

    thumbnailAutoDistribution = BooleanUtils.toBoolean(StringUtils.defaultString(
      (String) properties.get(OPT_THUMBNAIL_AUTO_DISTRIBUTION), DEFAULT_THUMBNAIL_AUTO_DISTRIBUTION.toString()));
    logger.debug("Thumbnail auto distribution: {}", thumbnailAutoDistribution);

    // OAI-PMH channel
    oaipmhChannel = StringUtils.defaultString(
      (String) properties.get(OPT_OAIPMH_CHANNEL), DEFAULT_OAIPMH_CHANNEL);
    logger.debug("OAI-PMH channel set to '{}", oaipmhChannel);

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

  }
}
