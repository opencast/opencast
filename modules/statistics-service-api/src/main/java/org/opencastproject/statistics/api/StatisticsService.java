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

package org.opencastproject.statistics.api;


import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Statistics service API.
 *
 */
public interface StatisticsService {

  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.statistics";

  /**
   * @return All active providers.
   */
  Set<StatisticsProvider> getProviders();

  /**
   * @param resourceType
   *          The resource type.
   *
   * @return All active providers for the given resource type.
   */
  Set<StatisticsProvider> getProviders(ResourceType resourceType);

  /**
   * @param providerId
   *          The provider id.
   *
   * @return Optionally return the provider with the given id (if any).
   */
  Optional<StatisticsProvider> getProvider(String providerId);

  /**
   * Get time series statistics data from the given Provider.
   *
   * @param provider
   *          The provider to retrieve statistics from.
   * @param resourceId
   *          The id to access the resource to get statistics for (e.g. episode Id, organization Id or series Id).
   * @param from
   *          The start date to calculate the statistics for.
   * @param to
   *          The end date to calculate the statistics for.
   * @param resolution
   *          The resolution to get the statistics with.
   * @param zoneId
   *          The timezone to use for date calculations.
   * @return The time series data.
   */
  TimeSeries getTimeSeriesData(StatisticsProvider provider, String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId);

  /**
   * Write a duration to a statistics data base
   *
   * @param organizationId Organization ID of the data point
   * @param measurementName Measurement name of the data point
   * @param retentionPolicy Retention policy of the data point
   * @param organizationIdResourceName Resource name for the organization
   * @param fieldName Field name to write
   * @param temporalResolution The temporal resolution to store it in
   * @param duration The actual duration to write
   */
  void writeDuration(
          String organizationId,
          String measurementName,
          String retentionPolicy,
          String organizationIdResourceName,
          String fieldName,
          TimeUnit temporalResolution,
          Duration duration);
}
