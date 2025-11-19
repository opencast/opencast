/*
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

package org.opencastproject.statistics.provider.matomo;

import org.opencastproject.statistics.api.StatisticsCoordinator;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.provider.matomo.provider.BatchMatomoRequest;
import org.opencastproject.statistics.provider.matomo.provider.MatomoProviderConfiguration;
import org.opencastproject.statistics.provider.matomo.provider.MatomoTimeSeriesStatisticsProvider;
import org.opencastproject.util.ConfigurationException;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements statistics providers using Matomo for data retrieval.
 */
@Component(
    immediate = true,
    service = { ArtifactInstaller.class },
    property = {
        "service.description=Statistics Provider Matomo Service"
    }
)
public class StatisticsProviderMatomoService implements ArtifactInstaller {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsProviderMatomoService.class);

  private static final String KEY_MATOMO_API_URL = "matomo.api.url";
  private static final String KEY_MATOMO_API_TOKEN = "matomo.api.token";

  private String matomoApiUrl;
  private String matomoApiToken;

  private StatisticsCoordinator statisticsCoordinator;
  private Map<String, StatisticsProvider> fileNameToProvider = new ConcurrentHashMap<>();
  private Map<String, BatchMatomoRequest> methodToBatchRequest = new ConcurrentHashMap<>();

  /**
   * Get an existing batch request for a specific ResourceType and Matomo API method
   * @param resourceTypeMethod The ResourceType concatenated with the Matomo API method
   * @return The existing batch request or null if none exists
   */
  public BatchMatomoRequest getBatchRequest(String resourceTypeMethod) {
    return methodToBatchRequest.get(resourceTypeMethod);
  }

  /**
   * Register a new batch request for a specific ResourceType and Matomo API method
   * @param resourceTypeMethod The ResourceType concatenated with the Matomo API method
   * @param batchRequest The batch request to register
   */
  public void registerBatchRequest(String resourceTypeMethod, BatchMatomoRequest batchRequest) {
    methodToBatchRequest.put(resourceTypeMethod, batchRequest);
  }

  @Reference
  public void setStatisticsCoordinator(StatisticsCoordinator service) {
    this.statisticsCoordinator = service;
  }

  @Activate
  public void activate(Map<String, Object> properties) {
    logger.info("Activating Statistics Provider Matomo Service");
    modified(properties);
  }

  @Deactivate
  public void deactivate() {
    logger.info("Deactivating Statistics Provider Matomo Service");
  }

  @Override
  public void install(File file) throws Exception {
    final String json = new String(Files.readAllBytes(file.toPath()), Charset.forName("utf-8"));
    final MatomoProviderConfiguration providerCfg = MatomoProviderConfiguration.fromJson(json);
    StatisticsProvider provider;
    switch (providerCfg.getType().toLowerCase()) {
      case "timeseries": {
        provider = new MatomoTimeSeriesStatisticsProvider(
                this,
                providerCfg.getId(),
                providerCfg.getResourceType(),
                providerCfg.getTitle(),
                providerCfg.getDescription(),
                providerCfg.getSources());
        logger.info("Installed Matomo time series statistics provider '{}'", providerCfg.getId());
      }
      break;
      default:
        throw new ConfigurationException("Unknown Matomo statistics type: " + providerCfg.getType());
    }
    fileNameToProvider.put(file.getName(), provider);
    statisticsCoordinator.addProvider(provider);
  }

  @Override
  public void uninstall(File file) {
    if (fileNameToProvider.containsKey(file.getName())) {
      statisticsCoordinator.removeProvider(fileNameToProvider.get(file.getName()));
      fileNameToProvider.remove(file.getName());
    }
  }

  @Override
  public boolean canHandle(File file) {
    return "statistics".equals(file.getParentFile().getName())
        && file.getName().endsWith(".json")
        && file.getName().toUpperCase().startsWith("matomo.".toUpperCase());
  }

  @Override
  public void update(File file) throws Exception {
    uninstall(file);
    install(file);
  }

  @Modified
  public void modified(Map<String, Object> properties) {
    if (properties == null) {
      logger.info("No configuration available. Not connecting to Matomo API.");
    } else {
      final Object matomoApiUrlValue = properties.get(KEY_MATOMO_API_URL);
      if (matomoApiUrlValue != null) {
        matomoApiUrl = matomoApiUrlValue.toString();
      } else {
        throw new ConfigurationException("Matomo API URL is missing in config file.");
      }
      final Object matomoApiTokenValue = properties.get(KEY_MATOMO_API_TOKEN);
      if (matomoApiTokenValue != null) {
        matomoApiToken = matomoApiTokenValue.toString();
      } else {
        throw new ConfigurationException("Matomo API access token is missing in config file.");
      }
      logger.info("Updated Matomo API URL to '{}'", matomoApiUrl);
    }
  }

  public String getMatomoApiUrl() {
    return matomoApiUrl;
  }

  public String getMatomoApiToken() {
    return matomoApiToken;
  }
}
