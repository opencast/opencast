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

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Field;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;

import java.util.Date;

/**
 * A wrapper for query builder. All method calls are delegated to the wrapped builder by default.
 * Override to provide extra functionality.
 */
public class AQueryBuilderDecorator implements AQueryBuilder {
  private final AQueryBuilder delegate;

  public AQueryBuilderDecorator(AQueryBuilder delegate) {
    this.delegate = delegate;
  }

  @Override public ASelectQuery select(Target... target) {
    return delegate.select(target);
  }

  @Override public ADeleteQuery delete(String owner, Target target) {
    return delegate.delete(owner, target);
  }

  @Override public Predicate mediaPackageId(String mpId) {
    return delegate.mediaPackageId(mpId);
  }

  @Override public Field<String> seriesId() {
    return delegate.seriesId();
  }

  @Override public Predicate organizationId(String orgId) {
    return delegate.organizationId(orgId);
  }

  @Override public Field<String> organizationId() {
    return delegate.organizationId();
  }

  @Override public Field<String> owner() {
    return delegate.owner();
  }

  @Override public Predicate availability(Availability availability) {
    return delegate.availability(availability);
  }

  @Override public Field<Availability> availability() {
    return delegate.availability();
  }

  @Override public Predicate storage(String storageId) {
    return delegate.storage(storageId);
  }

  @Override public Field<String> storage() {
    return delegate.storage();
  }

  @Override public Predicate hasPropertiesOf(String namespace) {
    return delegate.hasPropertiesOf(namespace);
  }

  @Override public Predicate hasProperties() {
    return delegate.hasProperties();
  }

  @Override public Field<Date> archived() {
    return delegate.archived();
  }

  @Override public VersionField version() {
    return delegate.version();
  }

  @Override public <A> PropertyField<A> property(ValueType<A> ev, String namespace, String name) {
    return delegate.property(ev, namespace, name);
  }

  @Override public <A> PropertyField<A> property(ValueType<A> ev, PropertyName fqn) {
    return delegate.property(ev, fqn);
  }

  @Override public Target snapshot() {
    return delegate.snapshot();
  }

  @Override public Target propertiesOf(String... namespace) {
    return delegate.propertiesOf(namespace);
  }

  @Override public Target properties(
          PropertyName... fqn) {
    return delegate.properties(fqn);
  }

  @Override public Target nothing() {
    return delegate.nothing();
  }

  @Override public Field zero() {
    return delegate.zero();
  }

  @Override public Predicate always() {
    return delegate.always();
  }
}
