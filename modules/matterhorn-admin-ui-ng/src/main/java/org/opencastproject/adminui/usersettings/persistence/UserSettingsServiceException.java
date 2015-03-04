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
package org.opencastproject.adminui.usersettings.persistence;

/**
 * The exception thrown if there is an exception while trying to perform CRUD operations on a user setting.
 */
public class UserSettingsServiceException extends Exception {

  /**
   * UUID
   */
  private static final long serialVersionUID = 2329343969108832413L;

  /**
   * Create exception.
   */
  public UserSettingsServiceException() {
  }

  /**
   * Create exception with a message.
   *
   * @param message
   */
  public UserSettingsServiceException(String message) {
    super(message);
  }

  /**
   * Create exception with a cause.
   *
   * @param cause
   */
  public UserSettingsServiceException(Throwable cause) {
    super(cause);
  }

  /**
   * Create exception with a message and a cause.
   *
   * @param message
   * @param cause
   */
  public UserSettingsServiceException(String message, Throwable cause) {
    super(message, cause);
  }

}
