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
import static org.opencastproject.util.data.Collections.append;
import static org.opencastproject.util.data.Collections.array;
import static org.opencastproject.util.data.Collections.concat;
import static org.opencastproject.util.data.Collections.cons;
import static org.opencastproject.util.data.Collections.iterator;
import static org.opencastproject.util.data.Collections.join;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.repeat;
import static org.opencastproject.util.data.Collections.toArray;

public class CollectionsTest {

  private List<String> newList() {
    List<String> l = new ArrayList<String>();
    l.add("one");
    l.add("two");
    return l;
  }

  @Test
  public void testMap() {
    List<String> list = newList();
    Collection<String> mapped = Collections.map(list, new Function<String, String>() {
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
    List<String> list = newList();
    Collection<String> mapped = Collections.flatMap(list, new Function<String, Collection<String>>() {
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
    List<String> list = newList();
    Collection<String> filtered = Collections.filter(list, new Predicate<String>() {
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
    List<String> list = newList();
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
    assertTrue(Collections.head(list).fold(match));
    List<String> empty = list();
    assertFalse(Collections.head(empty).fold(match));
  }

  @Test
  public void testFoldl() {
    List<Integer> ints = list(1, 2, 3, 4);
    assertTrue(10 == Collections.foldl(ints, 0, new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a + b;
      }
    }));
    List<String> strings = list("vaughn", "bodé", "andré", "franquin");
    assertTrue(23 == Collections.foldl(strings, 0, new Function2<Integer, String, Integer>() {
      @Override
      public Integer apply(Integer a, String s) {
        return a + s.length();
      }
    }));
    assertTrue(5 == Collections.foldl(java.util.Collections.<Integer>emptyList(), 5, new Function2<Integer, Integer, Integer>() {
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
  public void testList() {
    List<Integer> i1 = Collections.toList(java.util.Collections.<Integer>emptyList().iterator());
    assertTrue(i1.isEmpty());
    List<Integer> i2 = Collections.toList(list(1, 2, 3).iterator());
    assertEquals(3, i2.size());
  }
  
  @Test
  public void testRepeat() {
    assertTrue(Collections.toList(repeat(1, 0)).isEmpty());
    assertEquals(3, Collections.toList(repeat(1, 3)).size());
  }
  
  @Test
  public void testJoin() {
    assertArrayEquals(array(1, 2, 3, 4, 5, 6), Collections.toArray(Collections.toList(join(list(1, 2, 3).iterator(), list(4, 5, 6).iterator()))));
    assertArrayEquals(array(1, 2, 3), Collections.toArray(Collections.toList(join(list(1, 2, 3).iterator(), java.util.Collections.<Integer>emptyList().iterator()))));
    assertArrayEquals(array(1, 2, 3), Collections.toArray(Collections.toList(join(java.util.Collections.<Integer>emptyList().iterator(), list(1, 2, 3).iterator()))));
    assertEquals(0, Collections.toArray(Collections.toList(join(java.util.Collections.emptyList().iterator(), java.util.Collections.emptyList().iterator()))).length);
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
    assertArrayEquals(new Integer[]{0, 1, 2, 3}, cons(0, array(1, 2, 3)));
    String[] x = cons("0", array("1", "2", "3"));
    assertArrayEquals(new String[]{"0", "1", "2", "3"}, x);
    assertArrayEquals(new Object[]{"0", "1", "2", "3"}, x);
  }

  @Test
  public void testAppendArray() {
    Object[] as = append(new String[]{"a", "b"}, new Exception());
  }

  @Test
  public void testConcat() {
    final List<List<Integer>> l = list(list(1), list(2, 3), Collections.<Integer>nil(), list(9, 2, 1));
    final List<Integer> c = concat(l);
    assertArrayEquals(array(1, 2, 3, 9, 2, 1), toArray(c));
  }
}
