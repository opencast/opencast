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

package org.opencastproject.statistics.provider.influx;

import org.opencastproject.statistics.api.ConfiguredProvider;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsProviderRegistry;
import org.opencastproject.statistics.provider.influx.provider.InfluxTimeSeriesStatisticsProvider;
import org.opencastproject.util.ConfigurationException;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.json.simple.parser.ParseException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements statistics providers using influxdb for permanent storage.
 */
public class StatisticsProviderInfluxService implements ManagedService, ArtifactInstaller {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsProviderInfluxService.class);

  private static final String KEY_INFLUX_URI = "influx.uri";
  private static final String KEY_INFLUX_USER = "influx.username";
  private static final String KEY_INFLUX_PW = "influx.password";
  private static final String KEY_INFLUX_DB = "influx.db";

  private String influxUri = "http://127.0.0.1:8086";
  private String influxUser = "root";
  private String influxPw = "root";
  private String influxDbName = "opencast";

  private volatile InfluxDB influxDB;


  private StatisticsProviderRegistry statisticsProviderRegistry;
  private Map<String, StatisticsProvider> fileNameToProvider = new ConcurrentHashMap<>();


  public void setStatisticsProviderRegistry(StatisticsProviderRegistry service) {
    this.statisticsProviderRegistry = service;
  }

  public void activate(ComponentContext cc) throws ParseException, IOException {
    logger.info("Activating Statistics Provider Influx Service");
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating Statistics Provider Influx Service");
    disconnectInflux();
  }

  @Override
  public void install(File file) throws Exception {
    final String json = new String(Files.readAllBytes(file.toPath()), Charset.forName("utf-8"));
    final ConfiguredProvider providerCfg = ConfiguredProvider.fromJson(json);
    if (!providerCfg.getSource().toUpperCase().startsWith("INFLUX:")) {
      throw new ConfigurationException("Unexpected source string: " + providerCfg.getSource());
    }
    if ("timeseries".equalsIgnoreCase(providerCfg.getType())) {
      final StatisticsProvider provider = new InfluxTimeSeriesStatisticsProvider(
          this,
          providerCfg.getId(),
          providerCfg.getResourceType(),
          providerCfg.getResolutions(),
          providerCfg.getTitle(),
          providerCfg.getDescription(),
          providerCfg.getSource().split(":")[2],
          providerCfg.getSource().split(":")[3],
          providerCfg.getSource().split(":")[1],
          providerCfg.getSource().split(":")[4]
      );
      fileNameToProvider.put(file.getName(), provider);
      if (influxDB != null) {
        statisticsProviderRegistry.addProvider(provider);
      }
    } else {
      throw new ConfigurationException("Unknown influx statistics type: " + providerCfg.getType());
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
        && file.getName().toUpperCase().startsWith("influx.".toUpperCase());
  }

  @Override
  public void update(File file) throws Exception {
    uninstall(file);
    install(file);
  }

  @Override
  public void updated(Dictionary<String, ?> dictionary) {
    if (dictionary == null) {
      logger.info("No configuration available. Not connecting to influx DB.");
      disconnectInflux();
    } else {
      final Object influxUriValue = dictionary.get(KEY_INFLUX_URI);
      if (influxUriValue != null) {
        influxUri = influxUriValue.toString();
      }
      final Object influxUserValue = dictionary.get(KEY_INFLUX_USER);
      if (influxUserValue != null) {
        influxUser = influxUserValue.toString();
      }
      final Object influxPwValue = dictionary.get(KEY_INFLUX_PW);
      if (influxPwValue != null) {
        influxPw = influxPwValue.toString();
      }
      final Object influxDbValue = dictionary.get(KEY_INFLUX_DB);
      if (influxDbValue != null) {
        influxDbName = influxDbValue.toString();
      }
      connectInflux();
    }
  }

  public InfluxDB getInfluxDB() {
    return influxDB;
  }

  private void connectInflux() {
    disconnectInflux();
    influxDB = InfluxDBFactory.connect(influxUri, influxUser, influxPw);
    influxDB.setDatabase(influxDbName);
    fileNameToProvider.values().forEach(provider -> statisticsProviderRegistry.addProvider(provider));
  }

  private void disconnectInflux() {
    if (influxDB != null) {
      fileNameToProvider.values().forEach(provider -> statisticsProviderRegistry.removeProvider(provider));
      influxDB.close();
      influxDB = null;
    }
  }

}
