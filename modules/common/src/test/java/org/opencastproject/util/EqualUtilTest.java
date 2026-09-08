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


package org.opencastproject.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.EqualsUtil.eqListUnsorted;
import static org.opencastproject.util.EqualsUtil.eqMap;
import static org.opencastproject.util.data.Arrays.array;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class EqualUtilTest {
  @Test
  public void testEqualMap() {
    assertTrue(eqMap(Map.of("a", "b"), Map.of("a", "b")));
    assertTrue(eqMap(Map.of("a", Map.of(1, "bla")), Map.of("a", Map.of(1, "bla"))));
    // this yields false since Java does not define equality on arrays.
    assertFalse(eqMap(Map.of(4, array(1, 2, 4)), Map.of(4, array(1, 2, 4))));
    assertFalse(eqMap(Map.of(1, new Object()), Map.of(1, new Object())));
    assertFalse(eqMap(Map.of("a", "b", "x", "y"), Map.of("a", "b", "x", "z")));
  }

  @Test
  public void testEqualListUnsorted() {
    // A List is equal to itself
    List<String> as = asList("a", "b");
    List<String> bs = asList("a", "b");
    assertTrue(eqListUnsorted(as, bs));
    // Permutations of unsorted Lists are equal
    List<String> permutedA = asList("a", "b");
    List<String> permutedB = asList("b", "a");
    assertTrue(eqListUnsorted(permutedA, permutedB));

    List<String> emptyA = asList();
    List<String> emptyB = asList();
    assertTrue(eqListUnsorted(emptyA, emptyB));

    List<String> nullA = null;
    List<String> nullB = null;
    assertTrue(eqListUnsorted(nullA, nullB));
    assertFalse(eqListUnsorted(nullA, emptyA));
    // Unsorted Lists are equal if their distinct entries correspond
    List<String> distinct = asList("a", "b");
    List<String> multiples = asList("a", "b", "a", "b", "a");
    assertTrue(eqListUnsorted(distinct, multiples));
  }
}
