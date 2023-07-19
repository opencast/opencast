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

package org.opencastproject.security.jwt;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;

/**
 * Authentication filter for JWTs in request parameters.
 *
 * What exactly this means depends on the request method.
 * Specifically, for {@code GET} requests, the JWT is looked up in the query parameters.
 * For {@code POST} this expects a form body and the JWT is read from there.
 */
public class JWTRequestParameterAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  /** Name of the parameter to extract the JWT from. */
  private String parameterName = null;

  /** Login handler. */
  private JWTLoginHandler loginHandler = null;

  /** If set to true, throws an exception when the configured parameter is not provided. */
  private boolean exceptionIfParameterMissing = true;

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.notNull(parameterName, "A parameter name must be set");
    Assert.notNull(loginHandler, "A JWTLoginHandler must be set");
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    Util.debug(logger, request);

    String token = request.getParameter(parameterName);
    if (token == null && exceptionIfParameterMissing) {
      throw new PreAuthenticatedCredentialsNotFoundException(parameterName + " parameter not found in request.");
    } else if (token == null || token.isBlank()) {
      return null;
    }

    return loginHandler.handleToken(token);
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }

  /**
   * Setter for the parameter name.
   *
   * @param parameterName The parameter name.
   */
  public void setParameterName(String parameterName) {
    this.parameterName = parameterName;
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
   * Defines whether an exception should be raised if the principal parameter is missing. Defaults to true.
   * @param exceptionIfParameterMissing set to {@code false} to override the default behaviour and allow the request to
   *                                    proceed if no parameter is found.
   */
  public void setExceptionIfParameterMissing(boolean exceptionIfParameterMissing) {
    this.exceptionIfParameterMissing = exceptionIfParameterMissing;
  }

}
