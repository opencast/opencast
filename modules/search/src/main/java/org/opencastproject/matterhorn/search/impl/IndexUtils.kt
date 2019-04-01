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

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Utility class for the solr database.
 */
object IndexUtils {

    /** The date format  */
    internal val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    /** The solr supported date format.  */
    internal var dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT)

    /** The solr supported date format for days  */
    internal var dayFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")

    /** The regular filter expression for single characters  */
    private val charCleanerRegex = "([\\+\\-\\!\\(\\)\\{\\}\\[\\]\\\\^\"\\~\\*\\?\\:])"

    /**
     * Clean up the user query input string to avoid invalid input parameters.
     *
     * @param q
     * The input String.
     * @return The cleaned string.
     */
    fun clean(q: String): String {
        var q = q
        q = q.replace(charCleanerRegex.toRegex(), "\\\\$1")
        q = q.replace("\\&\\&".toRegex(), "\\\\&\\\\&")
        q = q.replace("\\|\\|".toRegex(), "\\\\|\\\\|")
        return q
    }

    /**
     * Returns a serialized version of the date or `null` if
     * `null` was passed in for the date.
     *
     * @param date
     * the date
     * @return the serialized date
     */
    fun serializeDate(date: Date?): String? {
        return if (date == null) null else dateFormat.format(date)
    }

    /**
     * Returns an expression to search for any date that lies in between
     * `startDate` and `endDate`.
     *
     * @param startDate
     * the start date
     * @param endDate
     * the end date
     * @return the serialized search expression
     */
    fun serializeDateRange(startDate: Date?, endDate: Date?): String {
        if (startDate == null)
            throw IllegalArgumentException("Start date cannot be null")
        if (endDate == null)
            throw IllegalArgumentException("End date cannot be null")
        val buf = StringBuffer("[")
        buf.append(dateFormat.format(startDate))
        buf.append(" TO ")
        buf.append(dateFormat.format(endDate))
        buf.append("]")
        return buf.toString()
    }

    /**
     * Returns the date with all time related fields set to the start of the day.
     *
     * @param date
     * the date
     * @return the date with its time component set to the beginning of the day
     */
    fun beginningOfDay(date: Date?): Date? {
        if (date == null)
            return null
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    /**
     * Returns the date with all time related fields set to the end of the day.
     *
     * @param date
     * the date
     * @return the date with its time component set to the beginning of the day
     */
    fun endOfDay(date: Date?): Date? {
        if (date == null)
            return null
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 99)
        return c.time
    }

}
/**
 * Utility classes should not be initialized.
 */
