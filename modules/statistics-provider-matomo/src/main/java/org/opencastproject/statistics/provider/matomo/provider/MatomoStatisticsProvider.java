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

package org.opencastproject.statistics.provider.matomo.provider;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.provider.matomo.StatisticsProviderMatomoService;


public abstract class MatomoStatisticsProvider implements StatisticsProvider {

  protected StatisticsProviderMatomoService service;
  private String id;
  private ResourceType resourceType;
  private String title;
  private String description;

  public MatomoStatisticsProvider(
      StatisticsProviderMatomoService service,
      String id,
      ResourceType resourceType,
      String title,
      String description
  ) {
    this.service = service;
    this.id = id;
    this.resourceType = resourceType;
    this.title = title;
    this.description = description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public ResourceType getResourceType() {
    return resourceType;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getDescription() {
    return description;
  }

  protected static String dataResolutionToMatomoPeriod(DataResolution dataResolution) {
    switch (dataResolution) {
      case HOURLY:
        return "day";
      case DAILY:
        return "day";
      case WEEKLY:
        return "week";
      case MONTHLY:
        return "month";
      case YEARLY:
        return "year";
      default:
        throw new IllegalArgumentException("unmapped DataResolution: " + dataResolution.name());
    }
  }

  @Override
  public int hashCode() {
    return this.getId().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MatomoStatisticsProvider)) {
      return false;
    }
    final StatisticsProvider other = (StatisticsProvider) o;
    return this.getId().equals(other.getId());
  }
}
