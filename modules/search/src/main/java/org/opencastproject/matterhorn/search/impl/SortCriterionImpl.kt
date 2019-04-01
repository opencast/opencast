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


package org.opencastproject.matterhorn.search.impl

import java.util.Objects.requireNonNull

import org.opencastproject.matterhorn.search.SearchQuery.Order
import org.opencastproject.matterhorn.search.SortCriterion

import java.util.Objects

/**
 * A sort criterion represents the combination of a field name and a sort [Order]
 */
class SortCriterionImpl
/**
 * Create a order criterion based on the given field name and order.
 *
 * @param fieldName
 * the field name
 * @param order
 * the order
 */
(private val fieldName: String, private val order: Order) : SortCriterion {

    override fun getFieldName(): String {
        return fieldName
    }

    override fun getOrder(): Order {
        return order
    }

    override fun equals(o: Any?): Boolean {
        if (o === this)
            return true

        if (o !is SortCriterionImpl)
            return false

        val that = o as SortCriterionImpl?
        return this.fieldName == that!!.fieldName && this.order == that.order
    }

    override fun hashCode(): Int {
        return Objects.hash(fieldName, order)
    }

    override fun toString(): String {
        if (order == Order.Ascending)
            return "$fieldName:ASC"
        return if (order == Order.Descending) "$fieldName:DESC" else "$fieldName:NONE"
    }

    companion object {

        /**
         * Parse a string representation of a sort criterion.
         *
         * @param sortCriterion
         * the sort criterion string
         * @return the sort criterion
         */
        fun parse(sortCriterion: String): SortCriterionImpl {
            requireNonNull(sortCriterion)

            val parts = sortCriterion.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 2)
                throw IllegalArgumentException("sortOrder must be of form <field name>:ASC/DESC")

            if ("ASC".equals(parts[1], ignoreCase = true) || "Ascending".equals(parts[1], ignoreCase = true))
                return SortCriterionImpl(parts[0], Order.Ascending)
            if ("DESC".equals(parts[1], ignoreCase = true) || "Descending".equals(parts[1], ignoreCase = true))
                return SortCriterionImpl(parts[0], Order.Descending)

            throw IllegalArgumentException("Invalid order " + parts[1])
        }
    }

}
