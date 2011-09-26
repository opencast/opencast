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

package org.opencastproject.workflow.api;

/**
 * Exception that is thrown for failing database operations.
 */
public class WorkflowParsingException extends WorkflowException {


  /** Serial version uid */
  private static final long serialVersionUID = -8203912582435200347L;

  /**
   * Constructs a new workflow parsing exception without a message or a cause.
   */
  public WorkflowParsingException() {
  }

  /**
   * Constructs a new workflow parsing exception with a message.
   * 
   * @param message
   *          the message describing the exception
   */
  public WorkflowParsingException(String message) {
    super(message);
  }

  /**
   * Constructs a new workflow parsing exception with the throwable causing this exception to be thrown.
   * 
   * @param cause
   *          the cause of this exception
   */
  public WorkflowParsingException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new workflow parsing exception with a message and the throwable that caused this exception to be
   * thrown.
   * 
   * @param message
   *          the message describing the exception
   * @param cause
   *          the cause of this exception
   */
  public WorkflowParsingException(String message, Throwable cause) {
    super(message, cause);
  }

}
