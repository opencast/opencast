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

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.servlet.Servlet;
import org.opencastproject.engage.theodul.api.EngagePlugin;
import org.opencastproject.engage.theodul.api.EngagePluginManager;
import org.opencastproject.engage.theodul.api.EngagePluginRegistration;
import org.opencastproject.engage.theodul.api.EngagePluginRestService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.StaticResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that tracks the de-/registration of Theodul Player Plugins and
 * de-/installs static resource and REST endpoint servlets under a shared URL.
 */
public class EngagePluginManagerImpl implements EngagePluginManager, ServiceListener {

  private static final Logger log = LoggerFactory.getLogger(EngagePluginManagerImpl.class);
  static final String PLUGIN_URL_PREFIX = "/engage/theodul/plugin/";
  private BundleContext bundleContext;
  private static final String pluginServiceFilter = "(objectClass=" + EngagePlugin.class.getName() + ")";
  private final PluginDataStore plugins = new PluginDataStore();

  protected void activate(BundleContext bc) {
    bundleContext = bc;

    try {
      bundleContext.addServiceListener(this, pluginServiceFilter);
    } catch (InvalidSyntaxException ex) {
      log.error("Could not register as ServiceListener: " + ex.getMessage());
      throw new RuntimeException(ex);
    }
    log.info("Activated. Listening for Theodul Plugins. filter=" + pluginServiceFilter);
  }

  protected void deactivate(ComponentContext cc) {
    cc.getBundleContext().removeServiceListener(this);
    uninstallAll();
    log.info("Deactivated.");
  }

  @Override
  public void serviceChanged(ServiceEvent se) {
    ServiceReference sref = se.getServiceReference();

    switch (se.getType()) {

      case ServiceEvent.REGISTERED:
        try {
          installPlugin(sref);
        } catch (Exception e) {
          log.error("Failed to install Theodul Plugin: " + e.getMessage(), e);
        }
        break;

      case ServiceEvent.UNREGISTERING:
        try {
          uninstallPlugin(sref);
        } catch (Exception e) {
          log.error("Error while uninstalling Theodul Plugin: " + e.getMessage(), e);
        }
        break;
      default:
          break;
    }
  }

  private void installPlugin(ServiceReference sref) throws IllegalArgumentException {
    PluginData plugin = new PluginData(sref);

    // try to install static resources if availabel
    if (plugin.providesStaticResources()) {
      try {
        plugin.setStaticResourceRegistration(installStaticResources(plugin));
      } catch (Exception ex) {
        log.warn("Unable to install static resources.", ex);
      }
    }

    // try to install REST endpoint if availabel
    if (plugin.providesRestEndpoint()) {
      try {
        plugin.setRestEndpointRegistration(installRestEndpoint(plugin));
      } catch (Exception ex) {
        log.warn("Unable to install REST endpoint.", ex);
      }
    }

    // make sure we have no useless plugin after all
    if (plugin.getStaticResourceRegistration() == null
            && plugin.getRestEndpointRegistration() == null) {
      throw new IllegalStateException("Neither static resources nor a REST endpoint were registered, canceling plugin installation");
    }

    plugins.add(plugin);

    // construct and log success message
    log.info("Installed Theodul plugin {} (static: {} REST: {})", new Object[] {
        plugin.getName(),
        (plugin.getStaticResourceRegistration() != null) ? plugin.getStaticResourcesPath() : "no",
        (plugin.getRestEndpointRegistration() != null) ? plugin.getRestPath() : "no"
    });
  }

  /** Registers a <code>StaticResource</code> that serves the contents of the
   * plugins /static resource directory.
   *
   * @returns ServiceRegistration for the StaticResource
   */
  private ServiceRegistration installStaticResources(PluginData plugin) throws Exception {
    StaticResource staticResource = new StaticResource(
            new BundleDelegatingClassLoader(plugin.getServiceReference().getBundle()),
            EngagePlugin.STATIC_RESOURCES_PATH, plugin.getStaticResourcesPath(), null);
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("contextId", RestConstants.HTTP_CONTEXT_ID);
    props.put("alias", PLUGIN_URL_PREFIX + plugin.getStaticResourcesPath());
    return bundleContext.registerService(Servlet.class.getName(), staticResource, props);
  }

