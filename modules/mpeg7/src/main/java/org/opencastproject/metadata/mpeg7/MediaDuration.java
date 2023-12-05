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

import org.opencastproject.mediapackage.XmlElement;

/**
 * This interface describes a media duration.
 *
 * <pre>
 * &lt;simpleType name=&quot;mediaDurationType&quot;&gt;
 *   &lt;restriction base=&quot;mpeg7:basicDurationType&quot;&gt;
 *       &lt;pattern value=&quot;\-?P(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?(\d+N)?)?(\d+F)?&quot;/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 */
public interface MediaDuration extends XmlElement {

  /**
   * Returns the number of days.
   *
   * @return the days
   */
  int getDays();

  /**
   * Returns the fractions.
   *
   * @return the fractions
   */
  int getFractions();

  /**
   * Returns the fractions per second.
   *
   * @return the fractions per second
   */
  int getFractionsPerSecond();

  /**
   * Returns the number of hours.
   *
   * @return the hours
   */
  int getHours();

  /**
   * Returns the number of minutes.
   *
   * @return the minutes
   */
  int getMinutes();

  /**
   * Returns the number of seconds.
   *
   * @return the seconds
   */
  int getSeconds();

  /**
   * Returns the duration in milliseconds.
   *
   * @return the number of milliseconds
   */
  long getDurationInMilliseconds();

}
