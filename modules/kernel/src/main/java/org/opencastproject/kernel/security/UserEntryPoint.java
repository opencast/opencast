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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An {@link AuthenticationEntryPoint} that delegates to either a specified redirecting AEP or to
 * one replying with 403.
 */
public class UserEntryPoint implements AuthenticationEntryPoint {

  private LoginUrlAuthenticationEntryPoint redirectingEntryPoint;
  private Http403ForbiddenEntryPoint forbiddenEntryPoint = new Http403ForbiddenEntryPoint();
  private List<AntPathRequestMatcher> redirectingPathPatterns;
  private static final Logger logger = LoggerFactory.getLogger(UserEntryPoint.class);

  public UserEntryPoint() {
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
          throws IOException, ServletException {
    var routeMatch = this.redirectingPathPatterns.stream().filter(pattern -> pattern.matches(request)).findFirst();
    logger.debug("Match {} against routes: {}", request.getRequestURI(), routeMatch);

    if (routeMatch.isPresent()) {
      this.redirectingEntryPoint.commence(request, response, authException);
    } else {
      this.forbiddenEntryPoint.commence(request, response, authException);
    }
  }

  public void setRedirectingEntryPoint(LoginUrlAuthenticationEntryPoint redirectingEntryPoint) {
    this.redirectingEntryPoint = redirectingEntryPoint;
  }

  public void setRedirectingPathPatterns(List<String> patterns) {
    logger.debug("Set redirectingPathPatterns to {}", patterns);
    this.redirectingPathPatterns = patterns.stream()
          .map(pattern -> new AntPathRequestMatcher(pattern))
          .collect(Collectors.toList());
  }
}
