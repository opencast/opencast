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

/**
 * A Field refers to a field in the persistence layer of the asset manager.
 * This may either be a real field like a property or a virtual field that
 * helps to build a predicate for a real field.
 *
 * @param <A> type of the field, e.g. String or Date.
 */
// TODO split type into predicate builder and order builder methods
public interface Field<A> {
  /**
   * Create a predicate that holds true if the field's value and constant value
   * <code>right</code> are equal.
   */
  Predicate eq(A right);

  /**
   * Create a predicate that holds true if the field's value and the value of
   * property field <code>right</code> are equal.
   */
  Predicate eq(PropertyField<A> right);

  /**
   * Create a predicate that holds true if the field's value is strictly less
   * than constant value <code>right</code>.
   */
  Predicate lt(A right);

  /**
   * Create a predicate that holds true if the field's value is strictly less
  * than the value of property field <code>right</code>.
   */
  Predicate lt(PropertyField<A> right);

  /**
   * Create a predicate that holds true if the field's value is less than
   * constant value <code>right</code> or equal to it.
   */
  Predicate le(A right);

  /**
   * Create a predicate that holds true if the field's value is less than the
   * value of property field <code>right</code> or equal to it.
   */
  Predicate le(PropertyField<A> right);

  /**
   * Create a predicate that holds true if the field's value is strictly greater
   * than constant value <code>right</code>.
   */
  Predicate gt(A right);

  /**
   * Create a predicate that holds true if the field's value is strictly greater
   * than the value of property field <code>right</code>.
   */
  Predicate gt(PropertyField<A> right);

  /**
   * Create a predicate that holds true if the field's value is greater than
   * constant value <code>right</code> or equal to it.
   */
  Predicate ge(A right);

  /**
   * Create a predicate that holds true if the field's value is greater than the
   * value of property field <code>right</code> or equal to it.
   */
  Predicate ge(PropertyField<A> right);

  /**
   * Create a predicate that holds true if the field exists.
   */
  Predicate exists();

  /**
   * Create a predicate that holds true if the field does not exist.
   */
  Predicate notExists();

  /**
   * Create a descending order specifier for the field.
   */
  Order desc();

  /**
   * Create an ascending order specifier for the field.
   */
  Order asc();
}
