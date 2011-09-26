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

package org.opencastproject.mediapackage;

/**
 * This exception is thrown if an element is added to a {@link MediaPackage} and the element type is not supported.
 */
public class UnsupportedElementException extends RuntimeException {

  /** serial version id */
  private static final long serialVersionUID = 7594606321241704129L;

  /** the element */
  private MediaPackageElement element = null;

  /**
   * Creates a new exception with the given message.
   * 
   * @param message
   *          the error message
   */
  public UnsupportedElementException(String message) {
    super(message);
  }

  /**
   * Creates a new exception for the given element with the given message.
   * 
   * @param element
   *          the element
   * @param message
   */
  public UnsupportedElementException(MediaPackageElement element, String message) {
    super(message);
    this.element = element;
  }

  /**
   * Returns the element.
   * 
   * @return the element
   */
  public MediaPackageElement getElement() {
    return this.element;
  }

}
