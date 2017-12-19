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

package org.opencastproject.util.data.functions;

import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import java.util.List;

/** Functions for tuples. */
public final class Tuples {
  private Tuples() {
  }

  public static <A, B> Function<A, Tuple<A, B>> tupleA(final B b) {
    return new Function<A, Tuple<A, B>>() {
      @Override public Tuple<A, B> apply(A a) {
        return tuple(a, b);
      }
    };
  }

  public static <A, B> Function<B, Tuple<A, B>> tupleB(final A a) {
    return new Function<B, Tuple<A, B>>() {
      @Override public Tuple<A, B> apply(B b) {
        return tuple(a, b);
      }
    };
  }

  public static <A> Function<A[], Tuple<A, A>> fromArray() {
    return new Function<A[], Tuple<A, A>>() {
      @Override public Tuple<A, A> apply(A[] as) {
        return tuple(as[0], as[1]);
      }
    };
  }

  public static <A> Function<List<A>, Tuple<A, A>> fromList() {
    return new Function<List<A>, Tuple<A, A>>() {
      @Override public Tuple<A, A> apply(List<A> as) {
        return tuple(as.get(0), as.get(1));
      }
    };
  }

  public static <A> Function<A[], Boolean> arrayHasSize(final int size) {
    return new Function<A[], Boolean>() {
      @Override public Boolean apply(A[] as) {
        return as.length == size;
      }
    };
  }

  public static <A> Function<List<A>, Boolean> listHasSize(final int size) {
    return new Function<List<A>, Boolean>() {
      @Override public Boolean apply(List<A> as) {
        return as.size() == size;
      }
    };
  }
}
