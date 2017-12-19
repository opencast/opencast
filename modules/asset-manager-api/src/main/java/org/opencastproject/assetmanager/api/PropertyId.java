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

@Immutable
@ParametersAreNonnullByDefault
public final class PropertyId implements Serializable {
  private static final long serialVersionUID = -2614578081057869958L;

  private final String mpId;
  private final String namespace;
  private final String name;

  /**
   * Create a new property ID.
   */
  public PropertyId(String mpId, String namespace, String name) {
    this.mpId = RequireUtil.notEmpty(mpId, "mpId");
    this.namespace = RequireUtil.notEmpty(namespace, "namespace");
    this.name = RequireUtil.notEmpty(name, "name");
  }

  /**
   * Create a new property ID from the given parameters.
   */
  public static PropertyId mk(String mpId, String namespace, String propertyName) {
    return new PropertyId(mpId, namespace, propertyName);
  }

  /**
   * Create a new property ID from the given parameters.
   */
  public static PropertyId mk(String mpId, PropertyName fqn) {
    return new PropertyId(mpId, fqn.getNamespace(), fqn.getName());
  }

  public String getMediaPackageId() {
    return mpId;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public PropertyName getFqn() {
    return PropertyName.mk(namespace, name);
  }

  //

  @Override public int hashCode() {
    return Equality.hash(mpId, namespace, name);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof PropertyId && eqFields((PropertyId) that));
  }

  private boolean eqFields(PropertyId that) {
    return eq(mpId, that.mpId) && eq(namespace, that.namespace) && eq(name, that.name);
  }

  @Override public String toString() {
    return format("PropertyId(%s, %s, %s)", mpId, namespace, name);
  }
}
