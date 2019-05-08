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
package org.opencastproject.external.util.statistics;

import org.opencastproject.statistics.api.DataResolution;

import org.json.simple.JSONArray;

import java.util.Optional;
import java.util.Set;

public final class DataResolutionUtils {

  private DataResolutionUtils() {
  }

  public static JSONArray toJson(Set<DataResolution> dataResolutions) {
    JSONArray result = new JSONArray();
    for (DataResolution resolution : dataResolutions) {
      result.add(toString(resolution));
    }
    return result;
  }

  public static String toString(DataResolution dataResolution) {
    return dataResolution.toString().toLowerCase();
  }

  public static Optional<DataResolution> fromString(String dataResolution) {
    try {
      return Optional.of(Enum.valueOf(DataResolution.class, dataResolution.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

}
