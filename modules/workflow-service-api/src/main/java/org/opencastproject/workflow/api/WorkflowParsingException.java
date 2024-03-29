/*
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


package org.opencastproject.workflow.api;

/**
 * Exception that is thrown for failing database operations.
 */
public class WorkflowParsingException extends WorkflowException {


  /** Serial version uid */
  private static final long serialVersionUID = -8203912582435200347L;

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

}
