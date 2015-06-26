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


package org.opencastproject.manager.system;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.StaticResource;

import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.osgi.service.http.HttpService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opencastproject.manager.servlet.MHConfigurationsServlet;

/**
 * This class activate the module.
 *
 * @author Leonid Oldenburger
 */
public class MHManagerActivator implements BundleActivator {

  /**
   * the logging facility provided by log4j
   */
  private static final Logger logger = LoggerFactory.getLogger(MHManagerActivator.class);

  /** The static resource registration */
  protected ServiceRegistration staticResourceRegistration = null;

  /**
   * Starts module.
   *
   * @param bundleContext
   * @throws Exception
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {

    logger.info("Starting Matterhorn Manager");

    ServiceReference sRef = bundleContext.getServiceReference(HttpService.class.getName());

    if (sRef != null) {

      HttpService service = (HttpService) bundleContext.getService(sRef);
      service.registerServlet("/config", new MHConfigurationsServlet(bundleContext), null, null);

    }

    StaticResource staticResource = new StaticResource(getClass().getClassLoader(), "/ui-files", "/config/ui-files", null);

    Dictionary<String, String> resourceProps = new Hashtable<String, String>();
    resourceProps.put("contextId", RestConstants.HTTP_CONTEXT_ID);
    resourceProps.put("alias", "/ui-files");

    staticResourceRegistration = bundleContext.registerService(Servlet.class.getName(), staticResource, resourceProps);

  }

  /**
   * Stops module.
   *
   * @param context
   * @throws Exception
   */
  @Override
  public void stop(BundleContext context) throws Exception {

    logger.info("Stopped Matterhorn Manager");
    staticResourceRegistration.unregister();

  }
}
