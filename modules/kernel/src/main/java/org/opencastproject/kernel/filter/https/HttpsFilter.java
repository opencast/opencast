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

package org.opencastproject.kernel.filter.https;

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
 * This filter is wrapping <code>HttpServletRequest</code>s in such a way that they feature the https scheme.
 */
public class HttpsFilter implements Filter {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(HttpsFilter.class);

  /** Request header that is set when behind an SSL proxy */
  private static final String X_FORWARDED_SSL = "X-Forwarded-SSL";

  /** Alternative request header set by proxy */
  private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  /** Value of the X-Forwarded-SSL header that activates request wrapping */
  private static final String X_FORWARDED_SSL_VALUE = "on";

  /** Value of the X-Forwarded-Proto header that activates request wrapping */
  private static final String X_FORWARDED_PROTO_VALUE = "https";

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    HttpServletRequest httpReqquest = (HttpServletRequest) request;

    // Check if the forwarded SSL header is set
    if (X_FORWARDED_SSL_VALUE.equalsIgnoreCase(httpReqquest.getHeader(X_FORWARDED_SSL))) {
      logger.debug("Found forwarded SSL header");
      httpReqquest = new HttpsRequestWrapper(httpReqquest);
    } else if (X_FORWARDED_PROTO_VALUE.equalsIgnoreCase(httpReqquest.getHeader(X_FORWARDED_PROTO))) {
      logger.debug("Found forwarded proto HTTPS header");
      httpReqquest = new HttpsRequestWrapper(httpReqquest);
    }
    chain.doFilter(httpReqquest, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

}
