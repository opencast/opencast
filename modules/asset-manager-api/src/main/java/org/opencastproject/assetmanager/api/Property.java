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
package org.opencastproject.assetmanager.api;

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Equality.hash;
import static java.lang.String.format;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A property of a media package.
 * Properties can be defined and associated to a media package's version history.
 */
@ParametersAreNonnullByDefault
public class Property {
  private final PropertyId id;
  private final Value value;

  public Property(PropertyId id, Value value) {
    this.id = id;
    this.value = value;
  }

  public static Property mk(PropertyId id, Value value) {
    return new Property(id, value);
  }

  public PropertyId getId() {
    return id;
  }

  public Value getValue() {
    return value;
  }

  @Override public int hashCode() {
    return hash(id, value);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof Property && eqFields((Property) that));
  }

  private boolean eqFields(Property that) {
    return eq(id, that.id) && eq(value, that.value);
  }

  @Override public String toString() {
    return format("Property(%s=%s)", id, value);
  }
}
