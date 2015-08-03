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

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This {@link SeriesCatalogUIAdapter} is the default implementation for series metadata.
 */
public class CommonSeriesCatalogUIAdapter extends ConfigurableSeriesDCCatalogUIAdapter implements ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(CommonSeriesCatalogUIAdapter.class);

  private SecurityService securityService;

  public CommonSeriesCatalogUIAdapter() {
    Properties seriesCatalogProperties = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/series-catalog.properties");
      seriesCatalogProperties.load(in);
    } catch (IOException e) {
      throw new ComponentException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
    try {
      updated(seriesCatalogProperties);
    } catch (ConfigurationException e) {
      logger.error("Error while configuring: {}", ExceptionUtils.getStackTrace(e));
      throw new IllegalStateException("The default series DublinCore catalog UI adapter has configuration problems", e);
    }
  }

  @Override
  public String getOrganization() {
    return securityService.getOrganization().getId();
  }

  @Override
  public String getFlavor() {
    return MediaPackageElements.SERIES.toString();
  }

  @Override
  public String getUITitle() {
    return "Opencast Series DublinCore";
  }

  /** OSGi callback to bind {@link SecurityService} instance. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  protected Opt<DublinCoreCatalog> loadDublinCoreCatalog(String seriesId) {
    try {
      return Opt.nul(getSeriesService().getSeries(requireNonNull(seriesId)));
    } catch (SeriesException e) {
      logger.error("Error while loading DublinCore catalog of series '{}': {}", seriesId,
              ExceptionUtils.getStackTrace(e));
      return Opt.none();
    } catch (NotFoundException e) {
      logger.debug("No DublinCore metadata catalog for series '{}' found", seriesId);
      return Opt.none();
    } catch (UnauthorizedException e) {
      logger.warn(
              "The current user does not have sufficient permissions to load the DublinCore metadata catalog of the series '{}'",
              seriesId);
      return Opt.none();
    }
  }

  @Override
  protected boolean saveDublinCoreCatalog(String seriesId, DublinCoreCatalog dc) {
    try {
      getSeriesService().updateSeries(dc);
    } catch (SeriesException e) {
      logger.warn("Error while updating series DublinCore: {}", ExceptionUtils.getStackTrace(e));
      return false;
    } catch (UnauthorizedException e) {
      logger.warn("User is not authorized to change series DublinCore");
      return false;
    }
    return true;
  }

}
