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

package org.opencastproject.security.jwt;

import org.apache.commons.logging.Log;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/** Utility class */
public final class Util {
  /** Hide the constructor. */
  private Util() {
  }

  /**
   * Logs the headers of a request to the logging facility.
   *
   * @param request The request.
   */
  protected static void debug(Log logger, HttpServletRequest request) {
    Enumeration<String> he = request.getHeaderNames();
    while (he.hasMoreElements()) {
      String headerName = he.nextElement();
      StringBuilder builder = new StringBuilder(headerName).append(": ");
      Enumeration<String> hv = request.getHeaders(headerName);
      boolean first = true;
      while (hv.hasMoreElements()) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(hv.nextElement());
        first = false;
      }
      logger.debug(builder.toString());
    }
  }
}
