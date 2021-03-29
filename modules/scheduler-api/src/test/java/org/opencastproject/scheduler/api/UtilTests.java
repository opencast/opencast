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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Recur;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


public class UtilTests {
  private static final Logger logger = LoggerFactory.getLogger(UtilTests.class);
  /**
   * Tests for the method calculatePeriods
   */
  private final TimeZone jst = TimeZone.getTimeZone("Asia/Tokyo"); // Japan Standard Time (UTC +9)
  private final TimeZone pst = TimeZone.getTimeZone("America/Anchorage"); // Alaska Standard Time (UTC -8)
  private final TimeZone pstDL = TimeZone.getTimeZone("ATDT");  // Alaska Daylight Savings Time (UTC -7)
  private final TimeZone cet = TimeZone.getTimeZone("Europe/Zurich"); // European time (UTC +2)
  private final TimeZone nonDstTz = TimeZone.getTimeZone("America/Phoenix"); // Like UTC, it has no DaySavings Boundry

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
    start.set(Calendar.SECOND, 0);
    assertEquals(0, start.get(Calendar.SECOND));
    end = Calendar.getInstance(jst);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 5);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,FR,SA,SU"; // --> Still the same day when switch to UTC (22-9)
    logger.debug("expecting days {} tz {}", days, start.getTimeZone().getID());
    periods = generatePeriods(jst, start, end, days, durationMillis);
    logger.debug("Expecting 5 got {}", periods.size());
    assertEquals(5, periods.size());

    // PST
    start = Calendar.getInstance(pstDL);
    start.set(2016, 2, 23, 22, 0);
    start.set(Calendar.SECOND, 0);
    assertEquals(0, start.get(Calendar.SECOND));
    end = Calendar.getInstance(pstDL);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 5);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,WE,SA,SU"; // --> A day after when switching to UTC (22+8)
    logger.debug("expecting days {} tz {}", days, start.getTimeZone().getID());
    periods = generatePeriods(pstDL, start, end, days, durationMillis);
    logger.debug("Expecting 5 got {}", periods.size());
    assertEquals(5, periods.size());

    // CET
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 25, 15, 5);
    start.set(Calendar.SECOND, 0);
    assertEquals(0, start.get(Calendar.SECOND));
    end = Calendar.getInstance(cet);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 10);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,FR,SA,SU"; // --> A day before when switch to UCT (0-2)
    logger.trace("expecting days {} tz {}", days, start.getTimeZone().getID());
    periods = generatePeriods(cet, start, end, days, durationMillis);
    logger.trace("Expecting 5 got {}", periods.size());
    assertEquals(5, periods.size());

    // A Non-Daylight Saving observing TimeZone
    // reference https://github.com/ical4j/ical4j/issues/232
    start = Calendar.getInstance(nonDstTz);
    start.set(2016, 2, 10, 0, 5);
    start.set(Calendar.SECOND, 0);
    assertEquals(0, start.get(Calendar.SECOND));
    end = Calendar.getInstance(nonDstTz);
    end.set(2016, 2, 17, start.get(Calendar.HOUR_OF_DAY), 10);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,FR,SA,SU";
    logger.debug("expecting days {} tz {}", days, start.getTimeZone().getID());
    periods = generatePeriods(nonDstTz, start, end, days, durationMillis);
    logger.debug("Expecting 5 got {}", periods.size());
    assertEquals(5, periods.size());

  }

  @Test
  public void calculateDSTSpringForwardChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;

    // CET->CEST test (March 25 is CET->CEST)
    // On Sunday, March 27, 2:00 am CET->CEST
    int scheduledHour = 15; // the hour of the day that the events should occur
    int expectedCount = 20; // the amount of events that should be scheduled
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 15, scheduledHour, 5);
    start.set(Calendar.SECOND, 0);
    end = Calendar.getInstance(cet);
    end.set(2016, 3, 11, start.get(Calendar.HOUR_OF_DAY), 10);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU";
    logger.trace("expecting hour {} days {} tz {}", scheduledHour, days, start.getTimeZone().getID());
    doDSTChangeOverTest(cet, start, end, days, durationMillis, scheduledHour, expectedCount);
  }

  @Test
  public void calculateDSTFallBackChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    TimeZone cetCest = TimeZone.getTimeZone("Europe/Berlin");

    // On Sunday, October 30, 3:00 am, CEST->CET
    int scheduledHour = 0; // the hour of the day that the events should occur
    int expectedCount = 30; // the amount of events that should be scheduled
    start = Calendar.getInstance(cetCest);
    start.set(2016, 9, 20, scheduledHour, 5);
    start.set(Calendar.SECOND, 0);
    end = Calendar.getInstance(cetCest);
    end.set(2016, 10, 18, start.get(Calendar.HOUR_OF_DAY), 10);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,WE,TH,FR,SA,SU";
    logger.trace("expecting hour {} days {} tz {}", scheduledHour, days, start.getTimeZone().getID());
    doDSTChangeOverTest(cetCest, start, end, days, durationMillis, scheduledHour, expectedCount);
  }

  @Test
  public void calculateDSTSpringForwardChangeMiddleDay() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;

    // CET->CEST test (March 25 is CET->CEST)
    // On Sunday, March 27, 2:00 am CET->CEST
    int scheduledHour = 12; // the hour of the day that the events should occur
    int expectedCount = 17; // the amount of events that should be scheduled
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 20, scheduledHour, 5);
    start.set(Calendar.SECOND, 0);
    end = Calendar.getInstance(cet);
    end.set(2016, 3, 11, start.get(Calendar.HOUR_OF_DAY), 10);
    end.set(Calendar.SECOND, 50);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU";
    logger.trace("expecting hour {} days {} tz {}", scheduledHour, days, start.getTimeZone().getID());
    doDSTChangeOverTest(cet, start, end, days, durationMillis, scheduledHour, expectedCount);
  }

  @Test
  public void calculateDSTSpringForwardBackwordBothInOneRecurrence() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;

    // CET->CEST Sunday March 25 2am
    // CET->CEST Sunday, October 30, 3am

    int scheduledHour = 5; // the hour of the day that the events should occur
    int expectedCount = 535; // the amount of events that should be scheduled
    int minutesOfEvent = 3 * 60; // 3 hours
    start = Calendar.getInstance(cet);
    start.set(2015, 9, 15, scheduledHour, 5);
    // The seconds are NOT guaranteed to be 0 unless explicitly set
    start.set(Calendar.SECOND, 0);
    end = Calendar.getInstance(cet);
    end.set(2017, 9, 30, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE) % minutesOfEvent);
    // The seconds are NOT guaranteed to be 0 unless explicitly set
    end.set(Calendar.SECOND, 50);
    durationMillis = minutesOfEvent * 60 * 1000;
    days = "MO,TH,FR,SA,SU";
    logger.trace("expecting hour {} days {} tz {}", scheduledHour, days, start.getTimeZone().getID());
    doDSTChangeOverTest(cet, start, end, days, durationMillis, scheduledHour, expectedCount);
  }

  @Test
  // reference https://github.com/ical4j/ical4j/issues/232
  public void calculateNonDstTimeZoneInOneVeryLongRecurrence() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    int scheduledHour = 5; // the hour of the day that the events should occur
    int expectedCount = 535; // the amount of events that should be scheduled
    int minutesOfEvent = 3 * 60; // 3 hours
    start = Calendar.getInstance(nonDstTz);
    start.set(2015, 9, 15, scheduledHour, 5);
    // The seconds are NOT guaranteed to be 0 unless explicitly set
    start.set(Calendar.SECOND, 0);
    end = Calendar.getInstance(nonDstTz);
    end.set(2017, 9, 30, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE) % minutesOfEvent);
    // The seconds are NOT guaranteed to be 0 unless explicitly set
    end.set(Calendar.SECOND, 50);
    durationMillis = minutesOfEvent * 60 * 1000;
    days = "MO,TH,FR,SA,SU";
    logger.trace("expecting hour {} days {} tz {}", scheduledHour, days, start.getTimeZone().getID());
    doDSTChangeOverTest(nonDstTz, start, end, days, durationMillis, scheduledHour, expectedCount);
  }

  /**
   * Call with fallback and spring forward scenarios
   *
   * @param tz
   * @param start
   * @param end
   * @param days
   * @param durationMillis
   * @throws ParseException
   */
  private void doDSTChangeOverTest(TimeZone tz, Calendar start, Calendar end, String days, long durationMillis,
          int expectedHour, int expectedCount) throws ParseException {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy");
    simpleDateFormat.setTimeZone(tz);
    logger.debug("= start ======================================================");
    List<Period> periods = generatePeriods(tz, start, end, days, durationMillis);
    logger.debug("Expecting {} got {}", expectedCount, periods.size());
    assertEquals(expectedCount, periods.size());
    for (Period d : periods) {
      //logger.debug("Retrieved period start {} end {}", d.getStart().toString(),  d.getEnd().toString());
      Calendar cal = Calendar.getInstance(d.getStart().getTimeZone());
      cal.setTimeInMillis(d.getStart().getTime());
      logger.debug("Date {} Instance {}, calendar hour {} (expected {}), zone {}",
          d.getStart().toString(),
          simpleDateFormat.format(cal.getTime()),
          cal.get(Calendar.HOUR_OF_DAY),
          expectedHour,
          tz.getID());
      assertEquals(expectedHour, cal.get(Calendar.HOUR_OF_DAY));
    }
    logger.debug("= end ======================================================");
  }

  private List<Period> generatePeriods(TimeZone tz, Calendar startTz, Calendar endTz, String days, Long duration)
          throws ParseException {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy");
    simpleDateFormat.setTimeZone(tz);

    // Verify Dates are in TZ or 1 off ( DayLight TZ or Standard TZ)
    logger.debug("Period start {} end {}, Tz {}, local TimeZone {}", simpleDateFormat.format(startTz.getTime()),
        simpleDateFormat.format(endTz.getTime()), tz.getID(),
        TimeZone.getDefault().getID());
    assertTrue(Math.abs(endTz.getTimeZone().getRawOffset() - tz.getRawOffset()) < 2);
    assertTrue(Math.abs(startTz.getTimeZone().getRawOffset() - tz.getRawOffset()) < 2);

    String rRuleStr = generateRule(days, startTz.get(Calendar.HOUR_OF_DAY), startTz.get(Calendar.MINUTE));
    simpleDateFormat.setTimeZone(tz);
    logger.debug("Period start {} end {}, Tz {}, local TimeZone {}", simpleDateFormat.format(startTz.getTime()),
        simpleDateFormat.format(endTz.getTime()), tz.getID(),
        TimeZone.getDefault().getID());
    logger.debug(rRuleStr);
    return Util.calculatePeriods(startTz, endTz, duration, new Recur(rRuleStr), tz);
  }

  private String generateRule(String days, int hour, int minute) {
    return String.format("FREQ=WEEKLY;BYDAY=%s;BYHOUR=%d;BYMINUTE=%d", days, hour, minute);
  }
}
