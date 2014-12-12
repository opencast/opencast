/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class used to convert from and to <code>UTC</code> time.
 */
public final class DateTimeSupport {

  /** Disable construction of this utility class */
  private DateTimeSupport() {
  }

  /**
   * This methods reads a utc date string and returns it's unix time equivalent in milliseconds.
   *
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
    if (s.endsWith("Z")) {
      s = s.substring(0, s.length() - 1); // cut off the Z
    }
    String[] parts = s.split("T");
    if (parts.length != 2)
      throw new IllegalArgumentException("UTC date string is malformed");

    long utc = 0;

    // Parse date and time
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc = df.parse(parts[0]).getTime();
    SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
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
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc.append(df.format(d));
    utc.append("T");

    // Format the time
    SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
    tf.setTimeZone(TimeZone.getTimeZone("UTC"));
    utc.append(tf.format(d));
    utc.append("Z");

    return utc.toString();
  }

}
