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
package org.opencastproject.fn.juc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Constructor functions for immutable data types. */
public final class Immutables {
  private Immutables() {
  }

  public static <A> List<A> nil() {
    return Collections.emptyList();
  }

  /** Create an immutable map from <code>a</code>. */
  public static <A, B> Map<A, B> mk(Map<A, B> a) {
    return Collections.unmodifiableMap(new HashMap<A, B>(a));
  }

  /** Create an immutable list from <code>a</code>. */
  public static <A> List<A> mk(List<A> a) {
    return Collections.unmodifiableList(new ArrayList<A>(a));
  }

  public static <A>Set<A> mk(Set<A> a) {
    return Collections.unmodifiableSet(new HashSet<A>(a));
  }

  /** Create an immutable list from an array. */
  public static <A> List<A> list(A... as) {
    final List<A> a = new ArrayList<A>(as.length);
    Collections.addAll(a, as);
    return mk(a);
  }

  /** Create an immutable set from an array. */
  public static <A> Set<A> set(A... as) {
    final HashSet<A> a = new HashSet<A>(as.length);
    Collections.addAll(a, as);
    return mk(a);
  }
}
