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

import static org.opencastproject.composer.layout.Offset.offset;

/** Hosts predefined positioning specifications for the {@link TwoShapeLayout}. */
public final class TwoShapeLayouts {
  private TwoShapeLayouts() {
  }

  public static final class TwoShapeLayoutSpec {
    private final HorizontalCoverageLayoutSpec upper;
    private final HorizontalCoverageLayoutSpec lower;

    public TwoShapeLayoutSpec(HorizontalCoverageLayoutSpec upper, HorizontalCoverageLayoutSpec lower) {
      this.upper = upper;
      this.lower = lower;
    }

    public HorizontalCoverageLayoutSpec getUpper() {
      return upper;
    }

    public HorizontalCoverageLayoutSpec getLower() {
      return lower;
    }
  }

  /**
   * Layout specification placing the upper media in the top left and the lower media in the bottom right corner of the
   * canvas. The media in the top left corner is scaled to take 20% of the canvas size while the other media takes 80%.
   */
  public static final TwoShapeLayoutSpec TOP_LEFT_SMALL_BOTTOM_RIGHT_BIG = new TwoShapeLayoutSpec(
          new HorizontalCoverageLayoutSpec(new AnchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, offset(0, 0)), 0.2),
          new HorizontalCoverageLayoutSpec(new AnchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, offset(0, 0)),
                  0.8));
}
