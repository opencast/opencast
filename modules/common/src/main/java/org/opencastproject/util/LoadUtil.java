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

package org.opencastproject.util;

import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;

public final class LoadUtil {

  private LoadUtil() {
  }

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(LoadUtil.class);

  public static float getConfiguredLoadValue(@SuppressWarnings("rawtypes") Dictionary bundleProperties, String configKey, Float defaultValue, ServiceRegistry registry) {
    String jobLoad = StringUtils.trimToNull((String) bundleProperties.get(configKey));
    float loadValue = defaultValue;
    if (jobLoad != null) {
      try {
        loadValue = Float.parseFloat(jobLoad);
        if (loadValue < 0) {
          logger.warn("Load value for key {} set to less than 0, defaulting to 0", configKey);
          loadValue = 0.0f;
        }
        logger.info("Set load for key {} to {}", configKey, loadValue);
      } catch (NumberFormatException e) {
        logger.warn("Can not set job loads to {}. {} must be a float", jobLoad, configKey);
        loadValue = defaultValue;
        logger.info("Set load for key {} to default of {}", configKey, loadValue);
      }
    } else {
      logger.info("No job load configuration found for key {}, load to default of {}", configKey, loadValue);
    }
    try {
      checkJobFitsCluster(loadValue, configKey, registry.getHostRegistrations());
    } catch (ServiceRegistryException e) {
      logger.warn("Unable to verify that {} will run on this cluster due to load of {}", configKey, loadValue);
    }
    return loadValue;
  }

  public static void checkJobFitsCluster(float load, String loadType, List<HostRegistration> hosts) {
    boolean processable = false;
    if (hosts != null) {
      for (HostRegistration host : hosts) {
        if (host.getMaxLoad() >= load) {
          logger.trace("Host " + host.toString() + " can process jobs of type " + loadType + " with load " + load);
          processable = true;
          break;
        }
      }
    }
    if (!processable) {
      logger.warn("No hosts found that can process jobs of type {} with load {}", loadType, load);
    }
  }
}
