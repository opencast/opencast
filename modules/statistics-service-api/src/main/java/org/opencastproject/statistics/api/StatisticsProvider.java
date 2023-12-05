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

/**
 * Provider for statistics. A provider may provide statistics for only one {@link ResourceType}, but for different
 * {@link DataResolution}s. So e.g. there may be an "EpisodeViewsProvider" which provides the view counts of episodes
 * on a daily, monthly, and yearly basis.
 */
public interface StatisticsProvider {

  /**
   * @return The unique identifier of the provider.
   */
  String getId();

  /**
   * @return The type of resources this provider provides statistics for.
   */
  ResourceType getResourceType();

  /**
   * @return A title to display with the charts generated from the statistics provided by this provider. This should be
   * a translation string key.
   */
  String getTitle();

  /**
   * @return A description to display with the charts generated from the statistics provided by this provider. This
   * should be a translation string key.
   */
  String getDescription();
}
