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

package org.opencastproject.index.service.resources.list.provider;


import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.ServicesListQuery;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.util.SmartIterator;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Services list provider. */
public class ServicesListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(SeriesListProvider.class);

  /** Prefix for list names. */
  private static final String PROVIDER_PREFIX = "SERVICES";
  /** Name list name. */
  public static final String LIST_NAME = PROVIDER_PREFIX + ".NAME";
  /** Status list name. */
  public static final String LIST_STATUS = PROVIDER_PREFIX + ".STATUS";

  /** Prefix for filter labels. */
  private static final String FILTER_LABEL_PREFIX = "FILTERS." + PROVIDER_PREFIX;
  /** Service status filter label prefix. */
  public static final String SERVICE_STATUS_FILTER_PREFIX = FILTER_LABEL_PREFIX + ".STATUS.";

  /** The names of the different list available through this provider. */
  private static final String[] NAMES = {
    PROVIDER_PREFIX, LIST_NAME, LIST_STATUS,
  };

  /** Service registry instance. */
  private ServiceRegistry serviceRegistry;

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query)
          throws ListProviderException {

    ServicesListQuery servicesQuery;
    try {
      servicesQuery = (ServicesListQuery) query;
    } catch (ClassCastException ex) {
      servicesQuery = new ServicesListQuery(query);
    }

    Map<String, String> result = new HashMap<String,String>();
    if (LIST_STATUS.equals(listName)) {
      for (ServiceState s : ServiceState.values()) {
        result.put(s.toString(), SERVICE_STATUS_FILTER_PREFIX + s.toString());
      }
      return result;
    }

    List<ServiceRegistration> serviceRegistrations;
    try {
      serviceRegistrations = serviceRegistry.getServiceRegistrations();
    } catch (ServiceRegistryException ex) {
      throw new ListProviderException("Failed to get service registrations.", ex);
    }

    for (ServiceRegistration serviceRegistration : serviceRegistrations) {
      if (servicesQuery.getHostname().isSome()
              && !StringUtils.equals(servicesQuery.getHostname().get(), serviceRegistration.getHost()))
        continue;

      if (servicesQuery.getActions().isSome()
              && servicesQuery.getActions().get()
              && serviceRegistration.getServiceState() == ServiceState.NORMAL)
        continue;

      result.put(serviceRegistration.getServiceType(), serviceRegistration.getServiceType());
    }

    if (servicesQuery.getLimit().isSome() || servicesQuery.getLimit().isSome()) {
      int limit = servicesQuery.getLimit().getOrElse(0);
      int offset = servicesQuery.getOffset().getOrElse(0);
      result = new SmartIterator(limit, offset).applyLimitAndOffset(result);
    }

    return result;
  }

  /** OSGi service activation callback. */
  protected void activate(BundleContext bundleContext) {
    logger.info("Services list provider activated!");
  }

  /** OSGi callback for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return LIST_STATUS.equals(listName);
  }

  @Override
  public String getDefault() {
    return null;
  }
}
