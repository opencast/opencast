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
package org.opencastproject.assetmanager.api.query;

import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.Version;

import java.util.Date;

/**
 * The schema class helps to build type safe and easy to use property schemas.
 * It makes code using properties more readable and reliable.
 */
public abstract class PropertySchema {
  protected final AQueryBuilder q;
  protected final String namespace;

  /**
   * Create a new property schema.
   *
   * @param q a query builder
   * @param namespace
   */
  public PropertySchema(AQueryBuilder q, String namespace) {
    this.q = q;
    this.namespace = namespace;
  }

  /** Get the namespace of the schema. */
  public String namespace() {
    return namespace;
  }

  /** Get a predicate that matches if a property of the schema's namespace exists. */
  public Predicate hasPropertiesOfNamespace() {
    return q.hasPropertiesOf(namespace);
  }

  /** Get a target to select all properties of the schema's namespace. */
  public Target allProperties() {
    return q.propertiesOf(namespace);
  }

  /** Generic property field constructor. */
  protected <A> PropertyField<A> prop(ValueType<A> ev, String name) {
    return q.property(ev, namespace, name);
  }

  /** Create a property field for Strings. */
  protected PropertyField<String> stringProp(String name) {
    return prop(Value.STRING, name);
  }

  /** Create a property field for Longs. */
  protected PropertyField<Long> longProp(String name) {
    return prop(Value.LONG, name);
  }

  /** Create a property field for Booleans. */
  protected PropertyField<Boolean> booleanProp(String name) {
    return prop(Value.BOOLEAN, name);
  }

  /** Create a property field for Dates. */
  protected PropertyField<Date> dateProp(String name) {
    return prop(Value.DATE, name);
  }

  /** Create a property field for Versions. */
  protected PropertyField<Version> versionProp(String name) {
    return prop(Value.VERSION, name);
  }
}
