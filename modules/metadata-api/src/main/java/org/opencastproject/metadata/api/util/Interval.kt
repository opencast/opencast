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


package org.opencastproject.metadata.api.util

import java.util.Date

/**
 * An interval in time, possibly unbounded on one end.
 * The interval is described by its bounds which are absolutely placed instants in time with a precision
 * defined by [java.util.Date].
 */
abstract class Interval private constructor() {

    /**
     * Test if both endpoints are not infinite.
     */
    val isBounded: Boolean
        get() = !(isLeftInfinite || isRightInfinite)

    /**
     * Test if the right endpoint is infinite.
     */
    abstract val isLeftInfinite: Boolean

    /**
     * Test if the left endpoint is infinite.
     */
    abstract val isRightInfinite: Boolean

    /**
     * Safe decomposition.
     */
    abstract fun <A> fold(visitor: Match<A>): A

    interface Match<A> {
        fun bounded(leftBound: Date, rightBound: Date): A

        fun leftInfinite(rightBound: Date): A

        fun rightInfinite(leftBound: Date): A
    }

    companion object {

        /**
         * Constructor function for a bounded interval.
         */
        fun boundedInterval(leftBound: Date, rightBound: Date): Interval {
            return object : Interval() {
                override val isLeftInfinite: Boolean
                    get() = false

                override val isRightInfinite: Boolean
                    get() = false

                override fun <A> fold(visitor: Match<A>): A {
                    return visitor.bounded(leftBound, rightBound)
                }
            }
        }

        /**
         * Constructor function for a right bounded interval, i.e. an interval with an infinite left endpoint.
         */
        fun rightBoundedInterval(rightBound: Date): Interval {
            return object : Interval() {
                override val isLeftInfinite: Boolean
                    get() = true

                override val isRightInfinite: Boolean
                    get() = false

                override fun <A> fold(visitor: Match<A>): A {
                    return visitor.leftInfinite(rightBound)
                }
            }
        }

        /**
         * Constructor function for a left bounded interval, i.e. an interval with an infinite right endpoint.
         */
        fun leftBoundedInterval(leftBound: Date): Interval {
            return object : Interval() {
                override val isLeftInfinite: Boolean
                    get() = false

                override val isRightInfinite: Boolean
                    get() = true

                override fun <A> fold(visitor: Match<A>): A {
                    return visitor.rightInfinite(leftBound)
                }
            }
        }

        /**
         * Create an interval from two dates. One of the dates may be null to indicate an infinite endpoint.
         *
         * @throws IllegalArgumentException
         * if boths bounds are null
         */
        fun fromValues(leftBound: Date?, rightBound: Date?): Interval {
            if (leftBound != null && rightBound != null)
                return boundedInterval(leftBound, rightBound)
            if (leftBound != null)
                return leftBoundedInterval(leftBound)
            if (rightBound != null)
                return rightBoundedInterval(rightBound)
            throw IllegalArgumentException("Please give at least one bound")
        }
    }

}
