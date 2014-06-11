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

/**
 * <code>MediaTime</code> that contains a relative <code>MediaTimePoint</code>.
 */
public class MediaRelTimeImpl extends MediaTimeImpl {

  /**
   * Creates a new media time instance, representing a time and duration that is relative to
   * <code>referenceTimePoint</code>.
   *
   * @param time
   *          the time point relative to referenceTimePoint
   * @param duration
   *          the duration in miliseconds
   */
  public MediaRelTimeImpl(long time, long duration) {
    super(new MediaRelTimePointImpl(time), new MediaDurationImpl(duration));
  }

  /**
   * Creates a new media time instance, representing a time and duration that is relative to
   * <code>referenceTimePoint</code>.
   *
   * @param hour
   *          the number of hours
   * @param minute
   *          the number of minutes
   * @param second
   *          the number of seconds
   * @param fraction
   *          the number of milliseconds
   * @param fractionsPerSecond
   *          the number of fractions
   * @param duration
   *          the duration
   */
  public MediaRelTimeImpl(int hour, int minute, int second, int fraction, int fractionsPerSecond, long duration) {
    super(new MediaRelTimePointImpl(hour, minute, second, fraction, fractionsPerSecond),
            new MediaDurationImpl(duration));
  }

}
