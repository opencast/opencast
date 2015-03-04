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

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

/**
 * This layout specification describes where to position a shape in relation to another.
 * <p/>
 * In contrast to the {@link HorizontalCoverageLayoutSpec} this spec does not scale any shape.
 */
public final class AbsolutePositionLayoutSpec {
  private final AnchorOffset anchorOffset;

  /**
   * Create a new specification.
   *
   * @param anchorOffset
   *          The distance of the anchor points of canvas and shape. The canvas is the "reference", the shape the
   *          "referring" part of the distance object.
   */
  public AbsolutePositionLayoutSpec(AnchorOffset anchorOffset) {
    this.anchorOffset = anchorOffset;
  }

  /** Get the distance between the anchor points of canvas and shape. */
  public AnchorOffset getAnchorOffset() {
    return anchorOffset;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that)
            || (that instanceof AbsolutePositionLayoutSpec && eqFields((AbsolutePositionLayoutSpec) that));
  }

  private boolean eqFields(AbsolutePositionLayoutSpec that) {
    return eq(anchorOffset, that.anchorOffset);
  }

  @Override
  public int hashCode() {
    return hash(anchorOffset);
  }
}
