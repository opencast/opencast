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

package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Calendar;
import java.util.Formatter;

/**
 * TODO: Comment me!
 */
public class MediaTimePointImpl implements MediaTimePoint {

  /** The reference time point */
  private MediaTimePoint referenceTimePoint = null;

  /** Time delimiter */
  private static final String TimeSpecDelimiter = "T";

  /** Fraction delimiter */
  private static final String FractionSpecDelimiter = "F";

  /** Time separator */
  private static final String TimeSeparator = ":";

  /** Date separator */
  private static final String DateSeparator = "-";

  /** The year */
  protected int year = 0;

  /** The month */
  protected int month = 0;

  /** The day of month */
  protected int day = 0;

  /** The number of hour */
  protected int hour = 0;

  /** The nubmer of minutes */
  protected int minute = 0;

  /** The number of seconds */
  protected int second = 0;

  /** The fraction */
  protected int fractions = 0;

  /** The number of fractions per second */
  protected int fractionsPerSecond = 0;

  /** The time zone */
  protected String timeZone = "+00:00";

  /** Number of milliseconds per day */
  private static final long MS_PER_DAY = 86400000L;

  /** Number of milliseconds per hour */
  private static final long MS_PER_HOUR = 3600000L;

  /** Number of milliseconds per minute */
  private static final long MS_PER_MINUTE = 60000L;

  /** Number of milliseconds per second */
  private static final long MS_PER_SECOND = 1000L;

  /**
   * Creates a new media time point representing <code>T00:00:00:0F0</code>
   */
  public MediaTimePointImpl() {
    year = 0;
    month = 0;
    day = 0;
    hour = 0;
    minute = 0;
    second = 0;
    fractions = 0;
    fractionsPerSecond = 0;
  }

  /**
   * Creates a new timepoint representing the given time.
   * 
   * @param milliseconds
   *          the number of milliseconds
   */
  public MediaTimePointImpl(long milliseconds) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(milliseconds);
    second = calendar.get(Calendar.SECOND);
    minute = calendar.get(Calendar.MINUTE);
    hour = calendar.get(Calendar.HOUR_OF_DAY) - 1;
    day = calendar.get(Calendar.DAY_OF_MONTH) - 1;
    month = calendar.get(Calendar.MONTH);
    year = calendar.get(Calendar.YEAR);
    fractions = Math.round(milliseconds % 1000);
    fractionsPerSecond = 1000;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getDay()
   */
  public int getDay() {
    return day;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getHour()
   */
  public int getHour() {
    return hour;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getMinutes()
   */
  public int getMinutes() {
    return minute;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getMonth()
   */
  public int getMonth() {
    return month;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getNFractions()
   */
  public int getNFractions() {
    return fractions;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getSeconds()
   */
  public int getSeconds() {
    return second;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getYear()
   */
  public int getYear() {
    return year;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getFractionsPerSecond()
   */
  public int getFractionsPerSecond() {
    return fractionsPerSecond;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#getTimeInMilliseconds()
   */
  public long getTimeInMilliseconds() {
    long milliseconds = second * MS_PER_SECOND;
    milliseconds += minute * MS_PER_MINUTE;
    milliseconds += hour * MS_PER_HOUR;
    milliseconds += day * MS_PER_DAY;
    if (fractionsPerSecond > 0)
      milliseconds += (fractions * 1000L / fractionsPerSecond);
    return milliseconds;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTimePoint#isRelative()
   */
  public boolean isRelative() {
    return referenceTimePoint != null;
  }

  /**
   * Sets a reference time point which makes this time point relative to the given one.
   * 
   * @param timePoint
   *          the reference time point
   */
  public void setReferenceTimePoint(MediaTimePoint timePoint) {
    this.referenceTimePoint = timePoint;
  }

  public void setFractionsPerSecond(int fractionsPerSecond) {
    this.fractionsPerSecond = fractionsPerSecond;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public void setMinutes(int minutes) {
    this.minute = minutes;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public void setFractions(int fractions) {
    this.fractions = fractions;
  }

  public void setSeconds(int seconds) {
    this.second = seconds;
  }

  public void setYear(int year) {
    this.year = year;
  }

  /**
   * Creates a timepoint representation from the given time point string.
   * 
   * @param text
   *          the timepoint text representation
   * @return the the timepoint
   * @throws IllegalArgumentException
   *           if <code>text</code> is malformed
   */
  public static MediaTimePointImpl parseTimePoint(String text) throws IllegalArgumentException {
    MediaTimePointImpl timePoint = new MediaTimePointImpl();
    timePoint.parse(text);
    return timePoint;
  }

  /**
   * Parses a timepoint string.
   */
  private void parse(String text) throws IllegalArgumentException {
    String date = null;
    String time = null;
    String fractions = null;
    date = text.substring(0, text.indexOf(TimeSpecDelimiter));
    time = text.substring(text.indexOf(TimeSpecDelimiter) + 1, text.indexOf(TimeSpecDelimiter) + 9);
    fractions = text.substring(text.indexOf(TimeSpecDelimiter) + time.length() + 2);
    if (fractions.contains(TimeSeparator)) {
      timeZone = fractions.substring(fractions.length() - 6);
      fractions = fractions.substring(0, fractions.length() - 7);
    }
    parseDate(date);
    parseTime(time);
    parseFractions(fractions);
  }

  /**
   * Parses the date portion of a time point.
   * 
   * @param date
   *          the date
   */
  protected void parseDate(String date) {
    int firstDateSeparator = date.indexOf(DateSeparator);
    int lastDateSeparator = date.lastIndexOf(DateSeparator);
    if (firstDateSeparator > -1) {
      year = Integer.parseInt(date.substring(0, firstDateSeparator));
      month = Short.parseShort(date.substring(firstDateSeparator + 1, lastDateSeparator));
      day = Short.parseShort(date.substring(lastDateSeparator + 1));
    } else {
      year = 0;
      month = 0;
      day = 0;
    }
  }

  /**
   * Parses the time portion of a time point
   * 
   * @param time
   *          the time
   */
  protected void parseTime(String time) {
    int firstTimeSeparator = time.indexOf(TimeSeparator);
    int lastTimeSeparator = time.lastIndexOf(TimeSeparator);
    if (firstTimeSeparator > -1) {
      hour = Short.parseShort(time.substring(0, firstTimeSeparator));
      minute = Short.parseShort(time.substring(firstTimeSeparator + 1, lastTimeSeparator));
      second = Short.parseShort(time.substring(lastTimeSeparator + 1));
    }
  }

  /**
   * Parses the fractions of a time point.
   * 
   * @param fractions
   *          the fractions
   */
  private void parseFractions(String fractions) {
    this.fractions = Integer.parseInt(fractions.substring(0, fractions.indexOf(FractionSpecDelimiter)));
    this.fractionsPerSecond = Integer.parseInt(fractions.substring(fractions.indexOf(FractionSpecDelimiter) + 1));
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    Formatter f = new Formatter();
    String result = null;
    if (referenceTimePoint == null && year != 0) {
      result = f.format("%04d-%02d-%02dT%02d:%02d:%02d:%dF%d", year, month, day, hour, minute, second, fractions,
              fractionsPerSecond).toString();
    } else {
      result = f.format("T%02d:%02d:%02d:%dF%d", hour, minute, second, fractions, fractionsPerSecond).toString();
    }
    f.close();
    return result;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = null;
    if (referenceTimePoint != null)
      node = document.createElement("MediaRelTimePoint");
    else
      node = document.createElement("MediaTimePoint");
    node.setTextContent(toString());
    return node;
  }

}
