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

package org.opencastproject.util.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

import javax.management.Notification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * Functions supporting JMX.
 */
public final class JmxUtil {

  public static final String OPENCAST_UPDATE_NOTIFICATION = "org.opencastproject.update";

  private static final String MATTERORN_JMX_DOMAIN = "org.opencastproject";
  private static final Logger logger = LoggerFactory.getLogger(JmxUtil.class);

  private JmxUtil() {
  }

  public static <A> ObjectInstance registerMXBean(A bean, String type) {
    try {
      logger.info("Registering {} with JMX", bean.getClass().getName());
      return ManagementFactory.getPlatformMBeanServer().registerMBean(bean,
              new ObjectName(MATTERORN_JMX_DOMAIN + ":type=" + type));
    } catch (Exception e) {
      logger.warn("Unable to register {} as an mbean: {}", bean, e);
    }
    return null;
  }

  public static void unregisterMXBean(ObjectInstance bean) {
    logger.info("Unregistering {} with JMX", bean.getClassName());
    try {
      if (ManagementFactory.getPlatformMBeanServer().isRegistered(bean.getObjectName())) {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(bean.getObjectName());
      }
    } catch (Exception e) {
      logger.warn("Unable to unregister mbean {}: {}", bean.getClassName(), e);
    }
  }

  public static Notification createUpdateNotification(Object source, long sequenceNumber, String message) {
    return new Notification(OPENCAST_UPDATE_NOTIFICATION, source, sequenceNumber, System.currentTimeMillis(), message);
  }

}
