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
package org.opencastproject.scheduler.api;

/**
 * Thrown when a scheduled event can not be saved or loaded from persistence.
 */
public class SchedulerException extends Exception {

  /** The UID for java serialization */
  private static final long serialVersionUID = 9115248123073779409L;

  /**
   * Build a new scheduler exception with a message and an original cause.
   *
   * @param message
   *          the error message
   * @param cause
   *          the original exception causing this scheduler exception to be thrown
   */
  public SchedulerException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Build a new scheduler exception from the original cause.
   *
   * @param cause
   *          the original exception causing this scheduler exception to be thrown
   */
  public SchedulerException(Throwable cause) {
    super(cause);
  }

  /**
   * Build a new scheduler exception with a message
   *
   * @param message
   *          the error message
   */
  public SchedulerException(String message) {
    super(message);
  }
}