  /** Publishes the REST endpoint implemented by the plugin bundle.
   *
   * @returns ServiceRegistration for the REST endpoint
   */
  private ServiceRegistration installRestEndpoint(PluginData plugin) throws Exception {
    EngagePlugin service = (EngagePlugin) bundleContext.getService(plugin.getServiceReference());
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("service.description", plugin.getDescription());
    props.put("opencast.service.type", "org.opencast.engage.plugin." + Integer.toString(plugin.getPluginID()));
    props.put("opencast.service.path", PLUGIN_URL_PREFIX + plugin.getRestPath());
    return bundleContext.registerService(EngagePluginRestService.class.getName(), service, props);
  }

  private void uninstallPlugin(ServiceReference sref) {
    PluginData plugin = plugins.getByServiceReference(sref);
    if (plugin != null) {

      // uninstall static resources
      ServiceRegistration staticReg = plugin.getStaticResourceRegistration();
      if (staticReg != null) {
        log.info("Unregistering static resources for plugin " + plugin.getName());
        staticReg.unregister();
      }

      // uninstall REST endpoint
      ServiceRegistration restReg = plugin.getRestEndpointRegistration();
      if (restReg != null) {
        log.info("Unregistering REST endpoint for plugin " + plugin.getName());
        restReg.unregister();
      }

      plugins.remove(plugin);
    } else {
      throw new IllegalArgumentException("Unable to uninstall plugin. No plugin registered with the given ServiceReference.");
    }
  }

  private void uninstallAll() {
    for (PluginData plugin : plugins.getAll()) {
      uninstallPlugin(plugin.getServiceReference());
    }
  }

  @Override
  public List<EngagePluginRegistration> getAllRegisteredPlugins() {
    synchronized (plugins) {
      List<EngagePluginRegistration> list = new ArrayList<EngagePluginRegistration>();
      for (PluginData plugin : plugins.getAll()) {
        EngagePluginRegistrationImpl reg = new EngagePluginRegistrationImpl(
                plugin.getPluginID(), plugin.getName(), plugin.getDescription(),
                plugin.providesStaticResources() ? plugin.getStaticResourcesPath() : null,
                plugin.providesRestEndpoint() ? plugin.getRestPath() : null);
        list.add(reg);
      }
      return list;
    }
  }

  class BundleDelegatingClassLoader extends ClassLoader {

    private Bundle bundle;

    public BundleDelegatingClassLoader(Bundle bundle) {
      super();
      this.bundle = bundle;
    }

    @Override
    public URL getResource(String path) {
      return bundle.getResource(path);
    }
  }

  class PluginDataStore {

    private final Set<PluginData> data = new HashSet<PluginData>();

    public synchronized int size() {
      return data.size();
    }

    public synchronized void add(PluginData p) {
      data.add(p);
    }

    public synchronized void remove(PluginData p) {
      data.remove(p);
    }

    public synchronized PluginData[] getAll() {
      return data.toArray(new PluginData[0]);
    }

    public synchronized PluginData getByName(String name) {
      for (PluginData p : data) {
        if (p.getName().equals(name)) {
          return p;
        }
      }
      return null;
    }

    public boolean containsWithName(String name) {
      return null != getByName(name);
    }

    public synchronized PluginData getByPath(String path) {
      for (PluginData p : data) {
        if (p.getName().equals(path)) {
          return p;
        }
      }
      return null;
    }

    public boolean containsWithPath(String path) {
      return null != getByPath(path);
    }

    public synchronized PluginData getByServiceReference(ServiceReference sref) {
      for (PluginData p : data) {
        if (p.getServiceReference().equals(sref)) {
          return p;
        }
      }
      return null;
    }
  }
}
