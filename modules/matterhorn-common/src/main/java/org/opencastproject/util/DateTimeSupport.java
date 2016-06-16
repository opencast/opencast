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


package org.opencastproject.util;

import org.joda.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Utility class used to convert from and to <code>UTC</code> time.
 */
public final class DateTimeSupport {

  private static final String HOUR_MINUTE_SECOND_TIME_FORMAT = "HH:mm:ss";
  private static final String HOUR_MINUTE_TIME_FORMAT = "HH:mm";
  private static final String STANDARD_DATE_FORMAT = "yyyy-MM-dd";

  /** Disable construction of this utility class */
  private DateTimeSupport() {
  }

  /**
   * This methods reads a utc date string and returns it's unix time equivalent in milliseconds.
   * i.e. yyyy-MM-ddTHH:mm:ssZ e.g. 2014-09-27T16:25Z
   * @param s
   *          the utc string
   * @return the date/time in milliseconds
   * @throws IllegalStateException
   * @throws ParseException
   *           if the date string is malformed
   */
  public static long fromUTC(String s) throws IllegalStateException, ParseException {
   return fromUTC(s, STANDARD_DATE_FORMAT, HOUR_MINUTE_SECOND_TIME_FORMAT);
  }

  /**
   * This methods reads a utc date string without seconds and returns it's unix time equivalent in milliseconds.
   * i.e. yyyy-MM-ddTHH:mmZ e.g. 2014-09-27T16:25Z
   *
   * @param s
   *          the utc string
   * @return the date/time in milliseconds
   * @throws IllegalStateException
   * @throws ParseException
   *           if the date string is malformed
   */
  public static long fromUTCNoSeconds(String s) throws IllegalStateException, ParseException {
    return fromUTC(s, STANDARD_DATE_FORMAT, HOUR_MINUTE_TIME_FORMAT);
  }

  /**
   * This methods reads a utc date string and returns it's unix time equivalent in milliseconds.
   *
   * @param s
   *          the utc string
   * @param dateFormat
   *          The format to parse the date with
   * @param timeFormat
   *          The format to parse the time portion with
   * @return the date/time in milliseconds
   * @throws IllegalStateException
   * @throws ParseException
   *           if the date string is malformed
   */
  private static long fromUTC(String s, String dateFormat, String timeFormat) throws IllegalStateException,
          ParseException {
    if (s == null) {
      throw new IllegalArgumentException("UTC date string is null");
    }
    if (s.endsWith("Z")) {
      s = s.substring(0, s.length() - 1); // cut off the Z
    }
    String[] parts = s.split("T");
    if (parts.length != 2)
      throw new IllegalArgumentException("UTC date string is malformed");

    long utc = 0;

    // Parse date and time
    SimpleDateFormat df = new SimpleDateFormat(dateFormat);
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc = df.parse(parts[0]).getTime();
    SimpleDateFormat tf = new SimpleDateFormat(timeFormat);
    tf.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc += tf.parse(parts[1]).getTime();
    return utc;
  }

  /**
   * Returns the date and time in milliseconds as a utc formatted string.
   *
   * @param time
   *          the utc time string
   * @return the local time
   */
  public static String toUTC(long time) {
    StringBuffer utc = new StringBuffer();
    Date d = new Date(time);

    // Format the date
    SimpleDateFormat df = new SimpleDateFormat(STANDARD_DATE_FORMAT);
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc.append(df.format(d));
    utc.append("T");

    // Format the time
    SimpleDateFormat tf = new SimpleDateFormat(HOUR_MINUTE_SECOND_TIME_FORMAT);
    tf.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc.append(tf.format(d));
    utc.append("Z");

    return utc.toString();
  }

  /**
   * Forward to the next week day.
   * <p/>
   * If it's Monday forwarding to Tuesday will add 1 day. If it's Friday forwarding to Thursday will go to next week's
   * Thursday which is adding 6 days. Forward to Monday if on Monday simply returns <code>time</code>.
   *
   * @param weekDay
   *          like described in {@link org.joda.time.DateTimeConstants}
   */
  public static DateTime toNextWeekDay(DateTime time, int weekDay) {
    return time.getDayOfWeek() <= weekDay ? time.withDayOfWeek(weekDay) : time.plusWeeks(1).withDayOfWeek(weekDay);
  }

  /**
  * JAXB adapter that formats dates in UTC format YYYY-MM-DD'T'hh:mm:ss'Z' up to second,
  * e.g. 1970-01-01T00:00:00Z
  */
  public static final class UtcTimestampAdapter extends XmlAdapter<String, Date> {
    @Override
    public String marshal(Date date) throws Exception {
      return toUTC(date.getTime());
    }

    @Override
    public Date unmarshal(String date) throws Exception {
      return new Date(fromUTC(date));
    }
  }
}
