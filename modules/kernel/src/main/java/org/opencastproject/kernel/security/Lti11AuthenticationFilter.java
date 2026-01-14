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

import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

public class Lti11AuthenticationFilter extends OncePerRequestFilter {
  private OAuthConsumerDetailsService oAuthConsumerDetailsService;
  private LtiLaunchAuthenticationHandler ltiLaunchAuthenticationHandler;

  private static final Logger logger = LoggerFactory.getLogger(Lti11AuthenticationFilter.class);

  private static Set<String> ltiMessageTypes = new HashSet<String>(
          Set.of("basic-lti-launch-request", "ContentItemSelectionRequest"));
  private static final String LTI_MESSAGE_TYPE = "lti_message_type";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    // 1. Only process LTI launch requests (typically POST)
    if ("POST".equalsIgnoreCase(request.getMethod())
            && ltiMessageTypes.contains(request.getParameter(LTI_MESSAGE_TYPE))) {

      try {
        // 2. Verify the OAuth 1.0a signature
        // Use the IMS Global basiclti-util-java library for that.
        String consumerKey = request.getParameter("oauth_consumer_key");
        request.setCharacterEncoding("UTF-8");
        // Canvas already adds the query string parameters as request parameters and this makes
        // the LtiVerifier verify(request, secret) fail with invalid signature so we use
        // the verifyParameters instead.
        // #DCE Embedded videos may have a 'custom_start' and 'custom_end' query parameters
        // that are added to the POST request parameters by Canvas.

        // When converting the Map<String, String[]> from request.getParameterMap() to a
        // Map<String, String>, always take the first element: map.get(key)[0]. This is the
        // standard way to handle the LTI 1.1 "single-value" expectation while correctly
        // processing the roles string.
        Map<String, String> parameterMap = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
        String urlWithoutQueryString = request.getRequestURL().toString();

        // --- #DCE debug to list all request parameters including the query string added
        // by Canvas
        for (String key : parameterMap.keySet()) {
          logger.trace("#DCE Request parameter {}: {}", key, parameterMap.get(key));
        }
        // --- #DCE
        LtiVerifier verifier = new LtiOauthVerifier();

        LtiVerificationResult result = verifier.verifyParameters(parameterMap, urlWithoutQueryString, HttpMethod.POST,
                oAuthConsumerDetailsService.getConsumerSecret(consumerKey));

        if (result.getSuccess()) {
          logger.trace("Oauth signature verification success.");

          // 3. Extract user info and set Authentication in SecurityContext
          String username = request.getParameter("user_id");

          // Invoke LtiLaunchAuthenticationHandler to get the user roles
          Authentication auth = ltiLaunchAuthenticationHandler.createAuthentication(request);

          SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
          logger.warn("Invalid LTI signature for request: {}", request.getRequestURI());
          response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid LTI signature");
          return;
        }
      } catch (LtiVerificationException e) {
        logger.warn("LTI verification failed for request: {}", request.getRequestURI());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "LTI verification failed");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  public void setOAuthConsumerDetailsService(OAuthConsumerDetailsService service) {
    logger.trace("setOAuthConsumerDetailsService");
    this.oAuthConsumerDetailsService = service;
  }

  public void setLtiLaunchAuthenticationHandler(LtiLaunchAuthenticationHandler service) {
    logger.trace("setLtiLaunchAuthenticationHandler");
    this.ltiLaunchAuthenticationHandler = service;
  }
}
