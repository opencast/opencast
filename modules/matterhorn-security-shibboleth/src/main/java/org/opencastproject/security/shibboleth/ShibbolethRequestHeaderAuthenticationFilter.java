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
package org.opencastproject.security.shibboleth;

import org.opencastproject.security.api.UserDirectoryService;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.util.Assert;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/**
 * Handles for Shibboleth request headers to create Authorization ids. Optional operations can be assigned by setting
 * the ShibbolethLoginHandler; for example, to create corresponding user accounts if the user doesn't exist or update
 * user information on seeing Shibboleth attribute data.
 */
public class ShibbolethRequestHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {

  /** Spring security's user details manager */
  private UserDetailsService userDetailsService = null;

  /** The implementation that is taking care of extracting user attributes from the request */
  private ShibbolethLoginHandler loginHandler = null;

  /** If set to true, all request headers will be logged */
  private boolean debug = false;

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.notNull(userDetailsService, "A UserDetailsService must be set");
    Assert.notNull(loginHandler, "A ShibbolethLoginHandler must be set");
  }

  /**
   * This is called when a request is made, the returned object identifies the user and will either be Null or a String.
   * This method will throw an exception if exceptionIfHeaderMissing is set to true (default) and the required header is
   * missing.
   *
   * @param request
   *          the incoming request
   */
  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    String o = (String) (super.getPreAuthenticatedPrincipal(request));
    if (debug)
      debug(request);
    if (o != null && !"".equals(o.trim())) {
      try {
        if (userDetailsService.loadUserByUsername(o) != null) {
          loginHandler.existingUserLogin(o, request);
        }
      } catch (UsernameNotFoundException e) {
        loginHandler.newUserLogin(o, request);
        ((UserDirectoryService) userDetailsService).invalidate(o);
      }
    }
    return o;
  }

  /**
   * Logs all request headers to the logging facility.
   *
   * @param request
   *          the request
   */
  @SuppressWarnings("unchecked")
  protected void debug(HttpServletRequest request) {
    Enumeration<String> he = request.getHeaderNames();
    while (he.hasMoreElements()) {
      String headerName = he.nextElement();
      StringBuffer buf = new StringBuffer(headerName).append(": ");
      Enumeration<String> hv = request.getHeaders(headerName);
      boolean first = true;
      while (hv.hasMoreElements()) {
        if (!first)
          buf.append(", ");
        buf.append(hv.nextElement());
        first = false;
      }
      logger.info(buf.toString());
    }
  }

  /**
   * If set to <code>true</code>, the filter will log all request headers to the logging facility.
   *
   * @param debug
   *          <code>true</code> to log request headers
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Sets the user details service which allows to check whether a user is already known by the system or not.
   *
   * @param userDetailsService
   *          the user details service
   */
  public void setUserDetailsService(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  /**
   * Required. Used to handle creation and update of user accounts.
   *
   * @param loginHandler
   *          the handler
   */
  public void setShibbolethLoginHandler(ShibbolethLoginHandler loginHandler) {
    this.loginHandler = loginHandler;
  }

}
