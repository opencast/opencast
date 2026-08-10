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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  /** Create a list from an array. */
  @SafeVarargs
  public static <A> List<A> list(A... as) {
    final List<A> t = new ArrayList<>();
    java.util.Collections.addAll(t, as);
    return t;
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

  /** Concat (aka flatten) a collection of collections by concatenating them all. [[a]] -&gt; [a] */
  public static <A, M extends Collection<? extends Collection<A>>> List<A> concat(M as) {
    final List<A> target = new ArrayList<>(as.size());
    for (Collection<A> a : as) {
      target.addAll(a);
    }
    return target;
  }

}
