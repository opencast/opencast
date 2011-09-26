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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CollectionUtilTest {

  private List<String> newList() {
    List<String> l = new ArrayList<String>();
    l.add("one");
    l.add("two");
    return l;
  }

  @Test
  public void testMap() {
    List<String> list = newList();
    Collection<String> mapped = CollectionUtil.map(list, new Function<String, String>() {
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
    Collection<String> mapped = CollectionUtil.flatMap(list, new Function<String, Collection<String>>() {
      @Override
      public Collection<String> apply(String s) {
        return Arrays.asList(">", s);
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
    Collection<String> filtered = CollectionUtil.filter(list, new Predicate<String>() {
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
    assertTrue(CollectionUtil.head(list).fold(match));
    List<String> empty = Arrays.asList();
    assertFalse(CollectionUtil.head(empty).fold(match));
  }

  @Test
  public void testFoldl() {
    List<Integer> ints = Arrays.asList(1, 2, 3, 4);
    assertTrue(10 == CollectionUtil.foldl(ints, 0, new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a + b;
      }
    }));
    List<String> strings = Arrays.asList("vaughn", "bodé", "andré", "franquin");
    assertTrue(23 == CollectionUtil.foldl(strings, 0, new Function2<Integer, String, Integer>() {
      @Override
      public Integer apply(Integer a, String s) {
        return a + s.length();
      }
    }));
    assertTrue(5 == CollectionUtil.foldl(Collections.<Integer>emptyList(), 5, new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a + b;
      }
    }));
  }

  @Test
  public void mkString() {
    List<Integer> l = Arrays.asList(1, 2, 3);
    assertEquals("1-2-3", CollectionUtil.mkString(l, "-"));
  }
}
