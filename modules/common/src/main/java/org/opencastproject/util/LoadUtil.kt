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

package org.opencastproject.util

import org.opencastproject.serviceregistry.api.HostRegistration
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Dictionary

object LoadUtil {

    /** The logging instance  */
    private val logger = LoggerFactory.getLogger(LoadUtil::class.java!!)

    fun getConfiguredLoadValue(bundleProperties: Dictionary<*, *>, configKey: String, defaultValue: Float?, registry: ServiceRegistry): Float {
        val jobLoad = StringUtils.trimToNull(bundleProperties.get(configKey) as String)
        var loadValue = defaultValue!!
        if (jobLoad != null) {
            try {
                loadValue = java.lang.Float.parseFloat(jobLoad)
                if (loadValue < 0) {
                    logger.warn("Load value for key {} set to less than 0, defaulting to 0", configKey)
                    loadValue = 0.0f
                }
                logger.info("Set load for key {} to {}", configKey, loadValue)
            } catch (e: NumberFormatException) {
                logger.warn("Can not set job loads to {}. {} must be a float", jobLoad, configKey)
                loadValue = defaultValue
                logger.info("Set load for key {} to default of {}", configKey, loadValue)
            }

        } else {
            logger.info("No job load configuration found for key {}, load to default of {}", configKey, loadValue)
        }
        try {
            checkJobFitsCluster(loadValue, configKey, registry.hostRegistrations)
        } catch (e: ServiceRegistryException) {
            logger.warn("Unable to verify that {} will run on this cluster due to load of {}", configKey, loadValue)
        }

        return loadValue
    }

    fun checkJobFitsCluster(load: Float, loadType: String, hosts: List<HostRegistration>?) {
        var processable = false
        if (hosts != null) {
            for (host in hosts) {
                if (host.maxLoad >= load) {
                    logger.trace("Host $host can process jobs of type $loadType with load $load")
                    processable = true
                    break
                }
            }
        }
        if (!processable) {
            logger.warn("No hosts found that can process jobs of type {} with load {}", loadType, load)
        }
    }
}
