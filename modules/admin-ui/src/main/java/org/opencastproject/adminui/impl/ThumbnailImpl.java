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
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
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

  private final MediaPackageElementFlavor sourceFlavor;
  private final MediaPackageElementFlavor previewFlavor;
  private final MediaPackageElementFlavor publishFlavor;
  private final List<String> publishTags;
  private final Workspace workspace;
  private final OaiPmhPublicationService oaiPmhPublicationService;
  private final AssetManager assetManager;
  private final ConfigurablePublicationService configurablePublicationService;
  private final ComposerService composerService;
  private final String oaiPmhChannel;
  private final String encodingProfile;
  private final double defaultPosition;
  private final MediaPackageElementFlavor defaultTrackPrimary;
  private final MediaPackageElementFlavor defaultTrackSecondary;
  private String tempThumbnailFileName;
  private final String tempThumbnailId;
  private URI tempThumbnail;
  private MimeType tempThumbnailMimeType;

  private boolean thumbnailAutoDistribution;

  public ThumbnailImpl(final AdminUIConfiguration config, final Workspace workspace,
    final OaiPmhPublicationService oaiPmhPublicationService,
    final ConfigurablePublicationService configurablePublicationService, final AssetManager assetManager,
    final ComposerService composerService) {
    this.sourceFlavor = flavor(config.getThumbnailSourceFlavorType(), config.getThumbnailSourceFlavorSubtype());
    this.previewFlavor = parseFlavor(config.getThumbnailPreviewFlavor());
    this.publishFlavor = parseFlavor(config.getThumbnailPublishFlavor());
    this.publishTags = Arrays.asList(config.getThumbnailPublishTags().split(","));
    this.oaiPmhChannel = config.getOaipmhChannel();
    this.encodingProfile = config.getThumbnailEncodingProfile();
    this.defaultPosition = config.getThumbnailDefaultPosition();
    this.defaultTrackPrimary = flavor(config.getThumbnailDefaultTrackPrimary(), config.getThumbnailSourceFlavorSubtype());
    this.defaultTrackSecondary = flavor(config.getThumbnailDefaultTrackSecondary(), config.getThumbnailSourceFlavorSubtype());
    this.workspace = workspace;
    this.oaiPmhPublicationService = oaiPmhPublicationService;
    this.assetManager = assetManager;
    this.composerService = composerService;
    this.configurablePublicationService = configurablePublicationService;
    this.tempThumbnail = null;
    this.tempThumbnailId = null;
    this.tempThumbnailMimeType = null;
    this.tempThumbnailFileName = null;
    this.thumbnailAutoDistribution = config.getThumbnailAutoDistribution();
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
    EncoderException {
    createTempThumbnail(mp, inputStream, contentType);

    final Collection<URI> deletionUris = new ArrayList<>(0);
    try {
      // Archive uploaded thumbnail (and remove old one)
      archive(mp);

      final MediaPackageElementFlavor trackFlavor = getPrimaryOrSecondaryTrack(mp).getFlavor();

      final Tuple<URI, MediaPackageElement> internalPublicationResult = updateInternalPublication(mp, true);
      deletionUris.add(internalPublicationResult.getA());
      if (thumbnailAutoDistribution) {
        deletionUris.add(updateExternalPublication(mp, trackFlavor));
        deletionUris.add(updateOaiPmh(mp, trackFlavor));
      }

      assetManager.takeSnapshot(mp);

      // Set workflow settings: type = UPLOAD
      WorkflowPropertiesUtil
        .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, Long.toString(ThumbnailSource.UPLOAD.getNumber()));

      return internalPublicationResult.getB();
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
      Arrays.stream(mp.getTracks(defaultTrackPrimary)).findFirst()
        .orElse(Arrays.stream(mp.getTracks(defaultTrackSecondary)).findFirst()
          .orElse(null)));

    if (track.isPresent()) {
      return track.get();
    } else {
      throw new MediaPackageException("Cannot find stream with primary or secondaŕy default flavor.");
    }
  }

  private void archive(final MediaPackage mp) {
    final Attachment sourceAttachment = AttachmentImpl.fromURI(tempThumbnail);
    sourceAttachment.setIdentifier(tempThumbnailId);
    sourceAttachment.setFlavor(sourceFlavor);
    sourceAttachment.setMimeType(this.tempThumbnailMimeType);
    Arrays.stream(mp.getElementsByFlavor(sourceFlavor)).forEach(mp::remove);
    mp.add(sourceAttachment);
  }

  private Tuple<URI, MediaPackageElement> updateInternalPublication(final MediaPackage mp, final boolean downscale)
    throws NotFoundException, IOException, MediaPackageException, PublicationException,
    EncoderException {
    final Predicate<Attachment> priorFilter = attachment -> previewFlavor.matches(attachment.getFlavor());
    final String conversionProfile;
    if (downscale) {
      conversionProfile = "editor.thumbnail.preview.downscale";
    } else {
      conversionProfile = null;
    }
    return updatePublication(mp, InternalPublicationChannel.CHANNEL_ID, priorFilter, previewFlavor,
      Collections.emptyList(), conversionProfile);
  }

  private URI updateOaiPmh(final MediaPackage mp, final MediaPackageElementFlavor trackFlavor)
    throws NotFoundException, IOException, PublicationException, MediaPackageException {
    // Use OaiPmhPublicationService to re-publish thumbnail
    final Optional<Publication> oldOaiPmhPub = getPublication(mp,
      OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX + this.oaiPmhChannel);
    if (!oldOaiPmhPub.isPresent()) {
      return null;
    }

    // We have to update the external publication to contain the new thumbnail as an attachment
    final Optional<Publication> externalPublicationOpt = getPublication(mp, "api");
    final Set<Publication> publicationsToUpdate = new HashSet<>();
    externalPublicationOpt.ifPresent(publicationsToUpdate::add);

    final String publishThumbnailId = UUID.randomUUID().toString();
    final InputStream inputStream = tempInputStream();
    final URI publishThumbnailUri = workspace
      .put(mp.getIdentifier().compact(), publishThumbnailId, this.tempThumbnailFileName, inputStream);
    inputStream.close();

    final Attachment publishAttachment = AttachmentImpl.fromURI(publishThumbnailUri);
    publishAttachment.setIdentifier(UUID.randomUUID().toString());
    publishAttachment.setFlavor(publishFlavor.applyTo(trackFlavor));
    publishTags.forEach(publishAttachment::addTag);
    publishAttachment.setMimeType(this.tempThumbnailMimeType);

    final Publication oaiPmhPub = oaiPmhPublicationService.replaceSync(
      mp, oaiPmhChannel,
      Collections.singleton(publishAttachment), Collections.emptySet(),
      Collections.singleton(publishFlavor), Collections.emptySet(),
      publicationsToUpdate, false);
    mp.remove(oldOaiPmhPub.get());
    mp.add(oaiPmhPub);
    return publishThumbnailUri;
  }

  private Tuple<URI, MediaPackageElement> updatePublication(final MediaPackage mp, final String channelId,
    final Predicate<Attachment> priorFilter, final MediaPackageElementFlavor flavor, final Iterable<String> tags,
    final String conversionProfile) throws NotFoundException, IOException,
  MediaPackageException, PublicationException, EncoderException {

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
    if (conversionProfile != null) {
      downscaleAttachment(conversionProfile, attachment);
    }

    final Collection<MediaPackageElement> addElements = Collections.singleton(attachment);
    final Set<String> removeElementsIds = Arrays.stream(pub.getAttachments()).filter(priorFilter)
      .map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());
    final Publication newPublication = this.configurablePublicationService.replaceSync(mp, channelId, addElements, removeElementsIds);
    mp.remove(pub);
    mp.add(newPublication);
    //noinspection ConstantConditions
    final Attachment newElement = Arrays.stream(newPublication.getAttachments())
      .filter(att -> att.getIdentifier().equals(aid)).findAny().get();
    return Tuple.tuple(aUri, newElement);
  }

  private void downscaleAttachment(final String conversionProfile, final Attachment attachment)
    throws EncoderException, MediaPackageException {
    // What the composer returns is not our original attachment, modified, but a new one, basically containing just
    // a URI.
    final Attachment downscaled = composerService.convertImageSync(attachment, conversionProfile);
    attachment.setURI(downscaled.getURI());
  }

  private URI updateExternalPublication(final MediaPackage mp, final MediaPackageElementFlavor trackFlavor)
    throws IOException, NotFoundException, MediaPackageException, PublicationException,
    EncoderException {
    final Predicate<Attachment> flavorFilter = a -> a.getFlavor().matches(publishFlavor);
    final Predicate<Attachment> tagsFilter = a -> Arrays.asList(a.getTags()).containsAll(publishTags);
    final Predicate<Attachment> priorFilter = flavorFilter.and(tagsFilter);
    final Tuple<URI, MediaPackageElement> result = updatePublication(mp, "api", priorFilter,
      publishFlavor.applyTo(trackFlavor), publishTags, null);
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
    UnknownFileTypeException {
    tempThumbnail = composerService.imageSync(track, encodingProfile, position).get(0).getURI();
    tempThumbnailMimeType = MimeTypes.fromURI(tempThumbnail);
    tempThumbnailFileName = tempThumbnail.getPath().substring(tempThumbnail.getPath().lastIndexOf('/') + 1);

    final Collection<URI> deletionUris = new ArrayList<>(0);
    try {

      // Remove any uploaded thumbnails
      Arrays.stream(mp.getElementsByFlavor(sourceFlavor)).forEach(mp::remove);

      final Tuple<URI, MediaPackageElement> internalPublicationResult = updateInternalPublication(mp, false);
      deletionUris.add(internalPublicationResult.getA());
      if (thumbnailAutoDistribution) {
        deletionUris.add(updateExternalPublication(mp, track.getFlavor()));
        deletionUris.add(updateOaiPmh(mp, track.getFlavor()));
      }

      assetManager.takeSnapshot(mp);

      return internalPublicationResult.getB();
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
    UnknownFileTypeException {

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
    UnknownFileTypeException {

    final MediaPackageElementFlavor trackFlavor = flavor(trackFlavorType, sourceFlavor.getSubtype());
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
}
