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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Arrays.append;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.appendTo;
import static org.opencastproject.util.data.Collections.concat;
import static org.opencastproject.util.data.Collections.filter;
import static org.opencastproject.util.data.Collections.flatMap;
import static org.opencastproject.util.data.Collections.foldl;
import static org.opencastproject.util.data.Collections.head;
import static org.opencastproject.util.data.Collections.iterator;
import static org.opencastproject.util.data.Collections.join;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.repeat;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Collections.toList;

public class CollectionsTest {

  private List<String> newTestList() {
    return list("one", "two");
  }

  @Test
  public void testMap() {
    List<String> list = newTestList();
    Collection<String> mapped = map(list, new Function<String, String>() {
      @Override
      public String apply(String s) {
        return s + s;
      }
    });
    assertSame(mapped.getClass(), list.getClass());
    assertTrue(list.contains("one"));
    assertTrue(list.contains("two"));
    assertTrue(mapped.contains("oneone"));
    assertTrue(mapped.contains("twotwo"));
    assertEquals(2, list.size());
    assertEquals(2, mapped.size());
  }

  @Test
  public void testFlatMap() {
    List<String> list = newTestList();
    Collection<String> mapped = flatMap(list, new Function<String, Collection<String>>() {
      @Override
      public Collection<String> apply(String s) {
        return list(">", s);
      }
    });
    assertSame(mapped.getClass(), list.getClass());
    assertEquals(2, list.size());
    assertEquals(4, mapped.size());
    assertTrue(mapped.contains(">"));
    assertTrue(mapped.contains("one"));
    assertTrue(mapped.contains("two"));
  }

  @Test
  public void testFilter() {
    List<String> list = newTestList();
    Collection<String> filtered = filter(list, new Predicate<String>() {
      @Override
      public Boolean apply(String s) {
        return "one".equals(s);
      }
    });
    assertSame(filtered.getClass(), list.getClass());
    assertEquals(2, list.size());
    assertEquals(1, filtered.size());
    assertTrue(filtered.contains("one"));
  }

  @Test
  public void testHead() {
    List<String> list = newTestList();
    Option.Match<String, Boolean> match = new Option.Match<String, Boolean>() {
      @Override
      public Boolean some(String s) {
        return true;
      }

      @Override
      public Boolean none() {
        return false;
      }
    };
    assertTrue(head(list).fold(match));
    List<String> empty = list();
    assertFalse(head(empty).fold(match));
  }

  @Test
  public void testFoldl() {
    List<Integer> ints = list(1, 2, 3, 4);
    assertTrue(10 == foldl(ints, 0, new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a + b;
      }
    }));
    List<String> strings = list("vaughn", "bodé", "andré", "franquin");
    assertTrue(23 == foldl(strings, 0, new Function2<Integer, String, Integer>() {
      @Override
      public Integer apply(Integer a, String s) {
        return a + s.length();
      }
    }));
    assertTrue(5 == foldl(java.util.Collections.<Integer>emptyList(), 5, new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a + b;
      }
    }));
  }

  @Test
  public void mkString() {
    List<Integer> l = list(1, 2, 3);
    assertEquals("1-2-3", Collections.mkString(l, "-"));
  }

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
    List<Object> x = list(1, new Object(), "hallo");
    // does not compile
    // List<String> y = list(1, new Object(), "hallo");
  }

  @Test
  public void testRepeat() {
    assertTrue(toList(repeat(1, 0)).isEmpty());
    assertEquals(3, toList(repeat(1, 3)).size());
  }

  @Test
  public void testJoin() {
    assertArrayEquals(array(1, 2, 3, 4, 5, 6), toArray(Integer.class, toList(join(list(1, 2, 3).iterator(), list(4, 5, 6).iterator()))));
    assertArrayEquals(array(1, 2, 3), toArray(Integer.class, toList(join(list(1, 2, 3).iterator(), java.util.Collections.<Integer>emptyList().iterator()))));
    assertArrayEquals(array(1, 2, 3), toArray(Integer.class, toList(join(java.util.Collections.<Integer>emptyList().iterator(), list(1, 2, 3).iterator()))));
    assertEquals(0, toArray(Object.class, toList(join(java.util.Collections.emptyList().iterator(), java.util.Collections.emptyList().iterator()))).length);
  }

  @Test(expected = NoSuchElementException.class)
  public void testIterator1() {
    Iterator<Integer> i = iterator(new Integer[0]);
    assertFalse(i.hasNext());
    i.next();
  }

  @Test(expected = NoSuchElementException.class)
  public void testIterator2() {
    Iterator<Integer> i = iterator(new Integer[] {1});
    assertTrue(i.hasNext());
    i.next();
    assertFalse(i.hasNext());
    i.next();
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
    Object[] as = append(Object.class, new String[]{"a", "b"}, new Exception());
  }

  @Test
  public void testConcat() {
    final List<List<Integer>> l = list(list(1), list(2, 3), Collections.<Integer>nil(), list(9, 2, 1));
    final List<Integer> c = concat(l);
    assertArrayEquals(array(1, 2, 3, 9, 2, 1), toArray(Integer.class, c));
  }

  @Test
  public void testAppendTo() {
    final List<Object> a = new ArrayList<Object>();
    // compile check
    final Collection<Object> b = appendTo(a, list(1));
    appendTo(a, list("a"));
  }

  @Test
  public void testCons() {
    // compile check
    List<Object> x = Collections.<Object>cons("1", list(1, 3));
  }

  @Test
  public void testToArray() {
    final String[] a = toArray(String.class, Collections.<String>list());
    assertEquals(0, a.length);
  }
}
