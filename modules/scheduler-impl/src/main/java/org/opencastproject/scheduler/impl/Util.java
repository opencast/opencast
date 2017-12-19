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

package org.opencastproject.scheduler.impl;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class Util {
  private Util() {
  }

  /**
   * Giving a start time and end time with a recurrence rule and a timezone, all periods of the recurrence rule are
   * calculated taken daylight saving time into account.
   *
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
    DateTime seed = new DateTime(start);
    DateTime period = new DateTime();

    Calendar endCalendar = Calendar.getInstance(utc);
    endCalendar.setTime(end);
    Calendar calendar = Calendar.getInstance(utc);
    calendar.setTime(seed);
    calendar.set(Calendar.DAY_OF_MONTH, endCalendar.get(Calendar.DAY_OF_MONTH));
    calendar.set(Calendar.MONTH, endCalendar.get(Calendar.MONTH));
    calendar.set(Calendar.YEAR, endCalendar.get(Calendar.YEAR));
    period.setTime(calendar.getTime().getTime() + duration);
    duration = duration % (DateTimeConstants.MILLIS_PER_DAY);

    List<Period> periods = new ArrayList<Period>();

    TimeZone.setDefault(utc);
    for (Object date : rRule.getRecur().getDates(seed, period, Value.DATE_TIME)) {
      Date d = (Date) date;
      Calendar cDate = Calendar.getInstance(utc);

      // Adjust for DST, if start of event
      if (tz.inDaylightTime(seed)) { // Event starts in DST
        if (!tz.inDaylightTime(d)) { // Date not in DST?
          d.setTime(d.getTime() + tz.getDSTSavings()); // Adjust for Fall back one hour
        }
      } else { // Event doesn't start in DST
        if (tz.inDaylightTime(d)) {
          d.setTime(d.getTime() - tz.getDSTSavings()); // Adjust for Spring forward one hour
        }
      }
      cDate.setTime(d);

      periods.add(new Period(new DateTime(cDate.getTime()), new DateTime(cDate.getTimeInMillis() + duration)));
    }

    TimeZone.setDefault(null);
    return periods;
  }
}
