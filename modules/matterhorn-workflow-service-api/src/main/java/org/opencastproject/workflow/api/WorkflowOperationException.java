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

package org.opencastproject.workflow.api;

/**
 * Thrown when a {@link WorkflowOperationInstance} fails to run.
 */
public class WorkflowOperationException extends Exception {

  private static final long serialVersionUID = 5840096157653799867L;

  /**
   * Constructs a new {@link WorkflowOperationException} with a message and a root cause.
   *
   * @param message
   *          The message describing what went wrong
   * @param cause
   *          The exception that triggered this problem
   */
  public WorkflowOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new {@link WorkflowOperationException} with a message, but no root cause.
   *
   * @param message
   *          The message describing what went wrong
   */
  public WorkflowOperationException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link WorkflowOperationException} with a root cause.
   *
   * @param cause
   *          The exception that caused this problem
   */
  public WorkflowOperationException(Throwable cause) {
    super(cause);
  }

}
