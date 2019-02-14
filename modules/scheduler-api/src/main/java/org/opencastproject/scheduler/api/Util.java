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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public final class Util {

  private static final Logger logger = LoggerFactory.getLogger(Util.class);

  private Util() {
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
    List<Period> periods = null;
    String recurStr = rRule.getValue();
    Calendar startCal = Calendar.getInstance(tz);
    Calendar endCal = Calendar.getInstance(tz);
    startCal.setTime(start);
    endCal.setTime(end);
    try {
      periods = Util.calculatePeriods(startCal, endCal, duration, recurStr, tz);
    } catch (ParseException e) {
      // Swallowing this exception because this should never happen here.
      // The inbound RRule should have contained a valid rule string.
      logger.warn("Unexpected parse exception from RRule's recurrcence string {}", rRule.getValue(), e);
    }
    return periods;
  }

  /**
   * Given a start time and end time with a recurrence rule and a timezone, all periods of the recurrence rule are
   * calculated taken daylight saving time into account.
   *
   * @param startCalTz, Calendar date time of the recurrence in the timezone of the scheduled CA
   * @param endCalTz, Calendar date time of the recurrence in the timezone of the scheduled CA
   * @param duration, the length of each event
   * @param rRuleTzStr, the recurrence rule (based on the Start time zone, including start hour and days of week)
   * @param tz, the timezone of the scheduled CA
   * @return a list of event Periods that match the rule and start and end times
   * @throws ParseException
   */
  public static List<Period> calculatePeriods(Calendar startCalTz, Calendar endCalTz, long duration, String rRuleTzStr, TimeZone tz) throws ParseException {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy");
    simpleDateFormat.setTimeZone(tz);
    String tzStr = tz.getID();
    List<Period> event = new LinkedList<>();
    TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
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
            simpleDateFormat.format(new Date(periodEnd.getTime())), duration, rRuleTzStr);

    Recur recur = new Recur(rRuleTzStr);
    // Try use period start as seed to stick the local TZ
    DateList dates = recur.getDates(periodStart, periodStart, periodEnd, DATE_TIME);
    logger.trace("Got {} dates: {}, tz '{}'", dates.size(), dates.toString(), dates.getTimeZone().toZoneId());

    for (Date date : dates) {
      Date endTZ = new DateTime(date.getTime() + duration);
      DateTime startDT = new DateTime(date);
      DateTime endDT = new DateTime(endTZ);
      Period p = new Period(startDT, endDT);
      // remove duplicate dates from recur results
      if (!event.contains(p)) {
        event.add(p);
      }
    }
    for (Period e: event) {
      Calendar cal = Calendar.getInstance(e.getStart().getTimeZone());
      cal.setTimeInMillis(e.getStart().getTime());
      logger.debug("EventList start {} Instance {}, calendar hour {}, zone {}",
              e.getStart().toString(), simpleDateFormat.format(cal.getTime()), cal.get(Calendar.HOUR_OF_DAY), e.getStart().getTimeZone().toZoneId());
    }
    return event;
  }
}
