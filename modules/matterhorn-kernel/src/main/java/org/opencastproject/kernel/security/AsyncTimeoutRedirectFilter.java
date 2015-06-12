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
package org.opencastproject.kernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.ThrowableAnalyzer;
import org.springframework.security.web.util.ThrowableCauseExtractor;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter used to avoid redirect for asynchronous request in case of session timeout
 */
public class AsyncTimeoutRedirectFilter extends GenericFilterBean {

  private static final Logger logger = LoggerFactory.getLogger(AsyncTimeoutRedirectFilter.class);

  private ThrowableAnalyzer throwableAnalyzer = new DefaultThrowableAnalyzer();
  private AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

  /** The error code to return for the asynchronous request in case of session timeout */
  private static final int TIMEOUT_ERROR_CODE = 419;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    try {
      chain.doFilter(request, response);
      logger.debug("Chain processed normally");
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      Throwable[] causeChain = throwableAnalyzer.determineCauseChain(ex);
      RuntimeException exception = (AuthenticationException) throwableAnalyzer.getFirstThrowableOfType(
              AuthenticationException.class, causeChain);

      if (exception == null) {
        exception = (AccessDeniedException) throwableAnalyzer.getFirstThrowableOfType(AccessDeniedException.class,
                causeChain);
      }

      if (exception != null) {
        if (exception instanceof AuthenticationException) {
          throw exception;
        } else if (exception instanceof AccessDeniedException) {

          if (authenticationTrustResolver.isAnonymous(SecurityContextHolder.getContext().getAuthentication())) {
            logger.debug("User session expired or not logged in yet");

            String ajaxHeader = ((HttpServletRequest) request).getHeader("X-Requested-With");

            // If asynchronous request, we returned the error code set, otherwise we redirect to the login page
            if ("XMLHttpRequest".equals(ajaxHeader)) {
              logger.debug("Asynchronous call detected, send {} error code", TIMEOUT_ERROR_CODE);
              HttpServletResponse resp = (HttpServletResponse) response;
              resp.sendError(TIMEOUT_ERROR_CODE);
            } else {
              logger.debug("Redirect to login page");
              throw exception;
            }
          } else {
            throw exception;
          }
        }
      }

    }
  }

  private static final class DefaultThrowableAnalyzer extends ThrowableAnalyzer {
    /**
     * @see org.springframework.security.web.util.ThrowableAnalyzer#initExtractorMap()
     */
    protected void initExtractorMap() {
      super.initExtractorMap();

      registerExtractor(ServletException.class, new ThrowableCauseExtractor() {
        public Throwable extractCause(Throwable throwable) {
          ThrowableAnalyzer.verifyThrowableHierarchy(throwable, ServletException.class);
          return ((ServletException) throwable).getRootCause();
        }
      });
    }

  }
}
