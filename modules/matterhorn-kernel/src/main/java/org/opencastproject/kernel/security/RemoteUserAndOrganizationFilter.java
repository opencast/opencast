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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.security.api.SecurityConstants.ORGANIZATION_HEADER;
import static org.opencastproject.security.api.SecurityConstants.USER_HEADER;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
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
import javax.servlet.http.HttpServletResponse;

/**
 * Security filter used to set the organization and user in remote implementations.
 */
public class RemoteUserAndOrganizationFilter implements Filter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OrganizationFilter.class);

  /** The security service */
  protected SecurityService securityService = null;

  /** The organization directory to use when resolving organizations */
  protected OrganizationDirectoryService organizationDirectory = null;

  /** The user directory used to load users */
  protected UserDirectoryService userDirectory = null;

  /**
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;

    // Keep the original organization and user
    Organization originalOrganization = securityService.getOrganization();
    User originalUser = securityService.getUser();

    // Organization and user as specified by the request
    Organization requestOrganization = originalOrganization;
    User requestUser = originalUser;

    try {

      // See if there is an organization provided in the request
      String organizationHeader = httpRequest.getHeader(ORGANIZATION_HEADER);
      if (StringUtils.isNotBlank(organizationHeader)) {

        // Organization switching is only allowed if the request is coming in with
        // the global admin role enabled
        if (!originalUser.hasRole(GLOBAL_ADMIN_ROLE)) {
          logger.warn("An unauthorized request is trying to switch from organization '{}' to '{}'",
                  originalOrganization.getId(), organizationHeader);
          ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        try {
          requestOrganization = organizationDirectory.getOrganization(organizationHeader);
          securityService.setOrganization(requestOrganization);
          logger.trace("Switching to organization '{}' from request header {}", requestOrganization.getId(),
                  ORGANIZATION_HEADER);
        } catch (NotFoundException e) {
          logger.warn("Non-existing organization '{}' specified in request header {}", organizationHeader,
                  ORGANIZATION_HEADER);
          ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
      } else {
        logger.trace("Request organization remains '{}'", originalOrganization.getId());
      }

      // See if there is a user provided in the request
      String userHeader = httpRequest.getHeader(USER_HEADER);
      if (StringUtils.isNotBlank(userHeader)) {

        // User switching is only allowed if the request is coming in with
        // the global admin role enabled
        if (!originalUser.hasRole(GLOBAL_ADMIN_ROLE)) {
          logger.warn("An unauthorized request is trying to switch from user '{}' to '{}'", originalUser.getUsername(),
                  userHeader);
          ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        if (SecurityConstants.GLOBAL_ANONYMOUS_USERNAME.equals(userHeader)) {
          requestUser = SecurityUtil.createAnonymousUser(requestOrganization);
          logger.trace("Request user is switched to '{}'", requestUser.getUsername());
        } else {
          requestUser = userDirectory.loadUser(userHeader);
          if (requestUser != null) {
            securityService.setUser(requestUser);
            logger.trace("Switching to user '{}' from request header {}", userHeader, USER_HEADER);
          } else {
            logger.warn("Non-existing user '{}' specified in request header {}", userHeader, USER_HEADER);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
          }
        }
      }

      // Execute the rest of the filter chain
      logger.trace("Executing the filter chain with user '{}@{}'", requestUser.getUsername(),
              requestOrganization.getId());
      chain.doFilter(httpRequest, response);

    } finally {
      securityService.setOrganization(originalOrganization);
      securityService.setUser(originalUser);
    }

  }

  /**
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectory = organizationDirectory;
  }

  /**
   * Sets a reference to the user directory service.
   *
   * @param userDirectory
   *          the user directory
   */
  void setUserDirectoryService(UserDirectoryService userDirectory) {
    this.userDirectory = userDirectory;
  }

}
