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

import static org.opencastproject.util.data.functions.Strings.format;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for the solr database.
 */
public final class SolrUtils {

  /** Disallow construction of this utility class */
  private SolrUtils() {
  }

  /** The regular filter expression for single characters */
  private static final String charCleanerRegex = "([\\+\\-\\!\\(\\)\\{\\}\\[\\]\\\\^\"\\~\\*\\?\\:])";

  /**
   * Clean up the user query input string to avoid invalid input parameters.
   *
   * @param q
   *          The input String.
   * @return The cleaned string.
   */
  public static String clean(String q) {
    return q.replaceAll(charCleanerRegex, "\\\\$1")
            .replaceAll("\\&\\&", "\\\\&\\\\&")
            .replaceAll("\\|\\|", "\\\\|\\\\|");
  }

  /**
   * Returns a serialized version of the date or <code>null</code> if <code>null</code> was passed in for the date.
   *
   * @param date
   *          the date
   * @return the serialized date
   */
  public static String serializeDate(Date date) {
    if (date == null)
      return null;
    return newSolrDateFormat().format(date);
  }

  /**
   * Returns the date or <code>null</code> if <code>null</code> was passed in for the date.
   *
   * @param date
   *          the serialized date in UTC format yyyy-MM-dd'T'HH:mm:ss'Z'
   * @return the date
   * @throws ParseException
   *           if parsing the date fails
   */
  public static Date parseDate(String date) throws ParseException {
    if (StringUtils.isBlank(date))
      return null;
    return newSolrDateFormat().parse(date);
  }

  /**
   * Returns an expression to search for any date that lies in between <code>startDate</code> and <code>endDate</code>.
   *
   * @param startDate
   *          the start date or none for an infinite left endpoint, "*" in solr query syntax
   * @param endDate
   *          the end date or none for an infinite right endpoint, "*" in solr query syntax
   * @return the serialized search expression
   */
  public static String serializeDateRange(Option<Date> startDate, Option<Date> endDate) {
    final Function<Date, String> f = format(newSolrDateFormat());
    return new StringBuilder("[")
            .append(startDate.map(f).getOrElse("*"))
            .append(" TO ")
            .append(endDate.map(f).getOrElse("*"))
            .append("]")
            .toString();
  }

  /**
   * Return a date format suitable for solr. Format a date as UTC with a granularity of
   * milliseconds. <code>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</code>
   */
  public static DateFormat newSolrDateFormat() {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return f;
  }

}
