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

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.K;

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
import java.util.Optional;
import java.util.Set;

/**
 * This class provides functions to ease and secure the handling of collections by supporting a type safe -- at least to
 * the extent Java's type system allows -- immutable and more functional style.
 *
 * Note that all functions do <i>not</i> mutate input collections unless otherwise stated.
 *
 * @deprecated By now, all of this is natively available in Java.
 */
@Deprecated
public final class Collections {
  private Collections() {
  }

  /** Return the head of list <code>as</code> or <code>none</code>. */
  public static <A> Optional<A> head(List<A> as) {
    if (!as.isEmpty()) {
      return Optional.of(as.get(0));
    } else {
      return Optional.empty();
    }
  }

  /** Make a string from a collection separating each element by <code>sep</code>. */
  public static String mkString(Collection<?> as, String sep) {
    final StringBuilder b = new StringBuilder();
    for (Object a : as) {
      b.append(a).append(sep);
    }
    return b.substring(0, Math.max(b.length() - sep.length(), 0));
  }

  /** Concatenates two iterables into a new list. */
  public static <A, M extends Iterable<? extends A>> List<A> concat(M as, M bs) {
    List<A> x = new ArrayList<>();
    for (A a : as) {
      x.add(a);
    }
    for (A b : bs) {
      x.add(b);
    }
    return x;
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

  /** Create a set from a list. */
  public static <A> Set<A> toSet(List<A> as) {
    Set<A> r = new HashSet<>(as.size());
    for (A a : as) {
      r.add(a);
    }
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

  /** Concat (aka flatten) a collection of collections by concatenating them all. [[a]] -&gt; [a] */
  public static <A, M extends Collection<? extends Collection<A>>> List<A> concat(M as) {
    final List<A> target = new ArrayList<>(as.size());
    for (Collection<A> a : as) {
      target.addAll(a);
    }
    return target;
  }

}
