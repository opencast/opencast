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

/** A 3-tuple. */
public final class Tuple3<A, B, C> {

  private final A a;
  private final B b;
  private final C c;

  public Tuple3(A a, B b, C c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public A getA() {
    return a;
  }

  public B getB() {
    return b;
  }

  public C getC() {
    return c;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) return true;
    if (!eqClasses(this, that)) return false;
    Tuple3 thatc = (Tuple3) that;
    return a.equals(thatc.a) && b.equals(thatc.b) && c.equals(thatc.c);
  }

  @Override
  public int hashCode() {
    return hash(a, b, c);
  }

  public static <A, B, C> Tuple3<A, B, C> tuple3(A a, B b, C c) {
    return new Tuple3<A, B, C>(a, b, c);
  }
}
