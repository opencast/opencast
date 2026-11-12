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
import static org.opencastproject.util.data.Arrays.append;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Arrays.cons;
import static org.opencastproject.util.data.Arrays.singleton;

import org.junit.Test;

public class ArraysTest {
  @Test
  public void testCons() {
    final Ax<String>[] as = cons(Ax.class, new Ax<String>(), Arrays.array(new Bx<String>()));
    assertEquals(2, as.length);
  }

  @Test
  public void testSingleton() {
    final String[] as = singleton(String.class, "1");
    assertEquals("1", as[0]);
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

  static class Ax<A> {
  }

  static class Bx<A> extends Ax<A> {
  }
}
