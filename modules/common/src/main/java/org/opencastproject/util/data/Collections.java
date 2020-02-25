/**
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

import static org.opencastproject.util.data.Option.some;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This class provides functions to ease and secure the handling of collections by supporting a type safe -- at least to
 * the extent Java's type system allows -- immutable and more functional style.
 *
 * Note that all functions do <i>not</i> mutate input collections unless otherwise stated.
 */
public final class Collections {
  private Collections() {
  }

  // TODO check all clients of this method since it potentially breaks!
  @SuppressWarnings("unchecked")
  private static <A, B> Collection<A> buildFrom(Collection<B> as) {
    try {
      return as.getClass().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Type " + as.getClass() + " needs a parameterless constructor");
    }
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code> to produce a new collection
   * <code>bs</code>.
   *
   * An (empty) instance of the target collection has to be provided explicitly.
   *
   * @param as
   *          the source collection
   * @param bs
   *          the (empty) target collection
   * @param f
   *          the function to apply to each element of <code>as</code>
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A, B, M extends Collection<B>> M map(Collection<A> as, M bs, Function<A, B> f) {
    for (A x : as) {
      bs.add(f.apply(x));
    }
    return bs;
  }

  /**
   * Apply a binary function (operator) to a start value and all elements of the list in turn.
   *
   * Example: (+) 0 [1, 2, 3] -&gt; (((0 + 1) + 2) + 3)
   *
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A, B> B foldl(Collection<A> as, B start, Function2<B, A, B> f) {
    B fold = start;
    for (A a : as) {
      fold = f.apply(fold, a);
    }
    return fold;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code> to produce a new collection
   * <code>bs</code>.
   *
   * The type of collection <code>as</code> needs a parameterless constructor.
   *
   * Please note that since java does not support higher-order polymorphism -- which is needed to capture the type of
   * the collection -- some casting on the client side may still be necessary.
   *
   * @throws RuntimeException
   *           if the target collection cannot be created
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A, B> Collection<B> map(Collection<A> as, Function<A, B> f) {
    Collection<B> b = buildFrom(as);
    for (A x : as) {
      b.add(f.apply(x));
    }
    return b;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code> to produce a new collection
   * <code>bs</code> by concatenating the results.
   *
   * The type of collection <code>as</code> needs a parameterless constructor.
   *
   * Please note that since java does not support higher-order polymorphism -- which is needed to capture the type of
   * the collection -- some casting on the client side may still be necessary.
   *
   * @throws RuntimeException
   *           if the result collection cannot be created
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A, B> Collection<B> flatMap(Collection<A> as, Function<A, Collection<B>> f) {
    Collection<B> bs = buildFrom(as);
    for (A a : as) {
      bs.addAll(f.apply(a));
    }
    return bs;
  }

  /**
   * Exactly like {@link #flatMap(java.util.Collection, Function)} but you have to provide the target collection
   * yourself.
   *
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A, B, M extends Collection<B>> M flatMap(Collection<A> as, M bs, Function<A, Collection<B>> f) {
    for (A a : as) {
      bs.addAll(f.apply(a));
    }
    return bs;
  }

  /**
   * Returns the first element in <code>as</code> that satisfies a predicate <code>p</code>.
   *
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A> Option<A> find(Collection<A> as, Predicate<A> p) {
    for (A x : as) {
      if (p.apply(x))
        return some(x);
    }
    return Option.none();
  }

  /**
   * Tests if at least one element in <code>as</code> satisfies predicate <code>p</code>.
   *
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A> boolean exists(Collection<A> as, Predicate<A> p) {
    for (A a : as) {
      if (p.apply(a))
        return true;
    }
    return false;
  }

  /**
   * Return a new collection containing only the elements that satisfy predicate <code>p</code>.
   *
   * The type of collection <code>as</code> needs a parameterless constructor.
   *
   * @deprecated use {@link Monadics}
   */
  @Deprecated
  public static <A, M extends Collection<A>> M filter(M as, Predicate<A> p) {
    @SuppressWarnings("unchecked")
    final M filtered = (M) buildFrom(as);
    for (A a : as) {
      if (p.apply(a))
        filtered.add(a);
    }
    return filtered;
  }

  /** Return the head of list <code>as</code> or <code>none</code>. */
  public static <A> Option<A> head(List<A> as) {
    if (!as.isEmpty()) {
      return some(as.get(0));
    } else {
      return Option.none();
    }
  }

  /** Make a string from a collection separating each element by <code>sep</code>. */
  public static String mkString(Collection<?> as, String sep) {
    final StringBuilder b = new StringBuilder();
    for (Object a : as)
      b.append(a).append(sep);
    return b.substring(0, Math.max(b.length() - sep.length(), 0));
  }

  /** Append source collection <code>as</code> to <code>target</code>. */
  public static <A, T extends Collection<A>, S extends Iterable<? extends A>> T appendTo(T target, S as) {
    for (A a : as)
      target.add(a);
    return target;
  }

  /** Append source collections <code>as</code> to <code>target</code>. */
  @SafeVarargs
  public static <A, T extends Collection<A>, S extends Iterable<? extends A>> T appendToM(T target, S... as) {
    for (S s : as) {
      for (A a : s)
        target.add(a);
    }
    return target;
  }

  /** Append source collections <code>as</code> to <code>target</code>. */
  @SafeVarargs
  public static <A, T extends Collection<A>, X extends A> T appendToA(T target, X... as) {
    java.util.Collections.addAll(target, as);
    return target;
  }

  /** Concatenates two iterables into a new list. */
  public static <A, M extends Iterable<? extends A>> List<A> concat(M as, M bs) {
    List<A> x = new ArrayList<>();
    for (A a : as)
      x.add(a);
    for (A b : bs)
      x.add(b);
    return x;
  }

  /**
   * Merge two maps where <code>b</code> takes precedence.
   *
   * @return a new immutable map
   */
  public static <A, B> Map<A, B> merge(Map<? extends A, ? extends B> a, Map<? extends A, ? extends B> b) {
    final Map<A, B> x = new HashMap<>();
    x.putAll(a);
    x.putAll(b);
    return java.util.Collections.unmodifiableMap(x);
  }

  /** Drain all elements of <code>as</code> into a list. */
  public static <A> List<A> toList(Iterator<? extends A> as) {
    final List<A> t = new ArrayList<>();
    while (as.hasNext()) {
      t.add(as.next());
    }
    return t;
  }

  /** Drain all elements of <code>as</code> into a list. */
  public static <A> List<A> toList(Collection<A> as) {
    return new ArrayList<>(as);
  }

  /**
   * Return the list as is or nil, if <code>as</code> is null.
   * 
   * @deprecated use {@link #nullToNil(java.util.List)}
   */
  @Deprecated
  public static <A> List<A> mkList(List<A> as) {
    return as != null ? as : Collections.<A> nil();
  }

  /** Return the list as is or nil, if <code>as</code> is null. */
  public static <A> List<A> nullToNil(List<A> as) {
    return as != null ? as : Collections.<A> nil();
  }

  /** Create a list from an array. */
  @SafeVarargs
  public static <A> List<A> list(A... as) {
    final List<A> t = new ArrayList<>();
    java.util.Collections.addAll(t, as);
    return t;
  }

  /** Create a list from an array. */
  @SafeVarargs
  public static <A> List<A> nonNullList(A... as) {
    final List<A> t = new ArrayList<>();
    for (A a : as) {
      if (null != a) {
        t.add(a);
      }
    }
    return t;
  }

  /** The empty list. */
  @SuppressWarnings("unchecked")
  public static <A> List<A> nil() {
    return java.util.Collections.EMPTY_LIST;
  }

  /** The empty list. */
  @SuppressWarnings("unchecked")
  public static <A> List<A> nil(Class<A> type) {
    return java.util.Collections.EMPTY_LIST;
  }

  /** Construct a new list by prepending an element to a given list. */
  public static <A> List<A> cons(A a, List<? extends A> as) {
    final List<A> target = new ArrayList<>(as.size() + 1);
    target.add(a);
    target.addAll(as);
    return target;
  }

  /** Create a set from an array. */
  @SafeVarargs
  public static <A> Set<A> set(A... as) {
    final Set<A> t = new HashSet<>(as.length);
    java.util.Collections.addAll(t, as);
    return t;
  }

  /** Create a set from a list. */
  public static <A> Set<A> toSet(List<A> as) {
    Set<A> r = new HashSet<>(as.size());
    for (A a : as)
      r.add(a);
    return r;
  }

  /** Create a map from a list of tuples (K, V). */
  @SafeVarargs
  public static <K, V> Map<K, V> map(Tuple<? extends K, ? extends V>... ts) {
    final Map<K, V> map = new HashMap<>(ts.length);
    for (Tuple<? extends K, ? extends V> t : ts) {
      map.put(t.getA(), t.getB());
    }
    return map;
  }

  /** Create a dictionary from a list of tuples (K, V). */
  @SafeVarargs
  public static <K, V> Dictionary<K, V> dict(Tuple<? extends K, ? extends V>... ts) {
    final Dictionary<K, V> dict = new Hashtable<>(ts.length);
    for (Tuple<? extends K, ? extends V> t : ts) {
      dict.put(t.getA(), t.getB());
    }
    return dict;
  }

  /** Create an array from a collection. */
  @SuppressWarnings("unchecked")
  public static <A, B extends A> A[] toArray(Class<A> elemType, Collection<B> a) {
    return a.toArray((A[]) Array.newInstance(elemType, a.size()));
  }

  /** Convert a collection of {@link Double}s into an array of primitive type. */
  public static double[] toDoubleArray(Collection<Double> as) {
    final double[] target = new double[as.size()];
    int i = 0;
    for (Double a : as) {
      target[i] = a;
      i++;
    }
    return target;
  }

  /** Create an iterator form an array. */
  @SafeVarargs
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
  public static <A, X extends A> Iterator<A> repeat(final X a, final int times) {
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
   *
   * <pre>
   *   Iterator&lt;A&gt; as = ...
   *   for (A a : forc(as)) {
   *     ...
   *   }
   * </pre>
   */
  public static <A> Iterable<A> forc(final Iterator<A> as) {
    return new Iterable<A>() {
      @Override
      public Iterator<A> iterator() {
        return as;
      }
    };
  }

  /** Concat (aka flatten) a collection of collections by concatenating them all. [[a]] -&gt; [a] */
  public static <A, M extends Collection<? extends Collection<A>>> List<A> concat(M as) {
    final List<A> target = new ArrayList<>(as.size());
    for (Collection<A> a : as) {
      target.addAll(a);
    }
    return target;
  }

}
