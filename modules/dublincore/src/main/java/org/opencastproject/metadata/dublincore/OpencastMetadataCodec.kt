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

import com.entwinemedia.fn.Fn

import java.util.Date
import javax.annotation.ParametersAreNonnullByDefault

/**
 * Collection of metadata encoding functions following Opencast rules.
 */
@ParametersAreNonnullByDefault
object OpencastMetadataCodec {

    /** [OpencastMetadataCodec.decodeDate] as a function.  */
    val decodeDate: Fn<String, Date> = object : Fn<String, Date>() {
        override fun apply(a: String): Date {
            return decodeDate(a)
        }
    }

    /** [OpencastMetadataCodec.decodeDuration] as a function.  */
    val decodeDuration: Fn<String, Long> = object : Fn<String, Long>() {
        override fun apply(a: String): Long {
            return decodeDuration(a)
        }
    }

    /** [OpencastMetadataCodec.decodeTemporal] as a function.  */
    val decodeTemporal: Fn<DublinCoreValue, Temporal> = object : Fn<DublinCoreValue, Temporal>() {
        override fun apply(a: DublinCoreValue): Temporal {
            return decodeTemporal(a)
        }
    }

    /** Encode a date with day precision.  */
    fun encodeDate(date: Date): DublinCoreValue {
        return EncodingSchemeUtils.encodeDate(date, Precision.Day)
    }

    /** Encode a date with a given precision.  */
    fun encodeDate(date: Date, p: Precision): DublinCoreValue {
        return EncodingSchemeUtils.encodeDate(date, p)
    }

    /** Decode a W3C-DTF encoded date.  */
    fun decodeDate(date: String): Date {
        return EncodingSchemeUtils.decodeMandatoryDate(date)
    }

    /** Encode a duration.  */
    fun encodeDuration(ms: Long): DublinCoreValue {
        return EncodingSchemeUtils.encodeDuration(ms)
    }

    /** Decode a duration.  */
    fun decodeDuration(ms: String): Long {
        return EncodingSchemeUtils.decodeMandatoryDuration(ms)!!
    }

    /** Encode a period with a given precision.  */
    fun encodePeriod(from: Date, to: Date, precision: Precision): DublinCoreValue {
        return EncodingSchemeUtils.encodePeriod(DCMIPeriod(from, to), precision)
    }

    /** Decode a period.  */
    fun decodePeriod(period: String): DCMIPeriod {
        return EncodingSchemeUtils.decodeMandatoryPeriod(period)
    }

    /** Decode a temporal value.  */
    fun decodeTemporal(temporal: DublinCoreValue): Temporal {
        return EncodingSchemeUtils.decodeMandatoryTemporal(temporal)
    }

    private fun toStr(s: String): String {
        return s ?: ""
    }
}
