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

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to facilitate the work with DCMI encoding schemes.
 */
public final class EncodingSchemeUtils {

  private static final Map<Precision, String> formats = new HashMap<Precision, String>();

  static {
    formats.put(Precision.Year, "yyyy");
    formats.put(Precision.Month, "yyyy-MM");
    formats.put(Precision.Day, "yyyy-MM-dd");
    formats.put(Precision.Minute, "yyyy-MM-dd'T'HH:mm'Z'");
    formats.put(Precision.Second, "yyyy-MM-dd'T'HH:mm:ss'Z'");
    formats.put(Precision.Fraction, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  }

  /** Disable construction of this utility class */
  private EncodingSchemeUtils() {
  }

  /**
   * Encode a date with the given precision into a Dublin Core string value, using the recommended W3C-DTF scheme. The
   * UTC timezone is used for all precisions from {@link Precision#Minute} to {@link Precision#Fraction}. For years,
   * months and days the local timezone is used instead to ensure that the given date enters the DublinCore as is. If
   * UTC was used it may happen that you get the previous or next day, month or year respectively
   * <p/>
   * The language of the returned value is {@link DublinCore#LANGUAGE_UNDEFINED}.
   * <p/>
   * See <a href="http://www.w3.org/TR/NOTE-datetime">http://www.w3.org/TR/NOTE-datetime</a> for more information about
   * W3C-DTF.
   *
   * @param date
   *          the date to encode
   * @param precision
   *          the precision to use
   */
  public static DublinCoreValue encodeDate(Date date, Precision precision) {
    if (date == null)
      throw new IllegalArgumentException("The date must not be null");
    if (precision == null)
      throw new IllegalArgumentException("The precision must not be null");

    return new DublinCoreValue(formatDate(date, precision), DublinCore.LANGUAGE_UNDEFINED, DublinCore.ENC_SCHEME_W3CDTF);
  }

  public static String formatDate(Date date, Precision precision) {
    SimpleDateFormat f = new SimpleDateFormat(formats.get(precision));
    if (precision == Precision.Minute || precision == Precision.Second || precision == Precision.Fraction)
      f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return f.format(date);
  }

  /**
   * Encode a period with the given precision into a Dublin Core string value using the recommended DCMI Period scheme.
   * For the usage of the UTC timezone please refer to {@link #encodeDate(Date, Precision)} for further information.
   * <p/>
   * One of the dates may be null to create an open interval.
   * <p/>
   * The language of the returned value is {@link DublinCore#LANGUAGE_UNDEFINED}.
   * <p/>
   * See <a href="http://dublincore.org/documents/dcmi-period/">http://dublincore.org/documents/dcmi-period/</a> for
   * more information about DCMI Period.
   *
   * @param period
   *          the period
   * @param precision
   *          the precision
   */
  public static DublinCoreValue encodePeriod(DCMIPeriod period, Precision precision) {
    if (period == null)
      throw new IllegalArgumentException("The period must not be null");
    if (precision == null)
      throw new IllegalArgumentException("The precision must not be null");

    StringBuilder b = new StringBuilder();
    if (period.hasStart()) {
      b.append("start=").append(formatDate(period.getStart(), precision)).append(";");
    }
    if (period.hasEnd()) {
      if (b.length() > 0)
        b.append(" ");
      b.append("end=").append(formatDate(period.getEnd(), precision)).append(";");
    }
    if (period.hasName()) {
      b.append(" ").append("name=").append(period.getName().replace(";", "")).append(";");
    }
    b.append(" ").append("scheme=W3C-DTF;");
    return new DublinCoreValue(b.toString(), DublinCore.LANGUAGE_UNDEFINED, DublinCore.ENC_SCHEME_PERIOD);
  }

  /**
   * Encode a duration measured in milliseconds into a Dublin Core string using the
   * {@link DublinCore#ENC_SCHEME_ISO8601} encoding scheme <code>PTnHnMnS</code>.
   * <p/>
   * The language of the returned value is {@link DublinCore#LANGUAGE_UNDEFINED}.
   * <p/>
   * See <a href="http://en.wikipedia.org/wiki/ISO_8601#Durations"> ISO8601 Durations</a> for details.
   *
   * @param duration
   *          the duration in milliseconds
   */
  public static DublinCoreValue encodeDuration(long duration) {
    return new DublinCoreValue(ISOPeriodFormat.standard().print(new Duration(duration).toPeriod()),
            DublinCore.LANGUAGE_UNDEFINED, DublinCore.ENC_SCHEME_ISO8601);
  }

  /**
   * Decode a string encoded in the ISO8601 encoding scheme.
   * <p/>
   * Also supports the REPLAY legacy format <code>hh:mm:ss</code>.
   * <p/>
   * See <a href="http://en.wikipedia.org/wiki/ISO_8601#Durations"> ISO8601 Durations</a> for details.
   *
   * @param value
   *          the ISO encoded string
   * @return the duration in milliseconds or null, if the value cannot be parsed
   */
  public static Long decodeDuration(String value) {
    try {
      return ISOPeriodFormat.standard().parsePeriod(value).toStandardDuration().getMillis();
    } catch (IllegalArgumentException ignore) {
    }
    // also support the legacy format hh:mm:ss
    String[] parts = value.split(":");
    try {
      if (parts.length == 1)
        return Long.parseLong(parts[0]) * 1000;
      if (parts.length == 2)
        return Long.parseLong(parts[0]) * 1000 * 60 + Long.parseLong(parts[1]) * 1000;
      if (parts.length == 3)
        return Long.parseLong(parts[0]) * 1000 * 60 * 60 + Long.parseLong(parts[1]) * 1000 * 60
                + Long.parseLong(parts[2]) * 1000;
    } catch (NumberFormatException ignore) {
    }
    return null;
  }

  /**
   * Decode a string encoded in the ISO8601 encoding scheme.
   *
   * @param value
   *          the Dublin Core value
   * @return the duration in milliseconds or null, if the value cannot be parsed or is in a different encoding scheme
   */
  public static Long decodeDuration(DublinCoreValue value) {
    if (value.getEncodingScheme() == null || value.getEncodingScheme().equals(DublinCore.ENC_SCHEME_ISO8601)) {
      return decodeDuration(value.getValue());
    }
    return null;
  }

  public static Long decodeMandatoryDuration(DublinCoreValue value) {
    Long l = decodeDuration(value);
    if (l == null)
      throw new IllegalArgumentException("Cannot decode duration: " + value);
    return l;
  }

  public static Long decodeMandatoryDuration(String value) {
    Long l = decodeDuration(value);
    if (l == null)
      throw new IllegalArgumentException("Cannot decode duration: " + value);
    return l;
  }

  /**
   * Tries to decode the given value as a W3C-DTF encoded date. If decoding fails, null is returned.
   *
   * @return the date or null if decoding fails
   */
  public static Date decodeDate(DublinCoreValue value) {
    if (value.getEncodingScheme() == null || value.getEncodingScheme().equals(DublinCore.ENC_SCHEME_W3CDTF)) {
      try {
        return parseW3CDTF(value.getValue());
      } catch (IllegalArgumentException ignore) {
      }
    }
    return null;
  }

  /** {@link #decodeDate(org.opencastproject.metadata.dublincore.DublinCoreValue)} as a function. */
  public static final Function<DublinCoreValue, Option<Date>> dcValueToDate = new Function<DublinCoreValue, Option<Date>>() {
    @Override public Option<Date> apply(DublinCoreValue dublinCoreValue) {
      return option(decodeDate(dublinCoreValue));
    }
  };

  /**
   * Tries to decode the given value as a W3C-DTF encoded date. If decoding fails, null is returned.
   *
   * @return the date or null if decoding fails
   */
  public static Date decodeDate(String value) {
    try {
      return parseW3CDTF(value);
    } catch (IllegalArgumentException ignore) {
    }
    return null;
  }

  /** {@link #decodeDate(String)} as a function. */
  public static final Function<String, Option<Date>> stringToDate = new Function<String, Option<Date>>() {
    @Override public Option<Date> apply(String s) {
      return option(decodeDate(s));
    }
  };

  /**
   * Like {@link #decodeDate(String)}, but throws an {@link IllegalArgumentException} if the value cannot be decoded.
   *
   * @param value
   *          the value
   * @return the date
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static Date decodeMandatoryDate(DublinCoreValue value) {
    Date date = decodeDate(value);
    if (date == null)
      throw new IllegalArgumentException("Cannot decode to Date: " + value);
    return date;
  }

  /**
   * Like {@link #decodeDate(String)}, but throws an {@link IllegalArgumentException} if the value cannot be decoded.
   *
   * @return the date
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static Date decodeMandatoryDate(String value) {
    Date date = decodeDate(value);
    if (date == null)
      throw new IllegalArgumentException("Cannot decode to Date: " + value);
    return date;
  }

  private static final Pattern DCMI_PERIOD = Pattern.compile("(start|end|name)\\s*=\\s*(.*?)(?:;|\\s*$)");
  private static final Pattern DCMI_PERIOD_SCHEME = Pattern.compile("scheme\\s*=\\s*(.*?)(?:;|\\s*$)");

  /**
   * Tries to decode a string in the DCMI period format, using W3C-DTF for the encoding of the individual dates. If
   * parsing fails at any point, null will be returned.
   *
   * @return the period or null if decoding fails
   */
  public static DCMIPeriod decodePeriod(DublinCoreValue value) {
    return decodePeriod(value.getValue());
  }

  /**
   * Tries to decode a string in the DCMI period format, using W3C-DTF for the encoding of the individual dates. If
   * parsing fails at any point, null will be returned.
   *
   * @return the period or null if decoding fails
   */
  public static DCMIPeriod decodePeriod(String value) {
    // Parse value
    Matcher schemeMatcher = DCMI_PERIOD_SCHEME.matcher(value);
    boolean mayBeW3CDTFEncoded = true;
    if (schemeMatcher.find()) {
      String schemeString = schemeMatcher.group(1);
      if (!"W3C-DTF".equalsIgnoreCase(schemeString) && !"W3CDTF".equalsIgnoreCase(schemeString)) {
        mayBeW3CDTFEncoded = false;
      }
    }
    try {
      if (mayBeW3CDTFEncoded) {
        // Declare fields
        Date start = null;
        Date end = null;
        String name = null;
        // Parse
        Matcher m = DCMI_PERIOD.matcher(value);
        while (m.find()) {
          String field = m.group(1);
          String fieldValue = m.group(2);
          if ("start".equals(field)) {
            if (start != null)
              return null;
            start = parseW3CDTF(fieldValue);
          } else if ("end".equals(field)) {
            if (end != null)
              return null;
            end = parseW3CDTF(fieldValue);
          } else if ("name".equals(field)) {
            if (name != null)
              return null;
            name = fieldValue;
          }
        }
        if (start == null && end == null)
          return null;
        return new DCMIPeriod(start, end, name);
      }
    } catch (IllegalArgumentException ignore) {
      // Parse error
    }
    return null;
  }

  /**
   * Like {@link #decodePeriod(String)}, but throws an {@link IllegalArgumentException} if the value cannot be decoded.
   *
   * @return the period
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static DCMIPeriod decodeMandatoryPeriod(DublinCoreValue value) {
    return decodeMandatoryPeriod(value.getValue());
  }

  /**
   * Like {@link #decodePeriod(DublinCoreValue)}, but throws an {@link IllegalArgumentException} if the value cannot be
   * decoded.
   *
   * @return the period
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static DCMIPeriod decodeMandatoryPeriod(String value) {
    DCMIPeriod period = decodePeriod(value);
    if (period == null)
      throw new IllegalArgumentException("Cannot decode to DCMIPeriod: " + value);

    return period;
  }

  /**
   * Tries to decode the value to a temporal object. For now, supported types are {@link java.util.Date},
   * {@link DCMIPeriod} and Long for a duration.
   *
   * @param value
   *          the value to decode
   * @return a temporal object of the said types or null if decoding fails
   */
  public static Temporal decodeTemporal(DublinCoreValue value) {
    // First try Date
    Date instant = decodeDate(value);
    if (instant != null)
      return Temporal.instant(instant);
    DCMIPeriod period = decodePeriod(value);
    if (period != null)
      return Temporal.period(period);
    Long duration = decodeDuration(value);
    if (duration != null)
      return Temporal.duration(duration);
    return null;
  }

  /**
   * Like {@link #decodeTemporal(DublinCoreValue)}, but throws an {@link IllegalArgumentException} if the value cannot
   * be decoded.
   *
   * @return the temporal object of type {@link java.util.Date} or {@link DCMIPeriod}
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static Temporal decodeMandatoryTemporal(DublinCoreValue value) {
    Temporal temporal = decodeTemporal(value);
    if (value == null)
      throw new IllegalArgumentException("Cannot decode to either Date or DCMIPeriod: " + value);

    return temporal;
  }

  /**
   * @throws IllegalArgumentException
   *           if the value cannot be parsed
   */
  private static Date parseW3CDTF(String value) {
    return ISODateTimeFormat.dateTimeParser().parseDateTime(value).toDate();
  }

}
