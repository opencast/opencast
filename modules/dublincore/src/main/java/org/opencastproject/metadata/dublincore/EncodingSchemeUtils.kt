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

import org.opencastproject.util.data.Option.option

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import com.entwinemedia.fn.data.Opt

import org.joda.time.Duration
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.ISOPeriodFormat
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeDate
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeDuration
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodePeriod

import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.TimeZone
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility class to facilitate the work with DCMI encoding schemes.
 */
object EncodingSchemeUtils {

    private val formats = HashMap<Precision, String>()

    /** [.decodeDate] as a function.  */
    val dcValueToDate: Function<DublinCoreValue, Option<Date>> = object : Function<DublinCoreValue, Option<Date>>() {
        override fun apply(dublinCoreValue: DublinCoreValue): Option<Date> {
            return option(decodeDate(dublinCoreValue))
        }
    }

    /** [.decodeDate] as a function.  */
    val stringToDate: Function<String, Option<Date>> = object : Function<String, Option<Date>>() {
        override fun apply(s: String): Option<Date> {
            return option(decodeDate(s))
        }
    }

    private val DCMI_PERIOD = Pattern.compile("(start|end|name)\\s*=\\s*(.*?)(?:;|\\s*$)")
    private val DCMI_PERIOD_SCHEME = Pattern.compile("scheme\\s*=\\s*(.*?)(?:;|\\s*$)")

    init {
        formats[Precision.Year] = "yyyy"
        formats[Precision.Month] = "yyyy-MM"
        formats[Precision.Day] = "yyyy-MM-dd"
        formats[Precision.Minute] = "yyyy-MM-dd'T'HH:mm'Z'"
        formats[Precision.Second] = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        formats[Precision.Fraction] = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    }

    /**
     * Encode a date with the given precision into a Dublin Core string value, using the recommended W3C-DTF scheme. The
     * UTC timezone is used for all precisions from [Precision.Minute] to [Precision.Fraction]. For years,
     * months and days the local timezone is used instead to ensure that the given date enters the DublinCore as is. If
     * UTC was used it may happen that you get the previous or next day, month or year respectively
     *
     *
     * The language of the returned value is [DublinCore.LANGUAGE_UNDEFINED].
     *
     *
     * See [http://www.w3.org/TR/NOTE-datetime](http://www.w3.org/TR/NOTE-datetime) for more information about
     * W3C-DTF.
     *
     * @param date
     * the date to encode
     * @param precision
     * the precision to use
     */
    fun encodeDate(date: Date?, precision: Precision?): DublinCoreValue {
        if (date == null)
            throw IllegalArgumentException("The date must not be null")
        if (precision == null)
            throw IllegalArgumentException("The precision must not be null")

        return DublinCoreValue.mk(formatDate(date, precision), DublinCore.LANGUAGE_UNDEFINED, Opt.some<EName>(DublinCore.ENC_SCHEME_W3CDTF))
    }

    fun formatDate(date: Date?, precision: Precision): String {
        val f = SimpleDateFormat(formats[precision])
        if (precision == Precision.Minute || precision == Precision.Second || precision == Precision.Fraction)
            f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(date)
    }

    /**
     * Encode a period with the given precision into a Dublin Core string value using the recommended DCMI Period scheme.
     * For the usage of the UTC timezone please refer to [.encodeDate] for further information.
     *
     *
     * One of the dates may be null to create an open interval.
     *
     *
     * The language of the returned value is [DublinCore.LANGUAGE_UNDEFINED].
     *
     *
     * See [http://dublincore.org/documents/dcmi-period/](http://dublincore.org/documents/dcmi-period/) for
     * more information about DCMI Period.
     *
     * @param period
     * the period
     * @param precision
     * the precision
     */
    fun encodePeriod(period: DCMIPeriod?, precision: Precision?): DublinCoreValue {
        if (period == null)
            throw IllegalArgumentException("The period must not be null")
        if (precision == null)
            throw IllegalArgumentException("The precision must not be null")

        val b = StringBuilder()
        if (period.hasStart()) {
            b.append("start=").append(formatDate(period.start, precision)).append(";")
        }
        if (period.hasEnd()) {
            if (b.length > 0)
                b.append(" ")
            b.append("end=").append(formatDate(period.end, precision)).append(";")
        }
        if (period.hasName()) {
            b.append(" ").append("name=").append(period.name!!.replace(";", "")).append(";")
        }
        b.append(" ").append("scheme=W3C-DTF;")
        return DublinCoreValue.mk(b.toString(), DublinCore.LANGUAGE_UNDEFINED, Opt.some<EName>(DublinCore.ENC_SCHEME_PERIOD))
    }

