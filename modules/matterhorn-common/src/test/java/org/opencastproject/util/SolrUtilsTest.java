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

import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Test case for {@link SolrUtils}.
 */
public class SolrUtilsTest {

  private static DateFormat dateFormatUTC() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return df;
  }

  private static Date newDate(int year, int month, int day, int hour, int minute, int second) {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, minute);
    c.set(Calendar.SECOND, second);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  /**
   * Test method for {@link org.opencastproject.util.SolrUtils#clean(java.lang.String)}.
   */
  @Test
  public void testClean() {
    String test = "+-!(){}[]^\"~*?:&&||&|";
    String expected = "\\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\&\\&\\|\\|&|";
    assertEquals(expected, SolrUtils.clean(test));
  }

  /**
   * Test method for {@link org.opencastproject.util.SolrUtils#serializeDate(java.util.Date)}
   * .
   */
  @Test
  public void testSerializeDate() {
    Date date = newDate(2011, 5, 12, 5, 13, 0);
    String serializedDate = dateFormatUTC().format(date);
    assertEquals(serializedDate, SolrUtils.serializeDate(date));
  }

  /**
   * Test method for
   * {@link org.opencastproject.util.SolrUtils#serializeDateRange(Date, Date)}.
   */
  @Test
  public void testSerializeDateRange() {
    Date startDate = newDate(1999, 3, 21, 14, 0, 0);
    Date endDate = newDate(1999, 3, 21, 14, 30, 10);
    String serializedStartDate = dateFormatUTC().format(startDate);
    String serializedEndDate = dateFormatUTC().format(endDate);
    String day = "[" + serializedStartDate + " TO " + serializedEndDate + "]";
    assertEquals(day, SolrUtils.serializeDateRange(startDate, endDate));
    assertEquals("[* TO " + serializedEndDate + "]", SolrUtils.serializeDateRange(null, endDate));
    assertEquals("[" + serializedStartDate + " TO *]", SolrUtils.serializeDateRange(startDate, null));
  }

}
