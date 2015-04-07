/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.index.service.util;

import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.util.SmartIterator;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONAware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ListProviderUtil {

  private ListProviderUtil() {
  }

  /**
   * Sorts the given map by its values, ascendantly or not
   *
   * @param map
   *          The map to sort
   * @param ascending
   *          If the sorting should be done ascendantly (true) or not
   * @return a sorted map
   */
  public static Map<String, Object> sortMapByValue(Map<String, Object> map, boolean asc) {
    TreeMap<String, Object> treeMap = new TreeMap<String, Object>(new ValueComparer(map, asc));
    treeMap.putAll(map);
    return treeMap;
  }

  /** inner class to sort the map **/
  private static class ValueComparer implements Comparator<String> {
    private Map<String, Object> baseMap = null;
    private boolean ascending = true;

    /**
     * Comparator for a map of strings key/value
     *
     * @param data
     *          The base map to sort
     * @param ascending
     *          If the sorting should be done ascendantly (true) or not
     */
    public ValueComparer(Map<String, Object> data, boolean ascending) {
      super();
      this.baseMap = data;
      this.ascending = ascending;
    }

    @Override
    public int compare(String o1, String o2) {
      String e1 = (String) baseMap.get(o1);
      String e2 = (String) baseMap.get(o2);

      if (ascending)
        return e1.compareTo(e2);
      else
        return 0 - e1.compareTo(e2);
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
  public static Map<String, Object> filterMap(Map<String, Object> map, ResourceListQuery query) {
    if (noActionRequired(query))
      return map;

    int limit = query.getLimit().getOrElse(0);
    int offset = query.getOffset().getOrElse(0);
    SmartIterator<Object> si = new SmartIterator<Object>(limit, offset);
    return si.applyLimitAndOffset(map);
  }

  private static boolean noActionRequired(ResourceListQuery query) {
    return query == null || (query.getFilters().isEmpty() && query.getOffset().isNone() && query.getLimit().isNone());
  }

  public static List<JSONAware> filterMap(List<JSONAware> unfilteredList, ResourceListQuery query) {
    if (noActionRequired(query)) {
      return unfilteredList;
    }
    int limit = query.getLimit().getOrElse(0);
    int offset = query.getOffset().getOrElse(0);
    SmartIterator<JSONAware> si = new SmartIterator<JSONAware>(limit, offset);
    return si.applyLimitAndOffset(unfilteredList);
  }

  /**
   * Go through the given list of string and split the element with comma
   * 
   * @param inputList
   *          the list with the element to split
   * @return the list with the split strings
   */
  public static List<String> splitStringList(List<String> inputList) {
    List<String> outputList = new ArrayList<>();
    for (String item : inputList) {
      if (StringUtils.isNotBlank(item))
        outputList.addAll(Arrays.asList(item.split(",")));
    }
    return outputList;
  }

}
