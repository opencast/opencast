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
import java.util.ArrayList;
import java.util.List;

import static org.opencastproject.util.data.Option.some;

public final class Arrays {
  private Arrays() {
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
   * @see java.util.Arrays#sort(Object[])
   */
  public static <A> A[] sort(A[] as) {
    java.util.Arrays.sort(as);
    return as;
  }

  /** Create a new array by prepending <code>a</code> to <code>as</code>: <code>[a, as0, as1, .. asn]</code> */
  public static <A> A[] cons(A a, A[] as) {
    final A[] x = (A[]) Array.newInstance(a.getClass(), as.length + 1);
    x[0] = a;
    System.arraycopy(as, 0, x, 1, as.length);
    return x;
  }

  /** Create a new array by appending <code>a</code> to <code>as</code>: <code>[as0, as1, .. asn, a]</code>. */
  public static <A> A[] append(A[] as, A a) {
    final List<A> xs = new ArrayList<A>(as.length + 1);
    xs.add(a);
    for (A y : as) xs.add(y);
    return (A[]) xs.toArray(new Object[xs.size()]);
  }

  /** Create an array from the vararg parameter list. */
  public static <A> A[] array(A... as) {
    return as;
  }

  public static <A> Function<A[], List<A>> toList() {
    return new Function<A[], List<A>>() {
      @Override
      public List<A> apply(A[] as) {
        if (as != null) {
          return Collections.list(as);
        } else {
          return Collections.nil();
        }
      }
    };
  }

  /** Turn a value into a single element array. */
  public static <A> Function<A, A[]> singleton() {
    return new Function<A, A[]>() {
      @Override
      public A[] apply(A a) {
        return (A[]) new Object[]{a};
      }
    };
  }

  /** Functional version of {@link #head(Object[])}. */
  public static <A> Function<A[], Option<A>> head() {
    return new Function<A[], Option<A>>() {
      @Override
      public Option<A> apply(A[] as) {
        return head(as);
      }
    };
  }

  /** Functional version of {@link #sort}. */
  public static <A> Function<A[], A[]> sort() {
    return new Function<A[], A[]>() {
      @Override
      public A[] apply(A[] as) {
        return sort(as);
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
