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
package org.opencastproject.kernel.http;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Matterhorn's shared {@link HttpContext}. All Servlet and {@link org.opencastproject.rest.StaticResource}
 * registrations should use the {@link HttpContext} that is registered with the OSGi service registry.
 */
public class SharedHttpContext implements HttpContext {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SharedHttpContext.class);

  /** This osgi bundle's bundlecontext */
  protected BundleContext bundleContext = null;

  /** Activate the component */
  public void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
    logger.debug("Shared http context activated with bundle context {}", this.bundleContext);
  }

  /** Deactivate the component */
  public void deactivate() {
    this.bundleContext = null;
    logger.debug("Shared http context deactivated");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
   */
  @Override
  public String getMimeType(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
   */
  @Override
  public URL getResource(String path) {
    throw new UnsupportedOperationException("Resources should be mounted using the StaticResource class");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Let the filters handle security. If there are none, don't let the request through
    ServiceReference[] filterRefs;
    try {
      filterRefs = bundleContext.getAllServiceReferences(Filter.class.getName(), null);
      return filterRefs != null && filterRefs.length > 0;
    } catch (InvalidSyntaxException e) {
      logger.error(e.getMessage(), e);
      return false;
    }
  }
}
