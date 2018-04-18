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

import static org.junit.Assert.assertEquals;

import org.opencastproject.scheduler.api.Util;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.property.RRule;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class UtilTests {
  private static final Logger logger = LoggerFactory.getLogger(UtilTests.class);
  /**
   * Tests for the method calculatePeriods
   */
  private final TimeZone utc = TimeZone.getTimeZone("UTC");
  private final TimeZone jst = TimeZone.getTimeZone("JST"); // Japan Standard Time (UTC +9)
  private final TimeZone pst = TimeZone.getTimeZone("PST"); // Alaska Standard Time (UTC -8)
  private final TimeZone cet = TimeZone.getTimeZone("CET"); // Alaska Standard Time (UTC +2)

  @Before
  public void setUp() {

  }

  @Test
  public void calculateDaysChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    List<Period> periods;

    // JST
    start = Calendar.getInstance(jst);
    start.set(2016, 2, 25, 22, 0);
    end = Calendar.getInstance(jst);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 5);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,FR,SA,SU"; // --> Still the same day when switch to UTC (22-9)

    periods = generatePeriods(jst, start, end, days, durationMillis);
    assertEquals(5, periods.size());

    // PST
    start = Calendar.getInstance(pst);
    start.set(2016, 2, 25, 22, 0);
    end = Calendar.getInstance(pst);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 5);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,WE,SA,SU"; // --> A day after when switching to UTC (22+8)

    periods = generatePeriods(pst, start, end, days, durationMillis);
    assertEquals(5, periods.size());

    // CET
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 25, 0, 5);
    end = Calendar.getInstance(cet);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU"; // --> A day before when switch to UCT (0-2)

    periods = generatePeriods(cet, start, end, days, durationMillis);
    assertEquals(5, periods.size());
  }

  @Test
  public void calculateDSTChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    List<Period> periods;

    // CET
    TimeZone.setDefault(cet);
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 24, 0, 5);
    end = Calendar.getInstance(cet);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU";

    periods = generatePeriods(cet, start, end, days, durationMillis);
    for (Period p : periods) {
      logger.info(p.toString());
    }
    assertEquals(5, periods.size());
    TimeZone.setDefault(cet);
    for (Period d : periods) {
      DateTime dEnd = d.getEnd();

      Date date = new Date(dEnd.getTime());
      Calendar instance = Calendar.getInstance();
      instance.setTime(date);

      assertEquals(0, instance.get(Calendar.HOUR_OF_DAY));
    }

  }

  private List<Period> generatePeriods(TimeZone tz, Calendar start, Calendar end, String days, Long duration)
          throws ParseException {
    Calendar utcDate = Calendar.getInstance(utc);
    utcDate.setTime(start.getTime());
    RRule rRule = new RRule(generateRule(days, utcDate.get(Calendar.HOUR_OF_DAY), utcDate.get(Calendar.MINUTE)));
    return Util.calculatePeriods(start.getTime(), end.getTime(), duration, rRule, tz);
  }

  private String generateRule(String days, int hour, int minute) {
    return String.format("FREQ=WEEKLY;BYDAY=%s;BYHOUR=%d;BYMINUTE=%d", days, hour, minute);
  }
}
