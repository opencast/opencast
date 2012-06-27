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
 * General exception that is raised when problems occur while manipulating media packages like adding or removing media
 * package elements, creating manifests or moving and copying the media package itself.
 */
public class MediaPackageException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = -1645569283274593366L;

  /**
   * Creates a new media package exception with the specified message.
   * 
   * @param msg
   *          the error message
   */
  public MediaPackageException(String msg) {
    super(msg);
  }

  /**
   * Creates a new media package exception caused by Throwable <code>t</code>.
   * 
   * @param t
   *          the original exception
   */
  public MediaPackageException(Throwable t) {
    super(t.getMessage(), t);
  }

  /**
   * Creates a new media package exception caused by Throwable <code>t</code>.
   * 
   * @param msg
   *          individual error message
   * @param t
   *          the original exception
   */
  public MediaPackageException(String msg, Throwable t) {
    super(msg, t);
  }

}
