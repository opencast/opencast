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

package org.opencastproject.statistics.provider.random;

import org.opencastproject.statistics.api.ConfiguredProvider;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsProviderRegistry;
import org.opencastproject.statistics.provider.random.provider.RandomStatisticsProvider;
import org.opencastproject.util.ConfigurationException;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements statistics providers using random data (showcase).
 */
public class StatisticsProviderRandomService implements ArtifactInstaller {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsProviderRandomService.class);

  private StatisticsProviderRegistry statisticsProviderRegistry;
  private Map<String, StatisticsProvider> fileNameToProvider = new ConcurrentHashMap<>();

  public void setStatisticsProviderRegistry(StatisticsProviderRegistry service) {
    this.statisticsProviderRegistry = service;
  }

  public void activate(ComponentContext cc) {
    logger.info("Activating Statistics Provider Random Service");
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating Statistics Provider Random Service");
    fileNameToProvider.values().forEach(provider -> statisticsProviderRegistry.removeProvider(provider));
  }

  @Override
  public void install(File file) throws Exception {
    final String json = new String(Files.readAllBytes(file.toPath()), Charset.forName("utf-8"));
    final ConfiguredProvider providerCfg = ConfiguredProvider.fromJson(json);
    if (!providerCfg.getSource().toUpperCase().startsWith("RANDOM:")) {
      throw new ConfigurationException("Unexpected source string: " + providerCfg.getSource());
    }
    if ("timeseries".equalsIgnoreCase(providerCfg.getType())) {
      final StatisticsProvider provider = new RandomStatisticsProvider(
          providerCfg.getId(),
          providerCfg.getResourceType(),
          providerCfg.getResolutions(),
          providerCfg.getTitle(),
          providerCfg.getDescription()
      );
      fileNameToProvider.put(file.getName(), provider);
      statisticsProviderRegistry.addProvider(provider);
    } else {
      throw new ConfigurationException("Unknown random statistics type: " + providerCfg.getType());
    }
  }

  @Override
  public void uninstall(File file) throws Exception {
    if (fileNameToProvider.containsKey(file.getName())) {
      statisticsProviderRegistry.removeProvider(fileNameToProvider.get(file.getName()));
      fileNameToProvider.remove(file.getName());
    }
  }

  @Override
  public boolean canHandle(File file) {
    return "statistics".equals(file.getParentFile().getName())
        && file.getName().endsWith(".json")
        && file.getName().toUpperCase().startsWith("random.".toUpperCase());
  }

  @Override
  public void update(File file) throws Exception {
    uninstall(file);
    install(file);
  }
}
