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
package org.opencastproject.livepublication.publication;

import static org.opencastproject.livepublication.api.LivePublicationService.LIVE_CHANNEL_ID;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.livepublication.api.LivePublicationException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ArchiveUpdater {
  private static final Logger logger = LoggerFactory.getLogger(ArchiveUpdater.class);

  private final AssetManager assetManager;
  private final SecurityService securityService;

  /**
   * The engage base url property
   **/
  public static final String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";
  public static final String PLAYER_PATH = "/play/";

  private final String serverUrl;

  public ArchiveUpdater(AssetManager assetManager, SecurityService securityService, String serverUrl) {
    this.assetManager = assetManager;
    this.securityService = securityService;
    this.serverUrl = serverUrl;
  }

  public MediaPackage getMediapackageFromArchive(String mpId) throws NotFoundException {
    Optional<Snapshot> snapshot = assetManager.getLatestSnapshot(mpId);
    if (snapshot.isEmpty()) {
      throw new NotFoundException();
    }
    return snapshot.get().getMediaPackage();
  }

  public void addLivePublication(MediaPackage mp, List<MediaPackageElement> elements) throws LivePublicationException {
    logger.debug("Adding live channel publication element to media package {}", mp);
    String engageUrlString = StringUtils.trimToNull(
        securityService.getOrganization().getProperties().get(ENGAGE_URL_PROPERTY));
    if (engageUrlString == null) {
      engageUrlString = serverUrl;
      logger.info(
          "Using 'server.url' as a fallback for the non-existing organization level key '{}' for the publication url",
          ENGAGE_URL_PROPERTY);
    }

    try {
      URI engageUri = URIUtils.resolve(new URI(engageUrlString), PLAYER_PATH + mp.getIdentifier().toString());
      Publication publication = PublicationImpl.publication(UUID.randomUUID().toString(), LIVE_CHANNEL_ID, engageUri,
          MimeTypes.parseMimeType("text/html"));
      elements.forEach(e -> PublicationImpl.addElementToPublication(publication, e));
      mp.add(publication);
    } catch (URISyntaxException e) {
      throw new LivePublicationException(e);
    }
  }

  public void removeLivePublication(MediaPackage mp) {
    Optional<Publication> publicationOpt = getLivePublication(mp);
    publicationOpt.ifPresent(mp::remove);
  }

  public static Optional<Publication> getLivePublication(MediaPackage mp) {
    return Arrays.stream(mp.getPublications()).filter(p -> p.getChannel().equals(LIVE_CHANNEL_ID)).findFirst();
  }

  public void updateLivePublication(MediaPackage mp, Collection<MediaPackageElement> retractedElements,
      Collection<MediaPackageElement> distributedElements) throws NotFoundException {
    Optional<Publication> publicationOpt = getLivePublication(mp);
    if (publicationOpt.isPresent()) {
      Publication publication = publicationOpt.get();
      retractedElements.forEach(publication::removeElement);
      distributedElements.forEach(mpe -> PublicationImpl.addElementToPublication(publication, mpe));
    } else {
      throw new NotFoundException();
    }
  }

  public void updateLivePublication(MediaPackage mp, Collection<Track> newTracks) throws NotFoundException {
    Optional<Publication> publicationOpt = getLivePublication(mp);
    if (publicationOpt.isPresent()) {
      Publication publication = publicationOpt.get();
      publication.clearTracks();
      newTracks.forEach(publication::addTrack);
    } else {
      throw new NotFoundException();
    }
  }
}
