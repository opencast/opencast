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
package org.opencastproject.scheduler.api;

import org.opencastproject.rest.ErrorCodeException;

/**
 * Thrown when a scheduled event can not be added or updated because of a conflict.
 */
public class SchedulerConflictException extends SchedulerException implements ErrorCodeException {

  /** The UID for java serialization */
  private static final long serialVersionUID = 2424637228731668565L;

  public static final String ERROR_CODE = "ConflictsFound";

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }

  /**
   * Build a new scheduler conflict exception with a message and an original cause.
   *
   * @param message
   *          the error message
   * @param cause
   *          the original exception causing this scheduler conflict exception to be thrown
   */
  public SchedulerConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Build a new scheduler conflict exception from the original cause.
   *
   * @param cause
   *          the original exception causing this scheduler conflict exception to be thrown
   */
  public SchedulerConflictException(Throwable cause) {
    super(cause);
  }

  /**
   * Build a new scheduler conflict exception with a message
   *
   * @param message
   *          the error message
   */
  public SchedulerConflictException(String message) {
    super(message);
  }

}
