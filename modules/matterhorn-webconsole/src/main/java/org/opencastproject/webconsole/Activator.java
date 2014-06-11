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
package org.opencastproject.webconsole;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.StaticResource;

import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

/**
 * Activates the Felix web console, which has been customized for use in matterhorn
 */
public class Activator implements BundleActivator {
  /** The static resource registration */
  protected ServiceRegistration staticResourceRegistration = null;

  /** The web console registration */
  protected ServiceRegistration webConsoleRegistration;

  /** The web console servlet */
  protected OsgiManager manager;

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    StaticResource staticResource = new StaticResource(getClass().getClassLoader(), "/res", "/system/console/res", null);
    Dictionary<String, String> resourceProps = new Hashtable<String, String>();
    resourceProps.put("contextId", RestConstants.HTTP_CONTEXT_ID);
    resourceProps.put("alias", "/system/console/res");
    staticResourceRegistration = bundleContext.registerService(Servlet.class.getName(), staticResource, resourceProps);

    manager = new WebConsole(bundleContext);
    Dictionary<String, String> consoleProps = new Hashtable<String, String>();
    consoleProps.put("alias", "/system/console");
    consoleProps.put("contextId", RestConstants.HTTP_CONTEXT_ID);
    webConsoleRegistration = bundleContext.registerService(Servlet.class.getName(), manager, consoleProps);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    staticResourceRegistration.unregister();
    webConsoleRegistration.unregister();
    manager.destroy();
  }
}
