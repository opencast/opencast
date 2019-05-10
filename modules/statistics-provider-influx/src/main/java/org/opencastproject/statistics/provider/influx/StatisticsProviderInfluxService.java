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

import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsCoordinator;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsWriter;
import org.opencastproject.statistics.provider.influx.provider.InfluxProviderConfiguration;
import org.opencastproject.statistics.provider.influx.provider.InfluxRunningTotalStatisticsProvider;
import org.opencastproject.statistics.provider.influx.provider.InfluxTimeSeriesStatisticsProvider;
import org.opencastproject.util.ConfigurationException;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Implements statistics providers using influxdb for permanent storage.
 */
public class StatisticsProviderInfluxService implements ManagedService, ArtifactInstaller, StatisticsWriter {

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


  private StatisticsCoordinator statisticsCoordinator;
  private Map<String, StatisticsProvider> fileNameToProvider = new ConcurrentHashMap<>();


  public void setStatisticsCoordinator(StatisticsCoordinator service) {
    this.statisticsCoordinator = service;
  }

  public void activate(ComponentContext cc) {
    logger.info("Activating Statistics Provider Influx Service");
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating Statistics Provider Influx Service");
    disconnectInflux();
  }

  @Override
  public void install(File file) throws Exception {
    final String json = new String(Files.readAllBytes(file.toPath()), Charset.forName("utf-8"));
    final InfluxProviderConfiguration providerCfg = InfluxProviderConfiguration.fromJson(json);
    StatisticsProvider provider;
    switch (providerCfg.getType().toLowerCase()) {
      case "timeseries": {
        provider = new InfluxTimeSeriesStatisticsProvider(
                this,
                providerCfg.getId(),
                providerCfg.getResourceType(),
                providerCfg.getTitle(),
                providerCfg.getDescription(),
                providerCfg.getSources());
      }
      break;
      case "runningtotal":
        provider = new InfluxRunningTotalStatisticsProvider(
                this,
                providerCfg.getId(),
                ResourceType.ORGANIZATION,
                providerCfg.getTitle(),
                providerCfg.getDescription(),
                providerCfg.getSources());
        break;
      default:
        throw new ConfigurationException("Unknown influx statistics type: " + providerCfg.getType());
    }
    fileNameToProvider.put(file.getName(), provider);
    if (influxDB != null) {
      statisticsCoordinator.addProvider(provider);
    }
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
    fileNameToProvider.values().forEach(provider -> statisticsCoordinator.addProvider(provider));
    statisticsCoordinator.addWriter(this);
  }

  private void disconnectInflux() {
    if (influxDB != null) {
      fileNameToProvider.values().forEach(provider -> statisticsCoordinator.removeProvider(provider));
      influxDB.close();
      influxDB = null;
    }
  }

  @Override
  public void writeDuration(
          String organizationId,
          String measurementName,
          String retentionPolicy,
          String organizationIdResourceName,
          String fieldName,
          TimeUnit temporalResolution,
          Duration duration) {
    double divider;
    switch (temporalResolution) {
      case MILLISECONDS:
        divider = 1.0;
        break;
      case SECONDS:
        divider = 1000.0;
        break;
      case MINUTES:
        divider = 1000.0 * 60.0;
        break;
      case HOURS:
        divider = 1000.0 * 60.0 * 60.0;
        break;
      case DAYS:
        divider = 1000.0 * 60.0 * 60.0 * 24.0;
        break;
      default:
        throw new RuntimeException("nanosecond and microsecond resolution not supported");
    }
    final Point point = Point
            .measurement(measurementName)
            .tag(organizationIdResourceName, organizationId)
            .addField(fieldName, duration.toMillis() / divider)
            .build();
    if (retentionPolicy == null) {
      influxDB.write(point);
    } else {
      influxDB.write(BatchPoints.builder().point(point).retentionPolicy(retentionPolicy).build());
    }
  }

  @Override
  public String getId() {
    return "influxdb-writer";
  }
}
