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
package org.opencastproject.caption.util;

import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.caption.impl.TimeImpl;

/**
 * Auxiliary class that contains methods for converting from and to specific time formats.
 * 
 */
public final class TimeUtil {

  // time format regular expressions
  private static final String SRT_FORMAT = "[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{1,3}";
  private static final String DFXP_FORMAT_1 = "[0-9]{1,2}:[0-9]{2}:[0-9]{2}(.[0-9]+)?";

  // SRT time format functions

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private TimeUtil() {
    // Nothing to be done
  }

  /**
   * Parse String representation of SubRip time format.
   * 
   * @param timeSrt
   *          SubRip time format
   * @return parsed {@link Time} instance
   * @throws IllegalTimeFormatException
   *           if argument is not SubRip time format
   */
  public static Time importSrt(String timeSrt) throws IllegalTimeFormatException {
    if (!timeSrt.matches(SRT_FORMAT)) {
      throw new IllegalTimeFormatException(timeSrt + " does not appear to valid SubRip time format.");
    }

    String[] timeParts = timeSrt.split("[,:]");
    int hour = Integer.parseInt(timeParts[0]);
    int minute = Integer.parseInt(timeParts[1]);
    int second = Integer.parseInt(timeParts[2]);
    int milisecond = Integer.parseInt(timeParts[3]);
    return new TimeImpl(hour, minute, second, milisecond);
  }

  /**
   * Exports {@link Time} instance to the SubRip time format representation.
   * 
   * @param time
   *          {@link Time} instance to be exported
   * @return time exported to SubRip time format
   */
  public static String exportToSrt(Time time) {
    return String.format("%02d:%02d:%02d,%03d", time.getHours(), time.getMinutes(), time.getSeconds(),
            time.getMilliseconds());
  }

  // DFXP TT time format

  /**
   * Parse String representation of DFXP time format. It does not support parsing of metrics (for example: 34.567s).
   * 
   * @param timeDfxp
   *          DFXP time format
   * @return parsed {@link Time} instance
   * @throws IllegalTimeFormatException
   *           if argument is not DFXP time format or is not supported.
   */
  public static Time importDFXP(String timeDfxp) throws IllegalTimeFormatException {
    if (!timeDfxp.matches(DFXP_FORMAT_1)) {
      throw new IllegalTimeFormatException(timeDfxp + " is not valid DFXP time format or is not supported.");
    }

    // split
    String[] timeArray = timeDfxp.split("[\\.:]");
    int hour = Integer.parseInt(timeArray[0]);
    int minute = Integer.parseInt(timeArray[1]);
    int second = Integer.parseInt(timeArray[2]);
    int millisecond = 0;
    if (timeArray.length == 4) {
      // parse milliseconds
      if (timeArray[3].length() == 1) {
        millisecond = Integer.parseInt(timeArray[3]) * 100;
      } else if (timeArray[3].length() == 2) {
        millisecond = Integer.parseInt(timeArray[3]) * 10;
      } else if (timeArray[3].length() == 3) {
        millisecond = Integer.parseInt(timeArray[3]);
      } else {
        // more numbers - ignore the rest
        millisecond = Integer.parseInt(timeArray[3].trim().substring(0, 4));
      }
    }

    return new TimeImpl(hour, minute, second, millisecond);
  }

  /**
   * Exports {@link Time} instance to the DFXP time format representation. Specifically time format used is 0:00:00.000
   * 
   * @param time
   *          {@link Time} instance to be exported
   * @return time exported to DFXP time format
   */
  public static String exportToDFXP(Time time) {
    return String.format("%d:%02d:%02d.%03d", time.getHours(), time.getMinutes(), time.getSeconds(),
            time.getMilliseconds());
  }
}
