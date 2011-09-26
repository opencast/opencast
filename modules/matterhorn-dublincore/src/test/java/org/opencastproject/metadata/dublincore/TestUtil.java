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

package org.opencastproject.metadata.dublincore;

import org.junit.Ignore;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Some useful test utilities.
 */
@Ignore("Contains no tests, only helper functions")
public final class TestUtil {

  private TestUtil() {
  }

  public static Date createDate(int year, int month, int day, int hour, int minute, int second) {
    Calendar c = Calendar.getInstance();
    c.set(year, month - 1, day, hour, minute, second);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  public static Date createDate(int year, int month, int day, int hour, int minute, int second, String tz) {
    Calendar c = Calendar.getInstance(TimeZone.getTimeZone(tz));
    c.set(year, month - 1, day, hour, minute, second);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  public static Date midnight(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  public static Date precisionSecond(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  public static Date precisionDay(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

}
