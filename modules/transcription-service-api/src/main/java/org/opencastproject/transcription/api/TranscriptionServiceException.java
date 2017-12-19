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
package org.opencastproject.transcription.api;

public class TranscriptionServiceException extends Exception {
  private static final long serialVersionUID = 4196196907868554450L;

  private int code;

  public TranscriptionServiceException() {
    super();
  }

  public TranscriptionServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public TranscriptionServiceException(String message) {
    super(message);
  }

  public TranscriptionServiceException(String message, int code) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
