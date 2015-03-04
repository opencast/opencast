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

package org.opencastproject.matterhorn.search.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility class for the solr database.
 */
public final class IndexUtils {

  /** The date format */
  protected static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The solr supported date format. **/
  protected static DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

  /** The solr supported date format for days **/
  protected static DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

  /** The regular filter expression for single characters */
  private static final String charCleanerRegex = "([\\+\\-\\!\\(\\)\\{\\}\\[\\]\\\\^\"\\~\\*\\?\\:])";

  /**
   * Utility classes should not be initialized.
   */
  private IndexUtils() {
  }

  /**
   * Clean up the user query input string to avoid invalid input parameters.
   * 
   * @param q
   *          The input String.
   * @return The cleaned string.
   */
  public static String clean(String q) {
    q = q.replaceAll(charCleanerRegex, "\\\\$1");
    q = q.replaceAll("\\&\\&", "\\\\&\\\\&");
    q = q.replaceAll("\\|\\|", "\\\\|\\\\|");
    return q;
  }

  /**
   * Returns a serialized version of the date or <code>null</code> if
   * <code>null</code> was passed in for the date.
   * 
   * @param date
   *          the date
   * @return the serialized date
   */
  public static String serializeDate(Date date) {
    if (date == null)
      return null;
    return dateFormat.format(date);
  }

  /**
   * Returns an expression to search for any date that lies in between
   * <code>startDate</date> and <code>endDate</date>.
   * 
   * @param startDate
   *          the start date
   * @param endDate
   *          the end date
   * @return the serialized search expression
   */
  public static String serializeDateRange(Date startDate, Date endDate) {
    if (startDate == null)
      throw new IllegalArgumentException("Start date cannot be null");
    if (endDate == null)
      throw new IllegalArgumentException("End date cannot be null");
    StringBuffer buf = new StringBuffer("[");
    buf.append(dateFormat.format(startDate));
    buf.append(" TO ");
    buf.append(dateFormat.format(endDate));
    buf.append("]");
    return buf.toString();
  }

  /**
   * Returns the date with all time related fields set to the start of the day.
   * 
   * @param date
   *          the date
   * @return the date with its time component set to the beginning of the day
   */
  public static Date beginningOfDay(Date date) {
    if (date == null)
      return null;
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  /**
   * Returns the date with all time related fields set to the end of the day.
   * 
   * @param date
   *          the date
   * @return the date with its time component set to the beginning of the day
   */
  public static Date endOfDay(Date date) {
    if (date == null)
      return null;
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 99);
    return c.getTime();
  }

}
