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

package org.opencastproject.index.service.util;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;

import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

  /**
   * Turn a map into a {@link JObject} object
   *
   * @param map
   *          the source map
   * @return a new {@link JObject} generated with the map values
   */
  public static JObject mapToJSON(Map<String, String> map) {
    if (map == null) {
      throw new IllegalArgumentException("Map must not be null!");
    }

    List<Field> fields = new ArrayList<Field>();
    for (Entry<String, String> item : map.entrySet()) {
      fields.add(f(item.getKey(), v(item.getValue(), Jsons.BLANK)));
    }
    return obj(fields);
  }

  /**
   * Turn a set into a {@link JObject} object
   *
   * @param set
   *          the source set
   * @return a new {@link JObject} generated with the map values
   */
  public static JValue setToJSON(Set<String> set) {
    if (set == null) {
      return arr();
    }
    List<JValue> arrEntries = new ArrayList<JValue>();
    for (String item : set) {
      arrEntries.add(v(item, Jsons.BLANK));
    }
    return arr(arrEntries);
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
  public static JValue filtersToJSON(ResourceListQuery query, ListProvidersService listProvidersService,
          Organization org) throws ListProviderException {

    List<Field> filtersJSON = new ArrayList<Field>();
    List<Field> fields = null;
    List<ResourceListFilter<?>> filters = query.getAvailableFilters();

    for (ResourceListFilter<?> f : filters) {
      fields = new ArrayList<Field>();

      fields.add(f("type", v(f.getSourceType().toString().toLowerCase())));
      fields.add(f("label", v(f.getLabel())));

      Option<String> listProviderName = f.getValuesListName();

      if (listProviderName.isSome()) {
        Map<String, String> values = null;
        boolean translatable = false;

        if (org != null && !listProvidersService.hasProvider(listProviderName.get(), org.getId())
                && !listProvidersService.hasProvider(listProviderName.get())) {
          values = new HashMap<String, String>();
        } else {
          values = listProvidersService.getList(listProviderName.get(), query, false);
          if (Arrays.asList(userListsToReduce).contains(listProviderName.get())) {
            // reduces the user list ('values' map) by the configured userFilterRegex
            values.keySet().removeIf(u -> !u.matches(userFilterRegex));
          }
          translatable = listProvidersService.isTranslatable(listProviderName.get());
        }

        List<Field> valuesJSON = new ArrayList<Field>();
        for (Entry<String, String> entry : values.entrySet()) {
          valuesJSON.add(f(entry.getKey(), v(entry.getValue(), Jsons.BLANK)));
        }

        fields.add(f("options", obj(valuesJSON)));
        fields.add(f("translatable", translatable));
      }

      filtersJSON.add(f(f.getName(), obj(fields)));
    }

    return obj(filtersJSON);
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
   * @param series
   *          The Series with write access
   * @return
   * @throws ListProviderException
   *           if the possible values can not be retrieved correctly from the list provider.
   */
  public static JValue filtersToJSONSeriesWriteAccess(ResourceListQuery query, ListProvidersService listProvidersService,
          Organization org, Map<String, String> series) throws ListProviderException {

    List<Field> filtersJSON = new ArrayList<Field>();
    List<Field> fields = null;
    List<ResourceListFilter<?>> filters = query.getAvailableFilters();

    for (ResourceListFilter<?> filter : filters) {
      fields = new ArrayList<Field>();

      fields.add(f("type", v(filter.getSourceType().toString().toLowerCase())));
      fields.add(f("label", v(filter.getLabel())));

      Option<String> listProviderName = filter.getValuesListName();

      if (listProviderName.isSome()) {
        boolean translatable = false;
        List<Field> valuesJSON = new ArrayList<>();

        if (listProvidersService.hasProvider(listProviderName.get())) {
          if (listProviderName.get().equals("SERIES")) {
            for (Entry<String, String> entry : series.entrySet()) {
              valuesJSON.add(f(entry.getValue(), v(entry.getKey(), Jsons.BLANK)));
            }
          } else {
            Map<String, String> values = listProvidersService.getList(listProviderName.get(), query, false);
            for (Entry<String, String> entry : values.entrySet()) {
              valuesJSON.add(f(entry.getKey(), v(entry.getValue(), Jsons.BLANK)));
            }
          }
          translatable = listProvidersService.isTranslatable(listProviderName.get());
        }

        fields.add(f("options", obj(valuesJSON)));
        fields.add(f("translatable", translatable));
      }

      filtersJSON.add(f(filter.getName(), obj(fields)));
    }

    return obj(filtersJSON);
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

    if (map == null)
      return json;

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
    if (json == null)
      return Collections.emptyMap();

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
