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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

public class SmartIteratorTest {

  private SmartIterator smartIterator;

  @Test
  public void testNoLimitNoOffset() {
    smartIterator = new SmartIterator(0, 0);
    Map<String, Object> unfilteredList = createMap(5);
    Map<String, Object> filteredList = smartIterator.applyLimitAndOffset(unfilteredList);
    assertEquals(5, filteredList.size());
  }

  @Test
  public void testLimit() {
    smartIterator = new SmartIterator(2, 0);
    Map<String, Object> unfilteredList = createMap(5);
    Map<String, Object> filteredList = smartIterator.applyLimitAndOffset(unfilteredList);
    assertMapContainsOnly(filteredList, 0, 1);
  }

  @Test
  public void testHigherLimitThanSources() {
    smartIterator = new SmartIterator(10, 0);
    Map<String, Object> unfilteredList = createMap(5);
    Map<String, Object> filteredList = smartIterator.applyLimitAndOffset(unfilteredList);
    assertMapContainsOnly(filteredList, 0, 1, 2, 3, 4);
  }

  @Test
  public void testOffset() {
    smartIterator = new SmartIterator(0, 2);
    Map<String, Object> unfilteredList = createMap(5);
    Map<String, Object> filteredList = smartIterator.applyLimitAndOffset(unfilteredList);
    assertMapContainsOnly(filteredList, 2, 3, 4);
  }

  @Test
  public void testOffsetAndLimit() {
    smartIterator = new SmartIterator(2, 2);
    Map<String, Object> unfilteredList = createMap(5);
    Map<String, Object> filteredList = smartIterator.applyLimitAndOffset(unfilteredList);
    assertMapContainsOnly(filteredList, 2, 3);
  }

  private void assertMapContainsOnly(Map<String, Object> map, int... allowedKeys) {
    assertEquals(allowedKeys.length, map.size());
    for (int allowedKey : allowedKeys) {
      assertTrue(map.containsKey(String.valueOf(allowedKey)));
    }
  }

  private Map<String, Object> createMap(int i) {
    int j = 0;
    Map<String, Object> result = new TreeMap<String, Object>();
    while (j < i) {
      result.put(String.valueOf(j++), new Object());
    }
    return result;
  }

}
