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

package org.opencastproject.gstreamer.service.api;

/**
 * This exception may be thrown by an gstreamer launch.
 */
public class GStreamerLaunchException extends Exception {

  /** serial version uid */
  private static final long serialVersionUID = 7371209976253680445L;
  
  /** Exit code of external processes */
  private int exitCode = -1;

  /**
   * Creates a new gstreamer launch exception with the given error message.
   * 
   * @param message
   *          the error message
   */
  public GStreamerLaunchException(String message) {
    super(message);
  }

  /**
   * Creates a new gstreamer launch exception with the given error message, caused by the given exception.
   * 
   * @param message
   *          the error message
   * @param cause
   *          the error cause
   */
  public GStreamerLaunchException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new gstreamer launch exception, caused by the given exception.
   * 
   * @param cause
   *          the error cause
   */
  public GStreamerLaunchException(Throwable cause) {
    super(cause);
  }

  /**
   * Returns the exit code of the process if it was not 0. If the exception wasn't caused by an exit code unequal to 0,
   * -1 is returned.
   * 
   * @return the exit code
   */
  public int getExitCode() {
    return exitCode;
  }

}
