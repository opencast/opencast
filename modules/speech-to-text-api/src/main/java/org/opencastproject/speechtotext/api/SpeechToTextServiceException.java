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


package org.opencastproject.speechtotext.api;

/**
 * Exception thrown by the {@link SpeechToTextService} operations.
 */
public class SpeechToTextServiceException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = 2082939507235048476L;

  /**
   * Creates a new speech-to-text exception with the specified error message.
   *
   * @param message
   *          the error message
   */
  public SpeechToTextServiceException(String message) {
    super(message);
  }

  /**
   * Creates a new speech-to-text exception with the specified error message and wrapping the original exception.
   *
   * @param message
   *          the error message
   * @param cause
   *          the original error
   */
  public SpeechToTextServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new speech-to-text exception by wrapping the original error .
   *
   * @param cause
   *          the original error
   */
  public SpeechToTextServiceException(Throwable cause) {
    super(cause);
  }

}
