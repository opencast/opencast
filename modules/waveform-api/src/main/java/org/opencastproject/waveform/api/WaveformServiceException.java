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
package org.opencastproject.waveform.api;

/**
 * This exception is thrown if errors occur during waveform extraction.
 */
public class WaveformServiceException extends Exception {

  /**
   * Creates a new waveform service exception with <code>message</code> as reason.
   *
   * @param message
   *          the reason of failure
   */
  public WaveformServiceException(String message) {
    super(message);
  }

  /**
   * Creates a new waveform service exception where <code>cause</code> identifies the root cause of failure.
   *
   * @param cause
   *          the root cause of the failure
   */
  public WaveformServiceException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new waveform service exception with <code>message</code> as reason and <code>cause</code> as the root
   * cause of failure.
   *
   * @param message
   *          the reason of failure
   * @param cause
   *          the root cause of the failure
   */
  public WaveformServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
