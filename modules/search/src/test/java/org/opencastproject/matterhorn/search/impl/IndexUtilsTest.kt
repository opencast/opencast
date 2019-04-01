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

import org.junit.Test

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Test case for [IndexUtils].
 */
class IndexUtilsTest {

    /** The date format  */
    protected val df: DateFormat = SimpleDateFormat("yyyy-MM-dd")

    /**
     * Test method for [org.opencastproject.matterhorn.search.impl.IndexUtils.clean].
     */
    @Test
    fun testClean() {
        val test = "+-!(){}[]^\"~*?:&&||&|"
        val expected = "\\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\&\\&\\|\\|&|"
        assertEquals(expected, IndexUtils.clean(test))
    }

    /**
     * Test method for [org.opencastproject.matterhorn.search.impl.IndexUtils.serializeDate].
     */
    @Test
    fun testSerializeDate() {
        val d = Calendar.getInstance()
        d.set(Calendar.DAY_OF_MONTH, 2)
        d.set(Calendar.HOUR, 5)
        d.set(Calendar.HOUR_OF_DAY, 5)
        d.set(Calendar.MINUTE, 59)
        d.set(Calendar.SECOND, 13)
        d.set(Calendar.MILLISECOND, 0)
        val date = d.time
        val serializedDate = df.format(date) + "T05:59:13Z"
        assertEquals(serializedDate, IndexUtils.serializeDate(date))
    }

    /**
     * Test method for [org.opencastproject.matterhorn.search.impl.IndexUtils.serializeDateRange].
     */
    @Test
    fun testSerializeDateRange() {
        val d = Calendar.getInstance()
        d.set(Calendar.MILLISECOND, 0)
        d.set(Calendar.SECOND, 0)
        d.set(Calendar.MINUTE, 0)
        d.set(Calendar.HOUR_OF_DAY, 0)
        val startDate = d.time
        d.add(Calendar.DAY_OF_MONTH, 2)
        d.set(Calendar.HOUR_OF_DAY, 5)
        d.set(Calendar.MINUTE, 59)
        val endDate = d.time
        val serializedStartDate = df.format(startDate) + "T00:00:00Z"
        val serializedEndDate = df.format(endDate) + "T05:59:00Z"
        val day = "[$serializedStartDate TO $serializedEndDate]"
        assertEquals(day, IndexUtils.serializeDateRange(startDate, endDate))
    }

}
