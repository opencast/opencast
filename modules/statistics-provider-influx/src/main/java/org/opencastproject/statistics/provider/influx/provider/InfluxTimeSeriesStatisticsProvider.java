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

package org.opencastproject.statistics.provider.influx.provider;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.statistics.provider.influx.StatisticsProviderInfluxService;
import org.opencastproject.util.data.Tuple;

import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InfluxTimeSeriesStatisticsProvider extends InfluxStatisticsProvider implements TimeSeriesProvider {

  private static final Logger logger = LoggerFactory.getLogger(InfluxTimeSeriesStatisticsProvider.class);

  private Set<InfluxProviderConfiguration.InfluxProviderSource> sources;



  public InfluxTimeSeriesStatisticsProvider(
      StatisticsProviderInfluxService service,
      String id,
      ResourceType resourceType,
      String title,
      String description,
      Set<InfluxProviderConfiguration.InfluxProviderSource> sources
  ) {
    super(service, id, resourceType, title, description);
    this.sources = sources;
  }

  @Override
  public TimeSeries getValues(String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId) {
    final String influxGrouping = dataResolutionToInfluxGrouping(resolution);
    final List<Tuple<Instant, Instant>> periods = getPeriods(from, to, resolution, zoneId);
    final List<String> labels = new ArrayList<>();
    final List<Double> values = new ArrayList<>();
    final InfluxProviderConfiguration.InfluxProviderSource source = getSource(resolution);
    for (final Tuple<Instant, Instant> period : periods) {
      final Query query = BoundParameterQuery.QueryBuilder
          .newQuery("SELECT " + source.getAggregation() + "(" + source.getAggregationVariable() + ") FROM "
                            + source.getMeasurement() + " WHERE " + source.getResourceIdName()
                            + "=$resourceId AND time>=$from AND time<=$to" + influxGrouping)
          .bind("resourceId", resourceId)
          .bind("from", period.getA())
          .bind("to", period.getB())
          .create();
      try {
        final QueryResult results = service.getInfluxDB().query(query);
        final TimeSeries currentViews = queryResultToTimeSeries(results);
        labels.addAll(currentViews.getLabels());
        values.addAll(currentViews.getValues());
      } catch (InfluxDBIOException e) {
        if (e.getCause() instanceof ConnectException) {
          logger.error("Influx connect exception: {}", e.getMessage());
        } else {
          throw e;
        }
      }
    }
    final Double total = "SUM".equalsIgnoreCase(source.getAggregation()) ? values.stream().mapToDouble(v -> v).sum() : null;
    return new TimeSeries(labels, values, total);
  }

  @Override
  public Set<DataResolution> getDataResolutions() {
    return sources.stream().flatMap(s -> s.getResolutions().stream()).collect(Collectors.toSet());
  }

  private InfluxProviderConfiguration.InfluxProviderSource getSource(DataResolution resolution) {
    return sources.stream()
        .filter(s -> s.getResolutions().contains(resolution))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("No source available for data resolution " + resolution.name()));
  }

  protected static TimeSeries queryResultToTimeSeries(QueryResult results) {
    if (results.hasError()) {
      throw new RuntimeException("Error while retrieving result from influx: " + results.getError());
    }
    final List<String> labels = new ArrayList<>();
    final List<Double> values = new ArrayList<>();
    for (final QueryResult.Result result : results.getResults()) {
      if (result.hasError()) {
        logger.warn("An element from the set of data returned by influx DB has an error: '{}'. Ignoring this one.",
            result.getError());
        continue;
      }
      if (result.getSeries() == null || result.getSeries().isEmpty()) {
        continue;
      }
      labels.addAll(result.getSeries().get(0).getValues().stream()
          .map(l -> (String) l.get(0))
          .collect(Collectors.toList()));
      values.addAll(result.getSeries().get(0).getValues().stream()
          .map(l -> l.get(1))
          .map(v -> v == null ? 0 : (Double) v)
          .collect(Collectors.toList()));
    }
    return new TimeSeries(labels, values);
  }
}
