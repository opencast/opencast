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

package org.opencastproject.caption.impl

import org.opencastproject.caption.api.IllegalTimeFormatException
import org.opencastproject.caption.api.Time

/**
 * Implementation of [Time].
 */
class TimeImpl @Throws(IllegalTimeFormatException::class)
constructor(h: Int, m: Int, s: Int, ms: Int) : Time {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.Time.getHours
     */
    /**
     * Checks if hours are inside the boundaries (between 0 and 99 hours).
     *
     * @param h
     * number of hours
     * @throws IllegalTimeFormatException
     * if argument is less than 0 or more than 99.
     */
    override var hours: Int = 0
        @Throws(IllegalTimeFormatException::class)
        private set(h) {
            if (h < 0 || h > 99)
                throw IllegalTimeFormatException("Invalid hour time: $h")
            field = h
        }
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.Time.getMinutes
     */
    /**
     * Checks if minutes are inside the boundaries (between 0 and 59).
     *
     * @param m
     * number of minutes
     * @throws IllegalTimeFormatException
     * if argument is less than 0 or more than 59.
     */
    override var minutes: Int = 0
        @Throws(IllegalTimeFormatException::class)
        private set(m) {
            if (m < 0 || m > 59)
                throw IllegalTimeFormatException("Invalid minute time: $m")
            field = m
        }
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.Time.getSeconds
     */
    /**
     * Checks if seconds are inside the boundaries (between 0 and 59).
     *
     * @param s
     * number of seconds
     * @throws IllegalTimeFormatException
     * if argument is less than 0 or more than 59.
     */
    override var seconds: Int = 0
        @Throws(IllegalTimeFormatException::class)
        private set(s) {
            if (s < 0 || s > 59)
                throw IllegalTimeFormatException("Invalid second time: $s")
            field = s
        }
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.Time.getMilliseconds
     */
    /**
     * Checks if milliseconds are inside the boundaries (between 0 and 999).
     *
     * @param ms
     * number of milliseconds
     * @throws IllegalTimeFormatException
     * if argument is less than 0 or more than 999.
     */
    override var milliseconds: Int = 0
        @Throws(IllegalTimeFormatException::class)
        private set(ms) {
            if (ms < 0 || ms > 999)
                throw IllegalTimeFormatException("Invalid milisecond time: $ms")
            field = ms
        }

    init {
        this.hours = h
        this.minutes = m
        this.seconds = s
        this.milliseconds = ms
    }

    /**
     * @see java.lang.Comparable.compareTo
     */
    override fun compareTo(arg0: Time): Int {
        return getMilliseconds(this) - getMilliseconds(arg0)
    }

    /**
     * Helper function that converts time to milliseconds. Used for time comparing.
     *
     * @param time
     * to be converted
     * @return milliseconds
     */
    private fun getMilliseconds(time: Time): Int {
        return (time.hours * 3600 + time.minutes * 60 + time.seconds) * 1000 + time.milliseconds
    }
}
