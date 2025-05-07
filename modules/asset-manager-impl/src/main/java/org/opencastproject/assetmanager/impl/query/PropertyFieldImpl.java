/*
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
package org.opencastproject.assetmanager.impl.query;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.fn.ProductBuilder;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PropertyFieldImpl<A> implements PropertyField<A> {
  private static final ProductBuilder pTest = ProductBuilder.E;

  private final PropertyFieldImpl self = this;

  private final PropertyName name;
  private final ValueType<A> mkValue;

  public PropertyFieldImpl(ValueType<A> mkValue, PropertyName name) {
    this.mkValue = mkValue;
    this.name = name;
  }

  @Override public PropertyName name() {
    return name;
  }

  @Override public Property mk(String mpId, A value) {
    return Property.mk(PropertyId.mk(mpId, name), mkValue.mk(value));
  }

  @Override public Predicate eq(final A right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate eq(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate lt(final A right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate lt(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate le(A right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");  }

  @Override public Predicate le(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate gt(A right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate gt(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate ge(A right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate ge(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate exists() {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate notExists() {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Order desc() {
    // TODO implement order
    throw new UnsupportedOperationException();
  }

  @Override public Order asc() {
    // TODO implement order
    throw new UnsupportedOperationException();
  }
}
