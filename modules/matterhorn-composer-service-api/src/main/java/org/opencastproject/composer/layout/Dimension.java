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

/** Dimension of a rectangular shape. */
public final class Dimension {
  private final int width;
  private final int height;

  public Dimension(int width, int height) {
    this.width = RequireUtil.min(width, 0);
    this.height = RequireUtil.min(height, 0);
  }

  public static Dimension dimension(int width, int height) {
    return new Dimension(width, height);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Dimension && eqFields((Dimension) that));
  }

  private boolean eqFields(Dimension that) {
    return width == that.width && height == that.height;
  }

  @Override
  public int hashCode() {
    return hash(width, height);
  }

  @Override
  public String toString() {
    return format("Dimension(%d,%d)", width, height);
  }
}
