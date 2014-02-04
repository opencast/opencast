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
package org.opencastproject.util.data.functions;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import static org.opencastproject.util.data.Tuple.tuple;

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
}
