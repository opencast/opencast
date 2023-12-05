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

import org.opencastproject.statistics.export.api.DetailLevel;

import org.json.simple.JSONArray;

import java.util.Optional;
import java.util.Set;

public final class DetailLevelUtils {

  private DetailLevelUtils() {
  }

  public static JSONArray toJson(Set<DetailLevel> detailLevels) {
    JSONArray result = new JSONArray();
    for (DetailLevel resolution : detailLevels) {
      result.add(toString(resolution));
    }
    return result;
  }

  public static String toString(DetailLevel detailLevel) {
    return detailLevel.toString().toLowerCase();
  }

  public static Optional<DetailLevel> fromString(String detailLevel) {
    try {
      return Optional.of(Enum.valueOf(DetailLevel.class, detailLevel.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

}
