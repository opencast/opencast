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
package org.opencastproject.adminui.exception;

/**
 * An exception which indicates a non existing or non readable path.
 * 
 * @author ademasi
 * 
 */
public class IllegalPathException extends Exception {

  private static final long serialVersionUID = 9196222189807408636L;

  /**
   * Constructor without cause.
   * 
   * @param message
   * @param cause
   */
  public IllegalPathException(String message) {
    super(message);
  }

  /**
   * Full fledged constructor.
   * 
   * @param message
   * @param cause
   */
  public IllegalPathException(String message, Throwable cause) {
    super(message, cause);
  }
}
