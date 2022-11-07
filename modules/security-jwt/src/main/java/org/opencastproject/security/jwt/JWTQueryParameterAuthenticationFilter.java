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

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

/**
 * Authentication filter for JWTs in query parameters
 */
public class JWTQueryParameterAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  /** Name of the parameter to extract the JWT from. */
  private String parameterName = null;

  /** Login handler. */
  private JWTLoginHandler loginHandler = null;

  /** If set to true, throws an exception when the configured parameter is not provided. */
  private boolean exceptionIfParameterMissing = true;

  /** If set to true, all request headers will be logged. */
  private boolean debug = false;

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.notNull(parameterName, "A parameter name must be set");
    Assert.notNull(loginHandler, "A JWTLoginHandler must be set");
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    if (debug) {
      Util.debug(logger, request);
    }

    String queryString = Objects.requireNonNullElse(request.getQueryString(), "");

    String token = Arrays.stream(queryString.split("&"))
            .map(kv -> kv.split("="))
            .filter(kv -> kv[0].equals(parameterName))
            .findAny()
            .map(kv -> kv.length < 2 ? "" : kv[1])
            .orElseGet(() -> {
              if (exceptionIfParameterMissing) {
                throw new PreAuthenticatedCredentialsNotFoundException(
                        parameterName + " parameter not found in request.");
              }
              return "";
            });

    if (token.isBlank()) {
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
   * Defines whether an exception should be raised if the principal query parameter is missing. Defaults to true.
   * @param exceptionIfParameterMissing set to {@code false} to override the default behaviour and allow the request to
   *                                    proceed if no parameter is found.
   */
  public void setExceptionIfParameterMissing(boolean exceptionIfParameterMissing) {
    this.exceptionIfParameterMissing = exceptionIfParameterMissing;
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
