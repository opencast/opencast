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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_SUDO_ROLE;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_SYSTEM_ROLES;
import static org.opencastproject.security.api.SecurityConstants.ORGANIZATION_HEADER;
import static org.opencastproject.security.api.SecurityConstants.ROLES_HEADER;
import static org.opencastproject.security.api.SecurityConstants.RUN_AS_USER_HEADER;
import static org.opencastproject.security.api.SecurityConstants.RUN_WITH_ROLES;
import static org.opencastproject.security.api.SecurityConstants.USER_HEADER;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

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
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;

    // Keep the original organization and user
    final Organization originalOrganization = securityService.getOrganization();
    final User originalUser = securityService.getUser();

    // Organization and user as specified by the request
    Organization requestedOrganization = originalOrganization;
    User requestedUser = originalUser;

    try {

      // See if there is an organization provided in the request
      String organizationHeader = httpRequest.getHeader(ORGANIZATION_HEADER);
      if (StringUtils.isNotBlank(organizationHeader)) {

        // Organization switching is only allowed if the request is coming in with the global admin role enabled
        if (!originalUser.hasRole(GLOBAL_ADMIN_ROLE)) {
          logger.warn("An unauthorized request is trying to switch from organization '{}' to '{}'",
                  originalOrganization.getId(), organizationHeader);
          ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        try {
          requestedOrganization = organizationDirectory.getOrganization(organizationHeader);
          securityService.setOrganization(requestedOrganization);
          logger.trace("Switching to organization '{}' from request header {}", requestedOrganization.getId(),
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
      if (StringUtils.isBlank(userHeader)) {
        userHeader = httpRequest.getHeader(RUN_AS_USER_HEADER);
      }
      if (StringUtils.isNotBlank(userHeader)) {

        // User switching is only allowed if the request is coming in with the global sudo role enabled
        if (!originalUser.hasRole(GLOBAL_SUDO_ROLE)) {
          logger.warn("An unauthorized request is trying to switch from user '{}' to '{}'", originalUser.getUsername(),
                  userHeader);
          ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        if (SecurityConstants.GLOBAL_ANONYMOUS_USERNAME.equals(userHeader)) {
          requestedUser = SecurityUtil.createAnonymousUser(requestedOrganization);
          logger.trace("Request user is switched to '{}'", requestedUser.getUsername());
        } else {
          requestedUser = userDirectory.loadUser(userHeader);

          // Does the target user exist?
          if (requestedUser == null) {
            logger.warn("Unable to switch to non-existing user '{}' as specified in request header {}", userHeader,
                    USER_HEADER);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
          }

          if (!originalUser.hasRole(GLOBAL_ADMIN_ROLE)) {
            // if the original user did not have system privileges, the target user must not gain those, either.
            for (String systemRole : GLOBAL_SYSTEM_ROLES) {
              if (requestedUser.hasRole(systemRole)) {
                logger.warn("An unauthorized request is trying to switch to an admin user, from '{}' to '{}'",
                        originalUser.getUsername(), userHeader);
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
              }
            }

            // make sure the user does not gain organization administrator privileges
            String organizationAdminRole = requestedOrganization.getAdminRole();
            if (!originalUser.hasRole(organizationAdminRole) && requestedUser.hasRole(organizationAdminRole)) {
              logger.warn("An unauthorized request is trying to switch to an admin user, from '{}' to '{}'",
                      originalUser.getUsername(), userHeader);
              ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
              return;
            }
          }
        }

        logger.trace("Switching from user '{}' to user '{}' from request header '{}'", originalUser.getUsername(),
                requestedUser.getUsername(), USER_HEADER);
        securityService.setUser(requestedUser);
      }

      // See if there are roles provided in the request
      String rolesHeader = httpRequest.getHeader(ROLES_HEADER);
      if (StringUtils.isBlank(rolesHeader)) {
        rolesHeader = httpRequest.getHeader(RUN_WITH_ROLES);
      }
      if (StringUtils.isNotBlank(rolesHeader)) {

        // Role switching is only allowed if the request is coming in with the global sudo role enabled
        if (!originalUser.hasRole(GLOBAL_SUDO_ROLE)) {
          logger.warn("An unauthorized request is trying to switch roles from '{}' to '{}'", requestedUser.getRoles(),
                  rolesHeader);
          ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        Collection<String> requestedRoles = Arrays.asList(StringUtils.split(rolesHeader, ","));

        if (!originalUser.hasRole(GLOBAL_ADMIN_ROLE)) {
          // Role switching is only allowed to non-system roles
          for (String systemRole : GLOBAL_SYSTEM_ROLES) {
            if (requestedRoles.contains(systemRole)) {
              logger.warn("An unauthorized request by user '{}' is trying to gain admin role '{}'",
                      originalUser.getUsername(), systemRole);
              ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
              return;
            }
          }

          // Role switching is only allowed to non-organization administrator roles
          String organizationAdminRole = requestedOrganization.getAdminRole();
          if (!originalUser.hasRole(organizationAdminRole) && requestedRoles.contains(organizationAdminRole)) {
            logger.warn("An unauthorized request by user '{}' is trying to gain admin role '{}'",
                    originalUser.getUsername(), organizationAdminRole);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
          }
        }

        // If no user has been provider by the request create a virtual user
        if (StringUtils.isBlank(userHeader)) {
          requestedUser = SecurityUtil.createAnonymousUser(requestedOrganization);
        }

        // Set roles to requested user
        requestedUser = new JaxbUser(requestedUser.getUsername(), requestedUser.getPassword(), requestedUser.getName(),
                requestedUser.getEmail(), requestedUser.getProvider(),
                JaxbOrganization.fromOrganization(requestedUser.getOrganization()),
                Stream.$(requestedRoles).map(toJaxbRole._2(requestedOrganization)).toSet());
        logger.trace("Request roles '{}' are amended to user '{}'", rolesHeader, requestedUser.getUsername());
        securityService.setUser(requestedUser);
      }

      // Execute the rest of the filter chain
      logger.trace("Executing the filter chain with user '{}@{}'", requestedUser.getUsername(),
              requestedOrganization.getId());
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

  private static final Fn2<String, Organization, JaxbRole> toJaxbRole = new Fn2<String, Organization, JaxbRole>() {
    @Override
    public JaxbRole apply(String role, Organization organization) {
      return new JaxbRole(role, JaxbOrganization.fromOrganization(organization));
    }
  };

}
