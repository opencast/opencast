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


package org.opencastproject.elasticsearch.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for the solr database.
 */
public final class IndexUtils {

  /** The date format */
  protected static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The solr supported date format. **/
  protected static DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

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
    if (date == null) {
      return null;
    }
    return dateFormat.format(date);
  }

  /**
   * Returns an expression to search for any date that lies in between
   * <code>startDate</code> and <code>endDate</code>.
   *
   * @param startDate
   *          the start date
   * @param endDate
   *          the end date
   * @return the serialized search expression
   */
  public static String serializeDateRange(Date startDate, Date endDate) {
    if (startDate == null) {
      throw new IllegalArgumentException("Start date cannot be null");
    }
    if (endDate == null) {
      throw new IllegalArgumentException("End date cannot be null");
    }
    StringBuffer buf = new StringBuffer("[");
    buf.append(dateFormat.format(startDate));
    buf.append(" TO ");
    buf.append(dateFormat.format(endDate));
    buf.append("]");
    return buf.toString();
  }

}
