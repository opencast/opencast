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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util.jmx

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory

import javax.management.Notification
import javax.management.ObjectInstance
import javax.management.ObjectName

/**
 * Functions supporting JMX.
 */
object JmxUtil {

    val OPENCAST_UPDATE_NOTIFICATION = "org.opencastproject.update"

    private val MATTERORN_JMX_DOMAIN = "org.opencastproject"
    private val logger = LoggerFactory.getLogger(JmxUtil::class.java!!)

    fun <A> registerMXBean(bean: A, type: String): ObjectInstance? {
        try {
            logger.info("Registering {} with JMX", bean.javaClass.getName())
            return ManagementFactory.getPlatformMBeanServer().registerMBean(bean,
                    ObjectName("$MATTERORN_JMX_DOMAIN:type=$type"))
        } catch (e: Exception) {
            logger.warn("Unable to register {} as an mbean: {}", bean, e)
        }

        return null
    }

    fun unregisterMXBean(bean: ObjectInstance) {
        logger.info("Unregistering {} with JMX", bean.className)
        try {
            if (ManagementFactory.getPlatformMBeanServer().isRegistered(bean.objectName)) {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(bean.objectName)
            }
        } catch (e: Exception) {
            logger.warn("Unable to unregister mbean {}: {}", bean.className, e)
        }

    }

    fun createUpdateNotification(source: Any, sequenceNumber: Long, message: String): Notification {
        return Notification(OPENCAST_UPDATE_NOTIFICATION, source, sequenceNumber, System.currentTimeMillis(), message)
    }

}
