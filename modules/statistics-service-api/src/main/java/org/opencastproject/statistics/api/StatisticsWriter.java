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
import java.util.concurrent.TimeUnit;

/**
 * Interface for a class that can write statistical data of various data types
 */
public interface StatisticsWriter {
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

  /**
   * Lorem ipsum dolor sit amet
   * @return the writer's ID
   */
  String getId();
}
