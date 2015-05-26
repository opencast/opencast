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

package org.opencastproject.kernel.rest;

import org.opencastproject.rest.RestConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Goes through each request and sets its max inactive time to a default value if they are a normal request or
 * invalidates the session if they are a security request. Without this filter you will see HashSession object contain
 * more and more objects and running the garbage collector will not clear them out until the server runs out of memory.
 * This will not be obvious on a test server unless it is under heavy load for a long period of time. Please see ticket
 * http://opencast.jira.com/browse/MH-8205 for more details and discussion.
 */
public class CleanSessionsFilter implements Filter {
  private static final int NO_MAX_INACTIVE_INTERVAL_SET = -1;
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CleanSessionsFilter.class);

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig config) throws ServletException {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
          ServletException {

    // Cast the request and response to HTTP versions
    HttpServletRequest request = (HttpServletRequest) req;
    if (request != null && request.getSession() != null) {
      if (request.getSession().getMaxInactiveInterval() == NO_MAX_INACTIVE_INTERVAL_SET) {
        // There is no maxInactiveInterval set so we need to set one.
        logger.trace("Setting maxInactiveInterval to " + RestConstants.MAX_INACTIVE_INTERVAL + " on request @" + request.getRequestURL());
        request.getSession().setMaxInactiveInterval(RestConstants.MAX_INACTIVE_INTERVAL);
      }
    }
    chain.doFilter(req, resp);

    // This has to be run after the chain.doFilter to invalidate the sessions after Spring Security has run as it creates new sessions.
    if (request != null && HttpServletRequest.DIGEST_AUTH.equals(request.getAuthType())) {
      logger.trace("Invalidating digest request.");
      request.getSession().invalidate();
    }
    else if (request.getHeader("Authorization") != null) {
      logger.trace("Invalidating digest request.");
      request.getSession().invalidate();
    }
  }
}
