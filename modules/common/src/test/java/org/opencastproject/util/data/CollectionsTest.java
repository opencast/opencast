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


package org.opencastproject.util.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Arrays.append;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.concat;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.toList;

import org.junit.Test;

import java.util.List;

public class CollectionsTest {

  @Test
  public void testToList() {
    List<Integer> i1 = toList(java.util.Collections.<Integer>emptyList().iterator());
    assertTrue(i1.isEmpty());
    List<Integer> i2 = toList(list(1, 2, 3).iterator());
    assertEquals(3, i2.size());
    List<Object> i3 = toList(list("1", 2, new Object()).iterator());
    assertEquals(3, i3.size());
  }

  @Test
  public void testList() {
    // compile test
    list(1, new Object(), "hallo");
    // does not compile
    // List<String> y = list(1, new Object(), "hallo");
  }

  @Test
  public void testArray() {
    assertArrayEquals(new Integer[]{1, 2, 3}, array(1, 2, 3));
    assertArrayEquals(new String[]{"1", "2", "3"}, array("1", "2", "3"));
  }

  @Test
  public void testConsArray() {
    assertArrayEquals(new Integer[]{0, 1, 2, 3}, Arrays.cons(Integer.class, 0, array(1, 2, 3)));
    String[] x = Arrays.cons(String.class, "0", array("1", "2", "3"));
    assertArrayEquals(new String[]{"0", "1", "2", "3"}, x);
    assertArrayEquals(new Object[]{"0", "1", "2", "3"}, x);
  }

  @Test
  public void testAppendArray() {
    append(Object.class, new String[]{"a", "b"}, new Exception());
  }

  @Test
  public void testConcat() {
    final List<List<Integer>> l = list(list(1), list(2, 3), Collections.<Integer>nil(), list(9, 2, 1));
    final List<Integer> c = concat(l);
    assertArrayEquals(array(1, 2, 3, 9, 2, 1), c.toArray(Integer[]::new));
  }
}
