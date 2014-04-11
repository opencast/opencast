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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.EqualsUtil.eqMap;
import static org.opencastproject.util.EqualsUtil.hash;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;

import org.junit.Test;

public class EqualUtilTest {
  @Test
  public void testHash() {
    assertEquals(hash("a", "b", "c"), 41 * (41 * (41 + "a".hashCode()) + "b".hashCode()) + "c".hashCode());
    assertEquals(hash("a", null, "c"), 41 * (41 * (41 + "a".hashCode()) + 0L) + "c".hashCode());
    assertEquals(0L, hash());
    assertEquals(0L, hash(null));
  }

  @Test
  public void testEqualMap() {
    assertTrue(eqMap(map(tuple("a", "b")), map(tuple("a", "b"))));
    assertTrue(eqMap(map(tuple("a", map(tuple(1, "bla")))), map(tuple("a", map(tuple(1, "bla"))))));
    // this yields false since Java does not define equality on arrays.
    assertFalse(eqMap(map(tuple(4, array(1, 2, 4))), map(tuple(4, array(1, 2, 4)))));
    assertFalse(eqMap(map(tuple(1, new Object())), map(tuple(1, new Object()))));
    assertFalse(eqMap(map(tuple("a", "b"), tuple("x", "y")), map(tuple("a", "b"), tuple("x", "z"))));
  }
}
