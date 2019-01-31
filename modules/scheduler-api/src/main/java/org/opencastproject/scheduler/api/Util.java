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

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.property.RRule;

import org.joda.time.DateTimeConstants;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public final class Util {
  /** The minimum separation between one event ending and the next starting */
  public static final int EVENT_MINIMUM_SEPARATION_MILLISECONDS = 60 * 1000;

  private Util() {
  }

  /**
   * Giving a start time and end time with a recurrence rule and a timezone, all periods of the recurrence rule are
   * calculated taken daylight saving time into account.
   *
   *  NOTE: Do not modify this without making the same modifications to the copy of this method in IndexServiceImplTest
   *  I would have moved this to the scheduler-api bundle, but that would introduce a circular dependency :(
   *
   * @param start
   *          the start date time
   * @param end
   *          the end date
   * @param duration
   *          the duration
   * @param rRule
   *          the recurrence rule
   * @param tz
   * @return a list of scheduling periods
   */
  public static List<Period> calculatePeriods(Date start, Date end, long duration, RRule rRule, TimeZone tz) {
    final TimeZone utc = TimeZone.getTimeZone("UTC");
    TimeZone.setDefault(tz);
    net.fortuna.ical4j.model.DateTime periodStart = new net.fortuna.ical4j.model.DateTime(start);
    net.fortuna.ical4j.model.DateTime periodEnd = new net.fortuna.ical4j.model.DateTime();

    Calendar endCalendar = Calendar.getInstance(utc);
    endCalendar.setTime(end);
    Calendar calendar = Calendar.getInstance(utc);
    calendar.setTime(periodStart);
    calendar.set(Calendar.DAY_OF_MONTH, endCalendar.get(Calendar.DAY_OF_MONTH));
    calendar.set(Calendar.MONTH, endCalendar.get(Calendar.MONTH));
    calendar.set(Calendar.YEAR, endCalendar.get(Calendar.YEAR));
    periodEnd.setTime(calendar.getTime().getTime() + duration);
    duration = duration % (DateTimeConstants.MILLIS_PER_DAY);

    List<Period> events = new LinkedList<>();

    TimeZone.setDefault(utc);
    for (Object date : rRule.getRecur().getDates(periodStart, periodEnd, net.fortuna.ical4j.model.parameter.Value.DATE_TIME)) {
      Date d = (Date) date;
      Calendar cDate = Calendar.getInstance(utc);

      // Adjust for DST, if start of event
      if (tz.inDaylightTime(periodStart)) { // Event starts in DST
        if (!tz.inDaylightTime(d)) { // Date not in DST?
          d.setTime(d.getTime() + tz.getDSTSavings()); // Adjust for Fall back one hour
        }
      } else { // Event doesn't start in DST
        if (tz.inDaylightTime(d)) {
          d.setTime(d.getTime() - tz.getDSTSavings()); // Adjust for Spring forward one hour
        }
      }
      cDate.setTime(d);

      TimeZone.setDefault(null);
      Period p = new Period(new net.fortuna.ical4j.model.DateTime(cDate.getTime()),
              new net.fortuna.ical4j.model.DateTime(cDate.getTimeInMillis() + duration));
      events.add(p);
      TimeZone.setDefault(utc);
    }
    TimeZone.setDefault(null);
    return events;
  }

  /**
   * Check if two intervals (for example, for two scheduled events), overlap.
   * @param start1 start of first interval
   * @param end1 end of first interval
   * @param start2 start of second interval
   * @param end2 end of second interval
   * @return <code>true</code> if they overlap, <code>false</code> if they don't
   */
  public static boolean schedulingIntervalsOverlap(final Date start1, final Date end1, final Date start2, final Date end2) {
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
