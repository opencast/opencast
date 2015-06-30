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

import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.MetadataField;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.IoSupport;

import com.entwinemedia.fn.data.Opt;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Episode dublincore catalog implementation of a {@link AbstractEventsCatalogUIAdapter}
 */
public class CommonEventCatalogUIAdapter extends ConfigurableEventDCCatalogUIAdapter implements ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CommonEventCatalogUIAdapter.class);

  public static final String EPISODE_TITLE = "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE";

  private SecurityService securityService;

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback. */
  public void activate() {
    Properties episodeCatalogProperties = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/episode-catalog.properties");
      episodeCatalogProperties.load(in);
    } catch (IOException e) {
      throw new ComponentException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
    try {
      updated(episodeCatalogProperties);
      logger.info("Activated episode dublin core catalog UI adapter");
    } catch (ConfigurationException e) {
      logger.error("Error while configuring: {}", ExceptionUtils.getStackTrace(e));
      throw new IllegalStateException("The default series DublinCore catalog UI adapter has configuration problems", e);
    }
  }

  @Override
  public String getUITitle() {
    return EPISODE_TITLE;
  }

  @Override
  public String getOrganization() {
    return securityService.getOrganization().getId();
  }

  @Override
  public MediaPackageElementFlavor getFlavor() {
    return MediaPackageElements.EPISODE;
  }

  @Override
  public Catalog storeFields(MediaPackage mediaPackage, AbstractMetadataCollection abstractMetadata) {
    Catalog storeFields = super.storeFields(mediaPackage, abstractMetadata);

    // Update the metadata stored in the mediapackage
    MetadataField<?> presenters = abstractMetadata.getOutputFields().get("creator");
    if (presenters != null && presenters.isUpdated() && presenters.getValue() instanceof Iterable<?>) {
      String[] creators = mediaPackage.getCreators();
      for (String creator : creators) {
        mediaPackage.removeCreator(creator);
      }
      for (String presenter : MetadataUtils.getIterableStringMetadata(presenters)) {
        mediaPackage.addCreator(presenter);
      }
    }

    MetadataField<?> series = abstractMetadata.getOutputFields().get("series");
    if (series != null && series.isUpdated())
      mediaPackage.setSeries(series.getValue().toString());

    Opt<Date> startDate = MetadataUtils.getUpdatedDateMetadata(abstractMetadata, "startDate");
    if (startDate != null && startDate.isSome())
      mediaPackage.setDate(startDate.get());

    // Update all the metadata related to the episode dublin core catalog
    MetadataField<?> title = abstractMetadata.getOutputFields().get("title");
    if (title != null && title.isUpdated()) {
      mediaPackage.setTitle(title.getValue().get().toString());
    }
    return storeFields;
  }

}
