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

import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.data.Tuple;
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

public class ServicesStatistics extends NotificationBroadcasterSupport implements ServicesStatisticsMXBean {

  private static final Logger logger = LoggerFactory.getLogger(ServicesStatistics.class);

  private static final String DELIMITER = ";";

  private Map<Tuple<String, String>, ServiceState> services = new HashMap<Tuple<String, String>, ServiceState>();
  private long sequenceNumber = 1;
  private final String hostName;

  public ServicesStatistics(String hostName, List<ServiceStatistics> statistics) {
    this.hostName = hostName;
    for (ServiceStatistics stats : statistics) {
      if (!stats.getServiceRegistration().isActive()) {
        logger.trace("Ignoring inactive service '{}'", stats);
        continue;
      }
      String host = stats.getServiceRegistration().getHost();
      String serviceType = stats.getServiceRegistration().getServiceType();
      ServiceState serviceState = stats.getServiceRegistration().getServiceState();
      services.put(Tuple.tuple(host, serviceType), serviceState);
    }
  }

  public void updateService(ServiceRegistration registration) {
    if (!registration.isActive()) {
      services.remove(Tuple.tuple(registration.getHost(), registration.getServiceType()));
      logger.trace("Removing inactive service '{}'", registration);
    } else {
      services.put(Tuple.tuple(registration.getHost(), registration.getServiceType()), registration.getServiceState());
    }

    sendNotification(JmxUtil.createUpdateNotification(this, sequenceNumber++, "Service updated"));
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
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getServiceCount()
   */
  @Override
  public int getServiceCount() {
    return services.size();
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getServiceCountByNode()
   */
  @Override
  public int getServiceCountByNode() {
    int i = 0;
    for (Tuple<String, String> key : services.keySet()) {
      if (key.getA().equals(hostName))
        i++;
    }
    return i;
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getNormalServiceCountByNode()
   */
  @Override
  public int getNormalServiceCountByNode() {
    int i = 0;
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (entry.getKey().getA().equals(hostName) && ServiceState.NORMAL.equals(entry.getValue()))
        i++;
    }
    return i;
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getWarningServiceCountByNode()
   */
  @Override
  public int getWarningServiceCountByNode() {
    int i = 0;
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (entry.getKey().getA().equals(hostName) && ServiceState.WARNING.equals(entry.getValue()))
        i++;
    }
    return i;
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getErrorServiceCountByNode()
   */
  @Override
  public int getErrorServiceCountByNode() {
    int i = 0;
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (entry.getKey().getA().equals(hostName) && ServiceState.ERROR.equals(entry.getValue()))
        i++;
    }
    return i;
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getNormalServiceCount()
   */
  @Override
  public int getNormalServiceCount() {
    return CollectionUtils.countMatches(services.values(), PredicateUtils.equalPredicate(ServiceState.NORMAL));
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getWarningServiceCount()
   */
  @Override
  public int getWarningServiceCount() {
    return CollectionUtils.countMatches(services.values(), PredicateUtils.equalPredicate(ServiceState.WARNING));
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getErrorServiceCount()
   */
  @Override
  public int getErrorServiceCount() {
    return CollectionUtils.countMatches(services.values(), PredicateUtils.equalPredicate(ServiceState.ERROR));
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getServices()
   */
  @Override
  public String[] getServices() {
    List<String> serviceList = new ArrayList<String>();
    for (Tuple<String, String> key : services.keySet()) {
      serviceList.add(key.getA() + DELIMITER + key.getB());
    }
    return serviceList.toArray(new String[serviceList.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getServicesByNode()
   */
  @Override
  public String[] getServicesByNode() {
    List<String> serviceList = new ArrayList<String>();
    for (Tuple<String, String> key : services.keySet()) {
      if (key.getA().equals(hostName))
        serviceList.add(key.getA() + DELIMITER + key.getB());
    }
    return serviceList.toArray(new String[serviceList.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getNormalServices()
   */
  @Override
  public String[] getNormalServices() {
    List<String> normalServices = new ArrayList<String>();
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (ServiceState.NORMAL.equals(entry.getValue()))
        normalServices.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB());
    }
    return normalServices.toArray(new String[normalServices.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getNormalServicesByNode()
   */
  @Override
  public String[] getNormalServicesByNode() {
    List<String> normalServices = new ArrayList<String>();
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (entry.getKey().getA().equals(hostName) && ServiceState.NORMAL.equals(entry.getValue()))
        normalServices.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB());
    }
    return normalServices.toArray(new String[normalServices.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getWarningServices()
   */
  @Override
  public String[] getWarningServices() {
    List<String> warningServices = new ArrayList<String>();
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (ServiceState.WARNING.equals(entry.getValue()))
        warningServices.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB());
    }
    return warningServices.toArray(new String[warningServices.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getWarningServicesByNode()
   */
  @Override
  public String[] getWarningServicesByNode() {
    List<String> warningServices = new ArrayList<String>();
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (entry.getKey().getA().equals(hostName) && ServiceState.WARNING.equals(entry.getValue()))
        warningServices.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB());
    }
    return warningServices.toArray(new String[warningServices.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getErrorServices()
   */
  @Override
  public String[] getErrorServices() {
    List<String> erroServices = new ArrayList<String>();
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (ServiceState.ERROR.equals(entry.getValue()))
        erroServices.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB());
    }
    return erroServices.toArray(new String[erroServices.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.ServicesStatisticsMXBean#getErrorServicesByNode()
   */
  @Override
  public String[] getErrorServicesByNode() {
    List<String> erroServices = new ArrayList<String>();
    for (Entry<Tuple<String, String>, ServiceState> entry : services.entrySet()) {
      if (entry.getKey().getA().equals(hostName) && ServiceState.ERROR.equals(entry.getValue()))
        erroServices.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB());
    }
    return erroServices.toArray(new String[erroServices.size()]);
  }

}
