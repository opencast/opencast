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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.scheduler.api;

import static net.fortuna.ical4j.model.parameter.Value.DATE_TIME;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.property.RRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public final class Util {
  /** The minimum separation between one event ending and the next starting */
  public static final int EVENT_MINIMUM_SEPARATION_MILLISECONDS = 60 * 1000;

  private static final Logger logger = LoggerFactory.getLogger(Util.class);
  private static final TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

  private Util() {
  }


  /**
   * Adjust the given UTC rrule to the given timezone.
   *
   * @param rRule The rrule to adjust.
   * @param start The start date in UTC
   *
   * @param tz The target timezone.
   */
  public static void adjustRrule(final RRule rRule, final Date start, final TimeZone tz) {
    final Recur recur = rRule.getRecur();
    if (recur.getHourList().size() != 1 || recur.getMinuteList().size() != 1) {
      throw new IllegalArgumentException(
          "RRules with multiple hours/minutes are not supported by Opencast. " + recur.toString());
    }
    final ZonedDateTime adjustedDate = ZonedDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC)
            .withHour(recur.getHourList().get(0))
            .withMinute(recur.getMinuteList().get(0))
            .withZoneSameInstant(tz.toZoneId());
    recur.getHourList().set(0, adjustedDate.getHour());
    recur.getMinuteList().set(0, adjustedDate.getMinute());
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
  public static List<Period> calculatePeriods(Date start, Date end, long duration, RRule rRule, TimeZone tz) {
    Calendar startCal = Calendar.getInstance(tz);
    Calendar endCal = Calendar.getInstance(tz);
    startCal.setTime(start);
    endCal.setTime(end);
    return Util.calculatePeriods(startCal, endCal, duration, rRule.getRecur(), tz);
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
  public static List<Period> calculatePeriods(
      Calendar startCalTz, Calendar endCalTz, long duration, Recur recur, TimeZone tz) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy");
    simpleDateFormat.setTimeZone(tz);
    String tzStr = tz.getID();
    List<Period> event = new LinkedList<>();
    logger.debug("Inbound start of recurrence {} to end of recurrence {}, in Tz {}",
        simpleDateFormat.format(startCalTz.getTime()),
        simpleDateFormat.format(endCalTz.getTime()), tzStr);

    DateTime periodStart = new DateTime(startCalTz.getTime());
    logger.debug("ical4j timeZone for {} is {}", tzStr, registry.getTimeZone(tzStr).toZoneId());
    periodStart.setTimeZone(registry.getTimeZone(tzStr));
    DateTime periodEnd = new DateTime(endCalTz.getTime());
    periodEnd.setTimeZone(registry.getTimeZone(tzStr));

    logger.trace("is utc {}? Tz is {} ", periodStart.isUtc(), periodStart.getTimeZone().toZoneId());
    logger.debug("({}) Looking at recurrences for {} to {}, duration {}, {}", periodStart.getTimeZone().toZoneId(),
        simpleDateFormat.format(new Date(periodStart.getTime())),
        simpleDateFormat.format(new Date(periodEnd.getTime())), duration, recur.toString());

    DateList dates = recur.getDates(periodStart, periodEnd, DATE_TIME);
    logger.trace("Got {} dates: {}, tz '{}'", dates.size(), dates.toString(), dates.getTimeZone().toZoneId());

    for (Date date : dates) {
      Date endTZ = new DateTime(date.getTime() + duration);
      DateTime startDT = new DateTime(date);
      DateTime endDT = new DateTime(endTZ);
      Period p = new Period(startDT, endDT);
      event.add(p);
    }
    for (Period e: event) {
      Calendar cal = Calendar.getInstance(e.getStart().getTimeZone());
      cal.setTimeInMillis(e.getStart().getTime());
      logger.debug("EventList start {} Instance {}, calendar hour {}, zone {}",
          e.getStart().toString(),
          simpleDateFormat.format(cal.getTime()),
          cal.get(Calendar.HOUR_OF_DAY),
          e.getStart().getTimeZone().toZoneId());
    }
    return event;
  }

  /**
   * Check if two intervals (for example, for two scheduled events), overlap.
   * @param start1 start of first interval
   * @param end1 end of first interval
   * @param start2 start of second interval
   * @param end2 end of second interval
   * @return <code>true</code> if they overlap, <code>false</code> if they don't
   */
  public static boolean schedulingIntervalsOverlap(
      final Date start1, final Date end1, final Date start2, final Date end2) {
    return (start1.after(start2) && start1.before(end2)
        || end1.after(start2) && end1.before(end2)
        || start1.before(start2) && end1.after(end2)
        || eventWithinMinimumSeparation(start1, end1, start2, end2));
  }

  /**
   * Returns true of checkStart is within EVENT_MINIMUM_SEPARATION_SECONDS of either the start or end dates, or checkEnd
   * is within EVENT_MINIMUM_SEPARATION_SECONDS of either the start or end dates.  False otherwise
   */
  private static boolean eventWithinMinimumSeparation(Date checkStart, Date checkEnd, Date start, Date end) {
    return Math.abs(checkStart.getTime() - start.getTime()) < EVENT_MINIMUM_SEPARATION_MILLISECONDS
        || Math.abs(checkStart.getTime() - end.getTime()) < EVENT_MINIMUM_SEPARATION_MILLISECONDS
        || Math.abs(checkEnd.getTime() - start.getTime()) < EVENT_MINIMUM_SEPARATION_MILLISECONDS
        || Math.abs(checkEnd.getTime() - end.getTime()) < EVENT_MINIMUM_SEPARATION_MILLISECONDS;
  }
}
