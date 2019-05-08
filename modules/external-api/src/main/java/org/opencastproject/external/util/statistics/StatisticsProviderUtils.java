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

import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.TimeSeriesProvider;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

  public static JSONObject toJson(StatisticsProvider provider, Boolean withParameters) {
    final JSONObject result = new JSONObject();
    result.put("identifier", provider.getId());
    result.put("title", provider.getTitle());
    result.put("description", provider.getDescription());
    result.put("type", typeOf(provider));
    result.put("resourceType", ResourceTypeUtils.toString(provider.getResourceType()));
    if (withParameters && provider instanceof TimeSeriesProvider) {
      JSONArray parameters = new JSONArray();
      addParameter(parameters, "resourceId", PARAMETER_TYPE_STRING, false);
      addParameter(parameters, "from", PARAMETER_TYPE_DATETIME, false);
      addParameter(parameters, "to", PARAMETER_TYPE_DATETIME, false);
      addEnumParameter(parameters, "dataResolution",
          DataResolutionUtils.toJson(((TimeSeriesProvider) provider).getDataResolutions()), false);
      result.put("parameters", parameters);
    }
    return result;
  }

  private static JSONObject createParameter(String name, String type, Boolean optional) {
    JSONObject paramJson = new JSONObject();
    paramJson.put("name", name);
    paramJson.put("type", type);
    paramJson.put("optional", optional);
    return paramJson;
  }

  private static void addParameter(JSONArray parameters, String name, String type, Boolean optional) {
    parameters.add(createParameter(name, type, optional));
  }

  private static void addEnumParameter(JSONArray parameters, String name, JSONArray values, Boolean optional) {
    JSONObject enumJson = createParameter(name, PARAMETER_TYPE_ENUMERATION, optional);
    enumJson.put("values", values);
    parameters.add(enumJson);
  }
}
