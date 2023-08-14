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


package org.opencastproject.oaipmh;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * General purpose functions for OAI-PMH.
 */
public final class OaiPmhUtil {
  private OaiPmhUtil() {
  }

  /**
   * Convert a date into a UTC string of the given granularity.
   */
  public static String toUtc(Date d, Granularity g) {
    if (d != null) {
      switch (g) {
        case SECOND:
          return toUtcSecond(d);
        case DAY:
          return toUtcDay(d);
        default:
          throw new RuntimeException("bug");
      }
    } else {
      return null;
    }
  }

  /**
   * Convert to a UTC date and time string (granularity is "second").
   */
  public static String toUtcSecond(Date d) {
    return newDateTimeFormat().format(d);
  }

  /**
   * Convert to a UTC date string containing only the day (granularity is "day").
   */
  public static String toUtcDay(Date d) {
    return newDateFormat().format(d);
  }

  /**
   * Convert to a date from a UTC string with either day or second granularity.
   * @throws ParseException
   *          malformed date string
   */
  public static Date fromUtc(String d) throws ParseException {
    try {
      return newDateTimeFormat().parse(d);
    } catch (ParseException e) {
      return newDateFormat().parse(d);
    }
  }

  public static DateFormat newDateTimeFormat() {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return f;
  }

  public static DateFormat newDateFormat() {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return f;
  }

  private static final String GRANULARITY_DAY = "YYYY-MM-DD";
  private static final String GRANULARITY_SECOND = "YYYY-MM-DDThh:mm:ssZ";

  /**
   * Convert from a OAI representation.
   * See the <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Identify">spec</a> for
   * further details.
   *
   * @throws IllegalArgumentException
   *          oai is not valid representation
   */
  public static Granularity fromOaiRepresentation(String oai) {
    if (GRANULARITY_DAY.equals(oai))
      return Granularity.DAY;
    if (GRANULARITY_SECOND.equals(oai))
      return Granularity.SECOND;
    throw new IllegalArgumentException(oai + " is not a valid representation");
  }

  public static String toOaiRepresentation(Granularity g) {
    switch (g) {
      case DAY: return GRANULARITY_DAY;
      case SECOND: return GRANULARITY_SECOND;
      default: throw new RuntimeException("bug");
    }
  }
}
