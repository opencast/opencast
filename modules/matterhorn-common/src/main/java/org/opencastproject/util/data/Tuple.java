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

import static org.opencastproject.util.EqualsUtil.eqClasses;
import static org.opencastproject.util.EqualsUtil.hash;

/** A pair. */
public final class Tuple<A, B> {

  private final A a;
  private final B b;

  public Tuple(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public A getA() {
    return a;
  }

  public B getB() {
    return b;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) return true;
    if (!eqClasses(this, that)) return false;
    Tuple thatc = (Tuple) that;
    return a.equals(thatc.a) && b.equals(thatc.b);
  }

  @Override
  public int hashCode() {
    return hash(a, b);
  }

  public static <A, B> Tuple<A, B> tuple(A a, B b) {
    return new Tuple<A, B>(a, b);
  }

  @Override public String toString() {
    return "(" + a + "," + b + ")";
  }
}
