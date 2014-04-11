/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.security;

import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.INITIAL_REQUEST_PATH;

import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Sends authenticated users to one of the configured welcome pages after login.
 */
public class AuthenticationSuccessHandler implements
        org.springframework.security.web.authentication.AuthenticationSuccessHandler {

  /** The wildcard role matcher for welcome pages */
  public static final String WILDCARD = "*";

  /** The root resource */
  public static final String ROOT = "/";

  /** The security service */
  private SecurityService securityService = null;

  /** The maps of roles to welcome pages */
  private Map<String, String> welcomePages = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.springframework.security.web.authentication.AuthenticationSuccessHandler#onAuthenticationSuccess(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse, org.springframework.security.core.Authentication)
   */
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication) throws IOException, ServletException {

    // If the user originally attempted to access a specific URI other than /, but was forwarded to the login page,
    // redirect the user back to that initial URI.
    HttpSession session = request.getSession();
    String initialRequestUri = (String) session.getAttribute(INITIAL_REQUEST_PATH);
    session.removeAttribute(INITIAL_REQUEST_PATH);
    if (initialRequestUri != null) {
      response.sendRedirect(initialRequestUri);
      return;
    }

    // If there are no configured welcome pages, send the user to /
    if (welcomePages == null || welcomePages.isEmpty()) {
      response.sendRedirect(ROOT);
      return;
    }

    // Look for a welcome page for one of this user's roles
    User currentUser = securityService.getUser();
    for (Role role : currentUser.getRoles()) {
      if (welcomePages.containsKey(role.getName())) {
        response.sendRedirect(welcomePages.get(role.getName()));
        return;
      }
    }

    // None of the user's roles are in the welcome pages map, so try the wildcard. If that's not present, redirect to /
    if (welcomePages.containsKey(WILDCARD)) {
      response.sendRedirect(welcomePages.get(WILDCARD));
    } else {
      response.sendRedirect(ROOT);
    }
  }

  /**
   * Sets the security service
   * 
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the welcome pages mapping.
   * 
   * @param welcomePages
   *          the welcomePages to set
   */
  public void setWelcomePages(Map<String, String> welcomePages) {
    this.welcomePages = welcomePages;
  }
}
