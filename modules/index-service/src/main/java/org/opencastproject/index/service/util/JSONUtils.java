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

package org.opencastproject.index.service.util;

import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Utility class providing helpers for all operation related to JSON.
 */
public final class JSONUtils {

  /** This regex is used to reduce the users in the filter selectbox.
   * The filter is located in the top right corner in the admin ui. */
  private static String userFilterRegex;
  private static final String[] userListsToReduce = {"CONTRIBUTORS", "PUBLISHER",
      "ORGANIZERS", "CONTRIBUTORS.USERNAMES", "EVENTS.PUBLISHER", "USERS.NAME"};

  private JSONUtils() {

  }

  public static String safeString(Object input) {
    return input != null ? input.toString() : "";
  }

  /**
   * Turn a collection into a {@link JsonArray}
   *
   * @param collection
   *          the collection
   * @return a new {@link JsonArray} generated with the colleciton values
   */
  public static JsonArray collectionToJsonArray(Collection<?> collection) {
    JsonArray jsonArray = new JsonArray();

    if (collection != null) {
      for (Object item : collection) {
        jsonArray.add(convertToJsonElement(item));
      }
    }

    return jsonArray;
  }

  public static <T> JsonArray arrayToJsonArray(T[] array) {
    JsonArray jsonArray = new JsonArray();

    if (array != null) {
      for (T item : array) {
        jsonArray.add(convertToJsonElement(item));
      }
    }

    return jsonArray;
  }


  /**
   * Turn a map into a {@link JsonObject} object
   *
   * @param map
   *          the source map
   * @return a new {@link JsonObject} generated with the map values
   */
  public static JsonObject mapToJsonObject(Map<?, ?> map) {
    JsonObject jsonObject = new JsonObject();

    if (map != null) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = String.valueOf(entry.getKey());
        JsonElement value = convertToJsonElement(entry.getValue());
        jsonObject.add(key, value);
      }
    }

    return jsonObject;
  }

  private static JsonElement convertToJsonElement(Object item) {
    if (item == null) {
      return JsonNull.INSTANCE;
    }
    if (item instanceof JsonElement) {
      return (JsonElement) item;
    }
    if (item instanceof Number) {
      return new JsonPrimitive((Number) item);
    }
    if (item instanceof Boolean) {
      return new JsonPrimitive((Boolean) item);
    }
    if (item instanceof Character) {
      return new JsonPrimitive((Character) item);
    }
    if (item instanceof String) {
      return new JsonPrimitive((String) item);
    }
    return new JsonPrimitive(item.toString());
  }


  /**
   * Generate JSON presentation of the given filters
   *
   * @param query
   *          The {@link ResourceListQuery}
   * @param listProvidersService
   *          The {@link ListProvidersService} to get the possible values
   * @param org
   *          The {@link Organization}
   * @return
   * @throws ListProviderException
   *           if the possible values can not be retrieved correctly from the list provider.
   */
  public static JsonObject filtersToJSON(ResourceListQuery query, ListProvidersService listProvidersService,
      Organization org) throws ListProviderException {

    JsonObject filtersJson = new JsonObject();

    List<ResourceListFilter<?>> filters = query.getAvailableFilters();

    for (ResourceListFilter<?> f : filters) {
      JsonObject filterObject = new JsonObject();

      filterObject.addProperty("type", f.getSourceType().toString().toLowerCase());
      filterObject.addProperty("label", f.getLabel());

      Optional<String> listProviderName = f.getValuesListName();

      if (listProviderName.isPresent()) {
        Map<String, String> values;
        boolean translatable = false;

        String providerName = listProviderName.get();

        if (org != null
            && !listProvidersService.hasProvider(providerName, org.getId())
            && !listProvidersService.hasProvider(providerName)) {
          values = new HashMap<>();
        } else {
          values = listProvidersService.getList(providerName, query, false);
          if (Arrays.asList(userListsToReduce).contains(providerName)) {
            // reduces the user list ('values' map) by the configured userFilterRegex
            values.keySet().removeIf(u -> !u.matches(userFilterRegex));
          }
          translatable = listProvidersService.isTranslatable(providerName);
        }

        JsonObject optionsJson = new JsonObject();
        for (Entry<String, String> entry : values.entrySet()) {
          optionsJson.addProperty(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }

        filterObject.add("options", optionsJson);
        filterObject.addProperty("translatable", translatable);
      }

      filtersJson.add(f.getName(), filterObject);
    }

    return filtersJson;
  }

  /**
   * Generate JSON presentation of the given filters
   *
   * @param query
   *          The {@link ResourceListQuery}
   * @param listProvidersService
   *          The {@link ListProvidersService} to get the possible values
   * @param series
   *          The Series with write access
   * @return
   * @throws ListProviderException
   *           if the possible values can not be retrieved correctly from the list provider.
   */
  public static JsonObject filtersToJSONSeriesWriteAccess(ResourceListQuery query,
      ListProvidersService listProvidersService, Map<String, String> series) throws ListProviderException {
    JsonObject filtersJson = new JsonObject();

    for (ResourceListFilter<?> filter : query.getAvailableFilters()) {
      JsonObject filterObject = new JsonObject();
      filterObject.addProperty("type", filter.getSourceType().toString().toLowerCase());
      filterObject.addProperty("label", filter.getLabel());

      Optional<String> listProviderName = filter.getValuesListName();

      if (listProviderName.isPresent()) {
        String providerName = listProviderName.get();
        JsonObject optionsJson = new JsonObject();
        boolean translatable = false;

        if (listProvidersService.hasProvider(providerName)) {
          if (listProviderName.get().equals("SERIES")) {
            for (Entry<String, String> entry : series.entrySet()) {
              optionsJson.addProperty(entry.getValue(), entry.getKey() != null ? entry.getKey() : "");
            }
          } else {
            Map<String, String> values = listProvidersService.getList(providerName, query, false);
            for (Entry<String, String> entry : values.entrySet()) {
              optionsJson.addProperty(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }
          }

          translatable = listProvidersService.isTranslatable(providerName);
        }

        filterObject.add("options", optionsJson);
        filterObject.addProperty("translatable", translatable);
      }

      filtersJson.add(filter.getName(), filterObject);
    }

    return filtersJson;
  }

  /**
   * Returns a JSON object with key-value from given map
   *
   * @param map
   *          The source list for the JSON object
   * @return a JSON object containing the all the key-value as parameter
   * @throws JSONException
   */
  public static JSONObject fromMap(Map<String, String> map) throws JSONException {
    JSONObject json = new JSONObject();

    if (map == null) {
      return json;
    }

    for (Entry<String, String> entry : map.entrySet()) {
      json.put(entry.getKey(), entry.getValue());
    }
    return json;
  }

  /**
   * Converts a JSON object to a map. All values are of type {@link String}
   *
   * @param json
   *          the JSON object to convert
   * @return the map
   */
  public static Map<String, String> toMap(JSONObject json) {
    if (json == null) {
      return Collections.emptyMap();
    }

    HashMap<String, String> map = new HashMap<String, String>();
    for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
      String key = iterator.next();
      map.put(key, json.optString(key));
    }

    return map;
  }

  public static void setUserRegex(String regex) {
    userFilterRegex = regex;
  }

}
