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

import java.lang.reflect.Array;

public final class Arrays {
  private Arrays() {
  }

  /** Create a new array by prepending <code>a</code> to <code>as</code>: <code>[a, as0, as1, .. asn]</code> */
  public static <A> A[] cons(Class<A> type, A a, A[] as) {
    @SuppressWarnings("unchecked")
    final A[] x = (A[]) Array.newInstance(type, as.length + 1);
    x[0] = a;
    System.arraycopy(as, 0, x, 1, as.length);
    return x;
  }

  /** Create a new array by appending <code>a</code> to <code>as</code>: <code>[as0, as1, .. asn, a]</code>. */
  public static <A> A[] append(Class<A> type, A[] as, A a) {
    @SuppressWarnings("unchecked")
    final A[] x = (A[]) Array.newInstance(type, as.length + 1);
    System.arraycopy(as, 0, x, 0, as.length);
    x[as.length] = a;
    return x;
  }

  /** Create an array from the vararg parameter list. */
  public static <A> A[] array(A... as) {
    return as;
  }

  /** Turn a value into a single element array. */
  public static <A> Function<A, A[]> singleton(final Class<A> type) {
    return new Function<A, A[]>() {
      @Override
      public A[] apply(A a) {
        @SuppressWarnings("unchecked")
        final A[] as = (A[]) Array.newInstance(type, 1);
        as[0] = a;
        return as;
      }
    };
  }

  /** Make a string from a collection separating each element by <code>sep</code>. */
  public static <A> String mkString(A[] as, String sep) {
    final StringBuilder b = new StringBuilder();
    for (Object a : as) b.append(a).append(sep);
    return b.substring(0, Math.max(b.length() - sep.length(), 0));
  }
}
