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


package org.opencastproject.videosegmenter.api;

/**
 * This exception is thrown during media segmentation.
 */
public class VideoSegmenterException extends Exception {

  /** The serial version ui */
  private static final long serialVersionUID = 8647839276281407394L;

  /**
   * Creates a new media analysis exception with <code>message</code> as a reason.
   *
   * @param message
   *          the reason of failure
   */
  public VideoSegmenterException(String message) {
    super(message);
  }

  /**
   * Creates a new media analysis exception where <code>cause</code> identifies the original reason of failure.
   *
   * @param cause
   *          the root cause for the failure
   */
  public VideoSegmenterException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new media analysis exception with <code>message</code> as a reason and <code>cause</code> as the original
   * cause of failure.
   *
   * @param message
   *          the reason of failure
   * @param cause
   *          the root cause for the failure
   */
  public VideoSegmenterException(String message, Throwable cause) {
    super(message, cause);
  }

}
