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

package org.opencastproject.caption.util

import org.opencastproject.caption.api.IllegalTimeFormatException
import org.opencastproject.caption.api.Time
import org.opencastproject.caption.impl.TimeImpl

/**
 * Auxiliary class that contains methods for converting from and to specific time formats.
 *
 */
object TimeUtil {

    // time format regular expressions
    private val SRT_FORMAT = "[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{1,3}"
    private val DFXP_FORMAT_1 = "[0-9]{1,2}:[0-9]{2}:[0-9]{2}(.[0-9]+)?"

    /**
     * Parse String representation of SubRip time format.
     *
     * @param timeSrt
     * SubRip time format
     * @return parsed [Time] instance
     * @throws IllegalTimeFormatException
     * if argument is not SubRip time format
     */
    @Throws(IllegalTimeFormatException::class)
    fun importSrt(timeSrt: String): Time {
        if (!timeSrt.matches(SRT_FORMAT.toRegex())) {
            throw IllegalTimeFormatException("$timeSrt does not appear to valid SubRip time format.")
        }

        val timeParts = timeSrt.split("[,:]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val hour = Integer.parseInt(timeParts[0])
        val minute = Integer.parseInt(timeParts[1])
        val second = Integer.parseInt(timeParts[2])
        val milisecond = Integer.parseInt(timeParts[3])
        return TimeImpl(hour, minute, second, milisecond)
    }

    /**
     * Exports [Time] instance to the SubRip time format representation.
     *
     * @param time
     * [Time] instance to be exported
     * @return time exported to SubRip time format
     */
    fun exportToSrt(time: Time): String {
        return String.format("%02d:%02d:%02d,%03d", time.hours, time.minutes, time.seconds,
                time.milliseconds)
    }

    /**
     * Exports [Time] instance to the WebVTT time format representation.
     *
     * @param time
     * [Time] instance to be exported
     * @return time exported to WebVTT time format
     */
    fun exportToVtt(time: Time): String {
        return String.format("%02d:%02d:%02d.%03d", time.hours, time.minutes, time.seconds,
                time.milliseconds)
    }

    // DFXP TT time format

    /**
     * Parse String representation of DFXP time format. It does not support parsing of metrics (for example: 34.567s).
     *
     * @param timeDfxp
     * DFXP time format
     * @return parsed [Time] instance
     * @throws IllegalTimeFormatException
     * if argument is not DFXP time format or is not supported.
     */
    @Throws(IllegalTimeFormatException::class)
    fun importDFXP(timeDfxp: String): Time {
        if (!timeDfxp.matches(DFXP_FORMAT_1.toRegex())) {
            throw IllegalTimeFormatException("$timeDfxp is not valid DFXP time format or is not supported.")
        }

        // split
        val timeArray = timeDfxp.split("[\\.:]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val hour = Integer.parseInt(timeArray[0])
        val minute = Integer.parseInt(timeArray[1])
        val second = Integer.parseInt(timeArray[2])
        var millisecond = 0
        if (timeArray.size == 4) {
            // parse milliseconds
            if (timeArray[3].length == 1) {
                millisecond = Integer.parseInt(timeArray[3]) * 100
            } else if (timeArray[3].length == 2) {
                millisecond = Integer.parseInt(timeArray[3]) * 10
            } else if (timeArray[3].length == 3) {
                millisecond = Integer.parseInt(timeArray[3])
            } else {
                // more numbers - ignore the rest
                millisecond = Integer.parseInt(timeArray[3].trim { it <= ' ' }.substring(0, 4))
            }
        }

        return TimeImpl(hour, minute, second, millisecond)
    }

    /**
     * Exports [Time] instance to the DFXP time format representation. Specifically time format used is 0:00:00.000
     *
     * @param time
     * [Time] instance to be exported
     * @return time exported to DFXP time format
     */
    fun exportToDFXP(time: Time): String {
        return String.format("%d:%02d:%02d.%03d", time.hours, time.minutes, time.seconds,
                time.milliseconds)
    }
}// SRT time format functions
/**
 * Private constructor to prevent instantiation of this utility class.
 */// Nothing to be done
