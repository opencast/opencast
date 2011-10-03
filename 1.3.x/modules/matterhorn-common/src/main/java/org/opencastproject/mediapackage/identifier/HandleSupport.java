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

import java.io.File;

/**
 * Utility class used to deal with handle identifiers.
 */
public final class HandleSupport {

  /** Disable constructing this utility class */
  private HandleSupport() {
  }

  /**
   * Converts the handle into a valid path name by replacing forward slashes with dots.
   * 
   * @param handle
   *          the handle identifier
   * @return the pathname
   */
  public static String toPath(Handle handle) {
    StringBuffer buf = new StringBuffer(handle.getNamingAuthority());
    buf.append("/");
    buf.append(File.separatorChar);
    buf.append(handle.getLocalName().replace('/', File.separatorChar));
    return buf.toString();
  }

}
