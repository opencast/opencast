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
package org.opencastproject.liveschedule.publication;

import static org.opencastproject.liveschedule.api.LiveScheduleService.LIVE_CHANNEL_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.liveschedule.api.LiveScheduleException;
import org.opencastproject.liveschedule.util.CatalogAndAttachmentEquator;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementSelector;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DistributionUpdater {

  private final DownloadDistributionService downloadDistributionService;
  private final Workspace workspace;

  private final MediaPackageElementSelector<MediaPackageElement> publishElementSelector;

  public static final String RETRACTED = "Retracted";
  public static final String DISTRIBUTED = "Distributed";

  public DistributionUpdater(DownloadDistributionService downloadDistributionService, Workspace workspace,
      MediaPackageElementSelector<MediaPackageElement> publishElementSelector) {
    this.downloadDistributionService = downloadDistributionService;
    this.workspace = workspace;

    this.publishElementSelector = publishElementSelector;
  }

  public List<MediaPackageElement> distributeElements(MediaPackage mp) throws LiveScheduleException {

    try {
      Collection<MediaPackageElement> elements = publishElementSelector.select(mp, false);
      return distributeElementsInternal(mp, elements);
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  private List<MediaPackageElement> distributeElementsInternal(MediaPackage mp,
      Collection<MediaPackageElement> elements) throws DistributionException {

    Set<String> elementIds = elements.stream().map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());
    List<MediaPackageElement> distributedElements = downloadDistributionService.distributeSync(LIVE_CHANNEL_ID, mp,
        elementIds, false); // TODO this might not work

    // Clean up
    for (MediaPackageElement element : elements) {
      try {
        workspace.delete(element.getURI());
      } catch (NotFoundException | IOException e) {
        // Do nothing
      }
    }
    return distributedElements;
  }

  public Map<String, Collection<MediaPackageElement>> updateElements(MediaPackage oldMp, MediaPackage newMp)
          throws LiveScheduleException {
    try {
      Collection<MediaPackageElement> oldElements = Stream.concat(Arrays.stream(oldMp.getAttachments()),
          Arrays.stream(oldMp.getCatalogs())).toList();
      Collection<MediaPackageElement> newElements = publishElementSelector.select(newMp, false);

      Collection<MediaPackageElement> toDistribute = CollectionUtils.removeAll(newElements, oldElements,
          new CatalogAndAttachmentEquator());
      Collection<MediaPackageElement> toRetract = CollectionUtils.removeAll(oldElements, newElements,
          new CatalogAndAttachmentEquator());

      Map<String, Collection<MediaPackageElement>> results = new HashMap<>();
      if (!toRetract.isEmpty()) {
        results.put(RETRACTED, downloadDistributionService.retractSync(LIVE_CHANNEL_ID, oldMp,
            toRetract.stream().map(MediaPackageElement::getIdentifier).collect(Collectors.toSet())));

      }

      if (!toDistribute.isEmpty()) {
        results.put(DISTRIBUTED, distributeElementsInternal(newMp, toDistribute));
      }

      return results;

    } catch (DistributionException e) {
      throw new LiveScheduleException(e);
    }
  }

  public void retractAllElements(MediaPackage mp, boolean fromPublication) throws DistributionException {
    Set<String> elementIds;
    if (fromPublication) {
      Optional<Publication> publicationOpt = ArchiveUpdater.getLivePublication(mp);
      if (publicationOpt.isPresent()) {
        elementIds = Arrays.stream(publicationOpt.get().getElements()).filter(
            mpe -> !MediaPackageElement.Type.Track.equals(mpe.getElementType()))
            .map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());
      } else {
        throw new DistributionException("Publication of " + mp.getIdentifier() + " not found!");
      }
    } else {
      elementIds = Arrays.stream(mp.getElements())
          .filter(mpe -> !(mpe.getElementType().equals(MediaPackageElement.Type.Publication)
              || mpe.getElementType().equals(MediaPackageElement.Type.Track)))
          .map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());
    }
    downloadDistributionService.retractSync(LIVE_CHANNEL_ID, mp, elementIds);
  }
}
