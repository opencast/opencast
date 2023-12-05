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

package org.opencastproject.userdirectory;


/**
 * An exception thrown when something is being created has been created. For example a group id that is already being
 * used.
 *
 */
public class ConflictException extends Exception {

  /** The serial version ui */
  private static final long serialVersionUID = 898989376281407395L;

  /**
   * @param message
   */
  public ConflictException(String message) {
    super(message);
  }

  /**
   * @param cause
   */
  public ConflictException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public ConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   */
  public ConflictException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
