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

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

/** The offset between the anchor points of two rectangular shapes. */
public final class AnchorOffset {
  private final Anchor referenceAnchor;
  private final Anchor referringAnchor;
  private final Offset offset;

  /**
   * Create a new offset.
   *
   * @param referenceAnchor
   *          anchor point of the reference shape
   * @param referringAnchor
   *          anchor point of the referring shape
   * @param offset
   *          offset between the two anchor points measured from the reference
   */
  public AnchorOffset(Anchor referenceAnchor, Anchor referringAnchor, Offset offset) {
    this.referenceAnchor = referenceAnchor;
    this.referringAnchor = referringAnchor;
    this.offset = offset;
  }

  public static AnchorOffset anchorOffset(Anchor referenceAnchor, Anchor referringAnchor, Offset offset) {
    return new AnchorOffset(referenceAnchor, referringAnchor, offset);
  }

  public static AnchorOffset anchorOffset(Anchor referenceAnchor, Anchor referringAnchor, int xOffset, int yOffset) {
    return new AnchorOffset(referenceAnchor, referringAnchor, new Offset(xOffset, yOffset));
  }

  /** Get the anchor point of the reference shape. */
  public Anchor getReferenceAnchor() {
    return referenceAnchor;
  }

  /** Get the anchor point of the shape referring to the reference shape. */
  public Anchor getReferringAnchor() {
    return referringAnchor;
  }

  /** Get the offset between the two anchor points. */
  public Offset getOffset() {
    return offset;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof AnchorOffset && eqFields((AnchorOffset) that));
  }

  private boolean eqFields(AnchorOffset that) {
    return eq(offset, that.offset) && eq(referenceAnchor, that.referenceAnchor)
            && eq(referringAnchor, that.referringAnchor);
  }

  @Override
  public int hashCode() {
    return hash(offset, referenceAnchor, referringAnchor);
  }

  @Override
  public String toString() {
    return format("AnchorOffset(referenceAnchor=%s,referringAnchor=%s,offset=%s)", referenceAnchor, referringAnchor,
            offset);
  }
}
