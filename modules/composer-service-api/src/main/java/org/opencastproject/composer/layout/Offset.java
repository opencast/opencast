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
import static org.opencastproject.util.EqualsUtil.hash;

/** Offset in a left-handed cartesian coordinate system. */
public final class Offset {
  private final int y;
  private final int x;

  public Offset(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public static Offset offset(int x, int y) {
    return new Offset(x, y);
  }

  /** Get the x offset. */
  public int getX() {
    return x;
  }

  /** Get the y offset. */
  public int getY() {
    return y;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Offset && eqFields((Offset) that));
  }

  private boolean eqFields(Offset that) {
    return x == that.x && y == that.y;
  }

  @Override
  public int hashCode() {
    return hash(x, y);
  }

  @Override
  public String toString() {
    return format("Offset(%d,%d)", x, y);
  }
}
