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
package org.opencastproject.composer.layout;

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.hash;

import org.opencastproject.util.RequireUtil;

/** Anchor point of a rectangular shape, expressed relatively to its width and height. */
public final class Anchor {
  private final double left;
  private final double top;

  /**
   * Create a new anchor point.
   *
   * @param left
   *          width ratio measured from the left. 0 <= left <= 1
   * @param top
   *          height ratio measured from the top. 0 <= top <= 1
   */
  public Anchor(double left, double top) {
    this.left = RequireUtil.between(left, 0.0, 1.0);
    this.top = RequireUtil.between(top, 0.0, 1.0);
  }

  public double getLeft() {
    return left;
  }

  public double getTop() {
    return top;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Anchor && eqFields((Anchor) that));
  }

  private boolean eqFields(Anchor that) {
    return (left == that.left) && (top == that.top);
  }

  @Override
  public int hashCode() {
    return hash(left, top);
  }

  @Override
  public String toString() {
    return format("Anchor(%f,%f)", left, top);
  }
}
