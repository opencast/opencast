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

import org.opencastproject.mediapackage.XmlElement;

/**
 * Media time point represents a time within the track, e. g. the starting time of a video segment.
 */
public interface MediaTimePoint extends XmlElement {

  /**
   * Returns the day of month.
   *
   * @return the day of month
   */
  int getDay();

  /**
   * Returns the hour of day (in 24-hour representation).
   *
   * @return the hour
   */
  int getHour();

  /**
   * Returns the number of minutes.
   *
   * @return the minutes
   */
  int getMinutes();

  /**
   * Returns the month.
   *
   * @return the month
   */
  int getMonth();

  /**
   * Returns the fractions.
   *
   * @return the fractions
   */
  int getNFractions();

  /**
   * Returns the seconds.
   *
   * @return the seconds
   */
  int getSeconds();

  /**
   * Returns the year.
   *
   * @return the year
   */
  int getYear();

  /**
   * The fractions per second.
   *
   * @return the fractions per second
   */
  int getFractionsPerSecond();

  /**
   * Returns the media time point in milliseconds.
   *
   * @return the media time point
   */
  long getTimeInMilliseconds();

  /**
   * Returns <code>true</code> if this time point is relative to another time point, e. g. the starting time point of a
   * temporal segment's track.
   *
   * @return <code>true</code> if this time point is relative
   */
  boolean isRelative();

}
