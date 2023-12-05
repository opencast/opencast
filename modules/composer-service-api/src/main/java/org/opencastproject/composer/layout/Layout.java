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

/** The layout of a rectangular shape on a rectangular canvas. */
public final class Layout {
  private final Dimension dim;
  private final Offset offset;

  /**
   * Create a new layout.
   *
   * @param dim
   *          the dimension of the shape
   * @param offset
   *          the offset between the top left corner of the shape and the top left corner of the canvas
   */
  public Layout(Dimension dim, Offset offset) {
    this.dim = dim;
    this.offset = offset;
  }

  /** Return the dimension of the shape. */
  public Dimension getDimension() {
    return dim;
  }

  /** Return the offset between the shape's and canvas' origin. */
  public Offset getOffset() {
    return offset;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Layout && eqFields((Layout) that));
  }

  private boolean eqFields(Layout that) {
    return eq(dim, that.dim) && eq(offset, that.offset);
  }

  @Override
  public int hashCode() {
    return hash(dim, offset);
  }
}
