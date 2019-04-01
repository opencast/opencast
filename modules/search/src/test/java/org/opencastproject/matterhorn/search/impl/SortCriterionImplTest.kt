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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import org.opencastproject.matterhorn.search.SearchQuery.Order
import org.opencastproject.matterhorn.search.SortCriterion

import org.junit.Test

/**
 * Test cases for [SortCriterionImpl]
 */
class SortCriterionImplTest {

    /** Test method for [SortCriterionImpl.getFieldName]  */
    @Test
    fun testGetFieldName() {
        assertEquals("name", orderByNameAsc.fieldName)
        assertEquals("date", orderByDateAsc.fieldName)
    }

    /** Test method for [SortCriterionImpl.getOrder]  */
    @Test
    fun testGetOrder() {
        assertEquals(Order.Ascending, orderByNameAsc.order)
        assertEquals(Order.Descending, orderByNameDesc.order)
    }

    /** Test method for [SortCriterionImpl.parse]  */
    @Test
    fun testParse() {
        assertEquals(orderByNameAsc, SortCriterionImpl.parse("name:ASC"))
        assertEquals(orderByDateDesc, SortCriterionImpl.parse("date:DESC"))
    }

    /** Test method for [SortCriterionImpl.parse]  */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testParseWithTooManyParts() {
        SortCriterionImpl.parse("name:ASC:first")
    }

    /** Test method for [SortCriterionImpl.parse]  */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testParseWithOnlyOnePart() {
        SortCriterionImpl.parse("name:")
    }

    /** Test method for [SortCriterionImpl.parse]  */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testParseWithInvalidDirection() {
        SortCriterionImpl.parse("name:ASCDESC")
    }

    /** Test method for [SortCriterionImpl.equals]  */
    @Test
    fun testEquals() {
        assertTrue(orderByNameAsc == SortCriterionImpl("name", Order.Ascending))
    }

    /** Test method for [SortCriterionImpl.hashCode]  */
    @Test
    fun testHashCode() {
        assertEquals(orderByNameAsc.hashCode().toLong(), SortCriterionImpl("name", Order.Ascending).hashCode().toLong())
    }

    /** Test method for [SortCriterionImpl.toString]  */
    @Test
    fun testToString() {
        assertEquals("name:ASC", orderByNameAsc.toString())
        assertEquals("date:DESC", orderByDateDesc.toString())
    }

    companion object {

        private val orderByNameAsc = SortCriterionImpl("name", Order.Ascending)
        private val orderByNameDesc = SortCriterionImpl("name", Order.Descending)
        private val orderByDateAsc = SortCriterionImpl("date", Order.Ascending)
        private val orderByDateDesc = SortCriterionImpl("date", Order.Descending)
    }

}
