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

package org.opencastproject.index.service.catalog.adapter;

import static java.lang.String.format;
import static org.opencastproject.util.OsgiUtil.getCfg;

import com.entwinemedia.fn.Prelude;
import org.opencastproject.index.service.catalog.adapter.events.ConfigurableEventDCCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.series.ConfigurableSeriesDCCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.series.SeriesCatalogUIAdapter;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * Based on the given service configuration this service factory will create new instances of
 * {@link SeriesCatalogUIAdapter} services and register them in the OSGi service registry.
 */
public class CatalogUIAdapterFactory implements ManagedServiceFactory {

  /** The logging facility. */
  private static final Logger logger = LoggerFactory.getLogger(CatalogUIAdapterFactory.class);

  /* The collection of keys to read from the OSGI configuration file */
  public static final String CONF_TYPE_KEY = "type";
  public static final String CONF_ORGANIZATION_KEY = "organization";
  public static final String CONF_FLAVOR_KEY = "flavor";
  public static final String CONF_TITLE_KEY = "title";

  private static final String CATALOG_TYPE_EVENTS = "events";
  private static final String CATALOG_TYPE_SERIES = "series";

  /** Map with service registrations of registered {@link SeriesCatalogUIAdapter} instances. */
  private final Map<String, ServiceRegistration> adapterServiceRegistrations = new HashMap<String, ServiceRegistration>();

  /** Reference to a {@link ListProvidersService} instance. */
  private ListProvidersService listProvidersService;

  /** Reference to a {@link SeriesService} instance. */
  private SeriesService seriesService;

  /** Reference to a {@link Workspace} instance. */
  private Workspace workspace;

  /** The OSGi bundle context. */
  private BundleContext bundleContext;

  /** OSGi callback. */
  void activate(ComponentContext cc) {
    bundleContext = cc.getBundleContext();
  }

  /** OSGi callback for deactivating component. */
  void deactivate() {
    for (ServiceRegistration serviceRegistration : adapterServiceRegistrations.values()) {
      bundleContext.ungetService(serviceRegistration.getReference());
    }
  }

  @Override
  public String getName() {
    return "Catalog UI Adapter Factory";
  }

  @Override
  public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    final String type = getCfg(properties, CONF_TYPE_KEY);

    // Check for valid configuration values
    if (!(CATALOG_TYPE_EVENTS.equalsIgnoreCase(type) || CATALOG_TYPE_SERIES.equalsIgnoreCase(type))) {
      throw new ConfigurationException(CONF_TYPE_KEY, format("The type must either be '%s' or '%s'",
              CATALOG_TYPE_EVENTS, CATALOG_TYPE_SERIES));
    }

    switch (type) {
      case CATALOG_TYPE_EVENTS: {
        if (adapterServiceRegistrations.containsKey(pid)) {
          ServiceRegistration serviceRegistration = adapterServiceRegistrations.get(pid);
          ConfigurableEventDCCatalogUIAdapter adapter = (ConfigurableEventDCCatalogUIAdapter) bundleContext
                  .getService(serviceRegistration.getReference());

          adapter.updated(properties);
        } else {
          ConfigurableEventDCCatalogUIAdapter adapter = new ConfigurableEventDCCatalogUIAdapter();
          adapter.setListProvidersService(listProvidersService);
          adapter.setWorkspace(workspace);
          adapter.updated(properties);

          ServiceRegistration configurationRegistration = bundleContext.registerService(
                  EventCatalogUIAdapter.class.getName(), adapter, null);
          adapterServiceRegistrations.put(pid, configurationRegistration);
        }
        break;
      }
      case CATALOG_TYPE_SERIES: {
        if (adapterServiceRegistrations.containsKey(pid)) {
          ServiceRegistration serviceRegistration = adapterServiceRegistrations.get(pid);
          ConfigurableSeriesDCCatalogUIAdapter adapter = (ConfigurableSeriesDCCatalogUIAdapter) bundleContext
                  .getService(serviceRegistration.getReference());
          adapter.updated(properties);
        } else {
          ConfigurableSeriesDCCatalogUIAdapter adapter = new ConfigurableSeriesDCCatalogUIAdapter();
          adapter.setListProvidersService(listProvidersService);
          adapter.setSeriesService(seriesService);
          adapter.updated(properties);

          ServiceRegistration adapterServiceRegistration = bundleContext.registerService(
                  SeriesCatalogUIAdapter.class.getName(), adapter, null);
          adapterServiceRegistrations.put(pid, adapterServiceRegistration);
        }
        break;
      }
      default:
        Prelude.unexhaustiveMatch(type);
    }

  }

  @Override
  public void deleted(String pid) {
    if (adapterServiceRegistrations.containsKey(pid)) {
      ServiceRegistration serviceRegistration = adapterServiceRegistrations.remove(pid);
      bundleContext.ungetService(serviceRegistration.getReference());
      logger.info("Service registration for PID {} removed", pid);
    }
  }

  /** OSGi callback to bind {@link ListProvidersService} instance. */
  void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }

  /** OSGi callback to bind {@link SeriesService} instance. */
  void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi callback to bind {@link Workspace} instance. */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
