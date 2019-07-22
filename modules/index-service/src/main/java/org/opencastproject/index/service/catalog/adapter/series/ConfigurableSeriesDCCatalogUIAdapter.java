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

package org.opencastproject.index.service.catalog.adapter.series;

import static java.util.Objects.requireNonNull;

import org.opencastproject.index.service.catalog.adapter.ConfigurableDCCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.index.service.catalog.adapter.events.ConfigurableEventDCCatalogUIAdapter;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCoreByteFormat;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * A series catalog UI adapter that is managed by a configuration.
 */
public class ConfigurableSeriesDCCatalogUIAdapter extends ConfigurableDCCatalogUIAdapter
        implements SeriesCatalogUIAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurableEventDCCatalogUIAdapter.class);

  private SeriesService seriesService;
  private SecurityService securityService;

  @Override
  public Opt<MetadataCollection> getFields(String seriesId) {

    final Opt<DublinCoreCatalog> optDCCatalog = loadDublinCoreCatalog(
            RequireUtil.requireNotBlank(seriesId, "seriesId"));
    if (optDCCatalog.isNone()) {
      return Opt.none();
    }
    return Opt.some(getFieldsFromCatalogs(Arrays.asList(optDCCatalog.get())));
  }

  @Override
  public boolean storeFields(String seriesId, MetadataCollection metadata) {
    final Opt<DublinCoreCatalog> optDCCatalog = loadDublinCoreCatalog(
            RequireUtil.requireNotBlank(seriesId, "seriesId"));
    if (optDCCatalog.isSome()) {
      final DublinCoreCatalog dc = optDCCatalog.get();
      dc.addBindings(config.getXmlNamespaceContext());
      DublinCoreMetadataUtil.updateDublincoreCatalog(dc, metadata);
      saveDublinCoreCatalog(seriesId, dc);
      return true;
    } else {
      return false;
    }
  }

  protected Opt<DublinCoreCatalog> loadDublinCoreCatalog(String seriesId) {
    try {
      Opt<byte[]> seriesElementData = getSeriesService().getSeriesElementData(requireNonNull(seriesId), flavor.getType());
      if (seriesElementData.isSome()) {
        final DublinCoreCatalog dc = DublinCoreByteFormat.read(seriesElementData.get());
        // Make sure that the catalog has its flavor set.
        // It may happen, when updating a system, that already saved catalogs
        // do not have a flavor.
        dc.setFlavor(flavor);
        dc.addBindings(config.getXmlNamespaceContext());
        return Opt.some(dc);
      } else {
        final DublinCoreCatalog dc = DublinCores.mkStandard();
        dc.addBindings(config.getXmlNamespaceContext());
        dc.setRootTag(new EName(config.getCatalogXmlRootNamespace(), config.getCatalogXmlRootElementName()));
        dc.setFlavor(flavor);
        return Opt.some(dc);
      }
    } catch (SeriesException e) {
      logger.error("Error while loading DublinCore catalog of series '{}': {}", seriesId,
              ExceptionUtils.getStackTrace(e));
      return Opt.none();
    }
  }

  protected boolean saveDublinCoreCatalog(String seriesId, DublinCoreCatalog dc) {
    try {
      final byte[] dcData = dc.toXmlString().getBytes("UTF-8");
      if (getSeriesService().getSeriesElementData(seriesId, flavor.getType()).isSome()) {
        return getSeriesService().updateSeriesElement(seriesId, flavor.getType(), dcData);
      } else {
        return getSeriesService().addSeriesElement(seriesId, flavor.getType(), dcData);
      }
    } catch (IOException e) {
      logger.error("Error while serializing the dublin core catalog to XML", e);
      return false;
    } catch (SeriesException e) {
      logger.error("Error while saving the series element", e);
      return false;
    }
  }

  /**
   * Return the {@link SeriesService} to get access to the metadata catalogs of a series
   */
  protected SeriesService getSeriesService() {
    return seriesService;
  }

  /**
   * OSGi callback to bind instance of {@link SeriesService}
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * Return instance of {@link SecurityService}.
   */
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * OSGi callback to bind instance of {@link SecurityService}
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
