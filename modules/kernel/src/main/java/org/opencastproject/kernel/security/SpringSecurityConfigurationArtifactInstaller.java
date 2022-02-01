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

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;

/**
 * Registers a security filter, which delegates to the spring filter chain appropriate for the current request's
 * organization. Organizational security configurations may be added to the security watch directory, and should be
 * named &lt;organization_id&gt;.xml.
 */
@Component(
    immediate = true,
    service = ArtifactInstaller.class,
    property = {
        "service.description=Security Configuration Scanner"
    }
)
public class SpringSecurityConfigurationArtifactInstaller implements ArtifactInstaller {
  protected static final Logger logger = LoggerFactory.getLogger(SpringSecurityConfigurationArtifactInstaller.class);

  /** This component's bundle context */
  protected BundleContext bundleContext = null;

  /** The security filter */
  protected SecurityFilter securityFilter = null;

  /** Spring application contexts */
  protected Map<String, OsgiBundleXmlApplicationContext> appContexts = null;

  /** OSGi DI. */
  @Reference(name = "securityFilter")
  public void setSecurityFilter(SecurityFilter securityFilter) {
    this.securityFilter = securityFilter;
  }

  /**
   * OSGI activation callback
   */
  @Activate
  protected void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
    this.appContexts = new HashMap<>();
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

    OsgiBundleXmlApplicationContext springContext = new OsgiBundleXmlApplicationContext(
            new String[] { "file:" + artifact.getAbsolutePath() });
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

}
