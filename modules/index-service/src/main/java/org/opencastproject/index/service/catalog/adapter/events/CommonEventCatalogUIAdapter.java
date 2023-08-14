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

import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;

import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

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
  public Catalog storeFields(MediaPackage mediaPackage, DublinCoreMetadataCollection abstractMetadata) {
    Catalog storeFields = super.storeFields(mediaPackage, abstractMetadata);

    // Update the metadata stored in the mediapackage
    MetadataField presenters = abstractMetadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName());
    if (presenters != null && presenters.isUpdated() && presenters.getValue() instanceof Iterable<?>) {
      String[] creators = mediaPackage.getCreators();
      for (String creator : creators) {
        mediaPackage.removeCreator(creator);
      }
      for (String presenter : MetadataUtils.getIterableStringMetadata(presenters)) {
        mediaPackage.addCreator(presenter);
      }
    }

    MetadataField series = abstractMetadata.getOutputFields().get(DublinCore.PROPERTY_IS_PART_OF.getLocalName());
    if (series.getValue() != null && series.isUpdated()) {
      if (isNotBlank(series.getValue().toString())) {
        mediaPackage.setSeries(series.getValue().toString());
        final String seriesTitle = getSeriesTitle(series);
        if (seriesTitle != null)
          mediaPackage.setSeriesTitle(seriesTitle);
      } else {
        mediaPackage.setSeries(null);
        mediaPackage.setSeriesTitle(null);
      }
    }

    // Mediapackage start date is set by event metadata start date. The "created" metadata field is not used.
    MetadataField startDate = abstractMetadata.getOutputFields().get("startDate");
    if (startDate != null && startDate.getValue() != null && startDate.isUpdated()
            && isNotBlank(startDate.getValue().toString())) {
      try {
        SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(startDate.getPattern());
        mediaPackage.setDate(sdf.parse((String) startDate.getValue()));
      } catch (ParseException e) {
        logger.warn("Not able to parse start date {} to update media package {} because {}", startDate.getValue(),
                mediaPackage.getIdentifier(), e);
      }
    }

    // Update all the metadata related to the episode dublin core catalog
    MetadataField title = abstractMetadata.getOutputFields().get(DublinCore.PROPERTY_TITLE.getLocalName());
    if (title != null && title.isUpdated()) {
      mediaPackage.setTitle(title.getValue().toString());
    }
    return storeFields;
  }

  private String getSeriesTitle(MetadataField series) {
    if (series.getCollection() == null)
      return null;
    for (Map.Entry<String, String> e : series.getCollection().entrySet()) {
      if (e.getValue().equals(series.getValue().toString())) return e.getKey();
    }
    return null;
  }

}
