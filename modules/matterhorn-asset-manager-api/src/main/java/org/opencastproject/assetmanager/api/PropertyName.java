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
import static java.lang.String.format;

import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.Equality;

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * A full qualified property name.
 */
@Immutable
@ParametersAreNonnullByDefault
public final class PropertyName implements Serializable {
  private static final long serialVersionUID = -7542245565083273994L;

  private final String namespace;
  private final String name;

  /**
   * Create a new full qualified property name.
   */
  public PropertyName(String namespace, String name) {
    this.namespace = RequireUtil.notEmpty(namespace, "namespace");
    this.name = RequireUtil.notEmpty(name, "name");
  }

  /**
   * Create a new full qualified property name.
   */
  public static PropertyName mk(String namespace, String name) {
    return new PropertyName(namespace, name);
  }

  /** Return the namespace. */
  public String getNamespace() {
    return namespace;
  }

  /** Return the namespace local name. */
  public String getName() {
    return name;
  }

  //

  @Override public int hashCode() {
    return Equality.hash(namespace, name);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof PropertyName && eqFields((PropertyName) that));
  }

  private boolean eqFields(PropertyName that) {
    return eq(namespace, that.namespace) && eq(name, that.name);
  }

  @Override public String toString() {
    return format("PropertyFqn(%s, %s)", namespace, name);
  }
}
