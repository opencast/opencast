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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Inspects request URLs and sets the organization for the request.
 */
@Component(
    immediate = true,
    service = Filter.class,
    property = {
        "service.description=Organization Filter",
        "httpContext.id=opencast.httpcontext",
        "httpContext.shared=true",
        "service.ranking=2",
        "urlPatterns=*"
    }
)
public class OrganizationFilter implements Filter {

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OrganizationFilter.class);

  /** The security service */
  protected SecurityService securityService = null;

  /** The organization directory to use when resolving organizations. This may be null. */
  protected OrganizationDirectoryService organizationDirectory = null;

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  @Reference(name = "orgDirectory")
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectory = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    URL url = new URL(httpRequest.getRequestURL().toString());

    Organization org = null;

    try {

      try {
        org = organizationDirectory.getOrganization(url);
      } catch (NotFoundException e) {
        logger.trace("No organization mapped to {}", url);
        List<Organization> orgs = organizationDirectory.getOrganizations();
        if (orgs.size() == 1) {
          org = orgs.get(0);
          logger.trace("Defaulting organization to {}", org);
        }
      }

      // If an organization was found, move on. Otherwise return a 404
      if (org == null) {
        logger.debug("No organization is mapped to handle {}", url);
        httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "No organization is mapped to handle " + url);
        return;
      }
      securityService.setOrganization(org);

      // Set the client's IP address
      if (StringUtils.isNotBlank(httpRequest.getHeader(X_FORWARDED_FOR))) {
        logger.trace("Found '{}' header for client IP '{}'", X_FORWARDED_FOR, httpRequest.getHeader(X_FORWARDED_FOR));
        securityService.setUserIP(httpRequest.getHeader(X_FORWARDED_FOR));
      } else {
        logger.trace("Using client IP from request '{}'", httpRequest.getRemoteAddr());
        securityService.setUserIP(httpRequest.getRemoteAddr());
      }

      chain.doFilter(request, response);

    } finally {
      securityService.setOrganization(null);
      securityService.setUser(null);
      securityService.setUserIP(null);
    }
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "securityService")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
