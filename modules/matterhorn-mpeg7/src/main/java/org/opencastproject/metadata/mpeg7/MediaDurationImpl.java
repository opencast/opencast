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


package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Formatter;

/**
 * TODO: Comment me!
 */
public class MediaDurationImpl implements MediaDuration {

  /** Time delimiter */
  private static final String TimeDelimiter = "T";

  /** Day delimiter */
  private static final String DayDelimiter = "D";

  /** Hour delimiter */
  private static final String HourDelimiter = "H";

  /** Minute delimiter */
  private static final String MinuteDelimiter = "M";

  /** Seconds delimiter */
  private static final String SecondsDelimiter = "S";

  /** Fractions delimiter */
  private static final String FractionDelimiter = "N";

  /** Fractions per second delimiter */
  private static final String FPSDelimiter = "F";

  /** Number of milliseconds per day */
  private static final long MS_PER_DAY = 86400000L;

  /** Number of milliseconds per hour */
  private static final long MS_PER_HOUR = 3600000L;

  /** Number of milliseconds per minute */
  private static final long MS_PER_MINUTE = 60000L;

  /** Number of milliseconds per second */
  private static final long MS_PER_SECOND = 1000L;

  private int days = 0;
  private int hours = 0;
  private int minutes = 0;
  private int seconds = 0;
  private int fractions = 0;
  private int fractionsPerSecond = 0;

  /**
   * Creates a media duration representing <code>PD0T00H00M00S00N0F</code>.
   */
  public MediaDurationImpl() {
    days = 0;
    hours = 0;
    minutes = 0;
    seconds = 0;
    fractions = 0;
    fractionsPerSecond = 0;
  }

  /**
   * Creates a media duration representing the given long value.
   */
  public MediaDurationImpl(long milliseconds) {
    fractions = (int) (milliseconds % MS_PER_SECOND);
    seconds = (int) ((milliseconds / MS_PER_SECOND) % 60);
    minutes = (int) ((milliseconds / MS_PER_MINUTE) % 60);
    hours = (int) ((milliseconds / MS_PER_HOUR) % 24);
    days = (int) (milliseconds / MS_PER_DAY);
    hours += days * 24;
    fractionsPerSecond = 1000;
  }

  /**
   * Parses a duration text representation.
   *
   * @param text
   *          the duration text representation
   * @return the media duration object
   * @throws IllegalArgumentException
   *           if the text is not in the right format
   */
  public static MediaDuration parseDuration(String text) throws IllegalArgumentException {
    MediaDurationImpl mediaDuration = new MediaDurationImpl();
    mediaDuration.parse(text);
    return mediaDuration;
  }

  /**
   * Parses a duration text representation.
   *
   * @param text
   *          the duration text representation
   * @throws IllegalArgumentException
   *           if the text is not in the right format
   */
  public void parse(String text) throws IllegalArgumentException {
    int index = 0;
    if ((!text.startsWith("P")) || (!text.contains(TimeDelimiter)))
      throw new IllegalArgumentException();
    if (text.contains(DayDelimiter)) {
      days = Integer.parseInt(text.substring(1, text.indexOf(DayDelimiter)));
    }
    index = text.indexOf(TimeDelimiter);
    if (text.contains(HourDelimiter)) {
      hours = Short.parseShort(text.substring(index + 1, text.indexOf(HourDelimiter)));
      index = text.indexOf(HourDelimiter);
    }
    if (text.contains(MinuteDelimiter)) {
      minutes = Short.parseShort(text.substring(index + 1, text.indexOf(MinuteDelimiter)));
      index = text.indexOf(MinuteDelimiter);
    }
    if (text.contains(SecondsDelimiter)) {
      seconds = Short.parseShort(text.substring(index + 1, text.indexOf(SecondsDelimiter)));
      index = text.indexOf(SecondsDelimiter);
    }
    if (text.contains(FractionDelimiter)) {
      fractions = Integer.parseInt(text.substring(index + 1, text.indexOf(FractionDelimiter)));
      index = text.indexOf(FractionDelimiter);
    }
    if (text.contains(FPSDelimiter)) {
      fractionsPerSecond = Integer.parseInt(text.substring(index + 1, text.indexOf(FPSDelimiter)));
    }
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getDays()
   */
  public int getDays() {
    return days;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getFractions()
   */
  public int getFractions() {
    return fractions;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getFractionsPerSecond()
   */
  public int getFractionsPerSecond() {
    return fractionsPerSecond;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getHours()
   */
  public int getHours() {
    return hours;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getMinutes()
   */
  public int getMinutes() {
    return minutes;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getSeconds()
   */
  public int getSeconds() {
    return seconds;
  }

  /**
   * Sets the number of days.
   *
   * @param days
   */
  public void setDayDuration(int days) {
    this.days = days;
  }

  /**
   * Sets the number of fractions.
   *
   * @param fractions
   *          the fractions
   */
  public void setFractionDuration(int fractions) {
    this.fractions = fractions;
  }

  /**
   * Sets the number of fractions per second.
   *
   * @param fractionsPerSecond
   *          the fractions per second
   */
  public void setFractionsPerSecond(int fractionsPerSecond) {
    this.fractionsPerSecond = fractionsPerSecond;
  }

  /**
   * Sets the number of hours.
   *
   * @param hours
   *          the hours
   */
  public void setHourDuration(short hours) {
    this.hours = hours;
  }

  /**
   * Sets the number of minutes.
   *
   * @param minutes
   *          the number of minutes
   */
  public void setMinuteDuration(short minutes) {
    this.minutes = minutes;
  }

  /**
   * Sets the number of seconds.
   *
   * @param seconds
   *          the number of seconds
   */
  public void setSecondDuration(short seconds) {
    this.seconds = seconds;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer returnString = new StringBuffer("PT");
    Formatter f = new Formatter(returnString);
    if (hours != 0) {
      f.format("%02d" + HourDelimiter, hours);
    }
    if (minutes != 0) {
      f.format("%02d" + MinuteDelimiter, minutes);
    }
    if (seconds != 0) {
      f.format("%02d" + SecondsDelimiter, seconds);
    }
    if (fractionsPerSecond != 0) {
      f.format("%d" + FractionDelimiter + "%d" + FPSDelimiter, fractions, fractionsPerSecond);
    }
    f.close();
    return returnString.toString();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaDuration#getDurationInMilliseconds()
   */
  public long getDurationInMilliseconds() {
    long s = seconds * MS_PER_SECOND;
    s += minutes * MS_PER_MINUTE;
    s += hours * MS_PER_HOUR;
    s += days * MS_PER_DAY;
    if (fractionsPerSecond > 0)
      s += (fractions * 1000L / fractionsPerSecond);
    return s;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MediaDuration)
      return ((MediaDuration) obj).getDurationInMilliseconds() == getDurationInMilliseconds();
    return false;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("MediaDuration");
    node.setTextContent(toString());
    return node;
  }

}
