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

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.UrlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

/**
 * An {@link AuthenticationEntryPoint} that redirects to a configured login URL with a specified return query parameter.
 */
public class RedirectQueryParamAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

  private String loginQueryParam;

  public RedirectQueryParamAuthenticationEntryPoint(String loginFormUrl, String loginQueryParam) {
    super(loginFormUrl);
    if (loginQueryParam == null) {
      throw new IllegalArgumentException("loginQueryParam cannot be null");
    }
    this.loginQueryParam = loginQueryParam;
  }

  @Override
  protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) {
    return UriBuilder.fromPath(super.determineUrlToUseForThisRequest(request, response, exception))
        .queryParam(loginQueryParam, UrlUtils.buildRequestUrl(request))
        .build()
        .toString();
  }

}
