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

package org.opencastproject.kernel.filter.proxy;

import static org.opencastproject.kernel.filter.proxy.TransparentProxyFilter.X_FORWARDED_FOR;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This wrapper is used to return the client's original IP address even if case of a proxy being the middle man.
 */
class TransparentProxyRequestWrapper extends HttpServletRequestWrapper {

  /** The original IP */
  private final String originalIP;

  /**
   * Wraps the request.
   *
   * @param request
   *          the original request
   */
  TransparentProxyRequestWrapper(HttpServletRequest request) {
    super(request);
    originalIP = request.getHeader(X_FORWARDED_FOR);
  }

  /**
   * Overwrites the original behavior by returning the address transported in the <code>X-FORWARDED-FOR</code> request
   * header instead of the proxy's ip.
   *
   * @see javax.servlet.ServletRequestWrapper#getRemoteAddr()
   */
  @Override
  public String getRemoteAddr() {
    return originalIP;
  }

  /**
   * Overwrites the original behavior by returning the address transported in the <code>X-FORWARDED-FOR</code> request
   * header instead of the proxy's hostname.
   *
   * @see javax.servlet.ServletRequestWrapper#getRemoteHost()
   */
  @Override
  public String getRemoteHost() {
    return originalIP;
  }

}
