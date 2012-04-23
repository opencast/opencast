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

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import static org.opencastproject.util.data.Option.some;

/**
 * This class provides functions to ease and secure the handling of collections by supporting a type safe
 * -- at least to the extent Java's type system allows -- immutable and more functional style. You'll
 * find some of the usual suspects from FP here like <code>map</code>, <code>flatMap</code> and <code>filter</code>
 * but also simple helpers like <code>set</code>.
 */
public final class Collections {

  private Collections() {
  }

  private static <A, B> Collection<A> buildFrom(Collection<B> as) {
    try {
      return as.getClass().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Type " + as.getClass() + " needs a parameterless constructor");
    }
  }

  /**
   * Get a value from a map, creating and adding a new one, if the value is missing, i.e. it is null.
   *
   * @param c
   *         creates the missing value
   */
  public static <K, V> V getOrCreate(Map<K, V> map, K key, Creator<V> c) {
    V v = map.get(key);
    if (v == null) {
      v = c.create();
      map.put(key, v);
    }
    return v;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code>
   * to produce a new collection <code>bs</code>.
   * <p/>
   * An (empty) instance of the target collection has to be provided explicitly.
   *
   * @param as
   *         the source collection
   * @param bs
   *         the (empty) target collection
   * @param f
   *         the function to apply to each element of <code>as</code>
   */
  public static <A, B, M extends Collection<B>> M map(Collection<A> as, M bs, Function<A, B> f) {
    for (A x : as) {
      bs.add(f.apply(x));
    }
    return bs;
  }

  /**
   * Apply a binary function (operator) to a start value and all elements of the list in turn.
   * <p/>
   * Example: (+) 0 [1, 2, 3] -> (((0 + 1) + 2) + 3)
   */
  public static <A, B> B foldl(Collection<A> as, B start, Function2<B, A, B> f) {
    B fold = start;
    for (A a : as) {
      fold = f.apply(fold, a);
    }
    return fold;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code>
   * to produce a new collection <code>bs</code>.
   * <p/>
   * The type of collection <code>as</code> needs a parameterless constructor.
   * <p/>
   * Please note that since java does not support higher-order polymorphism -- which is needed
   * to capture the type of the collection -- some casting on the client side may still be
   * necessary.
   *
   * @throws RuntimeException
   *         if the target collection cannot be created
   */
  public static <A, B> Collection<B> map(Collection<A> as, Function<A, B> f) {
    Collection<B> b = buildFrom(as);
    for (A x : as) {
      b.add(f.apply(x));
    }
    return b;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code>
   * to produce a new collection <code>bs</code> by concatenating the results.
   * <p/>
   * The type of collection <code>as</code> needs a parameterless constructor.
   * <p/>
   * Please note that since java does not support higher-order polymorphism -- which is needed
   * to capture the type of the collection -- some casting on the client side may still be
   * necessary.
   *
   * @throws RuntimeException
   *         if the result collection cannot be created
   */
  public static <A, B> Collection<B> flatMap(Collection<A> as, Function<A, Collection<B>> f) {
    Collection<B> bs = buildFrom(as);
    for (A a : as) {
      bs.addAll(f.apply(a));
    }
    return bs;
  }

  /**
   * Exactly like {@link #flatMap(java.util.Collection, Function)} but you have to provide
   * the target collection yourself.
   */
  public static <A, B, M extends Collection<B>> M flatMap(Collection<A> as, M bs, Function<A, Collection<B>> f) {
    for (A a : as) {
      bs.addAll(f.apply(a));
    }
    return bs;
  }

  /** Returns the first element in <code>as</code> that satisfies a predicate <code>p</code>. */
  public static <A> Option<A> find(Collection<A> as, Predicate<A> p) {
    for (A x : as) {
      if (p.apply(x)) return some(x);
    }
    return Option.none();
  }

  /** Tests if at least one element in <code>as</code> satisfies predicate <code>p</code>. */
  public static <A> boolean exists(Collection<A> as, Predicate<A> p) {
    for (A a : as) {
      if (p.apply(a)) return true;
    }
    return false;
  }

  /** Return the head of list <code>as</code> or <code>none</code>. */
  public static <A> Option<A> head(List<A> as) {
    if (!as.isEmpty()) {
      return some(as.get(0));
    } else {
      return Option.none();
    }
  }

  /** Return the head of array <code>as</code> or <code>none</code>. */
  public static <A> Option<A> head(A[] as) {
    if (as.length > 0) {
      return some(as[0]);
    } else {
      return Option.none();
    }
  }

  /**
   * Sort array <code>as</code> according to the natural ordering. Note that <code>as</code> gets
   * mutated!
   *
   * @return <code>as</code>
   * @see Arrays#sort(Object[])
   */
  public static <A> A[] sortArray(A[] as) {
    Arrays.sort(as);
    return as;
  }

  /**
   * Return a new collection containing only the elements that satisfy predicate <code>p</code>.
   * <p/>
   * The type of collection <code>as</code> needs a parameterless constructor.
   */
  public static <A, M extends Collection<A>> M filter(M as, Predicate<A> p) {
    M filtered = (M) buildFrom(as);
    for (A a : as) {
      if (p.apply(a))
        filtered.add(a);
    }
    return filtered;
  }

  /** Make a string from a collection separating each element by <code>sep</code>. */
  public static String mkString(Collection<?> as, String sep) {
    StringBuffer b = new StringBuffer();
    for (Object a : as) {
      b.append(a).append(sep);
    }
    return b.substring(0, Math.max(b.length() - sep.length(), 0));
  }

  /** Merge collections <code>a</code> and <code>b</code> into <code>target</code>. */
  public static <A, M extends Collection<A>> M concat(M target, Collection<? extends A> a, Collection<? extends A> b) {
    target.addAll(a);
    target.addAll(b);
    return target;
  }

  /** Concatenates two lists. */
  public static <A> List<A> concat(List<? extends A> a, List<? extends A> b) {
    List<A> x = new ArrayList<A>();
    x.addAll(a);
    x.addAll(b);
    return x;
  }

  /** Create a new array by prepending <code>a</code> to <code>as</code>: <code>[a, as0, as1, .. asn]</code> */
  public static <A> A[] cons(A a, A[] as) {
    A[] x = (A[]) Array.newInstance(a.getClass(), as.length + 1);
    x[0] = a;
    System.arraycopy(as, 0, x, 1, as.length);
    return x;
  }

  /** Create a new array by appending <code>a</code> to <code>as</code>: <code>[as0, as1, .. asn, a]</code>. */
  public static <A> A[] append(A[] as, A a) {
    List<A> xs = new ArrayList<A>(as.length + 1);
    xs.add(a);
    for (A y : as) xs.add(y);
    return (A[]) xs.toArray(new Object[xs.size()]);
  }

  /** Drain all elements of <code>as</code> into a list. */
  public static <A> List<A> toList(Iterator<A> as) {
    List<A> ax = new ArrayList<A>();
    while (as.hasNext()) {
      ax.add(as.next());
    }
    return ax;
  }

  /** Drain all elements of <code>as</code> into a list. */
  public static <A> List<A> toList(Collection<A> as) {
    List<A> ax = new ArrayList<A>();
    for (A a : as) {
      ax.add(a);
    }
    return ax;
  }

  /** Create a list from an array. */
  public static <A> List<A> list(A... as) {
    return Arrays.asList(as);
  }

  /** The empty list. */
  public static <A> List<A> nil() {
    return java.util.Collections.EMPTY_LIST;
  }

  /** Create a set from an array. */
  public static <A> Set<A> set(A... as) {
    Set<A> r = new HashSet<A>(as.length);
    for (A a : as) r.add(a);
    return r;
  }

  /** Create a set from a list. */
  public static <A> Set<A> toSet(List<A> as) {
    Set<A> r = new HashSet<A>(as.size());
    for (A a : as) r.add(a);
    return r;
  }

  /** Create a map from a list of tuples (K, V). */
  public static <K, V> Map<K, V> map(Tuple<K, V>... ts) {
    Map<K, V> map = new HashMap<K, V>(ts.length);
    for (Tuple<K, V> t : ts) {
      map.put(t.getA(), t.getB());
    }
    return map;
  }

  /** Create a dictionary from a list of tuples (K, V). */
  public static <K, V> Dictionary<K, V> dict(Tuple<K, V>... ts) {
    Dictionary<K, V> dict = new Hashtable<K, V>(ts.length);
    for (Tuple<K, V> t : ts) {
      dict.put(t.getA(), t.getB());
    }
    return dict;
  }

  /** Create properties from a list of tuples (K, V). */
  public static Properties properties(Tuple<String, String>... ts) {
    Properties a = new Properties();
    for (Tuple<String, String> t : ts) {
      a.setProperty(t.getA(), t.getB());
    }
    return a;
  }

  /** Create an array from the vararg parameter list. */
  public static <A> A[] array(A... as) {
    return as;
  }

  /** Create an array from a list. */
  public static <A> A[] toArray(List<A> a) {
    return (A[]) a.toArray(new Object[a.size()]);
  }

  /** Create an iterator form an array. */
  public static <A> Iterator<A> iterator(final A... as) {
    return new Iterator<A>() {
      private int i = 0;

      @Override
      public boolean hasNext() {
        return as.length > i;
      }

      @Override
      public A next() {
        if (i < as.length) {
          return as[i++];
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** Create an iterator that repeats <code>a</code> for the said times. */
  public static <A> Iterator<A> repeat(final A a, final int times) {
    return new Iterator<A>() {
      private int count = times;

      @Override
      public boolean hasNext() {
        return count > 0;
      }

      @Override
      public A next() {
        count--;
        return a;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** Join two iterators. */
  public static <A> Iterator<A> join(final Iterator<A> a, final Iterator<A> b) {
    return new Iterator<A>() {
      @Override
      public boolean hasNext() {
        return a.hasNext() || b.hasNext();
      }

      @Override
      public A next() {
        return a.hasNext() ? a.next() : b.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Make an Iterator usable in a for comprehension like this:
   * <pre>
   *   Iterator&lt;A&gt; as = ...
   *   for (A a : forc(as)) {
   *     ...
   *   }
   * </pre>
   */
  public static <A> Collection<A> forc(final Iterator<A> as) {
    return new AbstractCollection<A>() {
      @Override
      public Iterator<A> iterator() {
        return as;
      }

      @Override
      public int size() {
        return -1;
      }
    };
  }

  public static <A> Function<Option<A>, List<A>> optionToList() {
    return new Function<Option<A>, List<A>>() {
      @Override
      public List<A> apply(Option<A> as) {
        return as.list();
      }
    };
  }

  public static <A> Function<A[], List<A>> arrayToList() {
    return new Function<A[], List<A>>() {
      @Override
      public List<A> apply(A[] as) {
        if (as != null) {
          return Collections.list(as);
        } else {
          return nil();
        }
      }
    };
  }

  public static <A, B> Function<A[], List<B>> flatMapArrayToList(final Function<A, List<B>> f) {
    return new Function<A[], List<B>>() {
      @Override
      public List<B> apply(A[] as) {
        return Monadics.mlist(as).bind(f).value();
      }
    };
  }

  /** Turn a value into a single element array. */
  public static <A> Function<A, A[]> singletonArray() {
    return new Function<A, A[]>() {
      @Override
      public A[] apply(A a) {
        return (A[]) new Object[]{a};
      }
    };
  }

  /** Turn an option into an iterator. */
  public static <A> Function<Option<A>, Iterator<A>> optionToIterator() {
    return new Function<Option<A>, Iterator<A>>() {
      @Override
      public Iterator<A> apply(Option<A> as) {
        return as.iterator();
      }
    };
  }

  /** Functional version of {@link org.opencastproject.util.data.Collections#head(Object[])}. */
  public static <A> Function<A[], Option<A>> headArray() {
    return new Function<A[], Option<A>>() {
      @Override
      public Option<A> apply(A[] as) {
        return Collections.head(as);
      }
    };
  }

  /** Functional version of {@link org.opencastproject.util.data.Collections#head(Object[])}. */
  public static <A> Function<List<A>, Option<A>> head() {
    return new Function<List<A>, Option<A>>() {
      @Override
      public Option<A> apply(List<A> as) {
        return Collections.head(as);
      }
    };
  }

  /** Functional version of {@link org.opencastproject.util.data.Collections#sortArray}. */
  public static <A> Function<A[], A[]> sortArray() {
    return new Function<A[], A[]>() {
      @Override
      public A[] apply(A[] as) {
        return Collections.sortArray(as);
      }
    };
  }

  /** Sort a list. */
  public static <A extends Comparable> Function<List<A>, List<A>> sort() {
    return new Function<List<A>, List<A>>() {
      @Override
      public List<A> apply(List<A> as) {
        List<A> asCopy = new ArrayList<A>(as);
        java.util.Collections.sort(asCopy);
        return asCopy;
      }
    };
  }

  /** Create a function that checks if its argument is contained in <code>as</code>. */
  public static <A> Function<A, Boolean> containedIn(final List<A> as) {
    return new Function<A, Boolean>() {
      @Override public Boolean apply(A a) {
        return as.contains(a);
      }
    };
  }

  /** Curried version of {@link List#contains(Object)}. */
  public static <A> Function<List<A>, Function<A, Boolean>> containedIn() {
    return new Function<List<A>, Function<A, Boolean>>() {
      @Override public Function<A, Boolean> apply(final List<A> as) {
        return containedIn(as);
      }
    };
  }

  public static <A> Function<Option<A>, A> getOrElse(final A a) {
    return new Function<Option<A>, A>() {
      @Override public A apply(Option<A> ao) {
        return ao.getOrElse(a);
      }
    };
  }

  /** Sequence a collection of collections by concatenating them all. */
  public static <A, M1 extends Collection<A>, M2 extends Collection<M1>> List<A> sequence(M2 as) {
    final List<A> target = new ArrayList<A>(as.size());
    for (M1 a : as) {
      target.addAll(a);
    }
    return target;
  }
}
