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

package org.opencastproject.inspection.api;

/**
 * Exception thrown by the {@link MediaInspectionService} operations.
 */
public class MediaInspectionException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = 1336936343567817393L;

  public MediaInspectionException() {
    super();
  }

  /**
   * Creates a new inspection exception with the specified error message.
   *
   * @param message
   *          the error message
   */
  public MediaInspectionException(String message) {
    super(message);
  }

  /**
   * Creates a new inspection exception with the specified error message and wrapping the original exception.
   *
   * @param message
   *          the error message
   * @param cause
   *          the original error
   */
  public MediaInspectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new inspection exception by wrapping the original error .
   *
   * @param cause
   *          the original error
   */
  public MediaInspectionException(Throwable cause) {
    super(cause);
  }

}
