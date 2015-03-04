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
package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServersListProvider implements ResourceListProvider {

  public static final String PROVIDER_PREFIX = "SERVERS";

  public static final String FILTER_ONLINE = "ONLINE";
  public static final String FILTER_OFFLINE = "OFFLINE";
  public static final String FILTER_MAINTENANCE = "MAINTENANCE";
  public static final String FILTER_MAXJOBS = "MAXJOBS";
  public static final String FILTER_CORES = "CORES";
  public static final String FILTER_MEMORY = "MEMORY";
  public static final String FILTER_PATH = "PATH";
  public static final String FILTER_TEXT = "Q";

  public static final String LIST_ONLINE = PROVIDER_PREFIX + "." + FILTER_ONLINE;
  public static final String LIST_OFFLINE = PROVIDER_PREFIX + "." + FILTER_OFFLINE;
  public static final String LIST_MAINTENANCE = PROVIDER_PREFIX + "." + FILTER_MAINTENANCE;
  public static final String LIST_MAXJOBS = PROVIDER_PREFIX + "." + FILTER_MAXJOBS;
  public static final String LIST_CORES = PROVIDER_PREFIX + "." + FILTER_CORES;
  public static final String LIST_MEMORY = PROVIDER_PREFIX + "." + FILTER_MEMORY;
  public static final String LIST_PATH = PROVIDER_PREFIX + "." + FILTER_PATH;
  public static final String LIST_TEXT = PROVIDER_PREFIX + "." + FILTER_PATH;

  /** The list of filter criteria for this provider */
  public static enum SERVERS_FILTER_LIST {
    CORES, MAXJOBS, MEMORY, PATH, SERVICE, HOSTNAME;
  };

  /** The names of the different list available through this provider */
  private List<String> listNames;

  private ServiceRegistry serviceRegistry;

  private static final Logger logger = LoggerFactory.getLogger(ServersListProvider.class);

  protected void activate(BundleContext bundleContext) {
    logger.info("Servers list provider activated!");
    listNames = new ArrayList<String>();

    // Fill the list names
    for (SERVERS_FILTER_LIST value : SERVERS_FILTER_LIST.values()) {
      listNames.add(getListNameFromFilter(value));
    }

    // Standard list
    listNames.add(PROVIDER_PREFIX);
  }

  /** OSGi callback for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public String[] getListNames() {
    return listNames.toArray(new String[listNames.size()]);
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> list = new HashMap<String, Object>();

    // Get list name
    SERVERS_FILTER_LIST listValue;
    if (PROVIDER_PREFIX.equals(listName)) {
      listValue = SERVERS_FILTER_LIST.HOSTNAME;
    } else {
      try {
        listValue = SERVERS_FILTER_LIST.valueOf(listName.replace(PROVIDER_PREFIX + ".", "").toUpperCase());
      } catch (IllegalArgumentException e) {
        logger.warn("List name '{}' unavailable for jobs list provider: {}", listName, e);
        listValue = SERVERS_FILTER_LIST.HOSTNAME;
      }
    }

    // Get filters
    int offset = 0;
    int limit = 0;

    boolean fOnline = query.hasFilter(FILTER_ONLINE) ? (Boolean) query.getFilter(FILTER_ONLINE).getValue().get()
            : false;
    boolean fOffline = query.hasFilter(FILTER_OFFLINE) ? (Boolean) query.getFilter(FILTER_OFFLINE).getValue().get()
            : false;
    boolean fMaintenance = query.hasFilter(FILTER_MAINTENANCE) ? (Boolean) query.getFilter(FILTER_MAINTENANCE)
            .getValue().get() : false;
    Integer fMaxJobs = query.hasFilter(FILTER_MAXJOBS) ? (Integer) query.getFilter(FILTER_MAXJOBS).getValue().get()
            : -1;
    Integer fCores = query.hasFilter(FILTER_CORES) ? (Integer) query.getFilter(FILTER_CORES).getValue().get() : -1;
    Integer fMemory = query.hasFilter(FILTER_MEMORY) ? (Integer) query.getFilter(FILTER_MEMORY).getValue().get() : -1;
    String fPath = query.hasFilter(FILTER_PATH) ? (String) query.getFilter(FILTER_PATH).getValue().get() : null;
    String fText = query.hasFilter(FILTER_TEXT) ? (String) query.getFilter(FILTER_TEXT).getValue().get() : null;

    if (query.getOffset().isSome())
      offset = query.getOffset().get();

    if (query.getLimit().isSome())
      limit = query.getOffset().get();

    List<HostRegistration> allServers;
    try {
      allServers = serviceRegistry.getHostRegistrations();
    } catch (ServiceRegistryException e) {
      throw new ListProviderException("Not able to get the list of the hosts from the services registry");
    }

    int i = 0;
    for (HostRegistration server : allServers) {
      if (i++ < offset) {
        continue;
      } else if (limit != 0 && list.size() == limit) {
        break;
      } else {

        // Get all the services statistics pro host
        // TODO improve the service registry to get service statistics by host
        List<ServiceStatistics> servicesStatistics = null;
        try {
          servicesStatistics = serviceRegistry.getServiceStatistics();
        } catch (ServiceRegistryException e) {
          throw new ListProviderException("Not able to get the list of the hosts from the services registry");
        }
        Map<String, Object> services = new HashMap<String, Object>();
        for (ServiceStatistics serviceStat : servicesStatistics) {
          if (server.getBaseUrl().equals(serviceStat.getServiceRegistration().getHost())) {
            String service = serviceStat.getServiceRegistration().getServiceType();
            services.put(service, service);
          }
        }

        boolean vOnline = server.isOnline();
        boolean vMaintenance = server.isMaintenanceMode();
        String vName = server.getBaseUrl();
        int vCores = server.getCores();
        int vMaxJobs = server.getMaxJobs();

        if (fOffline && vOnline)
          continue;
        if (fOnline && !vOnline)
          continue;
        if (fMaintenance && !vMaintenance)
          continue;
        if (fMaxJobs > 0 && fMaxJobs < vMaxJobs)
          continue;
        if (fCores != null && fCores > 0 && fCores != vCores)
          continue;
        if (fMemory != null && fMemory > 0 && ((Integer) fMemory).longValue() != server.getMemory())
          continue;
        if (StringUtils.isNotBlank(fPath) && !vName.toLowerCase().contains(fPath.toLowerCase()))
          continue;
        if (StringUtils.isNotBlank(fText) && !vName.toLowerCase().contains(fText.toLowerCase())) {
          String allString = vName.toLowerCase().concat(server.getIpAddress().toLowerCase());
          if (!allString.contains(fText.toLowerCase()))
            continue;
        }

        switch (listValue) {
          case CORES:
            list.put(Integer.toString(vCores), Integer.toString(vCores));
            break;
          case HOSTNAME:
            list.put(vName, vName);
            break;
          case MAXJOBS:
            list.put(Integer.toString(vMaxJobs), Integer.toString(vMaxJobs));
            break;
          case MEMORY:
            list.put(Long.toString(server.getMemory()), Long.toString(server.getMemory()));
            break;
          case SERVICE:
            list.putAll(services);
            break;
          case PATH:
          default:
            list.put(vName, vName);
            break;
        }
      }
    }

    return list;
  }

  /**
   * Returns the list name related to the given filter
   * 
   * @param filter
   *          the filter from which the list name is needed
   * @return the list name related to the givne filter
   */
  public static String getListNameFromFilter(SERVERS_FILTER_LIST filter) {
    return PROVIDER_PREFIX + "." + filter.toString();
  }

  /**
   * Get all the names of all the different list with the prefix available with this provider
   * 
   * @return an string array containing the list names
   */
  public static String[] getAvailableFilters() {
    String[] list = new String[SERVERS_FILTER_LIST.values().length];
    int i = 0;
    for (SERVERS_FILTER_LIST value : SERVERS_FILTER_LIST.values()) {
      list[i++] = getListNameFromFilter(value);
    }
    return list;

  }
}