    /**
     * Encode a duration measured in milliseconds into a Dublin Core string using the
     * [DublinCore.ENC_SCHEME_ISO8601] encoding scheme `PTnHnMnS`.
     *
     *
     * The language of the returned value is [DublinCore.LANGUAGE_UNDEFINED].
     *
     *
     * See [ ISO8601 Durations](http://en.wikipedia.org/wiki/ISO_8601#Durations) for details.
     *
     * @param duration
     * the duration in milliseconds
     */
    fun encodeDuration(duration: Long): DublinCoreValue {
        return DublinCoreValue.mk(ISOPeriodFormat.standard().print(Duration(duration).toPeriod()),
                DublinCore.LANGUAGE_UNDEFINED, Opt.some<EName>(DublinCore.ENC_SCHEME_ISO8601))
    }

    /**
     * Decode a string encoded in the ISO8601 encoding scheme.
     *
     *
     * Also supports the REPLAY legacy format `hh:mm:ss`.
     *
     *
     * See [ ISO8601 Durations](http://en.wikipedia.org/wiki/ISO_8601#Durations) for details.
     *
     * @param value
     * the ISO encoded string
     * @return the duration in milliseconds or null, if the value cannot be parsed
     */
    fun decodeDuration(value: String): Long? {
        try {
            return ISOPeriodFormat.standard().parsePeriod(value).toStandardDuration().millis
        } catch (ignore: IllegalArgumentException) {
        }

        // also support the legacy format hh:mm:ss
        val parts = value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        try {
            if (parts.size == 1)
                return java.lang.Long.parseLong(parts[0]) * 1000
            if (parts.size == 2)
                return java.lang.Long.parseLong(parts[0]) * 1000 * 60 + java.lang.Long.parseLong(parts[1]) * 1000
            if (parts.size == 3)
                return (java.lang.Long.parseLong(parts[0]) * 1000 * 60 * 60 + java.lang.Long.parseLong(parts[1]) * 1000 * 60
                        + java.lang.Long.parseLong(parts[2]) * 1000)
        } catch (ignore: NumberFormatException) {
        }

        return null
    }

    /**
     * Decode a string encoded in the ISO8601 encoding scheme.
     *
     * @param value
     * the Dublin Core value
     * @return the duration in milliseconds or null, if the value cannot be parsed or is in a different encoding scheme
     */
    fun decodeDuration(value: DublinCoreValue): Long? {
        return if (!value.hasEncodingScheme() || value.encodingScheme.get() == DublinCore.ENC_SCHEME_ISO8601) {
            decodeDuration(value.value)
        } else null
    }

    fun decodeMandatoryDuration(value: DublinCoreValue): Long? {
        return decodeDuration(value) ?: throw IllegalArgumentException("Cannot decode duration: $value")
    }

    fun decodeMandatoryDuration(value: String): Long? {
        return decodeDuration(value) ?: throw IllegalArgumentException("Cannot decode duration: $value")
    }

    /**
     * Tries to decode the given value as a W3C-DTF encoded date. If decoding fails, null is returned.
     *
     * @return the date or null if decoding fails
     */
    fun decodeDate(value: DublinCoreValue): Date? {
        if (!value.hasEncodingScheme() || value.encodingScheme.get() == DublinCore.ENC_SCHEME_W3CDTF) {
            try {
                return parseW3CDTF(value.value)
            } catch (ignore: IllegalArgumentException) {
            }

        }

        // Try unixtime in milliseconds (backwards-compatibility with older mediapackages)
        try {
            val timestamp = java.lang.Long.parseLong(value.value)
            return Date(timestamp)
        } catch (nfe: NumberFormatException) {
        }

        return null
    }

    /**
     * Tries to decode the given value as a W3C-DTF encoded date. If decoding fails, null is returned.
     *
     * @return the date or null if decoding fails
     */
    fun decodeDate(value: String): Date? {
        try {
            return parseW3CDTF(value)
        } catch (ignore: IllegalArgumentException) {
        }

        // Try unixtime in milliseconds (backwards-compatibility with older mediapackages)
        try {
            val timestamp = java.lang.Long.parseLong(value)
            return Date(timestamp)
        } catch (nfe: NumberFormatException) {
        }

        return null
    }

    /**
     * Like [.decodeDate], but throws an [IllegalArgumentException] if the value cannot be decoded.
     *
     * @param value
     * the value
     * @return the date
     * @throws IllegalArgumentException
     * if the value cannot be decoded
     */
    fun decodeMandatoryDate(value: DublinCoreValue): Date {
        return decodeDate(value) ?: throw IllegalArgumentException("Cannot decode to Date: $value")
    }

