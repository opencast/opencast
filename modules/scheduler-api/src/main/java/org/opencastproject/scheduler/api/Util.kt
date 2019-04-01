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

package org.opencastproject.scheduler.api

import net.fortuna.ical4j.model.parameter.Value.DATE_TIME

import net.fortuna.ical4j.model.DateList
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.property.RRule

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.LinkedList
import java.util.TimeZone

object Util {
    /** The minimum separation between one event ending and the next starting  */
    val EVENT_MINIMUM_SEPARATION_MILLISECONDS = 60 * 1000

    private val logger = LoggerFactory.getLogger(Util::class.java)
    private val registry = TimeZoneRegistryFactory.getInstance().createRegistry()


    /**
     * Adjust the given UTC rrule to the given timezone.
     *
     * @param rRule The rrule to adjust.
     * @param start The start date in UTC
     *
     * @param tz The target timezone.
     */
    fun adjustRrule(rRule: RRule, start: Date, tz: TimeZone) {
        val recur = rRule.recur
        if (recur.hourList.size != 1 || recur.minuteList.size != 1) {
            throw IllegalArgumentException("RRules with multiple hours/minutes are not supported by Opencast. $recur")
        }
        val adjustedDate = ZonedDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC)
                .withHour(recur.hourList[0])
                .withMinute(recur.minuteList[0])
                .withZoneSameInstant(tz.toZoneId())
        recur.hourList[0] = adjustedDate.hour
        recur.minuteList[0] = adjustedDate.minute
    }

    /**
     * Backward compatibility, converts parameters as required for the updated calculatePeriods
     *
     * @param start date in the scheduled time zone
     * @param end date in the scheduled time zone
     * @param duration
     * @param rRule
     * @param tz, time zone of the scheduled event
     * @return the calculated periods
     */
    fun calculatePeriods(start: Date, end: Date, duration: Long, rRule: RRule, tz: TimeZone): List<Period> {
        val startCal = Calendar.getInstance(tz)
        val endCal = Calendar.getInstance(tz)
        startCal.time = start
        endCal.time = end
        return Util.calculatePeriods(startCal, endCal, duration, rRule.recur, tz)
    }

    /**
     * Given a start time and end time with a recurrence rule and a timezone, all periods of the recurrence rule are
     * calculated taken daylight saving time into account.
     *
     * @param startCalTz, Calendar date time of the recurrence in the timezone of the scheduled CA
     * @param endCalTz, Calendar date time of the recurrence in the timezone of the scheduled CA
     * @param duration, the length of each event
     * @param recur, the recurrence rule (based on the Start time zone, including start hour and days of week)
     * @param tz, the timezone of the scheduled CA
     * @return a list of event Periods that match the rule and start and end times
     */
    fun calculatePeriods(startCalTz: Calendar, endCalTz: Calendar, duration: Long, recur: Recur, tz: TimeZone): List<Period> {
        val simpleDateFormat = SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy")
        simpleDateFormat.timeZone = tz
        val tzStr = tz.id
        val event = LinkedList<Period>()
        logger.debug("Inbound start of recurrence {} to end of recurrence {}, in Tz {}",
                simpleDateFormat.format(startCalTz.time),
                simpleDateFormat.format(endCalTz.time), tzStr)

        val periodStart = DateTime(startCalTz.time)
        logger.debug("ical4j timeZone for {} is {}", tzStr, registry.getTimeZone(tzStr).toZoneId())
        periodStart.timeZone = registry.getTimeZone(tzStr)
        val periodEnd = DateTime(endCalTz.time)
        periodEnd.timeZone = registry.getTimeZone(tzStr)

        logger.trace("is utc {}? Tz is {} ", periodStart.isUtc, periodStart.timeZone.toZoneId())
        logger.debug("({}) Looking at recurrences for {} to {}, duration {}, {}", periodStart.timeZone.toZoneId(),
                simpleDateFormat.format(Date(periodStart.time)),
                simpleDateFormat.format(Date(periodEnd.time)), duration, recur.toString())

        val dates = recur.getDates(periodStart, periodEnd, DATE_TIME)
        logger.trace("Got {} dates: {}, tz '{}'", dates.size, dates.toString(), dates.timeZone.toZoneId())

        for (date in dates) {
            val endTZ = DateTime(date.time + duration)
            val startDT = DateTime(date)
            val endDT = DateTime(endTZ)
            val p = Period(startDT, endDT)
            event.add(p)
        }
        for (e in event) {
            val cal = Calendar.getInstance(e.start.timeZone)
            cal.timeInMillis = e.start.time
            logger.debug("EventList start {} Instance {}, calendar hour {}, zone {}",
                    e.start.toString(), simpleDateFormat.format(cal.time), cal.get(Calendar.HOUR_OF_DAY), e.start.timeZone.toZoneId())
        }
        return event
    }

    /**
     * Check if two intervals (for example, for two scheduled events), overlap.
     * @param start1 start of first interval
     * @param end1 end of first interval
     * @param start2 start of second interval
     * @param end2 end of second interval
     * @return `true` if they overlap, `false` if they don't
     */
    fun schedulingIntervalsOverlap(start1: Date, end1: Date, start2: Date, end2: Date): Boolean {
        return (start1.after(start2) && start1.before(end2)
                || end1.after(start2) && end1.before(end2)
                || start1.before(start2) && end1.after(end2)
                || eventWithinMinimumSeparation(start1, end1, start2, end2))
    }

    /**
     * Returns true of checkStart is within EVENT_MINIMUM_SEPARATION_SECONDS of either the start or end dates, or checkEnd
     * is within EVENT_MINIMUM_SEPARATION_SECONDS of either the start or end dates.  False otherwise
     */
    private fun eventWithinMinimumSeparation(checkStart: Date, checkEnd: Date, start: Date, end: Date): Boolean {
        return (Math.abs(checkStart.time - start.time) < EVENT_MINIMUM_SEPARATION_MILLISECONDS
                || Math.abs(checkStart.time - end.time) < EVENT_MINIMUM_SEPARATION_MILLISECONDS
                || Math.abs(checkEnd.time - start.time) < EVENT_MINIMUM_SEPARATION_MILLISECONDS
                || Math.abs(checkEnd.time - end.time) < EVENT_MINIMUM_SEPARATION_MILLISECONDS)
    }
}
