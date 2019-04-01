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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.assetmanager.api.query

import org.opencastproject.assetmanager.api.Availability
import org.opencastproject.assetmanager.api.PropertyName
import org.opencastproject.assetmanager.api.Value.ValueType

import java.util.Date

/**
 * To phrase queries to the [org.opencastproject.assetmanager.api.AssetManager].
 *
 *
 * Implementations are supposed to be immutable so that one builder can be safely
 * used in a concurrent environment.
 */
interface AQueryBuilder {
    /**
     * Determine what should be included in the result records, i.e. what will actually be fetched from the database.
     * If no target is given, only the media package ID ([ARecord.getMediaPackageId]) is fetched.
     *
     *
     * Use targets to reduce the amount of database IO, e.g. if you're not interested in the attached properties do
     * not select them. Or, on the other hand if you want to work with properties only, do not select the snapshot
     * ([AQueryBuilder.snapshot]).
     *
     *
     * Please note that a result record always represents a snapshot accompanied by the properties of the whole episode.
     * That means that always snapshots are being selected. In case a property target is given, it only means that those
     * properties are added to the result set.
     * Please also note that properties are stored per episode, not per snapshot.
     *
     * @see ARecord
     */
    fun select(vararg target: Target): ASelectQuery

    /**
     * Create a new deletion query.
     *
     *
     * The query will only affect snapshots owned by the given owner. If the target is a property
     * the owner parameter will be ignored since properties belong to the whole episode and not
     * to individual snapshots.
     *
     * @param owner
     * the name of the owner or the empty string
     */
    fun delete(owner: String, target: Target): ADeleteQuery

    //
    // direct predicate constructors
    //

    /* -- */
    fun mediaPackageIds(vararg mpIds: String): Predicate

    /** Create a predicate to match an snapshot's media package ID.  */
    fun mediaPackageId(mpId: String): Predicate

    /** Get the snapshot's "seriesId" field. Use it to create a predicate.  */
    fun seriesId(): Field<String>

    /**
     * Create a predicate to match a snapshot's organization ID.
     *
     */
    @Deprecated("use {@link #organizationId()}.eq(orgId) instead")
    fun organizationId(orgId: String): Predicate

    /** Get the snapshot's "organizationId" field. Use it to create a predicate.  */
    fun organizationId(): Field<String>

    /** Get the snapshot's "owner" field. Use it to create a predicate.  */
    fun owner(): Field<String>

    fun availability(availability: Availability): Predicate

    fun storage(storage: String): Predicate

    /** Get the snapshots's "availability" field. Use it to create a predicate.  */
    fun availability(): Field<Availability>

    fun storage(): Field<String>

    /** Create a predicate that matches all snapshots with properties of the given namespace.  */
    fun hasPropertiesOf(namespace: String): Predicate

    /** Create a predicate that matches all snapshots with properties.  */
    fun hasProperties(): Predicate

    //
    // field definitions
    //

    /** Get the snapshot's "archived" field. Use it to create a predicate.  */
    fun archived(): Field<Date>

    /** Get the snapshot's "version" field. Use it to create a predicate.  */
    fun version(): VersionField

    /**
     * Create a field to query properties. Each parameter may be wild carded with the empty string.
     *
     * @param namespace
     * the namespace or "" to select all namespaces
     * @param name
     * the property name or "" to select all property names
     */
    fun <A> property(ev: ValueType<A>, namespace: String, name: String): PropertyField<A>

    fun <A> property(ev: ValueType<A>, fqn: PropertyName): PropertyField<A>

    //
    // targets of a select or delete query
    //

    /** Select or delete a snapshot.  */
    fun snapshot(): Target

    /**
     * Select or delete all properties that belong to the given namespaces.
     * Use an empty list of arguments to handle all properties of the media package.
     */
    fun propertiesOf(vararg namespace: String): Target

    /**
     * Select or delete all given properties.
     * Use an empty list of arguments to handle all properties of the media package.
     */
    fun properties(vararg fqn: PropertyName): Target

    //
    // zero/neutral elements
    //

    /**
     * The zero element of [Target]. Selecting nothing just selects nothing.
     */
    fun nothing(): Target

    /**
     * The zero element of [Field].
     * Using zero in a predicate expression yields an [.always] predicate.
     */
    fun zero(): Field<*>

    /**
     * The zero element of [Predicate].
     * An empty predicate does nothing.
     */
    fun always(): Predicate
}
