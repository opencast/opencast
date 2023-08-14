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

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

/**
 * A provider which provied time series data.
 */
public interface TimeSeriesProvider extends StatisticsProvider {

  /**
   * Get the time series data provided by this provider.
   *
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
  TimeSeries getValues(String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId);


  /**
   * @return A set of {@link DataResolution}s supported by this provider.
   */
  Set<DataResolution> getDataResolutions();
}
