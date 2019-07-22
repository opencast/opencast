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

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

  public static final String PATTERN_ISO_DATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

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
   * Create a JSON Array with the list given as String containing values separated by the given separator;
   *
   * @param list
   *          The list of values as String
   * @param separator
   *          The separator used in the list between each value
   * @return a JSON Array as {@link JValue}
   * @throws IllegalArgumentException
   *           - if the separator is not set
   */
  public static JValue jsonArrayFromString(String list, String separator) {
    if (StringUtils.isEmpty(separator))
      throw new IllegalArgumentException("The separator must be defined!");

    if (StringUtils.isBlank(list))
      return arr();

    List<JValue> values = new ArrayList<JValue>();
    for (String value : list.split(separator))
      values.add(v(value));

    return arr(values);
  }

  /**
   * Format the given Date as ISO String Date using the pattern "yyyy-MM-dd'T'HH:mm:ss'Z'".
   *
   * @param date
   *          The date to format
   * @return The date as ISO Date String
   * @throws IllegalArgumentException
   */
  public static String formatIsoDate(Date date) {
    if (date == null)
      throw new IllegalArgumentException("The given date must not be null.");
    return DateTimeSupport.toUTC(date.getTime());
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
   * Format the given period (define by start and end dates) to a JSON value.
   *
   * <pre>
   * {
   *    "start": 2012-12-20T23:11:23Z,
   *    "end": 2012-12-22T10:11:23Z
   * }
   * </pre>
   *
   * @param start
   *          The period start date
   * @param end
   *          The period end date
   * @return A {@link JValue} representing the period with a start and end property
   */
  public static JValue formatPeriod(Date start, Date end) {
    if (start == null || end == null)
      throw new IllegalArgumentException("The given start or end date from the period must not be null!");

    return obj(f("start", v(formatIsoDate(start))), f("end", v(formatIsoDate(end))));
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

}
