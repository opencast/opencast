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

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.SecurityService;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Filter;

/**
 * Registers a security filter, which delegates to the spring filter chain appropriate for the current request's
 * organization. Organizational security configurations may be added to the security watch directory, and should be
 * named <organization_id>.xml.
 */
public class SpringSecurityConfigurationArtifactInstaller implements ArtifactInstaller {
  protected static final Logger logger = LoggerFactory.getLogger(SpringSecurityConfigurationArtifactInstaller.class);

  /** This component's bundle context */
  protected BundleContext bundleContext = null;

  /** The security filter */
  protected SecurityFilter securityFilter = null;

  /** The security filter's service registration */
  protected ServiceRegistration filterRegistration = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** Spring application contexts */
  protected Map<String, OsgiBundleXmlApplicationContext> appContexts = null;

  /**
   * OSGI activation callback
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
    this.appContexts = new HashMap<String, OsgiBundleXmlApplicationContext>();

    // Register the security filter
    securityFilter = new SecurityFilter(securityService);
    Dictionary props = new Hashtable<String, Boolean>();
    props.put("contextId", RestConstants.HTTP_CONTEXT_ID);
    props.put("pattern", ".*");
    props.put("service.ranking", "2");
    filterRegistration = bundleContext.registerService(Filter.class.getName(), securityFilter, props);
  }

  /**
   * OSGI deactivation callback
   */
  protected void deactivate() {
    if (filterRegistration != null) {
      filterRegistration.unregister();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "security".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".xml");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public void install(File artifact) throws Exception {
    // If we already have a registration for this ID, take it out of the security filter and close it
    String orgId = FilenameUtils.getBaseName(artifact.getName());
    OsgiBundleXmlApplicationContext orgAppContext = appContexts.get(orgId);
    if (orgAppContext != null) {
      securityFilter.removeFilter(orgId);
      orgAppContext.close();
    }

    OsgiBundleXmlApplicationContext springContext = new OsgiBundleXmlApplicationContext(new String[] { "file:"
            + artifact.getAbsolutePath() });
    springContext.setBundleContext(bundleContext);
    logger.info("registered {} for {}", springContext, orgId);

    // Refresh the spring application context
    try {
      springContext.refresh();
    } catch (Exception e) {
      logger.error("Unable to refresh spring security configuration file {}: {}", artifact, e);
      return;
    }

    // Keep track of the app context so we can close it later
    appContexts.put(orgId, springContext);

    // Add the filter chain for this org to the security filter
    securityFilter.addFilter(orgId, (Filter) springContext.getBean("springSecurityFilterChain"));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    String orgId = FilenameUtils.getBaseName(artifact.getName());
    OsgiBundleXmlApplicationContext appContext = appContexts.get(orgId);
    if (appContext != null) {
      securityFilter.removeFilter(orgId);
      appContexts.remove(orgId);
      appContext.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    install(artifact);
  }

  /**
   * Sets the security service.
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
