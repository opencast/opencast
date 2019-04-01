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


package org.opencast.metadata.api.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.opencastproject.metadata.api.util.Interval.boundedInterval
import org.opencastproject.metadata.api.util.Interval.leftBoundedInterval
import org.opencastproject.metadata.api.util.Interval.rightBoundedInterval

import org.opencastproject.metadata.api.util.Interval

import org.junit.Test

import java.util.Date

class IntervalTest {

    @Test
    fun testInterval() {
        val closed = boundedInterval(Date(), Date())
        assertTrue(closed.isBounded)
        assertFalse(closed.isLeftInfinite)
        assertFalse(closed.isRightInfinite)
        val visitor = object : Interval.Match<Int> {
            override fun bounded(leftBound: Date, rightBound: Date): Int {
                return 1
            }

            override fun leftInfinite(rightBound: Date): Int {
                return 2
            }

            override fun rightInfinite(leftBound: Date): Int {
                return 3
            }
        }
        assertSame(1, closed.fold(visitor))
        val leftOpen = rightBoundedInterval(Date())
        assertFalse(leftOpen.isBounded)
        assertTrue(leftOpen.isLeftInfinite)
        assertFalse(leftOpen.isRightInfinite)
        assertSame(2, leftOpen.fold(visitor))
        val rightOpen = leftBoundedInterval(Date())
        assertFalse(rightOpen.isBounded)
        assertFalse(rightOpen.isLeftInfinite)
        assertTrue(rightOpen.isRightInfinite)
        assertSame(3, rightOpen.fold(visitor))
    }
}
