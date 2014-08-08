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

package org.opencastproject.mediapackage.identifier;

/**
 * General exception that is raised when problems occur while retreiving or dealing with handles.
 */
public class HandleException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = 1485516511882283397L;

  /**
   * Creates a new handle exception with the specified message.
   *
   * @param msg
   *          the error message
   */
  public HandleException(String msg) {
    super(msg);
  }

  /**
   * Creates a new handle exception caused by Throwable <code>t</code>.
   *
   * @param t
   *          the original exception
   */
  public HandleException(Throwable t) {
    super(t.getMessage(), t);
  }

  /**
   * Creates a new handle exception caused by Throwable <code>t</code>.
   *
   * @param msg
   *          individual error message
   * @param t
   *          the original exception
   */
  public HandleException(String msg, Throwable t) {
    super(msg, t);
  }

}
