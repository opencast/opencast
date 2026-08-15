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
package org.opencastproject.external.util.statistics;

import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.statistics.export.api.DetailLevel;
import org.opencastproject.util.data.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class StatisticsProviderUtils {

  private static final String PROVIDER_TYPE_TIME_SERIES = "timeseries";
  private static final String PROVIDER_TYPE_UNKNOWN = "unknown";

  private static final String PARAMETER_TYPE_DATETIME = "datetime";
  private static final String PARAMETER_TYPE_ENUMERATION = "enumeration";
  private static final String PARAMETER_TYPE_STRING = "string";


  private StatisticsProviderUtils() {
  }

  public static String typeOf(StatisticsProvider provider) {
    if (provider instanceof TimeSeriesProvider) {
      return PROVIDER_TYPE_TIME_SERIES;
    } else {
      return PROVIDER_TYPE_UNKNOWN;
    }
  }

  public static JsonObject toJson(StatisticsProvider provider, Boolean withParameters) {
    final JsonObject result = new JsonObject();
    result.addProperty("identifier", provider.getId());
    result.addProperty("title", provider.getTitle());
    result.addProperty("description", provider.getDescription());
    result.addProperty("type", typeOf(provider));
    result.addProperty("resourceType", ResourceTypeUtils.toString(provider.getResourceType()));
    if (withParameters != null && withParameters && provider instanceof TimeSeriesProvider) {
      JsonArray parameters = new JsonArray();
      addParameter(parameters, "resourceId", PARAMETER_TYPE_STRING, false);
      addParameter(parameters, "from", PARAMETER_TYPE_DATETIME, false);
      addParameter(parameters, "to", PARAMETER_TYPE_DATETIME, false);
      addEnumParameter(parameters, "dataResolution",
          DataResolutionUtils.toJson(((TimeSeriesProvider) provider).getDataResolutions()), false);
      addEnumParameter(parameters, "detailLevel",
              DetailLevelUtils.toJson(Collections.set(DetailLevel.values())), true);
      result.add("parameters", parameters);
    }
    return result;
  }

  private static JsonObject createParameter(String name, String type, Boolean optional) {
    JsonObject paramJson = new JsonObject();
    paramJson.addProperty("name", name);
    paramJson.addProperty("type", type);
    paramJson.addProperty("optional", optional);
    return paramJson;
  }

  private static void addParameter(JsonArray parameters, String name, String type, Boolean optional) {
    parameters.add(createParameter(name, type, optional));
  }

  private static void addEnumParameter(JsonArray parameters, String name, JsonArray values, Boolean optional) {
    JsonObject enumJson = createParameter(name, PARAMETER_TYPE_ENUMERATION, optional);
    enumJson.add("values", values);
    parameters.add(enumJson);
  }
}
