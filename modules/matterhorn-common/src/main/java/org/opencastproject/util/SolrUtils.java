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

import org.apache.commons.lang.StringUtils;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.opencastproject.util.data.functions.Strings.format;
import static org.opencastproject.util.data.functions.Strings.trimToNone;

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

  /** {@link #clean(String)} as a function. Return none if string is empty after cleaning. */
  public static final Function<String, Option<String>> clean = new Function<String, Option<String>>() {
    @Override public Option<String> apply(String s) {
      return trimToNone(clean(s));
    }
  };

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

  /** {@link #serializeDate(java.util.Date)} as a function. */
  public static final Function<Date, String> serializeDate = new Function<Date, String>() {
    @Override public String apply(Date date) {
      return serializeDate(date);
    }
  };

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
   * Returns an expression to search for any date that lies in between <code>startDate</date> and <code>endDate</date>.
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
   * Return a date format suitable for solr. Format a date as UTC with a granularity of seconds.
   * <code>yyyy-MM-dd'T'HH:mm:ss'Z'</code>
   */
  public static DateFormat newSolrDateFormat() {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return f;
  }

}
