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

import org.apache.commons.lang3.StringUtils;
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
 * This filter is looking for <code>X-FORWARDED-FOR</code> headers in the HTTP request and if found sets it as the
 * original IP.
 */
public class TransparentProxyFilter implements Filter {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TransparentProxyFilter.class);

  /** Request header that is set when behind a proxy */
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    HttpServletRequest httpReqquest = (HttpServletRequest) request;

    // Check if the X-Forwarded-For header is set
    if (StringUtils.isNotBlank(httpReqquest.getHeader(X_FORWARDED_FOR))) {
      logger.debug("Found X-Forwarded-For header. Resetting source IP");
      httpReqquest = new TransparentProxyRequestWrapper(httpReqquest);
    }

    chain.doFilter(httpReqquest, response);
  }

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  public void destroy() {
  }

}
