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

import org.opencastproject.util.RequireUtil;

/**
 * This layout specification describes how to position a shape and how much of the underlying width of the canvas it
 * shall cover.
 */
public final class HorizontalCoverageLayoutSpec {
  private final AnchorOffset anchorOffset;
  private final double horizontalCoverage;

  /**
   * Create a new specification.
   * 
   * @param anchorOffset
   *          The distance of the anchor points of canvas and shape. The canvas is the "reference", the shape the
   *          "referring" part of the distance object.
   * @param horizontalCoverage
   *          0 <= horizontalCoverage <= 1. How much space of the canvas should be covered.
   */
  public HorizontalCoverageLayoutSpec(AnchorOffset anchorOffset, double horizontalCoverage) {
    this.anchorOffset = anchorOffset;
    this.horizontalCoverage = RequireUtil.between(horizontalCoverage, 0.0, 1.0);
  }

  /** Get the distance between the anchor points of canvas and shape. */
  public AnchorOffset getAnchorOffset() {
    return anchorOffset;
  }

  /** Get the horizontal coverage. */
  public double getHorizontalCoverage() {
    return horizontalCoverage;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that)
            || (that instanceof HorizontalCoverageLayoutSpec && eqFields((HorizontalCoverageLayoutSpec) that));
  }

  private boolean eqFields(HorizontalCoverageLayoutSpec that) {
    return eq(anchorOffset, that.anchorOffset) && horizontalCoverage == that.horizontalCoverage;
  }

  @Override
  public int hashCode() {
    return hash(anchorOffset, horizontalCoverage);
  }
}
