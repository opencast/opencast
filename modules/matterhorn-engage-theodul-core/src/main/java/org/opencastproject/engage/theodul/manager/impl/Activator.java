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
package org.opencastproject.engage.theodul.manager.impl;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import org.apache.commons.lang.StringUtils;
import org.opencastproject.engage.theodul.api.EngagePluginManager;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.StaticResource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>BundleActivator</code> for the Theodul Core bundle. This has become
 * necessary since the because the static resources of the player should be
 * overridable by contents from filesystem. The solution for this is to register
 * this bundles static resouces with a modified ClassLoader, this must be done
 * programmatically. Hence manual bundle start via Activator was necessary.
 *
 */
public class Activator implements BundleActivator {

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

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    // ServiceRegistrations for the components we activate, needed for deactivation
    private ServiceRegistration registrationPluginManager;
    private ServiceRegistration registrationRestEndpoint;
    private ServiceRegistration registrationStaticResources;

    @Override
    public void start(BundleContext bc) throws Exception {
        logger.info("Starting Theodul Core Bundle.");
        registerStaticResources(getOverrideDir(bc), bc);
        registerServices(bc);
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        logger.info("Stopping Theodul Core Bundle.");
        unregisterServices();
        unregisterStaticResources(bc);
    }

    /**
     * Tries to get the override path from the system configuration, if not
     * available <code>DEFAULT_OVERRIDE_DIR</code> is used instead.
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

    private void registerServices(BundleContext bc) {
        // register plugin manager
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("service.description", "Service that manages plugins for the Theodul player");
        EngagePluginManagerImpl manager = new EngagePluginManagerImpl();
        manager.activate(bc);
        registrationPluginManager = bc.registerService(EngagePluginManager.class.getName(), manager, props);

        // register plugin manager endpoint
        props = new Hashtable<String, String>();
        props.put("service.description", "Theodul Plugin Manager REST Endpoint");
        props.put("opencast.service.type", "org.opencastproject.engage.plugin.manager");
        props.put("opencast.service.path", "/engage/theodul/manager");
        EngagePluginManagerRestService endpoint = new EngagePluginManagerRestService();
        endpoint.setPluginManager(manager);
        registrationRestEndpoint = bc.registerService(EngagePluginManagerRestService.class.getName(), endpoint, props);
        endpoint.activate();  // was DS before, keeping it for the activation message
    }

    private void unregisterServices() {
        registrationRestEndpoint.unregister();
        registrationPluginManager.unregister();
    }

    private void registerStaticResources(File overrideDir, BundleContext bc) {
        // build properties
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("contextId", RestConstants.HTTP_CONTEXT_ID);
        props.put("alias", URL_ALIAS);

        // instantiate Servelet that delivers the resources
        StaticResource staticResource = new StaticResource(
                new StaticResourceClassloader(bc.getBundle(), overrideDir, UI_CLASSPATH),
                UI_CLASSPATH, URL_ALIAS, UI_WELCOME_FILE);

        // register Servelet as a Service
        registrationStaticResources = bc.registerService(Servlet.class.getName(), staticResource, props);
    }

    private void unregisterStaticResources(BundleContext bc) {
        registrationStaticResources.unregister();
    }
}
