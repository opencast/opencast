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


package org.opencastproject.metadata.dublincore

import java.util.Date

/**
 * A time related algebraic data type.
 */
abstract class Temporal// prohibit inheritance from outside this class
private constructor() {

    /**
     * Safe decomposition.
     */
    abstract fun <A> fold(v: Match<A>): A

    /**
     * Safe temporal decomposition.
     */
    interface Match<A> {
        fun period(period: DCMIPeriod?): A

        fun instant(instant: Date?): A

        fun duration(duration: Long): A
    }

    companion object {

        /**
         * An instant in time.
         */
        fun instant(instant: Date?): Temporal {
            if (instant == null)
                throw IllegalArgumentException("instant must not be null")
            return object : Temporal() {
                val instant: Date
                    get() = instant

                override fun <A> fold(v: Match<A>): A {
                    return v.instant(instant)
                }
            }
        }

        /**
         * A period in time limited by at least one instant.
         */
        fun period(period: DCMIPeriod?): Temporal {
            if (period == null)
                throw IllegalArgumentException("period must not be null")
            return object : Temporal() {
                val period: DCMIPeriod
                    get() = period

                override fun <A> fold(v: Match<A>): A {
                    return v.period(period)
                }
            }
        }

        /**
         * A time span measured in milliseconds.
         */
        fun duration(duration: Long): Temporal {
            if (duration < 0)
                throw IllegalArgumentException("duration must be positive or zero")
            return object : Temporal() {
                val duration: Long
                    get() = duration

                override fun <A> fold(v: Match<A>): A {
                    return v.duration(duration)
                }
            }
        }
    }

}
