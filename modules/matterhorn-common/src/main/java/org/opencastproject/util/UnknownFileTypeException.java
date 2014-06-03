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

package org.opencastproject.util;

import java.io.File;

/**
 * Class description goes here.
 */
public class UnknownFileTypeException extends Exception {

  /** There serial version uid */
  private static final long serialVersionUID = -3505640764857664931L;

  /** The file in question */
  private File file = null;

  /**
   * Creates a new exception with the given message.
   *
   * @param message
   *          the error message
   */
  public UnknownFileTypeException(String message) {
    this(message, null);
  }

  /**
   * Creates a new exception with the given message and a reference to the file causing the exception.
   *
   * @param message
   *          the error message
   * @param file
   *          the file causing the error
   */
  public UnknownFileTypeException(String message, File file) {
    super(message);
    this.file = file;
  }

  /**
   * Returns the file that caused the exception.
   *
   * @return the file
   */
  public File getFile() {
    return file;
  }

}
