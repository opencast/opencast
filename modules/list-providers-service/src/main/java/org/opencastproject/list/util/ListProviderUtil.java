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

package org.opencastproject.list.util;

import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.util.SmartIterator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public final class ListProviderUtil {

  private ListProviderUtil() {
  }

  /**
   * Sorts the given map by its values, ascendantly or not
   *
   * @param map
   *          The map to sort
   * @param asc
   *          If the sorting should be done ascendantly (true) or not
   * @return a sorted map
   */
  public static Map<String, String> sortMapByValue(Map<String, String> map, boolean asc) {
    TreeMap<String, String> treeMap = new TreeMap<>(new ValueComparer(map, asc));
    treeMap.putAll(map);
    return treeMap;
  }

  /** inner class to sort the map **/
  private static class ValueComparer implements Comparator<String> {
    private Map<String, String> baseMap;
    private boolean ascending;

    /**
     * Comparator for a map of strings key/value
     *
     * @param data
     *          The base map to sort
     * @param ascending
     *          If the sorting should be done ascendantly (true) or not
     */
    ValueComparer(Map<String, String> data, boolean ascending) {
      super();
      this.baseMap = data;
      this.ascending = ascending;
    }

    @Override
    public int compare(String o1, String o2) {
      String e1 = baseMap.get(o1);
      String e2 = baseMap.get(o2);

      if (ascending) {
        return e1.compareTo(e2);
      } else {
        return -e1.compareTo(e2);
      }
    }
  }

  /**
   * Filtered a simple map with the given query using the limit and offset parameters
   *
   * @param map
   *          The map to filter
   * @param query
   *          The query
   * @return the filtered map
   */
  public static Map<String, String> filterMap(Map<String, String> map, ResourceListQuery query) {
    if (noActionRequired(query)) {
      return map;
    }

    int limit = query.getLimit().getOrElse(0);
    int offset = query.getOffset().getOrElse(0);
    SmartIterator<String> si = new SmartIterator<>(limit, offset);
    return si.applyLimitAndOffset(map);
  }

  private static boolean noActionRequired(ResourceListQuery query) {
    return query == null || (query.getFilters().isEmpty() && query.getOffset().isNone() && query.getLimit().isNone());
  }

  /**
   * Invert the key &amp; value in the given map
   *
   * @param map
   *          the map to invert
   * @return an inverted map
   */
  public static Map<String, String> invertMap(Map<String, String> map) {
    Map<String, String> inv = new HashMap<>();

    for (Entry<String, String> entry : map.entrySet()) {
      inv.put(entry.getValue(), entry.getKey());
    }

    return inv;
  }

}
