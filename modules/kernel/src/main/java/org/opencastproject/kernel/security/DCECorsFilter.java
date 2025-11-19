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
package org.opencastproject.kernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used on engage only to allow IC to post content. Temporary until we upgrade Spring security, which has a configurable
 * CorsFilter!
 */
public class DCECorsFilter extends GenericFilterBean {
  private static final Pattern hostRegex = Pattern.compile("https?://.*\\.dcex\\.harvard\\.edu(:[0-9]+)?");

  private static final Logger logger = LoggerFactory.getLogger(DCECorsFilter.class);

  public DCECorsFilter() {
    super();
    logger.info("DCECorsFilter constructor");
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
          throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    // This was copied from our nginx configuration on engage
    String corsOrigin = "*";
    String corsMethods = "GET, OPTIONS";

    String origin = request.getHeader("Origin");
    if (origin != null) {
      Matcher matcher = hostRegex.matcher(origin);
      if (matcher.matches()) {
        corsOrigin = origin;
        corsMethods = request.getHeader("Access-Control-Request-Method");
        response.addHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
        response.addHeader("X-Frame-Options", String.format("ALLOW FROM %s", origin));
        response.addHeader("Vary", "Origin");
        response.addHeader("Access-Control-Expose-Headers", "Content-Length,Content-Range");
      }
    }
    response.addHeader("Access-Control-Allow-Origin", corsOrigin);
    response.addHeader("Access-Control-Allow-Credentials", "true");
    response.addHeader("Access-Control-Allow-Methods", corsMethods);

    if (request.getMethod().equals("OPTIONS")) {
      // Tell client that this pre-flight info is valid for 20 days.
      response.addHeader("Access-Control-Max-Age", "1728000");
      response.addHeader("Content-Type", "text/plain charset=UTF-8");
      response.addHeader("Content-Length", "0");
      response.setStatus(HttpServletResponse.SC_ACCEPTED);
      // UP-6 allow Content-Type in header (Hopefully, not superceeded by the setting 3 lines above)
      response.addHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
      return;
    }

    chain.doFilter(request, servletResponse);

  }
}
