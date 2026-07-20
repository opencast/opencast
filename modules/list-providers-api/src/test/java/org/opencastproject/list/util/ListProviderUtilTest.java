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

package org.opencastproject.list.util;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.list.util.ListProviderUtil.filterMap;
import static org.opencastproject.list.util.ListProviderUtil.sortMapByValue;

import org.opencastproject.list.api.DefaultResourceListQuery;

import org.junit.Test;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

public class ListProviderUtilTest {

  private Map<String, String> getTestMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("21", "b: a test value");
    map.put("2", "c: second test value");
    map.put("34", "x: another test value");
    map.put("14", "o: one more");
    map.put("35", "z: again");
    map.put("26", "f: why not more");
    map.put("7", "a: the last one");

    return map;
  }

  @Test
  public void testSortingMap() {
    Map<String, String> testMap = getTestMap();

    // Test ascent sorting
    Map<String, String> sortedMap = sortMapByValue(testMap, true);

    assertEquals(testMap.size(), sortedMap.size());
    TreeSet<String> sortedValues = new TreeSet<>(Comparator.naturalOrder());
    sortedValues.addAll(testMap.values());

    Iterator<Entry<String, String>> iteratorSortedMap = sortedMap.entrySet().iterator();
    Iterator<String> iteratorSortedValues = sortedValues.iterator();

    while (iteratorSortedMap.hasNext()) {
      Entry<String, String> entry = iteratorSortedMap.next();
      String value = iteratorSortedValues.next();
      assertEquals(value, entry.getValue());
    }

    // Test descent sorting
    sortedMap = sortMapByValue(testMap, false);

    assertEquals(testMap.size(), sortedMap.size());
    sortedValues = new TreeSet<>(Comparator.reverseOrder());
    sortedValues.addAll(testMap.values());

    iteratorSortedMap = sortedMap.entrySet().iterator();
    iteratorSortedValues = sortedValues.iterator();

    while (iteratorSortedMap.hasNext()) {
      Entry<String, String> entry = iteratorSortedMap.next();
      String value = iteratorSortedValues.next();
      assertEquals(value, entry.getValue());
    }
  }

  @Test
  public void testFilterMap() {
    Map<String, String> testMap = getTestMap();
    DefaultResourceListQuery query = new DefaultResourceListQuery();
    query.setLimit(3);
    query.setOffset(0);

    assertEquals(3, filterMap(testMap, query).size());

    query.setLimit(0);
    query.setOffset(testMap.size());

    assertEquals(0, filterMap(testMap, query).size());

    query.setLimit(6);
    query.setOffset(testMap.size() - 3);

    assertEquals(3, filterMap(testMap, query).size());
  }

}
