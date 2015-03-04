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

import static org.opencastproject.index.service.util.ListProviderUtil.filterMap;
import static org.opencastproject.index.service.util.ListProviderUtil.sortMapByValue;
import static org.junit.Assert.assertEquals;

import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;

import org.junit.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

public class ListProviderUtilTest {

  private Map<String, Object> getTestMap() {
    Map<String, Object> map = new HashMap<String, Object>();
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
  public void testFilterMap() {
    Map<String, Object> testMap = getTestMap();
    ResourceListQueryImpl query = new ResourceListQueryImpl();
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

  @Test
  public void testSortingMap() {
    Map<String, Object> testMap = getTestMap();

    // Test ascent sorting
    Map<String, Object> sortedMap = sortMapByValue(testMap, true);

    assertEquals(testMap.size(), sortedMap.size());
    TreeSet<Object> sortedValues = new TreeSet<Object>(new Comparator<Object>() {
      public int compare(Object s1, Object s2) {
        if (s1 instanceof Comparable && s2 instanceof Comparable) {
          return ((Comparable) s1).compareTo((Comparable) s2);
        } else
          return -1;
      }
    });
    sortedValues.addAll(testMap.values());

    Iterator<Entry<String, Object>> iteratorSortedMap = sortedMap.entrySet().iterator();
    Iterator<Object> iteratorSortedValues = sortedValues.iterator();

    while (iteratorSortedMap.hasNext()) {
      Entry<String, Object> entry = iteratorSortedMap.next();
      Object value = iteratorSortedValues.next();
      assertEquals(value, entry.getValue());
    }

    // Test descent sorting
    sortedMap = sortMapByValue(testMap, false);

    assertEquals(testMap.size(), sortedMap.size());
    sortedValues = new TreeSet<Object>(new Comparator<Object>() {
      public int compare(Object s1, Object s2) {
        return 0 - ((Comparable) s1).compareTo((Comparable) s2);
      }
    });
    sortedValues.addAll(testMap.values());

    iteratorSortedMap = sortedMap.entrySet().iterator();
    iteratorSortedValues = sortedValues.iterator();

    while (iteratorSortedMap.hasNext()) {
      Entry<String, Object> entry = iteratorSortedMap.next();
      Object value = iteratorSortedValues.next();
      assertEquals(value, entry.getValue());
    }
  }
}
