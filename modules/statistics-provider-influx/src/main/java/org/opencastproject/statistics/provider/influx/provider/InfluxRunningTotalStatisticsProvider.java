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

import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InfluxRunningTotalStatisticsProvider extends InfluxStatisticsProvider implements TimeSeriesProvider {
  private final Set<InfluxProviderConfiguration.InfluxProviderSource> sources;

  public InfluxRunningTotalStatisticsProvider(
          StatisticsProviderInfluxService service,
          String id,
          ResourceType resourceType,
          String title,
          String description,
          Set<InfluxProviderConfiguration.InfluxProviderSource> sources) {
    super(service, id, resourceType, title, description);
    this.sources = sources;
  }

  private static double reduceResult(double previousTotal, QueryResult.Result newResult) {
    if (newResult.getSeries() == null) {
      return previousTotal;
    }
    return previousTotal + newResult
            .getSeries()
            .stream()
            .reduce(0.0, InfluxRunningTotalStatisticsProvider::reduceSeries, Double::sum);
  }

  private static double reduceSeries(double previousSeriesTotal, QueryResult.Series newSeries) {
    if (newSeries.getValues().isEmpty()) {
      return previousSeriesTotal;
    }
    if (newSeries.getValues().size() > 1) {
      throw new RuntimeException("invalid results returned for aggregation");
    }
    final List<Object> objects = newSeries.getValues().get(0);
    if (objects.size() != 2) {
      throw new RuntimeException("invalid results returned for aggregation");
    }
    final Object o = objects.get(1);
    if (o == null) {
      return previousSeriesTotal;
    }
    if (!(o instanceof Double)) {
      throw new RuntimeException("invalid results returned for aggregation");
    }
    return previousSeriesTotal + (Double) o;
  }

  private double getPreviousTotal(
          final InfluxProviderConfiguration.InfluxProviderSource source, final String resourceId, final Instant from) {
    final Query beforeQuery = BoundParameterQuery.QueryBuilder
            .newQuery("SELECT SUM(" + source.getAggregationVariable() + ") FROM " + source.getMeasurement() + " WHERE "
                              + source.getResourceIdName() + "=$resourceId AND time<$from")
            .bind("resourceId", resourceId)
            .bind("from", from)
            .create();
    final QueryResult beforeResults = service.getInfluxDB().query(beforeQuery);
    if (beforeResults.hasError()) {
      throw new RuntimeException("Error while retrieving result from influx: " + beforeResults.getError());
    }
    return beforeResults
            .getResults()
            .stream()
            .reduce(0.0, InfluxRunningTotalStatisticsProvider::reduceResult, Double::sum);
  }

  @Override
  public TimeSeries getValues(String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId) {
    final String influxGrouping = dataResolutionToInfluxGrouping(resolution);
    final List<Tuple<Instant, Instant>> periods = getPeriods(from, to, resolution, zoneId);
    final List<String> labels = new ArrayList<>();
    final List<Double> values = new ArrayList<>();
    final InfluxProviderConfiguration.InfluxProviderSource source = getSource(resolution);
    double previousTotal = getPreviousTotal(source, resourceId, from);
    for (final Tuple<Instant, Instant> period : periods) {
      final Query query = BoundParameterQuery.QueryBuilder
              .newQuery("SELECT " + source.getAggregation() + "(" + source.getAggregationVariable() + ") FROM " + source
                      .getMeasurement() + " WHERE " + source.getResourceIdName()
                                + "=$resourceId AND time>=$from AND time<=$to" + influxGrouping)
              .bind("resourceId", resourceId)
              .bind("from", period.getA())
              .bind("to", period.getB())
              .create();
      final QueryResult results = service.getInfluxDB().query(query);
      final Tuple<TimeSeries, Double> currentViews = queryResultToTimeSeries(results, previousTotal, period.getA());
      previousTotal = currentViews.getB();
      labels.addAll(currentViews.getA().getLabels());
      values.addAll(currentViews.getA().getValues());
    }
    final Double total = "SUM".equalsIgnoreCase(source.getAggregation())
            ? values.stream().mapToDouble(v -> v).sum()
            : null;
    return new TimeSeries(labels, values, total);
  }

  private InfluxProviderConfiguration.InfluxProviderSource getSource(DataResolution resolution) {
    return sources
            .stream()
            .filter(s -> s.getResolutions().contains(resolution))
            .findAny()
            .orElseThrow(() -> new IllegalStateException(
                    "No source available for data resolution " + resolution.name()));
  }

  @Override
  public Set<DataResolution> getDataResolutions() {
    return new HashSet<>(Arrays.asList(DataResolution.YEARLY, DataResolution.MONTHLY, DataResolution.DAILY));
  }

  private Tuple<TimeSeries, Double> queryResultToTimeSeries(
          final QueryResult results,
          final double previousTotal,
          final Instant periodStart) {
    if (results.hasError()) {
      throw new RuntimeException("Error while retrieving result from influx: " + results.getError());
    }
    final List<String> labels = new ArrayList<>();
    final List<Double> values = new ArrayList<>();
    double previousSum = previousTotal;
    for (final QueryResult.Result result : results.getResults()) {
      if (result.getSeries() == null || result.getSeries().isEmpty()) {
        labels.add(periodStart.toString());
        values.add(previousSum);
        continue;
      }
      labels.addAll(result
                            .getSeries()
                            .get(0)
                            .getValues()
                            .stream()
                            .map(l -> (String) l.get(0))
                            .collect(Collectors.toList()));
      for (List<Object> valueList : result.getSeries().get(0).getValues()) {
        final Double currentValue = reduceValueList(valueList);
        previousSum += currentValue;
        values.add(previousSum);
      }
    }
    return Tuple.tuple(new TimeSeries(labels, values), previousSum);
  }

  private double reduceValueList(List<Object> valueList) {
    Object v = valueList.get(1);
    if (v == null) {
      return 0.0;
    }
    return (Double) v;
  }
}
