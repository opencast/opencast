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

package org.opencastproject.index.service.catalog.adapter.events;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Episode dublincore catalog implementation of a AbstractEventsCatalogUIAdapter
 */
public class CommonEventCatalogUIAdapter extends ConfigurableEventDCCatalogUIAdapter implements ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CommonEventCatalogUIAdapter.class);

  public static final String EPISODE_TITLE = "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE";

  @Override
  public String getUITitle() {
    return EPISODE_TITLE;
  }

  @Override
  public MediaPackageElementFlavor getFlavor() {
    return MediaPackageElements.EPISODE;
  }

  @Override
  public Catalog storeFields(MediaPackage mediaPackage, MetadataCollection abstractMetadata) {
    Catalog storeFields = super.storeFields(mediaPackage, abstractMetadata);

    // Update the metadata stored in the mediapackage
    MetadataField<?> presenters = abstractMetadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName());
    if (presenters != null && presenters.isUpdated() && presenters.getValue() instanceof Iterable<?>) {
      String[] creators = mediaPackage.getCreators();
      for (String creator : creators) {
        mediaPackage.removeCreator(creator);
      }
      for (String presenter : MetadataUtils.getIterableStringMetadata(presenters)) {
        mediaPackage.addCreator(presenter);
      }
    }

    MetadataField<?> series = abstractMetadata.getOutputFields().get(DublinCore.PROPERTY_IS_PART_OF.getLocalName());
    if (series != null && series.isUpdated() && isNotBlank(series.getValue().get().toString())) {
      String seriesID = series.getValue().get().toString();
      mediaPackage.setSeries(seriesID);
      try {
        DublinCore seriesDC = getSeriesService().getSeries(seriesID);

        String seriesTitle = seriesDC.getFirst(DublinCore.PROPERTY_TITLE);
        if (seriesTitle != null) {
          mediaPackage.setSeriesTitle(seriesTitle);
        }
      } catch (NotFoundException e) {
        logger.error("Unable find series with id {}", seriesID);
      } catch (SeriesException e) {
        logger.error("Unable to get a series with id {}, because: {}", seriesID, getStackTrace(e));
      } catch (UnauthorizedException e) {
        logger.error("Unable to get a series with id {}, because: {}", seriesID, getStackTrace(e));
      }
    }

    Opt<Date> startDate = MetadataUtils.getUpdatedDateMetadata(abstractMetadata, "startDate");
    if (startDate != null && startDate.isSome())
      mediaPackage.setDate(startDate.get());

    // Update all the metadata related to the episode dublin core catalog
    MetadataField<?> title = abstractMetadata.getOutputFields().get(DublinCore.PROPERTY_TITLE.getLocalName());
    if (title != null && title.isUpdated()) {
      mediaPackage.setTitle(title.getValue().get().toString());
    }
    return storeFields;
  }

}
