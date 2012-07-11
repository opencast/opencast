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

package org.opencastproject.util.data;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.data.Option.Match;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

public class OptionTest {

  @Test
  public void testOption() {
    Option<String> s = some("test");
    Option<String> n = none();
    assertTrue(s.isSome());
    assertTrue(n.isNone());
    assertEquals("a test", s.fold(new Match<String, String>() {
      public String some(String s) {
        return "a " + s;
      }

      public String none() {
        return "";
      }
    }));
    assertEquals("none", n.fold(new Match<String, String>() {
      public String some(String s) {
        return s;
      }

      public String none() {
        return "none";
      }
    }));
    for (String x : n) {
      fail("should not happen");
    }
    String r = null;
    for (String x : s) {
      r = x;
    }
    assertEquals("test", r);
    assertEquals("test", s.getOrElse(""));
    assertEquals("", n.getOrElse(""));

    Function<String, Integer> len = new Function<String, Integer>() {
      @Override
      public Integer apply(String s) {
        return s.length();
      }
    };
    assertTrue(s.map(len).getOrElse(-1) == 4);
    assertTrue(n.map(len).getOrElse(-1) == -1);
  }

  /**
   * Test the hash and equals methods.
   */
  @Test
  public void testHashEquals() {
    Option<String> a = some("a");
    Option<String> a1 = some("a");
    Option<String> b = some("b");
    Option<String> c = some("c");
    Option<String> n = none();
    assertTrue(a.equals(a1));
    assertFalse(a.equals(b));
    assertFalse(b.equals(c));
    assertFalse(c.equals(n));
    Set<Option<String>> set = new HashSet<Option<String>>();
    set.add(a);
    assertTrue(set.contains(a));
    assertEquals(1, set.size());
    set.add(b);
    assertTrue(set.contains(b));
    assertEquals(2, set.size());
    set.add(c);
    assertTrue(set.contains(c));
    assertEquals(3, set.size());
    set.add(n);
    assertTrue(set.contains(n));
    assertEquals(4, set.size());
    //
    set.remove(n);
    assertFalse(set.contains(n));
    assertEquals(3, set.size());
    set.remove(c);
    assertFalse(set.contains(c));
    assertEquals(2, set.size());
    set.remove(b);
    assertFalse(set.contains(b));
    assertEquals(1, set.size());
    set.remove(a);
    assertFalse(set.contains(a));
    assertEquals(0, set.size());
  }

  @Test
  public void testFlatten() {
    {
      final Option<Option<Integer>> o = some(some(1));
      assertEquals(some(1), o.flatten());
    }
    {
      final Option<Integer> o = some(1);
      assertEquals(some(1), o.flatten());
    }
    {
      final Option<Integer> o = none();
      assertEquals(none(), o.flatten());
    }
    {
      final Option<Option<Option<Integer>>> o = some(some(some(1)));
      assertEquals(some(some(1)), o.flatten());
      assertNotSame(some(some(2)), o.flatten());
    }
  }
}
