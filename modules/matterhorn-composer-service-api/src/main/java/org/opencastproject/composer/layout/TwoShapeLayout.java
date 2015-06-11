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

package org.opencastproject.composer.layout;

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

/** Layout of two shapes on a common canvas. */
public final class TwoShapeLayout {
  private final Layout upper;
  private final Layout lower;
  private final Dimension canvas;

  public TwoShapeLayout(Dimension canvas, Layout upper, Layout lower) {
    this.upper = upper;
    this.lower = lower;
    this.canvas = canvas;
  }

  /** Get the layout information of the upper shape. */
  public Layout getUpper() {
    return upper;
  }

  /** Get the layout information of the lower shape. */
  public Layout getLower() {
    return lower;
  }

  /** Get the dimension of the composition canvas. */
  public Dimension getCanvas() {
    return canvas;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof TwoShapeLayout && eqFields((TwoShapeLayout) that));
  }

  private boolean eqFields(TwoShapeLayout that) {
    return eq(upper, that.upper) && eq(lower, that.lower) && eq(canvas, that.canvas);
  }

  @Override
  public int hashCode() {
    return hash(upper, lower, canvas);
  }
}
