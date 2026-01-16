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
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.statistics.provider.matomo.StatisticsProviderMatomoService;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;


public class MatomoTimeSeriesStatisticsProvider extends MatomoStatisticsProvider implements TimeSeriesProvider {


  private final MatomoProviderConfiguration.MatomoProviderSource source;
  private final BatchMatomoRequest batchRequest;

  public MatomoTimeSeriesStatisticsProvider(
      StatisticsProviderMatomoService service,
      String id,
      ResourceType resourceType,
      String title,
      String description,
      Set<MatomoProviderConfiguration.MatomoProviderSource> sources
  ) {
    super(service, id, resourceType, title, description);
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("At least one source must be provided");
    }
    if (sources.size() > 1) {
      throw new IllegalArgumentException("Currently only one source is supported");
    }
    this.source = sources.iterator().next();

    BatchMatomoRequest request = service.getBatchRequest(resourceType.toString() + source.getMethod());
    if (request == null) {
      request = new BatchMatomoRequest(service, source.getMethod());
      service.registerBatchRequest(resourceType.toString() + source.getMethod(), request);
    }
    request.addProvider(this, source.getAggregationVariable(), source.getAggregation());
    this.batchRequest = request;
  }

  @Override
  public TimeSeries getValues(String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId) {
    if (!source.getResolutions().contains(resolution)) {
      throw new IllegalArgumentException("Resolution " + resolution + " not supported by this provider");
    }
    final String matomoPeriod = dataResolutionToMatomoPeriod(resolution);

    // Execute batch request
    Map<String, TimeSeries> results = batchRequest.executeRequest(
        resourceId,
        from,
        to,
        matomoPeriod,
        source.getSiteId(),
        source.getDimensionId(),
        zoneId,
        resolution
    );

    // Return this provider's result
    return results.get(getId());
  }

  @Override
  public Set<DataResolution> getDataResolutions() {
    if (source == null) {
      throw new IllegalStateException("Source configuration is not initialized");
    }
    return source.getResolutions();
  }

}
