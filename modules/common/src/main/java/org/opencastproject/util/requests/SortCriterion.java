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


package org.opencastproject.util.requests;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * A sort criterion represents the combination of a field name and a sort {@link Order}
 */
public final class SortCriterion {

  /**
   * Sort order definitions.
   */
  public enum Order {
    None, Ascending, Descending
  }

  private final String fieldName;
  private final Order order;

  /**
   * Parse a string representation of a sort criterion.
   *
   * @param sortCriterion
   *          the sort criterion string
   * @return the sort criterion
   */
  public static SortCriterion parse(final String sortCriterion) {
    requireNonNull(sortCriterion);

    String[] parts = sortCriterion.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException("sortOrder must be of form <field name>:ASC/DESC");
    }

    if ("ASC".equalsIgnoreCase(parts[1]) || "Ascending".equalsIgnoreCase(parts[1])) {
      return new SortCriterion(parts[0].trim(), Order.Ascending);
    }
    if ("DESC".equalsIgnoreCase(parts[1]) || "Descending".equalsIgnoreCase(parts[1])) {
      return new SortCriterion(parts[0].trim(), Order.Descending);
    }

    throw new IllegalArgumentException("Invalid order " + parts[1]);
  }

  /**
   * Create a order criterion based on the given field name and order.
   *
   * @param fieldName
   *          the field name
   * @param order
   *          the order
   */
  public SortCriterion(String fieldName, Order order) {
    this.fieldName = fieldName;
    this.order = order;
  }

  public String getFieldName() {
    return fieldName;
  }

  public Order getOrder() {
    return order;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof SortCriterion)) {
      return false;
    }

    SortCriterion that = (SortCriterion) o;
    return Objects.equals(this.fieldName, that.fieldName) && Objects.equals(this.order, that.order);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, order);
  }

  @Override
  public String toString() {
    if (order.equals(Order.Ascending)) {
      return fieldName + ":ASC";
    }
    if (order.equals(Order.Descending)) {
      return fieldName + ":DESC";
    }
    return fieldName + ":NONE";
  }

}
