/*
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

import org.opencastproject.kernel.filter.proxy.TransparentProxyFilter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardFilterName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardFilterPattern;
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
 * <p>
 * The headers it acts on are only believed if the request reached us through a trusted reverse proxy, which
 * {@link TransparentProxyFilter} has determined before this filter runs.
 */
@Component(
    service = Filter.class,
    scope = ServiceScope.PROTOTYPE,
    property = {
        "service.description=Https Filter",
    }
)
@ServiceRanking(990)
@HttpWhiteboardFilterName("HttpsFilter")
@HttpWhiteboardFilterPattern("/*")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=opencast)")
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
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    // Anyone can send these headers, so they only mean something if a proxy we trust is the one who sent them
    if (!Boolean.TRUE.equals(httpRequest.getAttribute(TransparentProxyFilter.FORWARDED_HEADERS_TRUSTED))) {
      chain.doFilter(httpRequest, response);
      return;
    }

    // Check if the forwarded SSL header is set
    if (X_FORWARDED_SSL_VALUE.equalsIgnoreCase(httpRequest.getHeader(X_FORWARDED_SSL))) {
      logger.debug("Found forwarded SSL header");
      httpRequest = new HttpsRequestWrapper(httpRequest);
    } else if (X_FORWARDED_PROTO_VALUE.equalsIgnoreCase(httpRequest.getHeader(X_FORWARDED_PROTO))) {
      logger.debug("Found forwarded proto HTTPS header");
      httpRequest = new HttpsRequestWrapper(httpRequest);
    }
    chain.doFilter(httpRequest, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

}
