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
package org.opencastproject.editor.api;

/**
 * This exception may be thrown by the EditorService.
 */
public class EditorServiceException extends Exception {
  private ErrorStatus status = ErrorStatus.UNKNOWN;

  /**
   * Creates a new editor service exception with the given error message.
   *
   * @param message
   *          the error message
   */
  public EditorServiceException(String message, ErrorStatus status) {
    super(message);
    this.status = status;
  }

  /**
   * Creates a new editor service exception with the given error message, caused by the given exception.
   *
   * @param message
   *          the error message
   * @param cause
   *          the error cause
   */
  public EditorServiceException(String message, ErrorStatus status, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public ErrorStatus getErrorStatus() {
    return this.status;
  }

}
