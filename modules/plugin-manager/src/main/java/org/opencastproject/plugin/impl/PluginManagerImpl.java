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

package org.opencastproject.plugin.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A simple tutorial class to learn about Opencast Services
 */
@Component(
    property = {
        "service.description=Plugin Manager Service"
    },
    immediate = true,
    service = PluginManagerImpl.class
)
public class PluginManagerImpl {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(PluginManagerImpl.class);

  private static final String OPENCAST_FEATURE_PREFIX = "opencast-";
  private static final String PLUGIN_FEATURE_PREFIX = OPENCAST_FEATURE_PREFIX + "plugin-";
  private static final String VERBOSE = "verbose";

  private FeaturesService featuresService;
  private Set<String> activePlugins;
  private boolean verbose;

  @Reference
  public void setFeaturesService(FeaturesService featuresService) {
    this.featuresService = featuresService;
  }

  @Activate
  @Modified
  void activate(Map<String, Object> properties) {
    logger.debug("Activating {}", PluginManagerImpl.class);

    // Load plugin configuration
    activePlugins = properties.entrySet().stream()
            .filter(e -> BooleanUtils.toBoolean(Objects.toString(e.getValue(), "")))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    logger.debug("Active plugin configuration: {}", activePlugins);

    // Verbose Karaf logs active?
    verbose = BooleanUtils.toBoolean(Objects.toString(properties.get(VERBOSE)));

    Thread thread = new Thread(new PluginStarter());
    thread.start();
  }

  public class PluginStarter implements Runnable {
    public void run() {
      EnumSet<FeaturesService.Option> options = EnumSet.noneOf(FeaturesService.Option.class);
      options.add(FeaturesService.Option.NoAutoRefreshBundles);
      if (verbose) {
        options.add(FeaturesService.Option.Verbose);
      }

      try {
        var ocFeatures = Arrays.stream(featuresService.listInstalledFeatures())
                .map(Feature::getName)
                .filter(feature -> feature.startsWith(OPENCAST_FEATURE_PREFIX))
                .filter(feature -> !feature.startsWith(PLUGIN_FEATURE_PREFIX))
                .collect(Collectors.toSet());
        logger.debug("Detected active Opencast features: {}", ocFeatures);

        var installedPlugins = Arrays.stream(featuresService.listInstalledFeatures())
                .map(Feature::getName)
                .filter(feature -> feature.startsWith(PLUGIN_FEATURE_PREFIX))
                .collect(Collectors.toSet());
        logger.debug("Detected, already active Opencast plugins: {}", installedPlugins);

        logger.info("Loading plug-ins…");
        for (Feature feature : featuresService.listFeatures()) {
          // Check if feature actually is a plugin
          if (!feature.getName().startsWith(PLUGIN_FEATURE_PREFIX)) {
            logger.debug("Skipping non-plugin feature {}", feature);
            continue;
          }

          // Check if plugin is active
          if (!activePlugins.contains(feature.getName())) {
            logger.info("Skipping disabled plugin {}", feature);
            continue;
          }

          // Check if conditions match (or if there are none)
          var conditionsMatch = feature.getConditional().stream()
                  .flatMap(conditional -> conditional.getCondition().stream())
                  .map(ocFeatures::contains)
                  .reduce(Boolean::logicalOr)
                  .orElse(true);
          if (!conditionsMatch) {
            logger.info("Plugin conditions do not match. Skipping {}", feature);
            continue;
          }

          logger.info("Installing plugin {}", feature);
          featuresService.installFeature(feature, options);
        }

        // Check if any of the previously installed features need to be uninstalled
        for (var plugin: installedPlugins) {
          if (!activePlugins.contains(plugin)) {
            logger.info("Uninstalling plugin {}", plugin);
            featuresService.uninstallFeature(plugin, options);
          }
        }
      } catch (Exception e) {
        logger.error("Installing plugins failed", e);
      }
    }
  }

}
