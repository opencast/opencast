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
package org.opencastproject.serviceregistry.impl;

/**
 * Exception that is thrown if a job is not dispatchable by any service that would normally accept this type of work.
 * <p>
 * The exception indicates that there may be something wrong with the job or that the job cannot be dispatched because
 * of related circumstances.
 */
public class UndispatchableJobException extends Exception {

  /** The serial version UID */
  private static final long serialVersionUID = 6255027328266035849L;

  /**
   * Creates an exception with an error message.
   * 
   * @param message
   *          the error message
   */
  public UndispatchableJobException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a cause.
   * 
   * @param cause
   *          the original cause for failure
   */
  public UndispatchableJobException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates an exception with an error message and a cause.
   * 
   * @param message
   *          the error message
   * @param cause
   *          the original cause
   */
  public UndispatchableJobException(String message, Throwable cause) {
    super(message, cause);
  }

}