    /**
     * Like [.decodeDate], but throws an [IllegalArgumentException] if the value cannot be decoded.
     *
     * @return the date
     * @throws IllegalArgumentException
     * if the value cannot be decoded
     */
    fun decodeMandatoryDate(value: String): Date {
        return decodeDate(value) ?: throw IllegalArgumentException("Cannot decode to Date: $value")
    }

    /**
     * Tries to decode a string in the DCMI period format, using W3C-DTF for the encoding of the individual dates. If
     * parsing fails at any point, null will be returned.
     *
     * @return the period or null if decoding fails
     */
    fun decodePeriod(value: DublinCoreValue): DCMIPeriod? {
        return decodePeriod(value.value)
    }

    /**
     * Tries to decode a string in the DCMI period format, using W3C-DTF for the encoding of the individual dates. If
     * parsing fails at any point, null will be returned.
     *
     * @return the period or null if decoding fails
     */
    fun decodePeriod(value: String): DCMIPeriod? {
        // Parse value
        val schemeMatcher = DCMI_PERIOD_SCHEME.matcher(value)
        var mayBeW3CDTFEncoded = true
        if (schemeMatcher.find()) {
            val schemeString = schemeMatcher.group(1)
            if (!"W3C-DTF".equals(schemeString, ignoreCase = true) && !"W3CDTF".equals(schemeString, ignoreCase = true)) {
                mayBeW3CDTFEncoded = false
            }
        }
        try {
            if (mayBeW3CDTFEncoded) {
                // Declare fields
                var start: Date? = null
                var end: Date? = null
                var name: String? = null
                // Parse
                val m = DCMI_PERIOD.matcher(value)
                while (m.find()) {
                    val field = m.group(1)
                    val fieldValue = m.group(2)
                    if ("start" == field) {
                        if (start != null)
                            return null
                        start = parseW3CDTF(fieldValue)
                    } else if ("end" == field) {
                        if (end != null)
                            return null
                        end = parseW3CDTF(fieldValue)
                    } else if ("name" == field) {
                        if (name != null)
                            return null
                        name = fieldValue
                    }
                }
                return if (start == null && end == null) null else DCMIPeriod(start, end, name)
            }
        } catch (ignore: IllegalArgumentException) {
            // Parse error
        }

        return null
    }

    /**
     * Like [.decodePeriod], but throws an [IllegalArgumentException] if the value cannot be decoded.
     *
     * @return the period
     * @throws IllegalArgumentException
     * if the value cannot be decoded
     */
    fun decodeMandatoryPeriod(value: DublinCoreValue): DCMIPeriod {
        return decodeMandatoryPeriod(value.value)
    }

    /**
     * Like [.decodePeriod], but throws an [IllegalArgumentException] if the value cannot be
     * decoded.
     *
     * @return the period
     * @throws IllegalArgumentException
     * if the value cannot be decoded
     */
    fun decodeMandatoryPeriod(value: String): DCMIPeriod {

        return decodePeriod(value) ?: throw IllegalArgumentException("Cannot decode to DCMIPeriod: $value")
    }

    /**
     * Tries to decode the value to a temporal object. For now, supported types are [java.util.Date],
     * [DCMIPeriod] and Long for a duration.
     *
     * @param value
     * the value to decode
     * @return a temporal object of the said types or null if decoding fails
     */
    fun decodeTemporal(value: DublinCoreValue?): Temporal? {
        // First try Date
        val instant = decodeDate(value!!)
        if (instant != null)
            return Temporal.instant(instant)
        val period = decodePeriod(value)
        if (period != null)
            return Temporal.period(period)
        val duration = decodeDuration(value)
        return if (duration != null) Temporal.duration(duration) else null
    }

    /**
     * Like [.decodeTemporal], but throws an [IllegalArgumentException] if the value cannot
     * be decoded.
     *
     * @return the temporal object of type [java.util.Date] or [DCMIPeriod]
     * @throws IllegalArgumentException
     * if the value cannot be decoded
     */
    fun decodeMandatoryTemporal(value: DublinCoreValue?): Temporal? {
        val temporal = decodeTemporal(value)
        if (value == null)
            throw IllegalArgumentException("Cannot decode to either Date or DCMIPeriod: " + value!!)

        return temporal
    }

    /**
     * @throws IllegalArgumentException
     * if the value cannot be parsed
     */
    private fun parseW3CDTF(value: String): Date {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(value).toDate()
    }

}
/** Disable construction of this utility class  */
