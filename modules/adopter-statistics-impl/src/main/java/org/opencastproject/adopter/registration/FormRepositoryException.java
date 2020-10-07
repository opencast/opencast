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

package org.opencastproject.adopter.registration;

/**
 * Exception that will be thrown when an errors occurs in the form repository.
 */
public class FormRepositoryException extends RuntimeException {

  private static final long serialVersionUID = 7482018840067182789L;

  /** The exception constructor. */
  public FormRepositoryException() {
  }

  /**
   * Creates an exception with a message.
   * @param message An error message.
   */
  public FormRepositoryException(String message) {
    super(message);
  }

  /**
   * Create exception with a cause.
   * @param cause The exception cause.
   */
  public FormRepositoryException(Throwable cause) {
    super(cause);
  }

  /**
   * Create exception with a message and a cause.
   * @param message An error message.
   * @param cause The exception cause.
   */
  public FormRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }

}
