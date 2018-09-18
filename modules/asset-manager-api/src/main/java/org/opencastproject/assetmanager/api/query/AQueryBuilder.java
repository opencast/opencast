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

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value.ValueType;

import java.util.Date;

/**
 * To phrase queries to the {@link org.opencastproject.assetmanager.api.AssetManager}.
 * <p>
 * Implementations are supposed to be immutable so that one builder can be safely
 * used in a concurrent environment.
 */
public interface AQueryBuilder {
  /**
   * Determine what should be included in the result records, i.e. what will actually be fetched from the database.
   * If no target is given, only the media package ID ({@link ARecord#getMediaPackageId()}) is fetched.
   * <p>
   * Use targets to reduce the amount of database IO, e.g. if you're not interested in the attached properties do
   * not select them. Or, on the other hand if you want to work with properties only, do not select the snapshot
   * ({@link AQueryBuilder#snapshot()}).
   * <p>
   * Please note that a result record always represents a snapshot accompanied by the properties of the whole episode.
   * That means that always snapshots are being selected. In case a property target is given, it only means that those
   * properties are added to the result set.
   * Please also note that properties are stored per episode, not per snapshot.
   *
   * @see ARecord
   */
  ASelectQuery select(Target... target);

  /**
   * Create a new deletion query.
   * <p>
   * The query will only affect snapshots owned by the given owner. If the target is a property
   * the owner parameter will be ignored since properties belong to the whole episode and not
   * to individual snapshots.
   *
   * @param owner
   *          the name of the owner or the empty string
   */
  ADeleteQuery delete(String owner, Target target);

  //
  // direct predicate constructors
  //

  /* -- */
  Predicate mediaPackageIds(String... mpIds);

  /** Create a predicate to match an snapshot's media package ID. */
  Predicate mediaPackageId(String mpId);

  /** Get the snapshot's "seriesId" field. Use it to create a predicate. */
  Field<String> seriesId();

  /**
   * Create a predicate to match a snapshot's organization ID.
   *
   * @deprecated use {@link #organizationId()}.eq(orgId) instead
   */
  Predicate organizationId(String orgId);

  /** Get the snapshot's "organizationId" field. Use it to create a predicate. */
  Field<String> organizationId();

  /** Get the snapshot's "owner" field. Use it to create a predicate. */
  Field<String> owner();

  Predicate availability(Availability availability);

  Predicate storage(String storage);

  /** Get the snapshots's "availability" field. Use it to create a predicate. */
  Field<Availability> availability();

  Field<String> storage();

  /** Create a predicate that matches all snapshots with properties of the given namespace. */
  Predicate hasPropertiesOf(String namespace);

  /** Create a predicate that matches all snapshots with properties. */
  Predicate hasProperties();

  //
  // field definitions
  //

  /** Get the snapshot's "archived" field. Use it to create a predicate. */
  Field<Date> archived();

  /** Get the snapshot's "version" field. Use it to create a predicate. */
  VersionField version();

  /**
   * Create a field to query properties. Each parameter may be wild carded with the empty string.
   *
   * @param namespace
   *         the namespace or "" to select all namespaces
   * @param name
   *         the property name or "" to select all property names
   */
  <A> PropertyField<A> property(ValueType<A> ev, String namespace, String name);

  <A> PropertyField<A> property(ValueType<A> ev, PropertyName fqn);

  //
  // targets of a select or delete query
  //

  /** Select or delete a snapshot. */
  Target snapshot();

  /**
   * Select or delete all properties that belong to the given namespaces.
   * Use an empty list of arguments to handle all properties of the media package.
   */
  Target propertiesOf(String... namespace);

  /**
   * Select or delete all given properties.
   * Use an empty list of arguments to handle all properties of the media package.
   */
  Target properties(PropertyName... fqn);

  //
  // zero/neutral elements
  //

  /**
   * The zero element of {@link Target}. Selecting nothing just selects nothing.
   */
  Target nothing();

  /**
   * The zero element of {@link Field}.
   * Using zero in a predicate expression yields an {@link #always()} predicate.
   */
  Field zero();

  /**
   * The zero element of {@link Predicate}.
   * An empty predicate does nothing.
   */
  Predicate always();
}
