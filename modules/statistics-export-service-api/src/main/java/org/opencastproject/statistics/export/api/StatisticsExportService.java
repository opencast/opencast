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

package org.opencastproject.statistics.export.api;

import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.util.NotFoundException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public interface StatisticsExportService {


  /**
   * Get a CSV representation for the query result based on the given parameters.
   *
   * @param provider
   *          The provider to get the data from.
   * @param resourceId
   *          The id of the resource to get the data for.
   * @param from
   *          The start date of the time range to get the data for.
   * @param to
   *          The end date of the time range to get the data for.
   * @param dataResolution
   *          The data resolution.
   * @param index
   *          The index to get event or series meta data from.
   * @param zoneId
   *          The ZoneId to use for date formatting.
   * @return The data as CSV.
   * @throws SearchIndexException
   *           If the search index cannot be queried.
   * @throws UnauthorizedException
   *           If the user is not authorized to get the desired data.
   * @throws NotFoundException
   *           If the resource identified by resourceId could not be found.
   */
  String getCSV(StatisticsProvider provider, String resourceId, Instant from, Instant to, DataResolution dataResolution,
                AbstractSearchIndex index, ZoneId zoneId) throws SearchIndexException, UnauthorizedException,
          NotFoundException;

  /**
   * Get a CSV representation for the query result based on the given parameters.
   *
   * @param provider
   *          The provider to get the data from.
   * @param resourceId
   *          The id of the resource to get the data for.
   * @param from
   *          The start date of the time range to get the data for.
   * @param to
   *          The end date of the time range to get the data for.
   * @param dataResolution
   *          The data resolution.
   * @param index
   *          The index to get event or series meta data from.
   * @param zoneId
   *          The ZoneId to use for date formatting.
   * @param fullMetadata
   *          When true, creates a full export with all available meta data fields.
   * @param limit
   *         limit to use for pagination. Pass 0 for unlimited (not recommeded).
   * @param offset
   *         offset to use for pagination.
   * @param filters
   *         filters to apply when searching for events/series.
   * @return The data as CSV.
   * @throws SearchIndexException
   *           If the search index cannot be queried.
   * @throws UnauthorizedException
   *           If the user is not authorized to get the desired data.
   * @throws NotFoundException
   *           If the resource identified by resourceId could not be found.
   */
  String getCSV(StatisticsProvider provider, String resourceId, Instant from, Instant to, DataResolution dataResolution,
          AbstractSearchIndex index, ZoneId zoneId, boolean fullMetadata, DetailLevel detailLevel, int limit,
          int offset, Map<String, String> filters)
          throws SearchIndexException, UnauthorizedException, NotFoundException;
}
