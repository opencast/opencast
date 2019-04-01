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

package org.opencastproject.util

import org.opencastproject.util.data.functions.Strings.format
import org.opencastproject.util.data.functions.Strings.trimToNone

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.apache.commons.lang3.StringUtils

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Utility class for the solr database.
 */
object SolrUtils {

    /** The regular filter expression for single characters  */
    private val charCleanerRegex = "([\\+\\-\\!\\(\\)\\{\\}\\[\\]\\\\^\"\\~\\*\\?\\:])"

    /** [.clean] as a function. Return none if string is empty after cleaning.  */
    val clean: Function<String, Option<String>> = object : Function<String, Option<String>>() {
        override fun apply(s: String): Option<String> {
            return trimToNone(clean(s))
        }
    }

    /** [.serializeDate] as a function.  */
    val serializeDate: Function<Date, String> = object : Function<Date, String>() {
        override fun apply(date: Date): String? {
            return serializeDate(date)
        }
    }

    /**
     * Clean up the user query input string to avoid invalid input parameters.
     *
     * @param q
     * The input String.
     * @return The cleaned string.
     */
    fun clean(q: String): String {
        return q.replace(charCleanerRegex.toRegex(), "\\\\$1")
                .replace("\\&\\&".toRegex(), "\\\\&\\\\&")
                .replace("\\|\\|".toRegex(), "\\\\|\\\\|")
    }

    /**
     * Returns a serialized version of the date or `null` if `null` was passed in for the date.
     *
     * @param date
     * the date
     * @return the serialized date
     */
    fun serializeDate(date: Date?): String? {
        return if (date == null) null else newSolrDateFormat().format(date)
    }

    /**
     * Returns the date or `null` if `null` was passed in for the date.
     *
     * @param date
     * the serialized date in UTC format yyyy-MM-dd'T'HH:mm:ss'Z'
     * @return the date
     * @throws ParseException
     * if parsing the date fails
     */
    @Throws(ParseException::class)
    fun parseDate(date: String): Date? {
        return if (StringUtils.isBlank(date)) null else newSolrDateFormat().parse(date)
    }

    /**
     * Returns an expression to search for any date that lies in between `startDate` and `endDate`.
     *
     * @param startDate
     * the start date or none for an infinite left endpoint, "*" in solr query syntax
     * @param endDate
     * the end date or none for an infinite right endpoint, "*" in solr query syntax
     * @return the serialized search expression
     */
    fun serializeDateRange(startDate: Option<Date>, endDate: Option<Date>): String {
        val f = format<Date>(newSolrDateFormat())
        return StringBuilder("[")
                .append(startDate.map(f).getOrElse("*"))
                .append(" TO ")
                .append(endDate.map(f).getOrElse("*"))
                .append("]")
                .toString()
    }

    /**
     * Return a date format suitable for solr. Format a date as UTC with a granularity of seconds.
     * `yyyy-MM-dd'T'HH:mm:ss'Z'`
     */
    fun newSolrDateFormat(): DateFormat {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f
    }

}
/** Disallow construction of this utility class  */
