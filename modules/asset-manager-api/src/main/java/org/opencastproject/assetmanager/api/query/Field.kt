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

/**
 * A Field refers to a field in the persistence layer of the asset manager.
 * This may either be a real field like a property or a virtual field that
 * helps to build a predicate for a real field.
 *
 * @param <A> type of the field, e.g. String or Date.
</A> */
// TODO split type into predicate builder and order builder methods
interface Field<A> {
    /**
     * Create a predicate that holds true if the field's value and constant value `right` are equal.
     */
    fun eq(right: A): Predicate

    /**
     * Create a predicate that holds true if the field's value and the value of property field `right` are equal.
     */
    fun eq(right: PropertyField<A>): Predicate

    /**
     * Create a predicate that holds true if the field's value is strictly less than constant value `right`.
     */
    fun lt(right: A): Predicate

    /**
     * Create a predicate that holds true if the field's value is strictly less than the value of property field `right`.
     */
    fun lt(right: PropertyField<A>): Predicate

    /**
     * Create a predicate that holds true if the field's value is less than constant value `right` or equal to it.
     */
    fun le(right: A): Predicate

    /**
     * Create a predicate that holds true if the field's value is less than the value of property field `right` or equal to it.
     */
    fun le(right: PropertyField<A>): Predicate

    /**
     * Create a predicate that holds true if the field's value is strictly greater than constant value `right`.
     */
    fun gt(right: A): Predicate

    /**
     * Create a predicate that holds true if the field's value is strictly greater than the value of property field `right`.
     */
    fun gt(right: PropertyField<A>): Predicate

    /**
     * Create a predicate that holds true if the field's value is greater than constant value `right` or equal to it.
     */
    fun ge(right: A): Predicate

    /**
     * Create a predicate that holds true if the field's value is greater than the value of property field `right` or equal to it.
     */
    fun ge(right: PropertyField<A>): Predicate

    /**
     * Create a predicate that holds true if the field exists.
     */
    fun exists(): Predicate

    /**
     * Create a predicate that holds true if the field does not exist.
     */
    fun notExists(): Predicate

    /**
     * Create a descending order specifier for the field.
     */
    fun desc(): Order

    /**
     * Create an ascending order specifier for the field.
     */
    fun asc(): Order
}
