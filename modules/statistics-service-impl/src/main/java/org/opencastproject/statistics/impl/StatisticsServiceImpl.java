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

package org.opencastproject.statistics.impl;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsProviderRegistry;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.StatisticsUtil;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implements {@link StatisticsService}. Uses influxdb for permanent storage.
 */
public class StatisticsServiceImpl implements StatisticsService, StatisticsProviderRegistry {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

  private Map<String, StatisticsProvider> providers = new ConcurrentHashMap<>();


  public void activate(ComponentContext cc) {
    logger.info("Activating Statistics Service");
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating Statistics Service");
  }

  @Override
  public Set<StatisticsProvider> getProviders() {
    return new HashSet<>(providers.values());
  }

  @Override
  public Set<StatisticsProvider> getProviders(ResourceType resourceType) {
    return providers
            .values()
            .stream()
            .filter(p -> p.getResourceType().equals(resourceType))
            .collect(Collectors.toSet());
  }

  @Override
  public Optional<StatisticsProvider> getProvider(String providerId) {
    return providers.values().stream().filter(p -> p.getId().equals(providerId)).findAny();
  }

  @Override
  public TimeSeries getTimeSeriesData(
          StatisticsProvider provider,
          String resourceId,
          Instant from,
          Instant to,
          DataResolution resolution,
          ZoneId zoneId) {
    if (!(provider instanceof TimeSeriesProvider)) {
      throw new IllegalArgumentException("The given provider '" + provider.getTitle()
          + "' (" + provider.getId() + ") does not provide time series data");

    }
    final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, resolution, zoneId);
    return fill(((TimeSeriesProvider) provider).getValues(resourceId, from, to, resolution, zoneId), buckets);
  }

  @Override
  public void addProvider(StatisticsProvider provider) {
    logger.info("Registering statistics provider: {}", provider.getId());
    providers.put(provider.getId(), provider);
  }

  @Override
  public void removeProvider(StatisticsProvider provider) {
    logger.info("Unregistering statistics provider: {}", provider.getId());
    providers.remove(provider.getId());
  }

  private static TimeSeries fill(final TimeSeries timeSeries, final List<Instant> buckets) {
    final List<Double> filledValues = new ArrayList<>();
    final List<String> labels = buckets.stream().map(Instant::toString).collect(Collectors.toList());
    for (final String label : labels) {
      final int labelIndex = timeSeries.getLabels().indexOf(label);
      if (labelIndex != -1) {
        filledValues.add(timeSeries.getValues().get(labelIndex));
      } else {
        filledValues.add(0d);
      }
    }
    final TimeSeries result = new TimeSeries(labels, filledValues);
    if (timeSeries.getTotal().isPresent()) {
      result.setTotal(timeSeries.getTotal().getAsDouble());
    }
    return result;
  }
}
