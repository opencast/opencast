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
package org.opencastproject.assetmanager.impl;

import static org.opencastproject.util.EqualsUtil.hash;

import org.opencastproject.assetmanager.api.Version;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The version of an archived media package or element.
 * <p>
 * This class encapsulates the actual data type used for identifying versions to minimize
 * API changes.
 */
@ParametersAreNonnullByDefault
public final class VersionImpl implements Version {
  private static final long serialVersionUID = 3060347920702655628L;

  public static final VersionImpl FIRST = mk(0L);

  private final long nr;

  public VersionImpl(long nr) {
    this.nr = nr;
  }

  /** Constructor function. */
  public static VersionImpl mk(long nr) {
    return new VersionImpl(nr);
  }

  public static VersionImpl mk(Version v) {
    return new VersionImpl(Long.parseLong(v.toString()));
  }

  /** Create the next version after the <code>latest</code>. */
  public static VersionImpl next(long latest) {
    return new VersionImpl(latest + 1);
  }

  public long value() {
    return nr;
  }

  @Override
  public boolean isOlder(Version v) {
    return compareTo(v) < 0;
  }

  @Override
  public boolean isYounger(Version v) {
    return compareTo(v) > 0;
  }

  @Override
  public int compareTo(Version v) {
    return Long.compare(nr, RuntimeTypes.convert(v).nr);
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof VersionImpl && eqFields((VersionImpl) that));
  }

  private boolean eqFields(VersionImpl that) {
    return this.nr == that.nr;
  }

  @Override
  public int hashCode() {
    return hash(nr);
  }

  @Override
  public String toString() {
    return String.valueOf(nr);
  }
}
