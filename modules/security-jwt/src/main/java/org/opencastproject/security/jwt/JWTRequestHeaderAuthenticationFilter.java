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

import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.util.Assert;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/**
 * Authentication filter for JWTs.
 */
public class JWTRequestHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {

  /** Prefix of the principal that will be ignored. */
  private String principalPrefix = null;

  /** Login handler. */
  private JWTLoginHandler loginHandler = null;

  /** If set to true, all request headers will be logged. */
  private boolean debug = false;

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.notNull(loginHandler, "A JWTLoginHandler must be set");
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    if (debug) {
      debug(request);
    }

    String username = null;

    String principal = (String) (super.getPreAuthenticatedPrincipal(request));
    if (principal != null && !"".equals(principal.trim())) {
      if (principalPrefix != null) {
        if (!principal.startsWith(principalPrefix)) {
          return null;
        }
        principal = principal.replace(principalPrefix, "");
      }
      username = loginHandler.handleToken(principal);
    }

    return username;
  }

  /**
   * Logs the headers of a request to the logging facility.
   *
   * @param request The request.
   */
  protected void debug(HttpServletRequest request) {
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

  /**
   * Setter for the principal prefix.
   *
   * @param principalPrefix The principal prefix.
   */
  public void setPrincipalPrefix(String principalPrefix) {
    this.principalPrefix = principalPrefix;
  }

  /**
   * Setter for the login handler.
   *
   * @param loginHandler The login handler.
   */
  public void setLoginHandler(JWTLoginHandler loginHandler) {
    this.loginHandler = loginHandler;
  }

  /**
   * Setter for the debug switch.
   *
   * @param debug Value for the debug switch.
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

}
