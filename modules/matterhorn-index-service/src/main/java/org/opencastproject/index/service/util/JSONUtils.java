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

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JObjectWrite;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class providing helpers for all operation related to JSON.
 */
public final class JSONUtils {

  public static final String PATTERN_ISO_DATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** Date formatter for ISO Date */
  private static final SimpleDateFormat isoDateFormatter = new SimpleDateFormat(PATTERN_ISO_DATE);

  private JSONUtils() {

  }

  /**
   * Turn a map into a {@link JObjectWrite} object
   *
   * @param map
   *          the source map
   * @return a new {@link JObjectWrite} generated with the map values
   */
  public static JObjectWrite mapToJSON(Map<String, Object> map) {
    if (map == null) {
      throw new IllegalArgumentException("Map must not be null!");
    }

    List<JField> fields = new ArrayList<JField>();
    for (Entry<String, Object> item : map.entrySet()) {
      fields.add(f(item.getKey(), vN(item.getValue())));
    }
    return j(fields);
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
      return a();

    List<JValue> values = new ArrayList<JValue>();
    for (String value : list.split(separator))
      values.add(v(value));

    return a(values);
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

  // CHECKSTYLE:OFF These functions are seen as variables by checkstyles
  public static Function<Person, Val> personToJsonVal = new Function<Person, Val>() {
    @Override
    public Val apply(Person a) {
      return obj(p("id", a.getId()), p("email", a.getEmail()), p("name", a.getName()));
    }
  };

  public static Function<Recording, Val> recordingToJsonVal = new Function<Recording, Val>() {
    @Override
    public Val apply(Recording a) {
      return obj(p("id", a.getId().get()), p("title", a.getTitle()));
    }
  };

  // CHECKSTYLE:ON

  /**
   * Wrap the given blacklists in a JSON object. Only the current period or the next upcoming will be added in the
   * object and formatted using the method {@link #formatPeriod}.
   *
   * @param blacklist
   *          The blacklists to wrap in a JSON object
   * @return a {@link JValue} containing all the blacklist
   * @throws IllegalArgumentException
   *           If the given list is null
   */
  public static JValue blacklistToJSON(List<Blacklist> blacklist) {
    if (blacklist == null)
      throw new IllegalArgumentException("The blacklist must not be null!");

    JValue blacklistJSON = v("");
    Date now = new Date();
    Date blacklistStart = null;
    Date blacklistEnd = null;

    for (Blacklist item : blacklist) {
      List<Period> periods = item.getPeriods();
      for (Period p : periods) {
        Date start = p.getStart();
        Date end = p.getEnd();
        // Set this period as current/next period if
        if ((now.after(start) && now.before(end)) // the period already started but is not finished
                // or if the period is the next one
                || (now.before(end) && (blacklistStart == null || blacklistStart.after(start)))) {
          blacklistStart = start;
          blacklistEnd = end;
        }
      }
    }

    // If the blacklist contains periods, we format the current or next one to JSON
    if (blacklistStart != null && blacklistEnd != null)
      blacklistJSON = formatPeriod(blacklistStart, blacklistEnd);

    return blacklistJSON;
  }

  /**
   * Generate JSON presentation of the given filters
   *
   * @param query
   *          The {@link ResourceListQuery}
   * @param listProviderService
   *          The {@link ListProvidersService} to get the possible values
   * @param org
   *          The {@link Organization}
   * @return
   * @throws ListProviderException
   *           if the possible values can not be retrieved correctly from the list provider.
   */
  public static JValue filtersToJSON(ResourceListQuery query, ListProvidersService listProvidersService,
          Organization org) throws ListProviderException {

    List<JField> filtersJSON = new ArrayList<JField>();
    List<JField> fields = null;
    List<ResourceListFilter<?>> filters = query.getAvailableFilters();

    for (ResourceListFilter<?> f : filters) {
      fields = new ArrayList<JField>();

      fields.add(f("type", v(f.getSourceType().toString().toLowerCase())));
      fields.add(f("label", v(f.getLabel())));

      Option<String> listProviderName = f.getValuesListName();

      if (listProviderName.isSome()) {
        Map<String, Object> values = null;

        if (!listProvidersService.hasProvider(listProviderName.get()))
          values = new HashMap<String, Object>();
        else
          values = listProvidersService.getList(listProviderName.get(), query, org);

        List<JField> valuesJSON = new ArrayList<JField>();
        for (Entry<String, Object> entry : values.entrySet()) {
          valuesJSON.add(f(entry.getKey(), vN(entry.getValue())));
        }

        fields.add(f("options", j(valuesJSON)));
      }

      filtersJSON.add(f(f.getName(), j(fields)));
    }

    return j(filtersJSON);
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

    return j(f("start", v(formatIsoDate(start))), f("end", v(formatIsoDate(end))));
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
