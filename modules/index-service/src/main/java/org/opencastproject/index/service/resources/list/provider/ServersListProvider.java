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
import org.opencastproject.index.service.resources.list.query.ServersListQuery;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Servers list provider. */
public class ServersListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(ServersListProvider.class);

  /** Prefix for list names. */
  private static final String PROVIDER_PREFIX = "SERVERS";
  /** Hostname list name. */
  public static final String LIST_HOSTNAME = PROVIDER_PREFIX + ".HOSTNAME";
  /** NodeName list name. */
  public static final String LIST_NODE_NAME = PROVIDER_PREFIX + ".NODE_NAME";
  /** Status list name. */
  public static final String LIST_STATUS = PROVIDER_PREFIX + ".STATUS";

  /** Prefix for filter labels. */
  private static final String FILTER_LABEL_PREFIX = "FILTERS." + PROVIDER_PREFIX;
  /** Status online filter name. */
  public static final String SERVER_STATUS_ONLINE = "online";
  /** Status online filter label. */
  public static final String SERVER_STATUS_LABEL_ONLINE = FILTER_LABEL_PREFIX + ".STATUS.ONLINE";
  /** Status offline filter name. */
  public static final String SERVER_STATUS_OFFLINE = "offline";
  /** Status offline filter label. */
  public static final String SERVER_STATUS_LABEL_OFFLINE = FILTER_LABEL_PREFIX + ".STATUS.OFFLINE";
  /** Status maintenance filter name. */
  public static final String SERVER_STATUS_MAINTENANCE = "maintenance";
  /** Status maintenance filter label. */
  public static final String SERVER_STATUS_LABEL_MAINTENANCE = FILTER_LABEL_PREFIX + ".STATUS.MAINTENANCE";

  /** The names of the different list available through this provider. */
  private static final String[] NAMES = {
    PROVIDER_PREFIX, LIST_HOSTNAME, LIST_NODE_NAME, LIST_STATUS,
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
    Map<String, String> list = new HashMap<String, String>();

    if (StringUtils.equalsIgnoreCase(LIST_STATUS, listName)) {
      list.put(SERVER_STATUS_ONLINE, SERVER_STATUS_LABEL_ONLINE);
      list.put(SERVER_STATUS_OFFLINE, SERVER_STATUS_LABEL_OFFLINE);
      list.put(SERVER_STATUS_MAINTENANCE, SERVER_STATUS_LABEL_MAINTENANCE);
      return list;
    }

    ServersListQuery serversQuery;
    try {
      serversQuery = (ServersListQuery)query;
    } catch (ClassCastException ex) {
      serversQuery = new ServersListQuery(query);
    }

    Option<String> fHostname = serversQuery.getHostname();
    Option<String> fNodeName = serversQuery.getNodeName();
    Option<String> fStatus = serversQuery.getStatus();

    List<HostRegistration> allServers;
    try {
      allServers = serviceRegistry.getHostRegistrations();
    } catch (ServiceRegistryException e) {
      throw new ListProviderException("Not able to get the list of the hosts from the services registry");
    }

    for (HostRegistration server : allServers) {
      boolean vOnline = server.isOnline();
      boolean vMaintenance = server.isMaintenanceMode();
      String vHostname = server.getBaseUrl();
      String vNodeName = server.getNodeName();

      if (fHostname.isSome() && !StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(fHostname.get()), vHostname))
        continue;

      if (fNodeName.isSome() && !StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(fNodeName.get()), vNodeName))
        continue;

      if (fStatus.isSome()) {
        switch (StringUtils.trimToEmpty(fStatus.get())) {
          case SERVER_STATUS_ONLINE:
            if (!vOnline) continue;
            break;
          case SERVER_STATUS_OFFLINE:
            if (vOnline) continue;
            break;
          case SERVER_STATUS_MAINTENANCE:
            if (!vMaintenance) continue;
            break;
          default:
            break;
        }
      }

      switch (listName) {
        case LIST_NODE_NAME:
          if (vNodeName != null)
            list.put(vNodeName, vNodeName);
          break;
        case LIST_HOSTNAME:
        default:
          list.put(vHostname, vHostname);
          break;
      }
    }

    return list;
  }

  /** OSGi service activation callback. */
  protected void activate(BundleContext bundleContext) {
    logger.info("Servers list provider activated!");
  }

  /** OSGi callback for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return StringUtils.equalsIgnoreCase(LIST_STATUS, listName);
  }

  @Override
  public String getDefault() {
    return null;
  }
}
