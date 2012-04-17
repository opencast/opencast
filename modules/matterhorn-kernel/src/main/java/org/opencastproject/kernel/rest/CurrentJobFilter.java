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
package org.opencastproject.kernel.rest;

import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

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
 * Inspects request current job header and sets the current job for the request.
 */
public class CurrentJobFilter implements Filter {

  public static final String CURRENT_JOB_HEADER = "X-Opencast-Matterhorn-Current-Job-Id";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CurrentJobFilter.class);

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /**
   * Sets the service registry.
   * 
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
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
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String currentJobId = httpRequest.getHeader(CURRENT_JOB_HEADER);
    try {
      if (StringUtils.isNotBlank(currentJobId)) {
        Job currentJob = serviceRegistry.getJob(Long.parseLong(currentJobId));
        serviceRegistry.setCurrentJob(currentJob);
      }
      chain.doFilter(httpRequest, httpResponse);
    } catch (Exception e) {
      logger.error("Was not able to set the current job id {} to the service registry", currentJobId);
      httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Was not able to set the current job id {} to the service registry" + currentJobId);
    } finally {
      serviceRegistry.setCurrentJob(null);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

}
