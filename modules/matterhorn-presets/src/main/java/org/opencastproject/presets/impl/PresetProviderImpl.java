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

package org.opencastproject.presets.impl;

import org.opencastproject.presets.api.PresetProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that provides presets.
 */
public class PresetProviderImpl implements PresetProvider {

  private static final Logger logger = LoggerFactory.getLogger(PresetProvider.class);

  private SeriesService seriesService;
  /** The security service to get the current user's organization */
  private SecurityService securityService;

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public String getProperty(String seriesID, String propertyName) throws NotFoundException {
    String propertyValue = "";
    if (StringUtils.trimToNull(seriesID) != null) {
      try {
        propertyValue = seriesService.getSeriesProperty(seriesID, propertyName);
      } catch (NotFoundException e) {
        logger.debug("The property {} was not found in the series {} so we will try to find it in the organization.",
                propertyName, seriesID);
      } catch (Exception e) {
        logger.warn(
                "Unable to get the property {} from the series {} so we will try to find it in the organization. The exception was ",
                new Object[] { propertyName, seriesID, ExceptionUtils.getStackTrace(e) });
      }
    }
    if (StringUtils.isBlank(propertyValue)) {
      propertyValue = securityService.getOrganization().getProperties().get(propertyName);
      if (StringUtils.isBlank(propertyValue)) {
        throw new NotFoundException("Unable to find the property in either the series or organization");
      }
    }
    return propertyValue;
  }

}
