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

package org.opencastproject.engage.theodul.manager.impl;

import static org.joda.time.DateTimeConstants.MILLIS_PER_SECOND;

import org.opencastproject.kernel.rest.RestPublisher;
import org.opencastproject.rest.StaticResource;
import org.opencastproject.util.OsgiUtil;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Service that registers the static resources required by the Theodul player with an optional local override directory.
 */
public class TheodulCoreStaticResources {

  // property key for configuration of override path
  static final String PROPKEY_OVERRIDE_DIR = "org.opencastproject.engage.theodul.resource.path";

  // default theodul resource override directory
  static final String DEFAULT_OVERRIDE_DIR = "theodul.web";

  // path under which the static resources will be served
  static final String URL_ALIAS = "/engage/theodul/ui";

  // classpath under which resources are found inside bundle
  static final String UI_CLASSPATH = "/ui";

  // welcome page
  static final String UI_WELCOME_FILE = "index.html";

  private static final Logger logger = LoggerFactory.getLogger(TheodulCoreStaticResources.class);

  private ServiceRegistration registrationStaticResources;

  public void activate(BundleContext bc) throws Exception {
    logger.info("Starting Theodul Core Bundle.");
    registerStaticResources(getOverrideDir(bc), bc);
  }

  public void deactivate(BundleContext bc) throws Exception {
    logger.info("Stopping Theodul Core Bundle.");
    unregisterStaticResources(bc);
  }

  /**
   * Tries to get the override path from the system configuration, if not available <code>DEFAULT_OVERRIDE_DIR</code> is
   * used instead.
   *
   * @return File override directory
   */
  private File getOverrideDir(BundleContext bc) {
    String path = bc.getProperty(PROPKEY_OVERRIDE_DIR);
    if (StringUtils.trimToNull(path) == null) {
      path = DEFAULT_OVERRIDE_DIR;
    }
    File dir = new File(path);
    return dir;
  }

  private void registerStaticResources(File overrideDir, BundleContext bc) {
    StaticResource staticResource = new StaticResource(
            new StaticResourceClassloader(bc.getBundle(), overrideDir, UI_CLASSPATH), UI_CLASSPATH, URL_ALIAS,
            UI_WELCOME_FILE);
    registrationStaticResources = OsgiUtil.registerServlet(getKernelBundleContext(), staticResource, URL_ALIAS);
  }

  private BundleContext getKernelBundleContext() {
    BundleContext context = FrameworkUtil.getBundle(RestPublisher.class).getBundleContext();
    while (context == null) {
      logger.info("Waiting for the kernel bundle to become active...");
      try {
        Thread.sleep(MILLIS_PER_SECOND);
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for kernel bundle");
      }
      context = FrameworkUtil.getBundle(RestPublisher.class).getBundleContext();
    }
    return context;
  }

  private void unregisterStaticResources(BundleContext bc) {
    registrationStaticResources.unregister();
  }
}
