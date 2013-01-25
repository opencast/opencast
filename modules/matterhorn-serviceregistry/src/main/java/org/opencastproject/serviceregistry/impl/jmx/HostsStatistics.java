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
package org.opencastproject.serviceregistry.impl.jmx;

import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.jmx.JmxUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

public class HostsStatistics extends NotificationBroadcasterSupport implements HostsStatisticsMXBean {

  private static final Logger logger = LoggerFactory.getLogger(HostsStatistics.class);

  private static final int ONLINE = 0;
  private static final int MAINTENANCE = 1;
  private static final int OFFLINE = 2;
  private long sequenceNumber = 1;

  private Map<String, Integer> hosts = new HashMap<String, Integer>();

  public HostsStatistics(List<ServiceStatistics> statistics) {
    for (ServiceStatistics stats : statistics) {
      String host = stats.getServiceRegistration().getHost();
      boolean online = stats.getServiceRegistration().isOnline();
      boolean inMaintenanceMode = stats.getServiceRegistration().isInMaintenanceMode();
      if (!stats.getServiceRegistration().isActive()) {
        hosts.remove(host);
        logger.trace("Removing inactive host '{}'", host);
        continue;
      }
      if (online && !inMaintenanceMode) {
        hosts.put(host, ONLINE);
      } else if (online && inMaintenanceMode) {
        hosts.put(host, MAINTENANCE);
      } else {
        hosts.put(host, OFFLINE);
      }
    }
  }

  public void updateHost(HostRegistration host) {
    if (!host.isActive()) {
      hosts.remove(host.toString());
      logger.trace("Removing inactive host '{}'", host);
      return;
    }

    if (host.isOnline() && !host.isMaintenanceMode()) {
      hosts.put(host.toString(), ONLINE);
    } else if (host.isOnline() && host.isMaintenanceMode()) {
      hosts.put(host.toString(), MAINTENANCE);
    } else {
      hosts.put(host.toString(), OFFLINE);
    }

    sendNotification(JmxUtil.createUpdateNotification(this, sequenceNumber++, "Host updated"));
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    String[] types = new String[] { JmxUtil.MATTERHORN_UPDATE_NOTIFICATION };

    String name = Notification.class.getName();
    String description = "An update was executed";
    MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
    return new MBeanNotificationInfo[] { info };
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getTotalCount()
   */
  @Override
  public int getTotalCount() {
    return hosts.size();
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getOnlineCount()
   */
  @Override
  public int getOnlineCount() {
    return CollectionUtils.countMatches(hosts.values(), PredicateUtils.equalPredicate(ONLINE));
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getOfflineCount()
   */
  @Override
  public int getOfflineCount() {
    return CollectionUtils.countMatches(hosts.values(), PredicateUtils.equalPredicate(OFFLINE));
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getInMaintenanceCount()
   */
  @Override
  public int getInMaintenanceCount() {
    return CollectionUtils.countMatches(hosts.values(), PredicateUtils.equalPredicate(MAINTENANCE));
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getAll()
   */
  @Override
  public String[] getAll() {
    return hosts.keySet().toArray(new String[hosts.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getOnline()
   */
  @Override
  public String[] getOnline() {
    List<String> onlineHosts = new ArrayList<String>();
    for (Entry<String, Integer> entry : hosts.entrySet()) {
      if (entry.getValue().equals(ONLINE))
        onlineHosts.add(entry.getKey());
    }
    return onlineHosts.toArray(new String[onlineHosts.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getOffline()
   */
  @Override
  public String[] getOffline() {
    List<String> offlineHosts = new ArrayList<String>();
    for (Entry<String, Integer> entry : hosts.entrySet()) {
      if (entry.getValue().equals(OFFLINE))
        offlineHosts.add(entry.getKey());
    }
    return offlineHosts.toArray(new String[offlineHosts.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.HostsStatisticsMXBean#getInMaintenance()
   */
  @Override
  public String[] getInMaintenance() {
    List<String> maintenanceHosts = new ArrayList<String>();
    for (Entry<String, Integer> entry : hosts.entrySet()) {
      if (entry.getValue().equals(MAINTENANCE))
        maintenanceHosts.add(entry.getKey());
    }
    return maintenanceHosts.toArray(new String[maintenanceHosts.size()]);
  }

}
