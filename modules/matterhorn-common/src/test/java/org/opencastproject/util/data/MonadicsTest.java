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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.iterator;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.repeat;
import static org.opencastproject.util.data.Iterators.constant;
import static org.opencastproject.util.data.Iterators.intRangeE;
import static org.opencastproject.util.data.Monadics.IteratorMonadic;
import static org.opencastproject.util.data.Monadics.mlazy;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.util.data.functions.Booleans;
import org.opencastproject.util.data.functions.Functions;

import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MonadicsTest {

  @Test
  public void testMap() {
    List<Integer> list = asList(1, 2, 3);
    List<String> mapped = mlist(list).map(new Function<Integer, Integer>() {
      @Override
      public Integer apply(Integer a) {
        return a * a;
      }
    }).map(new Function<Integer, String>() {
      @Override
      public String apply(Integer a) {
        return a + " " + a;
      }
    }).value();
    assertEquals("1 1", mapped.get(0));
    assertEquals("4 4", mapped.get(1));
    assertEquals("9 9", mapped.get(2));
  }

  @Test
  public void testFlatMap() {
    List<Integer> mapped = mlist(new Integer[]{1, 2, 3}).flatMap(new Function<Integer, Collection<Integer>>() {
      @Override public Collection<Integer> apply(Integer a) {
        return asList(a, a);
      }
    }).value();
    assertEquals(6, mapped.size());
    assertEquals(new Integer(1), mapped.get(0));
    assertEquals(new Integer(2), mapped.get(2));
    assertEquals(new Integer(3), mapped.get(4));
  }

  @Test
  public void testFlatMap2() {
    final List<Object> l = list((Object) 1);
    final List<String> r1 = mlist(l).bind(new Function<Object, Option<String>>() {
      @Override public Option<String> apply(Object o) {
        return some("x");
      }
    }).value();
    assertEquals(1, r1.size());
    assertEquals("x", r1.get(0));
    final List<String> r2 = mlist(l).bind(new Function<Object, Option<String>>() {
      @Override public Option<String> apply(Object o) {
        return none();
      }
    }).value();
    assertEquals(0, r2.size());
  }

  @Test
  public void testFoldl() {
    String fold = mlist(new Integer[]{1, 2, 3}).foldl("", new Function2<String, Integer, String>() {
      @Override
      public String apply(String s, Integer a) {
        return s + a + a;
      }
    });
    assertEquals("112233", fold);
  }

  @Test
  public void testReducel() {
    String fold = mlist("a", "b", "c").reducel(new Function2<String, String, String>() {
      @Override
      public String apply(String a, String b) {
        return a + "," + b;
      }
    });
    assertEquals("a,b,c", fold);
  }

  @Test(expected = RuntimeException.class)
  public void testReducelError() {
    mlist(new String[]{}).reducel(new Function2<String, String, String>() {
      @Override
      public String apply(String a, String b) {
        return a + "," + b;
      }
    });
  }

  @Test
  public void testFlatten() {
    List<Integer> mapped = mlist(list(list(1, 2), list(3, 4))).flatMap(Functions.<List<Integer>>identity()).value();
    assertEquals(4, mapped.size());
    List<Integer> mapped2 = mlist(some(1), Option.<Integer>option(null), some(3), Option.<Integer>none()).flatMap(Functions.<Option<Integer>>identity()).value();
    assertEquals(2, mapped2.size());
  }

  @Test
  public void testTakeArray() {
    assertTrue(mlist(array(1, 2, 3, 4, 5)).take(0).value().isEmpty());
    assertEquals(3, mlist(array(1, 2, 3, 4, 5)).take(3).value().size());
    assertEquals(5, mlist(array(1, 2, 3, 4, 5)).take(5).value().size());
    assertEquals(5, mlist(array(1, 2, 3, 4, 5)).take(10).value().size());
  }

  @Test
  public void testTakeList() {
    assertTrue(mlist(asList(1, 2, 3, 4, 5)).take(0).value().isEmpty());
    assertEquals(3, mlist(asList(1, 2, 3, 4, 5)).take(3).value().size());
    assertEquals(5, mlist(asList(1, 2, 3, 4, 5)).take(5).value().size());
    assertEquals(5, mlist(asList(1, 2, 3, 4, 5)).take(10).value().size());
  }

  @Test
  public void testTakeIterator() {
    assertTrue(mlist(asList(1, 2, 3, 4, 5).iterator()).take(0).value().isEmpty());
    assertEquals(3, mlist(asList(1, 2, 3, 4, 5).iterator()).take(3).value().size());
    assertEquals(5, mlist(asList(1, 2, 3, 4, 5).iterator()).take(5).value().size());
    assertEquals(5, mlist(asList(1, 2, 3, 4, 5).iterator()).take(10).value().size());
  }

  @Test
  public void testDropArray() {
    assertTrue(mlist(array(1, 2, 3, 4, 5)).drop(10).value().isEmpty());
    assertEquals(3, mlist(array(1, 2, 3, 4, 5)).drop(2).value().size());
    assertEquals(1, mlist(array(1, 2, 3, 4, 5)).drop(4).value().size());
    assertEquals(5, mlist(array(1, 2, 3, 4, 5)).drop(0).value().size());
  }

  @Test
  public void testLazyMap() {
    final boolean[] applied = {false};
    IteratorMonadic<Integer> im = mlazy(asList(1, 2, 3, 4, 5))
            .map(new Function<Integer, Integer>() {
              @Override
              public Integer apply(Integer i) {
                applied[0] = true;
                return i * i;
              }
            })
            .map(new Function<Integer, Integer>() {
              @Override
              public Integer apply(Integer i) {
                applied[0] = true;
                return i + i;
              }
            });
    assertFalse(applied[0]);
    im.value();
    assertFalse(applied[0]);
    List<Integer> eval = im.eval();
    assertTrue(applied[0]);
    assertArrayEquals(new Integer[]{2, 8, 18, 32, 50}, eval.toArray(new Integer[]{}));
    // test empty input
    assertTrue(Collections.toList(
            mlazy(Collections.<Integer>nil()).map(Functions.<Integer>identity()).value()
    ).isEmpty());
  }

  @Test
  public void testLazyMapIndex() {
    List<Integer> eval = mlazy(asList(1, 2, 3, 4, 5)).mapIndex(new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer n, Integer i) {
        return n + i;
      }
    }).eval();
    assertArrayEquals(new Integer[]{1, 3, 5, 7, 9}, eval.toArray(new Integer[]{}));
  }

  @Test
  public void testLazyFlatMap() {
    List<Integer> eval = mlazy(asList(1, 2, 3))
            .flatMap(new Function<Integer, Iterator<Integer>>() {
              @Override
              public Iterator<Integer> apply(Integer integer) {
                if (integer >= 2) {
                  return asList(1, 2, 3).iterator();
                } else {
                  return java.util.Collections.<Integer>emptyList().iterator();
                }
              }
            })
            .flatMap(new Function<Integer, Iterator<Integer>>() {
              @Override
              public Iterator<Integer> apply(Integer integer) {
                return asList(integer * integer).iterator();
              }
            })
            .eval();
    assertArrayEquals(array(1, 4, 9, 1, 4, 9), Collections.toArray(Integer.class, eval));
  }

  @Test
  public void testLazyFlatMap2() {
    List<Integer> eval = mlazy(asList(1, 2, 3))
            .flatMap(new Function<Integer, Iterator<Integer>>() {
              @Override
              public Iterator<Integer> apply(Integer integer) {
                if (integer != 2) {
                  return asList(1, 2, 3).iterator();
                } else {
                  return java.util.Collections.<Integer>emptyList().iterator();
                }
              }
            })
            .flatMap(new Function<Integer, Iterator<Integer>>() {
              @Override
              public Iterator<Integer> apply(Integer integer) {
                return asList(integer * integer).iterator();
              }
            })
            .eval();
    assertArrayEquals(array(1, 4, 9, 1, 4, 9), Collections.toArray(Integer.class, eval));
  }

  @Test
  public void testLazyFlatMapDoubling() {
    List<Integer> eval = mlazy(asList(1, 2, 3, 4, 5)).flatMap(MonadicsTest.<Integer>twice()).eval();
    assertArrayEquals(new Integer[]{1, 1, 2, 2, 3, 3, 4, 4, 5, 5}, eval.toArray(new Integer[]{}));
  }

  @Test
  public void testLazyFlatMapKeepSize() {
    List<Integer> eval = mlazy(asList(1))
            .flatMap(new Function<Integer, Iterator<Integer>>() {
              @Override
              public Iterator<Integer> apply(Integer integer) {
                return Option.<Integer>some(2).iterator();
              }
            })
            .eval();
    assertArrayEquals(new Integer[]{2}, eval.toArray(new Integer[0]));
  }

  @Test
  public void testLazyFlatMapTimes() {
    List<Integer> eval = mlazy(asList(1, 2, 3, 4)).flatMap(times).eval();
    assertArrayEquals(new Integer[]{1, 2, 2, 3, 3, 3, 4, 4, 4, 4}, eval.toArray(new Integer[0]));
  }

  @Test
  public void testLazyFlatMapEmptyInput() {
    assertTrue(mlazy(java.util.Collections.<Integer>emptyList()).flatMap(times).eval().isEmpty());
  }

  @Test
  public void testLazyFlatMapEmptyOutput() {
    // test empty output
    List<Integer> eval = mlazy(asList(1, 2, 3))
            .flatMap(new Function<Integer, Iterator<Integer>>() {
              @Override
              public Iterator<Integer> apply(Integer integer) {
                return Option.<Integer>none().iterator();
              }
            })
            .eval();
    assertTrue(eval.isEmpty());
  }

  @Test
  public void testLazyFlatMapHasNext() {
    Iterator<Integer> ints = mlazy(asList(1, 2)).flatMap(MonadicsTest.<Integer>twice()).value();
    // test correctness of hasNext()
    assertTrue(ints.hasNext());
    assertTrue(ints.hasNext());
    assertTrue(ints.hasNext());
    assertTrue(ints.hasNext());
    assertTrue(ints.hasNext());
  }

  @Test
  public void testLazyFlatMapMultiple() {
    List<Integer> eval = mlazy(asList(1, 2))
            .flatMap(MonadicsTest.<Integer>twice())
            .flatMap(MonadicsTest.<Integer>twice())
            .eval();
    assertArrayEquals(new Integer[]{1, 1, 1, 1, 2, 2, 2, 2}, eval.toArray(new Integer[]{}));
  }

  @Test
  public void testLazyEachEmpty() {
    final boolean[] run = {false};
    mlazy(java.util.Collections.emptyList())
            .each(new Effect<Object>() {
              @Override
              public void run(Object o) {
                run[0] = true;
              }
            })
            .eval();
    assertFalse(run[0]);
  }

  @Test
  public void testLazyEach() {
    final int[] sum = {0};
    mlazy(asList(1, 2, 3, 4, 5))
            .each(new Effect<Integer>() {
              @Override
              public void run(Integer o) {
                sum[0] += o;
              }
            })
            .eval();
    assertEquals(15, sum[0]);
  }

  @Test
  public void testLazyEachIndexEmpty() {
    final boolean[] run = {false};
    mlazy(java.util.Collections.emptyList())
            .eachIndex(new Effect2<Object, Integer>() {
              @Override
              public void run(Object o, Integer i) {
                run[0] = true;
              }
            })
            .eval();
    assertFalse(run[0]);
  }

  @Test
  public void testLazyEachIndex() {
    final int[] sum = {0};
    mlazy(asList(1, 2, 3, 4, 5))
            .eachIndex(new Effect2<Integer, Integer>() {
              @Override
              public void run(Integer o, Integer i) {
                sum[0] += (o * i);
              }
            })
            .eval();
    assertEquals(40, sum[0]);
  }

  @Test
  public void testLazyTake() {
    assertTrue(mlazy(asList(1, 2, 3, 4, 5)).take(0).eval().isEmpty());
    assertEquals(3, mlazy(asList(1, 2, 3, 4, 5)).take(3).eval().size());
    assertEquals(5, mlazy(asList(1, 2, 3, 4, 5)).take(5).eval().size());
    assertEquals(5, mlazy(asList(1, 2, 3, 4, 5)).take(10).eval().size());
    assertEquals(100, mlazy(constant(10)).take(100).eval().size());
  }

  @Test
  public void testLazyFilter() {
    assertEquals(5, mlazy(intRangeE(0, 10)).filter(Booleans.lt(5)).eval().size());
    assertEquals(10, mlazy(intRangeE(0, 10)).filter(Booleans.lt(100)).eval().size());
    assertEquals(0, mlazy(intRangeE(0, 10)).filter(Booleans.lt(0)).eval().size());
  }

  @Test
  public void testLazyExists() {
    assertTrue(mlazy(intRangeE(0, 10)).exists(Booleans.gt(5)));
    assertFalse(mlazy(intRangeE(0, 10)).exists(Booleans.gt(9)));
  }

  @Test
  public void testZip() {
    {
      List<Tuple<String, Integer>> r = mlist(new String[]{"a", "b", "c"}).zip(list(1, 2, 3, 4, 5)).value();
      assertEquals(3, r.size());
      assertEquals(tuple("b", 2), r.get(1));
    }
    {
      List<Tuple<String, Integer>> r = mlist(new String[]{"a", "b", "c"}).zip(Collections.<Integer>nil()).value();
      assertEquals(0, r.size());
    }
    {
      List<Tuple<String, Integer>> r = mlist(list("a", "b", "c")).zip(list(1, 2)).value();
      assertEquals(2, r.size());
      assertEquals(tuple("b", 2), r.get(1));
    }
    {
      List<Tuple<String, Integer>> r = mlist(list("a", "b", "c").iterator()).zip(list(1, 2)).value();
      assertEquals(2, r.size());
      assertEquals(tuple("b", 2), r.get(1));
    }
    {
      List<Tuple<String, Integer>> r = mlist(Collections.<String>nil().iterator()).zip(list(1, 2)).value();
      assertEquals(0, r.size());
    }
  }

  @Test
  public void testConcat() {
    // does not compile
    // final List<Integer> a = mlist(1, 2, 3).concat(list(4, "5")).value();
    final List<Integer> a = mlist(1, 2, 3).concat(list(4, 5)).value();
    assertEquals(5, a.size());
    assertEquals(4, (Object) a.get(3)); // Object cast because of overloading ambiguity
    final List<Object> b = mlist((Object) 1).concat(Collections.<Object>list("x")).value();
    assertEquals(2, b.size());
    assertEquals(1, b.get(0));
    assertEquals("x", b.get(1));
  }

  private static <A> Function<A, Iterator<A>> twice() {
    return new Function<A, Iterator<A>>() {
      @Override
      public Iterator<A> apply(A a) {
        return iterator(a, a);
      }
    };
  }

  private static Function<Integer, Iterator<Integer>> times = new Function<Integer, Iterator<Integer>>() {
    @Override
    public Iterator<Integer> apply(Integer a) {
      return repeat(a, a);
    }
  };
}
