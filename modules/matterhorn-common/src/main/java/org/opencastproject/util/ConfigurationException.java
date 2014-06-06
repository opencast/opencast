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

/**
 * This exception is thrown on various occasions where the system detects a state of malconfiguration.
 */
public class ConfigurationException extends RuntimeException {

  /** Serial Version UID */
  private static final long serialVersionUID = -3960378289149011212L;

  /**
   * Creates a new configuration exception.
   *
   * @param message
   *          the exception message
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates a new configuration exception with the given message and cause of the malconfiguration.
   *
   * @param message
   *          the message
   * @param cause
   *          the exception cause
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
