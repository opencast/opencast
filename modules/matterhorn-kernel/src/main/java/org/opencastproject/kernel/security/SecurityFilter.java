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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet filter that delegates to the appropriate spring filter chain
 */
public final class SecurityFilter implements Filter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

  /** The filters for each organization */
  private Map<String, Filter> orgSecurityFilters = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The filter configuration provided by the servlet container */
  protected FilterConfig filterConfig = null;

  /**
   * Construct a new security filter.
   *
   * @param securityService
   *          the security service
   */
  public SecurityFilter(SecurityService securityService) {
    this.securityService = securityService;
    this.orgSecurityFilters = new HashMap<String, Filter>();
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  public void addFilter(String orgId, Filter filter) {
    // First remove any existing filter, then add the new one
    removeFilter(orgId);
    try {
      filter.init(filterConfig);
      orgSecurityFilters.put(orgId, filter);
    } catch (ServletException e) {
      logger.error("Unable to initialize {}", filter);
    }
  }

  public void removeFilter(String orgId) {
    Filter filter = orgSecurityFilters.remove(orgId);
    if (filter != null) {
      filter.destroy();
    }
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

    // Make sure we have an organization
    Organization org = securityService.getOrganization();
    if (org == null) {
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // Get a hold of the security filter for that organization
    Filter filter = orgSecurityFilters.get(org.getId());
    if (filter == null) {
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    filter.doFilter(request, response, chain);
  }
}
