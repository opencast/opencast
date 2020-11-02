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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.exception.JsonCreationException;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.list.query.StringListFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class EndpointUtil {

  private EndpointUtil() {
  }

  /**
   * Returns a generated JSON object with key-value from given list.
   *
   * Note that JSONObject (and JSON in general) does not preserve key ordering,
   * so while the Map passed to this function may have ordered keys, the resulting
   * JSONObject is not ordered.
   *
   * @param list
   *          The source list for the JSON object
   * @return a JSON object containing the all the key-value as parameter
   * @throws JsonCreationException
   */
  public static <T> JSONObject generateJSONObject(Map<String, T> list) throws JsonCreationException {

    if (list == null) {
      throw new JsonCreationException("List is null");
    }

    JSONObject jsonList = new JSONObject();

    for (Entry<String, T> entry : list.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String) {
        jsonList.put(entry.getKey(), value);
      } else if (value instanceof JSONObject) {
        jsonList.put(entry.getKey(), value);
      } else if (value instanceof List) {
        Collection collection = (Collection) value;
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(collection);
        jsonList.put(entry.getKey(), jsonArray);
      } else {
        throw new JsonCreationException("Could not deal with " + value);
      }
    }

    return jsonList;
  }

  /**
   * Add the string based filters to the given list query.
   *
   * @param filterString
   *          The string based filters
   * @param query
   *          The query to update with the filters
   */
  public static void addRequestFiltersToQuery(final String filterString, ResourceListQueryImpl query) {
    if (filterString != null) {
      String[] filters = filterString.split(",");
      for (String filter : filters) {
        String[] splitFilter = filter.split(":", 2);
        if (splitFilter != null && splitFilter.length == 2) {
          String key = splitFilter[0].trim();
          String value = splitFilter[1].trim();
          query.addFilter(new StringListFilter(key, value));
        }
      }
    }
  }
}
