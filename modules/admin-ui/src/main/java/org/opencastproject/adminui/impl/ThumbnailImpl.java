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

import static org.opencastproject.mediapackage.MediaPackageElementFlavor.flavor;
import static org.opencastproject.mediapackage.MediaPackageElementFlavor.parseFlavor;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil;
import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.publication.api.ConfigurablePublicationService;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ThumbnailImpl {
  /** Name of the thumbnail type workflow property */
  private static final String THUMBNAIL_PROPERTY_TYPE = "thumbnailType";
  /** Name of the thumbnail position workflow property */
  private static final String THUMBNAIL_PROPERTY_POSITION = "thumbnailPosition";
  /** Name of the thumbnail track workflow property */
  private static final String THUMBNAIL_PROPERTY_TRACK = "thumbnailTrack";

  public enum ThumbnailSource {
    DEFAULT(0),
    UPLOAD(1),
    SNAPSHOT(2);

    private final long number;

    ThumbnailSource(final long number) {
      this.number = number;
    }

    public long getNumber() {
      return number;
    }

    public static ThumbnailSource byNumber(final long number) {
      return Arrays.stream(ThumbnailSource.values()).filter(v -> v.number == number).findFirst().orElse(DEFAULT);
    }
  }

  public static class Thumbnail {
    private final ThumbnailSource type;
    private final Double position;
    private final String track;
    private final URI url;

    public Thumbnail(final ThumbnailSource type, final Double position, final String track, final URI url) {
      this.type = type;
      this.position = position;
      this.track = track;
      this.url = url;
    }

    public ThumbnailSource getType() {
      return type;
    }

    public OptionalDouble getPosition() {
      if (position != null) {
        return OptionalDouble.of(position);
      } else {
        return OptionalDouble.empty();
      }
    }

    public Optional<String> getTrack() {
      if (track != null) {
        return Optional.of(track);
      } else {
        return Optional.empty();
      }
    }

    public URI getUrl() {
      return url;
    }
  }

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ThumbnailImpl.class);

  private final MediaPackageElementFlavor previewFlavor;
  private final String masterProfile;
  private final String previewProfile;
  private final String previewProfileDownscale;
  private final MediaPackageElementFlavor uploadedFlavor;
  private final List<String> uploadedTags;
  private final Workspace workspace;
  private final OaiPmhPublicationService oaiPmhPublicationService;
  private final AssetManager assetManager;
  private final ConfigurablePublicationService configurablePublicationService;
  private final ComposerService composerService;
  private final double defaultPosition;
  private final String sourceFlavorSubtype;
  private final MediaPackageElementFlavor sourceFlavorPrimary;
  private final MediaPackageElementFlavor sourceFlavorSecondary;
  private String tempThumbnailFileName;
  private final String tempThumbnailId;
  private URI tempThumbnail;
  private MimeType tempThumbnailMimeType;

  private AdminUIConfiguration.ThumbnailDistributionSettings distributionOaiPmh;
  private AdminUIConfiguration.ThumbnailDistributionSettings distributionConfigurable;

  public ThumbnailImpl(final AdminUIConfiguration config, final Workspace workspace,
    final OaiPmhPublicationService oaiPmhPublicationService,
    final ConfigurablePublicationService configurablePublicationService, final AssetManager assetManager,
    final ComposerService composerService) {
    this.masterProfile = config.getThumbnailMasterProfile();
    this.previewFlavor = parseFlavor(config.getThumbnailPreviewFlavor());
    this.previewProfile = config.getThumbnailPreviewProfile();
    this.previewProfileDownscale = config.getThumbnailPreviewProfileDownscale();
    this.uploadedFlavor = parseFlavor(config.getThumbnailUploadedFlavor());
    this.uploadedTags = Arrays.asList(config.getThumbnailUploadedTags().split(","));
    this.defaultPosition = config.getThumbnailDefaultPosition();
    this.sourceFlavorSubtype = config.getThumbnailSourceFlavorSubtype();
    this.sourceFlavorPrimary = flavor(config.getThumbnailSourceFlavorTypePrimary(),
      config.getThumbnailSourceFlavorSubtype());
    this.sourceFlavorSecondary = flavor(config.getThumbnailSourceFlavorTypeSecondary(),
      config.getThumbnailSourceFlavorSubtype());
    this.workspace = workspace;
    this.oaiPmhPublicationService = oaiPmhPublicationService;
    this.assetManager = assetManager;
    this.composerService = composerService;
    this.configurablePublicationService = configurablePublicationService;
    this.tempThumbnail = null;
    this.tempThumbnailId = null;
    this.tempThumbnailMimeType = null;
    this.tempThumbnailFileName = null;
    this.distributionOaiPmh = config.getThumbnailDistributionOaiPmh();
    this.distributionConfigurable = config.getThumbnailDistributionConfigurable();
  }

  private Optional<Attachment> getThumbnailPreviewForMediaPackage(final MediaPackage mp) {
    final Optional<Publication> internalPublication = getPublication(mp, InternalPublicationChannel.CHANNEL_ID);
    if (internalPublication.isPresent()) {
      return Arrays
        .stream(internalPublication.get().getAttachments())
        .filter(attachment -> previewFlavor.matches(attachment.getFlavor()))
        .findFirst();
    } else {
      throw new IllegalStateException("Expected internal publication, but found none for mp " + mp.getIdentifier());
    }
  }

  public double getDefaultPosition() {
    return defaultPosition;
  }

  public Optional<Thumbnail> getThumbnail(final MediaPackage mp, final UrlSigningService urlSigningService,
        final Long expireSeconds) throws UrlSigningException, URISyntaxException {

    final Optional<Attachment> optThumbnail = getThumbnailPreviewForMediaPackage(mp);
    if (!optThumbnail.isPresent()) {
      return Optional.empty();
    }
    final Attachment thumbnail = optThumbnail.get();
    final URI url;
    if (urlSigningService.accepts(thumbnail.getURI().toString())) {
      url = new URI(urlSigningService.sign(optThumbnail.get().getURI().toString(), expireSeconds, null, null));
    } else {
      url = thumbnail.getURI();
    }

    final Map<String, String> ps = WorkflowPropertiesUtil
      .getLatestWorkflowProperties(assetManager, mp.getIdentifier().compact());
    final ThumbnailSource source = ps.entrySet().stream()
      .filter(p -> ThumbnailImpl.THUMBNAIL_PROPERTY_TYPE.equals(p.getKey()))
      .map(Map.Entry::getValue)
      .map(Long::parseLong)
      .map(ThumbnailSource::byNumber)
      .findAny()
      .orElse(ThumbnailSource.DEFAULT);
    final Double position = ps.entrySet().stream()
      .filter(p -> ThumbnailImpl.THUMBNAIL_PROPERTY_POSITION.equals(p.getKey()))
      .map(Map.Entry::getValue)
      .map(Double::parseDouble)
      .findAny().orElse(defaultPosition);
    final String track = ps.entrySet().stream()
      .filter(p -> ThumbnailImpl.THUMBNAIL_PROPERTY_TRACK.equals(p.getKey()))
      .map(Map.Entry::getValue)
      .findAny().orElse(null);

    return Optional.of(new Thumbnail(source, position, track, url));
  }

  public MediaPackageElement upload(final MediaPackage mp, final InputStream inputStream, final String contentType)
    throws IOException, NotFoundException, MediaPackageException, PublicationException,
    EncoderException, DistributionException {
    createTempThumbnail(mp, inputStream, contentType);

    final Collection<URI> deletionUris = new ArrayList<>(0);
    try {
      // Archive uploaded thumbnail (and remove old one)
      archive(mp);

      final MediaPackageElementFlavor trackFlavor = getPrimaryOrSecondaryTrack(mp).getFlavor();

      final Tuple<URI, List<MediaPackageElement>> internalPublicationResult = updateInternalPublication(mp, true);
      deletionUris.add(internalPublicationResult.getA());
      if (distributionConfigurable.getEnabled()) {
        deletionUris.add(updateConfigurablePublication(mp, trackFlavor));
      }
      if (distributionOaiPmh.getEnabled()) {
        deletionUris.add(updateOaiPmh(mp, trackFlavor));
      }

      assetManager.takeSnapshot(mp);

      // Set workflow settings: type = UPLOAD
      WorkflowPropertiesUtil
        .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, Long.toString(ThumbnailSource.UPLOAD.getNumber()));

      return internalPublicationResult.getB().get(0);
    } finally {
      inputStream.close();
      workspace.cleanup(mp.getIdentifier());
      for (final URI uri : deletionUris) {
        if (uri != null) {
          workspace.delete(uri);
        }
      }
    }
  }

  private Track getPrimaryOrSecondaryTrack(final MediaPackage mp) throws MediaPackageException {

    final Optional<Track> track = Optional.ofNullable(
      Arrays.stream(mp.getTracks(sourceFlavorPrimary)).findFirst()
        .orElse(Arrays.stream(mp.getTracks(sourceFlavorSecondary)).findFirst()
          .orElse(null)));

    if (track.isPresent()) {
      return track.get();
    } else {
      throw new MediaPackageException("Cannot find track with primary or secondary source flavor.");
    }
  }

  private void archive(final MediaPackage mp) {
    final Attachment attachment = AttachmentImpl.fromURI(tempThumbnail);
    attachment.setIdentifier(tempThumbnailId);
    attachment.setFlavor(uploadedFlavor);
    attachment.setMimeType(this.tempThumbnailMimeType);
    uploadedTags.forEach(attachment::addTag);
    Arrays.stream(mp.getElementsByFlavor(uploadedFlavor)).forEach(mp::remove);
    mp.add(attachment);
  }

  private Tuple<URI, List<MediaPackageElement>> updateInternalPublication(final MediaPackage mp, final boolean downscale)
    throws DistributionException, NotFoundException, IOException, MediaPackageException, PublicationException,
    EncoderException {
    final Predicate<Attachment> priorFilter = attachment -> previewFlavor.matches(attachment.getFlavor());
    if (downscale) {
      return updatePublication(mp, InternalPublicationChannel.CHANNEL_ID, priorFilter, previewFlavor,
             Collections.emptyList(), previewProfileDownscale);
    } else {
      return updatePublication(mp, InternalPublicationChannel.CHANNEL_ID, priorFilter, previewFlavor,
        Collections.emptyList());
    }
  }

  private URI updateOaiPmh(final MediaPackage mp, final MediaPackageElementFlavor trackFlavor)
    throws NotFoundException, IOException, PublicationException, MediaPackageException, DistributionException,
      EncoderException {
    // Use OaiPmhPublicationService to re-publish thumbnail
    final Optional<Publication> oldOaiPmhPub = getPublication(mp, this.distributionOaiPmh.getChannelId());
    if (!oldOaiPmhPub.isPresent()) {
      logger.debug("Thumbnail auto-distribution: No publications found for OAI-PMH publication channel {}",
        this.distributionOaiPmh.getChannelId());
      return null;
    } else {
      logger.debug("Thumbnail auto-distribution: Updating thumbnail of OAI-PMH publication channel {}",
        this.distributionOaiPmh.getChannelId());
    }

    // We have to update the configurable publication to contain the new thumbnail as an attachment
    final Optional<Publication> configurablePublicationOpt = getPublication(mp, distributionConfigurable.getChannelId());
    final Set<Publication> publicationsToUpdate = new HashSet<>();
    configurablePublicationOpt.ifPresent(publicationsToUpdate::add);

    final String publishThumbnailId = UUID.randomUUID().toString();
    final InputStream inputStream = tempInputStream();
    final URI publishThumbnailUri = workspace
      .put(mp.getIdentifier().compact(), publishThumbnailId, this.tempThumbnailFileName, inputStream);
    inputStream.close();

    final Attachment publishAttachment = AttachmentImpl.fromURI(publishThumbnailUri);
    publishAttachment.setIdentifier(UUID.randomUUID().toString());
    publishAttachment.setFlavor(distributionOaiPmh.getFlavor().applyTo(trackFlavor));
    for (String tag : distributionOaiPmh.getTags()) {
      publishAttachment.addTag(tag);
    }
    publishAttachment.setMimeType(this.tempThumbnailMimeType);

    // Create downscaled thumbnails if desired
    final Set<Attachment> addElements = new HashSet<>();
    if (distributionOaiPmh.getProfiles().length > 0) {
      addElements.addAll(downscaleAttachment(publishAttachment, distributionOaiPmh.getProfiles()));
    } else {
      addElements.add(publishAttachment);
    }

    final Publication oaiPmhPub = oaiPmhPublicationService.replaceSync(
      mp, getRepositoryName(distributionOaiPmh.getChannelId()),
      addElements, Collections.emptySet(),
      Collections.singleton(distributionOaiPmh.getFlavor()), Collections.emptySet(),
      publicationsToUpdate, false);
    mp.remove(oldOaiPmhPub.get());
    mp.add(oaiPmhPub);
    return publishThumbnailUri;
  }

  private Tuple<URI, List<MediaPackageElement>> updatePublication(final MediaPackage mp, final String channelId,
    final Predicate<Attachment> priorFilter, final MediaPackageElementFlavor flavor, final Collection<String> tags,
    final String... conversionProfiles) throws DistributionException, NotFoundException, IOException,
  MediaPackageException, PublicationException, EncoderException {

    logger.debug("Updating thumnbail of flavor '{}' in publication channel '{}'", flavor, channelId);
    final Optional<Publication> pubOpt = getPublication(mp, channelId);
    if (!pubOpt.isPresent()) {
      return null;
    }
    final Publication pub = pubOpt.get();

    final String aid = UUID.randomUUID().toString();
    final InputStream inputStream = tempInputStream();
    final URI aUri = workspace.put(mp.getIdentifier().compact(), aid, tempThumbnailFileName, inputStream);
    inputStream.close();
    final Attachment attachment = AttachmentImpl.fromURI(aUri);
    attachment.setIdentifier(aid);
    attachment.setFlavor(flavor);
    tags.forEach(attachment::addTag);
    attachment.setMimeType(tempThumbnailMimeType);
    final Collection<MediaPackageElement> addElements = new ArrayList<>();
    if (conversionProfiles != null && conversionProfiles.length > 0) {
      addElements.addAll(downscaleAttachment(attachment, conversionProfiles));
    } else {
      addElements.add(attachment);
    }
    final Set<String> removeElementsIds = Arrays.stream(pub.getAttachments()).filter(priorFilter)
      .map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());
    final Publication newPublication = this.configurablePublicationService.replaceSync(mp, channelId, addElements, removeElementsIds);
    mp.remove(pub);
    mp.add(newPublication);
    //noinspection ConstantConditions
    final Set<String> newAttachmentIds = addElements.stream()
      .map(MediaPackageElement::getIdentifier)
      .collect(Collectors.toSet());
    return Tuple.tuple(aUri, Arrays.stream(newPublication.getAttachments())
      .filter(att -> newAttachmentIds.contains(att.getIdentifier()))
      .collect(Collectors.toList()));
  }

  private List<Attachment> downscaleAttachment(final Attachment attachment, final String... conversionProfiles)
    throws DistributionException, EncoderException, MediaPackageException {
    // What the composer returns is not our original attachment, modified, but a new one, basically containing just
    // a URI.
    final List<Attachment> downscaled = composerService.convertImageSync(attachment, conversionProfiles);
    return downscaled.stream().map(a -> cloneAttachment(attachment, a.getURI())).collect(Collectors.toList());
  }

  private URI updateConfigurablePublication(final MediaPackage mp, final MediaPackageElementFlavor trackFlavor)
    throws IOException, NotFoundException, MediaPackageException, PublicationException,
      EncoderException, DistributionException {
    final Predicate<Attachment> flavorFilter = a -> a.getFlavor().matches(distributionConfigurable.getFlavor());
    final Predicate<Attachment> tagsFilter = a -> Arrays.asList(distributionConfigurable.getTags()).stream()
         .allMatch(t -> Arrays.asList(a.getTags()).contains(t));
    final Predicate<Attachment> priorFilter = flavorFilter.and(tagsFilter);
    final Tuple<URI, List<MediaPackageElement>> result = updatePublication(mp, distributionConfigurable.getChannelId(), priorFilter,
      distributionConfigurable.getFlavor().applyTo(trackFlavor), Arrays.asList(distributionConfigurable.getTags()),
        distributionConfigurable.getProfiles());
    if (result != null) {
      return result.getA();
    } else {
      return null;
    }
  }

  private InputStream tempInputStream() throws NotFoundException, IOException {
    return workspace.read(tempThumbnail);
  }

  private void createTempThumbnail(final MediaPackage mp, final InputStream inputStream, final String contentType)
    throws IOException {
    tempThumbnailMimeType = MimeTypes.parseMimeType(contentType);
    final String filename = "uploaded_thumbnail." + tempThumbnailMimeType.getSuffix().getOrElse("unknown");
    final String originalThumbnailId = UUID.randomUUID().toString();
    tempThumbnail = workspace.put(mp.getIdentifier().compact(), originalThumbnailId, filename, inputStream);
    tempThumbnailFileName = "uploaded_thumbnail." + tempThumbnailMimeType.getSuffix().getOrElse("unknown");
  }

  private Optional<Publication> getPublication(final MediaPackage mp, final String channelId) {
    return Arrays.stream(mp.getPublications()).filter(p -> p.getChannel().equalsIgnoreCase(channelId)).findAny();
  }

  private MediaPackageElement chooseThumbnail(final MediaPackage mp, final Track track, final double position)
    throws PublicationException, MediaPackageException, EncoderException, IOException, NotFoundException,
      UnknownFileTypeException, DistributionException {

    String encodingProfile;
    boolean downscale;
    if (isAutoDistributionEnabled()) {
      /* We extract a high quality image that will be converted to various formats as required by the distribution
         channels. We do need to downscale the thumbnail preview image for the video editor in this case. */
      encodingProfile = this.masterProfile;
      downscale = true;
    } else {
      /* We only need the thumbnail preview image for the video editor so we use the corresponding encoding profile
         and we do not need to downscale that image */
      encodingProfile = this.previewProfile;
      downscale = false;
    }

    tempThumbnail = composerService.imageSync(track, encodingProfile, position).get(0).getURI();
    tempThumbnailMimeType = MimeTypes.fromURI(tempThumbnail);
    tempThumbnailFileName = tempThumbnail.getPath().substring(tempThumbnail.getPath().lastIndexOf('/') + 1);

    final Collection<URI> deletionUris = new ArrayList<>(0);
    try {

      // Remove any uploaded thumbnails
      Arrays.stream(mp.getElementsByFlavor(uploadedFlavor)).forEach(mp::remove);

      final Tuple<URI, List<MediaPackageElement>> internalPublicationResult = updateInternalPublication(mp, downscale);
      deletionUris.add(internalPublicationResult.getA());

      if (distributionConfigurable.getEnabled()) {
        deletionUris.add(updateConfigurablePublication(mp, track.getFlavor()));
      }
      if (distributionOaiPmh.getEnabled()) {
        deletionUris.add(updateOaiPmh(mp, track.getFlavor()));
      }

      assetManager.takeSnapshot(mp);

      // We return the single thumbnail preview image for the video editor here.
      return internalPublicationResult.getB().get(0);
    } finally {
      workspace.cleanup(mp.getIdentifier());
      for (final URI uri : deletionUris) {
        if (uri != null) {
          workspace.delete(uri);
        }
      }
    }
  }

  public MediaPackageElement chooseDefaultThumbnail(final MediaPackage mp, final double position)
    throws PublicationException, MediaPackageException, EncoderException, IOException, NotFoundException,
    UnknownFileTypeException, DistributionException {

    final MediaPackageElement result = chooseThumbnail(mp, getPrimaryOrSecondaryTrack(mp), position);

    // Set workflow settings: type = DEFAULT
    WorkflowPropertiesUtil
      .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, Long.toString(ThumbnailSource.DEFAULT.getNumber()));
    // We switch from double to string here because the AssetManager cannot store doubles, and we need a double value
    // in the workflow (properties)
    WorkflowPropertiesUtil
      .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_POSITION, Double.toString(position));

    return result;
  }

  public MediaPackageElement chooseThumbnail(final MediaPackage mp, final String trackFlavorType, final double position)
    throws PublicationException, MediaPackageException, EncoderException, IOException, NotFoundException,
    UnknownFileTypeException, DistributionException {

    final MediaPackageElementFlavor trackFlavor = flavor(trackFlavorType, sourceFlavorSubtype);
    final Optional<Track> track = Arrays.stream(mp.getTracks(trackFlavor)).findFirst();

    if (!track.isPresent()) {
      throw new MediaPackageException("Cannot find stream with flavor " + trackFlavor + " to extract thumbnail.");
    }

    final MediaPackageElement result = chooseThumbnail(mp, track.get(), position);

    // Set workflow settings: type = SNAPSHOT, position, track
    WorkflowPropertiesUtil
      .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, Long.toString(ThumbnailSource.SNAPSHOT.getNumber()));
    // We switch from double to string here because the AssetManager cannot store doubles, and we need a double value
    // in the workflow (properties)
    WorkflowPropertiesUtil.storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_POSITION, Double.toString(position));
    WorkflowPropertiesUtil.storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TRACK, trackFlavor.getType());

    return result;
  }

  private static Attachment cloneAttachment(final Attachment attachmentToClone, final URI newUri) {
    try {
      final Attachment result = (Attachment) MediaPackageElementParser
        .getFromXml(MediaPackageElementParser.getAsXml(attachmentToClone));
      result.setIdentifier(UUID.randomUUID().toString());
      result.setURI(newUri);
      return result;
    } catch (MediaPackageException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isAutoDistributionEnabled() {
    return distributionOaiPmh.getEnabled() || distributionConfigurable.getEnabled();
  }

  private String getRepositoryName(final String publicationChannelId) {
    return publicationChannelId.replaceFirst(OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX, "");
  }

}
