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
package org.opencastproject.archive.api;

import static org.opencastproject.util.EqualsUtil.hash;

/**
 * The version number of an archived media package or element.
 * <p/>
 * This class encapsulates the actual data type used for identifying versions to minimize
 * API changes.
 */
public final class Version {
  public static final Version FIRST = version(0L);

  private final long nr;

  public Version(long nr) {
    this.nr = nr;
  }

  /** Contructor function. */
  public static Version version(long nr) {
    return new Version(nr);
  }

  public long value() {
    return nr;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Version && eqFields((Version) that));
  }

  private boolean eqFields(Version that) {
    return this.nr == that.nr;
  }

  @Override
  public int hashCode() {
    return hash(nr);
  }

  @Override public String toString() {
    return Long.toString(nr);
  }
}
