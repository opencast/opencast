/*
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

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Utility class used to convert from and to <code>UTC</code> time.
 */
public final class DateTimeSupport {

  private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
    if (s == null) {
      throw new IllegalArgumentException("UTC date string is null");
    }
    String withoutZone = s.endsWith("Z") ? s.substring(0, s.length() - 1) : s;
    try {
      return LocalDateTime.parse(withoutZone, UTC_FORMAT).toInstant(ZoneOffset.UTC).toEpochMilli();
    } catch (DateTimeParseException e) {
      throw new ParseException(e.getMessage(), e.getErrorIndex());
    }
  }

  /**
   * Returns the date and time in milliseconds as a utc formatted string.
   *
   * @param time
   *          the utc time string
   * @return the local time
   */
  public static String toUTC(long time) {
    return Instant.ofEpochMilli(time).truncatedTo(ChronoUnit.SECONDS).toString();
  }

  /**
   * Converts seconds to a human readable time string.
   */
  public static String humanReadableTime(long seconds) {
    return String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
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
