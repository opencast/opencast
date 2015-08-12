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

package org.opencastproject.fun.juc;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** Constructor functions for immutable data types. */
public final class Immutables {
  private Immutables() {
  }

  /** Create an immutable map from <code>a</code>. */
  public static <A, B> Map<A, B> mk(Map<A, B> a) {
    return Collections.unmodifiableMap(new HashMap<A, B>(a));
  }

  /** Create an immutable sorted map from <code>a</code>. */
  public static <A, B> SortedMap<A, B> mk(SortedMap<A, B> a) {
    return Collections.unmodifiableSortedMap(new TreeMap<A, B>(a));
  }

  /** Create an immutable list from <code>a</code>. */
  public static <A> List<A> mk(List<? extends A> a) {
    return Collections.unmodifiableList(new ArrayList<A>(a));
  }

  /** Create an immutable set from <code>a</code>. */
  public static <A> Set<A> mk(Set<? extends A> a) {
    return Collections.unmodifiableSet(new HashSet<A>(a));
  }

  /** Return the empty list. */
  public static <A> List<A> nil() {
    return Collections.emptyList();
  }

  /** Return the empty list. */
  public static <A> List<A> emptyList() {
    return Collections.emptyList();
  }

  /** Create an immutable list from some elements. */
  public static <A> List<A> list(A a, A... as) {
    final List<A> xs = new ArrayList<A>(as.length);
    xs.add(a);
    Collections.addAll(xs, as);
    return Collections.unmodifiableList(xs);
  }

  /** Create an immutable list from an array. */
  public static <A> List<A> list(A[] as) {
    final List<A> xs = new ArrayList<A>(as.length);
    Collections.addAll(xs, as);
    return Collections.unmodifiableList(xs);
  }

  public static <A> Function<A[], List<A>> listFromArrayFn() {
    return new Function<A[], List<A>>() {
      @Override
      public List<A> apply(A[] as) {
        return list(as);
      }
    };
  }

  /** Create an immutable list from a list and an array. */
  public static <A> List<A> list(List<? extends A> l, A... as) {
    final List<A> a = new ArrayList<A>(l.size() + as.length);
    a.addAll(l);
    Collections.addAll(a, as);
    return Collections.unmodifiableList(a);
  }

  /** Create an immutable list by concatenating two lists. */
  public static <A> List<A> list(List<? extends A> l, List<? extends A> m) {
    final List<A> a = new ArrayList<A>(l.size() + m.size());
    a.addAll(l);
    a.addAll(m);
    return Collections.unmodifiableList(a);
  }

  /** Return the empty set. */
  public static <A> Set<A> emptySet() {
    return Collections.emptySet();
  }

  /** Create an immutable set from some elements. */
  public static <A> Set<A> set(A a, A... as) {
    final HashSet<A> xs = new HashSet<A>();
    xs.add(a);
    Collections.addAll(xs, as);
    return Collections.unmodifiableSet(xs);
  }

  /** Create an immutable set from an array. */
  public static <A> Set<A> set(A[] as) {
    final HashSet<A> xs = new HashSet<A>();
    Collections.addAll(xs, as);
    return Collections.unmodifiableSet(xs);
  }

  /** Return the empty map. */
  public static <A, B> Map<A, B> emtpyMap() {
    return Collections.emptyMap();
  }

  /** Create an immutable map from some key value pairs. */
  public static <A, B> Map<A, B> map(Tuple<A, ? extends B> a, Tuple<A, ? extends B>... as) {
    final HashMap<A, B> xs = new HashMap<A, B>();
    xs.put(a.getA(), a.getB());
    for (Tuple<A, ? extends B> aa : as) {
      xs.put(aa.getA(), aa.getB());
    }
    return Collections.unmodifiableMap(xs);
  }

  /** Create an immutable map from some key value pairs. */
  public static <A, B> Map<A, B> map(List<Tuple<A, B>> as) {
    final HashMap<A, B> xs = new HashMap<A, B>();
    for (Tuple<A, ? extends B> aa : as) {
      xs.put(aa.getA(), aa.getB());
    }
    return Collections.unmodifiableMap(xs);
  }

  /** Create an immutable map from some key value pairs. */
  public static <A, B> Map<A, B> map(Tuple<A, ? extends B>[] as) {
    final HashMap<A, B> xs = new HashMap<A, B>();
    for (Tuple<A, ? extends B> a : as) {
      xs.put(a.getA(), a.getB());
    }
    return Collections.unmodifiableMap(xs);
  }

  /** Create an immutable map from a map and an array. */
  public static <A, B> Map<A, B> map(Map<A, ? extends B> m, Tuple<A, ? extends B>... as) {
    final HashMap<A, B> r = new HashMap<A, B>();
    r.putAll(m);
    for (Tuple<A, ? extends B> a : as) {
      r.put(a.getA(), a.getB());
    }
    return Collections.unmodifiableMap(r);
  }

  /** Create an immutable map from some key value pairs. */
  public static <A, B> SortedMap<A, B> sortedMap(Tuple<A, ? extends B> a, Tuple<A, ? extends B>... as) {
    final SortedMap<A, B> xs = new TreeMap<A, B>();
    xs.put(a.getA(), a.getB());
    for (Tuple<A, ? extends B> aa : as) {
      xs.put(aa.getA(), aa.getB());
    }
    return Collections.unmodifiableSortedMap(xs);
  }
}
